package org.springframework.content.commons.io;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ObservableInputStreamTest {

    private ObservableInputStream ois;

    private FileInputStream fis;
    private InputStreamObserver observer;

    @Nested
    class ObservableInputStreamCases {
        @Nested
        @DisplayName("When an input stream is observed")
        class InputStreamIsObserved {
            @Nested
            @DisplayName("When the input stream has listeners")
            class InputStreamHasListeners {
                @BeforeEach
                void setUp() {
                    fis = mock(FileInputStream.class);
                    observer = mock(InputStreamObserver.class);

                    ois = new ObservableInputStream(fis, observer);
                }

                @Test
                void shouldReturnThem() {
                    assertThat(ois.getObservers()).contains(observer);
                }
            }

            @Nested
            @DisplayName("When the input stream is read")
            class InputStreamIsRead {
                @BeforeEach
                void setUp() {
                    fis = mock(FileInputStream.class);
                    observer = mock(InputStreamObserver.class);

                    ois = new ObservableInputStream(fis, observer);
                }

                @Test
                void shouldDelegateToUnderlyingInputStream() throws IOException {
                    assertThat(ois.read()).isZero();
                    verify(fis).read();
                }
            }

            @Nested
            @DisplayName("When the input stream is closed")
            class InputStreamIsClosed {
                @BeforeEach
                void setUp() throws IOException {
                    fis = mock(FileInputStream.class);
                    observer = mock(InputStreamObserver.class);

                    ois = new ObservableInputStream(fis, observer);

                    ois.close();
                }

                @Test
                void shouldCallListenersOnClosedEventHandler() {
                    verify(observer).closed();
                }
            }
        }
    }
}
