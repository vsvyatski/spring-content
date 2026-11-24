package internal.org.springframework.content.rest.it.http405;

import internal.org.springframework.content.rest.it.boot.H2ContentSchemaJdbcConfig;
import org.springframework.context.annotation.Configuration;

/**
 * @author Vladimir Svyatski
 */
@Configuration
public class Http405ContentSchemaJdbcConfig extends H2ContentSchemaJdbcConfig {

    @Override
    protected String[] entityPackagesToScan() {
        return new String[] {
            "internal.org.springframework.content.rest.scoped.http405",
            "org.springframework.content.test.support.jpa.fixture"
        };
    }
}