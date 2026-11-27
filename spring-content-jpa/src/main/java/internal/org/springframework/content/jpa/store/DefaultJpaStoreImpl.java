package internal.org.springframework.content.jpa.store;

import com.github.f4b6a3.uuid.UuidCreator;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.io.DeletableResource;
import org.springframework.content.commons.mappingcontext.ContentProperty;
import org.springframework.content.commons.mappingcontext.MappingContext;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.*;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.content.commons.utils.ContentStoreGetContentSupport;
import org.springframework.content.commons.utils.DomainObjectUtils;
import org.springframework.content.jpa.io.BlobResource;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.WritableResource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;

import static java.lang.String.format;

@NullMarked
public class DefaultJpaStoreImpl<S, SID extends Serializable> implements ContentStore<S, SID> {

    private static final Log logger = LogFactory.getLog(DefaultJpaStoreImpl.class);

    private final ResourceLoader loader;
    private final int copyBufferSize;
    private final MappingContext mappingContext;

    public DefaultJpaStoreImpl(ResourceLoader blobResourceLoader, @Nullable MappingContext mappingContext,
                               int copyBufferSize) {
        this.loader = blobResourceLoader;
        this.mappingContext = mappingContext != null ? mappingContext : new MappingContext("/", ".");
        this.copyBufferSize = copyBufferSize;
    }

    @Override
    public @Nullable Resource getResource(SID id) {
        StoreEntityValidation.requireContentId(id);
        return loader.getResource(id.toString());
    }

    @Override
    public @Nullable Resource getResource(S entity) {
        StoreEntityValidation.requireEntity(entity);
        Object contentId = BeanUtils.getAnnotatedValue(entity, ContentId.class);
        if (contentId == null) {
            return null;
        }

        return loader.getResource(contentId.toString());
    }

    @Override
    public @Nullable Resource getResource(S entity, PropertyPath propertyPath) {
        StoreEntityValidation.requireEntity(entity);
        StoreEntityValidation.requirePropertyPath(propertyPath);
        return this.getResource(entity, propertyPath, new GetResourceParams(null));
    }

    @Override
    public @Nullable Resource getResource(S entity, PropertyPath propertyPath, GetResourceParams params) {
        StoreEntityValidation.requireEntity(entity);
        StoreEntityValidation.requirePropertyPath(propertyPath);
        StoreEntityValidation.requireGetResourceParams(params);
        SID contentId = getContentId(entity, propertyPath);
        if (contentId == null) {
            return null;
        }
        return getResource(contentId);
    }

    @Override
    public void associate(S entity, PropertyPath propertyPath, SID id) {
        StoreEntityValidation.requireEntity(entity);
        StoreEntityValidation.requirePropertyPath(propertyPath);
        StoreEntityValidation.requireContentId(id);
        ContentProperty property = this.mappingContext.getContentProperty(entity.getClass(), propertyPath.name());
        if (property == null) {
            throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.name()));
        }

        property.setContentId(entity, id, null);
    }

    @Override
    public void unassociate(S entity, PropertyPath propertyPath) {
        StoreEntityValidation.requireEntity(entity);
        StoreEntityValidation.requirePropertyPath(propertyPath);
        ContentProperty property = this.mappingContext.getContentProperty(entity.getClass(), propertyPath.name());
        if (property == null) {
            throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.name()));
        }

        property.setContentId(entity, null, DomainObjectUtils::contentIdPropertyMayBeCleared);
    }

    @Override
    public void associate(S entity, SID id) {
        StoreEntityValidation.requireEntity(entity);
        StoreEntityValidation.requireContentId(id);
        BeanUtils.setAnnotatedValue(entity, ContentId.class, id.toString());
    }

    @Override
    public void unassociate(S entity) {
        StoreEntityValidation.requireEntity(entity);
        BeanUtils.setAnnotatedValueConditionally(entity, ContentId.class, null, DomainObjectUtils::contentIdFieldMayBeCleared);
    }

    @Override
    public @Nullable InputStream getContent(S entity) {
        StoreEntityValidation.requireEntity(entity);
        Object id = BeanUtils.getAnnotatedValue(entity, ContentId.class);
        if (id == null) {
            return null;
        }
        Resource resource = loader.getResource(id.toString());
        return ContentStoreGetContentSupport.openContentStream(
            entity, ContentStoreGetContentSupport.propertyPathLabel(null), id, resource, "blob resource");
    }

    @Override
    public @Nullable InputStream getContent(S entity, PropertyPath propertyPath) {
        StoreEntityValidation.requireEntity(entity);
        StoreEntityValidation.requirePropertyPath(propertyPath);
        ContentProperty property = this.mappingContext.getContentProperty(entity.getClass(), propertyPath.name());
        if (property == null) {
            throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.name()));
        }
        Object id = property.getContentId(entity);
        if (id == null) {
            return null;
        }
        Resource resource = loader.getResource(id.toString());
        return ContentStoreGetContentSupport.openContentStream(
            entity, ContentStoreGetContentSupport.propertyPathLabel(propertyPath), id, resource, "blob resource");
    }

    @Transactional
    @Override
    public S setContent(S entity, InputStream content) {
        StoreEntityValidation.requireEntity(entity);
        StoreEntityValidation.requireContentInputStream(content);
        Object contentId = BeanUtils.getAnnotatedValue(entity, ContentId.class);
        if (contentId == null) {

            Serializable newId = UuidCreator.getTimeOrdered().toString();

            Object convertedId = convertToExternalContentIdType(entity, newId);

            BeanUtils.setAnnotatedValue(entity, ContentId.class, convertedId);
        }

        Resource resource = getResource(entity);
        if (resource == null) {
            return entity;
        }

        OutputStream os = null;
        long contentLen = -1L;
        try {
            if (resource instanceof WritableResource) {
                os = ((WritableResource) resource).getOutputStream();
                contentLen = IOUtils.copyLarge(content, os, new byte[this.copyBufferSize]);
            }
        } catch (IOException e) {
            logger.error(format("Unexpected error setting content for entity %s", entity), e);
            throw new StoreAccessException(format("Setting content for entity %s", entity), e);
        } finally {
            IOUtils.closeQuietly(content);
            IOUtils.closeQuietly(os);
        }

        BeanUtils.setAnnotatedValue(entity, ContentId.class, requireBlobResource(resource).getId());
        BeanUtils.setAnnotatedValue(entity, ContentLength.class, contentLen);

        return entity;
    }

    @Transactional
    @Override
    public S setContent(S entity, PropertyPath propertyPath, InputStream content) {
        StoreEntityValidation.requireEntity(entity);
        StoreEntityValidation.requirePropertyPath(propertyPath);
        return this.setContent(entity, propertyPath, content, -1L);
    }

    @Transactional
    @Override
    public S setContent(S entity, PropertyPath propertyPath, InputStream content, long contentLen) {
        StoreEntityValidation.requireEntity(entity);
        StoreEntityValidation.requirePropertyPath(propertyPath);
        return this.setContent(entity, propertyPath, content, new SetContentParams(contentLen, true, SetContentParams.ContentDisposition.Overwrite));
    }

    @Transactional
    @Override
    public S setContent(S entity, PropertyPath propertyPath, InputStream content,
                        SetContentParams params) {
        StoreEntityValidation.requireEntity(entity);
        StoreEntityValidation.requirePropertyPath(propertyPath);
        StoreEntityValidation.requireSetContentParams(params);
        StoreEntityValidation.requireContentInputStream(content);
        ContentProperty property = this.mappingContext.getContentProperty(entity.getClass(), propertyPath.name());
        if (property == null) {
            throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.name()));
        }

        SID contentId = getContentId(entity, propertyPath);
        if (contentId == null || params.disposition()
            .equals(SetContentParams.ContentDisposition.CreateNew)) {

            Serializable newId = UuidCreator.getTimeOrdered().toString();
            SID convertedId = convertToStoreContentId(newId, property.getContentIdType(entity));
            setContentId(entity, propertyPath, convertedId);
        }

        Resource resource = getResource(entity, propertyPath);
        if (resource == null) {
            reassignEmbeddableForPersistence(entity, property);
            return entity;
        }

        OutputStream os = null;
        long readLen = -1L;
        try {
            if (resource instanceof WritableResource) {
                os = ((WritableResource) resource).getOutputStream();
                readLen = IOUtils.copyLarge(content, os, new byte[this.copyBufferSize]);
            }
        } catch (IOException e) {
            logger.error(format("Unexpected error setting content for entity %s", entity), e);
            throw new StoreAccessException(format("Setting content for entity %s", entity), e);
        } finally {
            IOUtils.closeQuietly(content);
            IOUtils.closeQuietly(os);
        }

        property.setContentId(entity, requireBlobResource(resource).getId(), null);

        long len = params.contentLength();
        if (len == -1L) {
            len = readLen;
        }
        property.setContentLength(entity, len);
        reassignEmbeddableForPersistence(entity, property);

        return entity;
    }

    @Transactional
    @Override
    public S setContent(S entity, Resource resourceContent) {
        StoreEntityValidation.requireEntity(entity);
        StoreEntityValidation.requireContentResource(resourceContent);
        try {
            return this.setContent(entity, resourceContent.getInputStream());
        } catch (IOException e) {
            logger.error(format("Unexpected error setting content for entity %s", entity), e);
            throw new StoreAccessException(format("Setting content for entity %s", entity), e);
        }
    }

    @Transactional
    @Override
    public S setContent(S entity, PropertyPath propertyPath, Resource resourceContent) {
        StoreEntityValidation.requireEntity(entity);
        StoreEntityValidation.requirePropertyPath(propertyPath);
        StoreEntityValidation.requireContentResource(resourceContent);
        try {
            return this.setContent(entity, propertyPath, resourceContent.getInputStream());
        } catch (IOException e) {
            logger.error(format("Unexpected error setting content for entity %s", entity), e);
            throw new StoreAccessException(format("Setting content for entity %s", entity), e);
        }
    }

    @Transactional
    @Override
    public S unsetContent(S metadata) {
        StoreEntityValidation.requireEntity(metadata);
        Object id = BeanUtils.getAnnotatedValue(metadata, ContentId.class);
        if (id == null) {
            id = -1L;
        }
        deleteIfExists(metadata, loader.getResource(id.toString()));
        unassociate(metadata);
        Class<?> contentLenType = BeanUtils.getAnnotatedFieldType(metadata, ContentLength.class);
        if (contentLenType != null) {
            BeanUtils.setAnnotatedValue(metadata, ContentLength.class, BeanUtils.getDefaultValueForType(contentLenType));
        }

        return metadata;
    }

    @Transactional
    @Override
    public S unsetContent(S entity, PropertyPath propertyPath) {
        StoreEntityValidation.requireEntity(entity);
        StoreEntityValidation.requirePropertyPath(propertyPath);
        return this.unsetContent(entity, propertyPath, new UnsetContentParams(UnsetContentParams.Disposition.Remove));
    }

    @Transactional
    @Override
    public S unsetContent(S entity, PropertyPath propertyPath, UnsetContentParams params) {
        StoreEntityValidation.requireEntity(entity);
        StoreEntityValidation.requirePropertyPath(propertyPath);
        StoreEntityValidation.requireUnsetContentParams(params);
        ContentProperty property = this.mappingContext.getContentProperty(entity.getClass(), propertyPath.name());
        if (property == null) {
            throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.name()));
        }

        Resource resource = this.getResource(entity, propertyPath);
        if (resource != null) {
            if (params.disposition().equals(UnsetContentParams.Disposition.Remove)) deleteIfExists(entity, resource);

            unassociate(entity, propertyPath);
            property.setContentLength(entity, BeanUtils.getDefaultValueForType(property.getContentLengthType().getType()));
            reassignEmbeddableForPersistence(entity, property);
        }

        return entity;
    }

    protected Object convertToExternalContentIdType(S property, Object contentId) {
        ConversionService converter = new DefaultConversionService();
        if (converter.canConvert(TypeDescriptor.forObject(contentId),
            TypeDescriptor.valueOf(BeanUtils.getAnnotatedFieldType(property,
                ContentId.class)))) {
            Object converted = converter.convert(contentId, TypeDescriptor.forObject(contentId),
                TypeDescriptor.valueOf(BeanUtils.getAnnotatedFieldType(property,
                    ContentId.class)));
            assert converted != null;
            return converted;
        }
        return contentId.toString();
    }

    private Object convertToExternalContentIdType(Object contentId, TypeDescriptor contentIdType) {
        ConversionService converter = new DefaultConversionService();
        if (converter.canConvert(TypeDescriptor.forObject(contentId),
            contentIdType)) {
            Object converted = converter.convert(contentId, TypeDescriptor.forObject(contentId),
                contentIdType);
            assert converted != null;
            return converted;
        }
        return contentId.toString();
    }

    private @Nullable SID getContentId(S entity, PropertyPath propertyPath) {
        StoreEntityValidation.requireEntity(entity);
        Assert.notNull(propertyPath, "propertyPath must not be null");

        ContentProperty property = this.mappingContext.getContentProperty(entity.getClass(), propertyPath.name());
        if (property == null) {
            throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.name()));
        }

        return castStoreContentId(property.getContentId(entity));
    }

    private void setContentId(S entity, PropertyPath propertyPath, SID contentId) {
        StoreEntityValidation.requireEntity(entity);
        Assert.notNull(propertyPath, "propertyPath must not be null");

        ContentProperty property = this.mappingContext.getContentProperty(entity.getClass(), propertyPath.name());
        if (property == null) {
            throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.name()));
        }
        property.setContentId(entity, contentId, null);
    }

    private BlobResource requireBlobResource(Resource resource) {
        if (resource instanceof BlobResource blobResource) {
            return blobResource;
        }
        throw new StoreAccessException(format("JPA store expected BlobResource but got %s", resource));
    }

    private SID convertToStoreContentId(Object contentId, TypeDescriptor contentIdType) {
        @SuppressWarnings("unchecked")
        SID sid = (SID) convertToExternalContentIdType(contentId, contentIdType);
        return sid;
    }

    @SuppressWarnings("unchecked")
    private @Nullable SID castStoreContentId(@Nullable Object contentId) {
        return contentId == null ? null : (SID) contentId;
    }

    /**
     * Hibernate may not flush in-place mutations of a reusable {@code @Embedded} instance; assign a copy so content
     * metadata columns are written.
     */
    private void reassignEmbeddableForPersistence(S entity, ContentProperty property) {
        String contentIdPropertyPath = property.getContentIdPropertyPath();
        if (contentIdPropertyPath == null || !contentIdPropertyPath.contains(".")) {
            return;
        }
        String embeddablePath = contentIdPropertyPath.substring(0, contentIdPropertyPath.lastIndexOf('.'));
        BeanWrapper wrapper = new BeanWrapperImpl(entity);
        Object embeddable = wrapper.getPropertyValue(embeddablePath);
        if (embeddable == null) {
            return;
        }
        try {
            Object replacement = org.springframework.beans.BeanUtils.instantiateClass(embeddable.getClass());
            org.springframework.beans.BeanUtils.copyProperties(embeddable, replacement);
            wrapper.setPropertyValue(embeddablePath, replacement);
        } catch (Exception ex) {
            logger.warn(format("Failed to reassign JPA embeddable at %s on %s", embeddablePath, entity), ex);
        }
    }

    private void deleteIfExists(S entity, @Nullable Resource resource) {
        Resource resourceToDelete = resource != null ? resource : this.getResource(entity);
        if (resourceToDelete != null && resourceToDelete.exists() && resourceToDelete instanceof DeletableResource) {
            try {
                ((DeletableResource) resourceToDelete).delete();
            } catch (Exception e) {
                logger.error(format("Unexpected error unsetting content for entity %s", entity));
                throw new StoreAccessException(format("Unsetting content for entity %s", entity), e);
            }
        }
    }
}
