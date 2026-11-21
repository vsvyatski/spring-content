package org.springframework.content.test.support.s3;

import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.test.support.SingleContentFields;

/**
 * {@link ContentStore} marker for {@link org.springframework.content.s3.config.EnableS3Stores} configuration tests.
 *
 * @author Vladimir Svyatski
 */
public interface SingleContentS3Store extends ContentStore<SingleContentFields, String> {
}
