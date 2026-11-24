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
public class SqlServerStoreItJdbcConfig extends AbstractStoreItJdbcConfig {

    @Value("/org/springframework/content/jpa/schema-drop-sqlserver.sql")
    private Resource dropRepositoryTables;

    @Value("/org/springframework/content/jpa/schema-sqlserver.sql")
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
            StoreItJdbcUrls.SQL_SERVER,
            StoreItJdbcUrls.SQL_SERVER_USERNAME,
            StoreItJdbcUrls.SQL_SERVER_PASSWORD);
    }

    @Override
    protected Database jpaDatabase() {
        return Database.SQL_SERVER;
    }
}
