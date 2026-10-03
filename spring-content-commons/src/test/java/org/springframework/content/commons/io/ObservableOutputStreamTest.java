package org.springframework.content.commons.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import static org.assertj.core.api.Assertions.assertThat;

import org.mockito.InOrder;

import java.io.IOException;
import java.io.OutputStream;

import static org.mockito.Mockito.*;

public class ObservableOutputStreamTest {

    private ObservableOutputStream observable;

    private OutputStreamObserver observer1;
    private OutputStreamObserver observer2;

    private OutputStream os;

    private Exception exception;

    
    @Nested
    class ObservableOutputStreamCases {
        @Nested
        class WhenTheOutputStreamHasListeners {
            @BeforeEach
            void setUp() throws Throwable {
                os = mock(OutputStream.class);
                observer1 = mock(OutputStreamObserver.class);
                observer2 = mock(OutputStreamObserver.class);

                observable = new ObservableOutputStream(os);
                observable.addObservers(observer1);
                observable.addObservers(observer2);

            }

            @Test
            void shouldReturnThem() throws Throwable {
                assertThat(observable.getObservers()).contains(observer1);

            }

        }

        @Nested
        class WhenTheOutputStreamIsWrittenTo {
            @BeforeEach
            void setUp() throws Throwable {
                os = mock(OutputStream.class);
                observer1 = mock(OutputStreamObserver.class);
                observer2 = mock(OutputStreamObserver.class);

                observable = new ObservableOutputStream(os);
                observable.addObservers(observer1);
                observable.addObservers(observer2);

                observable.write(32);

            }

            @Test
            void shouldDelegateToTheUnderlyingInputStream() throws Throwable {
                verify(os).write(32);

            }

        }

        @Nested
        class WhenTheOutputStreamIsClosed {
            @BeforeEach
            void setUp() throws Throwable {
                os = mock(OutputStream.class);
                observer1 = mock(OutputStreamObserver.class);
                observer2 = mock(OutputStreamObserver.class);

                observable = new ObservableOutputStream(os);
                observable.addObservers(observer1);
                observable.addObservers(observer2);

                observable.close();

            }

            @Test
            void shouldCallListenersOnClosedEventHandlerInOrder() throws Throwable {
                InOrder inOrder = inOrder(observer1, observer2);
                verify(observer1).closed();
                verify(observer2).closed();
                verifyNoMoreInteractions(observer1, observer2);

            }

        }

        @Nested
        class WhenTheOutputStreamIsClosedAndThrowsAnException {
            @BeforeEach
            void setUp() throws Throwable {
                os = mock(OutputStream.class);
                observer1 = mock(OutputStreamObserver.class);
                observer2 = mock(OutputStreamObserver.class);

                doThrow(new IOException("badness")).when(os).close();

                observable = new ObservableOutputStream(os);
                observable.addObservers(observer1);
                observable.addObservers(observer2);

                try {
                    observable.close();
                } catch (Exception e) {
                    exception = e;
                }

            }

            @Test
            void shouldCallListenersOnClosedEventHandlerAndThrowTheException() throws Throwable {
                verify(observer1).closed();
                assertThat(exception).isNotNull();

            }

        }

    }

}
