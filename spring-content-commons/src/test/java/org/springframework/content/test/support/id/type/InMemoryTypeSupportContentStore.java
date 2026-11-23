package org.springframework.content.test.support.id.type;

import org.apache.commons.io.IOUtils;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.commons.store.GetResourceParams;
import org.springframework.content.commons.store.SetContentParams;
import org.springframework.content.commons.store.UnsetContentParams;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.core.io.Resource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * In-memory {@link ContentStore} for content-id type ITs (no storage backend).
 *
 * @author Vladimir Svyatski
 */
public class InMemoryTypeSupportContentStore<S extends TypeSupportIdGenerator<SID>, SID extends Serializable>
    implements ContentStore<S, SID> {

    private static final String NOT_SUPPORTED = "The method is not supported in the test class.";

    private final Map<Object, byte[]> storage = new HashMap<>();

    @Override
    public S setContent(S entity, InputStream content) {
        if (entity == null) {
            return null;
        }

        Object contentId = BeanUtils.getFieldWithAnnotation(entity, ContentId.class);
        if (contentId == null) {
            contentId = entity.generateId();
            BeanUtils.setFieldWithAnnotation(entity, ContentId.class, contentId);
        }

        try {
            byte[] data = content != null ? IOUtils.toByteArray(content) : null;
            storage.put(contentId, data);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store an entity in the test storage.", e);
        }

        return entity;
    }

    @Override
    public S unsetContent(S entity) {
        if (entity == null) {
            return null;
        }

        Object contentId = BeanUtils.getFieldWithAnnotation(entity, ContentId.class);
        if (contentId != null) {
            storage.remove(contentId);
            BeanUtils.setFieldWithAnnotation(entity, ContentId.class, null);
        }
        return entity;
    }

    @Override
    public InputStream getContent(S entity) {
        if (entity == null) {
            return null;
        }

        Object contentId = BeanUtils.getFieldWithAnnotation(entity, ContentId.class);
        if (contentId != null) {
            return new ByteArrayInputStream(storage.get(contentId));
        }
        return null;
    }

    @Override
    public S setContent(S entity, PropertyPath propertyPath, InputStream content) {
        throw new UnsupportedOperationException(NOT_SUPPORTED);
    }

    @Override
    public S setContent(S entity, PropertyPath propertyPath, InputStream content, long contentLen) {
        throw new UnsupportedOperationException(NOT_SUPPORTED);
    }

    @Override
    public S setContent(S entity, PropertyPath propertyPath, InputStream content, SetContentParams params) {
        throw new UnsupportedOperationException(NOT_SUPPORTED);
    }

    @Override
    public S setContent(S entity, Resource resourceContent) {
        throw new UnsupportedOperationException(NOT_SUPPORTED);
    }

    @Override
    public S setContent(S entity, PropertyPath propertyPath, Resource resourceContent) {
        throw new UnsupportedOperationException(NOT_SUPPORTED);
    }

    @Override
    public S unsetContent(S entity, PropertyPath propertyPath) {
        throw new UnsupportedOperationException(NOT_SUPPORTED);
    }

    @Override
    public S unsetContent(S entity, PropertyPath propertyPath, UnsetContentParams params) {
        throw new UnsupportedOperationException(NOT_SUPPORTED);
    }

    @Override
    public InputStream getContent(S entity, PropertyPath propertyPath) {
        throw new UnsupportedOperationException(NOT_SUPPORTED);
    }

    @Override
    public Resource getResource(S entity) {
        throw new UnsupportedOperationException(NOT_SUPPORTED);
    }

    @Override
    public Resource getResource(S entity, PropertyPath propertyPath) {
        throw new UnsupportedOperationException(NOT_SUPPORTED);
    }

    @Override
    public Resource getResource(S entity, PropertyPath propertyPath, GetResourceParams params) {
        throw new UnsupportedOperationException(NOT_SUPPORTED);
    }

    @Override
    public void associate(S entity, SID id) {
        throw new UnsupportedOperationException(NOT_SUPPORTED);
    }

    @Override
    public void associate(S entity, PropertyPath propertyPath, SID id) {
        throw new UnsupportedOperationException(NOT_SUPPORTED);
    }

    @Override
    public void unassociate(S entity) {
        throw new UnsupportedOperationException(NOT_SUPPORTED);
    }

    @Override
    public void unassociate(S entity, PropertyPath propertyPath) {
        throw new UnsupportedOperationException(NOT_SUPPORTED);
    }

    @Override
    public Resource getResource(SID id) {
        throw new UnsupportedOperationException(NOT_SUPPORTED);
    }
}
