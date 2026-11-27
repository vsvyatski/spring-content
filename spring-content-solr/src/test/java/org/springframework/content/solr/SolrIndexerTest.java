package org.springframework.content.solr;

import org.junit.jupiter.api.Test;
import org.springframework.content.commons.search.IndexService;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.commons.store.events.AbstractIndexServiceStoreEventListener;
import org.springframework.content.commons.store.events.AfterSetContentEvent;
import org.springframework.content.test.support.SingleContentFields;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.content.test.support.TestSupportData.SAMPLE_CONTENT_ID;

@SuppressWarnings("unchecked")
class SolrIndexerTest {

    @Test
    void shouldExtendAbstractIndexServiceStoreEventListener() {
        IndexService<Object> indexService = mock(IndexService.class);
        assertThat(new SolrIndexer(indexService)).isInstanceOf(AbstractIndexServiceStoreEventListener.class);
    }

    @Test
    void shouldDelegateStoreEventsToIndexService() throws IOException {
        IndexService<Object> indexService = mock(IndexService.class);
        ContentStore<Object, Serializable> store = mock(ContentStore.class);
        InputStream content = mock(InputStream.class);
        SingleContentFields entity = new SingleContentFields();
        entity.setContentId(SAMPLE_CONTENT_ID);

        when(store.getContent(eq(entity))).thenReturn(content);

        ExposingSolrIndexer indexer = new ExposingSolrIndexer(indexService);
        indexer.afterSetContent(new AfterSetContentEvent(entity, store));

        verify(indexService).index(eq(entity), eq(content));
    }

    static final class ExposingSolrIndexer extends SolrIndexer {
        ExposingSolrIndexer(IndexService<Object> indexService) {
            super(indexService);
        }

        void afterSetContent(AfterSetContentEvent event) {
            onAfterSetContent(event);
        }
    }
}
