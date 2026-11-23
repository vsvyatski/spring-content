package org.springframework.content.fs.io;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.springframework.content.commons.io.DeletableResource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;

public class FileSystemResourceLoaderTest {

    private FileSystemResourceLoader loader = null;

    private File file;

    @Nested
    class FileSystemResourceLoaderCases {
        @Nested
        class GetResource {
            @Nested
            @DisplayName("Given well formed path has a trailing slash")
            class WellFormedPathHasTrailingSlash {
                @Test
                @DisabledOnOs(OS.WINDOWS)
                void succeeds() throws IOException {
                    loader = new FileSystemResourceLoader("/some/well-formed/path/");
                    assertThat(loader.getResource("/something").getFile().getPath()).isEqualTo("/some/well-formed/path/something");
                    assertThat(loader.getResource("/something")).isInstanceOf(DeletableResource.class);
                }

                @Test
                @EnabledOnOs(OS.WINDOWS)
                void succeedsOnWindows() throws IOException {
                    loader = new FileSystemResourceLoader("C:\\some\\well-formed\\path\\");
                    assertThat(loader.getResource("/something").getFile().getPath()).endsWith("\\some\\well-formed\\path\\something");
                    assertThat(loader.getResource("/something")).isInstanceOf(DeletableResource.class);
                }
            }

            @Nested
            @DisplayName("Given malformed path without a trailing slash")
            class MalformedPathWithoutTrailingSlash {
                @Test
                @DisabledOnOs(OS.WINDOWS)
                void succeeds() throws IOException {
                    loader = new FileSystemResourceLoader("/some/malformed/path");
                    assertThat(loader.getResource("/something").getFile().getPath()).isEqualTo("/some/malformed/path/something");
                    assertThat(loader.getResource("/something")).isInstanceOf(DeletableResource.class);
                }

                @Test
                @EnabledOnOs(OS.WINDOWS)
                void succeedsOnWindows() throws IOException {
                    loader = new FileSystemResourceLoader("C:\\some\\malformed\\path");
                    assertThat(loader.getResource("/something").getFile().getPath()).endsWith("\\some\\malformed\\path\\something");
                    assertThat(loader.getResource("/something")).isInstanceOf(DeletableResource.class);
                }
            }
        }
    }

    @Nested
    class DeletableResourceCases {
        @Nested
        class Delete {
            @Nested
            @DisplayName("Given a file resource that exists")
            class FileResourceThatExists {
                @BeforeEach
                void setUp() throws IOException {
                    File parent = Files.createTempDirectory("fs-").toFile();
                    String location = "FileSystemResourceLoaderTest.tmp";
                    file = new File(parent, location);
                    FileUtils.touch(file);
                    assertThat(file.exists()).isTrue();

                    loader = new FileSystemResourceLoader(parent.getPath() + "/");
                    Resource resource = loader.getResource(location);
                    assertThat(resource).isInstanceOf(DeletableResource.class);
                    ((DeletableResource) resource).delete();
                }

                @Test
                void shouldDeleteUnderlyingFile() {
                    assertThat(file.exists()).isFalse();
                }
            }
        }
    }
}
