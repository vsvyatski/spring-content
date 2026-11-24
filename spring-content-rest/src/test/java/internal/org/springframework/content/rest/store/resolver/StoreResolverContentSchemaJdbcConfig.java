package internal.org.springframework.content.rest.store.resolver;

import internal.org.springframework.content.rest.it.boot.H2ContentSchemaJdbcConfig;
import org.springframework.context.annotation.Configuration;

/**
 * @author Vladimir Svyatski
 */
@Configuration
public class StoreResolverContentSchemaJdbcConfig extends H2ContentSchemaJdbcConfig {

    @Override
    protected String[] entityPackagesToScan() {
        return new String[]{"org.springframework.content.test.support.jpa.fixture"};
    }
}
