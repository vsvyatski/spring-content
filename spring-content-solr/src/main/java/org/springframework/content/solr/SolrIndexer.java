package org.springframework.content.solr;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.annotations.StoreEventHandler;
import org.springframework.content.commons.search.IndexService;
import org.springframework.content.commons.store.events.AbstractIndexServiceStoreEventListener;

@StoreEventHandler
public class SolrIndexer extends AbstractIndexServiceStoreEventListener {

    @Autowired
    public SolrIndexer(IndexService<Object> indexService) {
        super(indexService);
    }
}
