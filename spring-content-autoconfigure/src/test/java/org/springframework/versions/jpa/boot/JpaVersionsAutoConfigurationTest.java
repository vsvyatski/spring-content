package org.springframework.versions.jpa.boot;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Disabled;
import static org.assertj.core.api.Assertions.assertThat;

import internal.org.springframework.content.s3.boot.autoconfigure.S3ContentAutoConfiguration;
import internal.org.springframework.content.solr.boot.autoconfigure.SolrAutoConfiguration;
import internal.org.springframework.content.solr.boot.autoconfigure.SolrExtensionAutoConfiguration;
import internal.org.springframework.versions.jpa.boot.autoconfigure.JpaVersionsAutoConfiguration;
import internal.org.springframework.versions.jpa.boot.autoconfigure.JpaVersionsDatabaseInitializer;
import org.assertj.core.api.Assertions;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.CrudRepository;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.versions.LockingAndVersioningRepository;

import javax.sql.DataSource;

public class JpaVersionsAutoConfigurationTest {

    private AnnotationConfigApplicationContext context;
    private ApplicationContextRunner contextRunner;

    
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
                contextRunner.withUserConfiguration(StarterConfig.class).run((context) -> {
                    Assertions.assertThat(context).hasSingleBean(JpaVersionsDatabaseInitializer.class);
                });

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
                contextRunner.withUserConfiguration(StarterWithAnnotationConfig.class).run((context) -> {
                    Assertions.assertThat(context).hasSingleBean(NestedTestEntityRepository.class);
                });

            }

        }

    }

    @Test
    public void test() {
    }

    @Disabled("This is not a test")
    @Configuration
    public static class JpaTestConfig {
        @Bean
        public DataSource dataSource() {
            EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
            return builder.setType(EmbeddedDatabaseType.HSQL).build();
        }

        @Bean
        public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
            HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
            vendorAdapter.setDatabase(Database.HSQL);
            vendorAdapter.setGenerateDdl(true);

            LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
            factory.setJpaVendorAdapter(vendorAdapter);
            factory.setPackagesToScan(getClass().getPackage().getName());
            factory.setDataSource(dataSource());

            return factory;
        }

        @Bean
        public PlatformTransactionManager transactionManager() {
            JpaTransactionManager txManager = new JpaTransactionManager();
            txManager.setEntityManagerFactory(entityManagerFactory().getObject());
            return txManager;
        }
    }

    @Disabled("This is not a test")
    @SpringBootApplication(exclude = {SolrAutoConfiguration.class, SolrExtensionAutoConfiguration.class, S3ContentAutoConfiguration.class})
    @Import(JpaTestConfig.class)
    public static class StarterConfig /*extends BaseConfig*/ {
    }

    @Disabled("This is not a test")
    @SpringBootApplication(exclude = {SolrAutoConfiguration.class, SolrExtensionAutoConfiguration.class, S3ContentAutoConfiguration.class})
    @EnableJpaRepositories(basePackages = "org.springframework.versions",
            considerNestedRepositories = true)
    @Import(JpaTestConfig.class)
    public static class StarterWithAnnotationConfig /*extends BaseConfig*/ {
    }

    public interface NestedTestEntityRepository extends CrudRepository<TestEntityVersioned, Long>, LockingAndVersioningRepository<TestEntityVersioned> {
    }
}
