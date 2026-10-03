package internal.org.springframework.content.fs.config;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.fs.config.EnableFileSystemStores;
import org.springframework.content.fs.config.FileSystemStoreConfigurer;
import org.springframework.content.fs.config.FileSystemStoreConverter;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class EnableFileSystemStoresTest {

    private AnnotationConfigApplicationContext context;

    // mocks
    static FileSystemStoreConfigurer configurer;

    
    @Nested
    class EnableFileSystemStoresCases {
        @Nested
        class GivenAContextAndAConfigurationWithAFilesystemContentRepositoryBean {
            @BeforeEach
            void setUp() throws Throwable {
                context = new AnnotationConfigApplicationContext();
                                            context.register(TestConfig.class);
                                            context.refresh();
            }
            @AfterEach
            void tearDown() throws Throwable {
                context.close();
            }
            @Test
            void shouldHaveAContentRepositoryBean() throws Throwable {
                assertThat(context.getBean(TestEntityContentRepository.class)).isNotNull();
            }
            @Test
            void shouldHaveAFilesystemPlacementServiceBean() throws Throwable {
                assertThat(context.getBean("filesystemStorePlacementService")).isNotNull();
            }
            @Test
            void shouldHaveAFileSystemResourceLoaderBean() throws Throwable {
                assertThat(context.getBean("fileSystemResourceLoader")).isNotNull();
            }
        }
        @Nested
        class GivenAContextWithAConfigurer {
            @BeforeEach
            void setUp() throws Throwable {
                configurer = mock(FileSystemStoreConfigurer.class);

                                    context = new AnnotationConfigApplicationContext();
                                    context.register(ConverterConfig.class);
                                    context.refresh();
            }
            @AfterEach
            void tearDown() throws Throwable {
                context.close();
            }
            @Test
            void shouldCallThatConfigurerToHelpCustomizeTheStore() throws Throwable {
                verify(configurer).configureFileSystemStoreConverters(any());
            }
        }
        @Nested
        class GivenAContextWithAnEmptyConfiguration {
            @BeforeEach
            void setUp() throws Throwable {
                context = new AnnotationConfigApplicationContext();
                                    context.register(EmptyConfig.class);
                                    context.refresh();
            }
            @AfterEach
            void tearDown() throws Throwable {
                context.close();
            }
            @Test
            void shouldNotContainAnyFilesystemRepositoryBeans() throws Throwable {
                try {
                                        context.getBean(TestEntityContentRepository.class);
                                        fail("expected no such bean");
                                    } catch (NoSuchBeanDefinitionException e) {
                                        assertThat(true).isTrue();
                                    }
            }
        }
    }


    @Test
    public void noop() {
    }

    @Configuration
    @EnableFileSystemStores(basePackages = "contains.no.fs.repositories")
    @PropertySource("classpath:/test.properties")
    public static class EmptyConfig {
    }

    @Configuration
    @EnableFileSystemStores
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
    @EnableFileSystemStores
    @PropertySource("classpath:/test.properties")
    public static class ConverterConfig {

        @Value("${spring.content.fs.filesystemRoot:#{null}}")
        private String filesystemRoot;

        @Bean
        public FileSystemStoreConverter<UUID, String> uuidConverter() {
            return source -> String.format("/%s", source.toString().replaceAll("-", "/"));
        }

        @Bean
        public FileSystemStoreConfigurer configurer() {
            return configurer;
        }

        @Bean
        FileSystemResourceLoader fileSystemResourceLoader() {
            return new FileSystemResourceLoader(filesystemRoot);
        }
    }

    public static class TestEntity {
        @ContentId
        private String contentId;
    }

    public interface TestEntityContentRepository
            extends ContentStore<TestEntity, String> {
    }
}
