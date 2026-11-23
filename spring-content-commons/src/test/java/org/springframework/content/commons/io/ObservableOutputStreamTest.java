package org.springframework.content.commons.io;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.io.IOException;
import java.io.OutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.*;

public class ObservableOutputStreamTest {

    private ObservableOutputStream observable;

    private OutputStreamObserver observer1;
    private OutputStreamObserver observer2;

    private OutputStream os;

    private Throwable exception;

    @Nested
   class ObservableOutputStreamCases {
        @Nested
        @DisplayName("When the output stream has listeners")
        class OutputStreamHasListeners {
            @BeforeEach
            void setUp() {
                os = mock(OutputStream.class);
                observer1 = mock(OutputStreamObserver.class);
                observer2 = mock(OutputStreamObserver.class);

                observable = new ObservableOutputStream(os);
                observable.addObservers(observer1);
                observable.addObservers(observer2);
            }

            @Test
            void shouldReturnThem() {
                assertThat(observable.getObservers()).contains(observer1);
            }
        }

        @Nested

        @DisplayName("When the output stream is written to")

        class OutputStreamIsWrittenTo {
            @BeforeEach
            void setUp() throws IOException {
                os = mock(OutputStream.class);
                observer1 = mock(OutputStreamObserver.class);
                observer2 = mock(OutputStreamObserver.class);

                observable = new ObservableOutputStream(os);
                observable.addObservers(observer1);
                observable.addObservers(observer2);

                observable.write(32);
            }

            @Test
            void shouldDelegateToTheUnderlyingInputStream() throws IOException {
                verify(os).write(32);
            }
        }

        @Nested

        @DisplayName("When the output stream is closed")

        class OutputStreamIsClosed {
            @BeforeEach
            void setUp() throws IOException {
                os = mock(OutputStream.class);
                observer1 = mock(OutputStreamObserver.class);
                observer2 = mock(OutputStreamObserver.class);

                observable = new ObservableOutputStream(os);
                observable.addObservers(observer1);
                observable.addObservers(observer2);

                observable.close();
            }

            @Test
            void shouldCallListenersOnClosedEventHandlerInOrder() {
                InOrder inOrder = inOrder(observer1, observer2);
                inOrder.verify(observer1).closed();
                inOrder.verify(observer2).closed();
                verifyNoMoreInteractions(observer1, observer2);
            }
        }

        @Nested

        @DisplayName("When the output stream is closed and throws an exception")

        class OutputStreamIsClosedAndThrowsAnException {
            @BeforeEach
            void setUp() throws IOException {
                os = mock(OutputStream.class);
                observer1 = mock(OutputStreamObserver.class);
                observer2 = mock(OutputStreamObserver.class);

                doThrow(new IOException("badness")).when(os).close();

                observable = new ObservableOutputStream(os);
                observable.addObservers(observer1);
                observable.addObservers(observer2);

                exception = catchThrowable(observable::close);
            }

            @Test
            void shouldCallListenersOnClosedEventHandlerAndThrowTheException() {
                verify(observer1).closed();
                assertThat(exception).isNotNull();
            }
        }
    }
}
