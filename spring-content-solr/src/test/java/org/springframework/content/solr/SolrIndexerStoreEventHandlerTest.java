package org.springframework.content.solr;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.UUID;

import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.commons.store.StoreAccessException;
import org.springframework.content.commons.store.events.AfterSetContentEvent;
import org.springframework.content.commons.store.events.BeforeUnsetContentEvent;
import org.springframework.content.commons.search.IndexService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    
    @Nested
    class SolrIndexerStoreEventHandlerCases {
        @Nested
        class OnAfterSetContent {
            @Nested
            class GivenAContentEntity {
                @BeforeEach
                void setUp() throws IOException {
                    store = mock(ContentStore.class);
                    content = mock(InputStream.class);
                    indexer = mock(IndexService.class);
                    handler = new SolrIndexerStoreEventHandler(indexer);

                    contentEntity = new ContentEntity();
                    ((ContentEntity) contentEntity).contentId = UUID.randomUUID().toString();
                    ((ContentEntity) contentEntity).contentLen = 128L;
                    ((ContentEntity) contentEntity).mimeType = "text/plain";

                    when(store.getContent(eq(contentEntity))).thenReturn(content);

                    try {
                        afterSetEvent = new AfterSetContentEvent(contentEntity, store);
                        handler.onAfterSetContent(afterSetEvent);
                    } catch (Throwable e) {
                        SolrIndexerStoreEventHandlerTest.this.e = e;
                    }

                }

                @Test
                void shouldUseTheIndexerToIndexTheContent() {
                    assertThat(e).isNull();
                    verify(indexer).index(eq(contentEntity), eq(content));

                }

                @Nested
                class GivenTheIndexerThrowsAnException {
                    @BeforeEach
                    void setUp() throws IOException {
                        store = mock(ContentStore.class);
                        content = mock(InputStream.class);
                        indexer = mock(IndexService.class);
                        handler = new SolrIndexerStoreEventHandler(indexer);

                        contentEntity = new ContentEntity();
                        ((ContentEntity) contentEntity).contentId = UUID.randomUUID().toString();
                        ((ContentEntity) contentEntity).contentLen = 128L;
                        ((ContentEntity) contentEntity).mimeType = "text/plain";

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
            class GivenAContentEntityWithANullContentId {
                @BeforeEach
                void setUp() {
                    store = mock(ContentStore.class);
                    content = mock(InputStream.class);
                    indexer = mock(IndexService.class);
                    handler = new SolrIndexerStoreEventHandler(indexer);

                    contentEntity = new ContentEntity();
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
            class GivenABogusContentEntity {
                @BeforeEach
                void setUp() {
                    store = mock(ContentStore.class);
                    content = mock(InputStream.class);
                    indexer = mock(IndexService.class);
                    handler = new SolrIndexerStoreEventHandler(indexer);

                    contentEntity = new NotAContentEntity();
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
            class GivenAContentEntity {
                @BeforeEach
                void setUp() {
                    store = mock(ContentStore.class);
                    content = mock(InputStream.class);
                    indexer = mock(IndexService.class);
                    handler = new SolrIndexerStoreEventHandler(indexer);

                    contentEntity = new ContentEntity();
                    ((ContentEntity) contentEntity).contentId = UUID.randomUUID().toString();
                    ((ContentEntity) contentEntity).contentLen = 128L;
                    ((ContentEntity) contentEntity).mimeType = "text/plain";

                    try {
                        beforeUnsetEvent = new BeforeUnsetContentEvent(contentEntity, store);
                        handler.onBeforeUnsetContent(beforeUnsetEvent);
                    } catch (Exception e) {
                        SolrIndexerStoreEventHandlerTest.this.e = e;
                    }

                }

                @Test
                void shouldUseTheIndexerToUnindexTheContent() {
                    assertThat(e).isNull();
                    verify(indexer).unindex(eq(contentEntity));

                }

                @Nested
                class GivenAIOException {
                    @BeforeEach
                    void setUp() {
                        store = mock(ContentStore.class);
                        content = mock(InputStream.class);
                        indexer = mock(IndexService.class);
                        handler = new SolrIndexerStoreEventHandler(indexer);

                        contentEntity = new ContentEntity();
                        ((ContentEntity) contentEntity).contentId = UUID.randomUUID().toString();
                        ((ContentEntity) contentEntity).contentLen = 128L;
                        ((ContentEntity) contentEntity).mimeType = "text/plain";

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
                    void shouldThrowAContextAccessException() {
                        assertThat(e).isEqualTo(sae);
                    }

                }

            }

            @Nested
            class GivenAContentEntityWithANullContentId {
                @BeforeEach
                void setUp() {
                    store = mock(ContentStore.class);
                    content = mock(InputStream.class);
                    indexer = mock(IndexService.class);
                    handler = new SolrIndexerStoreEventHandler(indexer);

                    contentEntity = new ContentEntity();
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
            class GivenABogusContentEntity {
                @BeforeEach
                void setUp() {
                    store = mock(ContentStore.class);
                    content = mock(InputStream.class);
                    indexer = mock(IndexService.class);
                    handler = new SolrIndexerStoreEventHandler(indexer);

                    contentEntity = new NotAContentEntity();
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

    @Test
    public void test() {
    }

    public static class ContentEntity {
        @ContentId
        public String contentId;
        @ContentLength
        public Long contentLen;
        @MimeType
        public String mimeType;
    }

    public static class NotAContentEntity {
    }
}
