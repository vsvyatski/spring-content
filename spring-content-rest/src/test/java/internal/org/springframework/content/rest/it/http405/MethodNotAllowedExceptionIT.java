package internal.org.springframework.content.rest.it.http405;

import internal.org.springframework.content.rest.it.SecurityConfiguration;
import internal.org.springframework.content.rest.scoped.http405.*;
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
import org.springframework.content.fs.config.EnableFileSystemStores;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.context.WebApplicationContext;

import javax.sql.DataSource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;

@SpringBootTest(classes = MethodNotAllowedExceptionIT.Application.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension.class)
public class MethodNotAllowedExceptionIT {

    @Autowired
    private UnexportedStoreMethodsEntityRepository repo;

    @Autowired
    private UnexportedStoreMethodsEntityStore store;

    @Autowired
    private UnexportedContentPropertyEntityRepository repo2;

    @Autowired
    private UnexportedContentPropertyEntityStore store2;

    @Autowired
    private WebApplicationContext webApplicationContext;

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
        @EnableJpaRepositories(
            basePackages = "internal.org.springframework.content.rest.scoped.http405",
            considerNestedRepositories = true)
        @EnableFileSystemStores(basePackages = "internal.org.springframework.content.rest.scoped.http405")
        public static class TestConfig {

            @Value("/org/springframework/content/jpa/schema-drop-h2.sql")
            private ClassPathResource dropRepositoryTables;

            @Value("/org/springframework/content/jpa/schema-h2.sql")
            private ClassPathResource dataRepositorySchema;

            @Bean
            DataSourceInitializer datasourceInitializer(DataSource dataSource) {
                ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();

                databasePopulator.addScript(dropRepositoryTables);
                databasePopulator.addScript(dataRepositorySchema);
                databasePopulator.setIgnoreFailedDrops(true);

                DataSourceInitializer initializer = new DataSourceInitializer();
                initializer.setDataSource(dataSource);
                initializer.setDatabasePopulator(databasePopulator);

                return initializer;
            }

            @Bean
            public DataSource dataSource() {
                EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
                return builder.setType(EmbeddedDatabaseType.H2).build();
            }

            @Bean
            public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
                HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
                vendorAdapter.setDatabase(Database.H2);
                vendorAdapter.setGenerateDdl(true);

                LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
                factory.setJpaVendorAdapter(vendorAdapter);
                factory.setPackagesToScan(
                    "internal.org.springframework.content.rest.scoped.http405",
                    "org.springframework.content.test.support.jpa.fixture");
                factory.setDataSource(dataSource());

                return factory;
            }

            @Bean
            public PlatformTransactionManager transactionManager() {
                JpaTransactionManager txManager = new JpaTransactionManager();
                txManager.setEntityManagerFactory(entityManagerFactory().getObject());
                return txManager;
            }

            @Bean
            public FileSystemResourceLoader filesystemRoot() throws IOException {
                return new FileSystemResourceLoader(Files.createTempDirectory("").toFile().getAbsolutePath());
            }
        }
    }

    @Nested
    class WhenGetContentMethodIsNotExportedCases {
        @BeforeEach
        void setUp() {
            RestAssuredMockMvc.webAppContextSetup(webApplicationContext);
        }

        @Test
        void shouldThrowA405NotAllowed() {
            UnexportedStoreMethodsEntity entity = new UnexportedStoreMethodsEntity();
            entity = store.setContent(entity, new ByteArrayInputStream("some content".getBytes()));
            entity = repo.save(entity);

            given()
                .accept("text/plain")
                .when()
                .get("/tEntities/" + entity.getId())
                .then()
                .statusCode(405);
        }
    }

    @Nested
    class WhenSetContentMethodsAreNotExportedCases {
        @BeforeEach
        void setUp() {
            RestAssuredMockMvc.webAppContextSetup(webApplicationContext);
        }

        @Test
        void shouldThrowA405NotAllowed() {
            UnexportedStoreMethodsEntity entity = new UnexportedStoreMethodsEntity();
            entity = repo.save(entity);

            given()
                .contentType("text/plain")
                .body("some content".getBytes())
                .when()
                .post("/tEntities/" + entity.getId())
                .then()
                .statusCode(405);
        }
    }

    @Nested
    class WhenUnsetContentMethodAreNotExportedCases {
        @BeforeEach
        void setUp() {
            RestAssuredMockMvc.webAppContextSetup(webApplicationContext);
        }

        @Test
        void shouldThrowA405NotAllowed() {
            UnexportedStoreMethodsEntity entity = new UnexportedStoreMethodsEntity();
            entity = store.setContent(entity, new ByteArrayInputStream("some content".getBytes()));
            entity = repo.save(entity);

            given()
                .accept("text/plain")
                .when()
                .delete("/tEntities/" + entity.getId())
                .then()
                .statusCode(405);
        }
    }

    @Nested
    class WhenAContentPropertyIsNotExportedCases {
        @BeforeEach
        void setUp() {
            RestAssuredMockMvc.webAppContextSetup(webApplicationContext);
        }

        @Test
        void shouldThrowA405NotAllowedForAllRequests() {
            UnexportedContentPropertyEntity entity = new UnexportedContentPropertyEntity();
            entity = store2.setContent(entity, new ByteArrayInputStream("some content".getBytes()));
            entity = repo2.save(entity);

            given()
                .accept("text/plain")
                .when()
                .get("/tEntity2s/" + entity.getId() + "/content")
                .then()
                .statusCode(405);

            given()
                .contentType("text/plain")
                .body("some content".getBytes())
                .when()
                .post("/tEntity2s/" + entity.getId() + "/content")
                .then()
                .statusCode(405);

            given()
                .accept("text/plain")
                .when()
                .delete("/tEntity2s/" + entity.getId() + "/content")
                .then()
                .statusCode(405);
        }
    }
}
