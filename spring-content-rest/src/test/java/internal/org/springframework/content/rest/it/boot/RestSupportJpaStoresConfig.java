package internal.org.springframework.content.rest.it.boot;

import internal.org.springframework.content.rest.it.SecurityConfiguration;
import org.springframework.content.jpa.config.EnableJpaStores;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;

/**
 * JPA content stores and REST for {@code internal.org.springframework.content.rest.support} (dialect REST ITs).
 *
 * @author Vladimir Svyatski
 */
@Configuration
@Import({
    RestConfiguration.class, SecurityConfiguration.class, DelegatingWebMvcConfiguration.class,
    RepositoryRestMvcConfiguration.class
})
@EnableJpaRepositories(basePackages = "internal.org.springframework.content.rest.support")
@EnableTransactionManagement
@EnableJpaStores(basePackages = "internal.org.springframework.content.rest.support")
public class RestSupportJpaStoresConfig {
}