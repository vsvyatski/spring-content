package org.springframework.content.commons.store;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.content.commons.property.PropertyPath;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StoreEntityValidationTest {

    @Test
    @DisplayName("requireEntity accepts a non-null entity")
    void acceptsNonNullEntity() {
        Object entity = new Object();
        StoreEntityValidation.requireEntity(entity);
    }

    @Test
    @DisplayName("requireEntity throws IllegalArgumentException when null")
    void rejectsNullEntity() {
        assertRejectsNullArg(StoreEntityValidation::requireEntity, StoreEntityValidation.ENTITY_MUST_NOT_BE_NULL);
    }

    @Test
    @DisplayName("requirePropertyPath accepts a non-null path")
    void acceptsNonNullPropertyPath() {
        PropertyPath path = PropertyPath.from("content");
        StoreEntityValidation.requirePropertyPath(path);
    }

    @Test
    @DisplayName("requirePropertyPath throws when null")
    void rejectsNullPropertyPath() {
        assertRejectsNullArg(StoreEntityValidation::requirePropertyPath,
            StoreEntityValidation.PROPERTY_PATH_MUST_NOT_BE_NULL);
    }

    @Test
    @DisplayName("requireGetResourceParams throws when null")
    void rejectsNullGetResourceParams() {
        assertRejectsNullArg(StoreEntityValidation::requireGetResourceParams,
            StoreEntityValidation.GET_RESOURCE_PARAMS_MUST_NOT_BE_NULL);
    }

    @Test
    @DisplayName("requireSetContentParams throws when null")
    void rejectsNullSetContentParams() {
        assertRejectsNullArg(StoreEntityValidation::requireSetContentParams,
            StoreEntityValidation.SET_CONTENT_PARAMS_MUST_NOT_BE_NULL);
    }

    @Test
    @DisplayName("requireUnsetContentParams throws when null")
    void rejectsNullUnsetContentParams() {
        assertRejectsNullArg(StoreEntityValidation::requireUnsetContentParams,
            StoreEntityValidation.UNSET_CONTENT_PARAMS_MUST_NOT_BE_NULL);
    }

    @Test
    @DisplayName("requireContentId throws when null")
    void rejectsNullContentId() {
        assertRejectsNullArg((String value) -> StoreEntityValidation.requireContentId(value),
            StoreEntityValidation.CONTENT_ID_MUST_NOT_BE_NULL);
    }

    @Test
    @DisplayName("requireContentId accepts a non-null id")
    void acceptsNonNullContentId() {
        StoreEntityValidation.requireContentId("content-1");
    }

    @Test
    @DisplayName("requireContentInputStream throws when null")
    void rejectsNullContentInputStream() {
        assertRejectsNullArg(StoreEntityValidation::requireContentInputStream,
            StoreEntityValidation.CONTENT_MUST_NOT_BE_NULL);
    }

    private static <T> void assertRejectsNullArg(ValidationCall<T> call, String expectedMessage) {
        T value = nullReference();
        assertThatThrownBy(() -> call.validate(value))
            .isInstanceOf(IllegalArgumentException.class).hasMessage(expectedMessage);
    }

    private static <T> @Nullable T nullReference() {
        return null;
    }

    @FunctionalInterface
    private interface ValidationCall<T> {
        void validate(T value);
    }
}
