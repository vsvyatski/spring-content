package internal.org.springframework.content.fragments;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.fragments.ContentStoreAware;
import org.springframework.content.commons.mappingcontext.ContentProperty;
import org.springframework.content.commons.mappingcontext.MappingContext;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.*;
import org.springframework.content.encryption.config.EncryptingContentStoreConfigurer;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@NullMarked
public class EncryptingContentStoreImpl<S, SID extends Serializable>
    implements ContentStore<S, SID>, ContentStoreAware {

    private final MappingContext mappingContext;
    private final List<EncryptingContentStoreConfigurer<?>> configurers;
    private @Nullable ContentCryptoService<S, ?> cryptoService;
    private @Nullable ContentStore<S, SID> storeDelegate;

    @Autowired
    public EncryptingContentStoreImpl(
        @Autowired(required = false) @Nullable MappingContext mappingContext,
        @Autowired(required = false) @Nullable List<EncryptingContentStoreConfigurer<?>> configurers) {
        this.mappingContext = mappingContext != null ? mappingContext : new MappingContext("/", ".");
        this.configurers = configurers != null ? configurers : List.of();
    }

    @Override
    public S setContent(S o, InputStream inputStream) {
        throw new UnsupportedOperationException();
    }

    @Override
    public S setContent(S entity, PropertyPath propertyPath, InputStream content) {
        StoreEntityValidation.requireEntity(entity);
        StoreEntityValidation.requirePropertyPath(propertyPath);
        StoreEntityValidation.requireContentInputStream(content);
        return requireCryptoService().encrypt(entity, propertyPath, content,
            (e, s) -> requireStoreDelegate().setContent(e, propertyPath, s));
    }

    @Override
    public S setContent(S entity, PropertyPath propertyPath, InputStream inputStream, long l) {
        return this.setContent(entity, propertyPath, inputStream,
            new SetContentParams(l, true, SetContentParams.ContentDisposition.Overwrite));
    }

    @Override
    public S setContent(S entity, PropertyPath propertyPath, InputStream content, SetContentParams params) {
        StoreEntityValidation.requireEntity(entity);
        StoreEntityValidation.requirePropertyPath(propertyPath);
        StoreEntityValidation.requireSetContentParams(params);
        StoreEntityValidation.requireContentInputStream(content);
        return requireCryptoService().encrypt(entity, propertyPath, content,
            (e, s) -> requireStoreDelegate().setContent(e, propertyPath, s, params));
    }

    @Override
    public S setContent(S entity, Resource resource) {
        throw new UnsupportedOperationException();
    }

    @Override
    public S setContent(S entity, PropertyPath propertyPath, Resource resource) {
        StoreEntityValidation.requireEntity(entity);
        StoreEntityValidation.requirePropertyPath(propertyPath);
        StoreEntityValidation.requireContentResource(resource);

        try {
            return requireCryptoService().encrypt(entity, propertyPath, resource.getInputStream(),
                (e, s) -> requireStoreDelegate().setContent(e, propertyPath, new InputStreamResource(s)));
        } catch (IOException e) {
            throw new StoreAccessException("error encrypting resource", e);
        }
    }

    @Override
    public S unsetContent(S entity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public S unsetContent(S entity, PropertyPath propertyPath) {
        return unsetContent(entity, propertyPath,
            new UnsetContentParams(UnsetContentParams.Disposition.Remove));
    }

    @Override
    public S unsetContent(S entity, PropertyPath propertyPath, UnsetContentParams params) {
        StoreEntityValidation.requireEntity(entity);
        StoreEntityValidation.requirePropertyPath(propertyPath);
        StoreEntityValidation.requireUnsetContentParams(params);

        ContentProperty contentProperty = mappingContext.getContentProperty(entity.getClass(), propertyPath.name());
        if (contentProperty == null) {
            throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.name()));
        }

        var newEntity = requireCryptoService().clearKeys(entity, propertyPath);

        return requireStoreDelegate().unsetContent(newEntity, propertyPath, params);
    }

    @Override
    public @Nullable InputStream getContent(S entity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable InputStream getContent(S entity, PropertyPath propertyPath) throws IOException {
        StoreEntityValidation.requireEntity(entity);
        StoreEntityValidation.requirePropertyPath(propertyPath);

        return requireCryptoService()
            .decrypt(entity, propertyPath, null, () -> requireStoreDelegate().getResource(entity, propertyPath))
            .getInputStream();
    }

    @Override
    public @Nullable Resource getResource(S entity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable Resource getResource(S entity, PropertyPath propertyPath) {
        StoreEntityValidation.requireEntity(entity);
        StoreEntityValidation.requirePropertyPath(propertyPath);

        return requireCryptoService()
            .decrypt(entity, propertyPath, null, () -> requireStoreDelegate().getResource(entity, propertyPath));
    }

    @Override
    public @Nullable Resource getResource(S entity, PropertyPath propertyPath, GetResourceParams params) {
        StoreEntityValidation.requireEntity(entity);
        StoreEntityValidation.requirePropertyPath(propertyPath);
        StoreEntityValidation.requireGetResourceParams(params);

        return requireCryptoService()
            .decrypt(entity, propertyPath, params, () -> requireStoreDelegate().getResource(entity, propertyPath));
    }

    @Override
    public void associate(S entity, SID serializable) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void associate(S entity, PropertyPath propertyPath, SID serializable) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void unassociate(S entity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void unassociate(S entity, PropertyPath propertyPath) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable Resource getResource(SID id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDomainClass(Class<?> domainClass) {
    }

    @Override
    public void setIdClass(Class<?> idClass) {
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setContentStore(ContentStore<Object, Serializable> store) {
        this.storeDelegate = (ContentStore<S, SID>) store;
    }

    /**
     * Bean property {@code storeInterfaceClass} from store fragment registration.
     *
     * @see org.springframework.content.commons.config.AbstractStoreBeanDefinitionRegistrar#STORE_INTERFACE_CLASS_PROPERTY
     */
    @SuppressWarnings("unused")
    public void setStoreInterfaceClass(Class<? extends Store<?>> storeInterfaceClass) {
        configure(storeInterfaceClass);
    }

    private void configure(Class<? extends Store<?>> storeInterfaceClass) {
        EncryptingContentStoreConfigurationImpl<S> config = new EncryptingContentStoreConfigurationImpl<>();
        for (EncryptingContentStoreConfigurer<?> configurer : configurers) {
            Optional<Type> storeConfigurerType = Arrays.stream(configurer.getClass().getGenericInterfaces()).findFirst();
            if (storeConfigurerType.isEmpty()) {
                continue;
            }

            Type[] genericArguments = ((ParameterizedType) storeConfigurerType.get()).getActualTypeArguments();
            if (genericArguments.length < 1) {
                continue;
            }

            if (genericArguments[0].equals(storeInterfaceClass)) {
                applyConfigurer(configurer, config);
            }
        }

        cryptoService = config.initializeCryptoService(mappingContext, storeInterfaceClass);
    }

    @SuppressWarnings("unchecked")
    private static <T> void applyConfigurer(EncryptingContentStoreConfigurer<?> configurer,
                                            EncryptingContentStoreConfigurationImpl<T> config) {
        ((EncryptingContentStoreConfigurer<T>) configurer).configure(config);
    }

    private ContentStore<S, SID> requireStoreDelegate() {
        if (storeDelegate == null) {
            throw new IllegalStateException("store not set");
        }
        return storeDelegate;
    }

    private ContentCryptoService<S, ?> requireCryptoService() {
        if (cryptoService == null) {
            throw new IllegalStateException("cryptoService not set");
        }
        return cryptoService;
    }
}
