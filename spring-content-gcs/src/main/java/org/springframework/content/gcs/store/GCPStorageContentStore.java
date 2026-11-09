package org.springframework.content.gcs.store;

import org.springframework.content.commons.store.ContentStore;

import java.io.Serializable;

public interface GCPStorageContentStore<E,CID extends Serializable> extends ContentStore<E,CID> {
}
