package internal.org.springframework.content.jpa.boot.autoconfigure;

import internal.org.springframework.content.jpa.config.JpaStoreConfiguration;
import internal.org.springframework.content.jpa.config.JpaStoreFactoryBean;
import internal.org.springframework.content.jpa.config.JpaStoresRegistrar;
import internal.org.springframework.versions.jpa.boot.autoconfigure.JpaVersionsAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import javax.sql.DataSource;

@AutoConfiguration
@AutoConfigureAfter({DataSourceAutoConfiguration.class, JpaVersionsAutoConfiguration.class})
@ConditionalOnClass({DataSource.class, JpaStoresRegistrar.class})
@ConditionalOnProperty(
        prefix = "spring.content.storage.type",
        name = "default",
        havingValue = "jpa",
        matchIfMissing = true)
@EnableConfigurationProperties(ContentJpaProperties.class)
public class JpaContentAutoConfiguration {

    private final ContentJpaProperties properties;

    public JpaContentAutoConfiguration(ContentJpaProperties properties) {
        this.properties = properties;
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(DataSource.class)
    public ContentJpaDatabaseInitializer contentJpaDatabaseInitializer(DataSource dataSource) {
        return new ContentJpaDatabaseInitializer(dataSource, properties);
    }

    @Bean
    @ConditionalOnMissingBean(name = "copyBufferSize")
    public Integer copyBufferSize() {
        return properties.getCopyBufferSize();
    }

    @Configuration
    @ConditionalOnMissingBean(JpaStoreFactoryBean.class)
    @Import({JpaContentAutoConfigureRegistrar.class, JpaStoreConfiguration.class})
    public static class EnableJpaStoresConfig {
    }
}
