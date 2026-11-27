package org.springframework.content.commons.store.events;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.content.commons.search.IndexService;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.commons.store.Store;
import org.springframework.content.commons.store.StoreAccessException;
import org.springframework.content.test.support.FakeEntity;
import org.springframework.content.test.support.SingleContentFields;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.content.test.support.TestSupportData.SAMPLE_CONTENT_ID;

@SuppressWarnings("unchecked")
public class AbstractIndexServiceStoreEventListenerTest {

    private ExposingListener listener;

    private ContentStore<Object, Serializable> store;
    private IndexService<Object> indexService;

    private Object contentEntity;
    private AfterSetContentEvent afterSetEvent;
    private BeforeSetContentEvent beforeSetEvent;
    private BeforeUnsetContentEvent beforeUnsetEvent;
    private StoreAccessException sae;
    private Throwable e;

    private static SingleContentFields sampleContentEntity() {
        SingleContentFields entity = new SingleContentFields();
        entity.setContentId(SAMPLE_CONTENT_ID);
        entity.setContentLen(128L);
        return entity;
    }

    static final class ExposingListener extends AbstractIndexServiceStoreEventListener {
        ExposingListener(IndexService<Object> indexService) {
            super(indexService);
        }

        void afterSetContent(AfterSetContentEvent event) {
            onAfterSetContent(event);
        }

        void beforeSetContent(BeforeSetContentEvent event) {
            onBeforeSetContent(event);
        }

        void beforeUnsetContent(BeforeUnsetContentEvent event) {
            onBeforeUnsetContent(event);
        }
    }

    @Nested
    class AbstractIndexServiceStoreEventListenerCases {
        @Nested
        class OnAfterSetContent {
            @Nested
            @DisplayName("Given a content entity")
            class ContentEntity {
                @BeforeEach
                void setUp() throws IOException {
                    store = mock(ContentStore.class);
                    indexService = mock(IndexService.class);
                    listener = new ExposingListener(indexService);

                    contentEntity = sampleContentEntity();

                    when(store.getContent(eq(contentEntity))).thenAnswer(
                        invocation -> new ByteArrayInputStream("content".getBytes(StandardCharsets.UTF_8)));

                    try {
                        afterSetEvent = new AfterSetContentEvent(contentEntity, store);
                        listener.afterSetContent(afterSetEvent);
                    } catch (Throwable e) {
                        AbstractIndexServiceStoreEventListenerTest.this.e = e;
                    }
                }

                @Test
                void shouldUseIndexServiceToIndexContent() throws IOException {
                    assertThat(e).isNull();
                    verify(store).getContent(contentEntity);
                    ArgumentCaptor<InputStream> indexedContent = ArgumentCaptor.forClass(InputStream.class);
                    verify(indexService).index(eq(contentEntity), indexedContent.capture());
                    assertThat(IOUtils.toString(indexedContent.getValue(), StandardCharsets.UTF_8)).isEqualTo("content");
                }
            }

            @Nested
            @DisplayName("Given the indexer throws an exception")
            class IndexerThrowsException {
                @BeforeEach
                void setUp() throws IOException {
                    store = mock(ContentStore.class);
                    indexService = mock(IndexService.class);
                    listener = new ExposingListener(indexService);

                    contentEntity = sampleContentEntity();

                    when(store.getContent(eq(contentEntity))).thenAnswer(
                        invocation -> new ByteArrayInputStream("content".getBytes(StandardCharsets.UTF_8)));

                    sae = new StoreAccessException("badness");
                    doThrow(sae).when(indexService).index(any(), any());

                    try {
                        afterSetEvent = new AfterSetContentEvent(contentEntity, store);
                        listener.afterSetContent(afterSetEvent);
                    } catch (Throwable e) {
                        AbstractIndexServiceStoreEventListenerTest.this.e = e;
                    }
                }

                @Test
                void shouldReThrowThatException() {
                    assertThat(e).isEqualTo(sae);
                }
            }

            @Nested
            @DisplayName("Given content cannot be read")
            class ContentReadFails {
                @BeforeEach
                void setUp() throws IOException {
                    store = mock(ContentStore.class);
                    indexService = mock(IndexService.class);
                    listener = new ExposingListener(indexService);

                    contentEntity = sampleContentEntity();

                    when(store.getContent(eq(contentEntity))).thenThrow(new IOException("read failed"));

                    try {
                        afterSetEvent = new AfterSetContentEvent(contentEntity, store);
                        listener.afterSetContent(afterSetEvent);
                    } catch (Throwable e) {
                        AbstractIndexServiceStoreEventListenerTest.this.e = e;
                    }
                }

                @Test
                void shouldWrapIOExceptionInStoreAccessException() {
                    assertThat(e).isInstanceOf(StoreAccessException.class)
                            .hasMessage("Error reading content for indexing.")
                            .hasCauseInstanceOf(IOException.class);
                    verify(indexService, never()).index(any(), any());
                }
            }

            @Nested
            @DisplayName("Given a store that is not a content store")
            class NonContentStore {
                @BeforeEach
                void setUp() {
                    Store<Serializable> associativeStore = mock(Store.class);
                    indexService = mock(IndexService.class);
                    listener = new ExposingListener(indexService);

                    contentEntity = sampleContentEntity();

                    try {
                        afterSetEvent = new AfterSetContentEvent(contentEntity, associativeStore);
                        listener.afterSetContent(afterSetEvent);
                    } catch (Throwable e) {
                        AbstractIndexServiceStoreEventListenerTest.this.e = e;
                    }
                }

                @Test
                void shouldNotIndex() {
                    assertThat(e).isNull();
                    verify(indexService, never()).index(any(), any());
                }
            }

            @Nested
            @DisplayName("Given a content entity with a null content ID")
            class ContentEntityWithNullContentId {
                @BeforeEach
                void setUp() {
                    store = mock(ContentStore.class);
                    indexService = mock(IndexService.class);
                    listener = new ExposingListener(indexService);

                    contentEntity = new SingleContentFields();
                    try {
                        afterSetEvent = new AfterSetContentEvent(contentEntity, store);
                        listener.afterSetContent(afterSetEvent);
                    } catch (Throwable e) {
                        AbstractIndexServiceStoreEventListenerTest.this.e = e;
                    }
                }

                @Test
                void shouldNotIndex() {
                    assertThat(e).isNull();
                    verify(indexService, never()).index(any(), any());
                }
            }

            @Nested
            @DisplayName("Given a bogus content entity")
            class BogusContentEntity {
                @BeforeEach
                void setUp() {
                    store = mock(ContentStore.class);
                    indexService = mock(IndexService.class);
                    listener = new ExposingListener(indexService);

                    contentEntity = new FakeEntity();
                    try {
                        afterSetEvent = new AfterSetContentEvent(contentEntity, store);
                        listener.afterSetContent(afterSetEvent);
                    } catch (Throwable e) {
                        AbstractIndexServiceStoreEventListenerTest.this.e = e;
                    }
                }

                @Test
                void shouldNotIndex() {
                    assertThat(e).isNull();
                    verify(indexService, never()).index(any(), any());
                }
            }
        }

        @Nested
        class OnBeforeSetContent {
            @Nested
            @DisplayName("Given a content entity")
            class ContentEntity {
                @BeforeEach
                void setUp() {
                    store = mock(ContentStore.class);
                    indexService = mock(IndexService.class);
                    listener = new ExposingListener(indexService);

                    contentEntity = sampleContentEntity();

                    try {
                        beforeSetEvent = new BeforeSetContentEvent(contentEntity, store, (InputStream) null);
                        listener.beforeSetContent(beforeSetEvent);
                    } catch (Throwable e) {
                        AbstractIndexServiceStoreEventListenerTest.this.e = e;
                    }
                }

                @Test
                void shouldUseIndexServiceToUnindexExistingContent() {
                    assertThat(e).isNull();
                    verify(indexService).unindex(eq(contentEntity));
                    verify(indexService, never()).index(any(), any());
                }
            }

            @Nested
            @DisplayName("Given a content entity with a null content ID")
            class ContentEntityWithNullContentId {
                @BeforeEach
                void setUp() {
                    store = mock(ContentStore.class);
                    indexService = mock(IndexService.class);
                    listener = new ExposingListener(indexService);

                    contentEntity = new SingleContentFields();
                    try {
                        beforeSetEvent = new BeforeSetContentEvent(contentEntity, store, (InputStream) null);
                        listener.beforeSetContent(beforeSetEvent);
                    } catch (Throwable e) {
                        AbstractIndexServiceStoreEventListenerTest.this.e = e;
                    }
                }

                @Test
                void shouldNotUseIndexService() {
                    assertThat(e).isNull();
                    verify(indexService, never()).unindex(any());
                }
            }
        }

        @Nested
        class OnBeforeUnsetContent {
            @Nested
            @DisplayName("Given a content entity")
            class ContentEntity {
                @BeforeEach
                void setUp() {
                    store = mock(ContentStore.class);
                    indexService = mock(IndexService.class);
                    listener = new ExposingListener(indexService);

                    contentEntity = sampleContentEntity();

                    try {
                        beforeUnsetEvent = new BeforeUnsetContentEvent(contentEntity, store);
                        listener.beforeUnsetContent(beforeUnsetEvent);
                    } catch (Exception e) {
                        AbstractIndexServiceStoreEventListenerTest.this.e = e;
                    }
                }

                @Test
                void shouldUseIndexServiceToUnindexContent() {
                    assertThat(e).isNull();
                    verify(indexService).unindex(eq(contentEntity));
                }

                @Nested
                @DisplayName("Given the index service throws an exception")
                class IndexServiceThrowsException {
                    @BeforeEach
                    void setUp() {
                        store = mock(ContentStore.class);
                        indexService = mock(IndexService.class);
                        listener = new ExposingListener(indexService);

                        contentEntity = sampleContentEntity();

                        sae = new StoreAccessException("badness");
                        doThrow(sae).when(indexService).unindex(any());

                        try {
                            beforeUnsetEvent = new BeforeUnsetContentEvent(contentEntity, store);
                            listener.beforeUnsetContent(beforeUnsetEvent);
                        } catch (Exception e) {
                            AbstractIndexServiceStoreEventListenerTest.this.e = e;
                        }
                    }

                    @Test
                    void shouldReThrowThatException() {
                        assertThat(e).isEqualTo(sae);
                    }
                }
            }

            @Nested
            @DisplayName("Given a content entity with a null content ID")
            class ContentEntityWithNullContentId {
                @BeforeEach
                void setUp() {
                    store = mock(ContentStore.class);
                    indexService = mock(IndexService.class);
                    listener = new ExposingListener(indexService);

                    contentEntity = new SingleContentFields();
                    try {
                        beforeUnsetEvent = new BeforeUnsetContentEvent(contentEntity, store);
                        listener.beforeUnsetContent(beforeUnsetEvent);
                    } catch (Exception e) {
                        AbstractIndexServiceStoreEventListenerTest.this.e = e;
                    }
                }

                @Test
                void shouldNotUnindex() {
                    assertThat(e).isNull();
                    verify(indexService, never()).unindex(any());
                }
            }

            @Nested
            @DisplayName("Given a bogus content entity")
            class BogusContentEntity {
                @BeforeEach
                void setUp() {
                    store = mock(ContentStore.class);
                    indexService = mock(IndexService.class);
                    listener = new ExposingListener(indexService);

                    contentEntity = new FakeEntity();
                    try {
                        beforeUnsetEvent = new BeforeUnsetContentEvent(contentEntity, store);
                        listener.beforeUnsetContent(beforeUnsetEvent);
                    } catch (Exception e) {
                        AbstractIndexServiceStoreEventListenerTest.this.e = e;
                    }
                }

                @Test
                void shouldNeverAttemptDeletion() {
                    assertThat(e).isNull();
                    verify(indexService, never()).unindex(any());
                }
            }
        }
    }
}
