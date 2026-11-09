package org.springframework.content.commons.store.factory.testsupport;

import org.springframework.content.commons.store.ContentStore;

import java.io.Serializable;

public interface TestContentStore<S, SID extends Serializable> extends ContentStore<S, SID> {
}
