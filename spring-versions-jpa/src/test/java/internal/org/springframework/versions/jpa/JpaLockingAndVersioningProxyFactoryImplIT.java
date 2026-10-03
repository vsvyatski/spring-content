package internal.org.springframework.versions.jpa;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.springframework.aop.Advisor;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.versions.interceptors.OptimisticLockingInterceptor;
import org.springframework.versions.interceptors.PessimisticLockingInterceptor;
import org.springframework.versions.jpa.config.JpaLockingAndVersioningConfig;

import internal.org.springframework.versions.AuthenticationFacade;
import internal.org.springframework.versions.LockingService;

public class JpaLockingAndVersioningProxyFactoryImplIT {

    private JpaLockingAndVersioningProxyFactoryImpl factory;

	private AnnotationConfigApplicationContext context;
	
    private PlatformTransactionManager txn;
    private EntityManager em;
    private LockingService locker;
    private AuthenticationFacade auth;

    private ProxyFactory proxyFactory;

    
    @Nested
    class JpaLockingAndVersioningProxyFactoryImplCases {
        @Nested
        class GivenAContextWithARepositoryAndAStore {
            @Nested
            class Apply {
                @Nested
                class GivenNoExistngAdvise {
                    @BeforeEach
                    void setUp() {
                        context = new AnnotationConfigApplicationContext();
                        context.register(TestConfig.class);
                        context.refresh();

                        txn = context.getBean(PlatformTransactionManager.class);
                        EntityManagerFactory emf = context.getBean(EntityManagerFactory.class);
                        em = emf.createEntityManager();
                        locker = context.getBean(LockingService.class);
                        auth = context.getBean(AuthenticationFacade.class);

                        proxyFactory = new ProxyFactory();

                        factory = new JpaLockingAndVersioningProxyFactoryImpl(context, txn, em, locker, auth);

                        factory.apply(proxyFactory);
                    }

                    @Test
                    void shouldApplyTheTxnAdvice() {
                        Advisor[] advices = proxyFactory.getAdvisors();
                        assertThat(advices.length).isEqualTo(3);
                        assertThat(advices[0].getAdvice()).isInstanceOf(TransactionInterceptor.class);
                        assertThat(advices[1].getAdvice()).isInstanceOf(OptimisticLockingInterceptor.class);
                        assertThat(advices[2].getAdvice()).isInstanceOf(PessimisticLockingInterceptor.class);
                    }

                }

                @Nested
                class GivenAnExistngTxnAdvise {
                    @BeforeEach
                    void setUp() {
                        context = new AnnotationConfigApplicationContext();
                        context.register(TestConfig.class);
                        context.refresh();

                        txn = context.getBean(PlatformTransactionManager.class);
                        EntityManagerFactory emf = context.getBean(EntityManagerFactory.class);
                        em = emf.createEntityManager();
                        locker = context.getBean(LockingService.class);
                        auth = context.getBean(AuthenticationFacade.class);

                        proxyFactory = new ProxyFactory();
                        proxyFactory.addAdvice(new TransactionInterceptor());

                        factory = new JpaLockingAndVersioningProxyFactoryImpl(context, txn, em, locker, auth);

                        factory.apply(proxyFactory);
                    }

                    @Test
                    void shouldNotApplyTheAdviceAgain() {
                        Advisor[] advices = proxyFactory.getAdvisors();
                        assertThat(advices.length).isEqualTo(3);
                        assertThat(advices[0].getAdvice()).isInstanceOf(TransactionInterceptor.class);
                        assertThat(advices[1].getAdvice()).isInstanceOf(OptimisticLockingInterceptor.class);
                        assertThat(advices[2].getAdvice()).isInstanceOf(PessimisticLockingInterceptor.class);
                    }

                }

            }

        }

    }

    
	@Configuration
	@EnableJpaRepositories
	@Import({H2Config.class, JpaLockingAndVersioningConfig.class})
	public static class TestConfig {
	}

	@Configuration
	@EnableTransactionManagement
	public static class H2Config {
		
		@Bean
		public DataSource dataSource() {
			EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
			return builder.setType(EmbeddedDatabaseType.H2).build();
		}

		@Bean
		public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
			HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
			vendorAdapter.setDatabase(Database.H2);
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
}
