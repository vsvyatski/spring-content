package org.springframework.versions.jpa.boot;

import internal.org.springframework.content.jpa.boot.autoconfigure.JpaContentAutoConfiguration;
import internal.org.springframework.content.s3.boot.autoconfigure.S3ContentAutoConfiguration;
import internal.org.springframework.content.solr.boot.autoconfigure.SolrAutoConfiguration;
import internal.org.springframework.content.solr.boot.autoconfigure.SolrExtensionAutoConfiguration;
import internal.org.springframework.versions.jpa.boot.autoconfigure.JpaVersionsAutoConfiguration;
import internal.org.springframework.versions.jpa.boot.autoconfigure.JpaVersionsDatabaseInitializer;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
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
import org.springframework.versions.AncestorId;
import org.springframework.versions.AncestorRootId;
import org.springframework.versions.LockingAndVersioningRepository;
import org.springframework.versions.SuccessorId;

import javax.sql.DataSource;

public class JpaVersionsAutoConfigurationTest {

    private ApplicationContextRunner contextRunner;

    @Test
    void versionedEntityExposesVersioningPropertyAccessors() {
        TestEntityVersioned entity = new TestEntityVersioned();
        entity.setId(1L);
        entity.setAncestralRootId("root-1");
        entity.setAncestorId("ancestor-1");
        entity.setSuccessorId("successor-1");

        Assertions.assertThat(entity.getId()).isEqualTo(1L);
        Assertions.assertThat(entity.getAncestralRootId()).isEqualTo("root-1");
        Assertions.assertThat(entity.getAncestorId()).isEqualTo("ancestor-1");
        Assertions.assertThat(entity.getSuccessorId()).isEqualTo("successor-1");
    }

    public interface NestedTestEntityRepository extends CrudRepository<TestEntityVersioned, Long>, LockingAndVersioningRepository<TestEntityVersioned> {
    }

    @Entity
    public static class TestEntityVersioned {

        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        private Long id;

        @AncestorRootId
        private String ancestralRootId;

        @AncestorId
        private String ancestorId;

        @SuccessorId
        private String successorId;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getAncestralRootId() {
            return ancestralRootId;
        }

        public void setAncestralRootId(String ancestralRootId) {
            this.ancestralRootId = ancestralRootId;
        }

        public String getAncestorId() {
            return ancestorId;
        }

        public void setAncestorId(String ancestorId) {
            this.ancestorId = ancestorId;
        }

        public String getSuccessorId() {
            return successorId;
        }

        public void setSuccessorId(String successorId) {
            this.successorId = successorId;
        }
    }

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

    @SpringBootApplication(exclude = {
        JpaContentAutoConfiguration.class,
        SolrAutoConfiguration.class,
        SolrExtensionAutoConfiguration.class,
        S3ContentAutoConfiguration.class
    })
    @Import(JpaTestConfig.class)
    public static class StarterConfig /*extends BaseConfig*/ {
    }

    @SpringBootApplication(exclude = {
        JpaContentAutoConfiguration.class,
        SolrAutoConfiguration.class,
        SolrExtensionAutoConfiguration.class,
        S3ContentAutoConfiguration.class
    })
    @EnableJpaRepositories(basePackages = "org.springframework.versions", considerNestedRepositories = true)
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
                        Assertions.assertThat(context).hasSingleBean(NestedTestEntityRepository.class)
                );
            }
        }
    }
}
