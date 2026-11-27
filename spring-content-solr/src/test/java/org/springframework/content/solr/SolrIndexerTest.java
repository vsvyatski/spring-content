package org.springframework.content.solr;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.content.commons.search.IndexService;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.commons.store.Store;
import org.springframework.content.commons.store.StoreAccessException;
import org.springframework.content.commons.store.events.AfterSetContentEvent;
import org.springframework.content.commons.store.events.BeforeSetContentEvent;
import org.springframework.content.commons.store.events.BeforeUnsetContentEvent;
import org.springframework.content.test.support.FakeEntity;
import org.springframework.content.test.support.SingleContentFields;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.content.test.support.TestSupportData.SAMPLE_CONTENT_ID;

@SuppressWarnings("unchecked")
public class SolrIndexerTest {

    private SolrIndexer handler;

    // mocks
    private ContentStore<Object, Serializable> store;
    private IndexService<Object> indexer;

    // args
    private Object contentEntity;
    private AfterSetContentEvent afterSetEvent;
    private BeforeSetContentEvent beforeSetEvent;
    private BeforeUnsetContentEvent beforeUnsetEvent;
    private InputStream content;
    private StoreAccessException sae;
    private Throwable e;

    private static SingleContentFields sampleContentEntity() {
        SingleContentFields entity = new SingleContentFields();
        entity.setContentId(SAMPLE_CONTENT_ID);
        entity.setContentLen(128L);
        return entity;
    }

    @Nested
    class SolrIndexerCases {
        @Nested
        class OnAfterSetContent {
            @Nested
            @DisplayName("Given a content entity")
            class ContentEntity {
                @BeforeEach
                void setUp() throws IOException {
                    store = mock(ContentStore.class);
                    content = mock(InputStream.class);
                    indexer = mock(IndexService.class);
                    handler = new SolrIndexer(indexer);

                    contentEntity = sampleContentEntity();

                    when(store.getContent(eq(contentEntity))).thenReturn(content);

                    try {
                        afterSetEvent = new AfterSetContentEvent(contentEntity, store);
                        handler.onAfterSetContent(afterSetEvent);
                    } catch (Throwable e) {
                        SolrIndexerTest.this.e = e;
                    }
                }

                @Test
                void shouldUseIndexerToIndexContent() {
                    assertThat(e).isNull();
                    verify(indexer).index(eq(contentEntity), eq(content));
                }
            }

            @Nested
            @DisplayName("Given the indexer throws an exception")
            class IndexerThrowsException {
                @BeforeEach
                void setUp() throws IOException {
                    store = mock(ContentStore.class);
                    content = mock(InputStream.class);
                    indexer = mock(IndexService.class);
                    handler = new SolrIndexer(indexer);

                    contentEntity = sampleContentEntity();

                    when(store.getContent(eq(contentEntity))).thenReturn(content);

                    sae = new StoreAccessException("badness");
                    doThrow(sae).when(indexer).index(any(), any());

                    try {
                        afterSetEvent = new AfterSetContentEvent(contentEntity, store);
                        handler.onAfterSetContent(afterSetEvent);
                    } catch (Throwable e) {
                        SolrIndexerTest.this.e = e;
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
                    indexer = mock(IndexService.class);
                    handler = new SolrIndexer(indexer);

                    contentEntity = sampleContentEntity();

                    when(store.getContent(eq(contentEntity))).thenThrow(new IOException("read failed"));

                    try {
                        afterSetEvent = new AfterSetContentEvent(contentEntity, store);
                        handler.onAfterSetContent(afterSetEvent);
                    } catch (Throwable e) {
                        SolrIndexerTest.this.e = e;
                    }
                }

                @Test
                void shouldWrapIOExceptionInStoreAccessException() {
                    assertThat(e).isInstanceOf(StoreAccessException.class)
                            .hasMessage("Error reading content for indexing.")
                            .hasCauseInstanceOf(IOException.class);
                    verify(indexer, never()).index(any(), any());
                }
            }

            @Nested
            @DisplayName("Given a store that is not a content store")
            class NonContentStore {
                @BeforeEach
                void setUp() {
                    Store<Serializable> associativeStore = mock(Store.class);
                    indexer = mock(IndexService.class);
                    handler = new SolrIndexer(indexer);

                    contentEntity = sampleContentEntity();

                    try {
                        afterSetEvent = new AfterSetContentEvent(contentEntity, associativeStore);
                        handler.onAfterSetContent(afterSetEvent);
                    } catch (Throwable e) {
                        SolrIndexerTest.this.e = e;
                    }
                }

                @Test
                void shouldNotIndex() {
                    assertThat(e).isNull();
                    verify(indexer, never()).index(any(), any());
                }
            }

            @Nested
            @DisplayName("Given a content entity with a null content ID")
            class ContentEntityWithNullContentId {
                @BeforeEach
                void setUp() {
                    store = mock(ContentStore.class);
                    content = mock(InputStream.class);
                    indexer = mock(IndexService.class);
                    handler = new SolrIndexer(indexer);

                    contentEntity = new SingleContentFields();
                    try {
                        afterSetEvent = new AfterSetContentEvent(contentEntity, store);
                        handler.onAfterSetContent(afterSetEvent);
                    } catch (Throwable e) {
                        SolrIndexerTest.this.e = e;
                    }
                }

                @Test
                void shouldCallUpdate() {
                    assertThat(e).isNull();
                    verify(indexer, never()).index(any(), any());
                }
            }

            @Nested
            @DisplayName("Given a bogus content entity")
            class BogusContentEntity {
                @BeforeEach
                void setUp() {
                    store = mock(ContentStore.class);
                    content = mock(InputStream.class);
                    indexer = mock(IndexService.class);
                    handler = new SolrIndexer(indexer);

                    contentEntity = new FakeEntity();
                    try {
                        afterSetEvent = new AfterSetContentEvent(contentEntity, store);
                        handler.onAfterSetContent(afterSetEvent);
                    } catch (Throwable e) {
                        SolrIndexerTest.this.e = e;
                    }
                }

                @Test
                void spec() {
                    assertThat(e).isNull();
                    verify(indexer, never()).index(any(), any());
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
                    indexer = mock(IndexService.class);
                    handler = new SolrIndexer(indexer);

                    contentEntity = sampleContentEntity();

                    try {
                        beforeSetEvent = new BeforeSetContentEvent(contentEntity, store, (InputStream) null);
                        handler.onBeforeSetContent(beforeSetEvent);
                    } catch (Throwable e) {
                        SolrIndexerTest.this.e = e;
                    }
                }

                @Test
                void shouldUseIndexerToUnindexExistingContent() {
                    assertThat(e).isNull();
                    verify(indexer).unindex(eq(contentEntity));
                    verify(indexer, never()).index(any(), any());
                }
            }

            @Nested
            @DisplayName("Given a content entity with a null content ID")
            class ContentEntityWithNullContentId {
                @BeforeEach
                void setUp() {
                    store = mock(ContentStore.class);
                    indexer = mock(IndexService.class);
                    handler = new SolrIndexer(indexer);

                    contentEntity = new SingleContentFields();
                    try {
                        beforeSetEvent = new BeforeSetContentEvent(contentEntity, store, (InputStream) null);
                        handler.onBeforeSetContent(beforeSetEvent);
                    } catch (Throwable e) {
                        SolrIndexerTest.this.e = e;
                    }
                }

                @Test
                void shouldNotUseIndexer() {
                    assertThat(e).isNull();
                    verify(indexer, never()).unindex(any());
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
                    content = mock(InputStream.class);
                    indexer = mock(IndexService.class);
                    handler = new SolrIndexer(indexer);

                    contentEntity = sampleContentEntity();

                    try {
                        beforeUnsetEvent = new BeforeUnsetContentEvent(contentEntity, store);
                        handler.onBeforeUnsetContent(beforeUnsetEvent);
                    } catch (Exception e) {
                        SolrIndexerTest.this.e = e;
                    }
                }

                @Test
                void shouldUseIndexerToUnindexContent() {
                    assertThat(e).isNull();
                    verify(indexer).unindex(eq(contentEntity));
                }

                @Nested
                @DisplayName("Given an I/O exception")
                class IOException {
                    @BeforeEach
                    void setUp() {
                        store = mock(ContentStore.class);
                        content = mock(InputStream.class);
                        indexer = mock(IndexService.class);
                        handler = new SolrIndexer(indexer);

                        contentEntity = sampleContentEntity();

                        sae = new StoreAccessException("badness");
                        doThrow(sae).when(indexer).unindex(any());

                        try {
                            beforeUnsetEvent = new BeforeUnsetContentEvent(contentEntity, store);
                            handler.onBeforeUnsetContent(beforeUnsetEvent);
                        } catch (Exception e) {
                            SolrIndexerTest.this.e = e;
                        }
                    }

                    @Test
                    void shouldThrowContextAccessException() {
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
                    content = mock(InputStream.class);
                    indexer = mock(IndexService.class);
                    handler = new SolrIndexer(indexer);

                    contentEntity = new SingleContentFields();
                    try {
                        beforeUnsetEvent = new BeforeUnsetContentEvent(contentEntity, store);
                        handler.onBeforeUnsetContent(beforeUnsetEvent);
                    } catch (Exception e) {
                        SolrIndexerTest.this.e = e;
                    }
                }

                @Test
                void shouldCallUpdate() {
                    assertThat(e).isNull();
                    verify(indexer, never()).unindex(any());
                }
            }

            @Nested
            @DisplayName("Given a bogus content entity")
            class BogusContentEntity {
                @BeforeEach
                void setUp() {
                    store = mock(ContentStore.class);
                    content = mock(InputStream.class);
                    indexer = mock(IndexService.class);
                    handler = new SolrIndexer(indexer);

                    contentEntity = new FakeEntity();
                    try {
                        beforeUnsetEvent = new BeforeUnsetContentEvent(contentEntity, store);
                        handler.onBeforeUnsetContent(beforeUnsetEvent);
                    } catch (Exception e) {
                        SolrIndexerTest.this.e = e;
                    }
                }

                @Test
                void shouldNeverAttemptDeletion() {
                    assertThat(e).isNull();
                    verify(indexer, never()).unindex(any());
                }
            }
        }
    }
}
