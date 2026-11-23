package internal.org.springframework.content.mongo.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.content.mongo.config.EnableMongoStores;
import org.springframework.content.mongo.config.MongoStoreConverter;
import org.springframework.content.test.support.TestSupportPackages;
import org.springframework.content.test.support.mongo.SingleContentMongoStore;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class EnableMongoStoresTest {

    private AnnotationConfigApplicationContext context;

    @Configuration
    @EnableMongoStores(basePackages = "contains.no.mongo.repositories")
    @Import(InfrastructureConfig.class)
    public static class EmptyConfig {
    }

    @Configuration
    @EnableMongoStores(basePackages = TestSupportPackages.MONGO)
    @Import(InfrastructureConfig.class)
    public static class TestConfig {
    }

    @Configuration
    @EnableMongoStores(basePackages = TestSupportPackages.MONGO)
    @Import(InfrastructureConfig.class)
    public static class ConverterConfig {
        @Bean
        public MongoStoreConverter<UUID, String> uuidConverter() {
            // Not a lambda: PlacementServiceImpl resolves Converter<S,T> from converter.getClass().
            //noinspection Convert2Lambda
            return new MongoStoreConverter<>() {
                @Override
                public String convert(@NonNull UUID source) {
                    return String.format("/%s", source.toString().replace("-", "/"));
                }
            };
        }
    }

    @Configuration
    public static class InfrastructureConfig extends AbstractMongoClientConfiguration {

        @Override
        protected @NonNull String getDatabaseName() {
            return "spring-content";
        }

        @Bean
        public @NonNull MongoClient mongoClient() {
            return MongoClients.create("mongodb://localhost:27017");
        }

        @Bean
        public GridFsTemplate gridFsTemplate(MappingMongoConverter mongoConverter) {
            return new GridFsTemplate(mongoDbFactory(), mongoConverter);
        }

        @Bean
        public @NonNull MongoDatabaseFactory mongoDbFactory() {
            return new SimpleMongoClientDatabaseFactory(mongoClient(), getDatabaseName());
        }
    }

    @Nested
   class EnableMongoStoresCases {
        @Nested
        @DisplayName("Given an enabled configuration with a mongo content store bean")
        class EnabledConfigurationWithMongoContentStoreBean {
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
            void shouldHaveAMongoContentStoreBean() {
                assertThat(context.getBean(SingleContentMongoStore.class)).isNotNull();
            }

            @Test
            void shouldHaveAMongoStoreConverter() {
                assertThat(context.getBean("mongoStorePlacementService")).isNotNull();
            }
        }

        @Nested

        @DisplayName("Given a context with a custom converter")

        class ContextWithCustomConverter {
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

        @DisplayName("Given an enabled configuration with no mongo content store beans")

        class EnabledConfigurationWithNoMongoContentStoreBeans {
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
                assertThat(context.getBeanNamesForType(SingleContentMongoStore.class)).isEmpty();
            }
        }
    }
}
