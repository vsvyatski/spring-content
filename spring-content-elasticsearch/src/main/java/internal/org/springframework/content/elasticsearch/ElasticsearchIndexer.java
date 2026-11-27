package internal.org.springframework.content.elasticsearch;

import org.springframework.content.commons.annotations.StoreEventHandler;
import org.springframework.content.commons.search.IndexService;
import org.springframework.content.commons.store.events.AbstractIndexServiceStoreEventListener;

@StoreEventHandler
public class ElasticsearchIndexer extends AbstractIndexServiceStoreEventListener {

    public ElasticsearchIndexer(IndexService<Object> indexService) {
        super(indexService);
    }
}
