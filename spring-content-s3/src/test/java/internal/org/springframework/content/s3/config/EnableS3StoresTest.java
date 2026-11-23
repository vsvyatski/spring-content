package internal.org.springframework.content.s3.config;

import internal.org.springframework.content.s3.io.S3StoreResource;
import internal.org.springframework.content.s3.it.LocalStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.content.s3.config.EnableS3Stores;
import org.springframework.content.s3.config.MultiTenantS3ClientProvider;
import org.springframework.content.s3.config.S3StoreConfigurer;
import org.springframework.content.test.support.SingleContentFields;
import org.springframework.content.test.support.TestSupportPackages;
import org.springframework.content.test.support.s3.SingleContentS3Store;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URISyntaxException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class EnableS3StoresTest {

    private static final String BUCKET = "aws-test-bucket";

    static {
        System.setProperty("spring.content.s3.bucket", BUCKET);
    }

    private S3StoreConfigurer configurer;
    private S3Client client;
    private AnnotationConfigApplicationContext context;

    @Configuration
    @EnableS3Stores(basePackages = "contains.no.s3.repositories")
    @Import(InfrastructureConfig.class)
    public static class EmptyConfig {
    }

    @Configuration
    @EnableS3Stores(basePackages = TestSupportPackages.S3)
    @Import(InfrastructureConfig.class)
    public static class TestConfig {
    }

    @Configuration
    @EnableS3Stores(basePackages = TestSupportPackages.S3)
    @Import(InfrastructureConfig.class)
    public static class ConverterConfig {
        @Bean
        public S3StoreConfigurer configurer() {
            return mock(S3StoreConfigurer.class);
        }
    }

    @Configuration
    @EnableS3Stores(basePackages = TestSupportPackages.S3)
    @Import(InfrastructureConfig.class)
    public static class MultiTenantConfig {
        @Bean("multiTenantClient")
        public S3Client multiTenantClient() {
            return mock(S3Client.class);
        }

        @Bean
        public MultiTenantS3ClientProvider s3Provider(
            @Qualifier("multiTenantClient") S3Client multiTenantClient) {
            return () -> multiTenantClient;
        }
    }

    @Configuration
    public static class InfrastructureConfig {

        @Autowired
        private Environment env;

        @Bean
        public S3Client client() throws URISyntaxException {
            return LocalStack.getAmazonS3Client();
        }
    }

    @Nested
    class EnableS3StoresCases {
        @Nested
        @DisplayName("Given a context and a configuration with an s3content store bean")
        class ContextAndConfigurationWithS3ContentStoreBean {
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
                assertThat(context.getBean(SingleContentS3Store.class)).isNotNull();
            }

            @Test
            void shouldHavePlacementService() {
                assertThat(context.getBean("s3StorePlacementService")).isNotNull();
            }
        }

        @Nested
        @DisplayName("Given a context with a configurer")
        class ContextWithConfigurer {
            @BeforeEach
            void setUp() {
                context = new AnnotationConfigApplicationContext();
                context.register(ConverterConfig.class);
                context.refresh();
                configurer = context.getBean(S3StoreConfigurer.class);
            }

            @AfterEach
            void tearDown() {
                context.close();
            }

            @Test
            void shouldCallThatConfigurerToHelpSetupStore() {
                verify(configurer).configureS3StoreConverters(any(ConverterRegistry.class));
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
            void shouldNotContainsAnyS3RepositoryBeans() {
                assertThat(context.getBeanNamesForType(SingleContentS3Store.class)).isEmpty();
            }
        }

        @Nested
        @DisplayName("Given a context with a multi tenant configuration")
        class ContextWithMultiTenantConfiguration {
            @BeforeEach
            void setUp() {
                context = new AnnotationConfigApplicationContext();
                context.register(MultiTenantConfig.class);
                context.refresh();
                client = context.getBean("multiTenantClient", S3Client.class);
            }

            @AfterEach
            void tearDown() {
                context.close();
            }

            @Test
            void shouldUseCorrectClient() {
                SingleContentS3Store repo = context.getBean(SingleContentS3Store.class);
                SingleContentFields entity = new SingleContentFields();
                entity.setContentId("12345");
                Resource r = repo.getResource(entity);
                assertThat(((S3StoreResource) r).getClient()).isEqualTo(client);
            }
        }
    }

}

