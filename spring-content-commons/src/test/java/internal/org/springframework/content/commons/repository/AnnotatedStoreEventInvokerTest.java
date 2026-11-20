package internal.org.springframework.content.commons.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.content.commons.annotations.HandleAfterAssociate;
import org.springframework.content.commons.annotations.HandleAfterGetContent;
import org.springframework.content.commons.annotations.HandleAfterGetResource;
import org.springframework.content.commons.annotations.HandleAfterSetContent;
import org.springframework.content.commons.annotations.HandleAfterUnassociate;
import org.springframework.content.commons.annotations.HandleAfterUnsetContent;
import org.springframework.content.commons.annotations.HandleBeforeAssociate;
import org.springframework.content.commons.annotations.HandleBeforeGetContent;
import org.springframework.content.commons.annotations.HandleBeforeGetResource;
import org.springframework.content.commons.annotations.HandleBeforeSetContent;
import org.springframework.content.commons.annotations.HandleBeforeUnassociate;
import org.springframework.content.commons.annotations.HandleBeforeUnsetContent;
import org.springframework.content.commons.annotations.StoreEventHandler;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.commons.store.events.AfterAssociateEvent;
import org.springframework.content.commons.store.events.AfterGetContentEvent;
import org.springframework.content.commons.store.events.AfterGetResourceEvent;
import org.springframework.content.commons.store.events.AfterSetContentEvent;
import org.springframework.content.commons.store.events.AfterUnassociateEvent;
import org.springframework.content.commons.store.events.AfterUnsetContentEvent;
import org.springframework.content.commons.store.events.BeforeAssociateEvent;
import org.springframework.content.commons.store.events.BeforeGetContentEvent;
import org.springframework.content.commons.store.events.BeforeGetResourceEvent;
import org.springframework.content.commons.store.events.BeforeSetContentEvent;
import org.springframework.content.commons.store.events.BeforeUnassociateEvent;
import org.springframework.content.commons.store.events.BeforeUnsetContentEvent;
import org.springframework.content.commons.store.events.StoreEvent;
import org.springframework.content.commons.utils.ReflectionService;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.util.ReflectionUtils;

import java.io.InputStream;
import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class AnnotatedStoreEventInvokerTest {

    private static final String CUSTOM_HANDLER_BEAN = "custom-bean";

    private AnnotatedStoreEventInvoker invoker;
    private ReflectionService reflectionService;

    @SuppressWarnings("unused") // handler methods registered via reflection
    @StoreEventHandler
    public static class CustomEventHandler {

        @HandleBeforeGetResource
        public void beforeGetResource(Object contentObject) {
        }

        @HandleBeforeGetResource
        public void beforeGetResource(BeforeGetResourceEvent event) {
        }

        @HandleAfterGetResource
        public void afterGetResource(Object contentObject) {
        }

        @HandleAfterGetResource
        public void afterGetResource(AfterGetResourceEvent event) {
        }

        @HandleBeforeAssociate
        public void beforeAssociate(Object contentObject) {
        }

        @HandleBeforeAssociate
        public void beforeAssociate(BeforeAssociateEvent event) {
        }

        @HandleAfterAssociate
        public void afterAssociate(Object contentObject) {
        }

        @HandleAfterAssociate
        public void afterAssociate(AfterAssociateEvent event) {
        }

        @HandleBeforeUnassociate
        public void beforeUnassociate(Object contentObject) {
        }

        @HandleBeforeUnassociate
        public void beforeUnassociate(BeforeUnassociateEvent event) {
        }

        @HandleAfterUnassociate
        public void afterUnassociate(Object contentObject) {
        }

        @HandleAfterUnassociate
        public void afterUnassociate(AfterUnassociateEvent event) {
        }

        @HandleBeforeGetContent
        public void beforeGetContent(Object contentObject) {
        }

        @HandleBeforeGetContent
        public void beforeGetContent(BeforeGetContentEvent event) {
        }

        @HandleAfterGetContent
        public void afterGetContent(Object contentObject) {
        }

        @HandleAfterGetContent
        public void afterGetContent(AfterGetContentEvent event) {
        }

        @HandleBeforeSetContent
        public void beforeSetContent(Object contentObject) {
        }

        @HandleBeforeSetContent
        public void beforeSetContent(BeforeSetContentEvent event) {
        }

        @HandleAfterSetContent
        public void afterSetContent(Object contentObject) {
        }

        @HandleAfterSetContent
        public void afterSetContent(AfterSetContentEvent event) {
        }

        @HandleBeforeUnsetContent
        public void beforeUnsetContent(Object contentObject) {
        }

        @HandleBeforeUnsetContent
        public void beforeUnsetContent(BeforeUnsetContentEvent event) {
        }

        @HandleAfterUnsetContent
        public void afterUnsetContent(Object contentObject) {
        }

        @HandleAfterUnsetContent
        public void afterUnsetContent(AfterUnsetContentEvent event) {
        }
    }

    @SuppressWarnings("unused") // handler methods registered via reflection
    @StoreEventHandler
    public static class HighestPriorityCustomEventHandler {

        @HandleBeforeGetResource
        @Order(Ordered.HIGHEST_PRECEDENCE)
        public void beforeGetResource(Object contentObject) {
        }
    }

    public static class EventSource {
    }

    public static class UnknownContentEvent extends StoreEvent {

        @Serial
        private static final long serialVersionUID = 4393640168031790561L;

        public UnknownContentEvent(Object source, ContentStore<Object, Serializable> store) {
            super(source, store);
        }
    }

    private record HandlerInvocationCase(
            String handlerMethodName,
            Class<?> handlerParameterType,
            BiFunction<EventSource, ContentStore<Object, Serializable>, StoreEvent> eventFactory,
            Function<StoreEvent, Object> expectedArgument) {
    }

    static Stream<HandlerInvocationCase> handlerInvocationCases() {
        return Stream.of(
                caseForSource(BeforeGetResourceEvent::new, "beforeGetResource"),
                caseForEvent(BeforeGetResourceEvent::new, "beforeGetResource", BeforeGetResourceEvent.class),
                caseForSource(AfterGetResourceEvent::new, "afterGetResource"),
                caseForEvent(AfterGetResourceEvent::new, "afterGetResource", AfterGetResourceEvent.class),
                caseForSource(BeforeAssociateEvent::new, "beforeAssociate"),
                caseForEvent(BeforeAssociateEvent::new, "beforeAssociate", BeforeAssociateEvent.class),
                caseForSource(AfterAssociateEvent::new, "afterAssociate"),
                caseForEvent(AfterAssociateEvent::new, "afterAssociate", AfterAssociateEvent.class),
                caseForSource(BeforeUnassociateEvent::new, "beforeUnassociate"),
                caseForEvent(BeforeUnassociateEvent::new, "beforeUnassociate", BeforeUnassociateEvent.class),
                caseForSource(AfterUnassociateEvent::new, "afterUnassociate"),
                caseForEvent(AfterUnassociateEvent::new, "afterUnassociate", AfterUnassociateEvent.class),
                caseForSource(BeforeGetContentEvent::new, "beforeGetContent"),
                caseForEvent(BeforeGetContentEvent::new, "beforeGetContent", BeforeGetContentEvent.class),
                caseForSource(AfterGetContentEvent::new, "afterGetContent"),
                caseForEvent(AfterGetContentEvent::new, "afterGetContent", AfterGetContentEvent.class),
                caseForSource((source, store) -> new BeforeSetContentEvent(source, store, (InputStream) null), "beforeSetContent"),
                caseForEvent((source, store) -> new BeforeSetContentEvent(source, store, (InputStream) null), "beforeSetContent", BeforeSetContentEvent.class),
                caseForSource(AfterSetContentEvent::new, "afterSetContent"),
                caseForEvent(AfterSetContentEvent::new, "afterSetContent", AfterSetContentEvent.class),
                caseForSource(BeforeUnsetContentEvent::new, "beforeUnsetContent"),
                caseForEvent(BeforeUnsetContentEvent::new, "beforeUnsetContent", BeforeUnsetContentEvent.class),
                caseForSource(AfterUnsetContentEvent::new, "afterUnsetContent"),
                caseForEvent(AfterUnsetContentEvent::new, "afterUnsetContent", AfterUnsetContentEvent.class)
        );
    }

    private static HandlerInvocationCase caseForSource(
            BiFunction<EventSource, ContentStore<Object, Serializable>, StoreEvent> eventFactory,
            String handlerMethodName) {
        return new HandlerInvocationCase(handlerMethodName, Object.class, eventFactory, StoreEvent::getSource);
    }

    private static HandlerInvocationCase caseForEvent(
            BiFunction<EventSource, ContentStore<Object, Serializable>, StoreEvent> eventFactory,
            String handlerMethodName,
            Class<?> handlerParameterType) {
        return new HandlerInvocationCase(handlerMethodName, handlerParameterType, eventFactory, event -> event);
    }

    @Nested
    class PostProcessAfterInitialization {
        @Nested
        class WhenInitializedWithAStoreEventHandlerBean {
            @BeforeEach
            void setUp() {
                reflectionService = mock(ReflectionService.class);
                invoker = new AnnotatedStoreEventInvoker(reflectionService);
                invoker.postProcessAfterInitialization(new CustomEventHandler(), CUSTOM_HANDLER_BEAN);
            }

            @Test
            void registerTheHandlers() {
                assertThat(invoker.getHandlers().get(BeforeGetResourceEvent.class).size()).isEqualTo(2);
                assertThat(invoker.getHandlers().get(AfterGetResourceEvent.class).size()).isEqualTo(2);
                assertThat(invoker.getHandlers().get(BeforeAssociateEvent.class).size()).isEqualTo(2);
                assertThat(invoker.getHandlers().get(AfterAssociateEvent.class).size()).isEqualTo(2);
                assertThat(invoker.getHandlers().get(BeforeUnassociateEvent.class).size()).isEqualTo(2);
                assertThat(invoker.getHandlers().get(AfterUnassociateEvent.class).size()).isEqualTo(2);
                assertThat(invoker.getHandlers().get(BeforeGetContentEvent.class).size()).isEqualTo(2);
                assertThat(invoker.getHandlers().get(AfterGetContentEvent.class).size()).isEqualTo(2);
                assertThat(invoker.getHandlers().get(BeforeSetContentEvent.class).size()).isEqualTo(2);
                assertThat(invoker.getHandlers().get(AfterSetContentEvent.class).size()).isEqualTo(2);
                assertThat(invoker.getHandlers().get(BeforeUnsetContentEvent.class).size()).isEqualTo(2);
                assertThat(invoker.getHandlers().get(AfterUnsetContentEvent.class).size()).isEqualTo(2);
            }

            @Nested
            class WhenInitializedWithAnotherEventHandlerOfHighestPriority {
                private final HighestPriorityCustomEventHandler priorityHandler = new HighestPriorityCustomEventHandler();

                @BeforeEach
                void setUp() {
                    reflectionService = mock(ReflectionService.class);
                    invoker = new AnnotatedStoreEventInvoker(reflectionService);
                    invoker.postProcessAfterInitialization(new CustomEventHandler(), CUSTOM_HANDLER_BEAN);
                    invoker.postProcessAfterInitialization(priorityHandler, "high-priority-custom-bean");
                }

                @Test
                void shouldOrderTheHandlersByPriority() {
                    assertThat(invoker.getHandlers().get(BeforeGetResourceEvent.class).size()).isEqualTo(3);
                    assertThat(invoker.getHandlers().get(BeforeGetResourceEvent.class).get(0).handler).isEqualTo(priorityHandler);
                }
            }
        }
    }

    @Nested
    class OnApplicationEvent {

        @ParameterizedTest
        @MethodSource("internal.org.springframework.content.commons.repository.AnnotatedStoreEventInvokerTest#handlerInvocationCases")
        void shouldCallMatchingHandlerMethod(HandlerInvocationCase handlerCase) {
            reflectionService = mock(ReflectionService.class);
            invoker = new AnnotatedStoreEventInvoker(reflectionService);

            @SuppressWarnings("unchecked")
            ContentStore<Object, Serializable> store = mock(ContentStore.class);
            EventSource source = new EventSource();
            StoreEvent event = handlerCase.eventFactory().apply(source, store);

            invoker.postProcessAfterInitialization(new CustomEventHandler(), CUSTOM_HANDLER_BEAN);
            invoker.onApplicationEvent(event);

            Method handler = ReflectionUtils.findMethod(CustomEventHandler.class,
                    handlerCase.handlerMethodName(), handlerCase.handlerParameterType());
            assertThat(handler).isNotNull();

            Object expectedArgument = handlerCase.expectedArgument().apply(event);
            verify(reflectionService).invokeMethod(eq(handler), isA(CustomEventHandler.class), eq(expectedArgument));
        }

        @Nested
        class GivenAnEventHandlerAndAnUnknownEvent {
            @Test
            void shouldNotCallAnEventHandler() {
                reflectionService = mock(ReflectionService.class);
                invoker = new AnnotatedStoreEventInvoker(reflectionService);

                @SuppressWarnings("unchecked")
                ContentStore<Object, Serializable> store = mock(ContentStore.class);
                EventSource source = new EventSource();
                StoreEvent event = new UnknownContentEvent(source, store);

                invoker.postProcessAfterInitialization(new CustomEventHandler(), CUSTOM_HANDLER_BEAN);
                invoker.onApplicationEvent(event);

                verify(reflectionService, never()).invokeMethod(any(), any(), any());
            }
        }
    }
}
