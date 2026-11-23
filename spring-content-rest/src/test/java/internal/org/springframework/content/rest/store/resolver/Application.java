package internal.org.springframework.content.rest.store.resolver;

import internal.org.springframework.content.rest.it.SecurityConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.data.mongodb.autoconfigure.DataMongoAutoConfiguration;
import org.springframework.boot.data.mongodb.autoconfigure.DataMongoRepositoriesAutoConfiguration;
import org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration;
import org.springframework.content.commons.storeservice.StoreInfo;
import org.springframework.content.fs.config.EnableFileSystemStores;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.content.fs.store.FileSystemContentStore;
import org.springframework.content.jpa.config.EnableJpaStores;
import org.springframework.content.rest.config.ContentRestConfigurer;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;

@SpringBootApplication(exclude = {
    MongoAutoConfiguration.class, DataMongoAutoConfiguration.class, DataMongoRepositoriesAutoConfiguration.class
})
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Configuration
    @Import({RestConfiguration.class, SecurityConfiguration.class})
    @EnableJpaRepositories(basePackages = {
        "internal.org.springframework.content.rest.store.resolver",
        "internal.org.springframework.content.rest.scoped.store.resolver"
    }, considerNestedRepositories = true)
    @EnableTransactionManagement
    @EnableJpaStores(basePackages = "internal.org.springframework.content.rest.scoped.store.resolver")
    @EnableFileSystemStores(basePackages = "internal.org.springframework.content.rest.scoped.store.resolver")
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
                dataSource(), Database.H2, "org.springframework.content.test.support.jpa.fixture");
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
        public ContentRestConfigurer contentRestConfigurer() {
            return config -> config.addStoreResolver("storeResolverEntities", stores -> {
                for (StoreInfo info : stores) {
                    if (info.getImplementation(FileSystemContentStore.class) != null) {
                        return info;
                    }
                }
                return null;
            });
        }
    }
}
