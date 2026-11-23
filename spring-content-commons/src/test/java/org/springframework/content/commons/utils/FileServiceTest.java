package org.springframework.content.commons.utils;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class FileServiceTest {

    private FileService fileService;
    private File file;
    private File parent;
    private Exception ex;

    private void deleteFixture() {
        if (parent != null) {
            FileUtils.deleteQuietly(parent);
        }
    }

    @SuppressWarnings("OverlyBroadCatchBlock")
    private void mkdirsAndCaptureException() {
        ex = null;
        try {
            fileService.mkdirs(file);
        } catch (Exception caught) {
            ex = caught;
        }
    }

    @Nested
   class Mkdirs {
        @Nested
        @DisplayName("When passed in a file that exists")
        class PassedInFileThatExists {
            @BeforeEach
            void setUp() throws IOException {
                parent = Files.createTempDirectory("commons-").toFile();
                file = new File(parent, "something.txt");
                FileUtils.touch(file);
                assertThat(file.exists()).isTrue();

                fileService = new FileServiceImpl();
                mkdirsAndCaptureException();
            }

            @AfterEach
            void tearDown() {
                deleteFixture();
            }

            @Test
            void shouldThrowAnIOException() {
                assertThat(ex).isNotNull();
                assertThat(ex).isInstanceOf(IOException.class);
            }
        }

        @Nested

        @DisplayName("When passed in a file that does not exist")

        class PassedInFileThatDoesNotExist {
            @BeforeEach
            void setUp() throws IOException {
                parent = Files.createTempDirectory("commons-").toFile();
                file = new File(parent, "something.txt");
                assertThat(file.exists()).isFalse();

                fileService = new FileServiceImpl();
                mkdirsAndCaptureException();
            }

            @AfterEach
            void tearDown() {
                deleteFixture();
            }

            @Test
            void shouldNotThrowAnException() {
                assertThat(ex).isNull();
            }

            @Test
            void shouldCreateTheDirectory() {
                assertThat(file.isDirectory()).isTrue();
                assertThat(file.exists()).isTrue();
            }
        }

        @Nested

        @DisplayName("When passed in a directory that exists")

        class PassedInDirectoryThatExists {
            @BeforeEach
            void setUp() throws IOException {
                parent = Files.createTempDirectory("commons-").toFile();
                file = new File(parent, "something");
                if (!file.mkdirs() && !file.isDirectory()) {
                    throw new IllegalStateException("Failed to create " + file);
                }
                assertThat(file.exists()).isTrue();

                fileService = new FileServiceImpl();
                mkdirsAndCaptureException();
            }

            @AfterEach
            void tearDown() {
                deleteFixture();
            }

            @Test
            void shouldSucceed() {
                assertThat(ex).isNull();
                assertThat(file.exists()).isTrue();
                assertThat(file.isDirectory()).isTrue();
            }
        }

        @Nested

        @DisplayName("When passed in a directory that does not exist")

        class PassedInDirectoryThatDoesNotExist {
            @BeforeEach
            void setUp() throws IOException {
                parent = Files.createTempDirectory("commons-").toFile();
                file = new File(parent, "something");
                assertThat(file.exists()).isFalse();

                fileService = new FileServiceImpl();
                mkdirsAndCaptureException();
            }

            @AfterEach
            void tearDown() {
                deleteFixture();
            }

            @Test
            void shouldSucceed() {
                assertThat(ex).isNull();
                assertThat(file.exists()).isTrue();
                assertThat(file.isDirectory()).isTrue();
            }
        }

        @Nested

        @DisplayName("When passed null")

        class PassedNull {
            @BeforeEach
            void setUp() throws IOException {
                parent = Files.createTempDirectory("commons-").toFile();
                file = null;
                fileService = new FileServiceImpl();
                mkdirsAndCaptureException();
            }

            @Test
            void shouldThrowAnIllegalArgumentException() {
                assertThat(ex).isNotNull();
                assertThat(ex).isInstanceOf(IllegalArgumentException.class);
            }
        }
    }

    @Nested
   class Rmdirs {
        @BeforeEach
        void setUp() {
            fileService = new FileServiceImpl();
        }

        @Test
        void shouldDeleteEmptyDirectoriesButStopAtTo() throws IOException {
            Path p0 = Files.createTempDirectory(null);
            Path p1 = Files.createTempDirectory(p0, null);
            Path p2 = Files.createTempDirectory(p1, null);

            fileService.rmdirs(p2.toFile(), p0.toFile());

            assertThat(p2.toFile().exists()).isFalse();
            assertThat(p1.toFile().exists()).isFalse();
            assertThat(p0.toFile().exists()).isTrue();
        }

        @Test
        void shouldRejectFiles() throws IOException {
            Path tempFile = Files.createTempFile(null, null);

            assertThatThrownBy(() -> fileService.rmdirs(tempFile.toFile(), null))
                .isInstanceOf(IOException.class);
        }

        @Test
        void shouldLeaveDirectoriesThatAreNotEmpty() throws IOException {
            Path p0 = Files.createTempDirectory(null);
            Path p1 = Files.createTempDirectory(p0, null);
            Path f1 = Files.createTempFile(p1, null, null);
            Path p2 = Files.createTempDirectory(p1, null);

            fileService.rmdirs(p2.toFile(), p0.toFile());

            assertThat(p2.toFile().exists()).isFalse();
            assertThat(p1.toFile().exists()).isTrue();
            assertThat(f1.toFile().exists()).isTrue();
            assertThat(p0.toFile().exists()).isTrue();
        }

        @Test
        void shouldDoNothingWhenFromAndToAreTheSame() throws IOException {
            Path p0 = Files.createTempDirectory(null);

            fileService.rmdirs(p0.toFile(), p0.toFile());

            assertThat(p0.toFile().exists()).isTrue();
        }
    }
}
