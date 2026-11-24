package internal.org.springframework.content.rest.it.cache.control;

import internal.org.springframework.content.rest.it.boot.H2ContentSchemaJdbcConfig;
import internal.org.springframework.content.rest.it.boot.RestBootWebConfig;
import internal.org.springframework.content.rest.it.boot.RestIntegrationBootApplication;
import internal.org.springframework.content.rest.it.boot.TemporaryFileSystemResourceLoaderConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Import;

@RestIntegrationBootApplication
@Import({
    RestBootWebConfig.class, H2ContentSchemaJdbcConfig.class,
    CacheControlScenarioConfig.class, TemporaryFileSystemResourceLoaderConfig.class
})
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
