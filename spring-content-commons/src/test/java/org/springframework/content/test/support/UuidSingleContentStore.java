package org.springframework.content.test.support;

import org.springframework.content.commons.store.ContentStore;

import java.io.InputStream;
import java.util.UUID;

/**
 * {@link ContentStore} with {@link UUID} content id and matching store {@code SID} type.
 *
 * @author Vladimir Svyatski
 */
public interface UuidSingleContentStore extends ContentStore<UuidSingleContentFields, UUID> {

    @Override
    UuidSingleContentFields setContent(UuidSingleContentFields property, InputStream content);
}
