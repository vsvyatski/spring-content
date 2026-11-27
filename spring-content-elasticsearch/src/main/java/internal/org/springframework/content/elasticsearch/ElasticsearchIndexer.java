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
import org.springframework.util.Assert;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

@StoreEventHandler
public class ElasticsearchIndexer extends AbstractStoreEventListener<Object> {

    private final IndexService<Object> indexService;

    public ElasticsearchIndexer(IndexService<Object> indexService) {
        Assert.notNull(indexService, "indexService must not be null");
        this.indexService = indexService;
    }

    @Override
    protected void onBeforeSetContent(BeforeSetContentEvent event) {
        Object contentEntity = event.getSource();
        if (BeanUtils.getAnnotatedValue(contentEntity, ContentId.class) != null) {
            indexService.unindex(contentEntity);
        }
    }

    @Override
    protected void onAfterSetContent(AfterSetContentEvent event) {
        Object contentEntity = event.getSource();
        if (BeanUtils.getAnnotatedValue(contentEntity, ContentId.class) == null
            || !(event.getStore() instanceof ContentStore<?, ?> contentStore)) {
            return;
        }

        try {
            indexService.index(contentEntity, getContent(contentStore, contentEntity));
        } catch (IOException e) {
            throw new StoreAccessException("Error reading content for indexing.", e);
        }
    }

    @Override
    protected void onBeforeUnsetContent(BeforeUnsetContentEvent event) {
        Object contentEntity = event.getSource();
        if (BeanUtils.getAnnotatedValue(contentEntity, ContentId.class) != null) {
            indexService.unindex(contentEntity);
        }
    }

    @SuppressWarnings("unchecked")
    private static InputStream getContent(ContentStore<?, ?> store, Object entity) throws IOException {
        return ((ContentStore<Object, Serializable>) store).getContent(entity);
    }
}
