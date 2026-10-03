package internal.org.springframework.content.elasticsearch;

import java.io.IOException;

import org.springframework.content.commons.annotations.StoreEventHandler;
import org.springframework.content.commons.search.IndexService;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.commons.store.StoreAccessException;
import org.springframework.content.commons.store.events.AbstractStoreEventListener;
import org.springframework.content.commons.store.events.AfterSetContentEvent;
import org.springframework.content.commons.store.events.BeforeUnsetContentEvent;

@StoreEventHandler
public class ElasticsearchIndexer extends AbstractStoreEventListener<Object> {

    public static final String INDEX_NAME = "spring-content-fulltext-index";

    private final IndexService indexService;

    public ElasticsearchIndexer(IndexService indexService) {
        this.indexService = indexService;
    }

    @Override
    protected void onAfterSetContent(AfterSetContentEvent event) {
        if (event.getStore() instanceof ContentStore) {
            try {
                this.indexService.index(event.getSource(), ((ContentStore) event.getStore()).getContent(event.getSource()));
            } catch (IOException e) {
                throw new StoreAccessException("Error reading content for indexing.", e);
            }
        }
    }

    @Override
    protected void onBeforeUnsetContent(BeforeUnsetContentEvent event) {
        this.indexService.unindex(event.getSource());
    }
}
