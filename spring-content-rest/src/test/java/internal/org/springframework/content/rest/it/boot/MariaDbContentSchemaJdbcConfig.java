package internal.org.springframework.content.rest.it.boot;

import internal.org.springframework.content.jpa.it.support.MariaDbStoreItJdbcConfig;
import org.springframework.context.annotation.Configuration;

/**
 * @author Vladimir Svyatski
 */
@Configuration
public class MariaDbContentSchemaJdbcConfig extends MariaDbStoreItJdbcConfig {

    @Override
    protected String[] entityPackagesToScan() {
        return new String[]{RestItEntityPackages.REST_SUPPORT};
    }
}
