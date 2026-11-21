package org.springframework.content.commons.mappingcontext;

import jakarta.persistence.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.annotations.OriginalFileName;
import org.springframework.content.test.support.SingleContentFields;
import org.springframework.content.test.support.UuidSingleContentFields;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.data.mongodb.core.mapping.DBRef;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class ClassWalkerTest {

    /**
     * Hierarchy root; primary content from {@link SingleContentFields}.
     */
    @SuppressWarnings("unused")
    public static class RootWithNestedContent extends SingleContentFields {

        @MimeType
        private Long contentMimeType;
        @OriginalFileName
        private Long contentOriginalFileName;
        private NestedContentChild child;

        private CamelCasedContentProperty camelCaseProperty;
        private PluralizedCamelCasedContentProperty otherCamelCaseProperty;
    }

    /**
     * Nested level; {@code long} / {@code int} lengths exercise {@link TypeDescriptor} primitives.
     */
    @SuppressWarnings("unused")
    public static class NestedContentChild {
        @ContentId
        private String contentId;
        @ContentLength
        private long contentLength;
        @MimeType
        private Long contentMimeType;
        @OriginalFileName
        private Long contentOriginalFileName;
        private NestedContentGrandchild subChild;
    }

    @SuppressWarnings("unused")
    public static class NestedContentGrandchild {
        @ContentId
        private String contentId;
        @ContentLength
        private int contentLength;
        @MimeType
        private Long contentMimeType;
        @OriginalFileName
        private Long contentOriginalFileName;
    }

    /**
     * Uncorrelated attribute names; not shared test-jar shape.
     */
    @SuppressWarnings("unused")
    public static class UncorrelatedAttrClass {
        private @Id
        @GeneratedValue Long id;
        private String name;
        private @ContentId UUID contentId;
        private @ContentLength Long len;
        private @MimeType String mimeType;
        private @OriginalFileName String originalFileName;
        private String title;
    }

    @SuppressWarnings("unused")
    public static class CorrelatedAttrClass extends UuidSingleContentFields {

        private @Id
        @GeneratedValue Long id;
        private String name;
        private @MimeType String contentMimeType;
        private @OriginalFileName String contentOriginalFileName;
        private String title;
    }

    @SuppressWarnings("unused")
    public static class WrapperWithUncorrelatedChild {
        private UncorrelatedAttrClass child;
    }

    @SuppressWarnings("unused")
    public static class EntityWithNonContentRelations {

        @OneToOne
        private NestedContentChild oneToOne;
        @OneToMany
        private NestedContentChild oneToMany;
        @ManyToOne
        private NestedContentChild manyToOne;
        @ManyToMany
        private NestedContentChild manyToMany;
        @DBRef
        private NestedContentChild docRef;
    }

    @SuppressWarnings("unused")
    public static class SubclassOfNestedContentGrandchild extends NestedContentGrandchild {
    }

    /**
     * No declared content fields; {@link ContentId} comes only from {@link UuidSingleContentFields}.
     */
    @SuppressWarnings("unused")
    public static class InheritedUuidContentOnly extends UuidSingleContentFields {
    }

    @SuppressWarnings("unused")
    public static class EntityWithTwoUncorrelatedChildren {
        private UncorrelatedAttrClass child1;
        private UncorrelatedAttrClass child2;
    }

    @SuppressWarnings("unused")
    public static class WrapperWithPrimaryAndPreviewChild {
        private PrimaryAndPreviewContent child;
    }

    @SuppressWarnings("unused")
    public static class PrimaryAndPreviewContent extends UuidSingleContentFields {

        private @MimeType String contentMimeType;
        private @OriginalFileName String contentOriginalFileName;

        private @ContentId UUID previewId;
        private @ContentLength Integer previewLen;
        private @MimeType String previewMimeType;
        private @OriginalFileName String previewOriginalFileName;
    }

    @SuppressWarnings("unused")
    public static class NestedWrapperWithPrimaryAndPreviewChild {
        private WrapperWithPrimaryAndPreviewChild child;
    }

    @SuppressWarnings("unused")
    private static class CamelCasedContentProperty {
        private @ContentId UUID camelCasePropertyId;
        private @ContentLength Long camelCasePropertyLen;
        private @MimeType String camelCasePropertyMimeType;
        private @OriginalFileName String camelCasePropertyOriginalFileName;
    }

    @SuppressWarnings("unused")
    private static class PluralizedCamelCasedContentProperty {
        private @ContentId UUID camelCasePropertyIds;
        private @ContentLength Long camelCasePropertyLens;
        private @MimeType String camelCasePropertyMimetypes;
        private @OriginalFileName String camelCasePropertyFilenames;
    }

    @Nested
    class ClassWalkerCases {
        @Test
        void shouldVisitAllFields() {
            ContentPropertyMappingContextVisitor visitor = new ContentPropertyMappingContextVisitor("/", ".");
            ClassWalker walker = new ClassWalker(visitor);
            walker.accept(RootWithNestedContent.class);

            ContentProperty expectedProperty = new ContentProperty();
            expectedProperty.setContentPropertyPath("content");
            expectedProperty.setContentIdPropertyPath("contentId");
            expectedProperty.setContentIdType(TypeDescriptor.valueOf(String.class));
            expectedProperty.setContentLengthPropertyPath("contentLen");
            expectedProperty.setContentLengthType(TypeDescriptor.valueOf(Long.class));
            expectedProperty.setMimeTypePropertyPath("contentMimeType");
            expectedProperty.setOriginalFileNamePropertyPath("contentOriginalFileName");
            assertThat(visitor.getProperties()).containsEntry("content", expectedProperty);

            ContentProperty expectedSubClassProperty = new ContentProperty();
            expectedSubClassProperty.setContentPropertyPath("child.content");
            expectedSubClassProperty.setContentIdPropertyPath("child.contentId");
            expectedSubClassProperty.setContentIdType(TypeDescriptor.valueOf(String.class));
            expectedSubClassProperty.setContentLengthPropertyPath("child.contentLength");
            expectedSubClassProperty.setContentLengthType(TypeDescriptor.valueOf(long.class));
            expectedSubClassProperty.setMimeTypePropertyPath("child.contentMimeType");
            expectedSubClassProperty.setOriginalFileNamePropertyPath("child.contentOriginalFileName");
            assertThat(visitor.getProperties()).containsEntry("child", expectedSubClassProperty);

            ContentProperty expectedSubSubClassProperty = new ContentProperty();
            expectedSubSubClassProperty.setContentPropertyPath("child.subChild.content");
            expectedSubSubClassProperty.setContentIdPropertyPath("child.subChild.contentId");
            expectedSubSubClassProperty.setContentIdType(TypeDescriptor.valueOf(String.class));
            expectedSubSubClassProperty.setContentLengthPropertyPath("child.subChild.contentLength");
            expectedSubSubClassProperty.setContentLengthType(TypeDescriptor.valueOf(int.class));
            expectedSubSubClassProperty.setMimeTypePropertyPath("child.subChild.contentMimeType");
            expectedSubSubClassProperty.setOriginalFileNamePropertyPath("child.subChild.contentOriginalFileName");
            assertThat(visitor.getProperties()).containsEntry("child/subChild", expectedSubSubClassProperty);

            ContentProperty expectedCamelCaseProperty = new ContentProperty();
            expectedCamelCaseProperty.setContentPropertyPath("camelCaseProperty.camelCaseProperty");
            expectedCamelCaseProperty.setContentIdPropertyPath("camelCaseProperty.camelCasePropertyId");
            expectedCamelCaseProperty.setContentIdType(TypeDescriptor.valueOf(UUID.class));
            expectedCamelCaseProperty.setContentLengthPropertyPath("camelCaseProperty.camelCasePropertyLen");
            expectedCamelCaseProperty.setContentLengthType(TypeDescriptor.valueOf(Long.class));
            expectedCamelCaseProperty.setMimeTypePropertyPath("camelCaseProperty.camelCasePropertyMimeType");
            expectedCamelCaseProperty.setOriginalFileNamePropertyPath("camelCaseProperty.camelCasePropertyOriginalFileName");
            assertThat(visitor.getProperties()).containsEntry("camelCaseProperty", expectedCamelCaseProperty);

            ContentProperty expectedOtherCamelCaseProperty = new ContentProperty();
            expectedOtherCamelCaseProperty.setContentPropertyPath("otherCamelCaseProperty.camelCaseProperty");
            expectedOtherCamelCaseProperty.setContentIdPropertyPath("otherCamelCaseProperty.camelCasePropertyIds");
            expectedOtherCamelCaseProperty.setContentIdType(TypeDescriptor.valueOf(UUID.class));
            expectedOtherCamelCaseProperty.setContentLengthPropertyPath("otherCamelCaseProperty.camelCasePropertyLens");
            expectedOtherCamelCaseProperty.setContentLengthType(TypeDescriptor.valueOf(Long.class));
            expectedOtherCamelCaseProperty.setMimeTypePropertyPath("otherCamelCaseProperty.camelCasePropertyMimetypes");
            expectedOtherCamelCaseProperty.setOriginalFileNamePropertyPath("otherCamelCaseProperty.camelCasePropertyFilenames");
            assertThat(visitor.getProperties()).containsEntry("otherCamelCaseProperty", expectedOtherCamelCaseProperty);
        }
    }

    @Nested
    class GivenAClassWithUncorrelatedAttributes {
        @Test
        void shouldReturnTwoContentProperties() {
            ContentPropertyMappingContextVisitor visitor = new ContentPropertyMappingContextVisitor("/", ".");
            ClassWalker walker = new ClassWalker(visitor);
            walker.accept(UncorrelatedAttrClass.class);

            ContentProperty expectedProperty2 = new ContentProperty();
            expectedProperty2.setContentPropertyPath("content");
            expectedProperty2.setContentIdPropertyPath("contentId");
            expectedProperty2.setContentIdType(TypeDescriptor.valueOf(UUID.class));
            expectedProperty2.setContentLengthPropertyPath("len");
            expectedProperty2.setContentLengthType(TypeDescriptor.valueOf(Long.class));
            expectedProperty2.setMimeTypePropertyPath("mimeType");
            expectedProperty2.setOriginalFileNamePropertyPath("originalFileName");
            assertThat(visitor.getProperties()).containsEntry("content", expectedProperty2);
        }
    }

    @Nested
    class GivenAClassWithCorrelatedAttributes {
        @Test
        void shouldReturnTwoContentProperties() {
            ContentPropertyMappingContextVisitor visitor = new ContentPropertyMappingContextVisitor("/", ".");
            ClassWalker walker = new ClassWalker(visitor);
            walker.accept(CorrelatedAttrClass.class);

            ContentProperty expectedProperty2 = new ContentProperty();
            expectedProperty2.setContentPropertyPath("content");
            expectedProperty2.setContentIdPropertyPath("contentId");
            expectedProperty2.setContentIdType(TypeDescriptor.valueOf(UUID.class));
            expectedProperty2.setContentLengthPropertyPath("contentLen");
            expectedProperty2.setContentLengthType(TypeDescriptor.valueOf(Long.class));
            expectedProperty2.setMimeTypePropertyPath("contentMimeType");
            expectedProperty2.setOriginalFileNamePropertyPath("contentOriginalFileName");
            assertThat(visitor.getProperties()).containsEntry("content", expectedProperty2);
        }
    }

    @Nested
    class GivenAClassWithAChildWithUncorrelatedAttributes {
        @Test
        void shouldReturnTwoContentProperties() {
            ContentPropertyMappingContextVisitor visitor = new ContentPropertyMappingContextVisitor("/", ".");
            ClassWalker walker = new ClassWalker(visitor);
            walker.accept(WrapperWithUncorrelatedChild.class);

            ContentProperty expectedProperty = new ContentProperty();
            expectedProperty.setContentPropertyPath("child.content");
            expectedProperty.setContentIdPropertyPath("child.contentId");
            expectedProperty.setContentIdType(TypeDescriptor.valueOf(UUID.class));
            expectedProperty.setContentLengthPropertyPath("child.len");
            expectedProperty.setContentLengthType(TypeDescriptor.valueOf(Long.class));
            expectedProperty.setMimeTypePropertyPath("child.mimeType");
            expectedProperty.setOriginalFileNamePropertyPath("child.originalFileName");
            assertThat(visitor.getProperties()).containsEntry("child", expectedProperty);
        }
    }

    @Nested
    class GivenAClassWithRelationAttributes {
        @Test
        void shouldNotTraverseThem() {
            ContentPropertyMappingContextVisitor visitor = new ContentPropertyMappingContextVisitor("/", ".");
            ClassWalker walker = new ClassWalker(visitor);
            walker.accept(EntityWithNonContentRelations.class);

            assertThat(visitor.getProperties().size()).isEqualTo(0);
        }
    }

    @Nested
    class GivenInheritedContentIdOnSuperclassOnly {
        @Test
        void shouldCollapseToSingleContentProperty() {
            ContentPropertyMappingContextVisitor visitor = new ContentPropertyMappingContextVisitor("/", ".");
            ClassWalker walker = new ClassWalker(visitor);
            walker.accept(InheritedUuidContentOnly.class);

            assertThat(visitor.getProperties()).hasSize(1);
            ContentProperty expected = new ContentProperty();
            expected.setContentPropertyPath("content");
            expected.setContentIdPropertyPath("contentId");
            expected.setContentIdType(TypeDescriptor.valueOf(UUID.class));
            expected.setContentLengthPropertyPath("contentLen");
            expected.setContentLengthType(TypeDescriptor.valueOf(Long.class));
            assertThat(visitor.getProperties()).containsEntry("content", expected);
        }
    }

    @Nested
    class GivenAClassWithContentPropertiesInItsSuperClass {
        @Test
        void shouldVisitThem() {
            ContentPropertyMappingContextVisitor visitor = new ContentPropertyMappingContextVisitor("/", ".");
            ClassWalker walker = new ClassWalker(visitor);
            walker.accept(SubclassOfNestedContentGrandchild.class);

            ContentProperty expectedProperty = new ContentProperty();
            expectedProperty.setContentPropertyPath("content");
            expectedProperty.setContentIdPropertyPath("contentId");
            expectedProperty.setContentIdType(TypeDescriptor.valueOf(String.class));
            expectedProperty.setContentLengthPropertyPath("contentLength");
            expectedProperty.setContentLengthType(TypeDescriptor.valueOf(int.class));
            expectedProperty.setMimeTypePropertyPath("contentMimeType");
            expectedProperty.setOriginalFileNamePropertyPath("contentOriginalFileName");
            assertThat(visitor.getProperties()).containsEntry("content", expectedProperty);
        }
    }

    @Nested
    class GivenAClassWithMultipleChildContentPropertyObjects {
        @Test
        void shouldReturnTwoContentProperties() {
            ContentPropertyMappingContextVisitor visitor = new ContentPropertyMappingContextVisitor("/", ".");
            ClassWalker walker = new ClassWalker(visitor);
            walker.accept(EntityWithTwoUncorrelatedChildren.class);

            ContentProperty expectedProperty = new ContentProperty();
            expectedProperty.setContentPropertyPath("child1.content");
            expectedProperty.setContentIdPropertyPath("child1.contentId");
            expectedProperty.setContentIdType(TypeDescriptor.valueOf(UUID.class));
            expectedProperty.setContentLengthPropertyPath("child1.len");
            expectedProperty.setContentLengthType(TypeDescriptor.valueOf(Long.class));
            expectedProperty.setMimeTypePropertyPath("child1.mimeType");
            expectedProperty.setOriginalFileNamePropertyPath("child1.originalFileName");
            assertThat(visitor.getProperties()).containsEntry("child1", expectedProperty);

            ContentProperty expectedProperty3 = new ContentProperty();
            expectedProperty3.setContentPropertyPath("child2.content");
            expectedProperty3.setContentIdPropertyPath("child2.contentId");
            expectedProperty3.setContentIdType(TypeDescriptor.valueOf(UUID.class));
            expectedProperty3.setContentLengthPropertyPath("child2.len");
            expectedProperty3.setContentLengthType(TypeDescriptor.valueOf(Long.class));
            expectedProperty3.setMimeTypePropertyPath("child2.mimeType");
            expectedProperty3.setOriginalFileNamePropertyPath("child2.originalFileName");
            assertThat(visitor.getProperties()).containsEntry("child2", expectedProperty3);
        }
    }

    @Nested
    class GivenAClassWithAChildWithMultipleContentPropertyObjects {
        @Test
        void shouldReturnTwoContentProperties() {
            ContentPropertyMappingContextVisitor visitor = new ContentPropertyMappingContextVisitor("/", ".");
            ClassWalker walker = new ClassWalker(visitor);
            walker.accept(WrapperWithPrimaryAndPreviewChild.class);

            ContentProperty expectedProperty = new ContentProperty();
            expectedProperty.setContentPropertyPath("child.content");
            expectedProperty.setContentIdPropertyPath("child.contentId");
            expectedProperty.setContentIdType(TypeDescriptor.valueOf(UUID.class));
            expectedProperty.setContentLengthPropertyPath("child.contentLen");
            expectedProperty.setContentLengthType(TypeDescriptor.valueOf(Long.class));
            expectedProperty.setMimeTypePropertyPath("child.contentMimeType");
            expectedProperty.setOriginalFileNamePropertyPath("child.contentOriginalFileName");
            assertThat(visitor.getProperties()).containsEntry("child/content", expectedProperty);

            ContentProperty expectedProperty3 = new ContentProperty();
            expectedProperty3.setContentPropertyPath("child.preview");
            expectedProperty3.setContentIdPropertyPath("child.previewId");
            expectedProperty3.setContentIdType(TypeDescriptor.valueOf(UUID.class));
            expectedProperty3.setContentLengthPropertyPath("child.previewLen");
            expectedProperty3.setContentLengthType(TypeDescriptor.valueOf(Integer.class));
            expectedProperty3.setMimeTypePropertyPath("child.previewMimeType");
            expectedProperty3.setOriginalFileNamePropertyPath("child.previewOriginalFileName");
            assertThat(visitor.getProperties()).containsEntry("child/preview", expectedProperty3);
        }
    }

    @Nested
    class GivenAClassWithAChildWithAChildWithMultipleContentPropertyObjects {
        @Test
        void shouldReturnTwoContentProperties() {
            ContentPropertyMappingContextVisitor visitor = new ContentPropertyMappingContextVisitor("/", ".");
            ClassWalker walker = new ClassWalker(visitor);
            walker.accept(NestedWrapperWithPrimaryAndPreviewChild.class);

            ContentProperty expectedProperty = new ContentProperty();
            expectedProperty.setContentPropertyPath("child.child.content");
            expectedProperty.setContentIdPropertyPath("child.child.contentId");
            expectedProperty.setContentIdType(TypeDescriptor.valueOf(UUID.class));
            expectedProperty.setContentLengthPropertyPath("child.child.contentLen");
            expectedProperty.setContentLengthType(TypeDescriptor.valueOf(Long.class));
            expectedProperty.setMimeTypePropertyPath("child.child.contentMimeType");
            expectedProperty.setOriginalFileNamePropertyPath("child.child.contentOriginalFileName");
            assertThat(visitor.getProperties()).containsEntry("child/child/content", expectedProperty);

            ContentProperty expectedProperty2 = new ContentProperty();
            expectedProperty2.setContentPropertyPath("child.child.preview");
            expectedProperty2.setContentIdPropertyPath("child.child.previewId");
            expectedProperty2.setContentIdType(TypeDescriptor.valueOf(UUID.class));
            expectedProperty2.setContentLengthPropertyPath("child.child.previewLen");
            expectedProperty2.setContentLengthType(TypeDescriptor.valueOf(Integer.class));
            expectedProperty2.setMimeTypePropertyPath("child.child.previewMimeType");
            expectedProperty2.setOriginalFileNamePropertyPath("child.child.previewOriginalFileName");
            assertThat(visitor.getProperties()).containsEntry("child/child/preview", expectedProperty2);
        }
    }
}
