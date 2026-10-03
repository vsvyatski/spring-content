package internal.org.springframework.content.commons.storeservice;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.content.commons.store.AssociativeStore;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.commons.store.Store;
import internal.org.springframework.content.commons.store.factory.StoreFactory;
import org.springframework.content.commons.storeservice.StoreFilter;
import org.springframework.content.commons.storeservice.StoreInfo;
import org.springframework.content.commons.storeservice.Stores;
import org.springframework.context.ApplicationContext;

public class StoresImplTest {

	private StoresImpl contentRepoService;

	private ApplicationContext context;
	private StoreFactory mockFactory;

	
    @Nested
    class StoresImplCases {
        @Nested
        class GivenNoFactories {
            @BeforeEach
            void setUp() {
                context = mock(ApplicationContext.class);

                when(context.getBeanNamesForType(StoreFactory.class)).thenReturn(new String[]{});

                contentRepoService = new StoresImpl(context);
                contentRepoService.afterPropertiesSet();

            }

            @Test
            void shouldAlwaysReturnEmpty() {
                assertThat(contentRepoService.getStores(Store.class)).isEqualTo(new StoreInfo[] {});

            }

        }

        @Nested
        class GivenAContentStoreFactory {
            @BeforeEach
            void setUp() {
                context = mock(ApplicationContext.class);

                mockFactory = mock(StoreFactory.class);
                Store store = mock(ContentStore.class);
                when(mockFactory.getStore()).thenReturn(store);
                when(mockFactory.getStoreInterface())
                		.thenAnswer(new Answer<Object>() {
                			@Override
                			public Object answer(InvocationOnMock invocation) {
                				return ContentRepositoryInterface.class;
                			}
                		});

                when(context.getBeanNamesForType(StoreFactory.class)).thenReturn(new String[]{"&testStoreFactory"});
                when(context.getBean("&testStoreFactory", StoreFactory.class)).thenReturn(mockFactory);
                               when(context.getBean("testStoreFactory", Store.class)).thenReturn(store);

                contentRepoService = new StoresImpl(context);
                contentRepoService.afterPropertiesSet();

            }

            @Test
            void shouldReturnNoStoreInfo() {
                StoreInfo[] infos = contentRepoService.getStores(Store.class);
                assertThat(infos.length).isEqualTo(1);

            }

            @Test
            void shouldReturnNoAssociativestoreInfo() {
                StoreInfo[] infos = contentRepoService
                		.getStores(AssociativeStore.class);
                assertThat(infos.length).isEqualTo(1);

            }

            @Test
            void shouldReturnContentStoreInfo() {
                StoreInfo[] infos = contentRepoService.getStores(ContentStore.class);
                assertThat(infos.length).isEqualTo(1);

            }

        }

        @Nested
        class GivenAStoreFactory {
            @BeforeEach
            void setUp() {
                context = mock(ApplicationContext.class);

                mockFactory = mock(StoreFactory.class);
                               Store store = mock(Store.class);
                               when(mockFactory.getStore()).thenReturn(store);
                when(mockFactory.getStoreInterface())
                		.thenAnswer(new Answer<Object>() {
                			@Override
                			public Object answer(InvocationOnMock invocation) {
                				return StoreInterface.class;
                			}
                		});

                               context = mock(ApplicationContext.class);
                               when(context.getBeanNamesForType(StoreFactory.class)).thenReturn(new String[]{"&testStoreFactory"});
                               when(context.getBean("&testStoreFactory", StoreFactory.class)).thenReturn(mockFactory);
                               when(context.getBean("testStoreFactory", Store.class)).thenReturn(store);

                contentRepoService = new StoresImpl(context);
                contentRepoService.afterPropertiesSet();

            }

            @Test
            void shouldReturnStoreInfo() {
                StoreInfo[] infos = contentRepoService.getStores(Store.class);
                assertThat(infos.length).isEqualTo(1);

            }

            @Test
            void shouldReturnAssociativestoreInfo() {
                StoreInfo[] infos = contentRepoService
                		.getStores(AssociativeStore.class);
                assertThat(infos.length).isEqualTo(0);

            }

            @Test
            void shouldReturnNoContentStoreInfo() {
                StoreInfo[] infos = contentRepoService.getStores(ContentStore.class);
                assertThat(infos.length).isEqualTo(0);

            }

        }

        @Nested
        class GivenAnAssociativeStoreFactory {
            @BeforeEach
            void setUp() {
                context = mock(ApplicationContext.class);

                mockFactory = mock(StoreFactory.class);
                               Store store = mock(AssociativeStore.class);
                               when(mockFactory.getStore()).thenReturn(store);
                when(mockFactory.getStoreInterface())
                		.thenAnswer(new Answer<Object>() {
                			@Override
                			public Object answer(InvocationOnMock invocation) {
                				return AssociativeStoreInterface.class;
                			}
                		});

                               context = mock(ApplicationContext.class);
                               when(context.getBeanNamesForType(StoreFactory.class)).thenReturn(new String[]{"&testStoreFactory"});
                               when(context.getBean("&testStoreFactory", StoreFactory.class)).thenReturn(mockFactory);
                               when(context.getBean("testStoreFactory", Store.class)).thenReturn(store);

                contentRepoService = new StoresImpl(context);
                contentRepoService.afterPropertiesSet();

            }

            @Test
            void shouldReturnNoContentStoreInfo() {
                StoreInfo[] infos = contentRepoService.getStores(ContentStore.class);
                assertThat(infos.length).isEqualTo(0);

            }

            @Test
            void shouldReturnStoreInfo() {
                StoreInfo[] infos = contentRepoService.getStores(Store.class);
                assertThat(infos.length).isEqualTo(1);

            }

            @Test
            void shouldReturnAssociativestoreInfo() {
                StoreInfo[] infos = contentRepoService
                		.getStores(AssociativeStore.class);
                assertThat(infos.length).isEqualTo(1);

            }

        }

        @Nested
        class GivenMultipleStores {
            @BeforeEach
            void setUp() {
                context = mock(ApplicationContext.class);

                mockFactory = mock(StoreFactory.class);
                               Store store = mock(AssociativeStore.class);
                               when(mockFactory.getStore()).thenReturn(store);
                when(mockFactory.getStoreInterface())
                		.thenAnswer(new Answer<Object>() {
                			@Override
                			public Object answer(InvocationOnMock invocation) {
                				return EntityStoreInterface.class;
                			}
                		});

                StoreFactory mockFactory2 = mock(StoreFactory.class);
                Store store2 = mock(AssociativeStore.class);
                when(mockFactory2.getStore()).thenReturn(store2);
                when(mockFactory2.getStoreInterface())
                		.thenAnswer(new Answer<Object>() {
                			@Override
                			public Object answer(InvocationOnMock invocation) {
                				return OtherEntityStoreInterface.class;
                			}
                		});

                               context = mock(ApplicationContext.class);
                               when(context.getBeanNamesForType(StoreFactory.class)).thenReturn(new String[]{"&testStoreFactory1", "&testStoreFactory2"});
                               when(context.getBean("&testStoreFactory1", StoreFactory.class)).thenReturn(mockFactory);
                               when(context.getBean("&testStoreFactory2", StoreFactory.class)).thenReturn(mockFactory2);
                               when(context.getBean("testStoreFactory1", Store.class)).thenReturn(store);
                               when(context.getBean("testStoreFactory2", Store.class)).thenReturn(store2);

                contentRepoService = new StoresImpl(context);
                contentRepoService.afterPropertiesSet();

            }

            @Test
            void shouldReturnStoresThatMatchTheFilter() {
                StoreInfo[] infos = contentRepoService.getStores(
                		AssociativeStore.class, Stores.MATCH_ALL);
                assertThat(infos.length).isEqualTo(2);

            }

            @Test
            void shouldNotReturnStoresThatDontMatchTheFilter() {
                StoreInfo[] infos = contentRepoService
                		.getStores(AssociativeStore.class, new StoreFilter() {
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
        class GivenMultipleStoresForTheSameEntity {
            @BeforeEach
            void setUp() {
                context = mock(ApplicationContext.class);

                mockFactory = mock(StoreFactory.class);
                Store store = mock(ContentStore.class);
                when(mockFactory.getStore()).thenReturn(store);
                when(mockFactory.getStoreInterface())
                		.thenAnswer(new Answer<Object>() {
                			@Override
                			public Object answer(InvocationOnMock invocation) {
                				return FsEntityStoreInterface.class;
                			}
                		});

                StoreFactory mockFactory2 = mock(StoreFactory.class);
                               Store store2 = mock(ContentStore.class);
                               when(mockFactory2.getStore()).thenReturn(store2);
                when(mockFactory2.getStoreInterface())
                		.thenAnswer(new Answer<Object>() {
                			@Override
                			public Object answer(InvocationOnMock invocation) {
                				return JpaEntityStoreInterface.class;
                			}
                		});

                               context = mock(ApplicationContext.class);
                               when(context.getBeanNamesForType(StoreFactory.class)).thenReturn(new String[]{"&testStoreFactory1", "&testStoreFactory2"});
                               when(context.getBean("&testStoreFactory1", StoreFactory.class)).thenReturn(mockFactory);
                               when(context.getBean("&testStoreFactory2", StoreFactory.class)).thenReturn(mockFactory2);
                               when(context.getBean("testStoreFactory1", Store.class)).thenReturn(store);
                               when(context.getBean("testStoreFactory2", Store.class)).thenReturn(store2);

                contentRepoService = new StoresImpl(context);
                contentRepoService.afterPropertiesSet();

            }

            @Test
            void shouldReturnStoresThatMatchTheFilter() {
                StoreInfo[] infos = contentRepoService.getStores(ContentStore.class, Stores.MATCH_ALL);
                assertThat(infos.length).isEqualTo(2);

            }

        }

    }

	@Test
	public void test() {
	}

	public interface StoreInterface extends Store<String> {
	}

	public interface AssociativeStoreInterface extends AssociativeStore<Object, String> {
	}

	public interface ContentRepositoryInterface extends ContentStore<Object, String> {
	}

	public static class Entity {
	};

	public static class OtherEntity {
	};

	public interface EntityStoreInterface extends AssociativeStore<Entity, String> {
	}

	public interface OtherEntityStoreInterface
			extends AssociativeStore<OtherEntity, String> {
	}

	public interface FsEntityStoreInterface extends ContentStore<Entity, String> {
	}

	public interface JpaEntityStoreInterface extends ContentStore<Entity, String> {
	}
}
