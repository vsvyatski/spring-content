package internal.org.springframework.content.azure.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.content.azure.config.AzureStorageConfigurer;
import org.springframework.content.azure.config.BlobId;
import org.springframework.content.azure.config.EnableAzureStorage;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.store.AssociativeStore;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.converter.ConverterRegistry;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClientBuilder;

import internal.org.springframework.content.azure.it.Azurite;

public class EnableAzureStorageTest {

    private static final BlobServiceClientBuilder builder = Azurite.getBlobServiceClientBuilder();
    private static final BlobContainerClient client = builder.buildClient().getBlobContainerClient("test");

    static {
        if (!client.exists()) {
            client.create();
        }

        System.setProperty("spring.content.azure.bucket", "azure-test-bucket");
    }

	private AnnotationConfigApplicationContext context;

	// mocks
	static AzureStorageConfigurer configurer;

    @Nested
    class EnableAzureStorageCases {
        @Nested
        class GivenAContextAndAConfigurationWithAnAzureContentStore {
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
                assertThat(context.getBean(TestEntityContentStore.class)).isNotNull();
            }

            @Test
            void shouldHaveAnPlacementService() {
                assertThat(context.getBean("azureStoragePlacementService")).isNotNull();
            }

        }

        @Nested
        class GivenAContextWithAConfigurer {
            @BeforeEach
            void setUp() {
                configurer = mock(AzureStorageConfigurer.class);

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
                verify(configurer).configureAzureStorageConverters(any());
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
            void shouldNotContainsAnyAzureStorageBeans() {
                try {
                	context.getBean(TestEntityContentStore.class);
                	fail("expected no such bean");
                }
                catch (NoSuchBeanDefinitionException e) {
                	assertThat(true).isTrue();
                }
            }

        }

    }

	@Configuration
	@EnableAzureStorage(basePackages = "contains.no.fs.repositores")
	@Import(InfrastructureConfig.class)
	public static class EmptyConfig {
	}

	@Configuration
	@EnableAzureStorage
	@Import(InfrastructureConfig.class)
	public static class TestConfig {
	}

	@Configuration
	@EnableAzureStorage
	@Import(InfrastructureConfig.class)
	public static class ConverterConfig {
		@Bean
		public AzureStorageConfigurer configurer() {
			return configurer;
		}
	}

	@Configuration
	@EnableAzureStorage
	@Import(InfrastructureConfig.class)
	public static class TestConverterConfig {
		@Bean
		public AzureStorageConfigurer configurer() {
			return new AzureStorageConfigurer() {

				@Override
				public void configureAzureStorageConverters(ConverterRegistry registry) {
				}
			};
		}
	}

	public interface TestEntityStore extends AssociativeStore<TestEntity, BlobId> {
	}

	@Configuration
	public static class InfrastructureConfig {
	    @Bean
	    public BlobServiceClientBuilder builder() {
	        return builder;
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

	public interface TestEntityContentStore
			extends ContentStore<TestEntity, String> {
	}
}
