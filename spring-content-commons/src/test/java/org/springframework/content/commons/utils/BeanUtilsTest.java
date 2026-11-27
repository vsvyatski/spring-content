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
        class SetAnnotatedValue {
            @ParameterizedTest
            @MethodSource("org.springframework.content.commons.utils.BeanUtilsTest#entityFixtures")
            void shouldSetContentIdViaInheritedAccessor(EntityFixture fixture) {
                SingleContentFields testEntity = fixture.factory().get();
                BeanUtils.setAnnotatedValue(testEntity, ContentId.class, "a value");
                assertThat(testEntity.getContentId()).isEqualTo("a value");
            }

            @ParameterizedTest
            @MethodSource("org.springframework.content.commons.utils.BeanUtilsTest#entityFixtures")
            void shouldSetContentLenViaInheritedAccessor(EntityFixture fixture) {
                SingleContentFields testEntity = fixture.factory().get();
                BeanUtils.setAnnotatedValue(testEntity, ContentLength.class, 42L);
                assertThat(testEntity.getContentLen()).isEqualTo(42L);
            }

            @ParameterizedTest
            @MethodSource("org.springframework.content.commons.utils.BeanUtilsTest#entityFixtures")
            void shouldNotFailWhenToldToSetOnMissingField(EntityFixture fixture) {
                SingleContentFields testEntity = fixture.factory().get();
                assertThatCode(() -> BeanUtils.setAnnotatedValue(testEntity, Override.class, "value"))
                    .doesNotThrowAnyException();
            }
        }

        @Nested
        class SetAnnotatedValueConditionally {
            @ParameterizedTest
            @MethodSource("org.springframework.content.commons.utils.BeanUtilsTest#entityFixtures")
            void shouldSetContentIdIfConditionMatches(EntityFixture fixture) {
                SingleContentFields testEntity = fixture.factory().get();
                BeanUtils.setAnnotatedValueConditionally(testEntity, ContentId.class, "a value", new MatchingCondition());
                assertThat(testEntity.getContentId()).isEqualTo("a value");
            }

            @ParameterizedTest
            @MethodSource("org.springframework.content.commons.utils.BeanUtilsTest#entityFixtures")
            void shouldSetContentLenIfConditionMatches(EntityFixture fixture) {
                SingleContentFields testEntity = fixture.factory().get();
                BeanUtils.setAnnotatedValueConditionally(testEntity, ContentLength.class, 42L, new MatchingCondition());
                assertThat(testEntity.getContentLen()).isEqualTo(42L);
            }

            @ParameterizedTest
            @MethodSource("org.springframework.content.commons.utils.BeanUtilsTest#entityFixtures")
            void shouldNotSetContentIdIfConditionDoesNotMatch(EntityFixture fixture) {
                SingleContentFields testEntity = fixture.factory().get();
                BeanUtils.setAnnotatedValueConditionally(testEntity, ContentId.class, "a value", new UnmatchingCondition());
                assertThat(testEntity.getContentId()).isNull();
            }

            @ParameterizedTest
            @MethodSource("org.springframework.content.commons.utils.BeanUtilsTest#entityFixtures")
            void shouldNotSetContentLenIfConditionDoesNotMatch(EntityFixture fixture) {
                SingleContentFields testEntity = fixture.factory().get();
                BeanUtils.setAnnotatedValueConditionally(testEntity, ContentLength.class, 42L, new UnmatchingCondition());
                assertThat(testEntity.getContentLen()).isNull();
            }

            @ParameterizedTest
            @MethodSource("org.springframework.content.commons.utils.BeanUtilsTest#entityFixtures")
            void shouldSetContentIdWhenConditionIsNull(EntityFixture fixture) {
                SingleContentFields testEntity = fixture.factory().get();
                BeanUtils.setAnnotatedValueConditionally(testEntity, ContentId.class, "a value", null);
                assertThat(testEntity.getContentId()).isEqualTo("a value");
            }

            @ParameterizedTest
            @MethodSource("org.springframework.content.commons.utils.BeanUtilsTest#entityFixtures")
            void shouldNotFailWhenToldToSetOnMissingField(EntityFixture fixture) {
                SingleContentFields testEntity = fixture.factory().get();
                assertThatCode(() -> BeanUtils.setAnnotatedValueConditionally(testEntity, Override.class, "value", new MatchingCondition()))
                    .doesNotThrowAnyException();
            }
        }

        @Nested
        class GetAnnotatedValue {
            @ParameterizedTest
            @MethodSource("org.springframework.content.commons.utils.BeanUtilsTest#entityFixtures")
            void shouldGetContentIdViaInheritedAccessor(EntityFixture fixture) {
                SingleContentFields testEntity = fixture.factory().get();
                testEntity.setContentId("a value");
                testEntity.setContentLen(42L);
                Object value = BeanUtils.getAnnotatedValue(testEntity, ContentId.class);
                assertThat(value).isEqualTo("a value");
            }

            @ParameterizedTest
            @MethodSource("org.springframework.content.commons.utils.BeanUtilsTest#entityFixtures")
            void shouldGetContentLenViaInheritedAccessor(EntityFixture fixture) {
                SingleContentFields testEntity = fixture.factory().get();
                testEntity.setContentId("a value");
                testEntity.setContentLen(42L);
                Object value = BeanUtils.getAnnotatedValue(testEntity, ContentLength.class);
                assertThat(value).isEqualTo(42L);
            }

            @ParameterizedTest
            @MethodSource("org.springframework.content.commons.utils.BeanUtilsTest#entityFixtures")
            void shouldReturnNullWhenAnnotationMissing(EntityFixture fixture) {
                SingleContentFields testEntity = fixture.factory().get();
                testEntity.setContentId("a value");
                testEntity.setContentLen(42L);
                assertThat(BeanUtils.getAnnotatedValue(testEntity, Override.class)).isNull();
            }
        }

        @Nested
        class HasAnnotatedField {
            @ParameterizedTest
            @MethodSource("org.springframework.content.commons.utils.BeanUtilsTest#entityFixtures")
            void shouldReturnTrueForInheritedContentId(EntityFixture fixture) {
                SingleContentFields testEntity = fixture.factory().get();
                assertThat(BeanUtils.hasAnnotatedField(testEntity, ContentId.class)).isTrue();
            }

            @ParameterizedTest
            @MethodSource("org.springframework.content.commons.utils.BeanUtilsTest#entityFixtures")
            void shouldReturnTrueForInheritedContentLen(EntityFixture fixture) {
                SingleContentFields testEntity = fixture.factory().get();
                assertThat(BeanUtils.hasAnnotatedField(testEntity, ContentLength.class)).isTrue();
            }

            @ParameterizedTest
            @MethodSource("org.springframework.content.commons.utils.BeanUtilsTest#entityFixtures")
            void shouldReturnFalseWhenAnnotationMissing(EntityFixture fixture) {
                SingleContentFields testEntity = fixture.factory().get();
                assertThat(BeanUtils.hasAnnotatedField(testEntity, Override.class)).isFalse();
            }
        }

        @Nested
        class GetAnnotatedFieldType {
            @ParameterizedTest
            @MethodSource("org.springframework.content.commons.utils.BeanUtilsTest#entityFixtures")
            void shouldReturnTypeForInheritedContentId(EntityFixture fixture) {
                SingleContentFields testEntity = fixture.factory().get();
                assertThat(BeanUtils.getAnnotatedFieldType(testEntity, ContentId.class)).isEqualTo(Serializable.class);
            }

            @ParameterizedTest
            @MethodSource("org.springframework.content.commons.utils.BeanUtilsTest#entityFixtures")
            void shouldReturnTypeForInheritedContentLen(EntityFixture fixture) {
                SingleContentFields testEntity = fixture.factory().get();
                assertThat(BeanUtils.getAnnotatedFieldType(testEntity, ContentLength.class)).isEqualTo(Long.class);
            }

            @ParameterizedTest
            @MethodSource("org.springframework.content.commons.utils.BeanUtilsTest#entityFixtures")
            void shouldReturnNullWhenAnnotationMissing(EntityFixture fixture) {
                SingleContentFields testEntity = fixture.factory().get();
                assertThat(BeanUtils.getAnnotatedFieldType(testEntity, Override.class)).isNull();
            }
        }

        @Nested
        class FindAnnotatedField {
            @ParameterizedTest
            @MethodSource("org.springframework.content.commons.utils.BeanUtilsTest#entityFixtures")
            void shouldFindInheritedContentIdField(EntityFixture fixture) {
                SingleContentFields testEntity = fixture.factory().get();
                assertThat(BeanUtils.findAnnotatedField(testEntity, ContentId.class)).isNotNull();
            }

            @ParameterizedTest
            @MethodSource("org.springframework.content.commons.utils.BeanUtilsTest#entityFixtures")
            void shouldFindInheritedContentLenField(EntityFixture fixture) {
                SingleContentFields testEntity = fixture.factory().get();
                assertThat(BeanUtils.findAnnotatedField(testEntity, ContentLength.class)).isNotNull();
            }

            @ParameterizedTest
            @MethodSource("org.springframework.content.commons.utils.BeanUtilsTest#entityFixtures")
            void shouldReturnNullWhenAnnotationMissing(EntityFixture fixture) {
                SingleContentFields testEntity = fixture.factory().get();
                assertThat(BeanUtils.findAnnotatedField(testEntity, Override.class)).isNull();
            }
        }

        @Nested
        class FindAnnotatedFields {
            @Test
            void shouldFindFields() {
                assertThat(BeanUtils.findAnnotatedFields(DualContentWithMimeType.class, MimeType.class,
                    new BeanWrapperImpl(new DualContentWithMimeType()))).hasSize(1);
            }
        }

        @Nested
        class FindMultipleAnnotatedFieldsOnEntity {
            @Test
            void shouldFindBothInheritedContentIdFields() {
                DualContentWithMimeType entity = new DualContentWithMimeType();
                Field[] contentIdFields = BeanUtils.findAnnotatedFields(
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
