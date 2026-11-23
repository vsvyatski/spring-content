package org.springframework.content.commons.io;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class FileRemoverTest {

    private File file;

    @Nested
    class FileRemoverCases {
        @Nested
        @DisplayName("When a file input stream observer s closed is called")
        class FileInputStreamObserverSClosedIsCalled {
            @BeforeEach
            void setUp() {
                file = mock(File.class);
                FileRemover observer = new FileRemover(file);

                observer.closed();
            }

            @Test
            void shouldDeleteTheUnderlyingFile() {
                verify(file).delete();
            }
        }
    }
}
