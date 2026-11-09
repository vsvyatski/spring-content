package org.springframework.content.mongo.store;

import org.springframework.content.commons.store.ContentStore;

import java.io.Serializable;

public interface MongoContentStore<E, CID extends Serializable> extends ContentStore<E, CID> {
}
