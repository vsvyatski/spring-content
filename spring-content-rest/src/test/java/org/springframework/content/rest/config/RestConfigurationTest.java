package org.springframework.content.rest.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.content.fs.config.EnableFileSystemStores;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.content.fs.store.FileSystemContentStore;
import org.springframework.content.test.support.mongo.SingleContentMongoDocument;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;

import java.io.IOException;
import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class RestConfigurationTest {

    private static ContentRestConfigurer configurer;
    private AnnotationConfigWebApplicationContext context;

    public interface SingleContentMongoDocumentFsStore
        extends FileSystemContentStore<SingleContentMongoDocument, String> {
    }

    @Configuration
    @EnableFileSystemStores
    public static class TestConfig {

        @Bean
        public FileSystemResourceLoader filesystemRoot() throws IOException {
            return new FileSystemResourceLoader(Files.createTempDirectory("").toFile().getAbsolutePath());
        }

        @Bean
        public ContentRestConfigurer configurer() {
            return configurer;
        }
    }

    @Nested
   class RestConfigurationCases {
        @Nested
        @DisplayName("Given a context with a content rest configuration")
        class ContextWithContentRestConfiguration {
            @BeforeEach
            void setUp() {
                configurer = mock(ContentRestConfigurer.class);

                context = new AnnotationConfigWebApplicationContext();
                context.setServletContext(new MockServletContext());
                context.register(TestConfig.class,
                    DelegatingWebMvcConfiguration.class,
                    RepositoryRestMvcConfiguration.class,
                    RestConfiguration.class);
                context.refresh();
            }

            @Test
            void shouldHaveAContentHandlerMappingBean() {
                assertThat(context.getBean("contentHandlerMapping")).isNotNull();
            }

            @Test
            void shouldHaveTheContentRestControllers() {
                assertThat(context.getBean("storeRestController")).isNotNull();
            }

            @Test
            void shouldRegisterFilesystemStoreForMongoDocument() {
                assertThat(context.getBean(SingleContentMongoDocumentFsStore.class)).isNotNull();
            }

            @Test
            void shouldBeConfigurable() {
                RestConfiguration config = context.getBean(RestConfiguration.class);
                assertThat(config).isNotNull();

                verify(configurer).configure(config);
            }
        }
    }
}
