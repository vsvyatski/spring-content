package internal.org.springframework.content.fragments;

import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.fragments.ContentStoreAware;
import org.springframework.content.commons.mappingcontext.ContentProperty;
import org.springframework.content.commons.mappingcontext.MappingContext;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.commons.store.GetResourceParams;
import org.springframework.content.commons.store.SetContentParams;
import org.springframework.content.commons.store.Store;
import org.springframework.content.commons.store.UnsetContentParams;
import org.springframework.content.commons.store.StoreAccessException;
import org.springframework.content.commons.store.StoreEntityValidation;
import org.springframework.content.encryption.config.EncryptingContentStoreConfigurer;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class EncryptingContentStoreImpl<S, SID extends Serializable> implements
    org.springframework.content.commons.store.ContentStore<S, SID>, ContentStoreAware {

    private final MappingContext mappingContext;
    private final List<EncryptingContentStoreConfigurer<?>> configurers;
    private ContentCryptoService<S, ?> cryptoService;
    private ContentStore<S, SID> storeDelegate;

    @Autowired
    public EncryptingContentStoreImpl(
        @Autowired(required = false) MappingContext mappingContext,
        List<EncryptingContentStoreConfigurer<?>> configurers
    ) {
        this.mappingContext = Optional.ofNullable(mappingContext).orElseGet(() -> new MappingContext("/", "."));
        this.configurers = configurers;
    }

    @Override
    public S setContent(@NonNull S o, InputStream inputStream) {
        throw new UnsupportedOperationException();
    }

    @Override
    public S setContent(@NonNull S entity, @NonNull PropertyPath propertyPath, InputStream content) {
        StoreEntityValidation.requireEntity(entity);
        StoreEntityValidation.requirePropertyPath(propertyPath);
        return requireCryptoService().encrypt(entity, propertyPath, content,
            (e, s) -> requireStoreDelegate().setContent(e, propertyPath, s));
    }

    @Override
    public S setContent(@NonNull S entity, @NonNull PropertyPath propertyPath, InputStream inputStream, long l) {
        return this.setContent(entity, propertyPath, inputStream,
            new org.springframework.content.commons.store.SetContentParams(l, true, org.springframework.content.commons.store.SetContentParams.ContentDisposition.Overwrite));
    }

    @Override
    public S setContent(@NonNull S entity, @NonNull PropertyPath propertyPath, InputStream content,
            @NonNull SetContentParams params) {
        StoreEntityValidation.requireEntity(entity);
        StoreEntityValidation.requirePropertyPath(propertyPath);
        StoreEntityValidation.requireSetContentParams(params);
        return requireCryptoService().encrypt(entity, propertyPath, content,
            (e, s) -> requireStoreDelegate().setContent(e, propertyPath, s, params));
    }

    @Override
    public S setContent(@NonNull S entity, Resource resource) {
        throw new UnsupportedOperationException();
    }

    @Override
    public S setContent(@NonNull S entity, @NonNull PropertyPath propertyPath, Resource resource) {
        StoreEntityValidation.requireEntity(entity);
        StoreEntityValidation.requirePropertyPath(propertyPath);
        Assert.notNull(resource, "resource must not be null");

        try {
            return requireCryptoService().encrypt(entity, propertyPath, resource.getInputStream(),
                (e, s) -> requireStoreDelegate().setContent(e, propertyPath, new InputStreamResource(s)));
        } catch (IOException e) {
            throw new StoreAccessException("error encrypting resource", e);
        }
    }

    @Override
    public S unsetContent(@NonNull S entity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public S unsetContent(@NonNull S entity, @NonNull PropertyPath propertyPath) {
        return unsetContent(entity, propertyPath,
            new org.springframework.content.commons.store.UnsetContentParams(org.springframework.content.commons.store.UnsetContentParams.Disposition.Remove));
    }

    @Override
    public S unsetContent(@NonNull S entity, @NonNull PropertyPath propertyPath,
            @NonNull UnsetContentParams params) {
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
    public InputStream getContent(@NonNull S entity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public InputStream getContent(@NonNull S entity, @NonNull PropertyPath propertyPath) throws IOException {
        StoreEntityValidation.requireEntity(entity);
        StoreEntityValidation.requirePropertyPath(propertyPath);

        return requireCryptoService()
            .decrypt(entity, propertyPath, null, () -> requireStoreDelegate().getResource(entity, propertyPath))
            .getInputStream();
    }

    @Override
    public Resource getResource(@NonNull S entity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Resource getResource(@NonNull S entity, @NonNull PropertyPath propertyPath) {
        StoreEntityValidation.requireEntity(entity);
        StoreEntityValidation.requirePropertyPath(propertyPath);

        return requireCryptoService()
            .decrypt(entity, propertyPath, null, () -> requireStoreDelegate().getResource(entity, propertyPath));
    }

    @Override
    public Resource getResource(@NonNull S entity, @NonNull PropertyPath propertyPath, @NonNull GetResourceParams params) {
        StoreEntityValidation.requireEntity(entity);
        StoreEntityValidation.requirePropertyPath(propertyPath);
        StoreEntityValidation.requireGetResourceParams(params);

        return requireCryptoService()
            .decrypt(entity, propertyPath, params, () -> requireStoreDelegate().getResource(entity, propertyPath));
    }

    @Override
    public void associate(@NonNull S entity, SID serializable) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void associate(@NonNull S entity, @NonNull PropertyPath propertyPath, SID serializable) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void unassociate(@NonNull S entity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void unassociate(@NonNull S entity, @NonNull PropertyPath propertyPath) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Resource getResource(SID id) {
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
        if (configurers == null) {
            return;
        }

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
