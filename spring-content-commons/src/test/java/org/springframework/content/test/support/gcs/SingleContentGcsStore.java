package org.springframework.content.test.support.gcs;

import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.test.support.SingleContentFields;

/**
 * {@link ContentStore} marker for {@link org.springframework.content.gcs.config.EnableGCPStorage} configuration tests.
 *
 * @author Vladimir Svyatski
 */
public interface SingleContentGcsStore extends ContentStore<SingleContentFields, String> {
}
