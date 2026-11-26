package internal.org.springframework.content.commons.store.factory;

import internal.org.springframework.content.commons.config.StoreFragment;
import internal.org.springframework.content.commons.config.StoreFragments;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.io.IOUtils;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.store.AssociativeStore;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.commons.store.Store;
import org.springframework.content.commons.store.events.*;
import org.springframework.content.test.support.CommonsTestStores;
import org.springframework.content.test.support.UuidSingleContentFields;
import org.springframework.content.test.support.UuidSingleContentStore;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.security.util.SimpleMethodInvocation;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class StoreMethodInterceptorTest {

    private static final @NonNull Method getResourceMethod;
    private static final @NonNull Method getResourceEntityMethod;
    private static final @NonNull Method associateMethod;
    private static final @NonNull Method unassociateMethod;
    private static final @NonNull Method getContentMethod;
    private static final @NonNull Method setContentMethod;
    private static final @NonNull Method setContentFromResourceMethod;
    private static final @NonNull Method unsetContentMethod;
    private static final @NonNull Method toStringMethod;
    private static final Path CONTENT_COPY_ROOT = Paths.get(System.getProperty("java.io.tmpdir"));

    static {
        getResourceMethod = requireMethod(Store.class, "getResource", Serializable.class);
        getResourceEntityMethod = requireMethod(AssociativeStore.class, "getResource", Object.class);
        associateMethod = requireMethod(AssociativeStore.class, "associate", Object.class, Serializable.class);
        unassociateMethod = requireMethod(AssociativeStore.class, "unassociate", Object.class);
        getContentMethod = requireMethod(ContentStore.class, "getContent", Object.class);
        setContentMethod = requireMethod(ContentStore.class, "setContent", Object.class, InputStream.class);
        setContentFromResourceMethod = requireMethod(ContentStore.class, "setContent", Object.class, Resource.class);
        unsetContentMethod = requireMethod(ContentStore.class, "unsetContent", Object.class);
        toStringMethod = requireMethod(Object.class, "toString");
    }

    private StoreMethodInterceptor interceptor;
    private ContentStore<Object, Serializable> store;
    private MethodInvocation invocation;
    private ApplicationEventPublisher publisher;
    private Object result;
    private Throwable e;
    private ByteArrayInputStream modifiedStream;

    private static Method requireMethod(Class<?> type, String name, Class<?>... parameterTypes) {
        Method method = ReflectionUtils.findMethod(type, name, parameterTypes);
        Assert.notNull(method, () -> "Method not found: " + type.getName() + "#" + name);
        return method;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static StoreFragment<?> contentStoreFragment(
        ContentStore<Object, Serializable> contentStore,
        ApplicationEventPublisher eventPublisher) {
        return new StoreFragment(
            UuidSingleContentStore.class, new StoreImpl(contentStore, eventPublisher, CONTENT_COPY_ROOT));
    }

    @SuppressWarnings("OverlyBroadCatchBlock")
    private void invokeInterceptor() {
        e = null;
        try {
            interceptor.invoke(invocation);
        } catch (Throwable invokeException) {
            e = invokeException;
        }
    }

    private void attachInterceptor(ContentStore<Object, Serializable> contentStore, ApplicationEventPublisher eventPublisher) {
        interceptor = new StoreMethodInterceptor();
        interceptor.setStoreFragments(new StoreFragments(Collections.singletonList(
            contentStoreFragment(contentStore, eventPublisher))));
    }

    private void onBeforeSetContentPublishEvent(PublishEventAction action) {
        doAnswer(action::doAction).when(publisher).publishEvent(isA(BeforeSetContentEvent.class));
    }

    public interface PublishEventAction {
        Object doAction(InvocationOnMock invocationOnMock) throws Exception;
    }

    public static class TestMethodInvocation extends SimpleMethodInvocation {

        private static final Object VOID_METHOD_RESULT = new Object();

        TestMethodInvocation(Object targetObject, @NonNull Method method, Object... arguments) {
            super(targetObject, method, arguments);
        }

        @Override
        public @NonNull Object proceed() {
            Method method = getMethod();
            Object result = ReflectionUtils.invokeMethod(method, getThis(), getArguments());
            if (result != null) {
                return result;
            }
            if (method.getReturnType() == void.class) {
                return VOID_METHOD_RESULT;
            }
            throw new IllegalStateException(
                "Method " + method + " returned null but return type is " + method.getReturnType().getName());
        }
    }

    public static class ContentObject {
        @MimeType
        public String mimeType;

        public ContentObject(String mimeType) {
            this.mimeType = mimeType;
        }
    }

    @Nested
    @DisplayName("getContent")
    class GetContent {
        @Nested
        @DisplayName("When GET content is invoked successfully")
        class GetContentIsInvokedSuccessfully {
            @BeforeEach
            void setUp() throws IOException {
                publisher = mock(ApplicationEventPublisher.class);

                result = new ByteArrayInputStream(new byte[]{});

                store = CommonsTestStores.mockContentStore();
                doAnswer(invocation -> result).when(store).getContent(any());

                invocation = new TestMethodInvocation(store, getContentMethod, new Object());

                attachInterceptor(store, publisher);
                invokeInterceptor();
            }

            @Test
            void shouldProceed() throws IOException {
                assertThat(e).isNull();

                ArgumentCaptor<AfterStoreEvent> captor = ArgumentCaptor.forClass(AfterStoreEvent.class);
                InOrder inOrder = Mockito.inOrder(publisher, store);

                inOrder.verify(publisher).publishEvent(isA(StoreEvent.class));
                inOrder.verify(store).getContent(any());
                inOrder.verify(publisher).publishEvent(captor.capture());
                assertThat(captor.getValue().getResult()).isEqualTo(result);
            }
        }

        @Nested
        @DisplayName("When GET content is invoked with illegal arguments")
        class GetContentIsInvokedWithIllegalArguments {
            @BeforeEach
            void setUp() throws IOException {
                publisher = mock(ApplicationEventPublisher.class);

                result = new ByteArrayInputStream(new byte[]{});

                store = CommonsTestStores.mockContentStore();
                doAnswer(invocation -> result).when(store).getContent(any());

                invocation = new TestMethodInvocation(store, getContentMethod);

                attachInterceptor(store, publisher);
                invokeInterceptor();
            }

            @Test
            void shouldProceed() {
                assertThat(e).isNotNull();
            }
        }
    }

    @Nested
    @DisplayName("setContent")
    class SetContent {
        @Nested
        @DisplayName("When set content is invoked successfully")
        class SetContentIsInvokedSuccessfully {
            @BeforeEach
            void setUp() {
                publisher = mock(ApplicationEventPublisher.class);

                result = new Object();

                store = CommonsTestStores.mockContentStore();
                when(store.setContent(any(), any(InputStream.class))).thenReturn(result);

                invocation = new TestMethodInvocation(store, setContentMethod, new Object(), new ByteArrayInputStream("test".getBytes()));

                attachInterceptor(store, publisher);
                invokeInterceptor();
            }

            @Test
            void shouldProceed() throws IOException {
                assertThat(e).isNull();

                ArgumentCaptor<BeforeSetContentEvent> beforeArgCaptor = ArgumentCaptor.forClass(BeforeSetContentEvent.class);
                ArgumentCaptor<InputStream> setContentArgCaptor = ArgumentCaptor.forClass(InputStream.class);
                ArgumentCaptor<AfterStoreEvent> afterArgCaptor = ArgumentCaptor.forClass(AfterStoreEvent.class);
                InOrder inOrder = Mockito.inOrder(publisher, store);

                inOrder.verify(publisher).publishEvent(beforeArgCaptor.capture());
                assertThat(beforeArgCaptor.getValue().getResource()).isNull();
                assertThat(beforeArgCaptor.getValue().getInputStream()).isNotNull();

                inOrder.verify(store).setContent(any(), setContentArgCaptor.capture());
                try (InputStream setContentInputStream = setContentArgCaptor.getValue()) {
                    assertThat(IOUtils.toString(setContentInputStream, StandardCharsets.UTF_8)).isEqualTo("test");
                }

                inOrder.verify(publisher).publishEvent(afterArgCaptor.capture());
                assertThat(afterArgCaptor.getValue().getResult()).isEqualTo(result);
            }
        }

        @Nested
        @DisplayName("When the before set content event consumes the entire input stream")
        class BeforeSetContentEventConsumesEntireInputStream {
            @BeforeEach
            void setUp() {
                publisher = mock(ApplicationEventPublisher.class);

                result = new Object();

                store = CommonsTestStores.mockContentStore();
                when(store.setContent(any(), any(InputStream.class))).thenReturn(result);

                invocation = new TestMethodInvocation(store, setContentMethod, new Object(), new ByteArrayInputStream("test".getBytes()));

                onBeforeSetContentPublishEvent((invocationOnMock) -> {
                    try (InputStream is = ((BeforeSetContentEvent) invocationOnMock.getArgument(0)).getInputStream()) {
                        assertThat(IOUtils.toString(is, StandardCharsets.UTF_8)).isEqualTo("test");
                    }
                    return null;
                });

                attachInterceptor(store, publisher);
                invokeInterceptor();
            }

            @Test
            void shouldStillReceiveInputStreamInSetContentInvocation() throws IOException {
                assertThat(e).isNull();

                ArgumentCaptor<InputStream> setContentArgCaptor = ArgumentCaptor.forClass(InputStream.class);
                ArgumentCaptor<AfterStoreEvent> afterArgCaptor = ArgumentCaptor.forClass(AfterStoreEvent.class);
                InOrder inOrder = Mockito.inOrder(publisher, store);

                inOrder.verify(publisher).publishEvent(isA(BeforeSetContentEvent.class));

                inOrder.verify(store).setContent(any(), setContentArgCaptor.capture());
                try (InputStream setContentInputStream = setContentArgCaptor.getValue()) {
                    assertThat(IOUtils.toString(setContentInputStream, StandardCharsets.UTF_8)).isEqualTo("test");
                }

                inOrder.verify(publisher).publishEvent(afterArgCaptor.capture());
                assertThat(afterArgCaptor.getValue().getResult()).isEqualTo(result);
            }
        }

        @Nested
        @DisplayName("When the before set content event consumes partial input stream")
        class BeforeSetContentEventConsumesPartialInputStream {
            @BeforeEach
            void setUp() {
                publisher = mock(ApplicationEventPublisher.class);

                result = new Object();

                store = CommonsTestStores.mockContentStore();
                when(store.setContent(any(), any(InputStream.class))).thenReturn(result);

                invocation = new TestMethodInvocation(store, setContentMethod, new Object(), new ByteArrayInputStream("test".getBytes()));

                onBeforeSetContentPublishEvent((invocationOnMock) -> {
                    InputStream is = ((BeforeSetContentEvent) invocationOnMock.getArgument(0)).getInputStream();
                    assertThat((char) is.read()).isEqualTo('t');
                    assertThat((char) is.read()).isEqualTo('e');
                    return null;
                });

                attachInterceptor(store, publisher);
                invokeInterceptor();
            }

            @Test
            void shouldStillReceiveInputStreamInSetContentInvocation() throws IOException {
                assertThat(e).isNull();

                InOrder inOrder = Mockito.inOrder(publisher, store);

                ArgumentCaptor<InputStream> setContentArgCaptor = ArgumentCaptor.forClass(InputStream.class);
                ArgumentCaptor<AfterStoreEvent> afterArgCaptor = ArgumentCaptor.forClass(AfterStoreEvent.class);

                inOrder.verify(publisher).publishEvent(isA(BeforeSetContentEvent.class));

                inOrder.verify(store).setContent(any(), setContentArgCaptor.capture());

                try (InputStream setContentInputStream = setContentArgCaptor.getValue()) {
                    assertThat(IOUtils.toString(setContentInputStream, StandardCharsets.UTF_8)).isEqualTo("test");
                }

                inOrder.verify(publisher).publishEvent(afterArgCaptor.capture());
                assertThat(afterArgCaptor.getValue().getResult()).isEqualTo(result);
            }
        }

        @Nested
        @DisplayName("When the before set content event does not consume any of the input stream")
        class BeforeSetContentEventDoesNotConsumeAnyOfInputStream {
            @BeforeEach
            void setUp() {
                publisher = mock(ApplicationEventPublisher.class);

                result = new Object();

                store = CommonsTestStores.mockContentStore();
                when(store.setContent(any(), any(InputStream.class))).thenReturn(result);

                invocation = new TestMethodInvocation(store, setContentMethod, new Object(), new ByteArrayInputStream("test".getBytes()));

                onBeforeSetContentPublishEvent((invocationOnMock) -> null);

                attachInterceptor(store, publisher);
                invokeInterceptor();
            }

            @Test
            void shouldStillReceiveInputStreamInSetContentInvocation() throws IOException {
                assertThat(e).isNull();

                InOrder inOrder = Mockito.inOrder(publisher, store);

                ArgumentCaptor<InputStream> setContentArgCaptor = ArgumentCaptor.forClass(InputStream.class);
                ArgumentCaptor<AfterStoreEvent> afterArgCaptor = ArgumentCaptor.forClass(AfterStoreEvent.class);

                inOrder.verify(publisher).publishEvent(isA(BeforeSetContentEvent.class));

                inOrder.verify(store).setContent(any(), setContentArgCaptor.capture());

                try (InputStream setContentInputStream = setContentArgCaptor.getValue()) {
                    assertThat(IOUtils.toString(setContentInputStream, StandardCharsets.UTF_8)).isEqualTo("test");
                }

                inOrder.verify(publisher).publishEvent(afterArgCaptor.capture());
                assertThat(afterArgCaptor.getValue().getResult()).isEqualTo(result);
            }
        }

        @Nested
        @DisplayName("When the before set content event replaces the input stream")
        class BeforeSetContentEventReplacesInputStream {
            @BeforeEach
            void setUp() {
                publisher = mock(ApplicationEventPublisher.class);

                result = new Object();

                store = CommonsTestStores.mockContentStore();
                when(store.setContent(any(), any(InputStream.class))).thenReturn(result);

                invocation = new TestMethodInvocation(store, setContentMethod, new Object(), new ByteArrayInputStream("test".getBytes()));

                onBeforeSetContentPublishEvent((invocationOnMock) -> {
                    modifiedStream = new ByteArrayInputStream("encrypted".getBytes());
                    ((BeforeSetContentEvent) invocationOnMock.getArgument(0)).setInputStream(modifiedStream);
                    return null;
                });

                attachInterceptor(store, publisher);
                invokeInterceptor();
            }

            @Test
            void shouldStillReceiveReplacedInputStreamInSetContentInvocation() throws IOException {
                assertThat(e).isNull();

                InOrder inOrder = Mockito.inOrder(publisher, store);

                ArgumentCaptor<InputStream> setContentArgCaptor = ArgumentCaptor.forClass(InputStream.class);
                ArgumentCaptor<AfterStoreEvent> afterArgCaptor = ArgumentCaptor.forClass(AfterStoreEvent.class);

                inOrder.verify(publisher).publishEvent(isA(BeforeSetContentEvent.class));

                inOrder.verify(store).setContent(any(), setContentArgCaptor.capture());

                try (InputStream setContentInputStream = setContentArgCaptor.getValue()) {
                    assertThat(setContentInputStream).isEqualTo(modifiedStream);
                }

                inOrder.verify(publisher).publishEvent(afterArgCaptor.capture());
                assertThat(afterArgCaptor.getValue().getResult()).isEqualTo(result);
            }
        }

        @Nested
        @DisplayName("When set content is invoked with illegal arguments")
        class SetContentIsInvokedWithIllegalArguments {
            @BeforeEach
            void setUp() {
                publisher = mock(ApplicationEventPublisher.class);

                result = new Object();

                store = CommonsTestStores.mockContentStore();
                when(store.setContent(any(), any(InputStream.class))).thenReturn(result);

                invocation = new TestMethodInvocation(store, setContentMethod);

                attachInterceptor(store, publisher);
                invokeInterceptor();
            }

            @Test
            void shouldProceed() {
                assertThat(e).isNotNull();
            }
        }
    }

    @Nested
    @DisplayName("setContent from resource")
    class SetContentFromResource {
        @BeforeEach
        void setUp() {
            publisher = mock(ApplicationEventPublisher.class);
            result = new Object();

            store = CommonsTestStores.mockContentStore();
            when(store.setContent(any(), any(Resource.class))).thenReturn(result);

            invocation = new TestMethodInvocation(store, setContentFromResourceMethod, new Object(), new InputStreamResource(new ByteArrayInputStream("test".getBytes())));

            attachInterceptor(store, publisher);
            invokeInterceptor();
        }

        @Test
        void shouldProceed() throws IOException {
            assertThat(e).isNull();

            ArgumentCaptor<BeforeSetContentEvent> beforeArgCaptor = ArgumentCaptor.forClass(BeforeSetContentEvent.class);
            ArgumentCaptor<Resource> setContentArgCaptor = ArgumentCaptor.forClass(Resource.class);
            ArgumentCaptor<AfterStoreEvent> afterArgCaptor = ArgumentCaptor.forClass(AfterStoreEvent.class);
            InOrder inOrder = Mockito.inOrder(publisher, store);

            inOrder.verify(publisher).publishEvent(beforeArgCaptor.capture());
            assertThat(beforeArgCaptor.getValue().getResource()).isNotNull();
            assertThat(beforeArgCaptor.getValue().getInputStream()).isNull();

            inOrder.verify(store).setContent(any(), setContentArgCaptor.capture());
            try (InputStream setContentInputStream = setContentArgCaptor.getValue().getInputStream()) {
                assertThat(IOUtils.toString(setContentInputStream, StandardCharsets.UTF_8)).isEqualTo("test");
            }

            inOrder.verify(publisher).publishEvent(afterArgCaptor.capture());
            assertThat(afterArgCaptor.getValue().getResult()).isEqualTo(result);
        }
    }

    @Nested
    @DisplayName("unsetContent")
    class UnsetContent {
        @Nested
        @DisplayName("When unset content is invoked")
        class UnsetContentIsInvoked {
            @BeforeEach
            void setUp() {
                publisher = mock(ApplicationEventPublisher.class);
                result = new Object();

                store = CommonsTestStores.mockContentStore();
                when(store.unsetContent(any())).thenReturn(result);

                invocation = new TestMethodInvocation(store, unsetContentMethod, new Object());

                attachInterceptor(store, publisher);
                invokeInterceptor();
            }

            @Test
            void shouldProceed() {
                assertThat(e).isNull();

                InOrder inOrder = Mockito.inOrder(publisher, store);

                inOrder.verify(publisher).publishEvent(isA(BeforeUnsetContentEvent.class));
                inOrder.verify(store).unsetContent(any());

                ArgumentCaptor<AfterStoreEvent> captor = ArgumentCaptor.forClass(AfterStoreEvent.class);
                inOrder.verify(publisher).publishEvent(captor.capture());
                assertThat(captor.getValue().getResult()).isEqualTo(result);
            }
        }

        @Nested
        @DisplayName("When unset content is invoked with illegal arguments")
        class UnsetContentIsInvokedWithIllegalArguments {
            @BeforeEach
            void setUp() {
                publisher = mock(ApplicationEventPublisher.class);
                result = new Object();

                store = CommonsTestStores.mockContentStore();
                when(store.unsetContent(any())).thenReturn(result);

                invocation = new TestMethodInvocation(store, unsetContentMethod);

                attachInterceptor(store, publisher);
                invokeInterceptor();
            }

            @Test
            void shouldNotPublishEvents() {
                assertThat(e).isNotNull();
            }
        }
    }

    @Nested
    @DisplayName("getResource")
    class GetResource {
        @Nested
        @DisplayName("When GET resource is invoked")
        class GetResourceIsInvoked {
            @BeforeEach
            void setUp() {
                publisher = mock(ApplicationEventPublisher.class);

                result = mock(Resource.class);

                store = CommonsTestStores.mockContentStore();
                when(store.getResource(any(Serializable.class))).thenReturn((Resource) result);

                invocation = new TestMethodInvocation(store, getResourceMethod, "content-id");

                attachInterceptor(store, publisher);
                invokeInterceptor();
            }

            @Test
            void shouldProceed() {
                assertThat(e).isNull();

                ArgumentCaptor<AfterStoreEvent> captor = ArgumentCaptor.forClass(AfterStoreEvent.class);
                InOrder inOrder = Mockito.inOrder(publisher, store);

                inOrder.verify(publisher).publishEvent(isA(StoreEvent.class));
                verify(store).getResource(any(Serializable.class));
                inOrder.verify(publisher).publishEvent(captor.capture());
                assertThat(captor.getValue().getResult()).isEqualTo(result);
            }
        }

        @Nested
        @DisplayName("When GET resource entity is invoked")
        class GetResourceEntityIsInvoked {
            @BeforeEach
            void setUp() {
                publisher = mock(ApplicationEventPublisher.class);

                result = mock(Resource.class);

                store = CommonsTestStores.mockContentStore();
                when(store.getResource(isA(ContentObject.class))).thenReturn((Resource) result);

                invocation = new TestMethodInvocation(store, getResourceEntityMethod, new ContentObject("text/plain"));

                attachInterceptor(store, publisher);
                invokeInterceptor();
            }

            @Test
            void shouldProceed() {
                assertThat(e).isNull();

                InOrder inOrder = Mockito.inOrder(publisher, store);

                inOrder.verify(publisher).publishEvent(isA(BeforeGetResourceEvent.class));
                inOrder.verify(store).getResource(isA(ContentObject.class));

                ArgumentCaptor<AfterStoreEvent> captor = ArgumentCaptor.forClass(AfterStoreEvent.class);
                inOrder.verify(publisher).publishEvent(captor.capture());
                assertThat(captor.getValue().getResult()).isEqualTo(result);
            }
        }
    }

    @Nested
    @DisplayName("associate")
    class Associate {
        @Nested
        @DisplayName("When associate is invoked")
        class AssociateIsInvoked {
            @BeforeEach
            void setUp() {
                publisher = mock(ApplicationEventPublisher.class);
                store = CommonsTestStores.mockContentStore();

                invocation = new TestMethodInvocation(store, associateMethod, "", 123);

                attachInterceptor(store, publisher);
                invokeInterceptor();
            }

            @Test
            void shouldProceed() {
                InOrder inOrder = Mockito.inOrder(publisher, store);

                inOrder.verify(publisher).publishEvent(isA(BeforeAssociateEvent.class));
                inOrder.verify(store).associate(eq(""), eq(123));
                inOrder.verify(publisher).publishEvent(isA(AfterAssociateEvent.class));
            }
        }
    }

    @Nested
    @DisplayName("unassociate")
    class Unassociate {
        @Nested
        @DisplayName("When unassociate is invoked")
        class UnassociateIsInvoked {
            @BeforeEach
            void setUp() {
                publisher = mock(ApplicationEventPublisher.class);
                store = CommonsTestStores.mockContentStore();

                invocation = new TestMethodInvocation(store, unassociateMethod, "foo");

                attachInterceptor(store, publisher);
                invokeInterceptor();
            }

            @Test
            void shouldProceed() {
                InOrder inOrder = Mockito.inOrder(publisher, store);

                inOrder.verify(publisher).publishEvent(isA(BeforeUnassociateEvent.class));
                verify(store).unassociate("foo");
                inOrder.verify(publisher).publishEvent(isA(AfterUnassociateEvent.class));
            }
        }
    }

    @Nested
    @DisplayName("toString")
    class ToString {
        @Nested
        @DisplayName("When to string is invoked")
        class ToStringIsInvoked {
            @BeforeEach
            void setUp() {
                publisher = mock(ApplicationEventPublisher.class);
                store = CommonsTestStores.mockContentStore();

                invocation = new TestMethodInvocation(store, toStringMethod);

                attachInterceptor(store, publisher);
                invokeInterceptor();
            }

            @Test
            void shouldProceed() {
                verify(publisher, never()).publishEvent(any());
            }
        }
    }

    @Nested
    @DisplayName("When the entity argument is null")
    class NullEntity {
        @Test
        void setContentThrowsIllegalArgumentException() {
            publisher = mock(ApplicationEventPublisher.class);
            store = CommonsTestStores.mockContentStore();
            invocation = new TestMethodInvocation(store, setContentMethod, null,
                new ByteArrayInputStream("test".getBytes()));
            attachInterceptor(store, publisher);
            invokeInterceptor();
            assertThat(e).isInstanceOf(IllegalArgumentException.class)
                .hasMessage(org.springframework.content.commons.store.StoreEntityValidation.ENTITY_MUST_NOT_BE_NULL);
        }
    }

    @Nested
    @DisplayName("getMethod")
    class GetMethod {
        @Test
        void shouldResolveMethodWhenNotOverridden() {
            store = CommonsTestStores.mockContentStore();
            publisher = mock(ApplicationEventPublisher.class);
            interceptor = new StoreMethodInterceptor();
            Method m = ReflectionUtils.findMethod(UuidSingleContentStore.class, "unsetContent", Object.class);
            assertThat(m).isNotNull();
            Method actual = interceptor.getMethod(m, contentStoreFragment(store, publisher));
            assertThat(actual).isEqualTo(ReflectionUtils.findMethod(StoreImpl.class, "unsetContent", Object.class));
        }

        @Test
        void shouldResolveMethodWhenItIsOverriddenInInterface() {
            store = CommonsTestStores.mockContentStore();
            publisher = mock(ApplicationEventPublisher.class);
            interceptor = new StoreMethodInterceptor();
            Method m = ReflectionUtils.findMethod(UuidSingleContentStore.class, "setContent",
                UuidSingleContentFields.class, InputStream.class);
            assertThat(m).isNotNull();
            Method actual = interceptor.getMethod(m, contentStoreFragment(store, publisher));
            assertThat(actual).isEqualTo(ReflectionUtils.findMethod(StoreImpl.class, "setContent", Object.class, InputStream.class));
        }
    }
}
