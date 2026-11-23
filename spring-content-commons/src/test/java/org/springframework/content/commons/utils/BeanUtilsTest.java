package org.springframework.content.commons.utils;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.test.support.DualContentFields;
import org.springframework.content.test.support.SingleContentFields;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

public class BeanUtilsTest {

    static Stream<EntityFixture> entityFixtures() {
        return Stream.of(
            new EntityFixture("plain", SingleContentFields::new),
            new EntityFixture("inheriting", InheritingSingleContentFields::new)
        );
    }

    private record EntityFixture(String label, Supplier<SingleContentFields> factory) {
        @Override
        public @NonNull String toString() {
            return label;
        }
    }

    public static class InheritingSingleContentFields extends SingleContentFields {
    }

    public static class DualContentWithMimeType extends DualContentFields {

        @MimeType
        private String mimeType;

        public String getMimeType() {
            return mimeType;
        }

        public void setMimeType(String mimeType) {
            this.mimeType = mimeType;
        }
    }

    public static class MatchingCondition implements Condition {
        @Override
        public boolean matches(Field field) {
            return true;
        }
    }

    public static class UnmatchingCondition implements Condition {
        @Override
        public boolean matches(Field field) {
            return false;
        }
    }

    @Nested
    class BeanUtilsCases {
        @Nested
        class SetFieldWithAnnotation {
            @ParameterizedTest
            @MethodSource("org.springframework.content.commons.utils.BeanUtilsTest#entityFixtures")
            void shouldSetContentIdViaInheritedAccessor(EntityFixture fixture) {
                SingleContentFields testEntity = fixture.factory().get();
                BeanUtils.setFieldWithAnnotation(testEntity, ContentId.class, "a value");
                assertThat(testEntity.getContentId()).isEqualTo("a value");
            }

            @ParameterizedTest
            @MethodSource("org.springframework.content.commons.utils.BeanUtilsTest#entityFixtures")
            void shouldSetContentLenViaInheritedAccessor(EntityFixture fixture) {
                SingleContentFields testEntity = fixture.factory().get();
                BeanUtils.setFieldWithAnnotation(testEntity, ContentLength.class, 42L);
                assertThat(testEntity.getContentLen()).isEqualTo(42L);
            }

            @ParameterizedTest
            @MethodSource("org.springframework.content.commons.utils.BeanUtilsTest#entityFixtures")
            void shouldNotFailWhenToldToSetOnAMissingField(EntityFixture fixture) {
                SingleContentFields testEntity = fixture.factory().get();
                assertThatCode(() -> BeanUtils.setFieldWithAnnotation(testEntity, Override.class, "value"))
                    .doesNotThrowAnyException();
            }
        }

        @Nested
        class SetFieldWithAnnotationConditionally {
            @ParameterizedTest
            @MethodSource("org.springframework.content.commons.utils.BeanUtilsTest#entityFixtures")
            void shouldSetContentIdIfTheConditionMatches(EntityFixture fixture) {
                SingleContentFields testEntity = fixture.factory().get();
                BeanUtils.setFieldWithAnnotationConditionally(testEntity, ContentId.class, "a value", new MatchingCondition());
                assertThat(testEntity.getContentId()).isEqualTo("a value");
            }

            @ParameterizedTest
            @MethodSource("org.springframework.content.commons.utils.BeanUtilsTest#entityFixtures")
            void shouldSetContentLenIfTheConditionMatches(EntityFixture fixture) {
                SingleContentFields testEntity = fixture.factory().get();
                BeanUtils.setFieldWithAnnotationConditionally(testEntity, ContentLength.class, 42L, new MatchingCondition());
                assertThat(testEntity.getContentLen()).isEqualTo(42L);
            }

            @ParameterizedTest
            @MethodSource("org.springframework.content.commons.utils.BeanUtilsTest#entityFixtures")
            void shouldNotSetContentIdIfTheConditionDoesNotMatch(EntityFixture fixture) {
                SingleContentFields testEntity = fixture.factory().get();
                BeanUtils.setFieldWithAnnotationConditionally(testEntity, ContentId.class, "a value", new UnmatchingCondition());
                assertThat(testEntity.getContentId()).isNull();
            }

            @ParameterizedTest
            @MethodSource("org.springframework.content.commons.utils.BeanUtilsTest#entityFixtures")
            void shouldNotSetContentLenIfTheConditionDoesNotMatch(EntityFixture fixture) {
                SingleContentFields testEntity = fixture.factory().get();
                BeanUtils.setFieldWithAnnotationConditionally(testEntity, ContentLength.class, 42L, new UnmatchingCondition());
                assertThat(testEntity.getContentLen()).isNull();
            }

            @ParameterizedTest
            @MethodSource("org.springframework.content.commons.utils.BeanUtilsTest#entityFixtures")
            void shouldNotFailWhenToldToSetOnAMissingField(EntityFixture fixture) {
                SingleContentFields testEntity = fixture.factory().get();
                assertThatCode(() -> BeanUtils.setFieldWithAnnotationConditionally(testEntity, Override.class, "value", new MatchingCondition()))
                    .doesNotThrowAnyException();
            }
        }

        @Nested
        class GetFieldWithAnnotation {
            @ParameterizedTest
            @MethodSource("org.springframework.content.commons.utils.BeanUtilsTest#entityFixtures")
            void shouldGetContentIdViaInheritedAccessor(EntityFixture fixture) {
                SingleContentFields testEntity = fixture.factory().get();
                testEntity.setContentId("a value");
                testEntity.setContentLen(42L);
                Object value = BeanUtils.getFieldWithAnnotation(testEntity, ContentId.class);
                assertThat(value).isEqualTo("a value");
            }

            @ParameterizedTest
            @MethodSource("org.springframework.content.commons.utils.BeanUtilsTest#entityFixtures")
            void shouldGetContentLenViaInheritedAccessor(EntityFixture fixture) {
                SingleContentFields testEntity = fixture.factory().get();
                testEntity.setContentId("a value");
                testEntity.setContentLen(42L);
                Object value = BeanUtils.getFieldWithAnnotation(testEntity, ContentLength.class);
                assertThat(value).isEqualTo(42L);
            }

            @ParameterizedTest
            @MethodSource("org.springframework.content.commons.utils.BeanUtilsTest#entityFixtures")
            void shouldNotFailWhenToldToGetOnAMissingField(EntityFixture fixture) {
                SingleContentFields testEntity = fixture.factory().get();
                testEntity.setContentId("a value");
                testEntity.setContentLen(42L);
                assertThatCode(() -> BeanUtils.getFieldWithAnnotation(testEntity, Override.class))
                    .doesNotThrowAnyException();
            }
        }

        @Nested
        class HasFieldWithAnnotation {
            @ParameterizedTest
            @MethodSource("org.springframework.content.commons.utils.BeanUtilsTest#entityFixtures")
            void shouldReturnTrueForInheritedContentId(EntityFixture fixture) {
                SingleContentFields testEntity = fixture.factory().get();
                assertThat(BeanUtils.hasFieldWithAnnotation(testEntity, ContentId.class)).isTrue();
            }

            @ParameterizedTest
            @MethodSource("org.springframework.content.commons.utils.BeanUtilsTest#entityFixtures")
            void shouldReturnTrueForInheritedContentLen(EntityFixture fixture) {
                SingleContentFields testEntity = fixture.factory().get();
                assertThat(BeanUtils.hasFieldWithAnnotation(testEntity, ContentLength.class)).isTrue();
            }

            @ParameterizedTest
            @MethodSource("org.springframework.content.commons.utils.BeanUtilsTest#entityFixtures")
            void shouldNotFailWhenAboutAMissingField(EntityFixture fixture) {
                SingleContentFields testEntity = fixture.factory().get();
                assertThatCode(() -> BeanUtils.hasFieldWithAnnotation(testEntity, Override.class))
                    .doesNotThrowAnyException();
            }
        }

        @Nested
        class GetFieldWithAnnotationType {
            @ParameterizedTest
            @MethodSource("org.springframework.content.commons.utils.BeanUtilsTest#entityFixtures")
            void shouldReturnTypeForInheritedContentId(EntityFixture fixture) {
                SingleContentFields testEntity = fixture.factory().get();
                assertThat(BeanUtils.getFieldWithAnnotationType(testEntity, ContentId.class)).isEqualTo(Serializable.class);
            }

            @ParameterizedTest
            @MethodSource("org.springframework.content.commons.utils.BeanUtilsTest#entityFixtures")
            void shouldReturnTypeForInheritedContentLen(EntityFixture fixture) {
                SingleContentFields testEntity = fixture.factory().get();
                assertThat(BeanUtils.getFieldWithAnnotationType(testEntity, ContentLength.class)).isEqualTo(Long.class);
            }

            @ParameterizedTest
            @MethodSource("org.springframework.content.commons.utils.BeanUtilsTest#entityFixtures")
            void shouldNotFailWhenAskedAboutAMissingField(EntityFixture fixture) {
                SingleContentFields testEntity = fixture.factory().get();
                assertThatCode(() -> BeanUtils.getFieldWithAnnotationType(testEntity, Override.class))
                    .doesNotThrowAnyException();
            }
        }

        @Nested
        class FindFieldWithAnnotation {
            @ParameterizedTest
            @MethodSource("org.springframework.content.commons.utils.BeanUtilsTest#entityFixtures")
            void shouldFindInheritedContentIdField(EntityFixture fixture) {
                SingleContentFields testEntity = fixture.factory().get();
                assertThat(BeanUtils.findFieldWithAnnotation(testEntity, ContentId.class)).isNotNull();
            }

            @ParameterizedTest
            @MethodSource("org.springframework.content.commons.utils.BeanUtilsTest#entityFixtures")
            void shouldFindInheritedContentLenField(EntityFixture fixture) {
                SingleContentFields testEntity = fixture.factory().get();
                assertThat(BeanUtils.findFieldWithAnnotation(testEntity, ContentLength.class)).isNotNull();
            }

            @ParameterizedTest
            @MethodSource("org.springframework.content.commons.utils.BeanUtilsTest#entityFixtures")
            void shouldNotFailWhenAboutAMissingField(EntityFixture fixture) {
                SingleContentFields testEntity = fixture.factory().get();
                assertThatCode(() -> BeanUtils.findFieldWithAnnotation(testEntity, Override.class))
                    .doesNotThrowAnyException();
            }
        }

        @Nested
        class FindFieldsWithAnnotation {
            @Test
            void shouldFindFields() {
                assertThat(BeanUtils.findFieldsWithAnnotation(DualContentWithMimeType.class, MimeType.class,
                    new BeanWrapperImpl(new DualContentWithMimeType()))).hasSize(1);
            }
        }

        @Nested
        class GetFieldsWithAnnotation {
            @Test
            void shouldFindBothInheritedContentIdFields() {
                DualContentWithMimeType entity = new DualContentWithMimeType();
                Field[] contentIdFields = BeanUtils.findFieldsWithAnnotation(
                    DualContentWithMimeType.class, ContentId.class, new BeanWrapperImpl(entity));
                assertThat(contentIdFields).hasSize(2);

                entity.setContentId("100");
                entity.setRenditionId("200");
                assertThat(entity.getContentId()).isEqualTo("100");
                assertThat(entity.getRenditionId()).isEqualTo("200");
            }
        }
    }
}
