package org.springframework.content.test.support.mongo;

import org.springframework.content.commons.store.ContentStore;

/**
 * @author Vladimir Svyatski
 */
public interface DualContentMongoStore extends ContentStore<DualContentMongoDocument, String> {
}