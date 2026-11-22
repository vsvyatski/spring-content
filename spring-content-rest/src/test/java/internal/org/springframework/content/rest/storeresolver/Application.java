package internal.org.springframework.content.rest.storeresolver;

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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;

@SpringBootApplication(exclude = {
    MongoAutoConfiguration.class,
    DataMongoAutoConfiguration.class,
    DataMongoRepositoriesAutoConfiguration.class
})
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Configuration
    @Import({RestConfiguration.class, SecurityConfiguration.class})
    @EnableJpaRepositories(
        basePackages = {
            "internal.org.springframework.content.rest.storeresolver",
            "internal.org.springframework.content.rest.scoped.storeresolver"
        },
        considerNestedRepositories = true)
    @EnableTransactionManagement
    @EnableJpaStores(basePackages = "internal.org.springframework.content.rest.scoped.storeresolver")
    @EnableFileSystemStores(basePackages = "internal.org.springframework.content.rest.scoped.storeresolver")
    public static class AppConfig {

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
            factory.setPackagesToScan("org.springframework.content.test.support.jpa.fixture");
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