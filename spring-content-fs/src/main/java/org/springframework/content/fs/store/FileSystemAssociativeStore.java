package org.springframework.content.fs.store;

import java.io.Serializable;

import org.springframework.content.commons.store.AssociativeStore;

public interface FileSystemAssociativeStore<I, CID extends Serializable> extends AssociativeStore<I, CID> {
}
