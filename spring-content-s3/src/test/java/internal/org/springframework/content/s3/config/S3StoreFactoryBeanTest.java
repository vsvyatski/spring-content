package internal.org.springframework.content.s3.config;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.Mockito.mock;

import java.io.Serializable;

import org.springframework.content.commons.store.AssociativeStore;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.commons.store.Store;
import org.springframework.content.commons.utils.PlacementService;
import org.springframework.context.support.GenericApplicationContext;

import software.amazon.awssdk.services.s3.S3Client;

public class S3StoreFactoryBeanTest {

	private S3StoreFactoryBean factory;

	private GenericApplicationContext context = new GenericApplicationContext();
	private S3Client client;
	private PlacementService placer;

	private Store store;

    @Nested
    class S3StoreFactoryBeanCases {
        @Nested
        class GetStore {
            @Nested
            class GivenAStore {
                @BeforeEach
                void setUp() {
                    client = mock(S3Client.class);
                    				placer = mock(PlacementService.class);

                    				context.registerBean("amazonS3", S3Client.class, () -> client);
                    				context.refresh();

                    				factory = new S3StoreFactoryBean(S3StoreFactoryBeanTest.TestStore.class/*, context, client, placer*/);
                    				factory.setContext(context);
                    				factory.setClient(client);
                    				factory.setS3StorePlacementService(placer);

                    factory.setBeanClassLoader(Thread.currentThread().getContextClassLoader());

                    store = factory.getStore();
                }
                @Test
                void shouldReturnAStoreImplementation() {
                    assertThat(store).isNotNull();
                }
            }
            @Nested
            class GivenAnAssociativeStore {
                @BeforeEach
                void setUp() {
                    client = mock(S3Client.class);
                    				placer = mock(PlacementService.class);

                    				context.registerBean("amazonS3", S3Client.class, () -> client);
                    				context.refresh();

                    				factory = new S3StoreFactoryBean(S3StoreFactoryBeanTest.TestStore.class/*, context, client, placer*/);
                    				factory.setContext(context);
                    				factory.setClient(client);
                    				factory.setS3StorePlacementService(placer);

                    factory.setBeanClassLoader(Thread.currentThread().getContextClassLoader());

                    store = factory.getStore();
                }
                @Test
                void shouldReturnAStoreImplementation() {
                    assertThat(store).isNotNull();
                }
            }
        }
    }

	public interface TestStore extends Store<Serializable> {
	}

	private interface TestAssociativeStore extends AssociativeStore<Object, Serializable> {
	}

	private interface TestContentStore extends ContentStore<Object, Serializable> {
	}
}
