package org.springframework.content.fs.boot.autoconfigure;

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
import org.springframework.content.fs.config.EnableFileSystemStores;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.content.fs.store.FileSystemContentStore;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.support.TestEntity;

public class ContentFileSystemAutoConfigurationTest {

    private ApplicationContextRunner contextRunner;

    
    @Nested
    class FileSystemContentAutoConfigurationCases {
        @Nested
        class GivenADefaultConfiguration {
            @BeforeEach
            void setUp() throws Throwable {
                contextRunner = new ApplicationContextRunner()
                                    .withConfiguration(AutoConfigurations.of(FileSystemContentAutoConfiguration.class));
            }

            @Test
            void shouldLoadTheContext() throws Throwable {
                contextRunner.withUserConfiguration(TestConfig.class).run((context) -> Assertions.assertThat(context).hasSingleBean(TestEntityContentRepository.class));
            }

        }

        @Nested
        class GivenAnEnvironmentSpecifyingAFilesystemRootUsingSpringPrefix {
            @BeforeEach
            void setUp() throws Throwable {
                contextRunner = new ApplicationContextRunner()
                                    .withConfiguration(AutoConfigurations.of(FileSystemContentAutoConfiguration.class));
                System.setProperty("spring.content.fs.filesystem-root",
                                        "${java.io.tmpdir}/UPPERCASE/NOTATION/");
            }

            @AfterEach
            void tearDown() throws Throwable {
                System.clearProperty("spring.content.fs.filesystem-root");
            }

            @Test
            void shouldHaveAFilesystemPropertiesBeanWithTheCorrectRootSet() throws Throwable {
                contextRunner.withUserConfiguration(TestConfig.class).run((context) -> {
                                            Assertions.assertThat(context).hasSingleBean(FileSystemContentAutoConfiguration.FileSystemProperties.class);
                                            Assertions.assertThat(context).getBean(FileSystemContentAutoConfiguration.FileSystemProperties.class).extracting("fileSystemRoot").matches((val) -> val.toString().endsWith("/UPPERCASE/NOTATION/"));
                                        });
            }

        }

        @Nested
        class GivenAConfigurationThatContributesALoaderBean {
            @BeforeEach
            void setUp() throws Throwable {
                contextRunner = new ApplicationContextRunner()
                                    .withConfiguration(AutoConfigurations.of(FileSystemContentAutoConfiguration.class));
            }

            @Test
            void shouldHaveThatLoaderBeanInTheContext() throws Throwable {
                contextRunner.withUserConfiguration(ConfigWithLoaderBean.class).run((context) -> {
                                                Assertions.assertThat(context).hasSingleBean(FileSystemResourceLoader.class);
                                                Assertions.assertThat(context).getBean(FileSystemResourceLoader.class).extracting("root").matches((val) -> val.toString().contains("/some/random/path"));
                                            });
            }

        }

        @Nested
        class GivenAConfigurationWithExplicitEnableFileSystemStoresAnnotation {
            @BeforeEach
            void setUp() throws Throwable {
                contextRunner = new ApplicationContextRunner()
                                    .withConfiguration(AutoConfigurations.of(FileSystemContentAutoConfiguration.class));
            }

            @Test
            void shouldLoadTheContext() throws Throwable {
                contextRunner.withUserConfiguration(ConfigWithExplicitEnableFileSystemStores.class).run((context) -> {
                                        Assertions.assertThat(context).hasSingleBean(TestEntityContentRepository.class);
                                        Assertions.assertThat(context).getBean(FileSystemResourceLoader.class);
                                    });
            }

        }

    }

    @Disabled("This is not a test")
    @SpringBootApplication(exclude = {SolrAutoConfiguration.class, SolrExtensionAutoConfiguration.class, S3ContentAutoConfiguration.class})
    public static class TestConfig {
    }

    @Disabled("This is not a test")
    @SpringBootApplication
    public static class ConfigWithLoaderBean {

        @Bean
        FileSystemResourceLoader fileSystemResourceLoader() {
            return new FileSystemResourceLoader("/some/random/path/");
        }
    }

    @Disabled("This is not a test")
    @SpringBootApplication
    @EnableFileSystemStores
    public static class ConfigWithExplicitEnableFileSystemStores {
    }

    public interface TestEntityRepository extends JpaRepository<TestEntity, Long> {
    }

    public interface TestEntityContentRepository extends FileSystemContentStore<TestEntity, String> {
    }
}
