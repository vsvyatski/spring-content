package internal.org.springframework.content.azure.config;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import internal.org.springframework.content.azure.it.Azurite;
import org.junit.jupiter.api.*;
import org.springframework.content.azure.config.AzureStorageConfigurer;
import org.springframework.content.azure.config.EnableAzureStorage;
import org.springframework.content.test.support.TestSupportPackages;
import org.springframework.content.test.support.azure.SingleContentAzureStore;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class EnableAzureStorageTest {

    private static final BlobServiceClientBuilder builder = Azurite.getBlobServiceClientBuilder();
    private static final BlobContainerClient client = builder.buildClient().getBlobContainerClient("test");

    static {
        if (!client.exists()) {
            client.create();
        }

        System.setProperty("spring.content.azure.bucket", "azure-test-bucket");
    }

    private AzureStorageConfigurer configurer;
    private AnnotationConfigApplicationContext context;

    @Configuration
    @EnableAzureStorage(basePackages = "contains.no.azure.repositories")
    @Import(InfrastructureConfig.class)
    public static class EmptyConfig {
    }

    @Configuration
    @EnableAzureStorage(basePackages = TestSupportPackages.AZURE)
    @Import(InfrastructureConfig.class)
    public static class TestConfig {
    }

    @Configuration
    @EnableAzureStorage(basePackageClasses = SingleContentAzureStore.class)
    @Import(InfrastructureConfig.class)
    public static class BasePackageClassesConfig {
    }

    @Configuration
    @EnableAzureStorage(basePackages = TestSupportPackages.AZURE)
    @Import(InfrastructureConfig.class)
    public static class ConverterConfig {
        @Bean
        public AzureStorageConfigurer configurer() {
            return mock(AzureStorageConfigurer.class);
        }
    }

    @Configuration
    public static class InfrastructureConfig {
        @Bean
        public BlobServiceClientBuilder builder() {
            return builder;
        }
    }

    @Nested
    class EnableAzureStorageCases {
        @Nested
        @DisplayName("Given a context and a configuration with an Azure content store")
        class ContextAndConfigurationWithAzureContentStore {
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
                assertThat(context.getBean(SingleContentAzureStore.class)).isNotNull();
            }

            @Test
            void shouldHavePlacementService() {
                assertThat(context.getBean("azureStoragePlacementService")).isNotNull();
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
                configurer = context.getBean(AzureStorageConfigurer.class);
            }

            @AfterEach
            void tearDown() {
                context.close();
            }

            @Test
            void shouldCallThatConfigurerToHelpSetupStore() {
                verify(configurer).configureAzureStorageConverters(any());
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
            void shouldNotContainsAnyAzureStorageBeans() {
                assertThat(context.getBeanNamesForType(SingleContentAzureStore.class)).isEmpty();
            }
        }

        @Nested
        @DisplayName("Given basePackageClasses on the annotation")
        class BasePackageClassesScan {
            @BeforeEach
            void setUp() {
                context = new AnnotationConfigApplicationContext();
                context.register(BasePackageClassesConfig.class);
                context.refresh();
            }

            @AfterEach
            void tearDown() {
                context.close();
            }

            @Test
            void shouldHaveContentStoreBean() {
                assertThat(context.getBean(SingleContentAzureStore.class)).isNotNull();
            }
        }
    }

    @Nested
    @DisplayName("EnableAzureStorage annotation attributes")
    class EnableAzureStorageAnnotation {
        @Test
        void shouldExposeBasePackageClassesAndStoreFactoryBeanClass() {
            EnableAzureStorage byClass = BasePackageClassesConfig.class.getAnnotation(EnableAzureStorage.class);
            assertThat(byClass.basePackageClasses()).containsExactly(SingleContentAzureStore.class);
            assertThat(byClass.storeFactoryBeanClass()).isEqualTo(AzureStorageFactoryBean.class);

            EnableAzureStorage byPackage = TestConfig.class.getAnnotation(EnableAzureStorage.class);
            assertThat(byPackage.basePackageClasses()).isEmpty();
            assertThat(byPackage.storeFactoryBeanClass()).isEqualTo(AzureStorageFactoryBean.class);
        }
    }
}

