package internal.org.springframework.content.mongo.config;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.mongo.config.EnableMongoStores;
import org.springframework.content.mongo.config.MongoStoreConverter;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.util.UUID;

public class EnableMongoStoresTest {

	private AnnotationConfigApplicationContext context;

    @Nested
    class EnableMongoStoresCases {
        @Nested
        class GivenAnEnabledConfigurationWithAMongoContentRepositoryBean {
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
            void shouldHaveAMongoContentRepositoryBean() {
                assertThat(context.getBean(TestEntityContentRepository.class)).isNotNull();
            }
            @Test
            void shouldHaveAMongoStoreConverter() {
                assertThat(context.getBean("mongoStorePlacementService")).isNotNull();
            }
        }
        @Nested
        class GivenAContextWithACustomConverter {
            @BeforeEach
            void setUp() {
                context = new AnnotationConfigApplicationContext();
                					context.register(ConverterConfig.class);
                					context.refresh();
            }
            @AfterEach
            void tearDown() {
                context.close();
            }
            @Test
            void shouldUseThatConverter() {
                ConversionService converters = (ConversionService) context
                							.getBean("mongoStorePlacementService");
                					assertThat(converters.convert(
                									UUID.fromString(
                											"e49d5464-26ce-11e7-93ae-92361f002671"),
                									String.class)).isEqualTo("/e49d5464/26ce/11e7/93ae/92361f002671");
            }
        }
        @Nested
        class GivenAnEnabledConfigurationWithNoMongoContentRepositoryBeans {
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
            void shouldLoadTheContextButHaveNoMongoRepositoryBeans() {
                try {
                										context.getBean(
                												TestEntityContentRepository.class);
                										fail("expected no such bean");
                									}
                									catch (NoSuchBeanDefinitionException e) {
                										assertThat(true).isTrue();
                									}
            }
        }
    }

	@Test
	public void noop() {
		// noop
	}

	@Configuration
	@EnableMongoStores(basePackages = "contains.no.mongo.repositores")
	@Import(InfrastructureConfig.class)
	public static class EmptyConfig {
		//
	}

	@Configuration
	@EnableMongoStores
	@Import(InfrastructureConfig.class)
	public static class TestConfig {
		//
	}

	@Configuration
	@EnableMongoStores
	@Import(InfrastructureConfig.class)
	public static class ConverterConfig {
		@Bean
		public MongoStoreConverter<UUID, String> uuidConverter() {
			return new MongoStoreConverter<UUID, String>() {

				@Override
				public String convert(UUID source) {
					return String.format("/%s", source.toString().replaceAll("-", "/"));
				}

			};
		}
	}

	@Configuration
	public static class InfrastructureConfig extends AbstractMongoClientConfiguration {

		@Override
		protected String getDatabaseName() {
			return "spring-content";
		}

		@Bean
		public MongoClient mongoClient() {
			return MongoClients.create("mongodb://localhost:27017");
		}

		@Bean
		public GridFsTemplate gridFsTemplate(MappingMongoConverter mongoConverter) throws Exception {
			return new GridFsTemplate(mongoDbFactory(), mongoConverter);
		}

		@Bean
		public MongoDatabaseFactory mongoDbFactory() {
			return new SimpleMongoClientDatabaseFactory(mongoClient(), getDatabaseName());
		}
	}

	public class TestEntity {
		@Id
		private String id;
		@ContentId
		private String contentId;
	}

	public interface TestEntityContentRepository
			extends ContentStore<TestEntity, String> {
	}
}
