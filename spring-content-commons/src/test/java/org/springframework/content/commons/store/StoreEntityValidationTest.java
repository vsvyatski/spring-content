package org.springframework.content.commons.store;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.content.commons.property.PropertyPath;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StoreEntityValidationTest {

    @Test
    @DisplayName("requireEntity returns the entity when non-null")
    void returnsEntityWhenPresent() {
        Object entity = new Object();
        assertThat(StoreEntityValidation.requireEntity(entity)).isSameAs(entity);
    }

    @Test
    @DisplayName("requireEntity throws IllegalArgumentException when null")
    void rejectsNullEntity() {
        assertThatThrownBy(() -> StoreEntityValidation.requireEntity(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(StoreEntityValidation.ENTITY_MUST_NOT_BE_NULL);
    }

    @Test
    @DisplayName("requirePropertyPath returns the path when non-null")
    void returnsPropertyPathWhenPresent() {
        PropertyPath path = PropertyPath.from("content");
        assertThat(StoreEntityValidation.requirePropertyPath(path)).isSameAs(path);
    }

    @Test
    @DisplayName("requirePropertyPath throws when null")
    void rejectsNullPropertyPath() {
        assertThatThrownBy(() -> StoreEntityValidation.requirePropertyPath(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(StoreEntityValidation.PROPERTY_PATH_MUST_NOT_BE_NULL);
    }

    @Test
    @DisplayName("requireGetResourceParams throws when null")
    void rejectsNullGetResourceParams() {
        assertThatThrownBy(() -> StoreEntityValidation.requireGetResourceParams(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(StoreEntityValidation.GET_RESOURCE_PARAMS_MUST_NOT_BE_NULL);
    }

    @Test
    @DisplayName("requireSetContentParams throws when null")
    void rejectsNullSetContentParams() {
        assertThatThrownBy(() -> StoreEntityValidation.requireSetContentParams(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(StoreEntityValidation.SET_CONTENT_PARAMS_MUST_NOT_BE_NULL);
    }

    @Test
    @DisplayName("requireUnsetContentParams throws when null")
    void rejectsNullUnsetContentParams() {
        assertThatThrownBy(() -> StoreEntityValidation.requireUnsetContentParams(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(StoreEntityValidation.UNSET_CONTENT_PARAMS_MUST_NOT_BE_NULL);
    }
}
