package internal.org.springframework.content.s3.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.content.commons.store.AssociativeStore;
import org.springframework.content.commons.store.Store;
import org.springframework.content.commons.utils.PlacementService;
import org.springframework.context.support.GenericApplicationContext;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.Serializable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class S3StoreFactoryBeanTest {

    private final GenericApplicationContext context = new GenericApplicationContext();
    private S3StoreFactoryBean factory;
    private S3Client client;
    private PlacementService placer;

    private Store<Serializable> store;

    public interface TestStore extends Store<Serializable> {
    }

    private interface TestAssociativeStore extends AssociativeStore<Object, Serializable> {
    }

    @Nested
    class S3StoreFactoryBeanCases {
        @Nested
        class GetStore {
            @Nested
            @DisplayName("Given a store")
            class Store {
                @BeforeEach
                void setUp() {
                    client = mock(S3Client.class);
                    placer = mock(PlacementService.class);

                    context.registerBean("s3Client", S3Client.class, () -> client);
                    context.refresh();

                    factory = new S3StoreFactoryBean(TestStore.class);
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
            @DisplayName("Given an associative store")
            class AssociativeStore {
                @BeforeEach
                void setUp() {
                    client = mock(S3Client.class);
                    placer = mock(PlacementService.class);

                    context.registerBean("s3Client", S3Client.class, () -> client);
                    context.refresh();

                    factory = new S3StoreFactoryBean(TestAssociativeStore.class);
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
}
