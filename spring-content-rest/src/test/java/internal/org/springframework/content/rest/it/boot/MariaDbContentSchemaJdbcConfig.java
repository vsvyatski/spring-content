package internal.org.springframework.content.rest.it.boot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.content.test.support.EmbeddedJpa;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.orm.jpa.vendor.Database;

import javax.sql.DataSource;

/**
 * @author Vladimir Svyatski
 */
@Configuration
public class MariaDbContentSchemaJdbcConfig extends AbstractContentSchemaJdbcConfig {

    @Value("/org/springframework/content/jpa/schema-drop-mariadb.sql")
    private ClassPathResource dropRepositoryTables;

    @Value("/org/springframework/content/jpa/schema-mariadb.sql")
    private ClassPathResource dataRepositorySchema;

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
            "jdbc:tc:mariadb:10.11:///databasename?TC_TMPFS=/testtmpfs:rw&TC_DAEMON=true",
            "test",
            "test");
    }

    @Override
    protected Database jpaDatabase() {
        return Database.MYSQL;
    }
}