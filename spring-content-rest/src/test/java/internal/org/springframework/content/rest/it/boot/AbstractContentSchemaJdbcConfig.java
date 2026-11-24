package internal.org.springframework.content.rest.it.boot;

import org.springframework.content.test.support.EmbeddedJpa;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * Embedded JPA plus Spring Content schema scripts for REST {@code @SpringBootTest} apps.
 *
 * @author Vladimir Svyatski
 */
@Configuration
public abstract class AbstractContentSchemaJdbcConfig {

    private static final String REST_SUPPORT_ENTITIES = "internal.org.springframework.content.rest.support";

    protected abstract Resource dropSchemaResource();

    protected abstract Resource dataSchemaResource();

    protected abstract DataSource jdbcDataSource();

    protected abstract Database jpaDatabase();

    protected String[] entityPackagesToScan() {
        return new String[]{REST_SUPPORT_ENTITIES};
    }

    @Bean
    DataSourceInitializer contentSchemaInitializer(DataSource dataSource) {
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
    public PlatformTransactionManager transactionManager() {
        return EmbeddedJpa.transactionManager(entityManagerFactory().getObject());
    }
}
