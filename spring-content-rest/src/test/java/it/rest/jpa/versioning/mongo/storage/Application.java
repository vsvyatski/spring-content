package it.rest.jpa.versioning.mongo.storage;

import com.mongodb.client.MongoClient;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.content.mongo.config.EnableMongoStores;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.content.test.support.EmbeddedJpa;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.versions.jpa.config.JpaLockingAndVersioningConfig;
import org.springframework.versions.test.support.VersioningTestPackages;

import javax.sql.DataSource;

@SpringBootApplication
@EntityScan(basePackages = VersioningTestPackages.JPA)
@EnableJpaRepositories(considerNestedRepositories = true,
    basePackages = {"it.rest.jpa.versioning.mongo.storage", "org.springframework.versions"})
@EnableTransactionManagement
@EnableMongoStores(basePackages = "it.rest.jpa.versioning.mongo.storage")
@Import({JpaLockingAndVersioningConfig.class, RestConfiguration.class, LockingAndVersioningRestIT.SecurityConfiguration.class})
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Configuration
    public static class AppConfig extends AbstractMongoClientConfiguration {
        @Value("/org/springframework/versions/jpa/schema-drop-hsqldb.sql")
        private ClassPathResource dropRepositoryTables;
        @Value("/org/springframework/versions/jpa/schema-hsqldb.sql")
        private ClassPathResource dataRepositorySchema;

        @Override
        protected @NonNull String getDatabaseName() {
            return MongoTestContainer.getTestDbName();
        }

        @Override
        @Bean
        public @NonNull MongoClient mongoClient() {
            return MongoTestContainer.getMongoClient();
        }

        @Bean
        public GridFsTemplate gridFsTemplate(MappingMongoConverter mongoConverter) {
            return new GridFsTemplate(mongoDbFactory(), mongoConverter);
        }

        @Override
        @Bean
        public @NonNull MongoDatabaseFactory mongoDbFactory() {
            return new SimpleMongoClientDatabaseFactory(mongoClient(), getDatabaseName());
        }

        @Bean
        DataSourceInitializer datasourceInitializer(DataSource dataSource) {
            return EmbeddedJpa.schemaInitializer(dataSource, dropRepositoryTables, dataRepositorySchema);
        }

        @Bean
        public DataSource dataSource() {
            return EmbeddedJpa.embeddedDataSource(EmbeddedDatabaseType.HSQL);
        }

        @Bean
        public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
            return EmbeddedJpa.entityManagerFactory(
                dataSource(), Database.HSQL, "it.rest.jpa.versioning.mongo.storage", VersioningTestPackages.JPA);
        }

        @Bean
        public PlatformTransactionManager transactionManager() {
            return EmbeddedJpa.transactionManager(entityManagerFactory().getObject());
        }
    }
}