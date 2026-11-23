package org.springframework.content.fs.boot.defaultstorage;

import internal.org.springframework.content.fs.boot.autoconfigure.FileSystemContentAutoConfiguration;
import internal.org.springframework.content.s3.boot.autoconfigure.S3ContentAutoConfiguration;
import internal.org.springframework.content.solr.boot.autoconfigure.SolrAutoConfiguration;
import internal.org.springframework.content.solr.boot.autoconfigure.SolrExtensionAutoConfiguration;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.content.fs.io.FileSystemResourceLoader;

public class FileSystemAutoConfigurationTest {

    private ApplicationContextRunner contextRunner;

    @SpringBootApplication(exclude = {SolrAutoConfiguration.class, SolrExtensionAutoConfiguration.class, S3ContentAutoConfiguration.class})
    public static class TestConfig {
    }

    @Nested
    @DisplayName("Given a default storage type of FS")
    class DefaultStorageTypeOfFs {
        @BeforeEach
        void setUp() {
            contextRunner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(FileSystemContentAutoConfiguration.class));
            System.setProperty("spring.content.storage.type.default", "fs");
        }

        @AfterEach
        void tearDown() {
            System.clearProperty("spring.content.storage.type.default");
        }

        @Test
        void shouldCreateFileSystemResourceLoaderBean() {
            contextRunner
                .withUserConfiguration(TestConfig.class).run((context) ->
                    Assertions.assertThat(context).hasSingleBean(FileSystemResourceLoader.class));
        }
    }

    @Nested
    @DisplayName("Given a default storage type other than FS")
    class DefaultStorageTypeOtherThanFs {
        @BeforeEach
        void setUp() {
            contextRunner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(FileSystemContentAutoConfiguration.class));
            System.setProperty("spring.content.storage.type.default", "s3");
        }

        @AfterEach
        void tearDown() {
            System.clearProperty("spring.content.storage.type.default");
        }

        @Test
        void shouldNotCreateFileSystemResourceLoaderBean() {
            contextRunner.withUserConfiguration(TestConfig.class).run((context) ->
                Assertions.assertThat(context).doesNotHaveBean(FileSystemResourceLoader.class));
        }
    }

    @Nested
    @DisplayName("Given no default storage type")
    class NoDefaultStorageType {
        @BeforeEach
        void setUp() {
            contextRunner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(FileSystemContentAutoConfiguration.class));
        }

        @Test
        void shouldCreateFileSystemResourceLoaderBean() {
            contextRunner.withUserConfiguration(TestConfig.class)
                .run((context) -> Assertions.assertThat(context).hasSingleBean(FileSystemResourceLoader.class));
        }
    }
}