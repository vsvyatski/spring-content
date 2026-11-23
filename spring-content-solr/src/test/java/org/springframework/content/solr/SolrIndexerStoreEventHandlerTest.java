package org.springframework.content.solr;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.content.commons.search.IndexService;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.commons.store.StoreAccessException;
import org.springframework.content.commons.store.events.AfterSetContentEvent;
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
public class SolrIndexerStoreEventHandlerTest {

    private SolrIndexerStoreEventHandler handler;

    // mocks
    private ContentStore<Object, Serializable> store;
    private IndexService<Object> indexer;

    // args
    private Object contentEntity;
    private AfterSetContentEvent afterSetEvent;
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
    class SolrIndexerStoreEventHandlerCases {
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
                    handler = new SolrIndexerStoreEventHandler(indexer);

                    contentEntity = sampleContentEntity();

                    when(store.getContent(eq(contentEntity))).thenReturn(content);

                    try {
                        afterSetEvent = new AfterSetContentEvent(contentEntity, store);
                        handler.onAfterSetContent(afterSetEvent);
                    } catch (Throwable e) {
                        SolrIndexerStoreEventHandlerTest.this.e = e;
                    }
                }

                @Test
                void shouldUseIndexerToIndexContent() {
                    assertThat(e).isNull();
                    verify(indexer).index(eq(contentEntity), eq(content));
                }

                @Nested
                @DisplayName("Given the indexer throws an exception")
                class IndexerThrowsException {
                    @BeforeEach
                    void setUp() throws IOException {
                        store = mock(ContentStore.class);
                        content = mock(InputStream.class);
                        indexer = mock(IndexService.class);
                        handler = new SolrIndexerStoreEventHandler(indexer);

                        contentEntity = sampleContentEntity();

                        when(store.getContent(eq(contentEntity))).thenReturn(content);

                        sae = new StoreAccessException("badness");
                        doThrow(sae).when(indexer).index(any(), any());

                        try {
                            afterSetEvent = new AfterSetContentEvent(contentEntity, store);
                            handler.onAfterSetContent(afterSetEvent);
                        } catch (Throwable e) {
                            SolrIndexerStoreEventHandlerTest.this.e = e;
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
                    content = mock(InputStream.class);
                    indexer = mock(IndexService.class);
                    handler = new SolrIndexerStoreEventHandler(indexer);

                    contentEntity = new SingleContentFields();
                    try {
                        afterSetEvent = new AfterSetContentEvent(contentEntity, store);
                        handler.onAfterSetContent(afterSetEvent);
                    } catch (Throwable e) {
                        SolrIndexerStoreEventHandlerTest.this.e = e;
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
                    handler = new SolrIndexerStoreEventHandler(indexer);

                    contentEntity = new FakeEntity();
                    try {
                        afterSetEvent = new AfterSetContentEvent(contentEntity, store);
                        handler.onAfterSetContent(afterSetEvent);
                    } catch (Throwable e) {
                        SolrIndexerStoreEventHandlerTest.this.e = e;
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
        class OnBeforeUnsetContent {
            @Nested
            @DisplayName("Given a content entity")
            class ContentEntity {
                @BeforeEach
                void setUp() {
                    store = mock(ContentStore.class);
                    content = mock(InputStream.class);
                    indexer = mock(IndexService.class);
                    handler = new SolrIndexerStoreEventHandler(indexer);

                    contentEntity = sampleContentEntity();

                    try {
                        beforeUnsetEvent = new BeforeUnsetContentEvent(contentEntity, store);
                        handler.onBeforeUnsetContent(beforeUnsetEvent);
                    } catch (Exception e) {
                        SolrIndexerStoreEventHandlerTest.this.e = e;
                    }
                }

                @Test
                void shouldUseIndexerToUnindexContent() {
                    assertThat(e).isNull();
                    verify(indexer).unindex(eq(contentEntity));
                }

                @Nested
                @DisplayName("Given aio exception")
                class IOException {
                    @BeforeEach
                    void setUp() {
                        store = mock(ContentStore.class);
                        content = mock(InputStream.class);
                        indexer = mock(IndexService.class);
                        handler = new SolrIndexerStoreEventHandler(indexer);

                        contentEntity = sampleContentEntity();

                        sae = new StoreAccessException("badness");
                        doThrow(sae).when(indexer).unindex(any());

                        try {
                            beforeUnsetEvent = new BeforeUnsetContentEvent(contentEntity, store);
                            handler.onBeforeUnsetContent(beforeUnsetEvent);
                        } catch (Exception e) {
                            SolrIndexerStoreEventHandlerTest.this.e = e;
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
                    handler = new SolrIndexerStoreEventHandler(indexer);

                    contentEntity = new SingleContentFields();
                    try {
                        beforeUnsetEvent = new BeforeUnsetContentEvent(contentEntity, store);
                        handler.onBeforeUnsetContent(beforeUnsetEvent);
                    } catch (Exception e) {
                        SolrIndexerStoreEventHandlerTest.this.e = e;
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
                    handler = new SolrIndexerStoreEventHandler(indexer);

                    contentEntity = new FakeEntity();
                    try {
                        beforeUnsetEvent = new BeforeUnsetContentEvent(contentEntity, store);
                        handler.onBeforeUnsetContent(beforeUnsetEvent);
                    } catch (Exception e) {
                        SolrIndexerStoreEventHandlerTest.this.e = e;
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
