package org.springframework.content.fs.boot.autoconfigure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.assertj.core.api.Assertions.assertThat;

import internal.org.springframework.content.fs.boot.autoconfigure.FileSystemContentAutoConfiguration;

public class FileSystemPropertiesTest {

    private FileSystemContentAutoConfiguration.FileSystemProperties props;

    @Nested
    class FileSystemPropertiesCases {
        @Nested
        class GivenAFilesystemPropertiesWithNoRootSet {
            @BeforeEach
            void setUp() {
                props = new FileSystemContentAutoConfiguration.FileSystemProperties();
            }

            @Test
            void shouldReturnAJavaIOTmpDirBasedDefault() {
                assertThat(props.getFileSystemRoot()).startsWith(System.getProperty("java.io.tmpdir"));
            }
        }

        @Nested
        class GivenAFilesystemPropertiesWithRootSet {
            @BeforeEach
            void setUp() {
                props = new FileSystemContentAutoConfiguration.FileSystemProperties();
            }

            @Test
            @EnabledOnOs(OS.WINDOWS)
            void shouldReturnTheConfiguredRootOnWindows() {
                props.setFileSystemRoot("C:\\some\\random\\path");
                assertThat(props.getFileSystemRoot()).isEqualTo("C:\\some\\random\\path");
            }

            @Test
            @DisabledOnOs(OS.WINDOWS)
            void shouldReturnTheConfiguredRoot() {
                props.setFileSystemRoot("/some/random/path");
                assertThat(props.getFileSystemRoot()).isEqualTo("/some/random/path");
            }
        }
    }
}
