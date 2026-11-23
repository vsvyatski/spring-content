package internal.org.springframework.content.rest.it.oracle;

import internal.org.springframework.content.rest.it.SecurityConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.data.mongodb.autoconfigure.DataMongoAutoConfiguration;
import org.springframework.boot.data.mongodb.autoconfigure.DataMongoRepositoriesAutoConfiguration;
import org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration;
import org.springframework.content.jpa.config.EnableJpaStores;
import org.springframework.content.test.support.EmbeddedJpa;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.TimeZone;

@SpringBootApplication(exclude = {
    MongoAutoConfiguration.class, DataMongoAutoConfiguration.class, DataMongoRepositoriesAutoConfiguration.class
})
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Configuration
    @Import({RestConfiguration.class, SecurityConfiguration.class})
    @EnableJpaRepositories(basePackages = "internal.org.springframework.content.rest.support")
    @EnableTransactionManagement
    @EnableJpaStores(basePackages = "internal.org.springframework.content.rest.support")
    public static class AppConfig {

        @Value("/org/springframework/content/jpa/schema-drop-oracle.sql")
        private ClassPathResource dropRepositoryTables;

        @Value("/org/springframework/content/jpa/schema-oracle.sql")
        private ClassPathResource dataRepositorySchema;

        @Bean
        DataSourceInitializer datasourceInitializer(DataSource dataSource) {
            return EmbeddedJpa.schemaInitializer(dataSource, dropRepositoryTables, dataRepositorySchema);
        }

        @Bean
        public DataSource dataSource() {
            // Timezone is not set in GitHub containers, need this for connections to work
            TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
            return EmbeddedJpa.driverManagerDataSource(
                "jdbc:tc:oracle:///databasename?TC_TMPFS=/testtmpfs:rw?TC_DAEMON=true",
                "system",
                "oracle");
        }

        @Bean
        public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
            return EmbeddedJpa.entityManagerFactory(
                dataSource, Database.ORACLE, "internal.org.springframework.content.rest.support");
        }

        @Bean
        public PlatformTransactionManager transactionManager(
            LocalContainerEntityManagerFactoryBean entityManagerFactory) {
            return EmbeddedJpa.transactionManager(entityManagerFactory.getObject());
        }
    }
}
