package org.springframework.content.commons.utils;

import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.test.support.SingleContentFields;
import org.springframework.content.test.support.jpa.SharedIdSpringJpaEntity;
import org.springframework.core.convert.TypeDescriptor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Vladimir Svyatski
 */
class DomainObjectUtilsEntityIdTest {

    @Test
    void recognizesEntityIdAnnotationTypes() {
        assertThat(DomainObjectUtils.isEntityIdAnnotationTypeName(DomainObjectUtils.JAKARTA_PERSISTENCE_ID_TYPE))
            .isTrue();
        assertThat(DomainObjectUtils.isEntityIdAnnotationTypeName(DomainObjectUtils.SPRING_DATA_ID_TYPE))
            .isTrue();
        assertThat(DomainObjectUtils.isEntityIdAnnotationTypeName(ContentId.class.getName())).isFalse();
    }

    @Test
    void isEntityIdAnnotationRecognizesIdMarkers() throws NoSuchFieldException {
        assertThat(DomainObjectUtils.isEntityIdAnnotation(
            EntityWithSpringDataId.class.getDeclaredField("id").getAnnotation(org.springframework.data.annotation.Id.class)))
            .isTrue();
        assertThat(DomainObjectUtils.isEntityIdAnnotation(
            EntityWithJakartaId.class.getDeclaredField("id").getAnnotation(jakarta.persistence.Id.class)))
            .isTrue();
        assertThat(DomainObjectUtils.isEntityIdAnnotation(
            EntityWithContentIdOnly.class.getDeclaredField("contentId").getAnnotation(ContentId.class)))
            .isFalse();
    }

    @Test
    void contentIdFieldMayBeClearedSkipsSharedEntityId() throws NoSuchFieldException {
        assertThat(DomainObjectUtils.contentIdFieldMayBeCleared(
            EntityWithSpringDataId.class.getDeclaredField("id"))).isFalse();
        assertThat(DomainObjectUtils.contentIdFieldMayBeCleared(
            EntityWithJakartaId.class.getDeclaredField("id"))).isFalse();
        assertThat(DomainObjectUtils.contentIdFieldMayBeCleared(
            EntityWithContentIdOnly.class.getDeclaredField("contentId"))).isTrue();
    }

    @Test
    void contentIdPropertyMayBeClearedSkipsSharedEntityId() {
        TypeDescriptor shared = new BeanWrapperImpl(new SharedIdSpringJpaEntity())
            .getPropertyTypeDescriptor("contentId");
        TypeDescriptor contentOnly = new BeanWrapperImpl(new SingleContentFields())
            .getPropertyTypeDescriptor("contentId");

        assertThat(shared).isNotNull();
        assertThat(contentOnly).isNotNull();
        assertThat(DomainObjectUtils.contentIdPropertyMayBeCleared(shared)).isFalse();
        assertThat(DomainObjectUtils.contentIdPropertyMayBeCleared(contentOnly)).isTrue();
    }

    @SuppressWarnings("unused") // fields read via reflection for annotation metadata
    static class EntityWithSpringDataId {
        @org.springframework.data.annotation.Id
        @ContentId
        private String id;
    }

    @SuppressWarnings("unused") // fields read via reflection for annotation metadata
    static class EntityWithJakartaId {
        @jakarta.persistence.Id
        @ContentId
        private String id;
    }

    @SuppressWarnings("unused") // fields read via reflection for annotation metadata
    static class EntityWithContentIdOnly {
        @ContentId
        private String contentId;
    }
}
