package internal.org.springframework.content.commons.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import org.springframework.content.commons.annotations.*;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.commons.store.events.StoreEvent;
import org.springframework.content.commons.store.events.*;
import org.springframework.content.commons.utils.ReflectionService;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.util.ReflectionUtils;

import java.io.InputStream;
import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.Method;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
public class AnnotatedStoreEventInvokerTest {

    private AnnotatedStoreEventInvoker invoker;

    private StoreEvent event;

    // mocks
    private ReflectionService reflectionService;
    private ContentStore<Object, Serializable> store;

    // event handlers
    private final HighestPriorityCustomEventHandler priorityHandler = new HighestPriorityCustomEventHandler();

    
    @Nested
    class PostProcessAfterInitialization {
        @Nested
        class WhenInitializedWithAStoreEventHandlerBean {
            @BeforeEach
            void setUp() {
                store = mock(ContentStore.class);
                reflectionService = mock(ReflectionService.class);
                invoker = new AnnotatedStoreEventInvoker(reflectionService);
                invoker.postProcessAfterInitialization(new CustomEventHandler(),
                        "custom-bean");

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
                @BeforeEach
                void setUp() {
                    store = mock(ContentStore.class);
                    reflectionService = mock(ReflectionService.class);
                    invoker = new AnnotatedStoreEventInvoker(reflectionService);
                    invoker.postProcessAfterInitialization(new CustomEventHandler(),
                            "custom-bean");

                    invoker.postProcessAfterInitialization(
                                                    priorityHandler, "high-priority-custom-bean"
                                            );
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
        @Nested
        class GivenAnEventHandlerAndABeforeGetResourceEvent {
            @BeforeEach
            void setUp() {
                reflectionService = mock(ReflectionService.class);
                invoker = new AnnotatedStoreEventInvoker(reflectionService);

                EventSource source = new EventSource();
                event = new BeforeGetResourceEvent(source, store);

                invoker.postProcessAfterInitialization(new CustomEventHandler(),
                        "custom-bean");
                invoker.onApplicationEvent(event);

            }

            @Test
            void shouldCallThatCorrectHandlerMethod() {
                Method handler = ReflectionUtils.findMethod(CustomEventHandler.class,
                        "beforeGetResource", Object.class);
                assertThat(handler).isNotNull();

                verify(reflectionService).invokeMethod(org.mockito.ArgumentMatchers.eq(handler),
                        org.mockito.ArgumentMatchers.isA(CustomEventHandler.class),
                        org.mockito.ArgumentMatchers.eq(event.getSource()));

            }

        }

        @Nested
        class GivenAnEventHandlerAcceptingTheEventAndABeforeGetResourceEvent {
            @BeforeEach
            void setUp() {
                reflectionService = mock(ReflectionService.class);
                invoker = new AnnotatedStoreEventInvoker(reflectionService);

                EventSource source = new EventSource();
                event = new BeforeGetResourceEvent(source, store);

                invoker.postProcessAfterInitialization(new CustomEventHandler(),
                        "custom-bean");
                invoker.onApplicationEvent(event);

            }

            @Test
            void shouldCallThatCorrectHandlerMethod() {
                Method handler = ReflectionUtils.findMethod(CustomEventHandler.class,
                        "beforeGetResource", BeforeGetResourceEvent.class);
                assertThat(handler).isNotNull();

                verify(reflectionService).invokeMethod(org.mockito.ArgumentMatchers.eq(handler),
                        org.mockito.ArgumentMatchers.isA(CustomEventHandler.class),
                        org.mockito.ArgumentMatchers.eq(event));

            }

        }

        @Nested
        class GivenAnEventHandlerAndAAfterGetResourceEvent {
            @BeforeEach
            void setUp() {
                reflectionService = mock(ReflectionService.class);
                invoker = new AnnotatedStoreEventInvoker(reflectionService);

                EventSource source = new EventSource();
                event = new AfterGetResourceEvent(source, store);

                invoker.postProcessAfterInitialization(new CustomEventHandler(),
                        "custom-bean");
                invoker.onApplicationEvent(event);

            }

            @Test
            void shouldCallThatCorrectHandlerMethod() {
                Method handler = ReflectionUtils.findMethod(CustomEventHandler.class,
                        "afterGetResource", Object.class);
                assertThat(handler).isNotNull();

                verify(reflectionService).invokeMethod(org.mockito.ArgumentMatchers.eq(handler),
                        org.mockito.ArgumentMatchers.isA(CustomEventHandler.class),
                        org.mockito.ArgumentMatchers.eq(event.getSource()));

            }

        }

        @Nested
        class GivenAnEventHandlerAcceptingTheEventAndAAfterGetResourceEvent {
            @BeforeEach
            void setUp() {
                reflectionService = mock(ReflectionService.class);
                invoker = new AnnotatedStoreEventInvoker(reflectionService);

                EventSource source = new EventSource();
                event = new AfterGetResourceEvent(source, store);

                invoker.postProcessAfterInitialization(new CustomEventHandler(),
                        "custom-bean");
                invoker.onApplicationEvent(event);

            }

            @Test
            void shouldCallThatCorrectHandlerMethod() {
                Method handler = ReflectionUtils.findMethod(CustomEventHandler.class,
                        "afterGetResource", AfterGetResourceEvent.class);
                assertThat(handler).isNotNull();

                verify(reflectionService).invokeMethod(org.mockito.ArgumentMatchers.eq(handler),
                        org.mockito.ArgumentMatchers.isA(CustomEventHandler.class),
                        org.mockito.ArgumentMatchers.eq(event));

            }

        }

        @Nested
        class GivenAnEventHandlerAndABeforeAssociateEvent {
            @BeforeEach
            void setUp() {
                reflectionService = mock(ReflectionService.class);
                invoker = new AnnotatedStoreEventInvoker(reflectionService);

                EventSource source = new EventSource();
                event = new BeforeAssociateEvent(source, store);

                invoker.postProcessAfterInitialization(new CustomEventHandler(),
                        "custom-bean");
                invoker.onApplicationEvent(event);

            }

            @Test
            void shouldCallThatCorrectHandlerMethod() {
                Method handler = ReflectionUtils.findMethod(CustomEventHandler.class,
                        "beforeAssociate", Object.class);
                assertThat(handler).isNotNull();

                verify(reflectionService).invokeMethod(org.mockito.ArgumentMatchers.eq(handler),
                        org.mockito.ArgumentMatchers.isA(CustomEventHandler.class),
                        org.mockito.ArgumentMatchers.eq(event.getSource()));

            }

        }

        @Nested
        class GivenAnEventHandlerAcceptingTheEventAndABeforeAssociateEvent {
            @BeforeEach
            void setUp() {
                reflectionService = mock(ReflectionService.class);
                invoker = new AnnotatedStoreEventInvoker(reflectionService);

                EventSource source = new EventSource();
                event = new BeforeAssociateEvent(source, store);

                invoker.postProcessAfterInitialization(new CustomEventHandler(),
                        "custom-bean");
                invoker.onApplicationEvent(event);

            }

            @Test
            void shouldCallThatCorrectHandlerMethod() {
                Method handler = ReflectionUtils.findMethod(CustomEventHandler.class,
                        "beforeAssociate", BeforeAssociateEvent.class);
                assertThat(handler).isNotNull();

                verify(reflectionService).invokeMethod(org.mockito.ArgumentMatchers.eq(handler),
                        org.mockito.ArgumentMatchers.isA(CustomEventHandler.class),
                        org.mockito.ArgumentMatchers.eq(event));

            }

        }

        @Nested
        class GivenAnEventHandlerAndAAfterAssociateEvent {
            @BeforeEach
            void setUp() {
                reflectionService = mock(ReflectionService.class);
                invoker = new AnnotatedStoreEventInvoker(reflectionService);

                EventSource source = new EventSource();
                event = new AfterAssociateEvent(source, store);

                invoker.postProcessAfterInitialization(new CustomEventHandler(),
                        "custom-bean");
                invoker.onApplicationEvent(event);

            }

            @Test
            void shouldCallThatCorrectHandlerMethod() {
                Method handler = ReflectionUtils.findMethod(CustomEventHandler.class,
                        "afterAssociate", Object.class);
                assertThat(handler).isNotNull();

                verify(reflectionService).invokeMethod(org.mockito.ArgumentMatchers.eq(handler),
                        org.mockito.ArgumentMatchers.isA(CustomEventHandler.class),
                        org.mockito.ArgumentMatchers.eq(event.getSource()));

            }

        }

        @Nested
        class GivenAnEventHandlerAcceptingTheEventAndAAfterAssociateEvent {
            @BeforeEach
            void setUp() {
                reflectionService = mock(ReflectionService.class);
                invoker = new AnnotatedStoreEventInvoker(reflectionService);

                EventSource source = new EventSource();
                event = new AfterAssociateEvent(source, store);

                invoker.postProcessAfterInitialization(new CustomEventHandler(),
                        "custom-bean");
                invoker.onApplicationEvent(event);

            }

            @Test
            void shouldCallThatCorrectHandlerMethod() {
                Method handler = ReflectionUtils.findMethod(CustomEventHandler.class,
                        "afterAssociate", AfterAssociateEvent.class);
                assertThat(handler).isNotNull();

                verify(reflectionService).invokeMethod(org.mockito.ArgumentMatchers.eq(handler),
                        org.mockito.ArgumentMatchers.isA(CustomEventHandler.class),
                        org.mockito.ArgumentMatchers.eq(event));

            }

        }

        @Nested
        class GivenAnEventHandlerAndABeforeUnassociateEvent {
            @BeforeEach
            void setUp() {
                reflectionService = mock(ReflectionService.class);
                invoker = new AnnotatedStoreEventInvoker(reflectionService);

                EventSource source = new EventSource();
                event = new BeforeUnassociateEvent(source, store);

                invoker.postProcessAfterInitialization(new CustomEventHandler(),
                        "custom-bean");
                invoker.onApplicationEvent(event);

            }

            @Test
            void shouldCallThatCorrectHandlerMethod() {
                Method handler = ReflectionUtils.findMethod(CustomEventHandler.class,
                        "beforeUnassociate", Object.class);
                assertThat(handler).isNotNull();

                verify(reflectionService).invokeMethod(org.mockito.ArgumentMatchers.eq(handler),
                        org.mockito.ArgumentMatchers.isA(CustomEventHandler.class),
                        org.mockito.ArgumentMatchers.eq(event.getSource()));

            }

        }

        @Nested
        class GivenAnEventHandlerAcceptingTheEventAndABeforeUnassociateEvent {
            @BeforeEach
            void setUp() {
                reflectionService = mock(ReflectionService.class);
                invoker = new AnnotatedStoreEventInvoker(reflectionService);

                EventSource source = new EventSource();
                event = new BeforeUnassociateEvent(source, store);

                invoker.postProcessAfterInitialization(new CustomEventHandler(),
                        "custom-bean");
                invoker.onApplicationEvent(event);

            }

            @Test
            void shouldCallThatCorrectHandlerMethod() {
                Method handler = ReflectionUtils.findMethod(CustomEventHandler.class,
                        "beforeUnassociate", BeforeUnassociateEvent.class);
                assertThat(handler).isNotNull();

                verify(reflectionService).invokeMethod(org.mockito.ArgumentMatchers.eq(handler),
                        org.mockito.ArgumentMatchers.isA(CustomEventHandler.class),
                        org.mockito.ArgumentMatchers.eq(event));

            }

        }

        @Nested
        class GivenAnEventHandlerAndAAfterUnassociateEvent {
            @BeforeEach
            void setUp() {
                reflectionService = mock(ReflectionService.class);
                invoker = new AnnotatedStoreEventInvoker(reflectionService);

                EventSource source = new EventSource();
                event = new AfterUnassociateEvent(source, store);

                invoker.postProcessAfterInitialization(new CustomEventHandler(),
                        "custom-bean");
                invoker.onApplicationEvent(event);

            }

            @Test
            void shouldCallThatCorrectHandlerMethod() {
                Method handler = ReflectionUtils.findMethod(CustomEventHandler.class,
                        "afterUnassociate", Object.class);
                assertThat(handler).isNotNull();

                verify(reflectionService).invokeMethod(org.mockito.ArgumentMatchers.eq(handler),
                        org.mockito.ArgumentMatchers.isA(CustomEventHandler.class),
                        org.mockito.ArgumentMatchers.eq(event.getSource()));

            }

        }

        @Nested
        class GivenAnEventHandlerAcceptingTheEventAndAAfterUnassociateEvent {
            @BeforeEach
            void setUp() {
                reflectionService = mock(ReflectionService.class);
                invoker = new AnnotatedStoreEventInvoker(reflectionService);

                EventSource source = new EventSource();
                event = new AfterUnassociateEvent(source, store);

                invoker.postProcessAfterInitialization(new CustomEventHandler(),
                        "custom-bean");
                invoker.onApplicationEvent(event);

            }

            @Test
            void shouldCallThatCorrectHandlerMethod() {
                Method handler = ReflectionUtils.findMethod(CustomEventHandler.class,
                        "afterUnassociate", AfterUnassociateEvent.class);
                assertThat(handler).isNotNull();

                verify(reflectionService).invokeMethod(org.mockito.ArgumentMatchers.eq(handler),
                        org.mockito.ArgumentMatchers.isA(CustomEventHandler.class),
                        org.mockito.ArgumentMatchers.eq(event));

            }

        }

        @Nested
        class GivenAnEventHandlerAndABeforeGetContentEvent {
            @BeforeEach
            void setUp() {
                reflectionService = mock(ReflectionService.class);
                invoker = new AnnotatedStoreEventInvoker(reflectionService);

                EventSource source = new EventSource();
                event = new BeforeGetContentEvent(source, store);

                invoker.postProcessAfterInitialization(new CustomEventHandler(),
                        "custom-bean");
                invoker.onApplicationEvent(event);

            }

            @Test
            void shouldCallThatCorrectHandlerMethod() {
                Method handler = ReflectionUtils.findMethod(CustomEventHandler.class,
                        "beforeGetContent", Object.class);
                assertThat(handler).isNotNull();

                verify(reflectionService).invokeMethod(org.mockito.ArgumentMatchers.eq(handler),
                        org.mockito.ArgumentMatchers.isA(CustomEventHandler.class),
                        org.mockito.ArgumentMatchers.eq(event.getSource()));

            }

        }

        @Nested
        class GivenAnEventHandlerAcceptingTheEventAndABeforeGetContentEvent {
            @BeforeEach
            void setUp() {
                reflectionService = mock(ReflectionService.class);
                invoker = new AnnotatedStoreEventInvoker(reflectionService);

                EventSource source = new EventSource();
                event = new BeforeGetContentEvent(source, store);

                invoker.postProcessAfterInitialization(new CustomEventHandler(),
                        "custom-bean");
                invoker.onApplicationEvent(event);

            }

            @Test
            void shouldCallThatCorrectHandlerMethod() {
                Method handler = ReflectionUtils.findMethod(CustomEventHandler.class,
                        "beforeGetContent", BeforeGetContentEvent.class);
                assertThat(handler).isNotNull();

                verify(reflectionService).invokeMethod(org.mockito.ArgumentMatchers.eq(handler),
                        org.mockito.ArgumentMatchers.isA(CustomEventHandler.class),
                        org.mockito.ArgumentMatchers.eq(event));

            }

        }

        @Nested
        class GivenAnEventHandlerAndAAfterGetContentEvent {
            @BeforeEach
            void setUp() {
                reflectionService = mock(ReflectionService.class);
                invoker = new AnnotatedStoreEventInvoker(reflectionService);

                EventSource source = new EventSource();
                event = new AfterGetContentEvent(source, store);

                invoker.postProcessAfterInitialization(new CustomEventHandler(),
                        "custom-bean");
                invoker.onApplicationEvent(event);

            }

            @Test
            void shouldCallThatCorrectHandlerMethod() {
                Method handler = ReflectionUtils.findMethod(CustomEventHandler.class,
                        "afterGetContent", Object.class);
                assertThat(handler).isNotNull();

                verify(reflectionService).invokeMethod(org.mockito.ArgumentMatchers.eq(handler),
                        org.mockito.ArgumentMatchers.isA(CustomEventHandler.class),
                        org.mockito.ArgumentMatchers.eq(event.getSource()));

            }

        }

        @Nested
        class GivenAnEventHandlerAcceptingTheEventAndAAfterGetContentEvent {
            @BeforeEach
            void setUp() {
                reflectionService = mock(ReflectionService.class);
                invoker = new AnnotatedStoreEventInvoker(reflectionService);

                EventSource source = new EventSource();
                event = new AfterGetContentEvent(source, store);

                invoker.postProcessAfterInitialization(new CustomEventHandler(),
                        "custom-bean");
                invoker.onApplicationEvent(event);

            }

            @Test
            void shouldCallThatCorrectHandlerMethod() {
                Method handler = ReflectionUtils.findMethod(CustomEventHandler.class,
                        "afterGetContent", AfterGetContentEvent.class);
                assertThat(handler).isNotNull();

                verify(reflectionService).invokeMethod(org.mockito.ArgumentMatchers.eq(handler),
                        org.mockito.ArgumentMatchers.isA(CustomEventHandler.class),
                        org.mockito.ArgumentMatchers.eq(event));

            }

        }

        @Nested
        class GivenAnEventHandlerAndABeforeSetContentEvent {
            @BeforeEach
            void setUp() {
                reflectionService = mock(ReflectionService.class);
                invoker = new AnnotatedStoreEventInvoker(reflectionService);

                EventSource source = new EventSource();
                event = new BeforeSetContentEvent(source, store, (InputStream) null);

                invoker.postProcessAfterInitialization(new CustomEventHandler(),
                        "custom-bean");
                invoker.onApplicationEvent(event);

            }

            @Test
            void shouldCallThatCorrectHandlerMethod() {
                Method handler = ReflectionUtils.findMethod(CustomEventHandler.class,
                        "beforeSetContent", Object.class);
                assertThat(handler).isNotNull();

                verify(reflectionService).invokeMethod(org.mockito.ArgumentMatchers.eq(handler),
                        org.mockito.ArgumentMatchers.isA(CustomEventHandler.class),
                        org.mockito.ArgumentMatchers.eq(event.getSource()));

            }

        }

        @Nested
        class GivenAnEventHandlerAcceptingTheEventAndABeforeSetContentEvent {
            @BeforeEach
            void setUp() {
                reflectionService = mock(ReflectionService.class);
                invoker = new AnnotatedStoreEventInvoker(reflectionService);

                EventSource source = new EventSource();
                event = new BeforeSetContentEvent(source, store, (InputStream) null);

                invoker.postProcessAfterInitialization(new CustomEventHandler(),
                        "custom-bean");
                invoker.onApplicationEvent(event);

            }

            @Test
            void shouldCallThatCorrectHandlerMethod() {
                Method handler = ReflectionUtils.findMethod(CustomEventHandler.class,
                        "beforeSetContent", BeforeSetContentEvent.class);
                assertThat(handler).isNotNull();

                verify(reflectionService).invokeMethod(org.mockito.ArgumentMatchers.eq(handler),
                        org.mockito.ArgumentMatchers.isA(CustomEventHandler.class),
                        org.mockito.ArgumentMatchers.eq(event));

            }

        }

        @Nested
        class GivenAnEventHandlerAndABeforeSetContentEvent2 {
            @BeforeEach
            void setUp() {
                reflectionService = mock(ReflectionService.class);
                invoker = new AnnotatedStoreEventInvoker(reflectionService);

                EventSource source = new EventSource();
                event = new AfterSetContentEvent(source, store);

                invoker.postProcessAfterInitialization(new CustomEventHandler(),
                        "custom-bean");
                invoker.onApplicationEvent(event);

            }

            @Test
            void shouldCallThatCorrectHandlerMethod() {
                Method handler = ReflectionUtils.findMethod(CustomEventHandler.class,
                        "afterSetContent", Object.class);
                assertThat(handler).isNotNull();

                verify(reflectionService).invokeMethod(org.mockito.ArgumentMatchers.eq(handler),
                        org.mockito.ArgumentMatchers.isA(CustomEventHandler.class),
                        org.mockito.ArgumentMatchers.eq(event.getSource()));

            }

        }

        @Nested
        class GivenAnEventHandlerAcceptingTheEventAndAAfterSetContentEvent {
            @BeforeEach
            void setUp() {
                reflectionService = mock(ReflectionService.class);
                invoker = new AnnotatedStoreEventInvoker(reflectionService);

                EventSource source = new EventSource();
                event = new AfterSetContentEvent(source, store);

                invoker.postProcessAfterInitialization(new CustomEventHandler(),
                        "custom-bean");
                invoker.onApplicationEvent(event);

            }

            @Test
            void shouldCallThatCorrectHandlerMethod() {
                Method handler = ReflectionUtils.findMethod(CustomEventHandler.class,
                        "afterSetContent", AfterSetContentEvent.class);
                assertThat(handler).isNotNull();

                verify(reflectionService).invokeMethod(org.mockito.ArgumentMatchers.eq(handler),
                        org.mockito.ArgumentMatchers.isA(CustomEventHandler.class),
                        org.mockito.ArgumentMatchers.eq(event));

            }

        }

        @Nested
        class GivenAnEventHandlerAndABeforeUnsetContentEvent {
            @BeforeEach
            void setUp() {
                reflectionService = mock(ReflectionService.class);
                invoker = new AnnotatedStoreEventInvoker(reflectionService);

                EventSource source = new EventSource();
                event = new BeforeUnsetContentEvent(source, store);

                invoker.postProcessAfterInitialization(new CustomEventHandler(),
                        "custom-bean");
                invoker.onApplicationEvent(event);

            }

            @Test
            void shouldCallThatCorrectHandlerMethod() {
                Method handler = ReflectionUtils.findMethod(CustomEventHandler.class,
                        "beforeUnsetContent", Object.class);
                assertThat(handler).isNotNull();

                verify(reflectionService).invokeMethod(org.mockito.ArgumentMatchers.eq(handler),
                        org.mockito.ArgumentMatchers.isA(CustomEventHandler.class),
                        org.mockito.ArgumentMatchers.eq(event.getSource()));

            }

        }

        @Nested
        class GivenAnEventHandlerAcceptingTheEventAndABeforeUnsetContentEvent {
            @BeforeEach
            void setUp() {
                reflectionService = mock(ReflectionService.class);
                invoker = new AnnotatedStoreEventInvoker(reflectionService);

                EventSource source = new EventSource();
                event = new BeforeUnsetContentEvent(source, store);

                invoker.postProcessAfterInitialization(new CustomEventHandler(),
                        "custom-bean");
                invoker.onApplicationEvent(event);

            }

            @Test
            void shouldCallThatCorrectHandlerMethod() {
                Method handler = ReflectionUtils.findMethod(CustomEventHandler.class,
                        "beforeUnsetContent", BeforeUnsetContentEvent.class);
                assertThat(handler).isNotNull();

                verify(reflectionService).invokeMethod(org.mockito.ArgumentMatchers.eq(handler),
                        org.mockito.ArgumentMatchers.isA(CustomEventHandler.class),
                        org.mockito.ArgumentMatchers.eq(event));

            }

        }

        @Nested
        class GivenAnEventHandlerAndAAfterUnsetContentEvent {
            @BeforeEach
            void setUp() {
                reflectionService = mock(ReflectionService.class);
                invoker = new AnnotatedStoreEventInvoker(reflectionService);

                EventSource source = new EventSource();
                event = new AfterUnsetContentEvent(source, store);

                invoker.postProcessAfterInitialization(new CustomEventHandler(),
                        "custom-bean");
                invoker.onApplicationEvent(event);

            }

            @Test
            void shouldCallThatCorrectHandlerMethod() {
                Method handler = ReflectionUtils.findMethod(CustomEventHandler.class,
                        "afterUnsetContent", Object.class);
                assertThat(handler).isNotNull();

                verify(reflectionService).invokeMethod(org.mockito.ArgumentMatchers.eq(handler),
                        org.mockito.ArgumentMatchers.isA(CustomEventHandler.class),
                        org.mockito.ArgumentMatchers.eq(event.getSource()));

            }

        }

        @Nested
        class GivenAnEventHandlerAcceptingTheEventAndAAfterUnsetContentEvent {
            @BeforeEach
            void setUp() {
                reflectionService = mock(ReflectionService.class);
                invoker = new AnnotatedStoreEventInvoker(reflectionService);

                EventSource source = new EventSource();
                event = new AfterUnsetContentEvent(source, store);

                invoker.postProcessAfterInitialization(new CustomEventHandler(),
                        "custom-bean");
                invoker.onApplicationEvent(event);

            }

            @Test
            void shouldCallThatCorrectHandlerMethod() {
                Method handler = ReflectionUtils.findMethod(CustomEventHandler.class,
                        "afterUnsetContent", AfterUnsetContentEvent.class);
                assertThat(handler).isNotNull();

                verify(reflectionService).invokeMethod(org.mockito.ArgumentMatchers.eq(handler),
                        org.mockito.ArgumentMatchers.isA(CustomEventHandler.class),
                        org.mockito.ArgumentMatchers.eq(event));

            }

        }

        @Nested
        class GivenAnEventHandlerAndAnUnknownEvent {
            @BeforeEach
            void setUp() {
                reflectionService = mock(ReflectionService.class);
                invoker = new AnnotatedStoreEventInvoker(reflectionService);

                EventSource source = new EventSource();
                event = new UnknownContentEvent(source, store);

                invoker.postProcessAfterInitialization(new CustomEventHandler(),
                        "custom-bean");
                invoker.onApplicationEvent(event);

            }

            @Test
            void shouldNotCallAnEventHandler() {
                Method handler = ReflectionUtils.findMethod(CustomEventHandler.class,
                        "afterUnsetContent", Object.class);
                assertThat(handler).isNotNull();

                verify(reflectionService, never()).invokeMethod(any(), any(), any());

            }

        }

    }

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

        public UnknownContentEvent(Object source,
                                   ContentStore<Object, Serializable> store) {
            super(source, store);
        }
    }
}
