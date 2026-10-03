package internal.org.springframework.content.commons.store.factory;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.assertj.core.api.Assertions.assertThat;

import internal.org.springframework.content.commons.config.StoreFragment;
import internal.org.springframework.content.commons.config.StoreFragments;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.io.IOUtils;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.store.events.AfterStoreEvent;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.commons.store.events.StoreEvent;
import org.springframework.content.commons.store.events.*;
import org.springframework.content.commons.store.AssociativeStore;
import org.springframework.content.commons.store.Store;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.security.util.SimpleMethodInvocation;
import org.springframework.util.ReflectionUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@SuppressWarnings("unchecked")
public class StoreMethodInterceptorTest {

    private static final Method getResourceMethod;
    private static final Method getResourceEntityMethod;
    private static final Method associateMethod;
    private static final Method unassociateMethod;
    private static final Method getContentMethod;
    private static final Method setContentMethod;
    private static final Method setContentFromResourceMethod;
    private static final Method unsetContentMethod;
    private static final Method toStringMethod;

    static {
        getResourceMethod = ReflectionUtils.findMethod(Store.class, "getResource", Serializable.class);
        getResourceEntityMethod = ReflectionUtils.findMethod(AssociativeStore.class, "getResource", Object.class);
        associateMethod = ReflectionUtils.findMethod(AssociativeStore.class, "associate", Object.class, Serializable.class);
        unassociateMethod = ReflectionUtils.findMethod(AssociativeStore.class, "unassociate", Object.class);
        getContentMethod = ReflectionUtils.findMethod(ContentStore.class, "getContent", Object.class);
        setContentMethod = ReflectionUtils.findMethod(ContentStore.class, "setContent", Object.class, InputStream.class);
        setContentFromResourceMethod = ReflectionUtils.findMethod(ContentStore.class, "setContent", Object.class, Resource.class);
        unsetContentMethod = ReflectionUtils.findMethod(ContentStore.class, "unsetContent", Object.class);
        toStringMethod = ReflectionUtils.findMethod(Object.class, "toString");
    }

    private StoreMethodInterceptor interceptor;

    // mocks
    private ContentStore<Object, Serializable> store;
    private MethodInvocation invocation;
    private ApplicationEventPublisher publisher;

    private Object result;
    private Exception e;

    private ByteArrayInputStream modifiedStream = null;

    
    @Nested
    class InvokeCases {
        @Nested
        class GetContentCases {
            @Nested
            class Tests {
                @BeforeEach
                void setUp() throws Throwable {
                    store = mock(ContentStore.class);
                                    publisher = mock(ApplicationEventPublisher.class);

                    result = new ByteArrayInputStream(new byte[]{});

                                        store = mock(ContentStore.class);
                                        when(store.getContent(any())).thenReturn((InputStream) result);

                                        invocation = new TestMethodInvocation(store, getContentMethod, new Object());

                    interceptor = new StoreMethodInterceptor();
                                    StoreFragments fragments = new StoreFragments(Collections.singletonList(new StoreFragment(TestContentStore.class, new StoreImpl(store, publisher, Paths.get(System.getProperty("java.io.tmpdir"))))));
                                    interceptor.setStoreFragments(fragments);
                                    try {
                                        interceptor.invoke(invocation);
                                    } catch (Exception invokeException) {
                                        e = invokeException;
                                    }
                }
                @Test
                void shouldProceed() throws Throwable {
                    assertThat(e).isNull();

                                        ArgumentCaptor<AfterStoreEvent> captor = ArgumentCaptor.forClass(AfterStoreEvent.class);
                                        InOrder inOrder = Mockito.inOrder(publisher, store);

                                        inOrder.verify(publisher, times(1)).publishEvent(argThat(StoreEvent.class::isInstance));
                                        inOrder.verify(store).getContent(any());
                                        inOrder.verify(publisher, times(1)).publishEvent(captor.capture());
                                        assertThat(captor.getValue().getResult()).isEqualTo(result);
                }
            }
            @Nested
            class WhenGetContentIsInvokedWithIllegalArguments {
                @BeforeEach
                void setUp() throws Throwable {
                    store = mock(ContentStore.class);
                                    publisher = mock(ApplicationEventPublisher.class);

                    result = new ByteArrayInputStream(new byte[]{});

                                        store = mock(ContentStore.class);
                                        when(store.getContent(any())).thenReturn((InputStream) result);

                                        invocation = new TestMethodInvocation(store, getContentMethod, new Object());

                    invocation = new TestMethodInvocation(store, getContentMethod);

                    interceptor = new StoreMethodInterceptor();
                                    StoreFragments fragments = new StoreFragments(Collections.singletonList(new StoreFragment(TestContentStore.class, new StoreImpl(store, publisher, Paths.get(System.getProperty("java.io.tmpdir"))))));
                                    interceptor.setStoreFragments(fragments);
                                    try {
                                        interceptor.invoke(invocation);
                                    } catch (Exception invokeException) {
                                        e = invokeException;
                                    }
                }
                @Test
                void shouldProceed() throws Throwable {
                    assertThat(e).isNotNull();
                }
            }
        }
        @Nested
        class SetContentCases {
            @Nested
            class Tests {
                @BeforeEach
                void setUp() throws Throwable {
                    store = mock(ContentStore.class);
                                    publisher = mock(ApplicationEventPublisher.class);

                    result = new Object();

                                        store = mock(ContentStore.class);
                                        when(store.setContent(any(), any(InputStream.class))).thenReturn(result);

                                        invocation = new TestMethodInvocation(store, setContentMethod, new Object(), new ByteArrayInputStream("test".getBytes()));

                    interceptor = new StoreMethodInterceptor();
                                    StoreFragments fragments = new StoreFragments(Collections.singletonList(new StoreFragment(TestContentStore.class, new StoreImpl(store, publisher, Paths.get(System.getProperty("java.io.tmpdir"))))));
                                    interceptor.setStoreFragments(fragments);
                                    try {
                                        interceptor.invoke(invocation);
                                    } catch (Exception invokeException) {
                                        e = invokeException;
                                    }
                }
                @Test
                void shouldProceed() throws Throwable {
                    assertThat(e).isNull();

                                        ArgumentCaptor<BeforeSetContentEvent> beforeArgCaptor = ArgumentCaptor.forClass(BeforeSetContentEvent.class);
                                        ArgumentCaptor<InputStream> setContentArgCaptor = ArgumentCaptor.forClass(InputStream.class);
                                        ArgumentCaptor<AfterStoreEvent> afterArgCaptor = ArgumentCaptor.forClass(AfterStoreEvent.class);
                                        InOrder inOrder = Mockito.inOrder(publisher, store);

                                        inOrder.verify(publisher, times(1)).publishEvent(beforeArgCaptor.capture());
                                        assertThat(beforeArgCaptor.getValue().getResource()).isNull();
                                        assertThat(beforeArgCaptor.getValue().getInputStream()).isNotNull();

                                        inOrder.verify(store).setContent(any(), setContentArgCaptor.capture());
                                        try (InputStream setContentInputStream = setContentArgCaptor.getValue()) {
                                            assertThat(IOUtils.toString(setContentInputStream)).isEqualTo("test");
                                        }

                                        inOrder.verify(publisher, times(1)).publishEvent(afterArgCaptor.capture());
                                        assertThat(afterArgCaptor.getValue().getResult()).isEqualTo(result);
                }
            }
            @Nested
            class WhenTheBeforeSetContentEventConsumesTheEntireInputStream {
                @BeforeEach
                void setUp() throws Throwable {
                    store = mock(ContentStore.class);
                                    publisher = mock(ApplicationEventPublisher.class);

                    result = new Object();

                                        store = mock(ContentStore.class);
                                        when(store.setContent(any(), any(InputStream.class))).thenReturn(result);

                                        invocation = new TestMethodInvocation(store, setContentMethod, new Object(), new ByteArrayInputStream("test".getBytes()));

                    onBeforeSetContentPublishEvent((invocationOnMock) -> {
                                            try (InputStream is = ((BeforeSetContentEvent) invocationOnMock.getArgument(0)).getInputStream()) {
                                                assertThat(IOUtils.toString(is, Charset.defaultCharset())).isEqualTo("test");
                                            }
                                            return null;
                                        });

                    interceptor = new StoreMethodInterceptor();
                                    StoreFragments fragments = new StoreFragments(Collections.singletonList(new StoreFragment(TestContentStore.class, new StoreImpl(store, publisher, Paths.get(System.getProperty("java.io.tmpdir"))))));
                                    interceptor.setStoreFragments(fragments);
                                    try {
                                        interceptor.invoke(invocation);
                                    } catch (Exception invokeException) {
                                        e = invokeException;
                                    }
                }
                @Test
                void shouldStillReceiveTheInputStreamInTheSetContentInvocation() throws Throwable {
                    assertThat(e).isNull();

                                            ArgumentCaptor<InputStream> setContentArgCaptor = ArgumentCaptor.forClass(InputStream.class);
                                            ArgumentCaptor<AfterStoreEvent> afterArgCaptor = ArgumentCaptor.forClass(AfterStoreEvent.class);
                                            InOrder inOrder = Mockito.inOrder(publisher, store);

                                            inOrder.verify(publisher).publishEvent(argThat(BeforeSetContentEvent.class::isInstance));

                                            inOrder.verify(store).setContent(any(), setContentArgCaptor.capture());
                                            try (InputStream setContentInputStream = setContentArgCaptor.getValue()) {
                                                assertThat(IOUtils.toString(setContentInputStream, Charset.defaultCharset())).isEqualTo("test");
                                            }

                                            inOrder.verify(publisher, times(1)).publishEvent(afterArgCaptor.capture());
                                            assertThat(afterArgCaptor.getValue().getResult()).isEqualTo(result);
                }
            }
            @Nested
            class WhenTheBeforeSetContentEventConsumesPartialInputStream {
                @BeforeEach
                void setUp() throws Throwable {
                    store = mock(ContentStore.class);
                                    publisher = mock(ApplicationEventPublisher.class);

                    result = new Object();

                                        store = mock(ContentStore.class);
                                        when(store.setContent(any(), any(InputStream.class))).thenReturn(result);

                                        invocation = new TestMethodInvocation(store, setContentMethod, new Object(), new ByteArrayInputStream("test".getBytes()));

                    onBeforeSetContentPublishEvent((invocationOnMock) -> {
                                            InputStream is = ((BeforeSetContentEvent) invocationOnMock.getArgument(0)).getInputStream();
                                            assertThat((char) is.read()).isEqualTo('t');
                                            assertThat((char) is.read()).isEqualTo('e');
                                            return null;
                                        });

                    interceptor = new StoreMethodInterceptor();
                                    StoreFragments fragments = new StoreFragments(Collections.singletonList(new StoreFragment(TestContentStore.class, new StoreImpl(store, publisher, Paths.get(System.getProperty("java.io.tmpdir"))))));
                                    interceptor.setStoreFragments(fragments);
                                    try {
                                        interceptor.invoke(invocation);
                                    } catch (Exception invokeException) {
                                        e = invokeException;
                                    }
                }
                @Test
                void shouldStillReceiveTheInputStreamInTheSetContentInvocation() throws Throwable {
                    assertThat(e).isNull();

                                            InOrder inOrder = Mockito.inOrder(publisher, store);

                                            ArgumentCaptor<InputStream> setContentArgCaptor = ArgumentCaptor.forClass(InputStream.class);
                                            ArgumentCaptor<AfterStoreEvent> afterArgCaptor = ArgumentCaptor.forClass(AfterStoreEvent.class);

                                            inOrder.verify(publisher).publishEvent(argThat(BeforeSetContentEvent.class::isInstance));

                                            inOrder.verify(store).setContent(any(), setContentArgCaptor.capture());

                                            try (InputStream setContentInputStream = setContentArgCaptor.getValue()) {
                                                assertThat(IOUtils.toString(setContentInputStream, Charset.defaultCharset())).isEqualTo("test");
                                            }

                                            inOrder.verify(publisher, times(1)).publishEvent(afterArgCaptor.capture());
                                            assertThat(afterArgCaptor.getValue().getResult()).isEqualTo(result);
                }
            }
            @Nested
            class WhenTheBeforeSetContentEventDoesNotConsumeAnyOfTheInputStream {
                @BeforeEach
                void setUp() throws Throwable {
                    store = mock(ContentStore.class);
                                    publisher = mock(ApplicationEventPublisher.class);

                    result = new Object();

                                        store = mock(ContentStore.class);
                                        when(store.setContent(any(), any(InputStream.class))).thenReturn(result);

                                        invocation = new TestMethodInvocation(store, setContentMethod, new Object(), new ByteArrayInputStream("test".getBytes()));

                    onBeforeSetContentPublishEvent((invocationOnMock) -> null);

                    interceptor = new StoreMethodInterceptor();
                                    StoreFragments fragments = new StoreFragments(Collections.singletonList(new StoreFragment(TestContentStore.class, new StoreImpl(store, publisher, Paths.get(System.getProperty("java.io.tmpdir"))))));
                                    interceptor.setStoreFragments(fragments);
                                    try {
                                        interceptor.invoke(invocation);
                                    } catch (Exception invokeException) {
                                        e = invokeException;
                                    }
                }
                @Test
                void shouldStillReceiveTheInputStreamInTheSetContentInvocation() throws Throwable {
                    assertThat(e).isNull();

                                            InOrder inOrder = Mockito.inOrder(publisher, store);

                                            ArgumentCaptor<InputStream> setContentArgCaptor = ArgumentCaptor.forClass(InputStream.class);
                                            ArgumentCaptor<AfterStoreEvent> afterArgCaptor = ArgumentCaptor.forClass(AfterStoreEvent.class);

                                            inOrder.verify(publisher).publishEvent(argThat(BeforeSetContentEvent.class::isInstance));

                                            inOrder.verify(store).setContent(any(), setContentArgCaptor.capture());

                                            try (InputStream setContentInputStream = setContentArgCaptor.getValue()) {
                                                assertThat(IOUtils.toString(setContentInputStream, Charset.defaultCharset())).isEqualTo("test");
                                            }

                                            inOrder.verify(publisher, times(1)).publishEvent(afterArgCaptor.capture());
                                            assertThat(afterArgCaptor.getValue().getResult()).isEqualTo(result);
                }
            }
            @Nested
            class WhenTheBeforeSetContentEventReplacesTheInputStream {
                @BeforeEach
                void setUp() throws Throwable {
                    store = mock(ContentStore.class);
                                    publisher = mock(ApplicationEventPublisher.class);

                    result = new Object();

                                        store = mock(ContentStore.class);
                                        when(store.setContent(any(), any(InputStream.class))).thenReturn(result);

                                        invocation = new TestMethodInvocation(store, setContentMethod, new Object(), new ByteArrayInputStream("test".getBytes()));

                    onBeforeSetContentPublishEvent((invocationOnMock) -> {
                                            modifiedStream = new ByteArrayInputStream("encrypted".getBytes());
                                            ((BeforeSetContentEvent) invocationOnMock.getArgument(0)).setInputStream(modifiedStream);
                                            return null;
                                        });

                    interceptor = new StoreMethodInterceptor();
                                    StoreFragments fragments = new StoreFragments(Collections.singletonList(new StoreFragment(TestContentStore.class, new StoreImpl(store, publisher, Paths.get(System.getProperty("java.io.tmpdir"))))));
                                    interceptor.setStoreFragments(fragments);
                                    try {
                                        interceptor.invoke(invocation);
                                    } catch (Exception invokeException) {
                                        e = invokeException;
                                    }
                }
                @Test
                void shouldStillReceiveTheReplacedInputStreamInTheSetContentInvocation() throws Throwable {
                    assertThat(e).isNull();

                                            InOrder inOrder = Mockito.inOrder(publisher, store);

                                            ArgumentCaptor<InputStream> setContentArgCaptor = ArgumentCaptor.forClass(InputStream.class);
                                            ArgumentCaptor<AfterStoreEvent> afterArgCaptor = ArgumentCaptor.forClass(AfterStoreEvent.class);

                                            inOrder.verify(publisher).publishEvent(argThat(BeforeSetContentEvent.class::isInstance));

                                            inOrder.verify(store).setContent(any(), setContentArgCaptor.capture());

                                            try (InputStream setContentInputStream = setContentArgCaptor.getValue()) {
                                                assertThat(setContentInputStream).isEqualTo(modifiedStream);
                                            }

                                            inOrder.verify(publisher, times(1)).publishEvent(afterArgCaptor.capture());
                                            assertThat(afterArgCaptor.getValue().getResult()).isEqualTo(result);
                }
            }
            @Nested
            class WhenSetContentIsInvokedWithIllegalArguments {
                @BeforeEach
                void setUp() throws Throwable {
                    store = mock(ContentStore.class);
                                    publisher = mock(ApplicationEventPublisher.class);

                    result = new Object();

                                        store = mock(ContentStore.class);
                                        when(store.setContent(any(), any(InputStream.class))).thenReturn(result);

                                        invocation = new TestMethodInvocation(store, setContentMethod, new Object(), new ByteArrayInputStream("test".getBytes()));

                    invocation = new TestMethodInvocation(store, setContentMethod);

                    interceptor = new StoreMethodInterceptor();
                                    StoreFragments fragments = new StoreFragments(Collections.singletonList(new StoreFragment(TestContentStore.class, new StoreImpl(store, publisher, Paths.get(System.getProperty("java.io.tmpdir"))))));
                                    interceptor.setStoreFragments(fragments);
                                    try {
                                        interceptor.invoke(invocation);
                                    } catch (Exception invokeException) {
                                        e = invokeException;
                                    }
                }
                @Test
                void shouldProceed() throws Throwable {
                    assertThat(e).isNotNull();
                }
            }
        }
        @Nested
        class SetContentFromResourceCases {
            @BeforeEach
            void setUp() throws Throwable {
                store = mock(ContentStore.class);
                                publisher = mock(ApplicationEventPublisher.class);

                result = new Object();

                                    store = mock(ContentStore.class);
                                    when(store.setContent(any(), any(Resource.class))).thenReturn(result);

                                    invocation = new TestMethodInvocation(store, setContentFromResourceMethod, new Object(), new InputStreamResource(new ByteArrayInputStream("test".getBytes())));

                interceptor = new StoreMethodInterceptor();
                                StoreFragments fragments = new StoreFragments(Collections.singletonList(new StoreFragment(TestContentStore.class, new StoreImpl(store, publisher, Paths.get(System.getProperty("java.io.tmpdir"))))));
                                interceptor.setStoreFragments(fragments);
                                try {
                                    interceptor.invoke(invocation);
                                } catch (Exception invokeException) {
                                    e = invokeException;
                                }
            }
            @Test
            void shouldProceed() throws Throwable {
                assertThat(e).isNull();

                                    ArgumentCaptor<BeforeSetContentEvent> beforeArgCaptor = ArgumentCaptor.forClass(BeforeSetContentEvent.class);
                                    ArgumentCaptor<Resource> setContentArgCaptor = ArgumentCaptor.forClass(Resource.class);
                                    ArgumentCaptor<AfterStoreEvent> afterArgCaptor = ArgumentCaptor.forClass(AfterStoreEvent.class);
                                    InOrder inOrder = Mockito.inOrder(publisher, store);

                                    inOrder.verify(publisher, times(1)).publishEvent(beforeArgCaptor.capture());
                                    assertThat(beforeArgCaptor.getValue().getResource()).isNotNull();
                                    assertThat(beforeArgCaptor.getValue().getInputStream()).isNull();

                                    inOrder.verify(store).setContent(any(), setContentArgCaptor.capture());
                                    try (InputStream setContentInputStream = setContentArgCaptor.getValue().getInputStream()) {
                                        assertThat(IOUtils.toString(setContentInputStream)).isEqualTo("test");
                                    }

                                    inOrder.verify(publisher, times(1)).publishEvent(afterArgCaptor.capture());
                                    assertThat(afterArgCaptor.getValue().getResult()).isEqualTo(result);
            }
        }
        @Nested
        class UnsetContentCases {
            @Nested
            class WhenUnsetContentIsInvoked {
                @BeforeEach
                void setUp() throws Throwable {
                    store = mock(ContentStore.class);
                                    publisher = mock(ApplicationEventPublisher.class);

                    result = new Object();
                                        store = mock(ContentStore.class);
                                        when(store.unsetContent(any())).thenReturn(result);

                                        invocation = new TestMethodInvocation(store, unsetContentMethod, new Object());

                    interceptor = new StoreMethodInterceptor();
                                    StoreFragments fragments = new StoreFragments(Collections.singletonList(new StoreFragment(TestContentStore.class, new StoreImpl(store, publisher, Paths.get(System.getProperty("java.io.tmpdir"))))));
                                    interceptor.setStoreFragments(fragments);
                                    try {
                                        interceptor.invoke(invocation);
                                    } catch (Exception invokeException) {
                                        e = invokeException;
                                    }
                }
                @Test
                void shouldProceed() throws Throwable {
                    assertThat(e).isNull();

                                            InOrder inOrder = Mockito.inOrder(publisher, store);

                                            inOrder.verify(publisher).publishEvent(argThat(BeforeUnsetContentEvent.class::isInstance));
                                            inOrder.verify(store).unsetContent(any());

                                            ArgumentCaptor<AfterStoreEvent> captor = ArgumentCaptor.forClass(AfterStoreEvent.class);
                                            inOrder.verify(publisher, times(1)).publishEvent(captor.capture());
                                            assertThat(captor.getValue().getResult()).isEqualTo(result);
                }
            }
            @Nested
            class WhenUnsetContentIsInvokedWithIllegalArguments {
                @BeforeEach
                void setUp() throws Throwable {
                    store = mock(ContentStore.class);
                                    publisher = mock(ApplicationEventPublisher.class);

                    result = new Object();
                                        store = mock(ContentStore.class);
                                        when(store.unsetContent(any())).thenReturn(result);

                                        invocation = new TestMethodInvocation(store, unsetContentMethod, new Object());

                    invocation = new TestMethodInvocation(store, unsetContentMethod);

                    interceptor = new StoreMethodInterceptor();
                                    StoreFragments fragments = new StoreFragments(Collections.singletonList(new StoreFragment(TestContentStore.class, new StoreImpl(store, publisher, Paths.get(System.getProperty("java.io.tmpdir"))))));
                                    interceptor.setStoreFragments(fragments);
                                    try {
                                        interceptor.invoke(invocation);
                                    } catch (Exception invokeException) {
                                        e = invokeException;
                                    }
                }
                @Test
                void shouldNotPublishEvents() throws Throwable {
                    assertThat(e).isNotNull();
                }
            }
        }
        @Nested
        class GetResourceCases {
            @Nested
            class WhenGetResourceIsInvoked {
                @BeforeEach
                void setUp() throws Throwable {
                    store = mock(ContentStore.class);
                                    publisher = mock(ApplicationEventPublisher.class);

                    result = mock(Resource.class);

                                            store = mock(ContentStore.class);
                                            when(store.getResource(any(Serializable.class))).thenReturn((Resource) result);

                                            invocation = new TestMethodInvocation(store, getResourceMethod, new Serializable() {
                                            });

                    interceptor = new StoreMethodInterceptor();
                                    StoreFragments fragments = new StoreFragments(Collections.singletonList(new StoreFragment(TestContentStore.class, new StoreImpl(store, publisher, Paths.get(System.getProperty("java.io.tmpdir"))))));
                                    interceptor.setStoreFragments(fragments);
                                    try {
                                        interceptor.invoke(invocation);
                                    } catch (Exception invokeException) {
                                        e = invokeException;
                                    }
                }
                @Test
                void shouldProceed() throws Throwable {
                    assertThat(e).isNull();

                                            ArgumentCaptor<AfterStoreEvent> captor = ArgumentCaptor.forClass(AfterStoreEvent.class);
                                            InOrder inOrder = Mockito.inOrder(publisher, store);

                                            inOrder.verify(publisher, times(1)).publishEvent(argThat(StoreEvent.class::isInstance));
                                            verify(store).getResource(any(Serializable.class));
                                            inOrder.verify(publisher, times(1)).publishEvent(captor.capture());
                                            assertThat(captor.getValue().getResult()).isEqualTo(result);
                }
            }
            @Nested
            class WhenGetResourceEntityIsInvoked {
                @BeforeEach
                void setUp() throws Throwable {
                    store = mock(ContentStore.class);
                                    publisher = mock(ApplicationEventPublisher.class);

                    result = mock(Resource.class);

                                            store = mock(ContentStore.class);
                                            when(store.getResource(argThat(ContentObject.class::isInstance))).thenReturn((Resource) result);

                                            invocation = new TestMethodInvocation(store, getResourceEntityMethod, new ContentObject("text/plain"));

                    interceptor = new StoreMethodInterceptor();
                                    StoreFragments fragments = new StoreFragments(Collections.singletonList(new StoreFragment(TestContentStore.class, new StoreImpl(store, publisher, Paths.get(System.getProperty("java.io.tmpdir"))))));
                                    interceptor.setStoreFragments(fragments);
                                    try {
                                        interceptor.invoke(invocation);
                                    } catch (Exception invokeException) {
                                        e = invokeException;
                                    }
                }
                @Test
                void shouldProceed() throws Throwable {
                    assertThat(e).isNull();


                                            InOrder inOrder = Mockito.inOrder(publisher, store);

                                            inOrder.verify(publisher).publishEvent(argThat(BeforeGetResourceEvent.class::isInstance));
                                            inOrder.verify(store).getResource(argThat(ContentObject.class::isInstance));

                                            ArgumentCaptor<AfterStoreEvent> captor = ArgumentCaptor.forClass(AfterStoreEvent.class);
                                            inOrder.verify(publisher, times(1)).publishEvent(captor.capture());
                                            assertThat(captor.getValue().getResult()).isEqualTo(result);
                }
            }
        }
        @Nested
        class AssociateCases {
            @Nested
            class WhenAssociateIsInvoked {
                @BeforeEach
                void setUp() throws Throwable {
                    store = mock(ContentStore.class);
                                    publisher = mock(ApplicationEventPublisher.class);

                    result = mock(Resource.class);

                                        store = mock(ContentStore.class);

                                        invocation = new TestMethodInvocation(store, associateMethod, "", 123);

                    interceptor = new StoreMethodInterceptor();
                                    StoreFragments fragments = new StoreFragments(Collections.singletonList(new StoreFragment(TestContentStore.class, new StoreImpl(store, publisher, Paths.get(System.getProperty("java.io.tmpdir"))))));
                                    interceptor.setStoreFragments(fragments);
                                    try {
                                        interceptor.invoke(invocation);
                                    } catch (Exception invokeException) {
                                        e = invokeException;
                                    }
                }
                @Test
                void shouldProceed() throws Throwable {
                    ArgumentCaptor<AfterAssociateEvent> captor = ArgumentCaptor.forClass(AfterAssociateEvent.class);
                                        InOrder inOrder = Mockito.inOrder(publisher, store);

                                        inOrder.verify(publisher).publishEvent(argThat(BeforeAssociateEvent.class::isInstance));
                                        inOrder.verify(store).associate(eq(""), eq(123));
                                        inOrder.verify(publisher).publishEvent(argThat(AfterAssociateEvent.class::isInstance));
                }
            }
        }
        @Nested
        class UnassociateCases {
            @Nested
            class WhenUnassociateIsInvoked {
                @BeforeEach
                void setUp() throws Throwable {
                    store = mock(ContentStore.class);
                                    publisher = mock(ApplicationEventPublisher.class);

                    result = mock(Resource.class);

                                        store = mock(ContentStore.class);

                                        invocation = new TestMethodInvocation(store, unassociateMethod, "foo");

                    interceptor = new StoreMethodInterceptor();
                                    StoreFragments fragments = new StoreFragments(Collections.singletonList(new StoreFragment(TestContentStore.class, new StoreImpl(store, publisher, Paths.get(System.getProperty("java.io.tmpdir"))))));
                                    interceptor.setStoreFragments(fragments);
                                    try {
                                        interceptor.invoke(invocation);
                                    } catch (Exception invokeException) {
                                        e = invokeException;
                                    }
                }
                @Test
                void shouldProceed() throws Throwable {
                    ArgumentCaptor<AfterUnassociateEvent> captor = ArgumentCaptor.forClass(AfterUnassociateEvent.class);
                                        InOrder inOrder = Mockito.inOrder(publisher, store);

                                        inOrder.verify(publisher).publishEvent(argThat(BeforeUnassociateEvent.class::isInstance));
                                        verify(store).unassociate("foo");
                                        inOrder.verify(publisher).publishEvent(argThat(AfterUnassociateEvent.class::isInstance));
                }
            }
        }
        @Nested
        class ToStringCases {
            @Nested
            class WhenToStringIsInvoked {
                @BeforeEach
                void setUp() throws Throwable {
                    store = mock(ContentStore.class);
                                    publisher = mock(ApplicationEventPublisher.class);

                    result = mock(Resource.class);

                                        store = mock(ContentStore.class);

                                        invocation = new TestMethodInvocation(store, toStringMethod);

                    interceptor = new StoreMethodInterceptor();
                                    StoreFragments fragments = new StoreFragments(Collections.singletonList(new StoreFragment(TestContentStore.class, new StoreImpl(store, publisher, Paths.get(System.getProperty("java.io.tmpdir"))))));
                                    interceptor.setStoreFragments(fragments);
                                    try {
                                        interceptor.invoke(invocation);
                                    } catch (Exception invokeException) {
                                        e = invokeException;
                                    }
                }
                @Test
                void shouldProceed() throws Throwable {
                    verify(publisher, never()).publishEvent(any());
                }
            }
        }
    }
    @Nested
    class FindMethodCases {
        @Test
        void shouldResolveTheMethodWhenNotOverridden() throws Throwable {
            store = mock(ContentStore.class);
                            publisher = mock(ApplicationEventPublisher.class);
                            interceptor = new StoreMethodInterceptor();
                            try {
                                Method m = ReflectionUtils.findMethod(TestContentStore.class, "unsetContent", Object.class);
                                assertThat(m).isNotNull();
                                Method actual = interceptor.getMethod(m, new StoreFragment(TestContentStore.class, new StoreImpl(store, publisher, Paths.get(System.getProperty("java.io.tmpdir")))));
                                assertThat(actual).isEqualTo(ReflectionUtils.findMethod(StoreImpl.class, "unsetContent", Object.class));
                            } catch (Exception invokeException) {
                                e = invokeException;
                            }
        }
        @Test
        void shouldResolveTheMethodWhenItIsOverriddenInTheInterface() throws Throwable {
            store = mock(ContentStore.class);
                            publisher = mock(ApplicationEventPublisher.class);
                            interceptor = new StoreMethodInterceptor();
                            try {
                                Method m = ReflectionUtils.findMethod(TestContentStore.class, "setContent", TEntity.class, InputStream.class);
                                assertThat(m).isNotNull();
                                Method actual = interceptor.getMethod(m, new StoreFragment(TestContentStore.class, new StoreImpl(store, publisher, Paths.get(System.getProperty("java.io.tmpdir")))));
                                assertThat(actual).isEqualTo(ReflectionUtils.findMethod(StoreImpl.class, "setContent", Object.class, InputStream.class));
                            } catch (Exception invokeException) {
                                e = invokeException;
                            }
        }
    }


    private void onBeforeSetContentPublishEvent(PublishEventAction action) {
        doAnswer(action::doAction).when(publisher).publishEvent(argThat(BeforeSetContentEvent.class::isInstance));
    }

    public interface PublishEventAction {
        Object doAction(InvocationOnMock invocationOnMock) throws Exception;
    }

    public interface AContentRepositoryExtension<S> {
        void getCustomContent(S property);
    }

    public interface TestContentStore extends ContentStore<TEntity, UUID> {
        @Override
        TEntity setContent(TEntity property, InputStream content);
    }

    public static class TestMethodInvocation extends SimpleMethodInvocation {

        TestMethodInvocation(Object targetObject, Method method, Object... arguments) {
            super(targetObject, method, arguments);
        }

        @Override
        public Object proceed() {
            return ReflectionUtils.invokeMethod(getMethod(), getThis(), getArguments());
        }
    }

    public static class ContentObject {
        @MimeType
        public String mimeType;

        public ContentObject(String mimeType) {
            this.mimeType = mimeType;
        }
    }

    public static class TEntity {

        private UUID contentId;

        public UUID getContentId() {
            return contentId;
        }

        public void setContentId(UUID contentId) {
            this.contentId = contentId;
        }
    }
}
