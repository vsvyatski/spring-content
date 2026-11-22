package org.springframework.content.test.support.factory;

import org.springframework.content.commons.store.ContentStore;

import java.io.Serializable;

/**
 * Store marker scanned by {@link EnableTestStores} ({@link TestStoresRegistrar#getSignatureTypes()}).
 * Concrete test store interfaces (e.g. in {@code it.StoresIT}) extend this type.
 *
 * @author Vladimir Svyatski
 */
public interface TestContentStore<S, SID extends Serializable> extends ContentStore<S, SID> {
}
