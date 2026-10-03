package internal.org.springframework.content.fs.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;

import org.springframework.content.commons.utils.FileService;
import org.springframework.core.io.FileSystemResource;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FileSystemDeletableResourceTest {

    private FileSystemDeletableResource resource;

    private FileSystemResource delegate;
    private FileService fileService;

    
    @Nested
    class FileSystemDeletableResourceCases {
        @BeforeEach
        void setUp() throws Throwable {
            delegate = mock(FileSystemResource.class);
            fileService = mock(FileService.class);

            resource = new FileSystemDeletableResource(delegate, fileService);
        }

        @Test
        void shouldDelegateIsOpen() throws Throwable {
            resource.isOpen();
            verify(delegate).isOpen();

        }

        @Test
        void shouldDelegateExists() throws Throwable {
            resource.exists();
            verify(delegate).exists();

        }

        @Test
        void shouldDelegateIsReadable() throws Throwable {
            resource.isReadable();
            verify(delegate).isReadable();

        }

        @Test
        void shouldDelegateGetInputStream() throws Throwable {
            resource.getInputStream();
            verify(delegate).getInputStream();

        }

        @Test
        void shouldDelegateIsWritable() throws Throwable {
            resource.isWritable();
            verify(delegate).isWritable();

        }

        @Test
        void shouldDelegateGetOutputStream() throws Throwable {
            when(delegate.exists()).thenReturn(true);
            resource.getOutputStream();
            verify(delegate).getOutputStream();

        }

        @Test
        void shouldDelegateGetURL() throws Throwable {
            resource.getURL();
            verify(delegate).getURL();

        }

        @Test
        void shouldDelegateGetURI() throws Throwable {
            resource.getURI();
            verify(delegate).getURI();

        }

        @Test
        void shouldDelegateGetFile() throws Throwable {
            resource.getFile();
            verify(delegate).getFile();

        }

        @Test
        void shouldDelegateContentLength() throws Throwable {
            resource.contentLength();
            verify(delegate).contentLength();

        }

        @Test
        void shouldDelegateCreateRelative() throws Throwable {
            resource.createRelative("some-path");
            verify(delegate).createRelative("some-path");

        }

        @Test
        void shouldDelegateGetFilename() throws Throwable {
            resource.getFilename();
            verify(delegate).getFilename();

        }

        @Test
        void shouldDelegateGetDescription() throws Throwable {
            resource.getDescription();
            verify(delegate).getDescription();

        }

    }

}
