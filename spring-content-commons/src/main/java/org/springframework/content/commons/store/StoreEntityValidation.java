package org.springframework.content.commons.store;

import org.springframework.util.Assert;

/**
 * Validates content-store entity arguments at API boundaries.
 *
 * @author Vladimir Svyatski
 */
public final class StoreEntityValidation {

    public static final String ENTITY_MUST_NOT_BE_NULL = "entity must not be null";

    private StoreEntityValidation() {
    }

    public static <S> S requireEntity(S entity) {
        Assert.notNull(entity, ENTITY_MUST_NOT_BE_NULL);
        return entity;
    }
}
