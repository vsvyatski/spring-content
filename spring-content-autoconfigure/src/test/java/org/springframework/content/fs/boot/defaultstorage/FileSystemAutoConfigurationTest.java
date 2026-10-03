package org.springframework.content.fs.boot.defaultstorage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Disabled;
import static org.assertj.core.api.Assertions.assertThat;

import internal.org.springframework.content.fs.boot.autoconfigure.FileSystemContentAutoConfiguration;
import internal.org.springframework.content.s3.boot.autoconfigure.S3ContentAutoConfiguration;
import internal.org.springframework.content.solr.boot.autoconfigure.SolrAutoConfiguration;
import internal.org.springframework.content.solr.boot.autoconfigure.SolrExtensionAutoConfiguration;
import org.assertj.core.api.Assertions;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.support.TestEntity;

public class FileSystemAutoConfigurationTest {

    private ApplicationContextRunner contextRunner;

    
    @Nested
    class FileSystemContentAutoConfigurationCases {
        @Nested
        class GivenADefaultStorageTypeOfFs {
            @BeforeEach
            void setUp() throws Throwable {
                contextRunner = new ApplicationContextRunner()
                                    .withConfiguration(AutoConfigurations.of(FileSystemContentAutoConfiguration.class));
                System.setProperty("spring.content.storage.type.default", "fs");
            }

            @AfterEach
            void tearDown() throws Throwable {
                System.clearProperty("spring.content.storage.type.default");
            }

            @Test
            void shouldCreateAnFileSystemResourceLoaderBean() throws Throwable {
                contextRunner
                                        .withUserConfiguration(TestConfig.class).run((context) ->
                                                Assertions.assertThat(context).hasSingleBean(FileSystemResourceLoader.class));
            }

        }

        @Nested
        class GivenADefaultStorageTypeOtherThanFs {
            @BeforeEach
            void setUp() throws Throwable {
                contextRunner = new ApplicationContextRunner()
                                    .withConfiguration(AutoConfigurations.of(FileSystemContentAutoConfiguration.class));
                System.setProperty("spring.content.storage.type.default", "s3");
            }

            @AfterEach
            void tearDown() throws Throwable {
                System.clearProperty("spring.content.storage.type.default");
            }

            @Test
            void shouldNotCreateAnFileSystemResourceLoaderBean() throws Throwable {
                contextRunner.withUserConfiguration(TestConfig.class).run((context) ->
                                                Assertions.assertThat(context).doesNotHaveBean(FileSystemResourceLoader.class));
            }

        }

        @Nested
        class GivenNoDefaultStorageType {
            @BeforeEach
            void setUp() throws Throwable {
                contextRunner = new ApplicationContextRunner()
                                    .withConfiguration(AutoConfigurations.of(FileSystemContentAutoConfiguration.class));
            }

            @Test
            void shouldCreateAnFileSystemResourceLoaderBean() throws Throwable {
                contextRunner.withUserConfiguration(TestConfig.class)
                                                    .run((context) -> Assertions.assertThat(context).hasSingleBean(FileSystemResourceLoader.class));
            }

        }

    }

    @Disabled("This is not a test")
    @SpringBootApplication(exclude = {SolrAutoConfiguration.class, SolrExtensionAutoConfiguration.class, S3ContentAutoConfiguration.class})
    public static class TestConfig {
    }

    public interface TestEntityRepository extends JpaRepository<TestEntity, Long> {
    }

    public interface TestEntityContentRepository extends ContentStore<TestEntity, String> {
    }
}
