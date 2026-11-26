package internal.org.springframework.content.rest.it.boot;

import internal.org.springframework.content.rest.it.SecurityConfiguration;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * REST MVC and permissive test security for integration Boot tests.
 *
 * @author Vladimir Svyatski
 */
@Configuration
@Import({RestConfiguration.class, SecurityConfiguration.class})
public class RestBootWebConfig {
}