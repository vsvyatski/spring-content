package internal.org.springframework.content.rest.it.http405;

import org.springframework.content.fs.config.EnableFileSystemStores;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * @author Vladimir Svyatski
 */
@Configuration
@EnableJpaRepositories(
    basePackages = "internal.org.springframework.content.rest.scoped.http405",
    considerNestedRepositories = true)
@EnableFileSystemStores(basePackages = "internal.org.springframework.content.rest.scoped.http405")
public class Http405ScenarioConfig {
}