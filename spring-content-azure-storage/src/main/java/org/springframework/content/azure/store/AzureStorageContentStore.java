package org.springframework.content.azure.store;

import org.springframework.content.commons.store.ContentStore;

import java.io.Serializable;

public interface AzureStorageContentStore<E, CID extends Serializable> extends ContentStore<E, CID> {
}
