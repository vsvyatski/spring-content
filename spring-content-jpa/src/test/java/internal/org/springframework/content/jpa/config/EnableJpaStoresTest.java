package internal.org.springframework.content.jpa.config;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.InputStream;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import javax.sql.DataSource;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.commons.store.GetResourceParams;
import org.springframework.content.commons.store.SetContentParams;
import org.springframework.content.commons.store.UnsetContentParams;
import org.springframework.content.jpa.config.EnableJpaStores;
import org.springframework.content.jpa.io.BlobResourceLoader;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.Resource;
import org.springframework.data.annotation.Id;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import internal.org.springframework.content.jpa.io.DelegatingBlobResourceLoader;
import internal.org.springframework.content.jpa.io.MySQLBlobResource;
import internal.org.springframework.content.jpa.io.SQLServerBlobResource;

public class EnableJpaStoresTest {

	private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

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
                BlobResourceLoader loader = (BlobResourceLoader)context.getBean("mysqlBlobResourceLoader");
                						assertThat(loader).isNotNull();
                						assertThat(loader.getDatabaseName()).isEqualTo("MySQL");
                						assertThat(loader.getResource("some-id")).isInstanceOf(MySQLBlobResource.class);
            }
            @Test
            void shouldHaveASQLServerBlobResourceLoader() {
                BlobResourceLoader loader = (BlobResourceLoader)context.getBean("sqlServerBlobResourceLoader");
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
                try {
                						context.getBean(TestEntityContentRepository.class);
                						fail("expected no such bean");
                					}
                					catch (NoSuchBeanDefinitionException e) {
                						assertThat(true).isTrue();
                					}
            }
        }
    }

	@Test
	public void noop() {
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

	public class TestEntity {
		@Id
		private String id;
		@ContentId
		private String contentId;
	}

	public interface TestEntityContentRepository
			extends ContentStore<TestEntity, String> {
	}

	@Repository
	public class JpaTestContentRepository implements TestEntityContentRepository {

		@PersistenceContext
		private EntityManager em;

		@Override
		public TestEntity setContent(TestEntity property, InputStream content) {
			return null;
		}

		@Override
		public TestEntity setContent(TestEntity property, Resource resourceContent) {
			return null;
		}

		@Override
		public TestEntity unsetContent(TestEntity property) {
			return null;
		}

		@Override
		public InputStream getContent(TestEntity property) {
			return null;
		}

		@Override
		public Resource getResource(TestEntity entity) {
			return null;
		}

		@Override
		public void associate(TestEntity entity, String id) {
		}

		@Override
		public void unassociate(TestEntity entity) {
		}

		@Override
		public Resource getResource(String id) {
			return null;
		}

        @Override
        public Resource getResource(TestEntity entity, PropertyPath propertyPath) {
            // TODO Auto-generated method stub
            return null;
        }

		@Override
		public Resource getResource(TestEntity entity, PropertyPath propertyPath, GetResourceParams params) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
        public void associate(TestEntity entity, PropertyPath propertyPath, String id) {
            // TODO Auto-generated method stub
        }

        @Override
        public void unassociate(TestEntity entity, PropertyPath propertyPath) {
            // TODO Auto-generated method stub
        }

        @Override
        public TestEntity setContent(TestEntity property, PropertyPath propertyPath, InputStream contentm) {
            // TODO Auto-generated method stub
            return null;
        }

		@Override
		public TestEntity setContent(TestEntity entity, PropertyPath propertyPath, InputStream content, long contentLen) {
			return null;
		}

		@Override
		public TestEntity setContent(TestEntity entity, PropertyPath propertyPath, InputStream content, SetContentParams params) {
			return null;
		}

		@Override
        public TestEntity setContent(TestEntity property, PropertyPath propertyPath, Resource resourceContent) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public TestEntity unsetContent(TestEntity property, PropertyPath propertyPath) {
            // TODO Auto-generated method stub
            return null;
        }

		@Override
		public TestEntity unsetContent(TestEntity entity, PropertyPath propertyPath, UnsetContentParams params) {
			return null;
		}

		@Override
        public InputStream getContent(TestEntity property, PropertyPath propertyPath) {
            // TODO Auto-generated method stub
            return null;
        }
	}
}
