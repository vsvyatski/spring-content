package internal.org.springframework.content.rest.it.config;

import internal.org.springframework.content.rest.it.SecurityConfiguration;
import internal.org.springframework.content.rest.support.UuidSingleContentEntity;
import internal.org.springframework.content.rest.support.mock.store.EnableMockStores;
import internal.org.springframework.content.rest.support.mock.store.MockContentStore;
import internal.org.springframework.content.rest.support.mock.store.MockStoreFactoryBean;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.data.mongodb.autoconfigure.DataMongoAutoConfiguration;
import org.springframework.boot.data.mongodb.autoconfigure.DataMongoRepositoriesAutoConfiguration;
import org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.rest.config.ContentRestConfigurer;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.content.test.support.EmbeddedJpa;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.CrudRepository;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.context.WebApplicationContext;

import javax.sql.DataSource;
import java.util.UUID;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = PreferResourceForPutsAndPostsIT.Application.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension.class)
public class PreferResourceForPutsAndPostsIT {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private PreferResourceForPutsAndPostsRepository repo;

    @Autowired
    private WebApplicationContext webApplicationContext;

    public interface PreferResourceForPutsAndPostsRepository extends CrudRepository<UuidSingleContentEntity, Long> {
    }

    @SuppressWarnings("unused") // classpath / store component scan
    private interface PreferResourceForPutsAndPostsStore extends MockContentStore<UuidSingleContentEntity, UUID> {
    }

    @SpringBootApplication(exclude = {
        MongoAutoConfiguration.class,
        DataMongoAutoConfiguration.class,
        DataMongoRepositoriesAutoConfiguration.class
    })
    public static class Application {

        public static void main(String[] args) {
            SpringApplication.run(Application.class, args);
        }

        @Configuration
        @Import({RestConfiguration.class, SecurityConfiguration.class})
        @EnableJpaRepositories(basePackages = "internal.org.springframework.content.rest.it.config", considerNestedRepositories = true)
        @EnableMockStores(basePackages = "internal.org.springframework.content.rest.it.config")
        public static class AppConfig {

            @Value("/org/springframework/content/jpa/schema-drop-h2.sql")
            private ClassPathResource dropRepositoryTables;

            @Value("/org/springframework/content/jpa/schema-h2.sql")
            private ClassPathResource dataRepositorySchema;

            @Bean
            DataSourceInitializer datasourceInitializer(DataSource dataSource) {
                return EmbeddedJpa.schemaInitializer(dataSource, dropRepositoryTables, dataRepositorySchema);
            }

            @Bean
            public DataSource dataSource() {
                return EmbeddedJpa.embeddedDataSource(EmbeddedDatabaseType.H2);
            }

            @Bean
            public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
                return EmbeddedJpa.entityManagerFactory(
                    dataSource(), Database.H2, "internal.org.springframework.content.rest.support");
            }

            @Bean
            public PlatformTransactionManager transactionManager() {
                return EmbeddedJpa.transactionManager(entityManagerFactory().getObject());
            }

            @Bean
            public ContentRestConfigurer configurer() {
                return config -> config.forDomainType(UuidSingleContentEntity.class).putAndPostPreferResource();
            }
        }
    }

    @Nested
    class PreferResourceForPutsAndPosts {
        @BeforeEach
        void setUp() {
            RestAssuredMockMvc.webAppContextSetup(webApplicationContext);
        }

        @Test
        void shouldUseTheSetContentSResourceMethod() {
            UuidSingleContentEntity entity = new UuidSingleContentEntity();
            entity = repo.save(entity);

            given()
                .contentType("text/plain")
                .body("some content".getBytes())
                .when()
                .post("/uuidSingleContentEntities/" + entity.getId());

            MockStoreFactoryBean storeFactory = context.getBean(MockStoreFactoryBean.class);

            verify(storeFactory.getMock()).setContent(org.mockito.ArgumentMatchers.isA(UuidSingleContentEntity.class), org.mockito.ArgumentMatchers.isA(PropertyPath.class), org.mockito.ArgumentMatchers.isA(Resource.class));
        }
    }
}
