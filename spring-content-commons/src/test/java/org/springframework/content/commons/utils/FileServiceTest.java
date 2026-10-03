package org.springframework.content.commons.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileServiceTest {

	private FileService fileService;
	private File file;
	private File parent;
	private Exception ex;

	
    @Nested
    class Mkdirs {
        @Nested
        class WhenPassedInAFileThatExists {
            @BeforeEach
            void setUp() throws IOException {
                parent = Files.createTempDirectory("commons-").toFile();
                file = new File(parent, "something.txt");
                FileUtils.touch(file);
                assertThat(file.exists()).isTrue();

                fileService = new FileServiceImpl();
                try {
                	fileService.mkdirs(file);
                }
                catch (Exception e) {
                	ex = e;
                }

            }

            @AfterEach
            void tearDown() {
                file.delete();
            }

            @Test
            void shouldThrowAnIOException() {
                assertThat(ex).isNotNull();
                assertThat(ex).isInstanceOf(IOException.class);

            }

        }

        @Nested
        class WhenPassedInAFileThatDoesNotExist {
            @BeforeEach
            void setUp() throws IOException {
                parent = Files.createTempDirectory("commons-").toFile();
                file = new File(parent, "something.txt");
                assertThat(file.exists()).isFalse();

                fileService = new FileServiceImpl();
                try {
                	fileService.mkdirs(file);
                }
                catch (Exception e) {
                	ex = e;
                }

            }

            @AfterEach
            void tearDown() {
                file.delete();
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
        class WhenPassedInADirectoryThatExists {
            @BeforeEach
            void setUp() throws IOException {
                parent = Files.createTempDirectory("commons-").toFile();
                file = new File(parent, "something");
                file.mkdirs();
                assertThat(file.exists()).isTrue();

                fileService = new FileServiceImpl();
                try {
                	fileService.mkdirs(file);
                }
                catch (Exception e) {
                	ex = e;
                }

            }

            @AfterEach
            void tearDown() {
                file.delete();
            }

            @Test
            void shouldSucceed() {
                assertThat(ex).isNull();
                assertThat(file.exists()).isTrue();
                assertThat(file.isDirectory()).isTrue();

            }

        }

        @Nested
        class WhenPassedInADirectoryThatDoesNotExist {
            @BeforeEach
            void setUp() throws IOException {
                parent = Files.createTempDirectory("commons-").toFile();
                file = new File(parent, "something");
                assertThat(file.exists()).isFalse();

                fileService = new FileServiceImpl();
                try {
                	fileService.mkdirs(file);
                }
                catch (Exception e) {
                	ex = e;
                }

            }

            @AfterEach
            void tearDown() {
                file.delete();
            }

            @Test
            void shouldSucceed() {
                assertThat(ex).isNull();
                assertThat(file.exists()).isTrue();
                assertThat(file.isDirectory()).isTrue();

            }

        }

        @Nested
        class WhenPassedNull {
            @BeforeEach
            void setUp() throws IOException {
                parent = Files.createTempDirectory("commons-").toFile();
                file = null;
                fileService = new FileServiceImpl();
                try {
                	fileService.mkdirs(file);
                }
                catch (Exception e) {
                	ex = e;
                }

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

            try {
            	fileService.rmdirs(tempFile.toFile(), null);
            	fail("unexpected");
            } catch (IOException e) {
            	assertThat(e).isNotNull();
            }

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
