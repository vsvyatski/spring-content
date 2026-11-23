package internal.org.springframework.content.fs.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.content.fs.config.EnableFileSystemStores;
import org.springframework.content.fs.config.FileSystemStoreConfigurer;
import org.springframework.content.fs.config.FileSystemStoreConverter;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.content.test.support.TestSupportPackages;
import org.springframework.content.test.support.fs.SingleContentFsStore;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class EnableFileSystemStoresTest {

    private FileSystemStoreConfigurer configurer;
    private AnnotationConfigApplicationContext context;

    @Configuration
    @EnableFileSystemStores(basePackages = "contains.no.fs.repositories")
    @PropertySource("classpath:/test.properties")
    public static class EmptyConfig {
    }

    @Configuration
    @EnableFileSystemStores(basePackages = TestSupportPackages.FS)
    @PropertySource("classpath:/test.properties")
    public static class TestConfig {

        @Value("${spring.content.fs.filesystemRoot:#{null}}")
        private String filesystemRoot;

        @Bean
        FileSystemResourceLoader fileSystemResourceLoader() {
            return new FileSystemResourceLoader(filesystemRoot);
        }
    }

    @Configuration
    @EnableFileSystemStores(basePackages = TestSupportPackages.FS)
    @PropertySource("classpath:/test.properties")
    public static class ConverterConfig {

        @Value("${spring.content.fs.filesystemRoot:#{null}}")
        private String filesystemRoot;

        @Bean
        public FileSystemStoreConverter<UUID, String> uuidConverter() {
            return source -> String.format("/%s", source.toString().replace("-", "/"));
        }

        @Bean
        public FileSystemStoreConfigurer configurer() {
            return mock(FileSystemStoreConfigurer.class);
        }

        @Bean
        FileSystemResourceLoader fileSystemResourceLoader() {
            return new FileSystemResourceLoader(filesystemRoot);
        }
    }

    @Nested
    class EnableFileSystemStoresCases {
        @Nested
        @DisplayName("Given a context and a configuration with a filesystem content store bean")
        class ContextAndConfigurationWithFilesystemContentStoreBean {
            @BeforeEach
            void setUp() {
                context = new AnnotationConfigApplicationContext();
                context.register(TestConfig.class);
                context.refresh();
            }

            @AfterEach
            void tearDown() {
                context.close();
            }

            @Test
            void shouldHaveContentStoreBean() {
                assertThat(context.getBean(SingleContentFsStore.class)).isNotNull();
            }

            @Test
            void shouldHaveFilesystemPlacementServiceBean() {
                assertThat(context.getBean("filesystemStorePlacementService")).isNotNull();
            }

            @Test
            void shouldHaveFileSystemResourceLoaderBean() {
                assertThat(context.getBean("fileSystemResourceLoader")).isNotNull();
            }
        }

        @Nested
        @DisplayName("Given a context with a configurer")
        class ContextWithConfigurer {
            @BeforeEach
            void setUp() {
                context = new AnnotationConfigApplicationContext();
                context.register(ConverterConfig.class);
                context.refresh();
                configurer = context.getBean(FileSystemStoreConfigurer.class);
            }

            @AfterEach
            void tearDown() {
                context.close();
            }

            @Test
            void shouldCallThatConfigurerToHelpCustomizeStore() {
                verify(configurer).configureFileSystemStoreConverters(any());
            }
        }

        @Nested
        @DisplayName("Given a context with an empty configuration")
        class ContextWithEmptyConfiguration {
            @BeforeEach
            void setUp() {
                context = new AnnotationConfigApplicationContext();
                context.register(EmptyConfig.class);
                context.refresh();
            }

            @AfterEach
            void tearDown() {
                context.close();
            }

            @Test
            void shouldNotContainAnyFilesystemRepositoryBeans() {
                assertThat(context.getBeanNamesForType(SingleContentFsStore.class)).isEmpty();
            }
        }
    }
}

