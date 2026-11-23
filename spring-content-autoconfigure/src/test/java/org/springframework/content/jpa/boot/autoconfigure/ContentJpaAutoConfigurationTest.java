package org.springframework.content.jpa.boot.autoconfigure;

import internal.org.springframework.content.jpa.boot.autoconfigure.ContentJpaDatabaseInitializer;
import internal.org.springframework.content.jpa.boot.autoconfigure.ContentJpaProperties;
import internal.org.springframework.content.jpa.boot.autoconfigure.JpaContentAutoConfiguration;
import internal.org.springframework.content.s3.boot.autoconfigure.S3ContentAutoConfiguration;
import internal.org.springframework.content.solr.boot.autoconfigure.SolrAutoConfiguration;
import internal.org.springframework.content.solr.boot.autoconfigure.SolrExtensionAutoConfiguration;
import internal.org.springframework.versions.jpa.boot.autoconfigure.JpaVersionsAutoConfiguration;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.content.jpa.config.EnableJpaStores;
import org.springframework.content.jpa.store.JpaContentStore;
import org.springframework.content.test.support.EmbeddedJpa;
import org.springframework.content.test.support.jpa.SingleContentJpaLongIdEntity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

import static org.mockito.Mockito.mock;

public class ContentJpaAutoConfigurationTest {

    // mocks
    private static ContentJpaDatabaseInitializer initializer;

    private ApplicationContextRunner contextRunner;

    public interface TestEntityContentStore extends JpaContentStore<SingleContentJpaLongIdEntity, String> {
    }

    @Configuration
    public static class JpaTestConfig {
        @Bean
        public DataSource dataSource() {
            return EmbeddedJpa.embeddedDataSource(EmbeddedDatabaseType.HSQL);
        }

        @Bean
        public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
            return EmbeddedJpa.entityManagerFactory(dataSource(), Database.HSQL, getClass().getPackage().getName());
        }

        @Bean
        public PlatformTransactionManager transactionManager() {
            return EmbeddedJpa.transactionManager(entityManagerFactory().getObject());
        }
    }

    @SpringBootApplication(exclude = {JpaVersionsAutoConfiguration.class, SolrAutoConfiguration.class, SolrExtensionAutoConfiguration.class, S3ContentAutoConfiguration.class})
    @Import(JpaTestConfig.class)
    @PropertySource("classpath:/default.properties")
    public static class TestConfig {
    }

    @SpringBootApplication(exclude = {JpaVersionsAutoConfiguration.class, SolrAutoConfiguration.class, SolrExtensionAutoConfiguration.class, S3ContentAutoConfiguration.class})
    @Import(JpaTestConfig.class)
    public static class CustomBeanConfig extends TestConfig {
        @Bean
        public ContentJpaDatabaseInitializer initializer() {
            return initializer;
        }

        @Bean
        @SuppressWarnings("ALL")
        public Integer copyBufferSize() {
            return 16192;
        }
    }

    @SpringBootApplication(exclude = {JpaVersionsAutoConfiguration.class, SolrAutoConfiguration.class, SolrExtensionAutoConfiguration.class, S3ContentAutoConfiguration.class})
    @Import(JpaTestConfig.class)
    @EnableJpaStores
    public static class ConfigWithExplicitEnableJpaStores {
    }

    @SpringBootApplication(exclude = {JpaVersionsAutoConfiguration.class, SolrAutoConfiguration.class, SolrExtensionAutoConfiguration.class, S3ContentAutoConfiguration.class})
    @Import(JpaTestConfig.class)
    @PropertySource("classpath:/custom-jpa.properties")
    public static class CustomPropertiesConfig {
    }

    @Nested
   class ContentJpaAutoConfiguration {
        @BeforeEach
        void setUp() {
            initializer = mock(ContentJpaDatabaseInitializer.class);
            contextRunner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(JpaContentAutoConfiguration.class));
        }

        @Test
        void shouldHaveAContentStore() {
            contextRunner.withUserConfiguration(TestConfig.class).run((context) -> {
                Assertions.assertThat(context).hasSingleBean(TestEntityContentStore.class);
                Assertions.assertThat(context).hasSingleBean(ContentJpaDatabaseInitializer.class);
                Assertions.assertThat(context).hasBean("copyBufferSize");
            });
        }

        @Nested

        @DisplayName("When a custom bean configuration is used")

        class CustomBeanConfigurationIsUsed {
            @BeforeEach
            void setUp() {
                initializer = mock(ContentJpaDatabaseInitializer.class);
                contextRunner = new ApplicationContextRunner();
            }

            @Test
            void shouldUseTheSuppliedCustomBean() {
                contextRunner.withUserConfiguration(CustomBeanConfig.class).run((context) -> {
                    Assertions.assertThat(context).getBean(ContentJpaDatabaseInitializer.class).isEqualTo(initializer);
                    Assertions.assertThat(context).getBean("copyBufferSize").isEqualTo(16192);
                });
            }
        }

        @Nested

        @DisplayName("When an explicit enable file system stores is used")

        class ExplicitEnableFileSystemStoresIsUsed {
            @BeforeEach
            void setUp() {
                initializer = mock(ContentJpaDatabaseInitializer.class);
                contextRunner = new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(JpaContentAutoConfiguration.class));
            }

            @Test
            void shouldLoadTheContext() {
                contextRunner.withUserConfiguration(ConfigWithExplicitEnableJpaStores.class).run((context) -> {
                    Assertions.assertThat(context).hasSingleBean(TestEntityContentStore.class);
                    Assertions.assertThat(context).hasSingleBean(ContentJpaProperties.class);
                    Assertions.assertThat(context).hasSingleBean(ContentJpaDatabaseInitializer.class);
                    Assertions.assertThat(context).hasBean("copyBufferSize");
                });
            }
        }
    }
}
