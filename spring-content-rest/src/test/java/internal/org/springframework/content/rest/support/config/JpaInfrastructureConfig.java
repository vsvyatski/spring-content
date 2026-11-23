package internal.org.springframework.content.rest.support.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.test.support.EmbeddedJpa;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Objects;

@Configuration
@EnableJpaAuditing
public class JpaInfrastructureConfig {

    @Bean
    public DataSource dataSource() {
        return EmbeddedJpa.embeddedDataSource(EmbeddedDatabaseType.H2);
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        LocalContainerEntityManagerFactoryBean factory =
            EmbeddedJpa.entityManagerFactory(dataSource(), Database.H2, packagesToScan());
        factory.setPersistenceUnitName("spring-data-rest-webmvc");
        factory.afterPropertiesSet();
        return factory;
    }

    protected String[] packagesToScan() {
        return new String[]{
            "internal.org.springframework.content.rest.support"
        };
    }

    @Bean
    @DependsOn("entityManagerFactory")
    public PlatformTransactionManager transactionManager(
        @Autowired LocalContainerEntityManagerFactoryBean entityManagerFactory) {
        return EmbeddedJpa.transactionManager(Objects.requireNonNull(entityManagerFactory.getObject()));
    }
}
