package org.springframework.content.fs.store;

import org.springframework.content.commons.store.AssociativeStore;

import java.io.Serializable;

public interface FileSystemAssociativeStore<I, CID extends Serializable> extends AssociativeStore<I, CID> {
}
