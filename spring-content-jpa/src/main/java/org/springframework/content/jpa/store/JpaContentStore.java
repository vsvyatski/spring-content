package org.springframework.content.jpa.store;

import org.springframework.content.commons.store.ContentStore;

import java.io.Serializable;

public interface JpaContentStore<T, CID extends Serializable> extends ContentStore<T, CID> {
}
