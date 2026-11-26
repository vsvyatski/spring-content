package org.springframework.content.commons.store;

import org.jspecify.annotations.Nullable;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;

import java.io.InputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;

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

    public static final String CONTENT_ID_MUST_NOT_BE_NULL = "id must not be null";

    public static final String CONTENT_MUST_NOT_BE_NULL = "content must not be null";

    private StoreEntityValidation() {
    }

    public static <S> void requireEntity(@Nullable S entity) {
        Assert.notNull(entity, ENTITY_MUST_NOT_BE_NULL);
    }

    public static void requirePropertyPath(@Nullable PropertyPath propertyPath) {
        Assert.notNull(propertyPath, PROPERTY_PATH_MUST_NOT_BE_NULL);
    }

    public static void requireGetResourceParams(@Nullable GetResourceParams params) {
        Assert.notNull(params, GET_RESOURCE_PARAMS_MUST_NOT_BE_NULL);
    }

    public static void requireSetContentParams(@Nullable SetContentParams params) {
        Assert.notNull(params, SET_CONTENT_PARAMS_MUST_NOT_BE_NULL);
    }

    public static void requireUnsetContentParams(@Nullable UnsetContentParams params) {
        Assert.notNull(params, UNSET_CONTENT_PARAMS_MUST_NOT_BE_NULL);
    }

    public static <SID extends Serializable> void requireContentId(@Nullable SID id) {
        Assert.notNull(id, CONTENT_ID_MUST_NOT_BE_NULL);
    }

    public static void requireContentInputStream(@Nullable InputStream content) {
        Assert.notNull(content, CONTENT_MUST_NOT_BE_NULL);
    }

    public static void requireContentBuffer(@Nullable Flux<ByteBuffer> buffer) {
        Assert.notNull(buffer, CONTENT_MUST_NOT_BE_NULL);
    }

    public static void requireContentResource(@Nullable Resource resourceContent) {
        Assert.notNull(resourceContent, CONTENT_MUST_NOT_BE_NULL);
    }
}