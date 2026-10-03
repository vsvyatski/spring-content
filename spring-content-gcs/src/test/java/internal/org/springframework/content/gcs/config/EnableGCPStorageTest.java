package internal.org.springframework.content.gcs.config;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.contrib.nio.testing.LocalStorageHelper;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.store.AssociativeStore;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.gcs.config.EnableGCPStorage;
import org.springframework.content.gcs.config.GCPStorageConfigurer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class EnableGCPStorageTest {

    private AnnotationConfigApplicationContext context;

    // mocks
    static GCPStorageConfigurer configurer;

    
    @Nested
    class EnableGCPStorageCases {
        @Nested
        class GivenAContextAndAConfigurationWithAnGCSContentStore {
            @BeforeEach
            void setUp() throws Throwable {
                context = new AnnotationConfigApplicationContext();
                                            context.register(TestConfig.class);
                                            context.refresh();
            }
            @AfterEach
            void tearDown() throws Throwable {
                context.close();
            }
            @Test
            void shouldHaveAContentStoreBean() throws Throwable {
                assertThat(context.getBean(TestEntityContentStore.class)).isNotNull();
            }
            @Test
            void shouldHaveAnPlacementService() throws Throwable {
                assertThat(context.getBean("gcpStoragePlacementService")).isNotNull();
            }
        }
        @Nested
        class GivenAContextWithAConfigurer {
            @BeforeEach
            void setUp() throws Throwable {
                configurer = mock(GCPStorageConfigurer.class);

                                    context = new AnnotationConfigApplicationContext();
                                    context.register(ConverterConfig.class);
                                    context.refresh();
            }
            @AfterEach
            void tearDown() throws Throwable {
                context.close();
            }
            @Test
            void shouldCallThatConfigurerToHelpSetupTheStore() throws Throwable {
                verify(configurer).configureGCPStorageConverters(any());
            }
        }
        @Nested
        class GivenAContextWithAnEmptyConfiguration {
            @BeforeEach
            void setUp() throws Throwable {
                context = new AnnotationConfigApplicationContext();
                                    context.register(EmptyConfig.class);
                                    context.refresh();
            }
            @AfterEach
            void tearDown() throws Throwable {
                context.close();
            }
            @Test
            void shouldNotContainsAnyS3RepositoryBeans() throws Throwable {
                try {
                                        context.getBean(TestEntityContentStore.class);
                                        fail("expected no such bean");
                                    } catch (NoSuchBeanDefinitionException e) {
                                        assertThat(true).isTrue();
                                    }
            }
        }
    }


    @Test
    public void noop() {
    }

    @Configuration
    @EnableGCPStorage(basePackages = "contains.no.fs.repositores")
    @Import(InfrastructureConfig.class)
    public static class EmptyConfig {
    }

    @Configuration
    @EnableGCPStorage
    @Import(InfrastructureConfig.class)
    public static class TestConfig {
    }

    @Configuration
    @EnableGCPStorage
    @Import(InfrastructureConfig.class)
    public static class ConverterConfig {
        @Bean
        public GCPStorageConfigurer configurer() {
            return configurer;
        }
    }

    @Configuration
    @EnableGCPStorage
    @Import(InfrastructureConfig.class)
    public static class TestConverterConfig {
        @Bean
        public GCPStorageConfigurer configurer() {
            return registry -> {
            };
        }
    }

    public interface TestEntityStore extends AssociativeStore<TestEntity, BlobId> {
    }

    @Configuration
    public static class InfrastructureConfig {

        @Bean
        public static Storage storage() {
            return LocalStorageHelper.getOptions().getService();
        }
    }

    public static class TestEntity {
        @ContentId
        private String contentId;

        public String getContentId() {
            return contentId;
        }

        public void setContentId(String contentId) {
            this.contentId = contentId;
        }
    }

    public interface TestEntityContentStore extends ContentStore<TestEntity, String> {
    }
}
