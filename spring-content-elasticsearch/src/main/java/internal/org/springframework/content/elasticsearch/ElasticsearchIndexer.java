package internal.org.springframework.content.elasticsearch;

import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.StoreEventHandler;
import org.springframework.content.commons.search.IndexService;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.commons.store.StoreAccessException;
import org.springframework.content.commons.store.events.AbstractStoreEventListener;
import org.springframework.content.commons.store.events.AfterSetContentEvent;
import org.springframework.content.commons.store.events.BeforeSetContentEvent;
import org.springframework.content.commons.store.events.BeforeUnsetContentEvent;
import org.springframework.content.commons.utils.BeanUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

@StoreEventHandler
public class ElasticsearchIndexer extends AbstractStoreEventListener<Object> {

    private final IndexService<Object> indexService;

    public ElasticsearchIndexer(IndexService<Object> indexService) {
        this.indexService = indexService;
    }

    @Override
    protected void onBeforeSetContent(BeforeSetContentEvent event) {
        Object contentEntity = event.getSource();
        if (BeanUtils.getFieldWithAnnotation(contentEntity, ContentId.class) != null) {
            indexService.unindex(contentEntity);
        }
    }

    @Override
    protected void onAfterSetContent(AfterSetContentEvent event) {
        if (!(event.getStore() instanceof ContentStore<?, ?> contentStore)) {
            return;
        }
        Object contentEntity = event.getSource();
        try {
            indexService.index(contentEntity, getContent(contentStore, contentEntity));
        } catch (IOException e) {
            throw new StoreAccessException("Error reading content for indexing.", e);
        }
    }

    @Override
    protected void onBeforeUnsetContent(BeforeUnsetContentEvent event) {
        indexService.unindex(event.getSource());
    }

    @SuppressWarnings("unchecked")
    private static InputStream getContent(ContentStore<?, ?> store, Object entity) throws IOException {
        return ((ContentStore<Object, Serializable>) store).getContent(entity);
    }
}
