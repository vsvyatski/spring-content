package org.springframework.content.commons.fragments;

import org.springframework.content.commons.store.ContentStore;

import java.io.Serializable;

/**
 * Fragment wiring hooks: {@link org.springframework.content.commons.config.AbstractStoreBeanDefinitionRegistrar}
 * sets domain and ID types; the store method interceptor calls {@link #setContentStore(ContentStore)} before dispatch.
 */
public interface ContentStoreAware {

    void setDomainClass(Class<?> domainClass);

    void setIdClass(Class<?> idClass);

    void setContentStore(ContentStore<Object, Serializable> store);
}
