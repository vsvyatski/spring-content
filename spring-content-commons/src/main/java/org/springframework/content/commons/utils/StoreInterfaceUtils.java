package org.springframework.content.commons.utils;

import org.springframework.content.commons.store.AssociativeStore;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.commons.store.ReactiveContentStore;
import org.springframework.content.commons.store.Store;
import org.springframework.data.core.TypeInformation;
import org.springframework.data.util.Pair;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

public final class StoreInterfaceUtils {
    private StoreInterfaceUtils() {
    }

    public static Pair<Optional<Class<?>>, Class<? extends Serializable>> getStoreTypes(
        Class<?> storeClass) {
        List<TypeInformation<?>> types = null;

        Class<?>[] candidateStoreClasses = new Class<?>[]{
            ContentStore.class,
            AssociativeStore.class,
            Store.class,
            ReactiveContentStore.class
        };
        for (Class<?> candidateStoreClass : candidateStoreClasses) {
            try {
                types = TypeInformation.of(storeClass).getRequiredSuperTypeInformation(candidateStoreClass).getTypeArguments();
                break;
            } catch (IllegalArgumentException ignored) {
            }
        }

        if (types == null) {
            throw new IllegalArgumentException(
                storeClass.getName() + " does not extend a known Spring Content store interface");
        }

        final Optional<Class<?>> domainClass;
        final Class<? extends Serializable> idClass;

        if (types.size() == 2) {
            domainClass = Optional.of(types.get(0).getType());
            idClass = serializableIdClass(types.get(1).getType());
        } else if (types.size() == 1) {
            domainClass = Optional.empty();
            idClass = serializableIdClass(types.get(0).getType());
        } else {
            throw new IllegalArgumentException(
                "Store interface " + storeClass.getName() + " has unexpected type argument count: " + types.size());
        }

        return Pair.of(domainClass, idClass);
    }

    private static Class<? extends Serializable> serializableIdClass(Class<?> type) {
        Assert.isAssignable(Serializable.class, type);
        return type.asSubclass(Serializable.class);
    }
}
