package internal.org.springframework.content.fs.store;

import com.github.f4b6a3.uuid.UuidCreator;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.io.DeletableResource;
import org.springframework.content.commons.mappingcontext.ContentProperty;
import org.springframework.content.commons.mappingcontext.MappingContext;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.repository.AssociativeStore;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.store.GetResourceParams;
import org.springframework.content.commons.store.SetContentParams;
import org.springframework.content.commons.store.StoreAccessException;
import org.springframework.content.commons.store.UnsetContentParams;
import org.springframework.content.commons.store.UnsetContentParams.Disposition;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.content.commons.utils.FileService;
import org.springframework.content.commons.utils.PlacementService;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.io.*;
import java.lang.annotation.Annotation;
import java.util.UUID;

import static java.lang.String.format;

@Transactional(readOnly = true)
public class DefaultFileSystemStoreImpl<S, SID extends Serializable>
        implements Store<SID>, AssociativeStore<S, SID>, ContentStore<S, SID>,
        org.springframework.content.commons.store.ContentStore<S, SID> {

    private static final Log logger = LogFactory.getLog(DefaultFileSystemStoreImpl.class);

    private final FileSystemResourceLoader loader;
    private final PlacementService placer;
    private final FileService fileService;
    private MappingContext mappingContext;

    public DefaultFileSystemStoreImpl(FileSystemResourceLoader loader, MappingContext mappingContext,
                                      PlacementService conversion, FileService fileService) {
        this.loader = loader;
        this.placer = conversion;
        this.fileService = fileService;
        this.mappingContext = mappingContext;
        if (this.mappingContext == null) {
            this.mappingContext = new MappingContext("/", ".");
        }
    }

    @Override
    public Resource getResource(SID id) {
        String location = placer.convert(id, String.class);
        assert location != null;
        return loader.getResource(location);
    }

    @Override
    public Resource getResource(S entity) {
        if (placer.canConvert(entity.getClass(), String.class)) {
            String location = placer.convert(entity, String.class);
            assert location != null;
            return loader.getResource(location);
        }

        SID contentId = (SID) BeanUtils.getFieldWithAnnotation(entity, ContentId.class);
        if (contentId != null) {
            return getResource(contentId);
        }

        return null;
    }

    @Override
    public Resource getResource(S entity, PropertyPath propertyPath) {
        return this.getResource(entity, propertyPath, new GetResourceParams(null));
    }

    @Override
    public Resource getResource(S entity, PropertyPath propertyPath, GetResourceParams params) {
        ContentProperty contentProperty = this.mappingContext.getContentProperty(entity.getClass(), propertyPath.getName());
        if (contentProperty == null) {
            throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.getName()));
        }

        SID contentId = (SID) contentProperty.getContentId(entity);
        if (contentId == null) {
            return null;
        }
        return getResource(contentId);
    }

    @Override
    public Resource getResource(S entity, PropertyPath propertyPath,
                                org.springframework.content.commons.repository.GetResourceParams params) {
        ContentProperty contentProperty = this.mappingContext
                .getContentProperty(entity.getClass(), propertyPath.getName());
        if (contentProperty == null) {
            throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.getName()));
        }

        SID contentId = (SID) contentProperty.getContentId(entity);
        if (contentId == null) {
            return null;
        }
        return getResource(contentId);
    }

    @Override
    public void associate(S entity, SID id) {
        BeanUtils.setFieldWithAnnotation(entity, ContentId.class, id.toString());
    }

    @Override
    public void associate(S entity, PropertyPath propertyPath, SID id) {
        setContentId(entity, propertyPath, id, null);
    }

    @Override
    public void unassociate(S entity) {
        BeanUtils.setFieldWithAnnotationConditionally(entity, ContentId.class, null,
                field -> {
                    for (Annotation annotation : field.getAnnotations()) {
                        String canonicalName = annotation.annotationType().getCanonicalName();
                        if ("org.springframework.data.annotation.Id".equals(canonicalName)
                                || "jakarta.persistence.Id".equals(canonicalName)) {
                            return false;
                        }
                    }
                    return true;
                });
    }

    @Override
    public void unassociate(S entity, PropertyPath propertyPath) {
        setContentId(entity, propertyPath, null, descriptor -> {
            for (Annotation annotation : descriptor.getAnnotations()) {
                String canonicalName = annotation.annotationType().getCanonicalName();
                if ("org.springframework.data.annotation.Id".equals(canonicalName)
                        || "jakarta.persistence.Id".equals(canonicalName)) {
                    return false;
                }
            }
            return true;
        });
    }

    @Override
    @Transactional
    public S setContent(S entity, InputStream content) {
        Object contentId = BeanUtils.getFieldWithAnnotation(entity, ContentId.class);
        if (contentId == null) {

            Serializable newId = UuidCreator.getTimeOrdered().toString();

            Object convertedId = convertToExternalContentIdType(entity, newId);

            BeanUtils.setFieldWithAnnotation(entity, ContentId.class, convertedId);
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
            BeanUtils.setFieldWithAnnotation(entity, ContentLength.class,
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
        return this.setContent(property, propertyPath, content, -1L);
    }

    @Transactional
    @Override
    public S setContent(S property, PropertyPath propertyPath, InputStream content, long contentLen) {
        return this.setContent(property, propertyPath, content, new SetContentParams(contentLen, true, SetContentParams.ContentDisposition.Overwrite));
    }

    @Override
    public S setContent(S entity, PropertyPath propertyPath, InputStream content, org.springframework.content.commons.repository.SetContentParams params) {
        int ordinal = params.disposition().ordinal();
        SetContentParams params1 = new SetContentParams(
                params.contentLength(),
                params.overwriteExistingContent(),
                org.springframework.content.commons.store.SetContentParams.ContentDisposition.values()[ordinal]);
        return this.setContent(entity, propertyPath, content, params1);
    }

    @Transactional
    @Override
    public S setContent(S property, PropertyPath propertyPath, InputStream content, SetContentParams params) {

        ContentProperty contentProperty = this.mappingContext
                .getContentProperty(property.getClass(), propertyPath.getName());
        if (contentProperty == null) {
            throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.getName()));
        }

        Object contentId = contentProperty.getContentId(property);
        if (contentId == null || params.disposition()
                .equals(org.springframework.content.commons.store.SetContentParams.ContentDisposition.CreateNew)) {

            Serializable newId = UuidCreator.getTimeOrdered().toString();

            Object convertedId = placer.convert(
                    newId,
                    TypeDescriptor.forObject(newId),
                    contentProperty.getContentIdType(property));

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
            logger.error(format(
                    "Unexpected error setting content length for content for resource %s",
                    resource), e);
        }

        return property;
    }

    @Transactional
    @Override
    public S setContent(S property, Resource resourceContent) {
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
        try {
            return this.setContent(property, propertyPath, resourceContent.getInputStream());
        } catch (IOException e) {
            logger.error(format("Unexpected error setting content for entity %s", property), e);
            throw new StoreAccessException(format("Setting content for entity %s", property), e);
        }
    }

    @Override
    @Transactional
    public InputStream getContent(S entity) {
        if (entity == null)
            return null;

        Resource resource = getResource(entity);

        try {
            if (resource != null && resource.exists()) {
                return resource.getInputStream();
            }
        } catch (IOException e) {
            logger.error(format("Unexpected error getting content for entity %s", entity), e);
            throw new StoreAccessException(format("Getting content for entity %s", entity), e);
        }

        return null;
    }

    @Transactional
    @Override
    public InputStream getContent(S property, PropertyPath propertyPath) {
        if (property == null)
            return null;

        Resource resource = getResource(property, propertyPath);

        try {
            if (resource != null && resource.exists()) {
                return resource.getInputStream();
            }
        } catch (IOException e) {
            logger.error(format("Unexpected error getting content for entity %s", property), e);
            throw new StoreAccessException(format("Getting content for entity %s", property), e);
        }

        return null;
    }

    @Override
    @Transactional
    public S unsetContent(S entity) {
        if (entity == null)
            return null;

        Resource resource = getResource(entity);

        if (resource != null && resource.exists() && resource instanceof DeletableResource) {
            try {
                ((DeletableResource) resource).delete();
            } catch (IOException e) {
                logger.warn(format("Unable to get file for resource %s", resource));
            }
        }

        // reset content fields
        unassociate(entity);

        Class<?> contentLenType = BeanUtils.getFieldWithAnnotationType(entity, ContentLength.class);
        if (contentLenType != null) {
            BeanUtils.setFieldWithAnnotation(entity, ContentLength.class,
                    BeanUtils.getDefaultValueForType(contentLenType));
        }

        return entity;
    }

    @Transactional
    @Override
    public S unsetContent(S entity, PropertyPath propertyPath) {
        return unsetContent(entity, propertyPath, new UnsetContentParams(Disposition.Remove));
    }

    @Transactional
    @Override
    public S unsetContent(S entity, PropertyPath propertyPath,
                          org.springframework.content.commons.repository.UnsetContentParams params) {
        int ordinal = params.disposition().ordinal();
        return unsetContent(entity, propertyPath, new UnsetContentParams(Disposition.values()[ordinal]));
    }

    @Transactional
    @Override
    public S unsetContent(S entity, PropertyPath propertyPath, UnsetContentParams params) {
        ContentProperty property = this.mappingContext.getContentProperty(entity.getClass(), propertyPath.getName());
        if (property == null) {
            throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.getName()));
        }

        Resource resource = getResource(entity, propertyPath);

        if (resource != null && resource.exists() && resource instanceof DeletableResource && params.disposition().equals(Disposition.Remove)) {
            try {
                ((DeletableResource) resource).delete();
            } catch (IOException e) {
                logger.warn(format("Unable to get file for resource %s", resource));
            }
        }

        // reset content fields
        if (resource != null) {
            unassociate(entity, propertyPath);

            property.setContentLength(entity,
                    BeanUtils.getDefaultValueForType(property.getContentLengthType().getType()));
        }
        return entity;
    }

    private Object convertToExternalContentIdType(S property, Object contentId) {
        if (placer.canConvert(TypeDescriptor.forObject(contentId),
                TypeDescriptor.valueOf(BeanUtils.getFieldWithAnnotationType(property,
                        ContentId.class)))) {
            contentId = placer.convert(contentId, TypeDescriptor.forObject(contentId),
                    TypeDescriptor.valueOf(BeanUtils.getFieldWithAnnotationType(property, ContentId.class)));
            return contentId;
        }
        return contentId.toString();
    }

    private void setContentId(S entity, PropertyPath propertyPath, SID contentId,
                              org.springframework.content.commons.mappingcontext.Condition condition) {
        Assert.notNull(entity, "entity must not be null");
        Assert.notNull(propertyPath, "propertyPath must not be null");

        ContentProperty property = this.mappingContext.getContentProperty(entity.getClass(), propertyPath.getName());
        if (property == null) {
            throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.getName()));
        }
        property.setContentId(entity, contentId, condition);
    }
}
