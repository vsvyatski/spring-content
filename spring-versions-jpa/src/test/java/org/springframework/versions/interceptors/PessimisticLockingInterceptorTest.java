package org.springframework.versions.interceptors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import jakarta.persistence.Id;

import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.security.core.Authentication;
import org.springframework.util.ReflectionUtils;
import org.springframework.versions.LockOwnerException;

import internal.org.springframework.versions.AuthenticationFacade;
import internal.org.springframework.versions.LockingService;

public class PessimisticLockingInterceptorTest {

    private PessimisticLockingInterceptor interceptor;

    // mocks
    private LockingService locker;
    private AuthenticationFacade auth;
    private ProxyMethodInvocation mi;

    private Object result;
    private Exception e;

    private TestEntity entity;

    private Authentication principal, lockOwner;

    
    @Nested
    class PessimisticLockingInterceptorCases {
        @Nested
        class Invoke {
            @Nested
            class GivenAMethodInvocation {
                @Nested
                class GivenTheMethodIsSetContent {
                    @Nested
                    class WhenThereIsNoLockOwner {
                        @BeforeEach
                        void setUp() throws Throwable {
                            locker = mock(LockingService.class);
                            auth = mock(AuthenticationFacade.class);

                            mi = mock(ProxyMethodInvocation.class);

                            mi = mock(ProxyMethodInvocation.class);

                            when(mi.getMethod()).thenReturn(ReflectionUtils.findMethod(ContentStore.class, "setContent", Object.class, InputStream.class));
                            when(mi.getArguments()).thenReturn(new Object[]{new TestEntity(), new ByteArrayInputStream("".getBytes())});

                            when(locker.lockOwner(0L)).thenReturn(null);

                            interceptor = new PessimisticLockingInterceptor(locker, auth);

                            try {
                                result = interceptor.invoke(mi);
                            } catch (Exception e) {
                                PessimisticLockingInterceptorTest.this.e = e;
                            }

                        }

                        @Test
                        void shouldProceed() throws Throwable {
                            verify(locker).lockOwner(0L);
                            verify(mi).proceed();

                        }

                    }

                    @Nested
                    class WhenThePrincipalIsTheLockOwner {
                        @BeforeEach
                        void setUp() throws Throwable {
                            locker = mock(LockingService.class);
                            auth = mock(AuthenticationFacade.class);

                            mi = mock(ProxyMethodInvocation.class);

                            mi = mock(ProxyMethodInvocation.class);

                            when(mi.getMethod()).thenReturn(ReflectionUtils.findMethod(ContentStore.class, "setContent", Object.class, InputStream.class));
                            when(mi.getArguments()).thenReturn(new Object[]{new TestEntity(), new ByteArrayInputStream("".getBytes())});

                            principal = mock(Authentication.class);
                            when(auth.getAuthentication()).thenReturn(principal);
                            when(locker.lockOwner(0L)).thenReturn(principal);
                            when(locker.isLockOwner(eq(0L), any())).thenReturn(true);

                            interceptor = new PessimisticLockingInterceptor(locker, auth);

                            try {
                                result = interceptor.invoke(mi);
                            } catch (Exception e) {
                                PessimisticLockingInterceptorTest.this.e = e;
                            }

                        }

                        @Test
                        void shouldProceed() throws Throwable {
                            verify(locker).lockOwner(0L);
                            verify(locker).isLockOwner(0L,  principal);
                            verify(mi).proceed();

                        }

                    }

                    @Nested
                    class WhenThePrincipalIsNotTheLockOwner {
                        @BeforeEach
                        void setUp() throws Throwable {
                            locker = mock(LockingService.class);
                            auth = mock(AuthenticationFacade.class);

                            mi = mock(ProxyMethodInvocation.class);

                            mi = mock(ProxyMethodInvocation.class);

                            when(mi.getMethod()).thenReturn(ReflectionUtils.findMethod(ContentStore.class, "setContent", Object.class, InputStream.class));
                            when(mi.getArguments()).thenReturn(new Object[]{new TestEntity(), new ByteArrayInputStream("".getBytes())});

                            lockOwner = mock(Authentication.class);
                            principal = mock(Authentication.class);
                            when(auth.getAuthentication()).thenReturn(principal);
                            when(locker.lockOwner(0L)).thenReturn(lockOwner);
                            when(locker.isLockOwner(eq(0L), any())).thenReturn(false);

                            interceptor = new PessimisticLockingInterceptor(locker, auth);

                            try {
                                result = interceptor.invoke(mi);
                            } catch (Exception e) {
                                PessimisticLockingInterceptorTest.this.e = e;
                            }

                        }

                        @Test
                        void shouldProceed() {
                            assertThat(e).isInstanceOf(LockOwnerException.class);

                        }

                    }

                    @Nested
                    class WhenTheEntityDoesnTHaveAnID {
                        @BeforeEach
                        void setUp() throws Throwable {
                            locker = mock(LockingService.class);
                            auth = mock(AuthenticationFacade.class);

                            mi = mock(ProxyMethodInvocation.class);

                            mi = mock(ProxyMethodInvocation.class);

                            when(mi.getMethod()).thenReturn(ReflectionUtils.findMethod(ContentStore.class, "setContent", Object.class, InputStream.class));
                            when(mi.getArguments()).thenReturn(new Object[]{new TestEntity(), new ByteArrayInputStream("".getBytes())});

                            when(mi.getArguments()).thenReturn(new Object[]{new Object(), new ByteArrayInputStream("".getBytes())});

                            interceptor = new PessimisticLockingInterceptor(locker, auth);

                            try {
                                result = interceptor.invoke(mi);
                            } catch (Exception e) {
                                PessimisticLockingInterceptorTest.this.e = e;
                            }

                        }

                        @Test
                        void shouldProceed() throws Throwable {
                            verify(mi).proceed();

                        }

                    }

                }

            }

        }

    }

    public static class TestEntity {
        @Id
        private Long id = 0L;

        public TestEntity() {
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }
    }

    public static class TestEntity2 {
        @org.springframework.data.annotation.Id
        private Long id = 0L;

        public TestEntity2() {
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }
    }
}
