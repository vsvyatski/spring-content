package org.springframework.content.test.support;

import org.springframework.content.commons.store.AssociativeStore;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.commons.store.Store;

import java.io.Serializable;

import static org.mockito.Mockito.mock;

/**
 * Typed Mockito helpers for spring-content-commons tests.
 *
 * @author Vladimir Svyatski
 */
public final class CommonsTestStores {

    private CommonsTestStores() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static ContentStore<Object, Serializable> mockContentStore() {
        return mock(ContentStore.class);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <S, SID extends Serializable> AssociativeStore<S, SID> mockAssociativeStore() {
        return mock(AssociativeStore.class);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <SID extends Serializable> Store<SID> mockStore() {
        return mock(Store.class);
    }
}