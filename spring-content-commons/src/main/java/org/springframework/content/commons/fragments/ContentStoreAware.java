package org.springframework.content.commons.fragments;

import org.springframework.content.commons.store.ContentStore;

import java.io.Serializable;

public interface ContentStoreAware {

    void setDomainClass(Class<?> domainClass);

    void setIdClass(Class<?> idClass);

    void setContentStore(ContentStore<Object, Serializable> store);
}
