package org.springframework.content.commons.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileInputStream;
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
                void setUp() throws Throwable {
                    fis = mock(FileInputStream.class);
                    observer = mock(InputStreamObserver.class);

                    ois = new ObservableInputStream(fis, observer);

                }

                @Test
                void shouldReturnThem() throws Throwable {
                    assertThat(ois.getObservers()).contains(observer);

                }

            }

            @Nested
            class WhenTheInputStreamIsRead {
                @BeforeEach
                void setUp() throws Throwable {
                    fis = mock(FileInputStream.class);
                    observer = mock(InputStreamObserver.class);

                    ois = new ObservableInputStream(fis, observer);

                    ois.read();

                }

                @Test
                void shouldDelegateToTheUnderlyingInputStream() throws Throwable {
                    verify(fis).read();

                }

            }

            @Nested
            class WhenTheInputStreamIsClosed {
                @BeforeEach
                void setUp() throws Throwable {
                    fis = mock(FileInputStream.class);
                    observer = mock(InputStreamObserver.class);

                    ois = new ObservableInputStream(fis, observer);

                    ois.close();

                }

                @Test
                void shouldCallListenersOnClosedEventHandler() throws Throwable {
                    verify(observer).closed();

                }

            }

        }

    }

}
