package internal.org.springframework.content.rest.it.boot;

import internal.org.springframework.content.jpa.it.support.PostgresStoreItJdbcConfig;
import org.springframework.context.annotation.Configuration;

/**
 * @author Vladimir Svyatski
 */
@Configuration
public class PostgresContentSchemaJdbcConfig extends PostgresStoreItJdbcConfig {

    @Override
    protected String[] entityPackagesToScan() {
        return new String[]{RestItEntityPackages.REST_SUPPORT};
    }
}
