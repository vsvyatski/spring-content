package org.springframework.content.commons.utils;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;

import java.lang.reflect.Field;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

public class BeanUtilsTest {

    static Stream<EntityFixture> entityFixtures() {
        return Stream.of(
            new EntityFixture("plain", TestEntity::new),
            new EntityFixture("inheriting", InheritingTestEntity::new)
        );
    }

    private record EntityFixture(String label, Supplier<TestEntity> factory) {
        @Override
        public String toString() {
            return label;
        }
    }

    public static class TestEntity {
        @ContentId
        public String fieldOnly;
        @ContentLength
        private String fieldWithGetterSetter;

        public String getFieldWithGetterSetter() {
            return fieldWithGetterSetter;
        }

        public void setFieldWithGetterSetter(String fieldWithGetterSetter) {
            this.fieldWithGetterSetter = fieldWithGetterSetter;
        }
    }

    public static class InheritingTestEntity extends TestEntity {
    }

    public static class TestEntity2 {
        @ContentId
        public String contentId;
        @ContentId
        public String otherContentId;
        @ContentLength
        private String contentLen;
        @MimeType
        private String mimeType;
        @ContentLength
        private String otherContentLen;

        public String getContentId() {
            return contentId;
        }

        public void setContentId(String contentId) {
            this.contentId = contentId;
        }

        public String getContentLen() {
            return contentLen;
        }

        public void setContentLen(String contentLen) {
            this.contentLen = contentLen;
        }

        public String getMimeType() {
            return mimeType;
        }

        public void setMimeType(String mimeType) {
            this.mimeType = mimeType;
        }

        public String getOtherContentId() {
            return otherContentId;
        }

        public void setOtherContentId(String otherContentId) {
            this.otherContentId = otherContentId;
        }

        public String getOtherContentLen() {
            return otherContentLen;
        }

        public void setOtherContentLen(String otherContentLen) {
            this.otherContentLen = otherContentLen;
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
            void shouldSetFieldDirectly(EntityFixture fixture) {
                TestEntity testEntity = fixture.factory().get();
                BeanUtils.setFieldWithAnnotation(testEntity, ContentId.class, "a value");
                assertThat(testEntity.fieldOnly).isEqualTo("a value");
            }

            @ParameterizedTest
            @MethodSource("org.springframework.content.commons.utils.BeanUtilsTest#entityFixtures")
            void shouldSetFieldViaItsSetter(EntityFixture fixture) {
                TestEntity testEntity = fixture.factory().get();
                BeanUtils.setFieldWithAnnotation(testEntity, ContentLength.class, "b value");
                assertThat(testEntity.getFieldWithGetterSetter()).isEqualTo("b value");
            }

            @ParameterizedTest
            @MethodSource("org.springframework.content.commons.utils.BeanUtilsTest#entityFixtures")
            void shouldNotFailWhenToldToSetOnAMissingField(EntityFixture fixture) {
                TestEntity testEntity = fixture.factory().get();
                assertThatCode(() -> BeanUtils.setFieldWithAnnotation(testEntity, Override.class, "value"))
                    .doesNotThrowAnyException();
            }
        }

        @Nested
        class SetFieldWithAnnotationConditionally {
            @ParameterizedTest
            @MethodSource("org.springframework.content.commons.utils.BeanUtilsTest#entityFixtures")
            void shouldSetFieldIfTheConditionMatches(EntityFixture fixture) {
                TestEntity testEntity = fixture.factory().get();
                BeanUtils.setFieldWithAnnotationConditionally(testEntity, ContentId.class, "a value", new MatchingCondition());
                assertThat(testEntity.fieldOnly).isEqualTo("a value");
            }

            @ParameterizedTest
            @MethodSource("org.springframework.content.commons.utils.BeanUtilsTest#entityFixtures")
            void shouldSetFieldViaItsSetterIfTheConditionMatches(EntityFixture fixture) {
                TestEntity testEntity = fixture.factory().get();
                BeanUtils.setFieldWithAnnotationConditionally(testEntity, ContentLength.class, "b value", new MatchingCondition());
                assertThat(testEntity.getFieldWithGetterSetter()).isEqualTo("b value");
            }

            @ParameterizedTest
            @MethodSource("org.springframework.content.commons.utils.BeanUtilsTest#entityFixtures")
            void shouldNotSetFieldIfTheConditionDoesNotMatch(EntityFixture fixture) {
                TestEntity testEntity = fixture.factory().get();
                BeanUtils.setFieldWithAnnotationConditionally(testEntity, ContentId.class, "a value", new UnmatchingCondition());
                assertThat(testEntity.fieldOnly).isNull();
            }

            @ParameterizedTest
            @MethodSource("org.springframework.content.commons.utils.BeanUtilsTest#entityFixtures")
            void shouldNotSetFieldViaItsSetterIfTheConditionDoesNotMatch(EntityFixture fixture) {
                TestEntity testEntity = fixture.factory().get();
                BeanUtils.setFieldWithAnnotationConditionally(testEntity, ContentLength.class, "b value", new UnmatchingCondition());
                assertThat(testEntity.getFieldWithGetterSetter()).isNull();
            }

            @ParameterizedTest
            @MethodSource("org.springframework.content.commons.utils.BeanUtilsTest#entityFixtures")
            void shouldNotFailWhenToldToSetOnAMissingField(EntityFixture fixture) {
                TestEntity testEntity = fixture.factory().get();
                assertThatCode(() -> BeanUtils.setFieldWithAnnotationConditionally(testEntity, Override.class, "value", new MatchingCondition()))
                    .doesNotThrowAnyException();
            }
        }

        @Nested
        class GetFieldWithAnnotation {
            @ParameterizedTest
            @MethodSource("org.springframework.content.commons.utils.BeanUtilsTest#entityFixtures")
            void shouldGetFieldDirectly(EntityFixture fixture) {
                TestEntity testEntity = fixture.factory().get();
                testEntity.fieldOnly = "a value";
                testEntity.setFieldWithGetterSetter("b value");
                Object value = BeanUtils.getFieldWithAnnotation(testEntity, ContentId.class);
                assertThat(value).isEqualTo("a value");
            }

            @ParameterizedTest
            @MethodSource("org.springframework.content.commons.utils.BeanUtilsTest#entityFixtures")
            void shouldGetFieldViaItsGetter(EntityFixture fixture) {
                TestEntity testEntity = fixture.factory().get();
                testEntity.fieldOnly = "a value";
                testEntity.setFieldWithGetterSetter("b value");
                Object value = BeanUtils.getFieldWithAnnotation(testEntity, ContentLength.class);
                assertThat(value).isEqualTo("b value");
            }

            @ParameterizedTest
            @MethodSource("org.springframework.content.commons.utils.BeanUtilsTest#entityFixtures")
            void shouldNotFailWhenToldToGetOnAMissingField(EntityFixture fixture) {
                TestEntity testEntity = fixture.factory().get();
                testEntity.fieldOnly = "a value";
                testEntity.setFieldWithGetterSetter("b value");
                assertThatCode(() -> BeanUtils.getFieldWithAnnotation(testEntity, Override.class))
                    .doesNotThrowAnyException();
            }
        }

        @Nested
        class HasFieldWithAnnotation {
            @ParameterizedTest
            @MethodSource("org.springframework.content.commons.utils.BeanUtilsTest#entityFixtures")
            void shouldReturnTrueForAnnotatedPublicFields(EntityFixture fixture) {
                TestEntity testEntity = fixture.factory().get();
                assertThat(BeanUtils.hasFieldWithAnnotation(testEntity, ContentId.class)).isTrue();
            }

            @ParameterizedTest
            @MethodSource("org.springframework.content.commons.utils.BeanUtilsTest#entityFixtures")
            void shouldReturnTrueForAnnotatedPrivateFieldsWithGetter(EntityFixture fixture) {
                TestEntity testEntity = fixture.factory().get();
                assertThat(BeanUtils.hasFieldWithAnnotation(testEntity, ContentLength.class)).isTrue();
            }

            @ParameterizedTest
            @MethodSource("org.springframework.content.commons.utils.BeanUtilsTest#entityFixtures")
            void shouldNotFailWhenAboutAMissingField(EntityFixture fixture) {
                TestEntity testEntity = fixture.factory().get();
                assertThatCode(() -> BeanUtils.hasFieldWithAnnotation(testEntity, Override.class))
                    .doesNotThrowAnyException();
            }
        }

        @Nested
        class GetFieldWithAnnotationType {
            @ParameterizedTest
            @MethodSource("org.springframework.content.commons.utils.BeanUtilsTest#entityFixtures")
            void shouldReturnTypeForAnnotatedPublicFields(EntityFixture fixture) {
                TestEntity testEntity = fixture.factory().get();
                assertThat(BeanUtils.getFieldWithAnnotationType(testEntity, ContentId.class)).isEqualTo(String.class);
            }

            @ParameterizedTest
            @MethodSource("org.springframework.content.commons.utils.BeanUtilsTest#entityFixtures")
            void shouldReturnTypeForAnnotatedPrivateFieldsWithGetter(EntityFixture fixture) {
                TestEntity testEntity = fixture.factory().get();
                assertThat(BeanUtils.getFieldWithAnnotationType(testEntity, ContentLength.class)).isEqualTo(String.class);
            }

            @ParameterizedTest
            @MethodSource("org.springframework.content.commons.utils.BeanUtilsTest#entityFixtures")
            void shouldNotFailWhenAskedAboutAMissingField(EntityFixture fixture) {
                TestEntity testEntity = fixture.factory().get();
                assertThatCode(() -> BeanUtils.getFieldWithAnnotationType(testEntity, Override.class))
                    .doesNotThrowAnyException();
            }
        }

        @Nested
        class FindFieldWithAnnotation {
            @ParameterizedTest
            @MethodSource("org.springframework.content.commons.utils.BeanUtilsTest#entityFixtures")
            void shouldFindFields(EntityFixture fixture) {
                TestEntity testEntity = fixture.factory().get();
                assertThat(BeanUtils.findFieldWithAnnotation(testEntity, ContentId.class)).isNotNull();
            }

            @ParameterizedTest
            @MethodSource("org.springframework.content.commons.utils.BeanUtilsTest#entityFixtures")
            void shouldFindFieldsWithGetters(EntityFixture fixture) {
                TestEntity testEntity = fixture.factory().get();
                assertThat(BeanUtils.findFieldWithAnnotation(testEntity, ContentLength.class)).isNotNull();
            }

            @ParameterizedTest
            @MethodSource("org.springframework.content.commons.utils.BeanUtilsTest#entityFixtures")
            void shouldNotFailWhenAboutAMissingField(EntityFixture fixture) {
                TestEntity testEntity = fixture.factory().get();
                assertThatCode(() -> BeanUtils.findFieldWithAnnotation(testEntity, Override.class))
                    .doesNotThrowAnyException();
            }
        }

        @Nested
        class FindFieldsWithAnnotation {
            @Test
            void shouldFindFields() {
                assertThat(BeanUtils.findFieldsWithAnnotation(TestEntity2.class, MimeType.class, new BeanWrapperImpl(new TestEntity2())).length).isEqualTo(1);
            }
        }

        @Nested
        class GetFieldsWithAnnotation {
            @Test
            void shouldFindFields() {
                TestEntity2 t = new TestEntity2();
                t.setContentId("100");
                t.setOtherContentId("200");

                assertThat(BeanUtils.getFieldsWithAnnotation(t, ContentId.class)).isEqualTo(new Object[]{"100", "200"});
            }
        }
    }
}
