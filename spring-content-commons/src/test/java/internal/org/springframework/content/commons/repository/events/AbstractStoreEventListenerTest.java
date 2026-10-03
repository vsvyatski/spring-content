package internal.org.springframework.content.commons.repository.events;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.InputStream;
import java.io.Serializable;

import org.mockito.ArgumentCaptor;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.commons.store.events.StoreEvent;
import org.springframework.content.commons.store.events.AbstractStoreEventListener;
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

@SuppressWarnings("unchecked")
public class AbstractStoreEventListenerTest {

	private AbstractStoreEventListener<Object> listener;
	private StoreEvent event;

	// mocks
	private TestContentEventConsumer consumer;
	private ContentStore<Object, Serializable> store;
	
    @Nested
    class OnApplicationEvent {
        @Nested
        class GivenAContentEventListener {
            @Nested
            class GivenABeforeGetResourceEvent {
                @BeforeEach
                void setUp() {
                    consumer = mock(TestContentEventConsumer.class);
                    store = (ContentStore<Object, Serializable>) mock(ContentStore.class);

                    listener = new TestContentEventListener(consumer);

                    event = new BeforeGetResourceEvent(new EventSource(), store);

                    listener.onApplicationEvent(event);

                }

                @Test
                void shouldCallTheEventConsumer() {
                    ArgumentCaptor<BeforeGetResourceEvent> argumentCaptor = ArgumentCaptor
                    		.forClass(BeforeGetResourceEvent.class);
                    verify(consumer).onBeforeGetResource(argumentCaptor.capture());
                    assertThat(argumentCaptor.getValue()).isEqualTo(event);
                    assertThat(argumentCaptor.getValue().getSource()).isEqualTo(event.getSource());
                    assertThat(argumentCaptor.getValue().getStore()).isEqualTo(store);

                }

                @Test
                void shouldCallTheEventSourceConsumer() {
                    verify(consumer)
                    		.onBeforeGetResource(org.mockito.ArgumentMatchers.eq(event.getSource()));

                }

            }

            @Nested
            class GivenAnAfterGetResourceEvent {
                @BeforeEach
                void setUp() {
                    consumer = mock(TestContentEventConsumer.class);
                    store = (ContentStore<Object, Serializable>) mock(ContentStore.class);

                    listener = new TestContentEventListener(consumer);

                    event = new AfterGetResourceEvent(new EventSource(), store);

                    listener.onApplicationEvent(event);

                }

                @Test
                void shouldCallTheEventConsumer() {
                    ArgumentCaptor<AfterGetResourceEvent> argumentCaptor = ArgumentCaptor
                    		.forClass(AfterGetResourceEvent.class);
                    verify(consumer).onAfterGetResource(argumentCaptor.capture());
                    assertThat(argumentCaptor.getValue()).isEqualTo(event);
                    assertThat(argumentCaptor.getValue().getSource()).isEqualTo(event.getSource());
                    assertThat(argumentCaptor.getValue().getStore()).isEqualTo(store);

                }

                @Test
                void shouldCallTheEventSourceConsumer() {
                    verify(consumer)
                    		.onAfterGetResource(org.mockito.ArgumentMatchers.eq(event.getSource()));

                }

            }

            @Nested
            class GivenABeforeAssociateEvent {
                @BeforeEach
                void setUp() {
                    consumer = mock(TestContentEventConsumer.class);
                    store = (ContentStore<Object, Serializable>) mock(ContentStore.class);

                    listener = new TestContentEventListener(consumer);

                    event = new BeforeAssociateEvent(new EventSource(), store);

                    listener.onApplicationEvent(event);

                }

                @Test
                void shouldCallTheEventConsumer() {
                    ArgumentCaptor<BeforeAssociateEvent> argumentCaptor = ArgumentCaptor
                    		.forClass(BeforeAssociateEvent.class);
                    verify(consumer).onBeforeAssociate(argumentCaptor.capture());
                    assertThat(argumentCaptor.getValue()).isEqualTo(event);
                    assertThat(argumentCaptor.getValue().getSource()).isEqualTo(event.getSource());
                    assertThat(argumentCaptor.getValue().getStore()).isEqualTo(store);

                }

                @Test
                void shouldCallTheEventSourceConsumer() {
                    verify(consumer)
                    		.onBeforeAssociate(org.mockito.ArgumentMatchers.eq(event.getSource()));

                }

            }

            @Nested
            class GivenAnAfterAssociateEvent {
                @BeforeEach
                void setUp() {
                    consumer = mock(TestContentEventConsumer.class);
                    store = (ContentStore<Object, Serializable>) mock(ContentStore.class);

                    listener = new TestContentEventListener(consumer);

                    event = new AfterAssociateEvent(new EventSource(), store);

                    listener.onApplicationEvent(event);

                }

                @Test
                void shouldCallTheEventConsumer() {
                    ArgumentCaptor<AfterAssociateEvent> argumentCaptor = ArgumentCaptor
                    		.forClass(AfterAssociateEvent.class);
                    verify(consumer).onAfterAssociate(argumentCaptor.capture());
                    assertThat(argumentCaptor.getValue()).isEqualTo(event);
                    assertThat(argumentCaptor.getValue().getSource()).isEqualTo(event.getSource());
                    assertThat(argumentCaptor.getValue().getStore()).isEqualTo(store);

                }

                @Test
                void shouldCallTheEventSourceConsumer() {
                    verify(consumer)
                    		.onAfterAssociate(org.mockito.ArgumentMatchers.eq(event.getSource()));

                }

            }

            @Nested
            class GivenABeforeUnassociateEvent {
                @BeforeEach
                void setUp() {
                    consumer = mock(TestContentEventConsumer.class);
                    store = (ContentStore<Object, Serializable>) mock(ContentStore.class);

                    listener = new TestContentEventListener(consumer);

                    event = new BeforeUnassociateEvent(new EventSource(), store);

                    listener.onApplicationEvent(event);

                }

                @Test
                void shouldCallTheEventConsumer() {
                    ArgumentCaptor<BeforeUnassociateEvent> argumentCaptor = ArgumentCaptor
                    		.forClass(BeforeUnassociateEvent.class);
                    verify(consumer).onBeforeUnassociate(argumentCaptor.capture());
                    assertThat(argumentCaptor.getValue()).isEqualTo(event);
                    assertThat(argumentCaptor.getValue().getSource()).isEqualTo(event.getSource());
                    assertThat(argumentCaptor.getValue().getStore()).isEqualTo(store);

                }

                @Test
                void shouldCallTheEventSourceConsumer() {
                    verify(consumer)
                    		.onBeforeUnassociate(org.mockito.ArgumentMatchers.eq(event.getSource()));

                }

            }

            @Nested
            class GivenAnAfterUnassociateEvent {
                @BeforeEach
                void setUp() {
                    consumer = mock(TestContentEventConsumer.class);
                    store = (ContentStore<Object, Serializable>) mock(ContentStore.class);

                    listener = new TestContentEventListener(consumer);

                    event = new AfterUnassociateEvent(new EventSource(), store);

                    listener.onApplicationEvent(event);

                }

                @Test
                void shouldCallTheEventConsumer() {
                    ArgumentCaptor<AfterUnassociateEvent> argumentCaptor = ArgumentCaptor
                    		.forClass(AfterUnassociateEvent.class);
                    verify(consumer).onAfterUnassociate(argumentCaptor.capture());
                    assertThat(argumentCaptor.getValue()).isEqualTo(event);
                    assertThat(argumentCaptor.getValue().getSource()).isEqualTo(event.getSource());
                    assertThat(argumentCaptor.getValue().getStore()).isEqualTo(store);

                }

                @Test
                void shouldCallTheEventSourceConsumer() {
                    verify(consumer)
                    		.onAfterUnassociate(org.mockito.ArgumentMatchers.eq(event.getSource()));

                }

            }

            @Nested
            class GivenABeforeGetContentEvent {
                @BeforeEach
                void setUp() {
                    consumer = mock(TestContentEventConsumer.class);
                    store = (ContentStore<Object, Serializable>) mock(ContentStore.class);

                    listener = new TestContentEventListener(consumer);

                    event = new BeforeGetContentEvent(new EventSource(), store);

                    listener.onApplicationEvent(event);

                }

                @Test
                void shouldCallTheEventConsumer() {
                    ArgumentCaptor<BeforeGetContentEvent> argumentCaptor = ArgumentCaptor
                    		.forClass(BeforeGetContentEvent.class);
                    verify(consumer).onBeforeGetContent(argumentCaptor.capture());
                    assertThat(argumentCaptor.getValue()).isEqualTo(event);
                    assertThat(argumentCaptor.getValue().getSource()).isEqualTo(event.getSource());
                    assertThat(argumentCaptor.getValue().getStore()).isEqualTo(store);

                }

                @Test
                void shouldCallTheEventSourceConsumer() {
                    verify(consumer)
                    		.onBeforeGetContent(org.mockito.ArgumentMatchers.eq(event.getSource()));

                }

            }

            @Nested
            class GivenAnAfterGetContentEvent {
                @BeforeEach
                void setUp() {
                    consumer = mock(TestContentEventConsumer.class);
                    store = (ContentStore<Object, Serializable>) mock(ContentStore.class);

                    listener = new TestContentEventListener(consumer);

                    event = new AfterGetContentEvent(new EventSource(), store);

                    listener.onApplicationEvent(event);

                }

                @Test
                void shouldCallTheEventConsumer() {
                    ArgumentCaptor<AfterGetContentEvent> argumentCaptor = ArgumentCaptor
                    		.forClass(AfterGetContentEvent.class);
                    verify(consumer).onAfterGetContent(argumentCaptor.capture());
                    assertThat(argumentCaptor.getValue()).isEqualTo(event);
                    assertThat(argumentCaptor.getValue().getSource()).isEqualTo(event.getSource());
                    assertThat(argumentCaptor.getValue().getStore()).isEqualTo(store);

                }

                @Test
                void shouldCallTheEventSourceConsumer() {
                    verify(consumer)
                    		.onAfterGetContent(org.mockito.ArgumentMatchers.eq(event.getSource()));

                }

            }

            @Nested
            class GivenABeforeSetContentEvent {
                @BeforeEach
                void setUp() {
                    consumer = mock(TestContentEventConsumer.class);
                    store = (ContentStore<Object, Serializable>) mock(ContentStore.class);

                    listener = new TestContentEventListener(consumer);

                    event = new BeforeSetContentEvent(new EventSource(), store, (InputStream)null);

                    listener.onApplicationEvent(event);

                }

                @Test
                void shouldCallTheEventConsumer() {
                    ArgumentCaptor<BeforeSetContentEvent> argumentCaptor = ArgumentCaptor
                    		.forClass(BeforeSetContentEvent.class);
                    verify(consumer).onBeforeSetContent(argumentCaptor.capture());
                    assertThat(argumentCaptor.getValue()).isEqualTo(event);
                    assertThat(argumentCaptor.getValue().getSource()).isEqualTo(event.getSource());
                    assertThat(argumentCaptor.getValue().getStore()).isEqualTo(store);

                }

                @Test
                void shouldCallTheEventSourceConsumer() {
                    verify(consumer)
                    		.onBeforeSetContent(org.mockito.ArgumentMatchers.eq(event.getSource()));

                }

            }

            @Nested
            class GivenAAfterSetContentEvent {
                @BeforeEach
                void setUp() {
                    consumer = mock(TestContentEventConsumer.class);
                    store = (ContentStore<Object, Serializable>) mock(ContentStore.class);

                    listener = new TestContentEventListener(consumer);

                    event = new AfterSetContentEvent(new EventSource(), store);

                    listener.onApplicationEvent(event);

                }

                @Test
                void shouldCallTheEventConsumer() {
                    ArgumentCaptor<AfterSetContentEvent> argumentCaptor = ArgumentCaptor
                    		.forClass(AfterSetContentEvent.class);
                    verify(consumer).onAfterSetContent(argumentCaptor.capture());
                    assertThat(argumentCaptor.getValue()).isEqualTo(event);
                    assertThat(argumentCaptor.getValue().getSource()).isEqualTo(event.getSource());
                    assertThat(argumentCaptor.getValue().getStore()).isEqualTo(store);

                }

                @Test
                void shouldCallTheEventSourceConsumer() {
                    verify(consumer)
                    		.onAfterSetContent(org.mockito.ArgumentMatchers.eq(event.getSource()));

                }

            }

            @Nested
            class GivenABeforeUnsetContentEvent {
                @BeforeEach
                void setUp() {
                    consumer = mock(TestContentEventConsumer.class);
                    store = (ContentStore<Object, Serializable>) mock(ContentStore.class);

                    listener = new TestContentEventListener(consumer);

                    event = new BeforeUnsetContentEvent(new EventSource(), store);

                    listener.onApplicationEvent(event);

                }

                @Test
                void shouldCallTheEventConsumer() {
                    ArgumentCaptor<BeforeUnsetContentEvent> argumentCaptor = ArgumentCaptor
                    		.forClass(BeforeUnsetContentEvent.class);
                    verify(consumer).onBeforeUnsetContent(argumentCaptor.capture());
                    assertThat(argumentCaptor.getValue()).isEqualTo(event);
                    assertThat(argumentCaptor.getValue().getSource()).isEqualTo(event.getSource());
                    assertThat(argumentCaptor.getValue().getStore()).isEqualTo(store);

                }

                @Test
                void shouldCallTheEventSourceConsumer() {
                    verify(consumer)
                    		.onBeforeUnsetContent(org.mockito.ArgumentMatchers.eq(event.getSource()));

                }

            }

            @Nested
            class GivenAAfterUnsetContentEvent {
                @BeforeEach
                void setUp() {
                    consumer = mock(TestContentEventConsumer.class);
                    store = (ContentStore<Object, Serializable>) mock(ContentStore.class);

                    listener = new TestContentEventListener(consumer);

                    event = new AfterUnsetContentEvent(new EventSource(), store);

                    listener.onApplicationEvent(event);

                }

                @Test
                void shouldCallTheEventConsumer() {
                    ArgumentCaptor<AfterUnsetContentEvent> argumentCaptor = ArgumentCaptor
                    		.forClass(AfterUnsetContentEvent.class);
                    verify(consumer).onAfterUnsetContent(argumentCaptor.capture());
                    assertThat(argumentCaptor.getValue()).isEqualTo(event);
                    assertThat(argumentCaptor.getValue().getSource()).isEqualTo(event.getSource());
                    assertThat(argumentCaptor.getValue().getStore()).isEqualTo(store);

                }

                @Test
                void shouldCallTheEventSourceConsumer() {
                    verify(consumer)
                    		.onAfterUnsetContent(org.mockito.ArgumentMatchers.eq(event.getSource()));

                }

            }

        }

    }

	public static class TestContentEventListener
			extends AbstractStoreEventListener<Object> {
		final private TestContentEventConsumer consumer;

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

	public static class EventSource {
	}
}
