package org.springframework.content.fs.store;

import org.springframework.content.commons.store.Store;

import java.io.Serializable;

public interface FileSystemStore<CID extends Serializable> extends Store<CID> {
}
