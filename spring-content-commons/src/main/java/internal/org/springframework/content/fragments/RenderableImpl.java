package internal.org.springframework.content.fragments;

import internal.org.springframework.content.commons.renditions.RenditionServiceImpl;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.fragments.ContentStoreAware;
import org.springframework.content.commons.mappingcontext.ContentProperty;
import org.springframework.content.commons.mappingcontext.MappingContext;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.renditions.Renderable;
import org.springframework.content.commons.renditions.RenditionProvider;
import org.springframework.content.commons.renditions.RenditionService;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.commons.store.StoreEntityValidation;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * {@link Renderable} store fragment. Wired through {@link ContentStoreAware}; not a {@code @NullMarked}
 * {@link ContentStore} wrapper.
 *
 * @author Vladimir Svyatski
 */
public class RenderableImpl implements Renderable<Object>, ContentStoreAware {

    private static final Log LOGGER = LogFactory.getLog(RenderableImpl.class);

    private final MappingContext mappingContext;
    private final List<RenditionProvider> providers = new ArrayList<>();
    private ContentStore<Object, Serializable> store;
    private RenditionService renditionService;

    public RenderableImpl() {
        this.mappingContext = new MappingContext("/", ".");
    }

    @Autowired(required = false)
    public void setRenditionProviders(RenditionProvider... providers) {
        this.providers.addAll(Arrays.asList(providers));
    }

    @Override
    public void setDomainClass(Class<?> domainClass) {
    }

    @Override
    public void setIdClass(Class<?> idClass) {
    }

    @Override
    public void setContentStore(ContentStore<Object, Serializable> store) {
        this.store = store;
    }

    @Autowired(required = false)
    public void setRenditionService(RenditionService renditionService) {
        this.renditionService = renditionService;
    }

    public RenditionService getRenditionService() {
        if (this.renditionService == null) {
            this.renditionService = new RenditionServiceImpl(providers.toArray(new RenditionProvider[0]));
        }
        return this.renditionService;
    }

    @Override
    public InputStream getRendition(@NonNull Object entity, @NonNull String mimeType) {
        StoreEntityValidation.requireEntity(entity);
        Assert.notNull(mimeType, "mimeType must not be null");

        String fromMimeType = (String) BeanUtils.getAnnotatedValue(entity, MimeType.class);
        if (fromMimeType == null) {
            return null;
        }

        if (!getRenditionService().canConvert(fromMimeType, mimeType)) {
            return null;
        }

        try {
            if (store != null) {
                InputStream content = store.getContent(entity);
                if (content != null) {
                    return getRenditionService().convert(fromMimeType, content, mimeType);
                }
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.error(String.format("Failed to get rendition from %s to %s", fromMimeType, mimeType), e);
        }
        return null;
    }

    @Override
    public InputStream getRendition(@NonNull Object entity, @NonNull PropertyPath propertyPath, @NonNull String mimeType) {
        StoreEntityValidation.requireEntity(entity);
        StoreEntityValidation.requirePropertyPath(propertyPath);
        Assert.notNull(mimeType, "mimeType must not be null");

        ContentProperty property = mappingContext.getContentProperty(entity.getClass(), propertyPath.name());
        if (property == null) {
            return null;
        }

        Object fromMimeType = property.getMimeType(entity);
        if (fromMimeType == null) {
            return null;
        }

        if (!getRenditionService().canConvert(fromMimeType.toString(), mimeType)) {
            return null;
        }

        try {
            if (store != null) {
                Resource resource = store.getResource(entity, propertyPath);
                if (resource != null) {
                    try (InputStream content = resource.getInputStream()) {
                        return getRenditionService().convert(fromMimeType.toString(), content, mimeType);
                    }
                }
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.error(String.format("Failed to get rendition from %s to %s", fromMimeType, mimeType), e);
        }
        return null;
    }

    @Override
    public boolean hasRendition(@NonNull Object entity, @NonNull String mimeType) {
        StoreEntityValidation.requireEntity(entity);
        Assert.notNull(mimeType, "mimeType must not be null");

        String fromMimeType = (String) BeanUtils.getAnnotatedValue(entity, MimeType.class);
        if (fromMimeType == null) {
            return false;
        }

        return getRenditionService().canConvert(fromMimeType, mimeType);
    }

    @Override
    public boolean hasRendition(@NonNull Object entity, @NonNull PropertyPath propertyPath, @NonNull String mimeType) {
        StoreEntityValidation.requireEntity(entity);
        StoreEntityValidation.requirePropertyPath(propertyPath);
        Assert.notNull(mimeType, "mimeType must not be null");

        ContentProperty property = mappingContext.getContentProperty(entity.getClass(), propertyPath.name());
        if (property == null) {
            return false;
        }

        Object fromMimeType = property.getMimeType(entity);
        if (fromMimeType == null) {
            return false;
        }

        return getRenditionService().canConvert(fromMimeType.toString(), mimeType);
    }
}
