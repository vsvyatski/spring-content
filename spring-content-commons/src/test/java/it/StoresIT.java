package it;

import internal.org.springframework.content.commons.store.factory.StoreFactory;
import internal.org.springframework.content.commons.store.service.StoresImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.content.commons.store.Store;
import org.springframework.content.commons.store.StoreAccessException;
import org.springframework.content.commons.store.StoreExceptionTranslator;
import org.springframework.content.commons.storeservice.StoreFilter;
import org.springframework.content.commons.storeservice.StoreInfo;
import org.springframework.content.test.support.factory.TestContentStore;
import org.springframework.content.test.support.factory.TestStoreFactoryBean;
import org.springframework.context.support.GenericApplicationContext;

import java.io.ByteArrayInputStream;
import java.io.Serializable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class StoresIT {

    private StoresImpl stores;

    private GenericApplicationContext context;

    private static StoreFilter testStoreFilter() {
        return new StoreFilter() {
            @Override
            public String name() {
                return "test";
            }

            @Override
            public boolean matches(StoreInfo info) {
                return true;
            }
        };
    }

    public interface RightStore extends TestContentStore<Object, Serializable> {
    }

    public interface WrongStore extends TestContentStore<Object, Serializable> {
    }

    public interface RuntimeExceptionThrowingStore extends TestContentStore<Object, Serializable> {
    }

    @Nested
    class GetStoreCases {
        @Nested
        @DisplayName("When there are two stores that the filter matches but no store resolver")
        class ThereAreTwoStoresThatTheFilterMatchesButNoStoreResolver {
            @BeforeEach
            void setUp() {
                TestStoreFactoryBean factory1 = new TestStoreFactoryBean(WrongStore.class);
                factory1.setBeanClassLoader(this.getClass().getClassLoader());

                TestStoreFactoryBean factory2 = new TestStoreFactoryBean(RightStore.class);
                factory2.setBeanClassLoader(this.getClass().getClassLoader());

                context = new GenericApplicationContext();
                context.registerBean("factory1", StoreFactory.class, () -> factory1);
                context.registerBean("factory2", StoreFactory.class, () -> factory2);

                context.refresh();
                stores = new StoresImpl(context);
                stores.afterPropertiesSet();
            }

            @Test
            void shouldReturnTheRightStore() {
                assertThatThrownBy(() -> stores.getStore(Store.class, testStoreFilter()))
                    .hasMessageContaining("unable to resolve store");
            }
        }

        @Nested
        @DisplayName("When there are two stores that the filter matches and a store resolver")
        class ThereAreTwoStoresThatTheFilterMatchesAndAStoreResolver {
            @BeforeEach
            void setUp() {
                TestStoreFactoryBean factory1 = new TestStoreFactoryBean(WrongStore.class);
                factory1.setBeanClassLoader(this.getClass().getClassLoader());

                TestStoreFactoryBean factory2 = new TestStoreFactoryBean(RightStore.class);
                factory2.setBeanClassLoader(this.getClass().getClassLoader());

                context = new GenericApplicationContext();
                context.registerBean("factory1", StoreFactory.class, () -> factory1);
                context.registerBean("factory2", StoreFactory.class, () -> factory2);

                context.refresh();
                stores = new StoresImpl(context);
                stores.afterPropertiesSet();

                stores.addStoreResolver("test", infos -> {
                    for (StoreInfo info : infos) {
                        if (info.getInterface().equals(RightStore.class)) {
                            return info;
                        }
                    }
                    return null;
                });
            }

            @Test
            void shouldReturnTheRightStore() {
                StoreInfo info = stores.getStore(Store.class, testStoreFilter());

                assertThat(info.getInterface()).isEqualTo(RightStore.class);
            }
        }
    }

    @Nested
    class StoreExceptionTranslatorInterceptorCases {
        @Nested
        @DisplayName("Given there is no store exception translator registered")
        class ThereIsNoStoreExceptionTranslatorRegistered {
            @BeforeEach
            void setUp() {
                // All TestContentStore methods throw an UnsupportedOperationException, this test relies on this
                TestStoreFactoryBean factory = new TestStoreFactoryBean(RuntimeExceptionThrowingStore.class);
                factory.setBeanClassLoader(this.getClass().getClassLoader());

                context = new GenericApplicationContext();
                context.registerBean("factory", StoreFactory.class, () -> factory);

                context.refresh();
                stores = new StoresImpl(context);
                stores.afterPropertiesSet();
            }

            @Test
            void shouldReThrowRuntimeExceptionAsStoreAccessException() {
                StoreInfo storeInfo = stores.getStore(Store.class, testStoreFilter());
                RuntimeExceptionThrowingStore store = storeInfo.getImplementation(RuntimeExceptionThrowingStore.class);
                assertThatThrownBy(() -> store.setContent(new Object(), new ByteArrayInputStream("".getBytes())))
                    .isInstanceOf(UnsupportedOperationException.class);
            }
        }

        @Nested

        @DisplayName("Given there is a store exception translator registered")

        class ThereIsAStoreExceptionTranslatorRegistered {
            @BeforeEach
            void setUp() {
                // All TestContentStore methods throw an UnsupportedOperationException, this test relies on this
                TestStoreFactoryBean factory = new TestStoreFactoryBean(RuntimeExceptionThrowingStore.class);
                factory.setBeanClassLoader(this.getClass().getClassLoader());

                context = new GenericApplicationContext();
                context.registerBean("factory", StoreFactory.class, () -> factory);

                context.registerBean("translator", StoreExceptionTranslator.class,
                    () -> re -> new StoreAccessException(re.getMessage(), re));
                context.refresh();
                stores = new StoresImpl(context);
                stores.afterPropertiesSet();
            }

            @Test
            void shouldReThrowRuntimeExceptionAsStoreAccessException() {
                StoreInfo storeInfo = stores.getStore(Store.class, testStoreFilter());
                RuntimeExceptionThrowingStore store = storeInfo.getImplementation(RuntimeExceptionThrowingStore.class);
                assertThatThrownBy(() -> store.setContent(new Object(), new ByteArrayInputStream("".getBytes())))
                    .isInstanceOf(StoreAccessException.class);
            }
        }
    }
}
