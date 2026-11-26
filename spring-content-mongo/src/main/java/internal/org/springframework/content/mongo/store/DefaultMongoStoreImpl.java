package internal.org.springframework.content.mongo.store;

import internal.org.springframework.content.mongo.io.GridFsStoreResource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bson.types.ObjectId;
import org.jspecify.annotations.NonNull;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
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
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.io.Resource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.UUID;

import static java.lang.String.format;
import static org.springframework.data.mongodb.core.query.Query.query;
import static org.springframework.data.mongodb.gridfs.GridFsCriteria.whereFilename;

public class DefaultMongoStoreImpl<S, SID extends Serializable>
    implements ContentStore<S, SID> {

    private static final Log logger = LogFactory.getLog(DefaultMongoStoreImpl.class);

    private final GridFsTemplate gridFs;
    private final PlacementService placer;

    private MappingContext mappingContext;

    public DefaultMongoStoreImpl(GridFsTemplate template, MappingContext mappingContext, PlacementService placer) {
        Assert.notNull(template, "template cannot be null");
        Assert.notNull(placer, "placer cannot be null");

        this.gridFs = template;
        this.placer = placer;

        this.mappingContext = mappingContext;
        if (this.mappingContext == null) {
            this.mappingContext = new MappingContext("/", ".");
        }
    }

    @Override
    public Resource getResource(SID id) {
        if (id == null) {
            return null;
        }

        String location = placer.convert(id, String.class);
        assert location != null;
        return new GridFsStoreResource(location, gridFs);
    }

    @Override
    public Resource getResource(@NonNull S entity) {
        StoreEntityValidation.requireEntity(entity);

        ObjectId objectId;
        if (placer.canConvert(entity.getClass(), ObjectId.class)) {
            objectId = placer.convert(entity, ObjectId.class);

            if (objectId != null) {
                String location = placer.convert(objectId, String.class);
                assert location != null;
                return new GridFsStoreResource(location, gridFs);
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

        if (placer.canConvert(entity.getClass(), ObjectId.class)) {
            ObjectId objectId = placer.convert(entity, ObjectId.class);

            if (objectId != null) {
                String location = placer.convert(objectId, String.class);
                assert location != null;
                return new GridFsStoreResource(location, gridFs);
            }
        }

        ContentProperty property = this.mappingContext.getContentProperty(entity.getClass(), propertyPath.name());
        if (property == null) {
            throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.name()));
        }

        @SuppressWarnings("unchecked")
        SID contentId = (SID) property.getContentId(entity);
        if (contentId == null) {
            return null;
        }
        return this.getResource(contentId);
    }

    @Override
    public void associate(@NonNull S entity, SID id) {
        StoreEntityValidation.requireEntity(entity);
        Object convertedId = convertToExternalContentIdType(entity, id);
        BeanUtils.setFieldWithAnnotation(entity, ContentId.class, convertedId.toString());
    }

    @Override
    public void associate(@NonNull S entity, PropertyPath propertyPath, SID id) {
        StoreEntityValidation.requireEntity(entity);

        ContentProperty property = this.mappingContext.getContentProperty(entity.getClass(), propertyPath.name());
        if (property == null) {
            throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.name()));
        }

        Object convertedId = convertToExternalContentIdType(id, property.getContentIdType(entity));
        property.setContentId(entity, convertedId, null);
    }

    @Override
    public void unassociate(@NonNull S entity) {
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

    @Override
    @Transactional
    public S setContent(@NonNull S entity, InputStream content) {
        StoreEntityValidation.requireEntity(entity);
        Object contentId = BeanUtils.getFieldWithAnnotation(entity, ContentId.class);
        if (contentId == null) {
            Serializable newId = UUID.randomUUID().toString();
            Object convertedId = convertToExternalContentIdType(entity, newId);
            BeanUtils.setFieldWithAnnotation(entity, ContentId.class, convertedId);
        }

        Resource resource = this.getResource(entity);
        if (resource == null) {
            return entity;
        }

        String filename = gridFsFilename(resource);
        if (resource.exists()) {
            gridFs.delete(query(whereFilename().is(filename)));
        }

        try {
            gridFs.store(content, filename);
            resource = gridFs.getResource(filename);
        } catch (Exception e) {
            logger.error(format("Unexpected error setting content for entity  %s", entity), e);
            throw new StoreAccessException(format("Setting content for entity %s", entity), e);
        }

        long contentLen = 0L;
        try {
            contentLen = resource.contentLength();
        } catch (IOException ioe) {
            logger.debug(format("Unable to retrieve content length for %s", contentId));
        }
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
            Object convertedId = convertToExternalContentIdType(newId, property.getContentIdType(entity));
            property.setContentId(entity, convertedId, null);
        }

        Resource resource = this.getResource(entity, propertyPath);
        if (resource == null) {
            return entity;
        }

        String filename = gridFsFilename(resource);
        if (resource.exists()) {
            gridFs.delete(query(whereFilename().is(filename)));
        }

        try {
            gridFs.store(content, filename);
            resource = gridFs.getResource(filename);
        } catch (Exception e) {
            logger.error(format("Unexpected error setting content for entity  %s", entity), e);
            throw new StoreAccessException(format("Setting content for entity %s", entity), e);
        }

        try {
            long len = params.contentLength();
            if (len == -1L) {
                len = resource.contentLength();
            }
            property.setContentLength(entity, len);
        } catch (IOException ioe) {
            logger.debug(format("Unable to retrieve content length for %s", contentId));
        }

        return entity;
    }

    @Override
    @Transactional
    public S setContent(@NonNull S property, Resource resourceContent) {
        StoreEntityValidation.requireEntity(property);
        try {
            return setContent(property, resourceContent.getInputStream());
        } catch (IOException e) {
            logger.error(format("Unexpected error setting content for entity  %s", property), e);
            throw new StoreAccessException(format("Setting content for entity %s", property), e);
        }
    }

    @Transactional
    @Override
    public S setContent(@NonNull S entity, PropertyPath propertyPath, Resource resourceContent) {
        StoreEntityValidation.requireEntity(entity);
        try {
            return setContent(entity, propertyPath, resourceContent.getInputStream());
        } catch (IOException e) {
            logger.error(format("Unexpected error setting content for entity  %s", entity), e);
            throw new StoreAccessException(format("Setting content for entity %s", entity), e);
        }
    }

    @Override
    @Transactional
    public InputStream getContent(@NonNull S entity) {
        StoreEntityValidation.requireEntity(entity);
        Object contentId = BeanUtils.getFieldWithAnnotation(entity, ContentId.class);
        if (contentId == null)
            return null;

        String location = placer.convert(contentId, String.class);
        assert location != null;
        Resource resource = gridFs.getResource(location);
        return ContentStoreGetContentSupport.openContentStream(
            entity, ContentStoreGetContentSupport.propertyPathLabel(null), contentId, resource, "GridFS resource");
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

        String location = placer.convert(contentId, String.class);
        assert location != null;
        Resource resource = gridFs.getResource(location);
        return ContentStoreGetContentSupport.openContentStream(
            entity, ContentStoreGetContentSupport.propertyPathLabel(propertyPath), contentId, resource, "GridFS resource");
    }

    @Override
    @Transactional
    public S unsetContent(@NonNull S property) {
        StoreEntityValidation.requireEntity(property);
        Object contentId = BeanUtils.getFieldWithAnnotation(property, ContentId.class);
        if (contentId == null)
            return property;

        try {
            String location = placer.convert(contentId, String.class);
            assert location != null;
            Resource resource = gridFs.getResource(location);
            if (resource.exists()) {
                gridFs.delete(query(whereFilename().is(gridFsFilename(resource))));

                // reset content fields
                BeanUtils.setFieldWithAnnotationConditionally(property, ContentId.class, null, field -> {
                    for (Annotation annotation : field.getAnnotations()) {
                        String canonicalName = annotation.annotationType().getCanonicalName();
                        if ("jakarta.persistence.Id".equals(canonicalName)
                            || "org.springframework.data.annotation.Id".equals(canonicalName)) {
                            return false;
                        }
                    }
                    return true;
                });
                Class<?> contentLenType = BeanUtils.getFieldWithAnnotationType(property, ContentLength.class);
                if (contentLenType != null) {
                    BeanUtils.setFieldWithAnnotation(property, ContentLength.class, BeanUtils.getDefaultValueForType(contentLenType));
                }
            }
        } catch (Exception ase) {
            logger.error(format("Unexpected error unsetting content for entity %s", property), ase);
            throw new StoreAccessException(format("Unsetting content for entity %s", property), ase);
        }

        return property;
    }

    @Override
    public S unsetContent(@NonNull S entity, PropertyPath propertyPath) {
        StoreEntityValidation.requireEntity(entity);
        return this.unsetContent(entity, propertyPath, new org.springframework.content.commons.store.UnsetContentParams(org.springframework.content.commons.store.UnsetContentParams.Disposition.Remove));
    }

    @Override
    public S unsetContent(@NonNull S entity, PropertyPath propertyPath, org.springframework.content.commons.store.UnsetContentParams params) {
        StoreEntityValidation.requireEntity(entity);
        ContentProperty property = this.mappingContext.getContentProperty(entity.getClass(), propertyPath.name());
        if (property == null) {
            throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.name()));
        }

        Object contentId = property.getContentId(entity);
        if (contentId == null)
            return entity;

        try {
            String location = placer.convert(contentId, String.class);
            assert location != null;
            Resource resource = gridFs.getResource(location);
            if (resource.exists() && params.disposition().equals(org.springframework.content.commons.store.UnsetContentParams.Disposition.Remove)) {
                gridFs.delete(query(whereFilename().is(gridFsFilename(resource))));
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
        } catch (Exception ase) {
            logger.error(format("Unexpected error unsetting content for entity %s", entity), ase);
            throw new StoreAccessException(format("Unsetting content for entity %s", entity), ase);
        }

        return entity;
    }

    private static String gridFsFilename(Resource resource) {
        String filename = resource.getFilename();
        assert filename != null;
        return filename;
    }

    protected Object convertToExternalContentIdType(S property, Object contentId) {
        if (placer.canConvert(TypeDescriptor.forObject(contentId),
            TypeDescriptor.valueOf(BeanUtils.getFieldWithAnnotationType(property, ContentId.class)))) {
            contentId = placer.convert(contentId, TypeDescriptor.forObject(contentId),
                TypeDescriptor.valueOf(BeanUtils.getFieldWithAnnotationType(property,
                    ContentId.class)));
            return contentId;
        }
        return contentId.toString();
    }

    private Object convertToExternalContentIdType(Object contentId, TypeDescriptor contentIdType) {
        if (placer.canConvert(TypeDescriptor.forObject(contentId), contentIdType)) {
            contentId = placer.convert(contentId, TypeDescriptor.forObject(contentId),
                contentIdType);
            return contentId;
        }
        return contentId.toString();
    }
}
