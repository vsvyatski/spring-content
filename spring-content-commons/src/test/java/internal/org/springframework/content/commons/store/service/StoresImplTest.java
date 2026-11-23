package internal.org.springframework.content.commons.store.service;

import internal.org.springframework.content.commons.store.factory.StoreFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.content.commons.store.AssociativeStore;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.commons.store.Store;
import org.springframework.content.commons.storeservice.StoreFilter;
import org.springframework.content.commons.storeservice.StoreInfo;
import org.springframework.content.commons.storeservice.Stores;
import org.springframework.content.test.support.CommonsTestStores;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StoresImplTest {

    private StoresImpl storesImpl;

    private ApplicationContext context;
    private StoreFactory mockFactory;

    public interface StoreInterface extends Store<String> {
    }

    public interface AssociativeStoreInterface extends AssociativeStore<Object, String> {
    }

    public interface ContentStoreInterface extends ContentStore<Object, String> {
    }

    public interface EntityStoreInterface extends AssociativeStore<Entity, String> {
    }

    public interface OtherEntityStoreInterface extends AssociativeStore<OtherEntity, String> {
    }

    public interface FsEntityStoreInterface extends ContentStore<Entity, String> {
    }

    public interface JpaEntityStoreInterface extends ContentStore<Entity, String> {
    }

    public static class Entity {
    }

    public static class OtherEntity {
    }

    @Nested
    class StoresImplCases {
        @Nested
        @DisplayName("Given no factories")
        class NoFactories {
            @BeforeEach
            void setUp() {
                context = mock(ApplicationContext.class);

                when(context.getBeanNamesForType(StoreFactory.class)).thenReturn(new String[]{});

                storesImpl = new StoresImpl(context);
                storesImpl.afterPropertiesSet();
            }

            @Test
            void shouldAlwaysReturnEmpty() {
                assertThat(storesImpl.getStores(Store.class)).isEqualTo(new StoreInfo[]{});
            }
        }

        @Nested
        @DisplayName("Given a content store factory")
        class ContentStoreFactory {
            @BeforeEach
            void setUp() {
                context = mock(ApplicationContext.class);

                mockFactory = mock(StoreFactory.class);
                var store = CommonsTestStores.mockContentStore();
                when(mockFactory.getStore()).thenReturn(store);
                when(mockFactory.getStoreInterface())
                    .thenAnswer(invocation -> ContentStoreInterface.class);

                when(context.getBeanNamesForType(StoreFactory.class)).thenReturn(new String[]{"&testStoreFactory"});
                when(context.getBean("&testStoreFactory", StoreFactory.class)).thenReturn(mockFactory);
                when(context.getBean("testStoreFactory", Store.class)).thenReturn(store);

                storesImpl = new StoresImpl(context);
                storesImpl.afterPropertiesSet();
            }

            @Test
            void shouldReturnNoStoreInfo() {
                StoreInfo[] infos = storesImpl.getStores(Store.class);
                assertThat(infos.length).isEqualTo(1);
            }

            @Test
            void shouldReturnNoAssociativeStoreInfo() {
                StoreInfo[] infos = storesImpl.getStores(AssociativeStore.class);
                assertThat(infos.length).isEqualTo(1);
            }

            @Test
            void shouldReturnContentStoreInfo() {
                StoreInfo[] infos = storesImpl.getStores(ContentStore.class);
                assertThat(infos.length).isEqualTo(1);
            }
        }

        @Nested
        @DisplayName("Given a store factory")
        class RegisteredStoreFactory {
            @BeforeEach
            void setUp() {
                context = mock(ApplicationContext.class);

                mockFactory = mock(StoreFactory.class);
                var store = CommonsTestStores.mockStore();
                when(mockFactory.getStore()).thenReturn(store);
                when(mockFactory.getStoreInterface())
                    .thenAnswer(invocation -> StoreInterface.class);

                context = mock(ApplicationContext.class);
                when(context.getBeanNamesForType(StoreFactory.class)).thenReturn(new String[]{"&testStoreFactory"});
                when(context.getBean("&testStoreFactory", StoreFactory.class)).thenReturn(mockFactory);
                when(context.getBean("testStoreFactory", Store.class)).thenReturn(store);

                storesImpl = new StoresImpl(context);
                storesImpl.afterPropertiesSet();
            }

            @Test
            void shouldReturnStoreInfo() {
                StoreInfo[] infos = storesImpl.getStores(Store.class);
                assertThat(infos.length).isEqualTo(1);
            }

            @Test
            void shouldReturnAssociativeStoreInfo() {
                StoreInfo[] infos = storesImpl.getStores(AssociativeStore.class);
                assertThat(infos.length).isEqualTo(0);
            }

            @Test
            void shouldReturnNoContentStoreInfo() {
                StoreInfo[] infos = storesImpl.getStores(ContentStore.class);
                assertThat(infos.length).isEqualTo(0);
            }
        }

        @Nested
        @DisplayName("Given an associative store factory")
        class AssociativeStoreFactory {
            @BeforeEach
            void setUp() {
                context = mock(ApplicationContext.class);

                mockFactory = mock(StoreFactory.class);
                var store = CommonsTestStores.mockAssociativeStore();
                when(mockFactory.getStore()).thenReturn(store);
                when(mockFactory.getStoreInterface())
                    .thenAnswer(invocation -> AssociativeStoreInterface.class);

                context = mock(ApplicationContext.class);
                when(context.getBeanNamesForType(StoreFactory.class)).thenReturn(new String[]{"&testStoreFactory"});
                when(context.getBean("&testStoreFactory", StoreFactory.class)).thenReturn(mockFactory);
                when(context.getBean("testStoreFactory", Store.class)).thenReturn(store);

                storesImpl = new StoresImpl(context);
                storesImpl.afterPropertiesSet();
            }

            @Test
            void shouldReturnNoContentStoreInfo() {
                StoreInfo[] infos = storesImpl.getStores(ContentStore.class);
                assertThat(infos.length).isEqualTo(0);
            }

            @Test
            void shouldReturnStoreInfo() {
                StoreInfo[] infos = storesImpl.getStores(Store.class);
                assertThat(infos.length).isEqualTo(1);
            }

            @Test
            void shouldReturnAssociativeStoreInfo() {
                StoreInfo[] infos = storesImpl.getStores(AssociativeStore.class);
                assertThat(infos.length).isEqualTo(1);
            }
        }

        @Nested
        @DisplayName("Given multiple stores")
        class MultipleStores {
            @BeforeEach
            void setUp() {
                context = mock(ApplicationContext.class);

                mockFactory = mock(StoreFactory.class);
                var store = CommonsTestStores.mockAssociativeStore();
                when(mockFactory.getStore()).thenReturn(store);
                when(mockFactory.getStoreInterface())
                    .thenAnswer(invocation -> EntityStoreInterface.class);

                StoreFactory mockFactory2 = mock(StoreFactory.class);
                var store2 = CommonsTestStores.mockAssociativeStore();
                when(mockFactory2.getStore()).thenReturn(store2);
                when(mockFactory2.getStoreInterface())
                    .thenAnswer(invocation -> OtherEntityStoreInterface.class);

                context = mock(ApplicationContext.class);
                when(context.getBeanNamesForType(StoreFactory.class)).thenReturn(new String[]{"&testStoreFactory1", "&testStoreFactory2"});
                when(context.getBean("&testStoreFactory1", StoreFactory.class)).thenReturn(mockFactory);
                when(context.getBean("&testStoreFactory2", StoreFactory.class)).thenReturn(mockFactory2);
                when(context.getBean("testStoreFactory1", Store.class)).thenReturn(store);
                when(context.getBean("testStoreFactory2", Store.class)).thenReturn(store2);

                storesImpl = new StoresImpl(context);
                storesImpl.afterPropertiesSet();
            }

            @Test
            void shouldReturnStoresThatMatchFilter() {
                StoreInfo[] infos = storesImpl.getStores(AssociativeStore.class, Stores.MATCH_ALL);
                assertThat(infos.length).isEqualTo(2);
            }

            @Test
            void shouldNotReturnStoresThatDontMatchFilter() {
                StoreInfo[] infos = storesImpl.getStores(AssociativeStore.class, new StoreFilter() {
                    @Override
                    public String name() {
                        return "test";
                    }

                    @Override
                    public boolean matches(StoreInfo info) {
                        return false;
                    }
                });
                assertThat(infos.length).isEqualTo(0);
            }
        }

        @Nested
        @DisplayName("Given multiple stores for the same entity")
        class MultipleStoresForSameEntity {
            @BeforeEach
            void setUp() {
                context = mock(ApplicationContext.class);

                mockFactory = mock(StoreFactory.class);
                var store = CommonsTestStores.mockContentStore();
                when(mockFactory.getStore()).thenReturn(store);
                when(mockFactory.getStoreInterface())
                    .thenAnswer(invocation -> FsEntityStoreInterface.class);

                StoreFactory mockFactory2 = mock(StoreFactory.class);
                var store2 = CommonsTestStores.mockContentStore();
                when(mockFactory2.getStore()).thenReturn(store2);
                when(mockFactory2.getStoreInterface())
                    .thenAnswer(invocation -> JpaEntityStoreInterface.class);

                context = mock(ApplicationContext.class);
                when(context.getBeanNamesForType(StoreFactory.class)).thenReturn(new String[]{"&testStoreFactory1", "&testStoreFactory2"});
                when(context.getBean("&testStoreFactory1", StoreFactory.class)).thenReturn(mockFactory);
                when(context.getBean("&testStoreFactory2", StoreFactory.class)).thenReturn(mockFactory2);
                when(context.getBean("testStoreFactory1", Store.class)).thenReturn(store);
                when(context.getBean("testStoreFactory2", Store.class)).thenReturn(store2);

                storesImpl = new StoresImpl(context);
                storesImpl.afterPropertiesSet();
            }

            @Test
            void shouldReturnStoresThatMatchFilter() {
                StoreInfo[] infos = storesImpl.getStores(ContentStore.class, Stores.MATCH_ALL);
                assertThat(infos.length).isEqualTo(2);
            }
        }
    }
}
