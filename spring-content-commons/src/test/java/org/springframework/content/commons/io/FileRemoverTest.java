package org.springframework.content.commons.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;

import java.io.File;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class FileRemoverTest {

    private File file;

    private FileRemover observer;

    
    @Nested
    class FileRemoverCases {
        @Nested
        class WhenAFileInputStreamObserverSClosedIsCalled {
            @BeforeEach
            void setUp() {
                file = mock(File.class);
                observer = new FileRemover(file);

                observer.closed();
            }

            @Test
            void shouldDeleteTheUnderlyingFile() {
                verify(file).delete();
            }

        }

    }

}
