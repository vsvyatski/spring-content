package internal.org.springframework.content.fs.store;

import com.github.f4b6a3.uuid.UuidCreator;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.io.DeletableResource;
import org.springframework.content.commons.mappingcontext.Condition;
import org.springframework.content.commons.mappingcontext.ContentProperty;
import org.springframework.content.commons.mappingcontext.MappingContext;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.*;
import org.springframework.content.commons.store.UnsetContentParams.Disposition;
import org.springframework.content.commons.utils.*;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.io.*;

import static java.lang.String.format;

@NullMarked
@Transactional(readOnly = true)
public class DefaultFileSystemStoreImpl<S, SID extends Serializable> implements ContentStore<S, SID> {

    private static final Log logger = LogFactory.getLog(DefaultFileSystemStoreImpl.class);

    private final FileSystemResourceLoader loader;
    private final PlacementService placer;
    private final FileService fileService;
    private final MappingContext mappingContext;

    public DefaultFileSystemStoreImpl(FileSystemResourceLoader loader, @Nullable MappingContext mappingContext,
                                      PlacementService conversion, FileService fileService) {
        this.loader = loader;
        this.placer = conversion;
        this.fileService = fileService;
        this.mappingContext = mappingContext != null ? mappingContext : new MappingContext("/", ".");
    }

    @Override
    public @Nullable Resource getResource(SID id) {
        StoreEntityValidation.requireContentId(id);
        String location = placer.convert(id, String.class);
        assert location != null;
        return loader.getResource(location);
    }

    @Override
    public @Nullable Resource getResource(S entity) {
        StoreEntityValidation.requireEntity(entity);
        if (placer.canConvert(entity.getClass(), String.class)) {
            String location = placer.convert(entity, String.class);
            assert location != null;
            return loader.getResource(location);
        }

        try {
            @SuppressWarnings("unchecked")
            SID contentId = (SID) BeanUtils.getAnnotatedValue(entity, ContentId.class);
            if (contentId != null) return getResource(contentId);
        } catch (ClassCastException e) {
            logger.warn("Failed casting a field marked with @ContentId.", e);
        }

        return null;
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
        ContentProperty contentProperty = this.mappingContext.getContentProperty(entity.getClass(), propertyPath.name());
        if (contentProperty == null) {
            throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.name()));
        }

        try {
            @SuppressWarnings("unchecked")
            SID contentId = (SID) contentProperty.getContentId(entity);
            if (contentId != null) return getResource(contentId);
        } catch (ClassCastException e) {
            logger.warn("Failed casting a field marked with @ContentId.", e);
        }

        return null;
    }

    @Override
    public void associate(S entity, SID id) {
        StoreEntityValidation.requireEntity(entity);
        StoreEntityValidation.requireContentId(id);
        BeanUtils.setAnnotatedValue(entity, ContentId.class, id.toString());
    }

    @Override
    public void associate(S entity, PropertyPath propertyPath, SID id) {
        StoreEntityValidation.requireEntity(entity);
        StoreEntityValidation.requirePropertyPath(propertyPath);
        StoreEntityValidation.requireContentId(id);
        setContentId(entity, propertyPath, id, null);
    }

    @Override
    public void unassociate(S entity) {
        StoreEntityValidation.requireEntity(entity);
        BeanUtils.setAnnotatedValueConditionally(entity, ContentId.class, null,
            DomainObjectUtils::contentIdFieldMayBeCleared);
    }

    @Override
    public void unassociate(S entity, PropertyPath propertyPath) {
        StoreEntityValidation.requireEntity(entity);
        StoreEntityValidation.requirePropertyPath(propertyPath);
        setContentId(entity, propertyPath, null, DomainObjectUtils::contentIdPropertyMayBeCleared);
    }

    @Override
    @Transactional
    public S setContent(S entity, InputStream content) {
        StoreEntityValidation.requireEntity(entity);
        StoreEntityValidation.requireContentInputStream(content);
        Object contentId = BeanUtils.getAnnotatedValue(entity, ContentId.class);
        if (contentId == null) {
            Serializable newId = UuidCreator.getTimeOrdered().toString();
            Object convertedId = convertToExternalContentIdType(entity, newId);
            BeanUtils.setAnnotatedValue(entity, ContentId.class, convertedId);
        }

        Resource resource = this.getResource(entity);
        if (resource == null) {
            return entity;
        }

        OutputStream os = null;
        try {
            if (!resource.exists()) {
                File resourceFile = resource.getFile();
                File parent = resourceFile.getParentFile();
                this.fileService.mkdirs(parent);
            }
            if (resource instanceof WritableResource) {
                os = ((WritableResource) resource).getOutputStream();
                IOUtils.copy(content, os);
            }
        } catch (IOException e) {
            logger.error(format("Unexpected io error setting content for entity %s", entity), e);
            throw new StoreAccessException(format("Setting content for entity %s", entity), e);
        } finally {
            IOUtils.closeQuietly(os);
        }

        try {
            BeanUtils.setAnnotatedValue(entity, ContentLength.class,
                resource.contentLength());
        } catch (IOException e) {
            logger.error(format(
                "Unexpected error setting content length for content for resource %s",
                resource), e);
        }

        return entity;
    }

    @Transactional
    @Override
    public S setContent(S property, PropertyPath propertyPath, InputStream content) {
        StoreEntityValidation.requireEntity(property);
        StoreEntityValidation.requirePropertyPath(propertyPath);
        return this.setContent(property, propertyPath, content, -1L);
    }

    @Transactional
    @Override
    public S setContent(S property, PropertyPath propertyPath, InputStream content, long contentLen) {
        StoreEntityValidation.requireEntity(property);
        StoreEntityValidation.requirePropertyPath(propertyPath);
        return this.setContent(property, propertyPath, content, new SetContentParams(contentLen, true, SetContentParams.ContentDisposition.Overwrite));
    }

    @Transactional
    @Override
    public S setContent(S property, PropertyPath propertyPath, InputStream content, SetContentParams params) {
        StoreEntityValidation.requireEntity(property);
        StoreEntityValidation.requirePropertyPath(propertyPath);
        StoreEntityValidation.requireSetContentParams(params);
        StoreEntityValidation.requireContentInputStream(content);
        ContentProperty contentProperty = this.mappingContext
            .getContentProperty(property.getClass(), propertyPath.name());
        if (contentProperty == null) {
            throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.name()));
        }

        Object contentId = contentProperty.getContentId(property);
        if (contentId == null || params.disposition()
            .equals(SetContentParams.ContentDisposition.CreateNew)) {

            Serializable newId = UuidCreator.getTimeOrdered().toString();

            Object convertedId = placer.convert(newId, TypeDescriptor.forObject(newId),
                contentProperty.getContentIdType(property));
            assert convertedId != null;

            contentProperty.setContentId(property, convertedId, null);
        }

        Resource resource = this.getResource(property, propertyPath);
        if (resource == null) {
            return property;
        }

        OutputStream os = null;
        try {
            if (!resource.exists()) {
                File resourceFile = resource.getFile();
                File parent = resourceFile.getParentFile();
                this.fileService.mkdirs(parent);
            }
            if (resource instanceof WritableResource) {
                os = ((WritableResource) resource).getOutputStream();
                IOUtils.copy(content, os);
            }
        } catch (IOException e) {
            logger.error(format("Unexpected io error setting content for entity %s", property), e);
            throw new StoreAccessException(format("Setting content for entity %s", property), e);
        } finally {
            IOUtils.closeQuietly(os);
        }

        try {
            long len = params.contentLength();
            if (len == -1L) {
                len = resource.contentLength();
            }
            contentProperty.setContentLength(property, len);
        } catch (IOException e) {
            logger.error(format("Unexpected error setting content length for content for resource %s", resource), e);
        }

        return property;
    }

    @Transactional
    @Override
    public S setContent(S property, Resource resourceContent) {
        StoreEntityValidation.requireEntity(property);
        StoreEntityValidation.requireContentResource(resourceContent);
        try {
            return this.setContent(property, resourceContent.getInputStream());
        } catch (IOException e) {
            logger.error(format("Unexpected error setting content for entity %s", property), e);
            throw new StoreAccessException(format("Setting content for entity %s", property), e);
        }
    }

    @Transactional
    @Override
    public S setContent(S property, PropertyPath propertyPath, Resource resourceContent) {
        StoreEntityValidation.requireEntity(property);
        StoreEntityValidation.requirePropertyPath(propertyPath);
        StoreEntityValidation.requireContentResource(resourceContent);
        try {
            return this.setContent(property, propertyPath, resourceContent.getInputStream());
        } catch (IOException e) {
            logger.error(format("Unexpected error setting content for entity %s", property), e);
            throw new StoreAccessException(format("Setting content for entity %s", property), e);
        }
    }

    @Override
    @Transactional
    public @Nullable InputStream getContent(S entity) {
        StoreEntityValidation.requireEntity(entity);
        Object contentId = BeanUtils.getAnnotatedValue(entity, ContentId.class);
        if (contentId == null) {
            return null;
        }

        Resource resource = getResource(entity);
        return ContentStoreGetContentSupport.openContentStream(
            entity, ContentStoreGetContentSupport.propertyPathLabel(null), contentId, resource, "file resource");
    }

    @Transactional
    @Override
    public @Nullable InputStream getContent(S property, PropertyPath propertyPath) {
        StoreEntityValidation.requireEntity(property);
        StoreEntityValidation.requirePropertyPath(propertyPath);
        ContentProperty contentProperty =
            this.mappingContext.getContentProperty(property.getClass(), propertyPath.name());
        if (contentProperty == null) {
            throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.name()));
        }
        Object contentId = contentProperty.getContentId(property);
        if (contentId == null) {
            return null;
        }

        Resource resource = getResource(property, propertyPath);
        return ContentStoreGetContentSupport.openContentStream(
            property, ContentStoreGetContentSupport.propertyPathLabel(propertyPath), contentId, resource, "file resource");
    }

    @Override
    @Transactional
    public S unsetContent(S entity) {
        StoreEntityValidation.requireEntity(entity);
        deleteIfExists(entity, null);

        // reset content fields
        unassociate(entity);

        Class<?> contentLenType = BeanUtils.getAnnotatedFieldType(entity, ContentLength.class);
        if (contentLenType != null) {
            BeanUtils.setAnnotatedValue(entity, ContentLength.class,
                BeanUtils.getDefaultValueForType(contentLenType));
        }

        return entity;
    }

    @Transactional
    @Override
    public S unsetContent(S entity, PropertyPath propertyPath) {
        StoreEntityValidation.requireEntity(entity);
        StoreEntityValidation.requirePropertyPath(propertyPath);
        return unsetContent(entity, propertyPath, new UnsetContentParams(Disposition.Remove));
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

        Resource resource = getResource(entity, propertyPath);
        if (resource != null) {
            if (params.disposition().equals(Disposition.Remove)) deleteIfExists(entity, resource);

            // reset content fields
            unassociate(entity, propertyPath);
            property.setContentLength(entity,
                BeanUtils.getDefaultValueForType(property.getContentLengthType().getType()));
        }

        return entity;
    }

    private void deleteIfExists(S entity, @Nullable Resource resource) {
        Resource resourceToDelete = resource != null ? resource : getResource(entity);
        if (resourceToDelete != null && resourceToDelete.exists() && resourceToDelete instanceof DeletableResource) {
            try {
                ((DeletableResource) resourceToDelete).delete();
            } catch (IOException e) {
                logger.warn(format("Unable to get file for resource %s", resourceToDelete));
            }
        }
    }

    private Object convertToExternalContentIdType(S property, Object contentId) {
        if (placer.canConvert(TypeDescriptor.forObject(contentId),
            TypeDescriptor.valueOf(BeanUtils.getAnnotatedFieldType(property, ContentId.class)))) {
            Object converted = placer.convert(contentId, TypeDescriptor.forObject(contentId),
                TypeDescriptor.valueOf(BeanUtils.getAnnotatedFieldType(property, ContentId.class)));
            assert converted != null;
            return converted;
        }
        return contentId.toString();
    }

    private void setContentId(S entity, PropertyPath propertyPath, @Nullable SID contentId,
                              @Nullable Condition condition) {
        StoreEntityValidation.requireEntity(entity);
        Assert.notNull(propertyPath, "propertyPath must not be null");

        ContentProperty property = this.mappingContext.getContentProperty(entity.getClass(), propertyPath.name());
        if (property == null) {
            throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.name()));
        }
        property.setContentId(entity, contentId, condition);
    }
}
