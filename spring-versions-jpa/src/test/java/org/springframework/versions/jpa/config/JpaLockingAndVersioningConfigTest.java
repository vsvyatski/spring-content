package org.springframework.versions.jpa.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import static org.assertj.core.api.Assertions.assertThat;

import internal.org.springframework.versions.AuthenticationFacade;
import internal.org.springframework.versions.LockingService;
import internal.org.springframework.versions.jpa.CloningService;
import internal.org.springframework.versions.jpa.EntityInformationFacade;
import internal.org.springframework.versions.jpa.VersioningService;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.versions.LockingAndVersioningProxyFactory;

import jakarta.persistence.EntityManager;
import javax.sql.DataSource;

import static org.mockito.Mockito.mock;

public class JpaLockingAndVersioningConfigTest {

    private AnnotationConfigApplicationContext context;

    
    @Nested
    class JpaLockingAndVersioningConfigCases {
        @BeforeEach
        void setUp() {
            context = new AnnotationConfigApplicationContext();
            context.register(TestConfig.class);
            context.refresh();
        }

        @Test
        void shouldHaveAnAuthenticationFacadeBean() {
            assertThat(context.getBean(AuthenticationFacade.class)).isNotNull();
        }

        @Test
        void shouldHaveAnEntityInformationFacadeBean() {
            assertThat(context.getBean(EntityInformationFacade.class)).isNotNull();
        }

        @Test
        void shouldHaveALockingServiceBean() {
            assertThat(context.getBean(LockingService.class)).isNotNull();
        }

        @Test
        void shouldHaveAVersioningServiceBean() {
            assertThat(context.getBean(VersioningService.class)).isNotNull();
        }

        @Test
        void shouldHaveACloningServiceBean() {
            assertThat(context.getBean(CloningService.class)).isNotNull();
        }

        @Test
        void shouldHaveALockingAndVersioningProxyFactoryBean() {
            assertThat(context.getBean(LockingAndVersioningProxyFactory.class)).isNotNull();
        }

    }


    @Configuration
    @Import(JpaLockingAndVersioningConfig.class)
    public static class TestConfig {

        @Bean
        public DataSource ds() {
            return mock(DataSource.class);
        }

        @Bean
        public PlatformTransactionManager txn() {
            return mock(PlatformTransactionManager.class);
        }

        @Bean
        public EntityManager em() {
            return mock(EntityManager.class);
        }
    }

}
