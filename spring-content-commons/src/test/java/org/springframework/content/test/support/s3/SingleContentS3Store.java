package org.springframework.content.test.support.s3;

import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.test.support.SingleContentFields;

/**
 * {@link ContentStore} marker for S3 enable-store configuration tests.
 *
 * @author Vladimir Svyatski
 */
public interface SingleContentS3Store extends ContentStore<SingleContentFields, String> {
}
