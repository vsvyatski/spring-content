package org.springframework.content.test.support.factory;

import org.springframework.content.commons.store.ContentStore;

import java.io.Serializable;

@SuppressWarnings("unused") // classpath / store component scan
public interface TestContentStore<S, SID extends Serializable> extends ContentStore<S, SID> {
}
