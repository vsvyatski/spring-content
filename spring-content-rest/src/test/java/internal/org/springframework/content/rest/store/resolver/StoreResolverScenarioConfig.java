package internal.org.springframework.content.rest.store.resolver;

import org.springframework.content.commons.storeservice.StoreInfo;
import org.springframework.content.fs.config.EnableFileSystemStores;
import org.springframework.content.fs.store.FileSystemContentStore;
import org.springframework.content.jpa.config.EnableJpaStores;
import org.springframework.content.rest.config.ContentRestConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Vladimir Svyatski
 */
@Configuration
@EnableJpaRepositories(basePackages = {
    "internal.org.springframework.content.rest.store.resolver",
    "internal.org.springframework.content.rest.scoped.store.resolver"
}, considerNestedRepositories = true)
@EnableTransactionManagement
@EnableJpaStores(basePackages = "internal.org.springframework.content.rest.scoped.store.resolver")
@EnableFileSystemStores(basePackages = "internal.org.springframework.content.rest.scoped.store.resolver")
public class StoreResolverScenarioConfig {

    @Bean
    public ContentRestConfigurer contentRestConfigurer() {
        return config -> config.addStoreResolver("storeResolverEntities", stores -> {
            for (StoreInfo info : stores) {
                if (info.getImplementation(FileSystemContentStore.class) != null) {
                    return info;
                }
            }
            return null;
        });
    }
}