package internal.org.springframework.content.jpa.store;

import com.github.f4b6a3.uuid.UuidCreator;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.io.DeletableResource;
import org.springframework.content.commons.mappingcontext.ContentProperty;
import org.springframework.content.commons.mappingcontext.MappingContext;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.commons.store.GetResourceParams;
import org.springframework.content.commons.store.StoreAccessException;
import org.springframework.content.commons.store.StoreEntityValidation;
import org.springframework.content.commons.store.UnsetContentParams;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.content.commons.utils.DomainObjectUtils;
import org.springframework.content.commons.utils.ContentStoreGetContentSupport;
import org.springframework.content.jpa.io.BlobResource;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.WritableResource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.io.*;

import static java.lang.String.format;

public class DefaultJpaStoreImpl<S, SID extends Serializable>
    implements ContentStore<S, SID> {

    private static final Log logger = LogFactory.getLog(DefaultJpaStoreImpl.class);

    private final ResourceLoader loader;
    private final int copyBufferSize;
    private MappingContext mappingContext;

    public DefaultJpaStoreImpl(ResourceLoader blobResourceLoader, MappingContext mappingContext, int copyBufferSize) {
        this.loader = blobResourceLoader;
        this.mappingContext = mappingContext;
        if (this.mappingContext == null) {
            this.mappingContext = new MappingContext("/", ".");
        }
        this.copyBufferSize = copyBufferSize;
    }

    @Override
    public Resource getResource(SID id) {
        return loader.getResource(id.toString());
    }

    @Override
    public Resource getResource(@NonNull S entity) {
        StoreEntityValidation.requireEntity(entity);
        Object contentId = BeanUtils.getFieldWithAnnotation(entity, ContentId.class);
        if (contentId == null) {
            return null;
        }

        return loader.getResource(contentId.toString());
    }

    @Override
    public Resource getResource(@NonNull S entity, PropertyPath propertyPath) {
        StoreEntityValidation.requireEntity(entity);
        return this.getResource(entity, propertyPath, new GetResourceParams(null));
    }

    @Override
    public Resource getResource(@NonNull S entity, PropertyPath propertyPath, GetResourceParams params) {
        StoreEntityValidation.requireEntity(entity);
        SID contentId = getContentId(entity, propertyPath);
        if (contentId == null) {
            return null;
        }
        return getResource(contentId);
    }

    @Override
    public void associate(@NonNull S entity, PropertyPath propertyPath, SID id) {
        StoreEntityValidation.requireEntity(entity);
        ContentProperty property = this.mappingContext.getContentProperty(entity.getClass(), propertyPath.name());
        if (property == null) {
            throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.name()));
        }

        property.setContentId(entity, id, null);
    }

    @Override
    public void unassociate(@NonNull S entity, PropertyPath propertyPath) {
        StoreEntityValidation.requireEntity(entity);
        ContentProperty property = this.mappingContext.getContentProperty(entity.getClass(), propertyPath.name());
        if (property == null) {
            throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.name()));
        }

        property.setContentId(entity, null, DomainObjectUtils::contentIdPropertyMayBeCleared);
    }

    @Override
    public void associate(@NonNull S entity, SID id) {
        StoreEntityValidation.requireEntity(entity);
        BeanUtils.setFieldWithAnnotation(entity, ContentId.class, id.toString());
    }

    @Override
    public void unassociate(@NonNull S entity) {
        StoreEntityValidation.requireEntity(entity);
        BeanUtils.setFieldWithAnnotationConditionally(entity, ContentId.class, null, DomainObjectUtils::contentIdFieldMayBeCleared);
    }

    @Override
    public InputStream getContent(@NonNull S entity) {
        StoreEntityValidation.requireEntity(entity);
        Object id = BeanUtils.getFieldWithAnnotation(entity, ContentId.class);
        if (id == null) {
            return null;
        }
        Resource resource = loader.getResource(id.toString());
        return ContentStoreGetContentSupport.openContentStream(
            entity, ContentStoreGetContentSupport.propertyPathLabel(null), id, resource, "blob resource");
    }

    @Override
    public InputStream getContent(@NonNull S entity, PropertyPath propertyPath) {
        StoreEntityValidation.requireEntity(entity);
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
    public S setContent(@NonNull S entity, InputStream content) {
        StoreEntityValidation.requireEntity(entity);
        Object contentId = BeanUtils.getFieldWithAnnotation(entity, ContentId.class);
        if (contentId == null) {

            Serializable newId = UuidCreator.getTimeOrdered().toString();

            Object convertedId = convertToExternalContentIdType(entity, newId);

            BeanUtils.setFieldWithAnnotation(entity, ContentId.class, convertedId);
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

        BeanUtils.setFieldWithAnnotation(entity, ContentId.class, requireBlobResource(resource).getId());
        BeanUtils.setFieldWithAnnotation(entity, ContentLength.class, contentLen);

        return entity;
    }

    @Transactional
    @Override
    public S setContent(@NonNull S entity, PropertyPath propertyPath, InputStream content) {
        StoreEntityValidation.requireEntity(entity);
        return this.setContent(entity, propertyPath, content, -1L);
    }

    @Transactional
    @Override
    public S setContent(@NonNull S entity, PropertyPath propertyPath, InputStream content, long contentLen) {
        StoreEntityValidation.requireEntity(entity);
        return this.setContent(entity, propertyPath, content, new org.springframework.content.commons.store.SetContentParams(contentLen, true, org.springframework.content.commons.store.SetContentParams.ContentDisposition.Overwrite));
    }

    @Transactional
    @Override
    public S setContent(@NonNull S entity, PropertyPath propertyPath, InputStream content, org.springframework.content.commons.store.SetContentParams params) {
        StoreEntityValidation.requireEntity(entity);
        ContentProperty property = this.mappingContext.getContentProperty(entity.getClass(), propertyPath.name());
        if (property == null) {
            throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.name()));
        }

        SID contentId = getContentId(entity, propertyPath);
        if (contentId == null || params.disposition().equals(org.springframework.content.commons.store.SetContentParams.ContentDisposition.CreateNew)) {

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
    public S setContent(@NonNull S entity, Resource resourceContent) {
        StoreEntityValidation.requireEntity(entity);
        try {
            return this.setContent(entity, resourceContent.getInputStream());
        } catch (IOException e) {
            logger.error(format("Unexpected error setting content for entity %s", entity), e);
            throw new StoreAccessException(format("Setting content for entity %s", entity), e);
        }
    }

    @Transactional
    @Override
    public S setContent(@NonNull S entity, PropertyPath propertyPath, Resource resourceContent) {
        StoreEntityValidation.requireEntity(entity);
        try {
            return this.setContent(entity, propertyPath, resourceContent.getInputStream());
        } catch (IOException e) {
            logger.error(format("Unexpected error setting content for entity %s", entity), e);
            throw new StoreAccessException(format("Setting content for entity %s", entity), e);
        }
    }

    @Transactional
    @Override
    public S unsetContent(@NonNull S metadata) {
        StoreEntityValidation.requireEntity(metadata);
        Object id = BeanUtils.getFieldWithAnnotation(metadata, ContentId.class);
        if (id == null) {
            id = -1L;
        }
        Resource resource = loader.getResource(id.toString());
        if (resource instanceof DeletableResource) {
            try {
                ((DeletableResource) resource).delete();
            } catch (Exception e) {
                logger.error(format("Unexpected error unsetting content for entity %s", metadata));
                throw new StoreAccessException(format("Unsetting content for entity %s", metadata), e);
            }
        }
        unassociate(metadata);
        Class<?> contentLenType = BeanUtils.getFieldWithAnnotationType(metadata, ContentLength.class);
        if (contentLenType != null) {
            BeanUtils.setFieldWithAnnotation(metadata, ContentLength.class, BeanUtils.getDefaultValueForType(contentLenType));
        }

        return metadata;
    }

    @Transactional
    @Override
    public S unsetContent(@NonNull S entity, PropertyPath propertyPath) {
        StoreEntityValidation.requireEntity(entity);
        return this.unsetContent(entity, propertyPath, new org.springframework.content.commons.store.UnsetContentParams(org.springframework.content.commons.store.UnsetContentParams.Disposition.Remove));
    }

    @Transactional
    @Override
    public S unsetContent(@NonNull S entity, PropertyPath propertyPath, UnsetContentParams params) {
        StoreEntityValidation.requireEntity(entity);
        ContentProperty property = this.mappingContext.getContentProperty(entity.getClass(), propertyPath.name());
        if (property == null) {
            throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.name()));
        }

        Resource resource = this.getResource(entity, propertyPath);

        if (resource != null && resource.exists() && resource instanceof DeletableResource && params.disposition().equals(UnsetContentParams.Disposition.Remove)) {
            try {
                ((DeletableResource) resource).delete();
            } catch (Exception e) {
                logger.error(format("Unexpected error unsetting content for entity %s", entity));
                throw new StoreAccessException(format("Unsetting content for entity %s", entity), e);
            }
        }

        if (resource != null) {
            unassociate(entity, propertyPath);
            property.setContentLength(entity, BeanUtils.getDefaultValueForType(property.getContentLengthType().getType()));
            reassignEmbeddableForPersistence(entity, property);
        }

        return entity;
    }

    protected Object convertToExternalContentIdType(S property, Object contentId) {
        ConversionService converter = new DefaultConversionService();
        if (converter.canConvert(TypeDescriptor.forObject(contentId),
            TypeDescriptor.valueOf(BeanUtils.getFieldWithAnnotationType(property,
                ContentId.class)))) {
            contentId = converter.convert(contentId, TypeDescriptor.forObject(contentId),
                TypeDescriptor.valueOf(BeanUtils.getFieldWithAnnotationType(property,
                    ContentId.class)));
            return contentId;
        }
        return contentId.toString();
    }

    private Object convertToExternalContentIdType(Object contentId, TypeDescriptor contentIdType) {
        ConversionService converter = new DefaultConversionService();
        if (converter.canConvert(TypeDescriptor.forObject(contentId),
            contentIdType)) {
            contentId = converter.convert(contentId, TypeDescriptor.forObject(contentId),
                contentIdType);
            return contentId;
        }
        return contentId.toString();
    }

    private SID getContentId(S entity, PropertyPath propertyPath) {
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
        return castStoreContentId(convertToExternalContentIdType(contentId, contentIdType));
    }

    @SuppressWarnings("unchecked")
    private SID castStoreContentId(Object contentId) {
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
}
