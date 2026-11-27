package internal.org.springframework.content.mongo.store;

import internal.org.springframework.content.mongo.io.GridFsStoreResource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bson.types.ObjectId;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.mappingcontext.ContentProperty;
import org.springframework.content.commons.mappingcontext.MappingContext;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.*;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.content.commons.utils.ContentStoreGetContentSupport;
import org.springframework.content.commons.utils.DomainObjectUtils;
import org.springframework.content.commons.utils.PlacementService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.io.Resource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.UUID;

import static java.lang.String.format;
import static org.springframework.data.mongodb.core.query.Query.query;
import static org.springframework.data.mongodb.gridfs.GridFsCriteria.whereFilename;

@NullMarked
public class DefaultMongoStoreImpl<S, SID extends Serializable> implements ContentStore<S, SID> {

    private static final Log logger = LogFactory.getLog(DefaultMongoStoreImpl.class);

    private final GridFsTemplate gridFs;
    private final PlacementService placer;

    private final MappingContext mappingContext;

    public DefaultMongoStoreImpl(GridFsTemplate template, @Nullable MappingContext mappingContext,
                                 PlacementService placer) {
        Assert.notNull(template, "template cannot be null");
        Assert.notNull(placer, "placer cannot be null");

        this.gridFs = template;
        this.placer = placer;

        this.mappingContext = mappingContext != null ? mappingContext : new MappingContext("/", ".");
    }

    @Override
    public @Nullable Resource getResource(SID id) {
        StoreEntityValidation.requireContentId(id);

        String location = placer.convert(id, String.class);
        assert location != null;
        return new GridFsStoreResource(location, gridFs);
    }

    @Override
    public @Nullable Resource getResource(S entity) {
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
    public void associate(S entity, SID id) {
        StoreEntityValidation.requireEntity(entity);
        StoreEntityValidation.requireContentId(id);
        Object convertedId = convertToExternalContentIdType(entity, id);
        BeanUtils.setAnnotatedValue(entity, ContentId.class, convertedId.toString());
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

        Object convertedId = convertToExternalContentIdType(id, property.getContentIdType(entity));
        property.setContentId(entity, convertedId, null);
    }

    @Override
    public void unassociate(S entity) {
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

    @Override
    @Transactional
    public S setContent(S entity, InputStream content) {
        StoreEntityValidation.requireEntity(entity);
        StoreEntityValidation.requireContentInputStream(content);
        Object contentId = BeanUtils.getAnnotatedValue(entity, ContentId.class);
        if (contentId == null) {
            Serializable newId = UUID.randomUUID().toString();
            Object convertedId = convertToExternalContentIdType(entity, newId);
            BeanUtils.setAnnotatedValue(entity, ContentId.class, convertedId);
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
        if (contentId == null || params.disposition()
            .equals(SetContentParams.ContentDisposition.CreateNew)) {

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
    public S setContent(S property, Resource resourceContent) {
        StoreEntityValidation.requireEntity(property);
        StoreEntityValidation.requireContentResource(resourceContent);
        try {
            return setContent(property, resourceContent.getInputStream());
        } catch (IOException e) {
            logger.error(format("Unexpected error setting content for entity  %s", property), e);
            throw new StoreAccessException(format("Setting content for entity %s", property), e);
        }
    }

    @Transactional
    @Override
    public S setContent(S entity, PropertyPath propertyPath, Resource resourceContent) {
        StoreEntityValidation.requireEntity(entity);
        StoreEntityValidation.requirePropertyPath(propertyPath);
        StoreEntityValidation.requireContentResource(resourceContent);
        try {
            return setContent(entity, propertyPath, resourceContent.getInputStream());
        } catch (IOException e) {
            logger.error(format("Unexpected error setting content for entity  %s", entity), e);
            throw new StoreAccessException(format("Setting content for entity %s", entity), e);
        }
    }

    @Override
    @Transactional
    public @Nullable InputStream getContent(S entity) {
        StoreEntityValidation.requireEntity(entity);
        Object contentId = BeanUtils.getAnnotatedValue(entity, ContentId.class);
        if (contentId == null) return null;

        String location = placer.convert(contentId, String.class);
        assert location != null;
        Resource resource = gridFs.getResource(location);
        return ContentStoreGetContentSupport.openContentStream(
            entity, ContentStoreGetContentSupport.propertyPathLabel(null), contentId, resource, "GridFS resource");
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

        String location = placer.convert(contentId, String.class);
        assert location != null;
        Resource resource = gridFs.getResource(location);
        return ContentStoreGetContentSupport.openContentStream(
            entity, ContentStoreGetContentSupport.propertyPathLabel(propertyPath), contentId, resource, "GridFS resource");
    }

    @Override
    @Transactional
    public S unsetContent(S property) {
        StoreEntityValidation.requireEntity(property);
        Object contentId = BeanUtils.getAnnotatedValue(property, ContentId.class);
        if (contentId == null) return property;

        try {
            String location = placer.convert(contentId, String.class);
            assert location != null;
            Resource resource = gridFs.getResource(location);
            if (resource.exists()) {
                gridFs.delete(query(whereFilename().is(gridFsFilename(resource))));

                // reset content fields
                BeanUtils.setAnnotatedValueConditionally(property, ContentId.class, null,
                    DomainObjectUtils::contentIdFieldMayBeCleared);
                Class<?> contentLenType = BeanUtils.getAnnotatedFieldType(property, ContentLength.class);
                if (contentLenType != null) {
                    BeanUtils.setAnnotatedValue(property, ContentLength.class, BeanUtils.getDefaultValueForType(contentLenType));
                }
            }
        } catch (Exception ase) {
            logger.error(format("Unexpected error unsetting content for entity %s", property), ase);
            throw new StoreAccessException(format("Unsetting content for entity %s", property), ase);
        }

        return property;
    }

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

        Object contentId = property.getContentId(entity);
        if (contentId == null)
            return entity;

        try {
            String location = placer.convert(contentId, String.class);
            assert location != null;
            Resource resource = gridFs.getResource(location);
            if (resource.exists() && params.disposition().equals(UnsetContentParams.Disposition.Remove)) {
                gridFs.delete(query(whereFilename().is(gridFsFilename(resource))));
            }

            // reset content fields
            property.setContentId(entity, null, DomainObjectUtils::contentIdPropertyMayBeCleared);
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
            TypeDescriptor.valueOf(BeanUtils.getAnnotatedFieldType(property, ContentId.class)))) {
            Object converted = placer.convert(contentId, TypeDescriptor.forObject(contentId),
                TypeDescriptor.valueOf(BeanUtils.getAnnotatedFieldType(property,
                    ContentId.class)));
            assert converted != null;
            return converted;
        }
        return contentId.toString();
    }

    private Object convertToExternalContentIdType(Object contentId, TypeDescriptor contentIdType) {
        if (placer.canConvert(TypeDescriptor.forObject(contentId), contentIdType)) {
            Object converted = placer.convert(contentId, TypeDescriptor.forObject(contentId),
                contentIdType);
            assert converted != null;
            return converted;
        }
        return contentId.toString();
    }
}
