package internal.org.springframework.content.s3.store;

import internal.org.springframework.content.commons.utils.ContentPropertyInfoTypeDescriptor;
import internal.org.springframework.content.s3.io.S3StoreResource;
import internal.org.springframework.content.s3.io.SimpleStorageProtocolResolver;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;
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
import java.util.UUID;

import static java.lang.String.format;

@NullMarked
@Transactional
public class DefaultS3StoreImpl<S, SID extends Serializable> implements ContentStore<S, SID> {

    private static final Log logger = LogFactory.getLog(DefaultS3StoreImpl.class);

    private final ResourceLoader loader;
    private final PlacementService placementService;
    private final S3Client client;
    private final @Nullable MultiTenantS3ClientProvider clientProvider;

    private final MappingContext mappingContext;

    public DefaultS3StoreImpl(ApplicationContext context, ResourceLoader loader,
                              @Nullable MappingContext mappingContext,
                              PlacementService placementService, S3Client client,
                              @Nullable MultiTenantS3ClientProvider provider) {
        Assert.notNull(context, "context must be specified");
        Assert.notNull(loader, "loader must be specified");
        Assert.notNull(placementService, "placementService must be specified");
        Assert.notNull(client, "client must be specified");
        this.loader = loader;
        this.placementService = placementService;
        this.client = client;
        this.clientProvider = provider;
        this.mappingContext = mappingContext != null ? mappingContext : new MappingContext("/", ".");
    }

    @Override
    public @Nullable Resource getResource(SID id) {
        StoreEntityValidation.requireContentId(id);

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
    public @Nullable Resource getResource(S entity) {
        StoreEntityValidation.requireEntity(entity);
        if (placementService.canConvert(entity.getClass(), S3ObjectId.class)) {
            S3ObjectId s3ObjectId = placementService.convert(entity, S3ObjectId.class);
            if (s3ObjectId != null) {
                return this.getResourceInternal(s3ObjectId, new GetResourceParams(null));
            }
        }

        @SuppressWarnings("unchecked")
        SID contentId = (SID) BeanUtils.getAnnotatedValue(entity, ContentId.class);
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
    public void associate(Object entity, Serializable id) {
        StoreEntityValidation.requireEntity(entity);
        StoreEntityValidation.requireContentId(id);
        BeanUtils.setAnnotatedValue(entity, ContentId.class, id);
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
    public void unassociate(Object entity) {
        StoreEntityValidation.requireEntity(entity);
        BeanUtils.setAnnotatedValueConditionally(entity, ContentId.class, null, DomainObjectUtils::contentIdFieldMayBeCleared);
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

    private void setResourceContentTypeFromMimeType(S entity, @Nullable ContentProperty property, Resource resource) {
        if (resource instanceof S3StoreResource s3StoreResource) {
            Object mimeType;
            if (property == null) {
                mimeType = BeanUtils.getAnnotatedValue(entity, MimeType.class);
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
    public S setContent(S entity, InputStream content) {
        StoreEntityValidation.requireEntity(entity);
        StoreEntityValidation.requireContentInputStream(content);

        Object contentId = BeanUtils.getAnnotatedValue(entity, ContentId.class);
        if (contentId == null) {
            Serializable newId = UUID.randomUUID().toString();
            Object convertedId = placementService.convert(
                newId,
                TypeDescriptor.forObject(newId),
                TypeDescriptor.valueOf(BeanUtils.getAnnotatedFieldType(entity, ContentId.class)));
            if (convertedId == null) {
                convertedId = newId;
            }

            BeanUtils.setAnnotatedValue(entity, ContentId.class, convertedId);
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
            BeanUtils.setAnnotatedValue(entity, ContentLength.class, resource.contentLength());
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

        Object contentId = property.getContentId(entity);
        if (contentId == null || params.disposition().equals(SetContentParams.ContentDisposition.CreateNew)) {

            Serializable newId = UUID.randomUUID().toString();
            Object convertedId = placementService.convert(
                newId,
                TypeDescriptor.forObject(newId),
                property.getContentIdType(entity));
            if (convertedId == null) {
                convertedId = newId;
            }

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
    public S setContent(S entity, PropertyPath propertyPath, Resource resourceContent) {
        StoreEntityValidation.requireEntity(entity);
        StoreEntityValidation.requirePropertyPath(propertyPath);
        StoreEntityValidation.requireContentResource(resourceContent);
        try {
            return setContent(entity, propertyPath, resourceContent.getInputStream());
        } catch (IOException e) {
            logger.error(format("Unexpected error setting content for entity %s", entity), e);
            throw new StoreAccessException(format("Setting content for entity %s", entity), e);
        }
    }

    @Transactional
    @Override
    public @Nullable InputStream getContent(S entity) {
        StoreEntityValidation.requireEntity(entity);
        Object contentId = BeanUtils.getAnnotatedValue(entity, ContentId.class);
        if (contentId == null) {
            return null;
        }

        Resource resource = this.getResource(entity);
        return ContentStoreGetContentSupport.openContentStream(
            entity, ContentStoreGetContentSupport.propertyPathLabel(null), contentId, resource, "S3 object");
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
            entity, ContentStoreGetContentSupport.propertyPathLabel(propertyPath), contentId, resource, "S3 object");
    }

    @Transactional
    @Override
    public S unsetContent(S entity) {
        StoreEntityValidation.requireEntity(entity);
        deleteIfExists(entity, null);

        // reset content fields
        BeanUtils.setAnnotatedValueConditionally(entity, ContentId.class, null, DomainObjectUtils::contentIdFieldMayBeCleared);
        Class<?> contentLenType = BeanUtils.getAnnotatedFieldType(entity, ContentLength.class);
        if (contentLenType != null) {
            BeanUtils.setAnnotatedValue(entity, ContentLength.class, BeanUtils.getDefaultValueForType(contentLenType));
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
            if (params.disposition().equals(UnsetContentParams.Disposition.Remove)) {
                deleteIfExists(entity, resource);
            }

            // reset content fields
            property.setContentId(entity, null, DomainObjectUtils::contentIdPropertyMayBeCleared);
            property.setContentLength(entity, BeanUtils.getDefaultValueForType(property.getContentLengthType().getType()));
        }

        return entity;
    }

    private static String makeAbsolute(String bucket, String location) {
        Assert.state(!location.startsWith("s3://"), "only s3:// supported");
        String locationToUse = location.startsWith("/") ? location.substring(1) : location;
        return format("s3://%s/%s", bucket, locationToUse);
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
