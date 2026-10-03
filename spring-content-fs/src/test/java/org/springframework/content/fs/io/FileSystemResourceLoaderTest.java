package org.springframework.content.fs.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import static org.assertj.core.api.Assertions.assertThat;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.jspecify.annotations.NonNull;
import org.springframework.content.commons.io.DeletableResource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class FileSystemResourceLoaderTest {

    private FileSystemResourceLoader loader = null;

    private String path;

    private String location;

    private File parent;
    private File file;

    private Exception ex;

    
    @Nested
    class FileSystemResourceLoaderCases {
        @Nested
        class GetResource {
            @Nested
            class GivenWellFormedPathHasATrailingSlash {
                @BeforeEach
                void setUp() {
                    path = getPathWithProperSeparators("/some/well-formed/path/");
                    try {
                        loader = new FileSystemResourceLoader(path);
                    } catch (Exception e) {
                        ex = e;
                    }
                }

                @Test
                void succeeds() throws IOException {
                    assertThat(ex).isNull();
                    final String expected = getPathWithProperSeparators("/some/well-formed/path/something");
                    assertThat(loader.getResource("/something").getFile().getPath()).isEqualTo(expected);
                    assertThat(loader.getResource("/something")).isInstanceOf(DeletableResource.class);
                }

            }

            @Nested
            class GivenMalformedPathWithoutATrailingSlash {
                @BeforeEach
                void setUp() {
                    path = getPathWithProperSeparators("/some/malformed/path");
                    try {
                        loader = new FileSystemResourceLoader(path);
                    } catch (Exception e) {
                        ex = e;
                    }
                }

                @Test
                void succeeds() throws IOException {
                    assertThat(ex).isNull();
                    final String expected = getPathWithProperSeparators("/some/malformed/path/something");
                    assertThat(loader.getResource("/something").getFile().getPath()).isEqualTo(expected);
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
            class GivenAFileResourceThatExists {
                @BeforeEach
                void setUp() throws IOException {
                    parent = Files.createTempDirectory("fs-").toFile();
                    location = "FileSystemResourceLoaderTest.tmp";
                    file = new File(parent, location);
                    FileUtils.touch(file);
                    assertThat(file.exists()).isTrue();

                    loader = new FileSystemResourceLoader(parent.getPath() + "/");
                    Resource resource = loader.getResource(location);
                    assertThat(resource).isInstanceOf(DeletableResource.class);
                    ((DeletableResource) resource).delete();
                }

                @Test
                void shouldDeleteTheUnderlyingFile() {
                    assertThat(file.exists()).isFalse();
                }

            }

        }

    }

    private String getPathWithProperSeparators(@NonNull String path) {
        if (SystemUtils.IS_OS_WINDOWS) {
            return path.replace('/', '\\');
        }
        return path;
    }
}
