package internal.org.springframework.content.jpa.it.support;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.content.test.support.EmbeddedJpa;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.orm.jpa.vendor.Database;

import javax.sql.DataSource;

/**
 * @author Vladimir Svyatski
 */
@Configuration
public class PostgresStoreItJdbcConfig extends AbstractStoreItJdbcConfig {

    @Value("classpath:/org/springframework/content/jpa/schema-drop-postgresql.sql")
    private Resource dropRepositoryTables;

    @Value("classpath:/org/springframework/content/jpa/schema-postgresql.sql")
    private Resource dataRepositorySchema;

    @Override
    protected Resource dropSchemaResource() {
        return dropRepositoryTables;
    }

    @Override
    protected Resource dataSchemaResource() {
        return dataRepositorySchema;
    }

    @Override
    protected DataSource jdbcDataSource() {
        return EmbeddedJpa.driverManagerDataSource(StoreItJdbcUrls.POSTGRESQL,
            StoreItJdbcUrls.TESTCONTAINERS_USERNAME, StoreItJdbcUrls.TESTCONTAINERS_PASSWORD);
    }

    @Override
    protected Database jpaDatabase() {
        return Database.POSTGRESQL;
    }
}
