package org.springframework.content.fs.boot.autoconfigure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import static org.assertj.core.api.Assertions.assertThat;

import internal.org.springframework.content.fs.boot.autoconfigure.FileSystemContentAutoConfiguration;
import org.apache.commons.lang3.SystemUtils;

public class FileSystemPropertiesTest {

    private FileSystemContentAutoConfiguration.FileSystemProperties props;
    private String someRandomPath;

    
    @Nested
    class FileSystemPropertiesCases {
        @Nested
        class GivenAFilesystemPropertiesWithNoRootSet {
            @BeforeEach
            void setUp() {
                props = new FileSystemContentAutoConfiguration.FileSystemProperties();
            }

            @Test
            void shouldReturnAJAVAIOTMPDIRBasedDefault() {
                assertThat(props.getFileSystemRoot()).startsWith(System.getProperty("java.io.tmpdir"));
            }

        }

        @Nested
        class GivenAFilesystemPropertiesWithRootSet {
            @BeforeEach
            void setUp() {
                someRandomPath = SystemUtils.IS_OS_WINDOWS ?
                                        "C:\\some\\random\\path" : "/some/random/path";
                props = new FileSystemContentAutoConfiguration.FileSystemProperties();
                props.setFileSystemRoot(someRandomPath);
            }

            @Test
            void shouldReturnAJAVAIOTMPDIRBasedDefault() {
                assertThat(props.getFileSystemRoot()).isEqualTo(someRandomPath);
            }

        }

    }

}
