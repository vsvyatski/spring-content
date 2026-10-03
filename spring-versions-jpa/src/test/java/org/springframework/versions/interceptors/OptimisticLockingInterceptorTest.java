package org.springframework.versions.interceptors;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Version;

import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.ReflectionUtils;


public class OptimisticLockingInterceptorTest {

    private OptimisticLockingInterceptor interceptor;

    private Object result;

    //mocks
    private EntityManager em;
    private ProxyMethodInvocation mi;
    private Object entity;

    
    @Nested
    class OptimisticLockInterceptorCases {
        @Nested
        class Invoke {
            @Nested
            class WhenTheMethodInvocationIsGetContent {
                @BeforeEach
                void setUp() throws Throwable {
                    em = mock(EntityManager.class);

                    mi = mock(ProxyMethodInvocation.class);

                    entity = new TestEntity();
                                            when(mi.getMethod()).thenReturn(ReflectionUtils.findMethod(ContentStore.class, "getContent", Object.class));
                                            when(mi.getArguments()).thenReturn(new Object[]{entity});
                                            when(em.merge(entity)).thenReturn(entity);

                    interceptor = new OptimisticLockingInterceptor(em);

                    result = interceptor.invoke(mi);
                }
                @Test
                void shouldLockTheEntityAndProceed() throws Throwable {
                    verify(em).lock(entity, LockModeType.OPTIMISTIC);
                                            verify(mi).setArguments(entity);
                                            verify(mi).proceed();
                }
            }
            @Nested
            class WhenTheMethodInvocationIsGetContentWithPropertyPath {
                @BeforeEach
                void setUp() throws Throwable {
                    em = mock(EntityManager.class);

                    mi = mock(ProxyMethodInvocation.class);

                    entity = new TestEntity();
                                            when(mi.getMethod()).thenReturn(ReflectionUtils.findMethod(ContentStore.class, "getContent", Object.class, PropertyPath.class));
                                            when(mi.getArguments()).thenReturn(new Object[]{entity, PropertyPath.from("foo")});
                                            when(em.merge(entity)).thenReturn(entity);

                    interceptor = new OptimisticLockingInterceptor(em);

                    result = interceptor.invoke(mi);
                }
                @Test
                void shouldLockTheEntityAndProceed() throws Throwable {
                    verify(em).lock(entity, LockModeType.OPTIMISTIC);
                                            verify(mi).setArguments(entity, PropertyPath.from("foo"));
                                            verify(mi).proceed();
                }
            }
            @Nested
            class WhenTheMethodInvocationIsSetContent {
                @Nested
                class Tests {
                    @BeforeEach
                    void setUp() throws Throwable {
                        em = mock(EntityManager.class);

                        mi = mock(ProxyMethodInvocation.class);

                        entity = new TestEntity();
                                                when(mi.getMethod()).thenReturn(ReflectionUtils.findMethod(ContentStore.class, "setContent", Object.class, InputStream.class));
                                                when(mi.getArguments()).thenReturn(new Object[]{entity, new ByteArrayInputStream("".getBytes())});
                                                when(em.merge(entity)).thenReturn(entity);
                                                when(mi.proceed()).thenReturn(entity);

                        interceptor = new OptimisticLockingInterceptor(em);

                        result = interceptor.invoke(mi);
                    }
                    @Test
                    void shouldLockTheEntityAndProceed() throws Throwable {
                        assertThat(result).isNotNull();
                                                verify(em).lock(entity, LockModeType.OPTIMISTIC);
                                                verify(mi).setArguments(eq(entity), any());
                                                verify(mi).proceed();
                                                assertThat(((TestEntity) entity).getVersion()).isEqualTo(1L);
                    }
                }
                @Nested
                class WhenTheEntityIsNotVersioned {
                    @BeforeEach
                    void setUp() throws Throwable {
                        em = mock(EntityManager.class);

                        mi = mock(ProxyMethodInvocation.class);

                        entity = new TestEntity();
                                                when(mi.getMethod()).thenReturn(ReflectionUtils.findMethod(ContentStore.class, "setContent", Object.class, InputStream.class));
                                                when(mi.getArguments()).thenReturn(new Object[]{entity, new ByteArrayInputStream("".getBytes())});
                                                when(em.merge(entity)).thenReturn(entity);
                                                when(mi.proceed()).thenReturn(entity);

                        entity = new TestEntityUnversioned();

                        interceptor = new OptimisticLockingInterceptor(em);

                        result = interceptor.invoke(mi);
                    }
                    @Test
                    void shouldStillProceed() throws Throwable {
                        verify(mi).proceed();
                    }
                }
            }
            @Nested
            class WhenTheMethodInvocationIsSetContentWithPropertyPath {
                @Nested
                class Tests {
                    @BeforeEach
                    void setUp() throws Throwable {
                        em = mock(EntityManager.class);

                        mi = mock(ProxyMethodInvocation.class);

                        entity = new TestEntity();
                                                when(mi.getMethod()).thenReturn(ReflectionUtils.findMethod(ContentStore.class, "setContent", Object.class, PropertyPath.class, InputStream.class));
                                                when(mi.getArguments()).thenReturn(new Object[]{entity, PropertyPath.from("foo"), new ByteArrayInputStream("".getBytes())});
                                                when(em.merge(entity)).thenReturn(entity);
                                                when(mi.proceed()).thenReturn(entity);

                        interceptor = new OptimisticLockingInterceptor(em);

                        result = interceptor.invoke(mi);
                    }
                    @Test
                    void shouldLockTheEntityAndProceed() throws Throwable {
                        assertThat(result).isNotNull();
                                                verify(em).lock(entity, LockModeType.OPTIMISTIC);
                                                verify(mi).setArguments(eq(entity), eq(PropertyPath.from("foo")), any(ByteArrayInputStream.class));
                                                verify(mi).proceed();
                                                assertThat(((TestEntity) entity).getVersion()).isEqualTo(1L);
                    }
                }
                @Nested
                class WhenTheEntityIsNotVersioned {
                    @BeforeEach
                    void setUp() throws Throwable {
                        em = mock(EntityManager.class);

                        mi = mock(ProxyMethodInvocation.class);

                        entity = new TestEntity();
                                                when(mi.getMethod()).thenReturn(ReflectionUtils.findMethod(ContentStore.class, "setContent", Object.class, PropertyPath.class, InputStream.class));
                                                when(mi.getArguments()).thenReturn(new Object[]{entity, PropertyPath.from("foo"), new ByteArrayInputStream("".getBytes())});
                                                when(em.merge(entity)).thenReturn(entity);
                                                when(mi.proceed()).thenReturn(entity);

                        entity = new TestEntityUnversioned();

                        interceptor = new OptimisticLockingInterceptor(em);

                        result = interceptor.invoke(mi);
                    }
                    @Test
                    void shouldStillProceed() throws Throwable {
                        verify(mi).proceed();
                    }
                }
            }
            @Nested
            class WhenTheMethodInvocationIsSetContentWithResource {
                @Nested
                class Tests {
                    @BeforeEach
                    void setUp() throws Throwable {
                        em = mock(EntityManager.class);

                        mi = mock(ProxyMethodInvocation.class);

                        entity = new TestEntity();
                                                when(mi.getMethod()).thenReturn(ReflectionUtils.findMethod(ContentStore.class, "setContent", Object.class, Resource.class));
                                                when(mi.getArguments()).thenReturn(new Object[]{entity, new FileSystemResource("")});
                                                when(em.merge(entity)).thenReturn(entity);
                                                when(mi.proceed()).thenReturn(entity);

                        interceptor = new OptimisticLockingInterceptor(em);

                        result = interceptor.invoke(mi);
                    }
                    @Test
                    void shouldLockTheEntityAndProceed() throws Throwable {
                        assertThat(result).isNotNull();
                                                verify(em).lock(entity, LockModeType.OPTIMISTIC);
                                                verify(mi).setArguments(eq(entity), any());
                                                verify(mi).proceed();
                                                assertThat(((TestEntity) entity).getVersion()).isEqualTo(1L);
                    }
                }
                @Nested
                class WhenTheEntityIsNotVersioned {
                    @BeforeEach
                    void setUp() throws Throwable {
                        em = mock(EntityManager.class);

                        mi = mock(ProxyMethodInvocation.class);

                        entity = new TestEntity();
                                                when(mi.getMethod()).thenReturn(ReflectionUtils.findMethod(ContentStore.class, "setContent", Object.class, Resource.class));
                                                when(mi.getArguments()).thenReturn(new Object[]{entity, new FileSystemResource("")});
                                                when(em.merge(entity)).thenReturn(entity);
                                                when(mi.proceed()).thenReturn(entity);

                        entity = new TestEntityUnversioned();

                        interceptor = new OptimisticLockingInterceptor(em);

                        result = interceptor.invoke(mi);
                    }
                    @Test
                    void shouldStillProceed() throws Throwable {
                        verify(mi).proceed();
                    }
                }
            }
            @Nested
            class WhenTheMethodInvocationIsSetContentWithPropertyPathAndResource {
                @Nested
                class Tests {
                    @BeforeEach
                    void setUp() throws Throwable {
                        em = mock(EntityManager.class);

                        mi = mock(ProxyMethodInvocation.class);

                        entity = new TestEntity();
                                                when(mi.getMethod()).thenReturn(ReflectionUtils.findMethod(ContentStore.class, "setContent", Object.class, PropertyPath.class, Resource.class));
                                                when(mi.getArguments()).thenReturn(new Object[]{entity, PropertyPath.from("foo"), new FileSystemResource("")});
                                                when(em.merge(entity)).thenReturn(entity);
                                                when(mi.proceed()).thenReturn(entity);

                        interceptor = new OptimisticLockingInterceptor(em);

                        result = interceptor.invoke(mi);
                    }
                    @Test
                    void shouldLockTheEntityAndProceed() throws Throwable {
                        assertThat(result).isNotNull();
                                                verify(em).lock(entity, LockModeType.OPTIMISTIC);
                                                verify(mi).setArguments(eq(entity), eq(PropertyPath.from("foo")), any(FileSystemResource.class));
                                                verify(mi).proceed();
                                                assertThat(((TestEntity) entity).getVersion()).isEqualTo(1L);
                    }
                }
                @Nested
                class WhenTheEntityIsNotVersioned {
                    @BeforeEach
                    void setUp() throws Throwable {
                        em = mock(EntityManager.class);

                        mi = mock(ProxyMethodInvocation.class);

                        entity = new TestEntity();
                                                when(mi.getMethod()).thenReturn(ReflectionUtils.findMethod(ContentStore.class, "setContent", Object.class, PropertyPath.class, Resource.class));
                                                when(mi.getArguments()).thenReturn(new Object[]{entity, PropertyPath.from("foo"), new FileSystemResource("")});
                                                when(em.merge(entity)).thenReturn(entity);
                                                when(mi.proceed()).thenReturn(entity);

                        entity = new TestEntityUnversioned();

                        interceptor = new OptimisticLockingInterceptor(em);

                        result = interceptor.invoke(mi);
                    }
                    @Test
                    void shouldStillProceed() throws Throwable {
                        verify(mi).proceed();
                    }
                }
            }
            @Nested
            class WhenTheMethodInvocationIsUnsetContent {
                @Nested
                class Tests {
                    @BeforeEach
                    void setUp() throws Throwable {
                        em = mock(EntityManager.class);

                        mi = mock(ProxyMethodInvocation.class);

                        entity = new TestEntity();
                                                when(mi.getMethod()).thenReturn(ReflectionUtils.findMethod(ContentStore.class, "unsetContent", Object.class));
                                                when(mi.getArguments()).thenReturn(new Object[]{entity});
                                                when(em.merge(entity)).thenReturn(entity);
                                                when(mi.proceed()).thenReturn(entity);

                        interceptor = new OptimisticLockingInterceptor(em);

                        result = interceptor.invoke(mi);
                    }
                    @Test
                    void shouldLockTheEntityAndProceed() throws Throwable {
                        assertThat(result).isNotNull();
                                                verify(em).lock(entity, LockModeType.OPTIMISTIC);
                                                verify(mi).setArguments(eq(entity));
                                                verify(mi).proceed();
                                                assertThat(((TestEntity) entity).getVersion()).isEqualTo(1L);
                    }
                }
                @Nested
                class WhenTheEntityIsNotVersioned {
                    @BeforeEach
                    void setUp() throws Throwable {
                        em = mock(EntityManager.class);

                        mi = mock(ProxyMethodInvocation.class);

                        entity = new TestEntity();
                                                when(mi.getMethod()).thenReturn(ReflectionUtils.findMethod(ContentStore.class, "unsetContent", Object.class));
                                                when(mi.getArguments()).thenReturn(new Object[]{entity});
                                                when(em.merge(entity)).thenReturn(entity);
                                                when(mi.proceed()).thenReturn(entity);

                        entity = new TestEntityUnversioned();

                        interceptor = new OptimisticLockingInterceptor(em);

                        result = interceptor.invoke(mi);
                    }
                    @Test
                    void shouldStillProceed() throws Throwable {
                        verify(mi).proceed();
                    }
                }
            }
            @Nested
            class WhenTheMethodInvocationIsUnsetContentWithPropertyPath {
                @Nested
                class Tests {
                    @BeforeEach
                    void setUp() throws Throwable {
                        em = mock(EntityManager.class);

                        mi = mock(ProxyMethodInvocation.class);

                        entity = new TestEntity();
                                                when(mi.getMethod()).thenReturn(ReflectionUtils.findMethod(ContentStore.class, "unsetContent", Object.class, PropertyPath.class));
                                                when(mi.getArguments()).thenReturn(new Object[]{entity, PropertyPath.from("foo")});
                                                when(em.merge(entity)).thenReturn(entity);
                                                when(mi.proceed()).thenReturn(entity);

                        interceptor = new OptimisticLockingInterceptor(em);

                        result = interceptor.invoke(mi);
                    }
                    @Test
                    void shouldLockTheEntityAndProceed() throws Throwable {
                        assertThat(result).isNotNull();
                                                verify(em).lock(entity, LockModeType.OPTIMISTIC);
                                                verify(mi).setArguments(eq(entity), eq(PropertyPath.from("foo")));
                                                verify(mi).proceed();
                                                assertThat(((TestEntity) entity).getVersion()).isEqualTo(1L);
                    }
                }
                @Nested
                class WhenTheEntityIsNotVersioned {
                    @BeforeEach
                    void setUp() throws Throwable {
                        em = mock(EntityManager.class);

                        mi = mock(ProxyMethodInvocation.class);

                        entity = new TestEntity();
                                                when(mi.getMethod()).thenReturn(ReflectionUtils.findMethod(ContentStore.class, "unsetContent", Object.class, PropertyPath.class));
                                                when(mi.getArguments()).thenReturn(new Object[]{entity, PropertyPath.from("foo")});
                                                when(em.merge(entity)).thenReturn(entity);
                                                when(mi.proceed()).thenReturn(entity);

                        entity = new TestEntityUnversioned();

                        interceptor = new OptimisticLockingInterceptor(em);

                        result = interceptor.invoke(mi);
                    }
                    @Test
                    void shouldStillProceed() throws Throwable {
                        verify(mi).proceed();
                    }
                }
            }
        }
    }


    private static class TestEntity {
        @Version
        private Long version = 0L;

        public Long getVersion() {
            return version;
        }

        public void setVersion(Long version) {
            this.version = version;
        }
    }

    private static class TestEntityUnversioned {
    }
}
