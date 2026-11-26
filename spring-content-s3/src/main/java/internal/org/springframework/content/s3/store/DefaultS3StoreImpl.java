package internal.org.springframework.content.s3.store;

import internal.org.springframework.content.commons.utils.ContentPropertyInfoTypeDescriptor;
import internal.org.springframework.content.s3.io.S3StoreResource;
import internal.org.springframework.content.s3.io.SimpleStorageProtocolResolver;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.NonNull;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.config.ContentPropertyInfo;
import org.springframework.content.commons.io.DeletableResource;
import org.springframework.content.commons.mappingcontext.ContentProperty;
import org.springframework.content.commons.mappingcontext.MappingContext;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.commons.store.GetResourceParams;
import org.springframework.content.commons.store.StoreAccessException;
import org.springframework.content.commons.store.StoreEntityValidation;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.content.commons.utils.ContentStoreGetContentSupport;
import org.springframework.content.commons.utils.PlacementService;
import org.springframework.content.s3.S3ObjectId;
import org.springframework.content.s3.config.MultiTenantS3ClientProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.WritableResource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.UUID;

import static java.lang.String.format;

@Transactional
public class DefaultS3StoreImpl<S, SID extends Serializable> implements ContentStore<S, SID> {

    private static final Log logger = LogFactory.getLog(DefaultS3StoreImpl.class);

    private final ResourceLoader loader;
    private final PlacementService placementService;
    private final S3Client client;
    private final MultiTenantS3ClientProvider clientProvider;

    private MappingContext mappingContext;

    public DefaultS3StoreImpl(ApplicationContext context, ResourceLoader loader, MappingContext mappingContext, PlacementService placementService, S3Client client, MultiTenantS3ClientProvider provider) {
        Assert.notNull(context, "context must be specified");
        Assert.notNull(loader, "loader must be specified");
        Assert.notNull(placementService, "placementService must be specified");
        Assert.notNull(client, "client must be specified");
        this.loader = loader;
        this.placementService = placementService;
        this.client = client;
        this.clientProvider = provider;
        this.mappingContext = mappingContext;
        if (this.mappingContext == null) {
            this.mappingContext = new MappingContext("/", ".");
        }
    }

    @Override
    public Resource getResource(SID id) {
        if (id == null) return null;

        if (!(id instanceof S3ObjectId)) {
            S3ObjectId s3ObjectId;
            if (placementService.canConvert(id.getClass(), S3ObjectId.class)) {
                s3ObjectId = placementService.convert(id, S3ObjectId.class);
                if (s3ObjectId == null) {
                    throw new StoreAccessException(format("Unable to convert from %s to S3ObjectId", id));
                }
                return this.getResourceInternal(s3ObjectId, new GetResourceParams(null));
            }

            throw new StoreAccessException(format("Unable to convert from %s to S3ObjectId", id));
        } else {
            return this.getResourceInternal((S3ObjectId) id, new GetResourceParams(null));
        }
    }

    @Override
    public Resource getResource(@NonNull S entity) {
        StoreEntityValidation.requireEntity(entity);
        if (placementService.canConvert(entity.getClass(), S3ObjectId.class)) {
            S3ObjectId s3ObjectId = placementService.convert(entity, S3ObjectId.class);
            if (s3ObjectId != null) {
                return this.getResourceInternal(s3ObjectId, new GetResourceParams(null));
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
    public Resource getResource(@NonNull S entity, PropertyPath propertyPath) {
        StoreEntityValidation.requireEntity(entity);
        return this.getResource(entity, propertyPath, new GetResourceParams(null));
    }

    @Override
    public Resource getResource(@NonNull S entity, PropertyPath propertyPath, GetResourceParams params) {
        StoreEntityValidation.requireEntity(entity);
        ContentProperty property = this.mappingContext.getContentProperty(entity.getClass(), propertyPath.name());
        if (property == null) {
            throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.name()));
        }

        Object rawContentId = property.getContentId(entity);
        if (rawContentId == null) {
            return null;
        }

        TypeDescriptor contentPropertyInfoType = ContentPropertyInfoTypeDescriptor.withGenerics(entity, property);
        if (placementService.canConvert(contentPropertyInfoType, TypeDescriptor.valueOf(S3ObjectId.class))) {
            @SuppressWarnings("unchecked")
            ContentPropertyInfo<S, SID> contentPropertyInfo =
                new ContentPropertyInfo<>(entity, (SID) rawContentId, propertyPath, property);
            S3ObjectId s3ObjectId = (S3ObjectId) placementService.convert(contentPropertyInfo, contentPropertyInfoType,
                TypeDescriptor.valueOf(S3ObjectId.class));
            if (s3ObjectId != null) {
                return this.getResourceInternal(s3ObjectId, params);
            }
        }

        @SuppressWarnings("unchecked")
        SID contentId = (SID) rawContentId;
        return this.getResource(contentId);
    }

    protected Resource getResourceInternal(S3ObjectId id, GetResourceParams params) {
        String bucket = id.getBucket();

        String location;
        if (placementService.canConvert(S3ObjectId.class, String.class)) {
            location = placementService.convert(id, String.class);
        } else {
            Object objectId = id.getKey();
            location = placementService.convert(objectId, String.class);
        }
        assert location != null;
        location = makeAbsolute(bucket, location);

        return newS3StoreResource(bucket, location, params);
    }

    private S3StoreResource newS3StoreResource(String bucket, String location, GetResourceParams params) {
        S3Client clientToUse = client;
        ResourceLoader loaderToUse = loader;
        if (clientProvider != null) {
            S3Client tenantClient = clientProvider.getS3Client();
            if (tenantClient != null) {
                SimpleStorageProtocolResolver s3Protocol = new SimpleStorageProtocolResolver(tenantClient);
                s3Protocol.afterPropertiesSet();

                DefaultResourceLoader tenantLoader = new DefaultResourceLoader();
                tenantLoader.addProtocolResolver(s3Protocol);

                clientToUse = tenantClient;
                loaderToUse = tenantLoader;
            }
        }

        Resource resource = loaderToUse.getResource(location);
        S3StoreResource s3Resource = new S3StoreResource(clientToUse, bucket, resource);
        s3Resource.setRange(params.range());
        return s3Resource;
    }

    @Override
    public void associate(@NonNull Object entity, Serializable id) {
        StoreEntityValidation.requireEntity(entity);
        BeanUtils.setFieldWithAnnotation(entity, ContentId.class, id);
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
    public void unassociate(@NonNull Object entity) {
        StoreEntityValidation.requireEntity(entity);
        BeanUtils.setFieldWithAnnotationConditionally(entity, ContentId.class, null, field -> {
            for (Annotation annotation : field.getAnnotations()) {
                String canonicalName = annotation.annotationType().getCanonicalName();
                if ("jakarta.persistence.Id".equals(canonicalName)
                    || "org.springframework.data.annotation.Id".equals(canonicalName)) {
                    return false;
                }
            }
            return true;
        });
    }

    @Override
    public void unassociate(@NonNull S entity, PropertyPath propertyPath) {
        StoreEntityValidation.requireEntity(entity);

        ContentProperty property = this.mappingContext.getContentProperty(entity.getClass(), propertyPath.name());
        if (property == null) {
            throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.name()));
        }

        property.setContentId(entity, null, descriptor -> {
            for (Annotation annotation : descriptor.getAnnotations()) {
                String canonicalName = annotation.annotationType().getCanonicalName();
                if ("jakarta.persistence.Id".equals(canonicalName)
                    || "org.springframework.data.annotation.Id".equals(canonicalName)) {
                    return false;
                }
            }
            return true;
        });
    }

    private void setResourceContentTypeFromMimeType(S entity, ContentProperty property, Resource resource) {
        if (resource instanceof S3StoreResource s3StoreResource) {
            Object mimeType;
            if (property == null) {
                mimeType = BeanUtils.getFieldWithAnnotation(entity, MimeType.class);
            } else {
                mimeType = property.getMimeType(entity);
            }

            if (mimeType != null) {
                s3StoreResource.setContentType(mimeType.toString());
            }
        }
    }

    @Transactional
    @Override
    public S setContent(@NonNull S entity, InputStream content) {
        StoreEntityValidation.requireEntity(entity);

        Object contentId = BeanUtils.getFieldWithAnnotation(entity, ContentId.class);
        if (contentId == null) {
            Serializable newId = UUID.randomUUID().toString();
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

        setResourceContentTypeFromMimeType(entity, null, resource);

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
    public S setContent(@NonNull S entity, PropertyPath propertyPath, InputStream content) {
        StoreEntityValidation.requireEntity(entity);
        return this.setContent(entity, propertyPath, content, -1L);
    }

    @Transactional
    @Override
    public S setContent(@NonNull S entity, PropertyPath propertyPath, InputStream content, long contentLen) {
        StoreEntityValidation.requireEntity(entity);
        return this.setContent(entity, propertyPath, content,
            new org.springframework.content.commons.store.SetContentParams(contentLen, true, org.springframework.content.commons.store.SetContentParams.ContentDisposition.Overwrite));
    }

    @Override
    public S setContent(@NonNull S entity, PropertyPath propertyPath, InputStream content, org.springframework.content.commons.store.SetContentParams params) {
        StoreEntityValidation.requireEntity(entity);
        ContentProperty property = this.mappingContext.getContentProperty(entity.getClass(), propertyPath.name());
        if (property == null) {
            throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.name()));
        }

        Object contentId = property.getContentId(entity);
        if (contentId == null || params.disposition()
            .equals(org.springframework.content.commons.store.SetContentParams.ContentDisposition.CreateNew)) {

            Serializable newId = UUID.randomUUID().toString();
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

        setResourceContentTypeFromMimeType(entity, property, resource);

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
    public S setContent(@NonNull S property, Resource resourceContent) {
        StoreEntityValidation.requireEntity(property);
        try {
            return setContent(property, resourceContent.getInputStream());
        } catch (IOException e) {
            logger.error(format("Unexpected error setting content for entity %s", property), e);
            throw new StoreAccessException(format("Setting content for entity %s", property), e);
        }
    }

    @Override
    public S setContent(@NonNull S entity, PropertyPath propertyPath, Resource resourceContent) {
        StoreEntityValidation.requireEntity(entity);
        try {
            return setContent(entity, propertyPath, resourceContent.getInputStream());
        } catch (IOException e) {
            logger.error(format("Unexpected error setting content for entity %s", entity), e);
            throw new StoreAccessException(format("Setting content for entity %s", entity), e);
        }
    }

    @Transactional
    @Override
    public InputStream getContent(@NonNull S entity) {
        StoreEntityValidation.requireEntity(entity);
        Object contentId = BeanUtils.getFieldWithAnnotation(entity, ContentId.class);
        if (contentId == null) {
            return null;
        }

        Resource resource = this.getResource(entity);
        return ContentStoreGetContentSupport.openContentStream(
            entity, ContentStoreGetContentSupport.propertyPathLabel(null), contentId, resource, "S3 object");
    }

    @Transactional
    @Override
    public InputStream getContent(@NonNull S entity, PropertyPath propertyPath) {
        StoreEntityValidation.requireEntity(entity);
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
            entity, ContentStoreGetContentSupport.propertyPathLabel(propertyPath), contentId, resource, "S3 object");
    }

    @Transactional
    @Override
    public S unsetContent(@NonNull S entity) {
        StoreEntityValidation.requireEntity(entity);
        deleteIfExists(entity);

        // reset content fields
        BeanUtils.setFieldWithAnnotationConditionally(entity, ContentId.class, null, field -> {
            for (Annotation annotation : field.getAnnotations()) {
                String canonicalName = annotation.annotationType().getCanonicalName();
                if ("jakarta.persistence.Id".equals(canonicalName)
                    || "org.springframework.data.annotation.Id".equals(canonicalName)) {
                    return false;
                }
            }
            return true;
        });
        Class<?> contentLenType = BeanUtils.getFieldWithAnnotationType(entity, ContentLength.class);
        if (contentLenType != null) {
            BeanUtils.setFieldWithAnnotation(entity, ContentLength.class, BeanUtils.getDefaultValueForType(contentLenType));
        }

        return entity;
    }

    @Transactional
    @Override
    public S unsetContent(@NonNull S entity, PropertyPath propertyPath) {
        StoreEntityValidation.requireEntity(entity);
        return this.unsetContent(entity, propertyPath, new org.springframework.content.commons.store.UnsetContentParams(org.springframework.content.commons.store.UnsetContentParams.Disposition.Remove));
    }


    @Transactional
    @Override
    public S unsetContent(@NonNull S entity, PropertyPath propertyPath, org.springframework.content.commons.store.UnsetContentParams params) {
        StoreEntityValidation.requireEntity(entity);
        ContentProperty property = this.mappingContext.getContentProperty(entity.getClass(), propertyPath.name());
        if (property == null) {
            throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.name()));
        }

        Resource resource = this.getResource(entity, propertyPath);

        if (params.disposition().equals(org.springframework.content.commons.store.UnsetContentParams.Disposition.Remove)) {
            deleteIfExists(entity, resource);
        }

        // reset content fields
        property.setContentId(entity, null, descriptor -> {
            for (Annotation annotation : descriptor.getAnnotations()) {
                String canonicalName = annotation.annotationType().getCanonicalName();
                if ("jakarta.persistence.Id".equals(canonicalName)
                    || "org.springframework.data.annotation.Id".equals(canonicalName)) {
                    return false;
                }
            }
            return true;
        });
        property.setContentLength(entity, BeanUtils.getDefaultValueForType(property.getContentLengthType().getType()));

        return entity;
    }

    private static String makeAbsolute(String bucket, String location) {
        Assert.state(!location.startsWith("s3://"), "only s3:// supported");
        String locationToUse = location.startsWith("/") ? location.substring(1) : location;
        return format("s3://%s/%s", bucket, locationToUse);
    }

    private void deleteIfExists(S entity) {
        Resource resource = this.getResource(entity);
        if (resource != null && resource.exists() && resource instanceof DeletableResource) {

            try {
                ((DeletableResource) resource).delete();
            } catch (Exception e) {
                logger.error(format("Unexpected error unsetting content for entity %s", entity));
                throw new StoreAccessException(format("Unsetting content for entity %s", entity), e);
            }
        }
    }

    private void deleteIfExists(S entity, Resource resource) {
        if (resource != null && resource.exists() && resource instanceof DeletableResource) {
            try {
                ((DeletableResource) resource).delete();
            } catch (Exception e) {
                logger.error(format("Unexpected error unsetting content for entity %s", entity));
                throw new StoreAccessException(format("Unsetting content for entity %s", entity), e);
            }
        }
    }
}
