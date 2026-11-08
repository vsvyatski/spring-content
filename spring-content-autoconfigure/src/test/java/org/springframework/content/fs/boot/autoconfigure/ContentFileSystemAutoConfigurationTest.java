package org.springframework.content.fs.boot.autoconfigure;

import internal.org.springframework.content.fs.boot.autoconfigure.FileSystemContentAutoConfiguration;
import internal.org.springframework.content.s3.boot.autoconfigure.S3ContentAutoConfiguration;
import internal.org.springframework.content.solr.boot.autoconfigure.SolrAutoConfiguration;
import internal.org.springframework.content.solr.boot.autoconfigure.SolrExtensionAutoConfiguration;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.content.fs.config.EnableFileSystemStores;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.content.fs.store.FileSystemContentStore;
import org.springframework.context.annotation.Bean;
import org.springframework.support.TestEntity;

public class ContentFileSystemAutoConfigurationTest {

    private ApplicationContextRunner contextRunner;

    @Nested
    class FileSystemContentAutoConfigurationCases {
        @Nested
        class GivenADefaultConfiguration {
            @BeforeEach
            void setUp() {
                contextRunner = new ApplicationContextRunner()
                        .withConfiguration(AutoConfigurations.of(FileSystemContentAutoConfiguration.class));
            }

            @Test
            void shouldLoadTheContext() {
                contextRunner.withUserConfiguration(TestConfig.class).run((context) -> Assertions.assertThat(context).hasSingleBean(TestEntityContentRepository.class));
            }

        }

        @Nested
        class GivenAnEnvironmentSpecifyingAFilesystemRootUsingSpringPrefix {
            @BeforeEach
            void setUp() {
                contextRunner = new ApplicationContextRunner()
                        .withConfiguration(AutoConfigurations.of(FileSystemContentAutoConfiguration.class));
                System.setProperty("spring.content.fs.filesystem-root", "${java.io.tmpdir}/UPPERCASE/NOTATION/");
            }

            @AfterEach
            void tearDown() {
                System.clearProperty("spring.content.fs.filesystem-root");
            }

            @Test
            void shouldHaveAFilesystemPropertiesBeanWithTheCorrectRootSet() {
                contextRunner.withUserConfiguration(TestConfig.class).run((context) -> {
                    Assertions.assertThat(context).hasSingleBean(FileSystemContentAutoConfiguration.FileSystemProperties.class);
                    Assertions.assertThat(context).getBean(FileSystemContentAutoConfiguration.FileSystemProperties.class).extracting("fileSystemRoot").matches((val) -> val.toString().endsWith("/UPPERCASE/NOTATION/"));
                });
            }

        }

        @Nested
        class GivenAConfigurationThatContributesALoaderBean {
            @BeforeEach
            void setUp() {
                contextRunner = new ApplicationContextRunner()
                        .withConfiguration(AutoConfigurations.of(FileSystemContentAutoConfiguration.class));
            }

            @Test
            @DisabledOnOs(OS.WINDOWS)
            void shouldHaveThatLoaderBeanInTheContext() {
                contextRunner.withUserConfiguration(ConfigWithLoaderBean.class).run((context) -> {
                    Assertions.assertThat(context).hasSingleBean(FileSystemResourceLoader.class);
                    Assertions.assertThat(context).getBean(FileSystemResourceLoader.class).extracting("root").matches((val) -> val.toString().contains("/some/random/path"));
                });
            }

            @Test
            @EnabledOnOs(OS.WINDOWS)
            void shouldHaveThatLoaderBeanInTheContextOnWindows() {
                contextRunner.withUserConfiguration(ConfigWithLoaderBean.class).run((context) -> {
                    Assertions.assertThat(context).hasSingleBean(FileSystemResourceLoader.class);
                    Assertions.assertThat(context).getBean(FileSystemResourceLoader.class).extracting("root").matches((val) -> val.toString().matches("(?s).*([A-Za-z]:)?\\\\some\\\\random\\\\path.*"));
                });
            }

        }

        @Nested
        class GivenAConfigurationWithExplicitEnableFileSystemStoresAnnotation {
            @BeforeEach
            void setUp() {
                contextRunner = new ApplicationContextRunner()
                        .withConfiguration(AutoConfigurations.of(FileSystemContentAutoConfiguration.class));
            }

            @Test
            void shouldLoadTheContext() {
                contextRunner.withUserConfiguration(ConfigWithExplicitEnableFileSystemStores.class).run((context) -> {
                    Assertions.assertThat(context).hasSingleBean(TestEntityContentRepository.class);
                    Assertions.assertThat(context).hasSingleBean(FileSystemResourceLoader.class);
                });
            }
        }
    }

    @SpringBootApplication(exclude = {SolrAutoConfiguration.class, SolrExtensionAutoConfiguration.class, S3ContentAutoConfiguration.class})
    public static class TestConfig {
    }

    @SpringBootApplication
    public static class ConfigWithLoaderBean {

        @Bean
        FileSystemResourceLoader contributedFileSystemResourceLoader() {
            return new FileSystemResourceLoader("/some/random/path/");
        }
    }

    @SpringBootApplication
    @EnableFileSystemStores
    public static class ConfigWithExplicitEnableFileSystemStores {
    }

    public interface TestEntityContentRepository extends FileSystemContentStore<TestEntity, String> {
    }
}
