package org.springframework.content.s3.store;

import org.springframework.content.commons.store.ContentStore;

import java.io.Serializable;

public interface S3ContentStore<E,CID extends Serializable> extends ContentStore<E,CID> {
}
