package internal.org.springframework.content.rest.it.http405;

import internal.org.springframework.content.rest.it.SecurityConfiguration;
import internal.org.springframework.content.rest.scoped.http405.*;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import org.springframework.content.test.support.EmbeddedJpa;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
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
        MongoAutoConfiguration.class, DataMongoAutoConfiguration.class, DataMongoRepositoriesAutoConfiguration.class
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
                return EmbeddedJpa.schemaInitializer(dataSource, dropRepositoryTables, dataRepositorySchema);
            }

            @Bean
            public DataSource dataSource() {
                return EmbeddedJpa.embeddedDataSource(EmbeddedDatabaseType.H2);
            }

            @Bean
            public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
                return EmbeddedJpa.entityManagerFactory(
                    dataSource(),
                    Database.H2,
                    "internal.org.springframework.content.rest.scoped.http405",
                    "org.springframework.content.test.support.jpa.fixture");
            }

            @Bean
            public PlatformTransactionManager transactionManager() {
                return EmbeddedJpa.transactionManager(entityManagerFactory().getObject());
            }

            @Bean
            public FileSystemResourceLoader filesystemRoot() throws IOException {
                return new FileSystemResourceLoader(Files.createTempDirectory("").toFile().getAbsolutePath());
            }
        }
    }

    @Nested
    @DisplayName("When GET content method is not exported")
    class GetContentMethodIsNotExportedCases {
        @BeforeEach
        void setUp() {
            RestAssuredMockMvc.webAppContextSetup(webApplicationContext);
        }

        @Test
        void shouldThrow405NotAllowed() {
            UnexportedStoreMethodsEntity entity = new UnexportedStoreMethodsEntity();
            entity = store.setContent(entity, new ByteArrayInputStream("some content".getBytes()));
            entity = repo.save(entity);

            given()
                .accept("text/plain")
                .when()
                .get("/unexportedStoreMethodsEntities/" + entity.getId())
                .then()
                .statusCode(405);
        }
    }

    @Nested
    @DisplayName("When set content methods are not exported")
    class SetContentMethodsAreNotExportedCases {
        @BeforeEach
        void setUp() {
            RestAssuredMockMvc.webAppContextSetup(webApplicationContext);
        }

        @Test
        void shouldThrow405NotAllowed() {
            UnexportedStoreMethodsEntity entity = new UnexportedStoreMethodsEntity();
            entity = repo.save(entity);

            given()
                .contentType("text/plain")
                .body("some content".getBytes())
                .when()
                .post("/unexportedStoreMethodsEntities/" + entity.getId())
                .then()
                .statusCode(405);
        }
    }

    @Nested
    @DisplayName("When unset content methods are not exported")
    class UnsetContentMethodAreNotExportedCases {
        @BeforeEach
        void setUp() {
            RestAssuredMockMvc.webAppContextSetup(webApplicationContext);
        }

        @Test
        void shouldThrow405NotAllowed() {
            UnexportedStoreMethodsEntity entity = new UnexportedStoreMethodsEntity();
            entity = store.setContent(entity, new ByteArrayInputStream("some content".getBytes()));
            entity = repo.save(entity);

            given()
                .accept("text/plain")
                .when()
                .delete("/unexportedStoreMethodsEntities/" + entity.getId())
                .then()
                .statusCode(405);
        }
    }

    @Nested
    @DisplayName("When a content property is not exported")
    class ContentPropertyIsNotExportedCases {
        @BeforeEach
        void setUp() {
            RestAssuredMockMvc.webAppContextSetup(webApplicationContext);
        }

        @Test
        void shouldThrow405NotAllowedForAllRequests() {
            UnexportedContentPropertyEntity entity = new UnexportedContentPropertyEntity();
            entity = store2.setContent(entity, new ByteArrayInputStream("some content".getBytes()));
            entity = repo2.save(entity);

            given()
                .accept("text/plain")
                .when()
                .get("/unexportedContentPropertyEntities/" + entity.getId() + "/content")
                .then()
                .statusCode(405);

            given()
                .contentType("text/plain")
                .body("some content".getBytes())
                .when()
                .post("/unexportedContentPropertyEntities/" + entity.getId() + "/content")
                .then()
                .statusCode(405);

            given()
                .accept("text/plain")
                .when()
                .delete("/unexportedContentPropertyEntities/" + entity.getId() + "/content")
                .then()
                .statusCode(405);
        }
    }
}
