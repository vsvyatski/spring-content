package org.springframework.versions.jpa.boot;

import internal.org.springframework.content.jpa.boot.autoconfigure.JpaContentAutoConfiguration;
import internal.org.springframework.content.s3.boot.autoconfigure.S3ContentAutoConfiguration;
import internal.org.springframework.content.solr.boot.autoconfigure.SolrAutoConfiguration;
import internal.org.springframework.content.solr.boot.autoconfigure.SolrExtensionAutoConfiguration;
import internal.org.springframework.versions.jpa.boot.autoconfigure.JpaVersionsAutoConfiguration;
import internal.org.springframework.versions.jpa.boot.autoconfigure.JpaVersionsDatabaseInitializer;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.content.test.support.EmbeddedJpa;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.versions.test.support.VersioningTestPackages;
import org.springframework.versions.test.support.jpa.VersioningJpaLongIdRepository;

import javax.sql.DataSource;

public class JpaVersionsAutoConfigurationTest {

    private ApplicationContextRunner contextRunner;

    @Configuration
    public static class JpaTestConfig {
        @Bean
        public DataSource dataSource() {
            return EmbeddedJpa.embeddedDataSource(EmbeddedDatabaseType.HSQL);
        }

        @Bean
        public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
            return EmbeddedJpa.entityManagerFactory(dataSource(), Database.HSQL, VersioningTestPackages.JPA);
        }

        @Bean
        public PlatformTransactionManager transactionManager() {
            return EmbeddedJpa.transactionManager(entityManagerFactory().getObject());
        }
    }

    @SpringBootApplication(exclude = {
        JpaContentAutoConfiguration.class, SolrAutoConfiguration.class,
        SolrExtensionAutoConfiguration.class, S3ContentAutoConfiguration.class
    })
    @Import(JpaTestConfig.class)
    public static class StarterConfig /*extends BaseConfig*/ {
    }

    @SpringBootApplication(exclude = {
        JpaContentAutoConfiguration.class, SolrAutoConfiguration.class,
        SolrExtensionAutoConfiguration.class, S3ContentAutoConfiguration.class
    })
    @EnableJpaRepositories(basePackages = "org.springframework.versions")
    @Import(JpaTestConfig.class)
    public static class StarterWithAnnotationConfig /*extends BaseConfig*/ {
    }

    @Nested
    class JpaVersionsAutoConfigurationCases {
        @Nested
        class GivenAnApplicationContextThatReliesOnAutoConfiguration {
            @BeforeEach
            void setUp() {
                contextRunner = new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(JpaVersionsAutoConfiguration.class));
            }

            @Test
            void shouldIncludeTheRepositoryBean() {
                contextRunner.withUserConfiguration(StarterConfig.class).run((context) -> Assertions.assertThat(context).hasSingleBean(JpaVersionsDatabaseInitializer.class));
            }
        }

        @Nested
        class GivenAnApplicationContextWithAEnableJpaRepositoriesAnnotation {
            @BeforeEach
            void setUp() {
                contextRunner = new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(JpaVersionsAutoConfiguration.class));
            }

            @Test
            void shouldIncludeTheRepositoryBean() {
                contextRunner.withUserConfiguration(StarterWithAnnotationConfig.class).run(
                    (context) ->
                        Assertions.assertThat(context).hasSingleBean(VersioningJpaLongIdRepository.class)
                );
            }
        }
    }
}
