package org.springframework.content.commons.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.lang.reflect.Field;

import org.springframework.beans.BeanWrapperImpl;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;

public class BeanUtilsTest {

	private TestEntity testEntity;

	
    @Nested
    class BeanUtilsCases {
        @Nested
        class SetFieldWithAnnotation {
            @Nested
            class GivenASimpleNonInheritingClass {
                @BeforeEach
                void setUp() throws Throwable {
                    testEntity = new TestEntity();

                }

                @Test
                void shouldSetFieldDirectly() throws Throwable {
                    BeanUtils.setFieldWithAnnotation(testEntity, ContentId.class,"a value");
                    assertThat(testEntity.fieldOnly).isEqualTo("a value");

                }

                @Test
                void shouldSetFieldViaItsSetter() throws Throwable {
                    BeanUtils.setFieldWithAnnotation(testEntity, ContentLength.class,
                    		"b value");
                    assertThat(testEntity.getFieldWithGetterSetter()).isEqualTo("b value");

                }

                @Test
                void shouldNotFailWhenToldToSetOnAMissingField() throws Throwable {
                    try {
                    	BeanUtils.setFieldWithAnnotation(testEntity, Override.class,
                    			"value");
                    }
                    catch (Exception e) {
                    	fail("should not fail");
                    }

                }

            }

            @Nested
            class GivenAnInheritingClass {
                @BeforeEach
                void setUp() throws Throwable {
                    testEntity = new InheritingTestEntity();

                }

                @Test
                void shouldSetFieldDirectly() throws Throwable {
                    BeanUtils.setFieldWithAnnotation(testEntity, ContentId.class,
                    		"a value");
                    assertThat(testEntity.fieldOnly).isEqualTo("a value");

                }

                @Test
                void shouldSetFieldViaItsSetter() throws Throwable {
                    BeanUtils.setFieldWithAnnotation(testEntity, ContentLength.class,
                    		"b value");
                    assertThat(testEntity.getFieldWithGetterSetter()).isEqualTo("b value");

                }

                @Test
                void shouldNotFailWhenToldToSetOnAMissingField() throws Throwable {
                    try {
                    	BeanUtils.setFieldWithAnnotation(testEntity, Override.class,
                    			"value");
                    }
                    catch (Exception e) {
                    	fail("should not fail");
                    }

                }

            }

        }

        @Nested
        class SetFieldWithAnnotationConditionally {
            @Nested
            class GivenASimpleNonInheritingClass {
                @BeforeEach
                void setUp() throws Throwable {
                    testEntity = new TestEntity();

                }

                @Test
                void shouldSetFieldIfTheConditionMatches() throws Throwable {
                    BeanUtils.setFieldWithAnnotationConditionally(testEntity,
                    		ContentId.class, "a value", new MatchingCondition() {
                    		});
                    assertThat(testEntity.fieldOnly).isEqualTo("a value");

                }

                @Test
                void shouldSetFieldViaItsSetterIfTheConditionMatches() throws Throwable {
                    BeanUtils.setFieldWithAnnotationConditionally(testEntity,
                    		ContentLength.class, "b value", new MatchingCondition() {
                    		});
                    assertThat(testEntity.getFieldWithGetterSetter()).isEqualTo("b value");

                }

                @Test
                void shouldNotSetFieldIfTheConditionDoesNotMatch() throws Throwable {
                    BeanUtils.setFieldWithAnnotationConditionally(testEntity,
                    		ContentId.class, "a value", new UnmatchingCondition() {
                    		});
                    assertThat(testEntity.fieldOnly).isNull();

                }

                @Test
                void shouldNotSetFieldViaItsSetterIfTheConditionDoesNotMatch() throws Throwable {
                    BeanUtils.setFieldWithAnnotationConditionally(testEntity,
                    		ContentLength.class, "b value",
                    		new UnmatchingCondition() {
                    		});
                    assertThat(testEntity.getFieldWithGetterSetter()).isNull();

                }

                @Test
                void shouldNotFailWhenToldToSetOnAMissingField() throws Throwable {
                    try {
                    	BeanUtils.setFieldWithAnnotation(testEntity, Override.class,
                    			"value");
                    }
                    catch (Exception e) {
                    	fail("should not fail");
                    }

                }

            }

            @Nested
            class GivenAnInheritingClass {
                @BeforeEach
                void setUp() throws Throwable {
                    testEntity = new InheritingTestEntity();

                }

                @Test
                void shouldSetFieldIfTheConditionMatches() throws Throwable {
                    BeanUtils.setFieldWithAnnotationConditionally(testEntity,
                    		ContentId.class, "a value", new MatchingCondition() {
                    		});
                    assertThat(testEntity.fieldOnly).isEqualTo("a value");

                }

                @Test
                void shouldSetFieldViaItsSetterIfTheConditionMatches() throws Throwable {
                    BeanUtils.setFieldWithAnnotationConditionally(testEntity,
                    		ContentLength.class, "b value", new MatchingCondition() {
                    		});
                    assertThat(testEntity.getFieldWithGetterSetter()).isEqualTo("b value");

                }

                @Test
                void shouldNotSetFieldIfTheConditionDoesNotMatch() throws Throwable {
                    BeanUtils.setFieldWithAnnotationConditionally(testEntity,
                    		ContentId.class, "a value", new UnmatchingCondition() {
                    		});
                    assertThat(testEntity.fieldOnly).isNull();

                }

                @Test
                void shouldNotSetFieldViaItsSetterIfTheConditionDoesNotMatch() throws Throwable {
                    BeanUtils.setFieldWithAnnotationConditionally(testEntity,
                    		ContentLength.class, "b value",
                    		new UnmatchingCondition() {
                    		});
                    assertThat(testEntity.getFieldWithGetterSetter()).isNull();

                }

                @Test
                void shouldNotFailWhenToldToSetOnAMissingField() throws Throwable {
                    try {
                    	BeanUtils.setFieldWithAnnotation(testEntity, Override.class,
                    			"value");
                    }
                    catch (Exception e) {
                    	fail("should not fail");
                    }

                }

            }

        }

        @Nested
        class GetFieldWithAnnotation {
            @Nested
            class GivenASimpleNonInheritingClass {
                @BeforeEach
                void setUp() throws Throwable {
                    testEntity = new TestEntity();
                    testEntity.fieldOnly = "a value";
                    testEntity.setFieldWithGetterSetter("b value");

                }

                @Test
                void shouldGetFieldDirectly() throws Throwable {
                    Object value = BeanUtils.getFieldWithAnnotation(testEntity,
                    		ContentId.class);
                    assertThat(value).isEqualTo("a value");

                }

                @Test
                void shouldGetFieldViaItsGetter() throws Throwable {
                    Object value = BeanUtils.getFieldWithAnnotation(testEntity,
                    		ContentLength.class);
                    assertThat(value).isEqualTo("b value");

                }

                @Test
                void shouldNotFailWhenToldToGetOnAMissingField() throws Throwable {
                    try {
                    	BeanUtils.getFieldWithAnnotation(testEntity, Override.class);
                    }
                    catch (Exception e) {
                    	fail("should not fail");
                    }

                }

            }

            @Nested
            class GivenAnInheritingClass {
                @BeforeEach
                void setUp() throws Throwable {
                    testEntity = new InheritingTestEntity();
                    testEntity.fieldOnly = "a value";
                    testEntity.setFieldWithGetterSetter("b value");

                }

                @Test
                void shouldGetFieldDirectly() throws Throwable {
                    Object value = BeanUtils.getFieldWithAnnotation(testEntity,
                    		ContentId.class);
                    assertThat(value).isEqualTo("a value");

                }

                @Test
                void shouldGetFieldViaItsGetter() throws Throwable {
                    Object value = BeanUtils.getFieldWithAnnotation(testEntity,
                    		ContentLength.class);
                    assertThat(value).isEqualTo("b value");

                }

                @Test
                void shouldNotFailWhenToldToGetOnAMissingField() throws Throwable {
                    try {
                    	BeanUtils.getFieldWithAnnotation(testEntity, Override.class);
                    }
                    catch (Exception e) {
                    	fail("should not fail");
                    }

                }

            }

        }

        @Nested
        class HasFieldWithAnnotation {
            @Nested
            class GivenASimpleNonInheritingClass {
                @BeforeEach
                void setUp() throws Throwable {
                    testEntity = new TestEntity();

                }

                @Test
                void shouldReturnTrueForAnnotatedPublicFields() throws Throwable {
                    assertThat(BeanUtils.hasFieldWithAnnotation(testEntity,
                    		ContentId.class)).isTrue();

                }

                @Test
                void shouldReturnTrueForAnnotatedPrivateFieldsWithGetter() throws Throwable {
                    assertThat(BeanUtils.hasFieldWithAnnotation(testEntity,
                    		ContentLength.class)).isTrue();

                }

                @Test
                void shouldNotFailWhenAboutAMissingField() throws Throwable {
                    try {
                    	BeanUtils.hasFieldWithAnnotation(testEntity, Override.class);
                    }
                    catch (Exception e) {
                    	fail("should not fail");
                    }

                }

            }

            @Nested
            class GivenAnInheritingClass {
                @BeforeEach
                void setUp() throws Throwable {
                    testEntity = new InheritingTestEntity();

                }

                @Test
                void shouldReturnTrueForAnnotatedPublicFields() throws Throwable {
                    assertThat(BeanUtils.hasFieldWithAnnotation(testEntity,
                    		ContentId.class)).isTrue();

                }

                @Test
                void shouldReturnTrueForAnnotatedPrivateFieldsWithGetter() throws Throwable {
                    assertThat(BeanUtils.hasFieldWithAnnotation(testEntity,
                    		ContentLength.class)).isTrue();

                }

                @Test
                void shouldNotFailWhenAboutAMissingField() throws Throwable {
                    try {
                    	BeanUtils.hasFieldWithAnnotation(testEntity, Override.class);
                    }
                    catch (Exception e) {
                    	fail("should not fail");
                    }

                }

            }

        }

        @Nested
        class GetFieldWithAnnotationType {
            @Nested
            class GivenASimpleNonInheritingClass {
                @BeforeEach
                void setUp() throws Throwable {
                    testEntity = new TestEntity();

                }

                @Test
                void shouldReturnTrueForAnnotatedPublicFields() throws Throwable {
                    assertThat(BeanUtils.getFieldWithAnnotationType(testEntity,
                    				ContentId.class)).isEqualTo(String.class);

                }

                @Test
                void shouldReturnTrueForAnnotatedPrivateFieldsWithGetter() throws Throwable {
                    assertThat(BeanUtils.getFieldWithAnnotationType(testEntity,
                    				ContentLength.class)).isEqualTo(String.class);

                }

                @Test
                void shouldNotFailWhenAskedAboutAMissingField() throws Throwable {
                    try {
                    	BeanUtils.getFieldWithAnnotationType(testEntity,
                    			Override.class);
                    }
                    catch (Exception e) {
                    	fail("should not fail");
                    }

                }

            }

            @Nested
            class GivenAnInheritingClass {
                @BeforeEach
                void setUp() throws Throwable {
                    testEntity = new InheritingTestEntity();

                }

                @Test
                void shouldReturnTrueForAnnotatedPublicFields() throws Throwable {
                    assertThat(BeanUtils.getFieldWithAnnotationType(testEntity,
                    				ContentId.class)).isEqualTo(String.class);

                }

                @Test
                void shouldReturnTrueForAnnotatedPrivateFieldsWithGetter() throws Throwable {
                    assertThat(BeanUtils.getFieldWithAnnotationType(testEntity,
                    				ContentLength.class)).isEqualTo(String.class);

                }

                @Test
                void shouldNotFailWhenAskedAboutAMissingField() throws Throwable {
                    try {
                    	BeanUtils.getFieldWithAnnotationType(testEntity,
                    			Override.class);
                    }
                    catch (Exception e) {
                    	fail("should not fail");
                    }

                }

            }

        }

        @Nested
        class FindFieldWithAnnotation {
            @Nested
            class GivenASimpleNonInheritingClass {
                @BeforeEach
                void setUp() throws Throwable {
                    testEntity = new TestEntity();

                }

                @Test
                void shouldFindFields() throws Throwable {
                    assertThat(BeanUtils.findFieldWithAnnotation(testEntity,
                    		ContentId.class)).isNotNull();

                }

                @Test
                void shouldFindFieldsWithGetters() throws Throwable {
                    assertThat(BeanUtils.findFieldWithAnnotation(testEntity,
                    		ContentLength.class)).isNotNull();

                }

                @Test
                void shouldNotFailWhenAboutAMissingField() throws Throwable {
                    try {
                    	BeanUtils.findFieldWithAnnotation(testEntity, Override.class);
                    }
                    catch (Exception e) {
                    	fail("should not fail");
                    }

                }

            }

            @Nested
            class GivenAnInheritingClass {
                @BeforeEach
                void setUp() throws Throwable {
                    testEntity = new InheritingTestEntity();

                }

                @Test
                void shouldFindFields() throws Throwable {
                    assertThat(BeanUtils.findFieldWithAnnotation(testEntity,
                    		ContentId.class)).isNotNull();

                }

                @Test
                void shouldFindFieldsWithGetters() throws Throwable {
                    assertThat(BeanUtils.findFieldWithAnnotation(testEntity,
                    		ContentLength.class)).isNotNull();

                }

                @Test
                void shouldNotFailWhenAboutAMissingField() throws Throwable {
                    try {
                    	BeanUtils.findFieldWithAnnotation(testEntity, Override.class);
                    }
                    catch (Exception e) {
                    	fail("should not fail");
                    }

                }

            }

        }

        @Nested
        class FindFieldsWithAnnotation {
            @Test
            void shouldFindFields() throws Throwable {
                assertThat(BeanUtils.findFieldsWithAnnotation(TestEntity2.class, MimeType.class, new BeanWrapperImpl(new TestEntity2())).length).isEqualTo(1);

            }

        }

        @Nested
        class GetFieldsWithAnnotation {
            @Test
            void shouldFindFields() throws Throwable {
                TestEntity2 t = new TestEntity2();
                t.setContentId("100");
                t.setOtherContentId("200");

                assertThat(BeanUtils.getFieldsWithAnnotation(t, ContentId.class)).isEqualTo(new Object[]{"100", "200"});

            }

        }

    }

	public static class TestEntity {
		@ContentId
		public String fieldOnly;
		@ContentLength
		private String fieldWithGetterSetter;

		public TestEntity() {
		}

		public String getFieldWithGetterSetter() {
			return fieldWithGetterSetter;
		}

		public void setFieldWithGetterSetter(String fieldWithGetterSetter) {
			this.fieldWithGetterSetter = fieldWithGetterSetter;
		}
	}

	public static class InheritingTestEntity extends TestEntity {}

	public static class TestEntity2 {
		@ContentId public String contentId;
		@ContentLength private String contentLen;
		@MimeType private String mimeType;

		@ContentId public String otherContentId;
		@ContentLength private String otherContentLen;

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
}
