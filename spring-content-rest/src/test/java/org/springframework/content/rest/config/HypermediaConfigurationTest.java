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

public class HypermediaConfigurationTest {

    private AnnotationConfigWebApplicationContext context;

    @SuppressWarnings("unused") // classpath / store component scan
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
    }

    @Nested
   class HypermediaConfigurationCases {
        @Nested
        @DisplayName("Given a context with a content rest configuration")
        class ContextWithContentRestConfiguration {
            @BeforeEach
            void setUp() {
                context = new AnnotationConfigWebApplicationContext();
                context.setServletContext(new MockServletContext());
                context.register(TestConfig.class,
                    DelegatingWebMvcConfiguration.class,
                    RepositoryRestMvcConfiguration.class,
                    HypermediaConfiguration.class);
                context.refresh();
            }

            @Test
            void shouldHaveAContentLinksProcessorBean() {
                assertThat(context.getBean("contentLinksProcessor")).isNotNull();
            }
        }
    }
}
