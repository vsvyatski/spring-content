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
public class MariaDbStoreItJdbcConfig extends AbstractStoreItJdbcConfig {

    @Value("/org/springframework/content/jpa/schema-drop-mariadb.sql")
    private Resource dropRepositoryTables;

    @Value("/org/springframework/content/jpa/schema-mariadb.sql")
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
        return EmbeddedJpa.driverManagerDataSource(
            StoreItJdbcUrls.MARIADB,
            StoreItJdbcUrls.TESTCONTAINERS_USERNAME,
            StoreItJdbcUrls.TESTCONTAINERS_PASSWORD);
    }

    @Override
    protected Database jpaDatabase() {
        return Database.MYSQL;
    }
}
