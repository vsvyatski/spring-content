package internal.org.springframework.renditions.poi;

import org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.content.test.support.TestSupportData;
import org.springframework.renditions.poi.POIService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class POIServiceTest {

    private POIService poi;

    private InputStream stream;

    @Nested
    class POIServiceCases {
        @Nested
        class XwpfDocument {
            @Nested
            class GivenAnInputStream {
                @BeforeEach
                void setUp() {
                    poi = new POIServiceImpl();

                    stream = this.getClass().getResourceAsStream("/sample-docx.docx");
                }

                @Test
                void shouldReturnAnInstanceOfAnXPWFDocument() throws IOException {
                    assertThat(poi.xwpfDocument(stream)).isNotNull();
                }
            }

            @Nested
            class GivenNullInputStream {
                @BeforeEach
                void setUp() {
                    poi = new POIServiceImpl();
                }

                @Test
                void shouldThrowAnException() {
                    assertThatThrownBy(() -> poi.xwpfDocument(stream))
                        .isInstanceOf(IllegalArgumentException.class);
                }
            }

            @Nested
            class GivenInvalidInputStream {
                @BeforeEach
                void setUp() {
                    poi = new POIServiceImpl();

                    stream = new ByteArrayInputStream(
                        TestSupportData.SAMPLE_INVALID_OFFICE_XML_PAYLOAD.getBytes(StandardCharsets.UTF_8));
                }

                @Test
                void shouldThrowAnException() {
                    assertThatThrownBy(() -> poi.xwpfDocument(stream))
                        .isInstanceOf(NotOfficeXmlFileException.class);
                }
            }
        }
    }
}
