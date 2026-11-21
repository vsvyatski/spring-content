package org.springframework.content.test.support.azure;

import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.test.support.SingleContentFields;

/**
 * {@link ContentStore} marker for {@link org.springframework.content.azure.config.EnableAzureStorage} configuration
 * tests.
 *
 * @author Vladimir Svyatski
 */
public interface SingleContentAzureStore extends ContentStore<SingleContentFields, String> {
}
