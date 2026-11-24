package internal.org.springframework.content.rest.it.cache.control;

import org.springframework.content.fs.config.EnableFileSystemStores;
import org.springframework.content.rest.config.ContentRestConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.CacheControl;

import java.time.Duration;

/**
 * @author Vladimir Svyatski
 */
@Configuration
@EnableJpaRepositories(basePackages = "internal.org.springframework.content.rest.it.cache.control",
    considerNestedRepositories = true)
@EnableFileSystemStores(basePackages = "internal.org.springframework.content.rest.it.cache.control")
public class CacheControlScenarioConfig {

    @Bean
    public ContentRestConfigurer configurer() {
        return config -> config
            .cacheControl()
            .antMatcher("/uuidSingleContentEntities/*", CacheControl.maxAge(Duration.ofSeconds(60)));
    }
}