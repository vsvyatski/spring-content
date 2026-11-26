package internal.org.springframework.content.jpa.config;

import internal.org.springframework.content.jpa.io.DelegatingBlobResourceLoader;
import internal.org.springframework.content.jpa.io.MariaDbBlobResource;
import internal.org.springframework.content.jpa.io.SQLServerBlobResource;
import org.junit.jupiter.api.*;
import org.springframework.content.jpa.config.EnableJpaStores;
import org.springframework.content.jpa.io.BlobResourceLoader;
import org.springframework.content.test.support.EmbeddedJpa;
import org.springframework.content.test.support.TestSupportPackages;
import org.springframework.content.test.support.jpa.DualContentJpaStringIdStore;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

public class EnableJpaStoresTest {

    private AnnotationConfigApplicationContext context;

    @Configuration
    @EnableJpaStores(basePackages = "contains.no.jpa.repositories")
    @Import(InfrastructureConfig.class)
    public static class EmptyConfig {
    }

    @Configuration
    @EnableJpaStores(basePackages = TestSupportPackages.JPA)
    @Import(InfrastructureConfig.class)
    public static class TestConfig {
    }

    @Configuration
    @EnableTransactionManagement
    public static class InfrastructureConfig {
        @Bean
        public DataSource dataSource() {
            return EmbeddedJpa.embeddedDataSource(EmbeddedDatabaseType.HSQL);
        }

        @Bean
        public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
            return EmbeddedJpa.entityManagerFactory(dataSource(), Database.HSQL, TestSupportPackages.JPA);
        }

        @Bean
        public PlatformTransactionManager transactionManager() {
            return EmbeddedJpa.transactionManager(entityManagerFactory().getObject());
        }
    }

    @Nested
    class EnableJpaStoresCases {
        @Nested
        @DisplayName("Given a context and a configuration with a JPA content store bean")
        class ContextAndConfigurationWithJpaContentStoreBean {
            @BeforeEach
            void setUp() {
                context = new AnnotationConfigApplicationContext();
                context.register(TestConfig.class);
                context.refresh();
            }

            @AfterEach
            void tearDown() {
                context.close();
            }

            @Test
            void shouldHaveContentStoreBean() {
                // Dual-content JPA fixture (not SingleContent*): exercises two @ContentId properties in test-jar.
                assertThat(context.getBean(DualContentJpaStringIdStore.class)).isNotNull();
            }

            @Test
            void shouldHaveDelegatingBlobResourceLoader() {
                assertThat(context.getBean(DelegatingBlobResourceLoader.class)).isNotNull();
            }

            @Test
            void shouldHaveGenericBlobResourceLoader() {
                assertThat(context.getBean("genericBlobResourceLoader")).isNotNull();
            }

            @Test
            void shouldHaveMariaDbBlobResourceLoader() {
                BlobResourceLoader loader = (BlobResourceLoader) context.getBean("mariaDbBlobResourceLoader");
                assertThat(loader).isNotNull();
                assertThat(loader.getDatabaseName()).isEqualTo("MariaDB");
                assertThat(loader.getResource("some-id")).isInstanceOf(MariaDbBlobResource.class);
            }

            @Test
            void shouldHaveSQLServerBlobResourceLoader() {
                BlobResourceLoader loader = (BlobResourceLoader) context.getBean("sqlServerBlobResourceLoader");
                assertThat(loader).isNotNull();
                assertThat(loader.getDatabaseName()).isEqualTo("Microsoft SQL Server");
                assertThat(loader.getResource("some-id")).isInstanceOf(SQLServerBlobResource.class);
            }
        }

        @Nested
        @DisplayName("Given a context with an empty configuration")
        class ContextWithEmptyConfiguration {
            @BeforeEach
            void setUp() {
                context = new AnnotationConfigApplicationContext();
                context.register(EmptyConfig.class);
                context.refresh();
            }

            @AfterEach
            void tearDown() {
                context.close();
            }

            @Test
            void shouldNotContainAnyJpaRepositoryBeans() {
                assertThat(context.getBeanNamesForType(DualContentJpaStringIdStore.class)).isEmpty();
            }
        }
    }
}

