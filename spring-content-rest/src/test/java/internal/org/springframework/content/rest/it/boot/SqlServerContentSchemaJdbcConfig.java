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
public class SqlServerContentSchemaJdbcConfig extends AbstractContentSchemaJdbcConfig {

    @Value("/org/springframework/content/jpa/schema-drop-sqlserver.sql")
    private ClassPathResource dropRepositoryTables;

    @Value("/org/springframework/content/jpa/schema-sqlserver.sql")
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
            "jdbc:tc:sqlserver:///databasename?TC_TMPFS=/testtmpfs:rw&TC_DAEMON=true",
            "SA",
            "A_Str0ng_Required_Password");
    }

    @Override
    protected Database jpaDatabase() {
        return Database.SQL_SERVER;
    }
}