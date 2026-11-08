package org.springframework.content.rest.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Disabled;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;

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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class RestConfigurationTest {

   private AnnotationConfigWebApplicationContext context;

   // mocks
   private static ContentRestConfigurer configurer;

    @Nested
    class RestConfigurationCases {
        @Nested
        class GivenAContextWithAContentRestConfiguration {
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
            void shouldBeConfigurable() {
                RestConfiguration config = context.getBean(RestConfiguration.class);
                assertThat(config).isNotNull();

                verify(configurer).configure(config);
            }

        }

    }

   @Disabled("This is not a test and must not be treated as such.")
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

   @Disabled("This is not a test and must not be treated as such.")
   @Document
   public class TestEntity {
      @Id
      private String id;
      @ContentId
      private String contentId;
   }

   public interface TestEntityRepository extends MongoRepository<TestEntity, String> {
   }

   public interface TestEntityContentRepository extends FileSystemContentStore<TestEntity, String> {
   }
}
