package internal.org.springframework.content.jpa.config;

import internal.org.springframework.content.jpa.io.DelegatingBlobResourceLoader;
import internal.org.springframework.content.jpa.io.MySQLBlobResource;
import internal.org.springframework.content.jpa.io.SQLServerBlobResource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.jpa.config.EnableJpaStores;
import org.springframework.content.jpa.io.BlobResourceLoader;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.annotation.Id;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import static org.assertj.core.api.Assertions.assertThat;

public class EnableJpaStoresTest {

    private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

    @SuppressWarnings("unused") // classpath / store component scan
    public interface TestEntityContentRepository
            extends ContentStore<TestEntity, String> {
    }

    @Configuration
    @EnableJpaStores(basePackages = "contains.no.jpa.repositories")
    @Import(InfrastructureConfig.class)
    public static class EmptyConfig {
    }

    @Configuration
    @EnableJpaStores
    @Import(InfrastructureConfig.class)
    public static class TestConfig {
    }

    @Configuration
    @EnableTransactionManagement
    public static class InfrastructureConfig {
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

    @Nested
    class EnableJpaStoresCases {
        @Nested
        class GivenAContextAndAConfigurationWithAJpaContentRepositoryBean {
            @BeforeEach
            void setUp() {
                context.register(TestConfig.class);
                context.refresh();
            }

            @AfterEach
            void tearDown() {
                context.close();
            }

            @Test
            void shouldHaveAContentRepositoryBean() {
                assertThat(context.getBean(TestEntityContentRepository.class)).isNotNull();
            }

            @Test
            void shouldHaveADelegatingBlobResourceLoader() {
                assertThat(context.getBean(DelegatingBlobResourceLoader.class)).isNotNull();
            }

            @Test
            void shouldHaveAGenericBlobResourceLoader() {
                assertThat(context.getBean("genericBlobResourceLoader")).isNotNull();
            }

            @Test
            void shouldHaveAMySQLBlobResourceLoader() {
                BlobResourceLoader loader = (BlobResourceLoader) context.getBean("mysqlBlobResourceLoader");
                assertThat(loader).isNotNull();
                assertThat(loader.getDatabaseName()).isEqualTo("MySQL");
                assertThat(loader.getResource("some-id")).isInstanceOf(MySQLBlobResource.class);
            }

            @Test
            void shouldHaveASQLServerBlobResourceLoader() {
                BlobResourceLoader loader = (BlobResourceLoader) context.getBean("sqlServerBlobResourceLoader");
                assertThat(loader).isNotNull();
                assertThat(loader.getDatabaseName()).isEqualTo("Microsoft SQL Server");
                assertThat(loader.getResource("some-id")).isInstanceOf(SQLServerBlobResource.class);
            }
        }

        @Nested
        class GivenAContextWithAnEmptyConfiguration {
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
                assertThat(context.getBeanNamesForType(TestEntityContentRepository.class)).isEmpty();
            }
        }
    }

    public static class TestEntity {
        @Id
        private String id;
        @ContentId
        private String contentId;
    }
}
