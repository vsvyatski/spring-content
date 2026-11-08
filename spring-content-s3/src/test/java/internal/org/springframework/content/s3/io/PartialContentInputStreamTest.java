package internal.org.springframework.content.s3.io;

import org.junit.jupiter.api.extension.ExtendWith;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.apache.commons.io.IOUtils;

@ExtendWith(SpringExtension.class)
public class PartialContentInputStreamTest {

    private static final byte[] FULL_DATA = "This is a test string".getBytes(StandardCharsets.UTF_8);
    private static final byte NUL = 0;

    private InputStream inputStream;

    @Nested
    class PartialContentInputStreamCases {
        @Nested
        class WithARangeFromTheStart {
            @BeforeEach
            void setUp() {
                inputStream = PartialContentInputStream.fromContentRange(
                        new ByteArrayInputStream(FULL_DATA, 0, 4),
                        "bytes 0-3/"+FULL_DATA.length // bytes in the range description are *inclusive*
                );
            }

            @AfterEach
            void tearDown() throws IOException {
                inputStream.close();
            }

            @Test
            void readsFullyFromStartToFinish() throws IOException {
                var readData = new byte[FULL_DATA.length];
                Arrays.fill(readData, (byte)0xba); // Fill array to detect that it is properly filled with NUL bytes by the read function
                IOUtils.readFully(inputStream, readData);

                assertThat(readData[0]).isEqualTo(FULL_DATA[0]);
                assertThat(readData[1]).isEqualTo(FULL_DATA[1]);
                assertThat(readData[2]).isEqualTo(FULL_DATA[2]);
                assertThat(readData[3]).isEqualTo(FULL_DATA[3]);
                for(int i = 4; i < FULL_DATA.length; i++) {
                    assertThat(readData[i]).isEqualTo(NUL);
                }

                // Stream is at EOF after reading all the bytes
                assertThat(inputStream.read()).isEqualTo(-1);
            }

            @Test
            void skipsBytesIntoTheRange() throws IOException {
                inputStream.skipNBytes(2);

                var readData = new byte[6];
                Arrays.fill(readData, (byte)0xba); // Fill array to detect that it is properly filled with NUL bytes by the read function
                IOUtils.readFully(inputStream, readData);
                assertThat(readData[0]).isEqualTo(FULL_DATA[2]);
                assertThat(readData[1]).isEqualTo(FULL_DATA[3]);
                assertThat(readData[2]).isEqualTo(NUL);
            }

            @Test
            void skipsBytesInsideTheRange() throws IOException {
                assertThat(inputStream.read()).isEqualTo(FULL_DATA[0] & 0xff);
                inputStream.skipNBytes(2); // Bytes 1 & 2 are skipped
                assertThat(inputStream.read()).isEqualTo(FULL_DATA[3] & 0xff);
                assertThat(inputStream.read()).isEqualTo(NUL & 0xff);
            }

            @Test
            void skipsBytesOutOfTheRange() throws IOException {
                assertThat(inputStream.read()).isEqualTo(FULL_DATA[0] & 0xff);
                inputStream.skipNBytes(FULL_DATA.length - 1); // Skip until past the end of the range; right up until the end of the data

                assertThat(inputStream.read()).isEqualTo(-1); // EOF
            }

            @Test
            void skipsBytesAfterTheEndOfTheRange() throws IOException {
                inputStream.skipNBytes(4); // Skip right up to the end of the range

                assertThat(inputStream.skip(Long.MAX_VALUE)).isEqualTo((long)FULL_DATA.length - 4); // All the rest of the bytes can be skipped at once
            }

        }

        @Nested
        class WithARangeToTheEnd {
            @BeforeEach
            void setUp() {
                inputStream = PartialContentInputStream.fromContentRange(
                        new ByteArrayInputStream(FULL_DATA, 10, FULL_DATA.length-10),
                        "bytes 10-"+FULL_DATA.length+"/*"
                );
            }

            @AfterEach
            void tearDown() throws IOException {
                inputStream.close();
            }

            @Test
            void readsFullyFromStartToFinish() throws IOException {
                var readData = new byte[FULL_DATA.length];
                Arrays.fill(readData, (byte)0xba); // Fill array to detect that it is properly filled with NUL bytes by the read function
                IOUtils.readFully(inputStream, readData);

                for(int i = 0; i < 10; i++) {
                    assertThat(readData[i]).isEqualTo(NUL);
                }
                for(int i = 10; i < FULL_DATA.length; i++) {
                    assertThat(readData[i]).isEqualTo(FULL_DATA[i]);
                }
                // Stream is at EOF after reading all the bytes
                assertThat(inputStream.read()).isEqualTo(-1);
            }

            @Test
            void skipsBytesBeforeStartOfTheRange() throws IOException {
                assertThat(inputStream.skip(5)).isEqualTo(5L); // Can skip bytes before start of range

                // Check that read data skips the 5 bytes that were skipped
                var readData = new byte[FULL_DATA.length-5];
                Arrays.fill(readData, (byte)0xba); // Fill array to detect that it is properly filled with NUL bytes by the read function
                IOUtils.readFully(inputStream, readData);
                assertThat(readData[0]).isEqualTo(NUL);
                assertThat(readData[4]).isEqualTo(NUL);
                for(int i = 5; i < readData.length; i++) {
                    assertThat(readData[i]).isEqualTo(FULL_DATA[5+i]);
                }

                assertThat(inputStream.read()).isEqualTo(-1); // EOF
            }

            @Test
            void skipsBytesIntoTheRange() throws IOException {
                inputStream.skipNBytes(15);

                var readData = new byte[6];
                IOUtils.readFully(inputStream, readData);
                for(int i = 0; i < 6; i++) {
                    assertThat(readData[i]).isEqualTo(FULL_DATA[15+i]);
                }
            }

            @Test
            void skipsBytesInsideTheRange() throws IOException {
                inputStream.skipNBytes(10); // Skip right up to the start of the range

                assertThat(inputStream.read()).isEqualTo(FULL_DATA[10] & 0xff);
                inputStream.skipNBytes(2); // bytes 11 & 12 are skipped
                assertThat(inputStream.read()).isEqualTo(FULL_DATA[13] & 0xff);
            }

            @Test
            void skipsBytesOutOfTheRange() throws IOException {
                inputStream.skipNBytes(10); // Skip right up to the start of the range
                assertThat(inputStream.read()).isEqualTo(FULL_DATA[10] & 0xff);
                inputStream.skipNBytes(FULL_DATA.length - 11); // Skip until the end of the range

                assertThat(inputStream.read()).isEqualTo(-1); // EOF
            }

        }

        @Nested
        class WithARangeInTheMiddle {
            @BeforeEach
            void setUp() {
                inputStream = PartialContentInputStream.fromContentRange(
                        new ByteArrayInputStream(FULL_DATA, 3, 4),
                        "bytes 3-6/"+FULL_DATA.length // bytes in the range description are *inclusive*
                );
            }

            @AfterEach
            void tearDown() throws IOException {
                inputStream.close();
            }

            @Test
            void readsFullyFromStartToFinish() throws IOException {
                var readData = new byte[FULL_DATA.length];
                Arrays.fill(readData, (byte)0xba); // Fill array to detect that it is properly filled with NUL bytes by the read function
                IOUtils.readFully(inputStream, readData);

                for(int i = 0; i < 3; i++) {
                    assertThat(readData[i]).isEqualTo(NUL);
                }
                assertThat(readData[3]).isEqualTo(FULL_DATA[3]);
                assertThat(readData[4]).isEqualTo(FULL_DATA[4]);
                assertThat(readData[5]).isEqualTo(FULL_DATA[5]);
                assertThat(readData[6]).isEqualTo(FULL_DATA[6]);
                for(int i = 7; i < FULL_DATA.length; i++) {
                    assertThat(readData[i]).isEqualTo(NUL);
                }

                // Stream is at EOF after reading all the bytes
                assertThat(inputStream.read()).isEqualTo(-1);
            }

            @Test
            void skipsBytesBeforeStartOfTheRange() throws IOException {
                assertThat(inputStream.skip(2)).isEqualTo(2L); // Can skip bytes before start of range

                // Check that read data skips the 2 bytes that were skipped
                var readData = new byte[6];
                Arrays.fill(readData, (byte)0xba); // Fill array to detect that it is properly filled with NUL bytes by the read function
                IOUtils.readFully(inputStream, readData);
                assertThat(readData[0]).isEqualTo(NUL);
                assertThat(readData[1]).isEqualTo(FULL_DATA[3]);
                assertThat(readData[2]).isEqualTo(FULL_DATA[4]);
                assertThat(readData[3]).isEqualTo(FULL_DATA[5]);
                assertThat(readData[4]).isEqualTo(FULL_DATA[6]);
                assertThat(readData[5]).isEqualTo(NUL);
            }

            @Test
            void skipsBytesIntoTheRange() throws IOException {
                inputStream.skipNBytes(4);

                var readData = new byte[6];
                Arrays.fill(readData, (byte)0xba); // Fill array to detect that it is properly filled with NUL bytes by the read function
                IOUtils.readFully(inputStream, readData);
                assertThat(readData[0]).isEqualTo(FULL_DATA[4]);
                assertThat(readData[1]).isEqualTo(FULL_DATA[5]);
                assertThat(readData[2]).isEqualTo(FULL_DATA[6]);
                assertThat(readData[3]).isEqualTo(NUL);
            }

            @Test
            void skipsBytesInsideTheRange() throws IOException {
                inputStream.skipNBytes(3); // Skip right up to the start of the range

                assertThat(inputStream.read()).isEqualTo(FULL_DATA[3] & 0xff);
                inputStream.skipNBytes(2); // Bytes 4 & 5 are skipped
                assertThat(inputStream.read()).isEqualTo(FULL_DATA[6] & 0xff);
                assertThat(inputStream.read()).isEqualTo(NUL & 0xff);
            }

            @Test
            void skipsBytesOutOfTheRange() throws IOException {
                inputStream.skipNBytes(3); // Skip right up to the start of the range
                assertThat(inputStream.read()).isEqualTo(FULL_DATA[3] & 0xff);
                inputStream.skipNBytes(FULL_DATA.length - 4); // Skip until past the end of the range; right up until the end of the data

                assertThat(inputStream.read()).isEqualTo(-1); // EOF
            }

            @Test
            void skipsBytesAfterTheEndOfTheRange() throws IOException {
                inputStream.skipNBytes(7); // Skip right up to the end of the range

                assertThat(inputStream.skip(Long.MAX_VALUE)).isEqualTo((long)FULL_DATA.length - 7); // All the rest of the bytes can be skipped at once
            }

        }

    }

}