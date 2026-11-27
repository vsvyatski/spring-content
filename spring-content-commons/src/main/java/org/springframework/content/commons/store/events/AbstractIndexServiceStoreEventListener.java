package org.springframework.content.commons.store.events;

import org.apache.commons.io.IOUtils;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.search.IndexService;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.commons.store.StoreAccessException;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.core.annotation.Order;
import org.springframework.util.Assert;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

/**
 * {@link AbstractStoreEventListener} that indexes and unindexes entity content through an {@link IndexService} on set
 * and unset events.
 *
 * @author Vladimir Svyatski
 */
@Order(100)
public abstract class AbstractIndexServiceStoreEventListener extends AbstractStoreEventListener<Object> {

    private final IndexService<Object> indexService;

    protected AbstractIndexServiceStoreEventListener(IndexService<Object> indexService) {
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

        try (InputStream content = getContent(contentStore, contentEntity)) {
            indexService.index(contentEntity, new ByteArrayInputStream(IOUtils.toByteArray(content)));
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
