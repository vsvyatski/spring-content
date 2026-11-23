package org.springframework.content.fs.boot.autoconfigure;

import internal.org.springframework.content.fs.boot.autoconfigure.FileSystemContentAutoConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.assertj.core.api.Assertions.assertThat;

public class FileSystemPropertiesTest {

    private FileSystemContentAutoConfiguration.FileSystemProperties props;

    @Nested
    @DisplayName("Given file system properties with no root set")
    class FilesystemPropertiesWithNoRootSet {
        @BeforeEach
        void setUp() {
            props = new FileSystemContentAutoConfiguration.FileSystemProperties();
        }

        @Test
        void shouldReturnJavaIOTmpDirBasedDefault() {
            assertThat(props.getFileSystemRoot()).startsWith(System.getProperty("java.io.tmpdir"));
        }
    }

    @Nested
    @DisplayName("Given file system properties with root set")
    class FilesystemPropertiesWithRootSet {
        @BeforeEach
        void setUp() {
            props = new FileSystemContentAutoConfiguration.FileSystemProperties();
        }

        @Test
        @EnabledOnOs(OS.WINDOWS)
        void shouldReturnConfiguredRootOnWindows() {
            props.setFileSystemRoot("C:\\some\\random\\path");
            assertThat(props.getFileSystemRoot()).isEqualTo("C:\\some\\random\\path");
        }

        @Test
        @DisabledOnOs(OS.WINDOWS)
        void shouldReturnConfiguredRoot() {
            props.setFileSystemRoot("/some/random/path");
            assertThat(props.getFileSystemRoot()).isEqualTo("/some/random/path");
        }
    }
}