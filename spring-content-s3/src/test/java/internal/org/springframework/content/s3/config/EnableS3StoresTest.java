package internal.org.springframework.content.s3.config;

import internal.org.springframework.content.s3.io.S3StoreResource;
import internal.org.springframework.content.s3.it.LocalStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.store.AssociativeStore;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.s3.S3ObjectId;
import org.springframework.content.s3.config.EnableS3Stores;
import org.springframework.content.s3.config.MultiTenantS3ClientProvider;
import org.springframework.content.s3.config.S3StoreConfigurer;
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
    // mocks
    static S3StoreConfigurer configurer;
    static S3Client client;

    static {
        System.setProperty("spring.content.s3.bucket", BUCKET);
    }

    private AnnotationConfigApplicationContext context;

    @Test
    public void noop() {
    }

    public interface TestEntityStore extends AssociativeStore<TestEntity, S3ObjectId> {
    }

    public interface TestEntityContentRepository
            extends ContentStore<TestEntity, String> {
    }

    @Configuration
    @EnableS3Stores(basePackages = "contains.no.fs.repositores")
    @Import(InfrastructureConfig.class)
    public static class EmptyConfig {
    }

    @Configuration
    @EnableS3Stores
    @Import(InfrastructureConfig.class)
    public static class TestConfig {
    }

    @Configuration
    @EnableS3Stores
    @Import(InfrastructureConfig.class)
    public static class ConverterConfig {
        @Bean
        public S3StoreConfigurer configurer() {
            return configurer;
        }
    }

    @Configuration
    @EnableS3Stores
    @Import(InfrastructureConfig.class)
    public static class TestConverterConfig {
        @Bean
        public S3StoreConfigurer configurer() {
            return new S3StoreConfigurer() {

                @Override
                public void configureS3StoreConverters(ConverterRegistry registry) {
                }
            };
        }
    }

    @Configuration
    @EnableS3Stores
    @Import(InfrastructureConfig.class)
    public static class MultiTenantConfig {
        @Bean
        public MultiTenantS3ClientProvider s3Provider() {
            return new MultiTenantS3ClientProvider() {
                @Override
                public S3Client getS3Client() {
                    return client;
                }
            };
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
        class GivenAContextAndAConfigurationWithAnS3ContentRepositoryBean {
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
            void shouldHaveAContentRepositoryBean() {
                assertThat(context.getBean(TestEntityContentRepository.class)).isNotNull();
            }

            @Test
            void shouldHaveAnPlacementService() {
                assertThat(context.getBean("s3StorePlacementService")).isNotNull();
            }
        }

        @Nested
        class GivenAContextWithAConfigurer {
            @BeforeEach
            void setUp() {
                configurer = mock(S3StoreConfigurer.class);

                context = new AnnotationConfigApplicationContext();
                context.register(ConverterConfig.class);
                context.refresh();
            }

            @AfterEach
            void tearDown() {
                context.close();
            }

            @Test
            void shouldCallThatConfigurerToHelpSetupTheStore() {
                verify(configurer).configureS3StoreConverters(any(ConverterRegistry.class));
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
            void shouldNotContainsAnyS3RepositoryBeans() {
                assertThat(context.getBeanNamesForType(TestEntityContentRepository.class)).isEmpty();
            }
        }

        @Nested
        class GivenAContextWithAMultiTenantConfiguration {
            @BeforeEach
            void setUp() {
                // noinspection resource
                client = mock(S3Client.class);

                context = new AnnotationConfigApplicationContext();
                context.register(MultiTenantConfig.class);
                context.refresh();
            }

            @AfterEach
            void tearDown() {
                context.close();
            }

            @Test
            void shouldUseTheCorrectClient() {
                TestEntityContentRepository repo = context.getBean(TestEntityContentRepository.class);
                TestEntity tentity = new TestEntity();
                tentity.setContentId("12345");
                Resource r = repo.getResource(tentity);
                assertThat(((S3StoreResource) r).getClient()).isEqualTo(client);
            }
        }
    }

    public class TestEntity {
        @ContentId
        private String contentId;

        public String getContentId() {
            return contentId;
        }

        public void setContentId(String contentId) {
            this.contentId = contentId;
        }
    }
}
