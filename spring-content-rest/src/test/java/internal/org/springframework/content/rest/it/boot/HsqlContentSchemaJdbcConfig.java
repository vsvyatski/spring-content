package internal.org.springframework.content.rest.it.boot;

import internal.org.springframework.content.jpa.it.support.HsqlStoreItJdbcConfig;
import org.springframework.context.annotation.Configuration;

/**
 * @author Vladimir Svyatski
 */
@Configuration
public class HsqlContentSchemaJdbcConfig extends HsqlStoreItJdbcConfig {

    @Override
    protected String[] entityPackagesToScan() {
        return new String[]{RestItEntityPackages.REST_SUPPORT};
    }
}
