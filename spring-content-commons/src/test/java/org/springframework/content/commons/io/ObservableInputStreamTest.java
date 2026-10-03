package org.springframework.content.commons.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Observable;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ObservableInputStreamTest {

    private ObservableInputStream ois;

    private FileInputStream fis;
    private InputStreamObserver observer;

    @Nested
    class ObservableInputStreamCases {
        @Nested
        class WhenAnInputStreamIsObserved {
            @Nested
            class WhenTheInputStreamHasListeners {
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
            class WhenTheInputStreamIsRead {
                @BeforeEach
                void setUp() throws IOException {
                    fis = mock(FileInputStream.class);
                    observer = mock(InputStreamObserver.class);

                    ois = new ObservableInputStream(fis, observer);

                    ois.read();
                }

                @Test
                void shouldDelegateToTheUnderlyingInputStream() throws IOException {
                    verify(fis).read();
                }

            }

            @Nested
            class WhenTheInputStreamIsClosed {
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
