package org.springframework.content.commons.storeservice;

import org.springframework.content.commons.store.Store;

import java.io.Serializable;

public interface StoreInfo {
    /**
     * Returns the Store's interface class
     *
     * @return interface class
     */
    Class<? extends Store<? extends Serializable>> getInterface();

    /**
     * Returns the Store's domain object class if applicable. In cases where the Store
     * does not have a domain class, returns null
     *
     * @return domain object class
     */
    Class<?> getDomainObjectClass();

    /**
     * Returns the Store's implementation
     *
     * @param <T>   the type of the implementation class
     * @param clazz the type of the implementation
     * @return T
     * the implementation, or null
     */
    <T> T getImplementation(Class<? extends T> clazz);
}
