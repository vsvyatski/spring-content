package internal.org.springframework.content.rest.it.mariadb;

import internal.org.springframework.content.rest.it.boot.MariaDbContentSchemaJdbcConfig;
import internal.org.springframework.content.rest.it.boot.RestIntegrationBootApplication;
import internal.org.springframework.content.rest.it.boot.RestSupportJpaStoresConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Import;

@RestIntegrationBootApplication
@Import({RestSupportJpaStoresConfig.class, MariaDbContentSchemaJdbcConfig.class})
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
