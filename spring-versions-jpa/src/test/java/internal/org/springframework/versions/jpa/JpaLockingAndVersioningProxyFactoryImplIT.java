package internal.org.springframework.versions.jpa;

import internal.org.springframework.versions.AuthenticationFacade;
import internal.org.springframework.versions.LockingService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.aop.Advisor;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.content.test.support.EmbeddedJpa;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.versions.interceptors.OptimisticLockingInterceptor;
import org.springframework.versions.interceptors.PessimisticLockingInterceptor;
import org.springframework.versions.jpa.config.JpaLockingAndVersioningConfig;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

public class JpaLockingAndVersioningProxyFactoryImplIT {

    private JpaLockingAndVersioningProxyFactoryImpl factory;

    private AnnotationConfigApplicationContext context;

    private PlatformTransactionManager txn;
    private EntityManager em;
    private LockingService locker;
    private AuthenticationFacade auth;

    private ProxyFactory proxyFactory;

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
            return EmbeddedJpa.embeddedDataSource(EmbeddedDatabaseType.H2);
        }

        @Bean
        public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
            return EmbeddedJpa.entityManagerFactory(dataSource(), Database.H2, getClass().getPackage().getName());
        }

        @Bean
        public PlatformTransactionManager transactionManager() {
            return EmbeddedJpa.transactionManager(entityManagerFactory().getObject());
        }
    }

    @Nested
    @DisplayName("Given a context with a repository and a store")
    class ContextWithRepositoryAndStore {
        @Nested
        class Apply {
            @Nested
            @DisplayName("Given no existing advise")
            class NoExistingAdvise {
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
                void shouldApplyTxnAdvice() {
                    Advisor[] advices = proxyFactory.getAdvisors();
                    assertThat(advices.length).isEqualTo(3);
                    assertThat(advices[0].getAdvice()).isInstanceOf(TransactionInterceptor.class);
                    assertThat(advices[1].getAdvice()).isInstanceOf(OptimisticLockingInterceptor.class);
                    assertThat(advices[2].getAdvice()).isInstanceOf(PessimisticLockingInterceptor.class);
                }
            }

            @Nested
            @DisplayName("Given an existing txn advise")
            class ExistingTxnAdvise {
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
                void shouldNotApplyAdviceAgain() {
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
