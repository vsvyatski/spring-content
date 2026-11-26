package org.springframework.content.commons.store;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
}
