package org.springframework.content.test.support.fs;

import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.test.support.SingleContentFields;

/**
 * {@link ContentStore} marker for file-system enable-store tests.
 *
 * @author Vladimir Svyatski
 */
public interface SingleContentFsStore extends ContentStore<SingleContentFields, String> {
}
