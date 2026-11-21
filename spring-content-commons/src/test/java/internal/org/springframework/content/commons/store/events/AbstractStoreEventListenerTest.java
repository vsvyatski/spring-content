package internal.org.springframework.content.commons.store.events;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.commons.store.events.*;
import org.springframework.content.commons.store.events.testsupport.StoreEventSource;
import org.springframework.content.commons.testsupport.CommonsTestStores;

import java.io.InputStream;
import java.io.Serializable;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class AbstractStoreEventListenerTest {

    private AbstractStoreEventListener<Object> listener;
    private StoreEvent event;
    private TestContentEventConsumer consumer;
    private ContentStore<Object, Serializable> store;

    static Stream<StoreListenerCase> storeListenerCases() {
        return Stream.of(
            caseForEvent("BeforeGetResource", BeforeGetResourceEvent::new, BeforeGetResourceEvent.class,
                (consumer, ev) -> {
                    ArgumentCaptor<BeforeGetResourceEvent> captor = ArgumentCaptor.forClass(BeforeGetResourceEvent.class);
                    verify(consumer).onBeforeGetResource(captor.capture());
                    assertThat(captor.getValue()).isEqualTo(ev);
                    assertThat(captor.getValue().getSource()).isEqualTo(ev.getSource());
                    assertThat(captor.getValue().getStore()).isEqualTo(ev.getStore());
                },
                (consumer, source) -> verify(consumer).onBeforeGetResource(eq(source))),
            caseForEvent("AfterGetResource", AfterGetResourceEvent::new, AfterGetResourceEvent.class,
                (consumer, ev) -> {
                    ArgumentCaptor<AfterGetResourceEvent> captor = ArgumentCaptor.forClass(AfterGetResourceEvent.class);
                    verify(consumer).onAfterGetResource(captor.capture());
                    assertThat(captor.getValue()).isEqualTo(ev);
                    assertThat(captor.getValue().getSource()).isEqualTo(ev.getSource());
                    assertThat(captor.getValue().getStore()).isEqualTo(ev.getStore());
                },
                (consumer, source) -> verify(consumer).onAfterGetResource(eq(source))),
            caseForEvent("BeforeAssociate", BeforeAssociateEvent::new, BeforeAssociateEvent.class,
                (consumer, ev) -> {
                    ArgumentCaptor<BeforeAssociateEvent> captor = ArgumentCaptor.forClass(BeforeAssociateEvent.class);
                    verify(consumer).onBeforeAssociate(captor.capture());
                    assertThat(captor.getValue()).isEqualTo(ev);
                    assertThat(captor.getValue().getSource()).isEqualTo(ev.getSource());
                    assertThat(captor.getValue().getStore()).isEqualTo(ev.getStore());
                },
                (consumer, source) -> verify(consumer).onBeforeAssociate(eq(source))),
            caseForEvent("AfterAssociate", AfterAssociateEvent::new, AfterAssociateEvent.class,
                (consumer, ev) -> {
                    ArgumentCaptor<AfterAssociateEvent> captor = ArgumentCaptor.forClass(AfterAssociateEvent.class);
                    verify(consumer).onAfterAssociate(captor.capture());
                    assertThat(captor.getValue()).isEqualTo(ev);
                    assertThat(captor.getValue().getSource()).isEqualTo(ev.getSource());
                    assertThat(captor.getValue().getStore()).isEqualTo(ev.getStore());
                },
                (consumer, source) -> verify(consumer).onAfterAssociate(eq(source))),
            caseForEvent("BeforeUnassociate", BeforeUnassociateEvent::new, BeforeUnassociateEvent.class,
                (consumer, ev) -> {
                    ArgumentCaptor<BeforeUnassociateEvent> captor = ArgumentCaptor.forClass(BeforeUnassociateEvent.class);
                    verify(consumer).onBeforeUnassociate(captor.capture());
                    assertThat(captor.getValue()).isEqualTo(ev);
                    assertThat(captor.getValue().getSource()).isEqualTo(ev.getSource());
                    assertThat(captor.getValue().getStore()).isEqualTo(ev.getStore());
                },
                (consumer, source) -> verify(consumer).onBeforeUnassociate(eq(source))),
            caseForEvent("AfterUnassociate", AfterUnassociateEvent::new, AfterUnassociateEvent.class,
                (consumer, ev) -> {
                    ArgumentCaptor<AfterUnassociateEvent> captor = ArgumentCaptor.forClass(AfterUnassociateEvent.class);
                    verify(consumer).onAfterUnassociate(captor.capture());
                    assertThat(captor.getValue()).isEqualTo(ev);
                    assertThat(captor.getValue().getSource()).isEqualTo(ev.getSource());
                    assertThat(captor.getValue().getStore()).isEqualTo(ev.getStore());
                },
                (consumer, source) -> verify(consumer).onAfterUnassociate(eq(source))),
            caseForEvent("BeforeGetContent", BeforeGetContentEvent::new, BeforeGetContentEvent.class,
                (consumer, ev) -> {
                    ArgumentCaptor<BeforeGetContentEvent> captor = ArgumentCaptor.forClass(BeforeGetContentEvent.class);
                    verify(consumer).onBeforeGetContent(captor.capture());
                    assertThat(captor.getValue()).isEqualTo(ev);
                    assertThat(captor.getValue().getSource()).isEqualTo(ev.getSource());
                    assertThat(captor.getValue().getStore()).isEqualTo(ev.getStore());
                },
                (consumer, source) -> verify(consumer).onBeforeGetContent(eq(source))),
            caseForEvent("AfterGetContent", AfterGetContentEvent::new, AfterGetContentEvent.class,
                (consumer, ev) -> {
                    ArgumentCaptor<AfterGetContentEvent> captor = ArgumentCaptor.forClass(AfterGetContentEvent.class);
                    verify(consumer).onAfterGetContent(captor.capture());
                    assertThat(captor.getValue()).isEqualTo(ev);
                    assertThat(captor.getValue().getSource()).isEqualTo(ev.getSource());
                    assertThat(captor.getValue().getStore()).isEqualTo(ev.getStore());
                },
                (consumer, source) -> verify(consumer).onAfterGetContent(eq(source))),
            caseForEvent("BeforeSetContent",
                (source, contentStore) -> new BeforeSetContentEvent(source, contentStore, (InputStream) null),
                BeforeSetContentEvent.class,
                (consumer, ev) -> {
                    ArgumentCaptor<BeforeSetContentEvent> captor = ArgumentCaptor.forClass(BeforeSetContentEvent.class);
                    verify(consumer).onBeforeSetContent(captor.capture());
                    assertThat(captor.getValue()).isEqualTo(ev);
                    assertThat(captor.getValue().getSource()).isEqualTo(ev.getSource());
                    assertThat(captor.getValue().getStore()).isEqualTo(ev.getStore());
                },
                (consumer, source) -> verify(consumer).onBeforeSetContent(eq(source))),
            caseForEvent("AfterSetContent", AfterSetContentEvent::new, AfterSetContentEvent.class,
                (consumer, ev) -> {
                    ArgumentCaptor<AfterSetContentEvent> captor = ArgumentCaptor.forClass(AfterSetContentEvent.class);
                    verify(consumer).onAfterSetContent(captor.capture());
                    assertThat(captor.getValue()).isEqualTo(ev);
                    assertThat(captor.getValue().getSource()).isEqualTo(ev.getSource());
                    assertThat(captor.getValue().getStore()).isEqualTo(ev.getStore());
                },
                (consumer, source) -> verify(consumer).onAfterSetContent(eq(source))),
            caseForEvent("BeforeUnsetContent", BeforeUnsetContentEvent::new, BeforeUnsetContentEvent.class,
                (consumer, ev) -> {
                    ArgumentCaptor<BeforeUnsetContentEvent> captor = ArgumentCaptor.forClass(BeforeUnsetContentEvent.class);
                    verify(consumer).onBeforeUnsetContent(captor.capture());
                    assertThat(captor.getValue()).isEqualTo(ev);
                    assertThat(captor.getValue().getSource()).isEqualTo(ev.getSource());
                    assertThat(captor.getValue().getStore()).isEqualTo(ev.getStore());
                },
                (consumer, source) -> verify(consumer).onBeforeUnsetContent(eq(source))),
            caseForEvent("AfterUnsetContent", AfterUnsetContentEvent::new, AfterUnsetContentEvent.class,
                (consumer, ev) -> {
                    ArgumentCaptor<AfterUnsetContentEvent> captor = ArgumentCaptor.forClass(AfterUnsetContentEvent.class);
                    verify(consumer).onAfterUnsetContent(captor.capture());
                    assertThat(captor.getValue()).isEqualTo(ev);
                    assertThat(captor.getValue().getSource()).isEqualTo(ev.getSource());
                    assertThat(captor.getValue().getStore()).isEqualTo(ev.getStore());
                },
                (consumer, source) -> verify(consumer).onAfterUnsetContent(eq(source)))
        );
    }

    private static StoreListenerCase caseForEvent(
        String label,
        BiFunction<StoreEventSource, ContentStore<Object, Serializable>, StoreEvent> eventFactory,
        Class<? extends StoreEvent> eventType,
        BiConsumer<TestContentEventConsumer, StoreEvent> verifyEventHandler,
        BiConsumer<TestContentEventConsumer, Object> verifySourceHandler) {
        return new StoreListenerCase(label, eventFactory, eventType, verifyEventHandler, verifySourceHandler);
    }

    public interface TestContentEventConsumer {
        void onBeforeGetResource(BeforeGetResourceEvent event);

        void onBeforeGetResource(Object entity);

        void onAfterGetResource(AfterGetResourceEvent event);

        void onAfterGetResource(Object entity);

        void onBeforeAssociate(BeforeAssociateEvent event);

        void onBeforeAssociate(Object entity);

        void onAfterAssociate(AfterAssociateEvent event);

        void onAfterAssociate(Object entity);

        void onBeforeUnassociate(BeforeUnassociateEvent event);

        void onBeforeUnassociate(Object entity);

        void onAfterUnassociate(AfterUnassociateEvent event);

        void onAfterUnassociate(Object entity);

        void onBeforeGetContent(BeforeGetContentEvent event);

        void onBeforeGetContent(Object entity);

        void onAfterGetContent(AfterGetContentEvent event);

        void onAfterGetContent(Object entity);

        void onBeforeSetContent(BeforeSetContentEvent event);

        void onBeforeSetContent(Object argThat);

        void onAfterSetContent(AfterSetContentEvent event);

        void onAfterSetContent(Object argThat);

        void onBeforeUnsetContent(BeforeUnsetContentEvent event);

        void onBeforeUnsetContent(Object argThat);

        void onAfterUnsetContent(AfterUnsetContentEvent event);

        void onAfterUnsetContent(Object argThat);
    }

    private record StoreListenerCase(
        String label,
        BiFunction<StoreEventSource, ContentStore<Object, Serializable>, StoreEvent> eventFactory,
        Class<? extends StoreEvent> eventType,
        BiConsumer<TestContentEventConsumer, StoreEvent> verifyEventHandler,
        BiConsumer<TestContentEventConsumer, Object> verifySourceHandler) {
        @Override
        public @NonNull String toString() {
            return label;
        }
    }

    public static class TestContentEventListener extends AbstractStoreEventListener<Object> {

        private final TestContentEventConsumer consumer;

        public TestContentEventListener(TestContentEventConsumer consumer) {
            super();
            this.consumer = consumer;
        }

        @Override
        protected void onBeforeGetResource(BeforeGetResourceEvent event) {
            consumer.onBeforeGetResource(event);
        }

        @Override
        protected void onBeforeGetResource(Object entity) {
            consumer.onBeforeGetResource(entity);
        }

        @Override
        protected void onAfterGetResource(AfterGetResourceEvent event) {
            consumer.onAfterGetResource(event);
        }

        @Override
        protected void onAfterGetResource(Object entity) {
            consumer.onAfterGetResource(entity);
        }

        @Override
        protected void onBeforeAssociate(BeforeAssociateEvent event) {
            consumer.onBeforeAssociate(event);
        }

        @Override
        protected void onBeforeAssociate(Object entity) {
            consumer.onBeforeAssociate(entity);
        }

        @Override
        protected void onAfterAssociate(AfterAssociateEvent event) {
            consumer.onAfterAssociate(event);
        }

        @Override
        protected void onAfterAssociate(Object entity) {
            consumer.onAfterAssociate(entity);
        }

        @Override
        protected void onBeforeUnassociate(BeforeUnassociateEvent event) {
            consumer.onBeforeUnassociate(event);
        }

        @Override
        protected void onBeforeUnassociate(Object entity) {
            consumer.onBeforeUnassociate(entity);
        }

        @Override
        protected void onAfterUnassociate(AfterUnassociateEvent event) {
            consumer.onAfterUnassociate(event);
        }

        @Override
        protected void onAfterUnassociate(Object entity) {
            consumer.onAfterUnassociate(entity);
        }

        @Override
        protected void onBeforeGetContent(BeforeGetContentEvent event) {
            consumer.onBeforeGetContent(event);
        }

        @Override
        protected void onBeforeGetContent(Object entity) {
            consumer.onBeforeGetContent(entity);
        }

        @Override
        protected void onAfterGetContent(AfterGetContentEvent event) {
            consumer.onAfterGetContent(event);
        }

        @Override
        protected void onAfterGetContent(Object entity) {
            consumer.onAfterGetContent(entity);
        }

        @Override
        protected void onBeforeSetContent(BeforeSetContentEvent event) {
            consumer.onBeforeSetContent(event);
        }

        @Override
        protected void onBeforeSetContent(Object entity) {
            consumer.onBeforeSetContent(entity);
        }

        @Override
        protected void onAfterSetContent(AfterSetContentEvent event) {
            consumer.onAfterSetContent(event);
        }

        @Override
        protected void onAfterSetContent(Object entity) {
            consumer.onAfterSetContent(entity);
        }

        @Override
        protected void onBeforeUnsetContent(BeforeUnsetContentEvent event) {
            consumer.onBeforeUnsetContent(event);
        }

        @Override
        protected void onBeforeUnsetContent(Object entity) {
            consumer.onBeforeUnsetContent(entity);
        }

        @Override
        protected void onAfterUnsetContent(AfterUnsetContentEvent event) {
            consumer.onAfterUnsetContent(event);
        }

        @Override
        protected void onAfterUnsetContent(Object entity) {
            consumer.onAfterUnsetContent(entity);
        }
    }

    @Nested
    class OnApplicationEvent {

        @BeforeEach
        void setUp() {
            consumer = mock(TestContentEventConsumer.class);
            store = CommonsTestStores.mockContentStore();
            listener = new TestContentEventListener(consumer);
        }

        @ParameterizedTest
        @MethodSource("internal.org.springframework.content.commons.store.events.AbstractStoreEventListenerTest#storeListenerCases")
        void shouldCallTheEventConsumer(StoreListenerCase listenerCase) {
            StoreEventSource source = new StoreEventSource();
            event = listenerCase.eventFactory().apply(source, store);
            listener.onApplicationEvent(event);
            listenerCase.verifyEventHandler().accept(consumer, event);
        }

        @ParameterizedTest
        @MethodSource("internal.org.springframework.content.commons.store.events.AbstractStoreEventListenerTest#storeListenerCases")
        void shouldCallTheEventSourceConsumer(StoreListenerCase listenerCase) {
            StoreEventSource source = new StoreEventSource();
            event = listenerCase.eventFactory().apply(source, store);
            listener.onApplicationEvent(event);
            listenerCase.verifySourceHandler().accept(consumer, event.getSource());
        }
    }
}
