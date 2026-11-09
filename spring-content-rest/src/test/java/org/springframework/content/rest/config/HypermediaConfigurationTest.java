package org.springframework.content.rest.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.fs.config.EnableFileSystemStores;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.content.fs.store.FileSystemContentStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.repository.MongoRepository;
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
    public interface TestEntityRepository extends MongoRepository<TestEntity, String> {
    }

    @SuppressWarnings("unused") // classpath / store component scan
    public interface TestEntityContentStore extends FileSystemContentStore<TestEntity, String> {
    }

    @Configuration
    @EnableFileSystemStores
    public static class TestConfig {

        @Bean
        public FileSystemResourceLoader filesystemRoot() throws IOException {
            return new FileSystemResourceLoader(Files.createTempDirectory("").toFile().getAbsolutePath());
        }
    }

    @Document
    public static class TestEntity {
        @Id
        private String id;
        @ContentId
        private String contentId;
    }

    @Nested
    class HypermediaConfigurationCases {
        @Nested
        class GivenAContextWithAContentRestConfiguration {
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
