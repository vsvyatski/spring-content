package internal.org.springframework.content.jpa.it.support;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.content.test.support.EmbeddedJpa;
import org.springframework.content.test.support.TestSupportPackages;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

/**
 * Embedded JPA, Spring Content schema scripts, and dialect-specific {@link DataSource} for store ITs.
 *
 * @author Vladimir Svyatski
 */
@Configuration
@EnableTransactionManagement
public abstract class AbstractStoreItJdbcConfig {

    private static final String JPA_IT_PACKAGE = "internal.org.springframework.content.jpa";

    protected abstract Resource dropSchemaResource();

    protected abstract Resource dataSchemaResource();

    protected abstract DataSource jdbcDataSource();

    protected abstract Database jpaDatabase();

    protected String[] entityPackagesToScan() {
        return new String[]{JPA_IT_PACKAGE, TestSupportPackages.JPA};
    }

    @Bean
    DataSourceInitializer datasourceInitializer(DataSource dataSource) {
        return EmbeddedJpa.schemaInitializer(dataSource, dropSchemaResource(), dataSchemaResource());
    }

    @Bean
    public DataSource dataSource() {
        return jdbcDataSource();
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        return EmbeddedJpa.entityManagerFactory(dataSource(), jpaDatabase(), entityPackagesToScan());
    }

    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        return EmbeddedJpa.transactionManager(entityManagerFactory);
    }
}
