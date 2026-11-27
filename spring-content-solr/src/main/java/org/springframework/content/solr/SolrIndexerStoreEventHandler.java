package org.springframework.content.solr;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.annotations.*;
import org.springframework.content.commons.search.IndexService;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.commons.store.StoreAccessException;
import org.springframework.content.commons.store.events.AfterSetContentEvent;
import org.springframework.content.commons.store.events.BeforeSetContentEvent;
import org.springframework.content.commons.store.events.BeforeUnsetContentEvent;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.core.annotation.Order;
import org.springframework.util.Assert;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

@StoreEventHandler
public class SolrIndexerStoreEventHandler {

    private final IndexService<Object> indexer;

    @Autowired
    public SolrIndexerStoreEventHandler(IndexService<Object> indexer) {
        Assert.notNull(indexer, "indexer must not be null");

        this.indexer = indexer;
    }

    @HandleBeforeSetContent
    @Order(100)
    protected void onBeforeSetContent(BeforeSetContentEvent event) {
        Object contentEntity = event.getSource();
        if (BeanUtils.getAnnotatedValue(contentEntity, ContentId.class) != null) {
            indexer.unindex(contentEntity);
        }
    }

    @HandleAfterSetContent
    @Order(100)
    protected void onAfterSetContent(AfterSetContentEvent event) {
        Object contentEntity = event.getSource();
        if (BeanUtils.getAnnotatedValue(contentEntity, ContentId.class) == null) {
            return;
        }
        if (!(event.getStore() instanceof ContentStore<?, ?> contentStore)) {
            return;
        }
        try {
            indexer.index(contentEntity, getContent(contentStore, contentEntity));
        } catch (IOException e) {
            throw new StoreAccessException("Error reading content for indexing.", e);
        }
    }

    @HandleBeforeUnsetContent
    @Order(100)
    protected void onBeforeUnsetContent(BeforeUnsetContentEvent event) {
        Object contentEntity = event.getSource();
        if (BeanUtils.getAnnotatedValue(contentEntity, ContentId.class) != null) {
            indexer.unindex(contentEntity);
        }
    }

    @SuppressWarnings("unchecked")
    private static InputStream getContent(ContentStore<?, ?> store, Object entity) throws IOException {
        return ((ContentStore<Object, Serializable>) store).getContent(entity);
    }
}
