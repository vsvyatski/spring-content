package internal.org.springframework.content.rest.it.sqlserver;

import internal.org.springframework.content.rest.it.boot.RestIntegrationBootApplication;
import internal.org.springframework.content.rest.it.boot.RestSupportJpaStoresConfig;
import internal.org.springframework.content.rest.it.boot.SqlServerContentSchemaJdbcConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Import;

@RestIntegrationBootApplication
@Import({RestSupportJpaStoresConfig.class, SqlServerContentSchemaJdbcConfig.class})
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
