package internal.org.springframework.content.rest.it.h2;

import internal.org.springframework.content.rest.it.boot.H2ContentSchemaJdbcConfig;
import internal.org.springframework.content.rest.it.boot.RestIntegrationBootApplication;
import internal.org.springframework.content.rest.it.boot.RestSupportJpaStoresConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Import;

@RestIntegrationBootApplication
@Import({RestSupportJpaStoresConfig.class, H2ContentSchemaJdbcConfig.class})
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}