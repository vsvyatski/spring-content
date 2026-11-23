package internal.org.springframework.content.gcs.config;

import com.google.cloud.storage.Storage;
import internal.org.springframework.content.gcs.it.GCSStorageContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.content.gcs.config.EnableGCPStorage;
import org.springframework.content.gcs.config.GCPStorageConfigurer;
import org.springframework.content.test.support.TestSupportPackages;
import org.springframework.content.test.support.gcs.SingleContentGcsStore;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class EnableGCPStorageTest {

    private GCPStorageConfigurer configurer;
    private AnnotationConfigApplicationContext context;

    @Configuration
    @EnableGCPStorage(basePackages = "contains.no.gcs.repositories")
    @Import(InfrastructureConfig.class)
    public static class EmptyConfig {
    }

    @Configuration
    @EnableGCPStorage(basePackages = TestSupportPackages.GCS)
    @Import(InfrastructureConfig.class)
    public static class TestConfig {
    }

    @Configuration
    @EnableGCPStorage(basePackages = TestSupportPackages.GCS)
    @Import(InfrastructureConfig.class)
    public static class ConverterConfig {
        @Bean
        public GCPStorageConfigurer configurer() {
            return mock(GCPStorageConfigurer.class);
        }
    }

    @Configuration
    public static class InfrastructureConfig {

        @Bean
        public static Storage storage() {
            return GCSStorageContainer.getStorage();
        }
    }

    @Nested
   class EnableGCPStorageCases {
        @Nested
        @DisplayName("Given a context and a configuration with an gcs content store")
        class ContextAndConfigurationWithGCSContentStore {
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
            void shouldHaveAContentStoreBean() {
                assertThat(context.getBean(SingleContentGcsStore.class)).isNotNull();
            }

            @Test
            void shouldHaveAnPlacementService() {
                assertThat(context.getBean("gcpStoragePlacementService")).isNotNull();
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
                configurer = context.getBean(GCPStorageConfigurer.class);
            }

            @AfterEach
            void tearDown() {
                context.close();
            }

            @Test
            void shouldCallThatConfigurerToHelpSetupTheStore() {
                verify(configurer).configureGCPStorageConverters(any());
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
            void shouldNotContainsAnyGcsStoreBeans() {
                assertThat(context.getBeanNamesForType(SingleContentGcsStore.class)).isEmpty();
            }
        }
    }
}

