package internal.org.springframework.content.fragments;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.StoreEntityValidation;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RenderableImplTest {

    @Test
    @DisplayName("hasRendition returns false when content property is unknown")
    void hasRenditionFalseWhenPropertyMissing() {
        assertThat(new RenderableImpl()
            .hasRendition(new Object(), PropertyPath.from("content"), MediaType.IMAGE_PNG_VALUE))
            .isFalse();
    }

    @Test
    @DisplayName("getRendition requires a non-null entity")
    void getRenditionRequiresEntity() {
        assertThatThrownBy(() -> new RenderableImpl().getRendition(null, MediaType.IMAGE_PNG_VALUE))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(StoreEntityValidation.ENTITY_MUST_NOT_BE_NULL);
    }

    @Test
    @DisplayName("getRendition with property path requires a non-null propertyPath")
    void getRenditionRequiresPropertyPath() {
        assertThatThrownBy(() -> new RenderableImpl().getRendition(new Object(), null, MediaType.IMAGE_PNG_VALUE))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(StoreEntityValidation.PROPERTY_PATH_MUST_NOT_BE_NULL);
    }
}
