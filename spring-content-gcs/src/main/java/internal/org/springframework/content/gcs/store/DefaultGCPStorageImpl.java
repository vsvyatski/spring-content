package internal.org.springframework.content.gcs.store;

import com.github.f4b6a3.uuid.UuidCreator;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import internal.org.springframework.content.commons.utils.ContentPropertyInfoTypeDescriptor;
import internal.org.springframework.content.gcs.io.GCSResource;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.config.ContentPropertyInfo;
import org.springframework.content.commons.io.DeletableResource;
import org.springframework.content.commons.mappingcontext.ContentProperty;
import org.springframework.content.commons.mappingcontext.MappingContext;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.*;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.content.commons.utils.ContentStoreGetContentSupport;
import org.springframework.content.commons.utils.DomainObjectUtils;
import org.springframework.content.commons.utils.PlacementService;
import org.springframework.context.ApplicationContext;
import org.springframework.core.convert.TypeDescriptor;
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
@Transactional
public class DefaultGCPStorageImpl<S, SID extends Serializable> implements ContentStore<S, SID> {

    private static final Log logger = LogFactory.getLog(DefaultGCPStorageImpl.class);

    private final ResourceLoader loader;
    private final PlacementService placementService;
    private final Storage client;

    private final MappingContext mappingContext;

    public DefaultGCPStorageImpl(ApplicationContext context, ResourceLoader loader,
                                 @Nullable MappingContext mappingContext,
                                 PlacementService placementService, Storage client) {
        Assert.notNull(context, "context must be specified");
        Assert.notNull(loader, "loader must be specified");
        Assert.notNull(placementService, "placementService must be specified");
        Assert.notNull(client, "client must be specified");
        this.loader = loader;
        this.placementService = placementService;
        this.client = client;
        this.mappingContext = mappingContext != null ? mappingContext : new MappingContext("/", ".");
    }

    @Override
    public @Nullable Resource getResource(SID id) {
        StoreEntityValidation.requireContentId(id);

        if (!(id instanceof BlobId)) {
            if (placementService.canConvert(id.getClass(), BlobId.class)) {
                BlobId blobId = placementService.convert(id, BlobId.class);
                assert blobId != null;
                return this.getResourceInternal(blobId);
            }

            throw new StoreAccessException(format("Unable to convert from %s to BlobId", id));
        } else {
            return this.getResourceInternal((BlobId) id);
        }
    }

    @Override
    public @Nullable Resource getResource(S entity) {
        StoreEntityValidation.requireEntity(entity);
        if (placementService.canConvert(entity.getClass(), BlobId.class)) {
            BlobId blobId = placementService.convert(entity, BlobId.class);

            if (blobId != null) {
                return this.getResourceInternal(blobId);
            }
        }

        @SuppressWarnings("unchecked")
        SID contentId = (SID) BeanUtils.getFieldWithAnnotation(entity, ContentId.class);
        if (contentId == null) {
            return null;
        }
        return this.getResource(contentId);
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
        ContentProperty property = this.mappingContext.getContentProperty(entity.getClass(), propertyPath.name());
        if (property == null) {
            throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.name()));
        }

        Object rawContentId = property.getContentId(entity);
        TypeDescriptor contentPropertyInfoType = ContentPropertyInfoTypeDescriptor.withGenerics(entity, property);
        if (placementService.canConvert(contentPropertyInfoType, TypeDescriptor.valueOf(BlobId.class))) {
            @SuppressWarnings("unchecked")
            ContentPropertyInfo<S, SID> contentPropertyInfo =
                new ContentPropertyInfo<>(entity, (SID) rawContentId, propertyPath, property);
            BlobId blobId = (BlobId) placementService.convert(contentPropertyInfo, contentPropertyInfoType,
                TypeDescriptor.valueOf(BlobId.class));

            if (blobId != null) {
                return this.getResourceInternal(blobId);
            }
        }

        if (rawContentId == null) {
            return null;
        }
        @SuppressWarnings("unchecked")
        SID contentId = (SID) rawContentId;
        return this.getResource(contentId);
    }

    protected Resource getResourceInternal(BlobId id) {
        String bucket = id.getBucket();

        String location;
        if (placementService.canConvert(BlobId.class, String.class)) {
            location = placementService.convert(id, String.class);
        } else {
            Object objectId = id.getName();
            location = placementService.convert(objectId, String.class);
        }
        assert location != null;
        location = makeAbsolute(bucket, location);

        Resource resource = loader.getResource(location);
        return new GCSResource(client, bucket, resource);
    }

    @Override
    public void associate(Object entity, Serializable id) {
        StoreEntityValidation.requireEntity(entity);
        StoreEntityValidation.requireContentId(id);
        BeanUtils.setFieldWithAnnotation(entity, ContentId.class, id);
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
    public void unassociate(Object entity) {
        StoreEntityValidation.requireEntity(entity);
        BeanUtils.setFieldWithAnnotationConditionally(entity, ContentId.class, null,
            DomainObjectUtils::contentIdFieldMayBeCleared);
    }

    @Transactional
    @Override
    public S setContent(S entity, InputStream content) {
        StoreEntityValidation.requireEntity(entity);
        StoreEntityValidation.requireContentInputStream(content);

        Object contentId = BeanUtils.getFieldWithAnnotation(entity, ContentId.class);
        if (contentId == null) {

            Serializable newId = UuidCreator.getTimeOrdered().toString();

            Object convertedId = placementService.convert(
                newId,
                TypeDescriptor.forObject(newId),
                TypeDescriptor.valueOf(BeanUtils.getFieldWithAnnotationType(entity, ContentId.class)));

            BeanUtils.setFieldWithAnnotation(entity, ContentId.class, convertedId);
        }

        Resource resource = this.getResource(entity);
        if (resource == null) {
            return entity;
        }

        if (resource instanceof WritableResource) {
            try (OutputStream os = ((WritableResource) resource).getOutputStream()) {
                IOUtils.copy(content, os);
            } catch (IOException e) {
                logger.error(format("Unexpected error setting content for entity %s", entity), e);
                throw new StoreAccessException(format("Setting content for entity %s", entity), e);
            }
        }

        try {
            BeanUtils.setFieldWithAnnotation(entity, ContentLength.class, resource.contentLength());
        } catch (IOException e) {
            logger.error(format("Unexpected error setting content length for entity %s", entity), e);
        }
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
        return this.setContent(entity, propertyPath, content,
            new SetContentParams(contentLen, true, SetContentParams.ContentDisposition.Overwrite));
    }

    @Override
    public S setContent(S entity, PropertyPath propertyPath, InputStream content, SetContentParams params) {
        StoreEntityValidation.requireEntity(entity);
        StoreEntityValidation.requirePropertyPath(propertyPath);
        StoreEntityValidation.requireSetContentParams(params);
        StoreEntityValidation.requireContentInputStream(content);
        ContentProperty property = this.mappingContext.getContentProperty(entity.getClass(), propertyPath.name());
        if (property == null) {
            throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.name()));
        }

        Object contentId = property.getContentId(entity);
        if (contentId == null || params.disposition().equals(SetContentParams.ContentDisposition.CreateNew)) {

            Serializable newId = UuidCreator.getTimeOrdered().toString();

            Object convertedId = placementService.convert(
                newId,
                TypeDescriptor.forObject(newId),
                property.getContentIdType(entity));

            property.setContentId(entity, convertedId, null);
        }

        Resource resource = this.getResource(entity, propertyPath);
        if (resource == null) {
            return entity;
        }

        if (resource instanceof WritableResource) {
            try (OutputStream os = ((WritableResource) resource).getOutputStream()) {
                IOUtils.copy(content, os);
            } catch (IOException e) {
                logger.error(format("Unexpected error setting content for entity %s", entity), e);
                throw new StoreAccessException(format("Setting content for entity %s", entity), e);
            }
        }

        try {
            long len = params.contentLength();
            if (len == -1L) {
                len = resource.contentLength();
            }
            property.setContentLength(entity, len);
        } catch (IOException e) {
            logger.error(format("Unexpected error setting content length for entity %s", entity), e);
        }
        return entity;
    }

    @Override
    public S setContent(S property, Resource resourceContent) {
        StoreEntityValidation.requireEntity(property);
        StoreEntityValidation.requireContentResource(resourceContent);
        try {
            return setContent(property, resourceContent.getInputStream());
        } catch (IOException e) {
            logger.error(format("Unexpected error setting content for entity %s", property), e);
            throw new StoreAccessException(format("Setting content for entity %s", property), e);
        }
    }

    @Override
    public S setContent(S property, PropertyPath propertyPath, Resource resourceContent) {
        StoreEntityValidation.requireEntity(property);
        StoreEntityValidation.requirePropertyPath(propertyPath);
        StoreEntityValidation.requireContentResource(resourceContent);
        try {
            return setContent(property, propertyPath, resourceContent.getInputStream());
        } catch (IOException e) {
            logger.error(format("Unexpected error setting content for entity %s", property), e);
            throw new StoreAccessException(format("Setting content for entity %s", property), e);
        }
    }

    @Transactional
    @Override
    public @Nullable InputStream getContent(S entity) {
        StoreEntityValidation.requireEntity(entity);
        Object contentId = BeanUtils.getFieldWithAnnotation(entity, ContentId.class);
        if (contentId == null) {
            return null;
        }

        Resource resource = this.getResource(entity);
        return ContentStoreGetContentSupport.openContentStream(
            entity, ContentStoreGetContentSupport.propertyPathLabel(null), contentId, resource, "GCS object");
    }

    @Transactional
    @Override
    public @Nullable InputStream getContent(S entity, PropertyPath propertyPath) {
        StoreEntityValidation.requireEntity(entity);
        StoreEntityValidation.requirePropertyPath(propertyPath);
        ContentProperty property = this.mappingContext.getContentProperty(entity.getClass(), propertyPath.name());
        if (property == null) {
            throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.name()));
        }

        Object contentId = property.getContentId(entity);
        if (contentId == null) {
            return null;
        }

        Resource resource = this.getResource(entity, propertyPath);
        return ContentStoreGetContentSupport.openContentStream(
            entity, ContentStoreGetContentSupport.propertyPathLabel(propertyPath), contentId, resource, "GCS object");
    }

    @Transactional
    @Override
    public S unsetContent(S entity) {
        StoreEntityValidation.requireEntity(entity);
        deleteIfExists(entity, null);

        // reset content fields
        BeanUtils.setFieldWithAnnotationConditionally(entity, ContentId.class, null, DomainObjectUtils::contentIdFieldMayBeCleared);

        Class<?> contentLenType = BeanUtils.getFieldWithAnnotationType(entity, ContentLength.class);
        if (contentLenType != null) {
            BeanUtils.setFieldWithAnnotation(entity, ContentLength.class, BeanUtils.getDefaultValueForType(contentLenType));
        }

        return entity;
    }

    @Transactional
    @Override
    public S unsetContent(S entity, PropertyPath propertyPath) {
        StoreEntityValidation.requireEntity(entity);
        StoreEntityValidation.requirePropertyPath(propertyPath);
        return this.unsetContent(entity, propertyPath, new UnsetContentParams(UnsetContentParams.Disposition.Remove));
    }

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

            // reset content fields
            property.setContentId(entity, null, DomainObjectUtils::contentIdPropertyMayBeCleared);
            property.setContentLength(entity, BeanUtils.getDefaultValueForType(property.getContentLengthType().getType()));
        }

        return entity;
    }

    private static String makeAbsolute(String bucket, String location) {
        String locationToUse;
        Assert.state(!location.startsWith("gs://"), "Only gs:// locations supported");
        if (location.startsWith("/")) {
            locationToUse = location.substring(1);
        } else {
            locationToUse = location;
        }
        return format("gs://%s/%s", bucket, locationToUse);
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
