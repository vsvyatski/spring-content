package it;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;


import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.springframework.content.commons.store.Store;
import internal.org.springframework.content.commons.store.factory.StoreFactory;
import org.springframework.content.commons.store.factory.testsupport.TestContentStore;
import org.springframework.content.commons.store.factory.testsupport.TestStoreFactoryBean;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.commons.store.StoreAccessException;
import org.springframework.content.commons.store.StoreExceptionTranslator;
import org.springframework.content.commons.storeservice.StoreFilter;
import org.springframework.content.commons.storeservice.StoreInfo;
import org.springframework.content.commons.storeservice.StoreResolver;
import org.springframework.context.support.GenericApplicationContext;


import internal.org.springframework.content.commons.storeservice.StoresImpl;

public class StoresIT {

    private StoresImpl stores;

    private GenericApplicationContext context;
    private List<StoreFactory> factories = new ArrayList<>();
    private StoreResolver resolver;

    
    @Nested
    class GetStoreCases {
        @Nested
        class WhenThereAreTwoStoresThatTheFilterMatchesButNoStoreResolver {
            @BeforeEach
            void setUp() {
                TestStoreFactoryBean factory1 = new TestStoreFactoryBean(WrongStore.class);
                                    factory1.setBeanClassLoader(this.getClass().getClassLoader());
                                    factories.add(factory1);

                                    TestStoreFactoryBean factory2 = new TestStoreFactoryBean(RightStore.class);
                                    factory2.setBeanClassLoader(this.getClass().getClassLoader());
                                    factories.add(factory2);

                                    context = new GenericApplicationContext();
                                    context.registerBean("factory1", StoreFactory.class, () -> {return factory1;});
                                    context.registerBean("factory2", StoreFactory.class, () -> {return factory2;});

                context.refresh();
                                    stores = new StoresImpl(context);
                                    stores.afterPropertiesSet();
            }
            @Test
            void shouldReturnTheRightStore() {
                try {
                                        stores.getStore(Store.class, new StoreFilter() {
                                            @Override
                                            public String name() {
                                                return "test";
                                            }

                                            @Override
                                            public boolean matches(StoreInfo info) {
                                                return true;
                                            }
                                        });
                                        fail("exception not thrown");
                                    } catch (Exception e) {
                                        assertThat(e.getMessage()).contains("unable to resolve store");
                                    }
            }
        }
        @Nested
        class WhenThereAreTwoStoresThatTheFilterMatchesAndAStoreResolver {
            @BeforeEach
            void setUp() {
                TestStoreFactoryBean factory1 = new TestStoreFactoryBean(WrongStore.class);
                                    factory1.setBeanClassLoader(this.getClass().getClassLoader());
                                    factories.add(factory1);

                                    TestStoreFactoryBean factory2 = new TestStoreFactoryBean(RightStore.class);
                                    factory2.setBeanClassLoader(this.getClass().getClassLoader());
                                    factories.add(factory2);

                                    context = new GenericApplicationContext();
                                    context.registerBean("factory1", StoreFactory.class, () -> {return factory1;});
                                    context.registerBean("factory2", StoreFactory.class, () -> {return factory2;});

                context.refresh();
                                    stores = new StoresImpl(context);
                                    stores.afterPropertiesSet();

                                    stores.addStoreResolver("test", new StoreResolver() {
                                        @Override
                                        public StoreInfo resolve(StoreInfo... stores) {
                                            for (StoreInfo info : stores) {
                                                if (info.getInterface().equals(RightStore.class)) {
                                                    return info;
                                                }
                                            }
                                            return null;
                                        }
                                    });
            }
            @Test
            void shouldReturnTheRightStore() {
                StoreInfo info = stores.getStore(Store.class, new StoreFilter() {
                                        @Override
                                        public String name() {
                                            return "test";
                                        }
                                        @Override
                                        public boolean matches(StoreInfo info) {
                                            return true;
                                        }
                                    });

                                    assertThat(info.getInterface()).isEqualTo(RightStore.class);
            }
        }
    }
    @Nested
    class StoreExceptionTranslatorInterceptorCases {
        @Nested
        class GivenThereIsNoStoreExceptionTranslatorRegistered {
            @BeforeEach
            void setUp() {
                // All TestContentStore methods throw an UnsupportedOperationException, this test relies on this
                                TestStoreFactoryBean factory = new TestStoreFactoryBean(RuntimeExceptionThrowingStore.class);
                                factory.setBeanClassLoader(this.getClass().getClassLoader());
                                factories.add(factory);

                                context = new GenericApplicationContext();
                                context.registerBean("factory", StoreFactory.class, () -> {return factory;});

                context.refresh();
                                    stores = new StoresImpl(context);
                                    stores.afterPropertiesSet();
            }
            @Test
            void shouldReThrowRuntimeExceptionAsStoreAccessException() {
                StoreInfo storeInfo = stores.getStore(Store.class, new StoreFilter() {
                                            @Override
                                            public String name() {
                                                return "test";
                                            }

                                            @Override
                                            public boolean matches(StoreInfo info) {
                                                return true;
                                            }
                                    });
                                    ContentStore store = storeInfo.getImplementation(ContentStore.class);
                                    try {
                                        store.setContent(new Object(), new ByteArrayInputStream("".getBytes()));
                                    } catch (Exception e) {
                                        assertThat(e).isInstanceOf(UnsupportedOperationException.class);
                                    }
            }
        }
        @Nested
        class GivenThereIsAStoreExceptionTranslatorRegistered {
            @BeforeEach
            void setUp() {
                // All TestContentStore methods throw an UnsupportedOperationException, this test relies on this
                                TestStoreFactoryBean factory = new TestStoreFactoryBean(RuntimeExceptionThrowingStore.class);
                                factory.setBeanClassLoader(this.getClass().getClassLoader());
                                factories.add(factory);

                                context = new GenericApplicationContext();
                                context.registerBean("factory", StoreFactory.class, () -> {return factory;});

                context.registerBean("translator", StoreExceptionTranslator.class, () -> {return new StoreExceptionTranslator() {
                                        @Override
                                        public StoreAccessException translate(RuntimeException re) {
                                            return new StoreAccessException(re.getMessage(), re);
                                        }
                                    };});
                                    context.refresh();
                                    stores = new StoresImpl(context);
                                    stores.afterPropertiesSet();
            }
            @Test
            void shouldReThrowRuntimeExceptionAsStoreAccessException() {
                StoreInfo storeInfo = stores.getStore(Store.class, new StoreFilter() {
                                        @Override
                                        public String name() {
                                            return "test";
                                        }

                                        @Override
                                        public boolean matches(StoreInfo info) {
                                            return true;
                                        }
                                    });
                                    ContentStore store = storeInfo.getImplementation(ContentStore.class);
                                    try {
                                        store.setContent(new Object(), new ByteArrayInputStream("".getBytes()));
                                    } catch (Exception e) {
                                        assertThat(e).isInstanceOf(StoreAccessException.class);
                                    }
            }
        }
    }


    public interface RightStore extends TestContentStore<Object, Serializable>{};
    public interface WrongStore extends TestContentStore<Object, Serializable>{};
    public interface RuntimeExceptionThrowingStore extends TestContentStore<Object, Serializable>{};
}
