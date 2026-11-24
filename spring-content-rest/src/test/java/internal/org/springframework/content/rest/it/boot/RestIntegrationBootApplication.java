package internal.org.springframework.content.rest.it.boot;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.data.mongodb.autoconfigure.DataMongoAutoConfiguration;
import org.springframework.boot.data.mongodb.autoconfigure.DataMongoRepositoriesAutoConfiguration;
import org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration;

import java.lang.annotation.*;

/**
 * Shared {@link SpringBootApplication} defaults for REST integration Boot tests (Mongo auto-config off).
 *
 * @author Vladimir Svyatski
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@SpringBootApplication(exclude = {
    MongoAutoConfiguration.class, DataMongoAutoConfiguration.class, DataMongoRepositoriesAutoConfiguration.class
})
public @interface RestIntegrationBootApplication {
}
