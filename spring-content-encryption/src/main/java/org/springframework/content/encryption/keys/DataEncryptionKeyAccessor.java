package org.springframework.content.encryption.keys;

import org.springframework.content.commons.mappingcontext.ContentProperty;

import java.util.Collection;
import java.util.List;

/**
 * Reads and writes data encryption keys from the entity with content
 */
public interface DataEncryptionKeyAccessor<S, T extends StoredDataEncryptionKey> {

    Collection<T> findKeys(S entity, ContentProperty contentProperty);

    S setKeys(S entity, ContentProperty contentProperty, Collection<T> dataEncryptionKeys);

    default S clearKeys(S entity, ContentProperty contentProperty) {
        return setKeys(entity, contentProperty, List.of());
    }
}
