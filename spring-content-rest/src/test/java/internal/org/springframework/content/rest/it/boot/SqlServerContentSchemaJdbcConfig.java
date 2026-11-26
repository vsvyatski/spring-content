package internal.org.springframework.content.rest.it.boot;

import internal.org.springframework.content.jpa.it.support.SqlServerStoreItJdbcConfig;
import org.springframework.context.annotation.Configuration;

/**
 * @author Vladimir Svyatski
 */
@Configuration
public class SqlServerContentSchemaJdbcConfig extends SqlServerStoreItJdbcConfig {

    @Override
    protected String[] entityPackagesToScan() {
        return new String[]{RestItEntityPackages.REST_SUPPORT};
    }
}
