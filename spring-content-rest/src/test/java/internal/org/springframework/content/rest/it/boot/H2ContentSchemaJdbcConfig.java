package internal.org.springframework.content.rest.it.boot;

import internal.org.springframework.content.jpa.it.support.H2StoreItJdbcConfig;
import org.springframework.context.annotation.Configuration;

/**
 * H2 embedded database and H2 Spring Content schema scripts.
 *
 * @author Vladimir Svyatski
 */
@Configuration
public class H2ContentSchemaJdbcConfig extends H2StoreItJdbcConfig {

    @Override
    protected String[] entityPackagesToScan() {
        return new String[]{RestItEntityPackages.REST_SUPPORT};
    }
}
