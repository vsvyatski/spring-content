package org.springframework.content.test.support.azure;

import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.test.support.SingleContentFields;

/**
 * {@link ContentStore} marker for Azure enable-store configuration tests.
 *
 * @author Vladimir Svyatski
 */
public interface SingleContentAzureStore extends ContentStore<SingleContentFields, String> {
}
