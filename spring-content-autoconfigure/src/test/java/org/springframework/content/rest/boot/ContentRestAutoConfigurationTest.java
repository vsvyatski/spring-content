package org.springframework.content.rest.boot;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Disabled;
import static org.assertj.core.api.Assertions.assertThat;

import internal.org.springframework.content.rest.boot.autoconfigure.ContentRestAutoConfiguration;
import internal.org.springframework.content.rest.boot.autoconfigure.HypermediaAutoConfiguration;
import internal.org.springframework.content.rest.boot.autoconfigure.SpringBootContentRestConfigurer;
import internal.org.springframework.content.s3.boot.autoconfigure.S3ContentAutoConfiguration;
import internal.org.springframework.content.solr.boot.autoconfigure.SolrAutoConfiguration;
import internal.org.springframework.content.solr.boot.autoconfigure.SolrExtensionAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;
import org.springframework.boot.data.rest.autoconfigure.DataRestAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.web.context.servlet.AnnotationConfigServletWebApplicationContext;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import java.net.URI;

public class ContentRestAutoConfigurationTest {

    @Nested
    class ContentRestAutoConfigurationCases {
        @Nested
        class GivenADefaultConfiguration {
            @Test
            void shouldLoadTheContext() {
                AnnotationConfigServletWebApplicationContext context = new AnnotationConfigServletWebApplicationContext();
                context.setServletContext(new MockServletContext());
                context.register(TestConfig.class, HypermediaConfig.class);
                context.refresh();

                assertThat(context.getBean("contentHandlerMapping")).isNotNull();
                assertThat(context.getBean("contentLinksProcessor")).isNotNull();

                context.close();
            }

        }

        @Nested
        class GivenAnEnvironmentSpecifyingRestProperties {
            @BeforeEach
            void setUp() {
                System.setProperty("spring.content.rest.base-uri", "/contentApi");
                System.setProperty("spring.content.rest.fully-qualified-links", "false");
                               System.setProperty("spring.content.rest.shortcut-request-mappings.disabled", "true");
                System.setProperty("spring.content.rest.shortcut-request-mappings.excludes", "GET=a/b,c/d:PUT=*/*");
                System.setProperty("spring.content.rest.overwrite-existing-content", "false");
            }

            @AfterEach
            void tearDown() {
                System.clearProperty("spring.content.rest.base-uri");
                               System.clearProperty("spring.content.rest.fully-qualified-links");
                               System.clearProperty("spring.content.rest.shortcut-request-mappings.disabled");
                               System.clearProperty("spring.content.rest.shortcut-request-mappings.excludes");
                System.clearProperty("spring.content.rest.overwrite-existing-content");
            }

            @Test
            void shouldHaveAFilesystemPropertiesBeanWithTheCorrectPropertiesSet() {
                AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
                context.register(TestConfig.class);
                context.setServletContext(new MockServletContext());
                context.refresh();

                assertThat(context.getBean(ContentRestAutoConfiguration.ContentRestProperties.class).getBaseUri()).isEqualTo(URI.create("/contentApi"));
                assertThat(context.getBean(ContentRestAutoConfiguration.ContentRestProperties.class).fullyQualifiedLinks()).isFalse();
                               assertThat(context.getBean(ContentRestAutoConfiguration.ContentRestProperties.class).shortcutRequestMappings().disabled()).isTrue();
                               assertThat(context.getBean(ContentRestAutoConfiguration.ContentRestProperties.class).shortcutRequestMappings().excludes()).isEqualTo("GET=a/b,c/d:PUT=*/*");
                assertThat(context.getBean(ContentRestAutoConfiguration.ContentRestProperties.class).getOverwriteExistingContent()).isFalse();

                assertThat(context.getBean(SpringBootContentRestConfigurer.class)).isNotNull();

                context.close();
            }

        }

    }

	@Disabled("This is not a test")
	@SpringBootApplication(exclude={SolrAutoConfiguration.class, SolrExtensionAutoConfiguration.class, S3ContentAutoConfiguration.class})
	@ImportAutoConfiguration({ HibernateJpaAutoConfiguration.class, DataJpaRepositoriesAutoConfiguration.class,
			PropertyPlaceholderAutoConfiguration.class, DataRestAutoConfiguration.class,
			JacksonAutoConfiguration.class, ContentRestAutoConfiguration.class})
	public static class TestConfig {
	}

	@Disabled("This is not a test")
	@Configuration
	@ImportAutoConfiguration({HypermediaAutoConfiguration.class})
	public static class HypermediaConfig {}

	@Disabled("This is not a test")
	@Document
	public class TestEntity {
		@Id
		private String id;
		@ContentId
		private String contentId;
	}

	public interface TestEntityRepository extends MongoRepository<TestEntity, String> {
	}

	public interface TestEntityContentRepository extends ContentStore<TestEntity, String> {
	}
}
