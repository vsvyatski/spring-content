package org.springframework.versions.interceptors;

import internal.org.springframework.versions.AuthenticationFacade;
import internal.org.springframework.versions.LockingService;
import jakarta.persistence.Id;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.security.core.Authentication;
import org.springframework.util.ReflectionUtils;
import org.springframework.versions.LockOwnerException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class PessimisticLockingInterceptorTest {

    private PessimisticLockingInterceptor interceptor;

    // mocks
    private LockingService locker;
    private AuthenticationFacade auth;
    private ProxyMethodInvocation mi;

    private Exception e;

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

    @Nested
    class PessimisticLockingInterceptorCases {
        @Nested
        class Invoke {
            @Nested
            @DisplayName("Given a method invocation")
            class MethodInvocation {
                @Nested
                @DisplayName("Given the method is set content")
                class MethodIsSetContent {
                    @Nested
                    @DisplayName("When there is no lock owner")
                    class NoLockOwner {
                        @BeforeEach
                        void setUp() throws Throwable {
                            locker = mock(LockingService.class);
                            auth = mock(AuthenticationFacade.class);

                            mi = mock(ProxyMethodInvocation.class);

                            when(mi.getMethod()).thenReturn(ReflectionUtils.findMethod(ContentStore.class, "setContent", Object.class, InputStream.class));
                            when(mi.getArguments()).thenReturn(new Object[]{new TestEntity(), new ByteArrayInputStream("".getBytes())});

                            when(locker.lockOwner(0L)).thenReturn(null);

                            interceptor = new PessimisticLockingInterceptor(locker, auth);

                            try {
                                interceptor.invoke(mi);
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
                    @DisplayName("When the principal is the lock owner")
                    class PrincipalIsLockOwner {
                        private Authentication principal;

                        @BeforeEach
                        void setUp() throws Throwable {
                            locker = mock(LockingService.class);
                            auth = mock(AuthenticationFacade.class);

                            mi = mock(ProxyMethodInvocation.class);

                            when(mi.getMethod()).thenReturn(ReflectionUtils.findMethod(ContentStore.class, "setContent", Object.class, InputStream.class));
                            when(mi.getArguments()).thenReturn(new Object[]{new TestEntity(), new ByteArrayInputStream("".getBytes())});

                            principal = mock(Authentication.class);
                            when(auth.getAuthentication()).thenReturn(principal);
                            when(locker.lockOwner(0L)).thenReturn(principal);
                            when(locker.isLockOwner(eq(0L), any())).thenReturn(true);

                            interceptor = new PessimisticLockingInterceptor(locker, auth);

                            try {
                                interceptor.invoke(mi);
                            } catch (Exception e) {
                                PessimisticLockingInterceptorTest.this.e = e;
                            }
                        }

                        @Test
                        void shouldProceed() throws Throwable {
                            verify(locker).lockOwner(0L);
                            verify(locker).isLockOwner(0L, principal);
                            verify(mi).proceed();
                        }
                    }

                    @Nested
                    @DisplayName("When the principal is not the lock owner")
                    class PrincipalIsNotLockOwner {
                        @BeforeEach
                        void setUp() throws Throwable {
                            locker = mock(LockingService.class);
                            auth = mock(AuthenticationFacade.class);

                            mi = mock(ProxyMethodInvocation.class);

                            when(mi.getMethod()).thenReturn(ReflectionUtils.findMethod(ContentStore.class, "setContent", Object.class, InputStream.class));
                            when(mi.getArguments()).thenReturn(new Object[]{new TestEntity(), new ByteArrayInputStream("".getBytes())});

                            Authentication lockOwner = mock(Authentication.class);
                            Authentication principal = mock(Authentication.class);
                            when(auth.getAuthentication()).thenReturn(principal);
                            when(locker.lockOwner(0L)).thenReturn(lockOwner);
                            when(locker.isLockOwner(eq(0L), any())).thenReturn(false);

                            interceptor = new PessimisticLockingInterceptor(locker, auth);

                            try {
                                interceptor.invoke(mi);
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
                    @DisplayName("When the entity doesn't have an ID")
                    class EntityDoesntHaveID {
                        @BeforeEach
                        void setUp() throws Throwable {
                            locker = mock(LockingService.class);
                            auth = mock(AuthenticationFacade.class);

                            mi = mock(ProxyMethodInvocation.class);

                            when(mi.getMethod()).thenReturn(ReflectionUtils.findMethod(ContentStore.class, "setContent", Object.class, InputStream.class));
                            when(mi.getArguments()).thenReturn(new Object[]{new Object(), new ByteArrayInputStream("".getBytes())});

                            interceptor = new PessimisticLockingInterceptor(locker, auth);

                            try {
                                interceptor.invoke(mi);
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
}
