package internal.org.springframework.content.rest.it.boot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.content.test.support.EmbeddedJpa;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.vendor.Database;

import javax.sql.DataSource;

/**
 * @author Vladimir Svyatski
 */
@Configuration
public class HsqlContentSchemaJdbcConfig extends AbstractContentSchemaJdbcConfig {

    @Value("/org/springframework/content/jpa/schema-drop-hsqldb.sql")
    private ClassPathResource dropRepositoryTables;

    @Value("/org/springframework/content/jpa/schema-hsqldb.sql")
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
        return EmbeddedJpa.embeddedDataSource(EmbeddedDatabaseType.HSQL);
    }

    @Override
    protected Database jpaDatabase() {
        return Database.HSQL;
    }
}