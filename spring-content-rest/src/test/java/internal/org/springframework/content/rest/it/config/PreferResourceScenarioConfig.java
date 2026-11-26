package internal.org.springframework.content.rest.it.config;

import internal.org.springframework.content.rest.support.UuidSingleContentEntity;
import internal.org.springframework.content.rest.support.mock.store.EnableMockStores;
import org.springframework.content.rest.config.ContentRestConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * @author Vladimir Svyatski
 */
@Configuration
@EnableJpaRepositories(basePackages = "internal.org.springframework.content.rest.it.config",
    considerNestedRepositories = true)
@EnableMockStores(basePackages = "internal.org.springframework.content.rest.it.config")
public class PreferResourceScenarioConfig {

    @Bean
    public ContentRestConfigurer configurer() {
        return config -> config.forDomainType(UuidSingleContentEntity.class).putAndPostPreferResource();
    }
}
