package internal.org.springframework.content.fs.io;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.content.commons.utils.FileService;
import org.springframework.core.io.FileSystemResource;

import java.io.IOException;

import static org.mockito.Mockito.*;

public class FileSystemDeletableResourceTest {

    private FileSystemDeletableResource resource;

    private FileSystemResource delegate;

    @Nested
    class FileSystemDeletableResourceCases {
        @BeforeEach
        void setUp() {
            delegate = mock(FileSystemResource.class);
            FileService fileService = mock(FileService.class);

            resource = new FileSystemDeletableResource(delegate, fileService);
        }

        @Test
        void shouldDelegateIsOpen() {
            resource.isOpen();
            verify(delegate).isOpen();
        }

        @Test
        void shouldDelegateExists() {
            resource.exists();
            verify(delegate).exists();
        }

        @Test
        void shouldDelegateIsReadable() {
            resource.isReadable();
            verify(delegate).isReadable();
        }

        @Test
        void shouldDelegateGetInputStream() throws IOException {
            resource.getInputStream();
            verify(delegate).getInputStream();
        }

        @Test
        void shouldDelegateIsWritable() {
            resource.isWritable();
            verify(delegate).isWritable();
        }

        @Test
        void shouldDelegateGetOutputStream() throws IOException {
            when(delegate.exists()).thenReturn(true);
            resource.getOutputStream();
            verify(delegate).getOutputStream();
        }

        @Test
        void shouldDelegateGetURL() throws IOException {
            resource.getURL();
            verify(delegate).getURL();
        }

        @Test
        void shouldDelegateGetURI() throws IOException {
            resource.getURI();
            verify(delegate).getURI();
        }

        @Test
        void shouldDelegateGetFile() {
            resource.getFile();
            verify(delegate).getFile();
        }

        @Test
        void shouldDelegateContentLength() throws IOException {
            resource.contentLength();
            verify(delegate).contentLength();
        }

        @Test
        void shouldDelegateCreateRelative() {
            resource.createRelative("some-path");
            verify(delegate).createRelative("some-path");
        }

        @Test
        void shouldDelegateGetFilename() {
            resource.getFilename();
            verify(delegate).getFilename();
        }

        @Test
        void shouldDelegateGetDescription() {
            resource.getDescription();
            verify(delegate).getDescription();
        }
    }
}
