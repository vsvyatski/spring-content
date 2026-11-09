package org.springframework.content.fs.store;

import org.springframework.content.commons.store.ContentStore;

import java.io.Serializable;

public interface FileSystemContentStore<I, CID extends Serializable> extends ContentStore<I, CID> {
}
