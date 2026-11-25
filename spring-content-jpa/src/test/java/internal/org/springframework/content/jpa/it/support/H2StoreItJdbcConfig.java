package internal.org.springframework.content.jpa.it.support;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.content.test.support.EmbeddedJpa;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.vendor.Database;

import javax.sql.DataSource;

/**
 * @author Vladimir Svyatski
 */
@Configuration
public class H2StoreItJdbcConfig extends AbstractStoreItJdbcConfig {

    @Value("classpath:/org/springframework/content/jpa/schema-drop-h2.sql")
    private Resource dropRepositoryTables;

    @Value("classpath:/org/springframework/content/jpa/schema-h2.sql")
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
        return EmbeddedJpa.embeddedDataSource(EmbeddedDatabaseType.H2);
    }

    @Override
    protected Database jpaDatabase() {
        return Database.H2;
    }
}
