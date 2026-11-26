package org.springframework.content.test.support;

import org.jspecify.annotations.NullMarked;
import org.springframework.content.commons.store.ContentStore;

import java.io.InputStream;
import java.util.UUID;

/**
 * {@link ContentStore} with {@link UUID} content id and matching store {@code SID} type.
 *
 * @author Vladimir Svyatski
 */
@NullMarked
public interface UuidSingleContentStore extends ContentStore<UuidSingleContentFields, UUID> {

    /**
     * Re-declared so {@code ReflectionUtils.findMethod} can resolve
     * {@code setContent(UuidSingleContentFields, InputStream)} on this interface ({@code StoreMethodInterceptorTest}).
     */
    @Override
    UuidSingleContentFields setContent(UuidSingleContentFields property, InputStream content);
}
