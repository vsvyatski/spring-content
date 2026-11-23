package internal.org.springframework.content.rest.it.cache.control;

import internal.org.springframework.content.rest.it.SecurityConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.data.mongodb.autoconfigure.DataMongoAutoConfiguration;
import org.springframework.boot.data.mongodb.autoconfigure.DataMongoRepositoriesAutoConfiguration;
import org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration;
import org.springframework.content.fs.config.EnableFileSystemStores;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.content.rest.config.ContentRestConfigurer;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.content.test.support.EmbeddedJpa;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.CacheControl;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;

@SpringBootApplication(exclude = {
    MongoAutoConfiguration.class, DataMongoAutoConfiguration.class, DataMongoRepositoriesAutoConfiguration.class
})
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Configuration
    @Import({RestConfiguration.class, SecurityConfiguration.class})
    @EnableJpaRepositories(basePackages = "internal.org.springframework.content.rest.it.cache.control",
        considerNestedRepositories = true)
    @EnableFileSystemStores(basePackages = "internal.org.springframework.content.rest.it.cache.control")
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
        public FileSystemResourceLoader fileSystemResourceLoader() throws IOException {
            return new FileSystemResourceLoader(Files.createTempDirectory("").toFile().getAbsolutePath());
        }

        @Bean
        public ContentRestConfigurer configurer() {
            return config -> config
                .cacheControl()
                .antMatcher("/uuidSingleContentEntities/*", CacheControl.maxAge(Duration.ofSeconds(60)));
        }
    }
}
