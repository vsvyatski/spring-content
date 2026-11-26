package internal.org.springframework.content.rest.it.http405;

import internal.org.springframework.content.rest.it.boot.RestBootWebConfig;
import internal.org.springframework.content.rest.it.boot.RestIntegrationBootApplication;
import internal.org.springframework.content.rest.it.boot.TemporaryFileSystemResourceLoaderConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Import;

@RestIntegrationBootApplication
@Import({
    RestBootWebConfig.class,
    Http405ContentSchemaJdbcConfig.class,
    Http405ScenarioConfig.class,
    TemporaryFileSystemResourceLoaderConfig.class
})
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
