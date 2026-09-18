package org.springframework.content.fs.store;

import java.io.Serializable;

import org.springframework.content.commons.store.Store;

public interface FileSystemStore<CID extends Serializable> extends Store<CID> {
}
