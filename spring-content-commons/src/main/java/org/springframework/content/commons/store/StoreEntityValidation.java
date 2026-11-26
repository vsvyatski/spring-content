package org.springframework.content.commons.store;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.util.Assert;

/**
 * Validates content-store entity arguments at API boundaries.
 *
 * @author Vladimir Svyatski
 */
public final class StoreEntityValidation {

    public static final String ENTITY_MUST_NOT_BE_NULL = "entity must not be null";

    public static final String PROPERTY_PATH_MUST_NOT_BE_NULL = "propertyPath must not be null";

    public static final String GET_RESOURCE_PARAMS_MUST_NOT_BE_NULL = "getResourceParams must not be null";

    public static final String SET_CONTENT_PARAMS_MUST_NOT_BE_NULL = "setContentParams must not be null";

    public static final String UNSET_CONTENT_PARAMS_MUST_NOT_BE_NULL = "unsetContentParams must not be null";

    private StoreEntityValidation() {
    }

    public static <S> @NonNull S requireEntity(@Nullable S entity) {
        Assert.notNull(entity, ENTITY_MUST_NOT_BE_NULL);
        return entity;
    }

    public static @NonNull PropertyPath requirePropertyPath(@Nullable PropertyPath propertyPath) {
        Assert.notNull(propertyPath, PROPERTY_PATH_MUST_NOT_BE_NULL);
        return propertyPath;
    }

    public static @NonNull GetResourceParams requireGetResourceParams(@Nullable GetResourceParams params) {
        Assert.notNull(params, GET_RESOURCE_PARAMS_MUST_NOT_BE_NULL);
        return params;
    }

    public static @NonNull SetContentParams requireSetContentParams(@Nullable SetContentParams params) {
        Assert.notNull(params, SET_CONTENT_PARAMS_MUST_NOT_BE_NULL);
        return params;
    }

    public static @NonNull UnsetContentParams requireUnsetContentParams(@Nullable UnsetContentParams params) {
        Assert.notNull(params, UNSET_CONTENT_PARAMS_MUST_NOT_BE_NULL);
        return params;
    }
}
