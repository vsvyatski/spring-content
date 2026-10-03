package internal.org.springframework.content.rest.mappings;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Disabled;
import static org.assertj.core.api.Assertions.assertThat;

import internal.org.springframework.content.rest.mappingcontext.RestResourceMappingBuilder;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.annotations.OriginalFileName;
import org.springframework.content.commons.mappingcontext.ClassWalker;
import org.springframework.content.rest.RestResource;

import java.util.UUID;

public class RestResourceMappingBuilderTest {

    @Nested
    class RestResourceMappingBuilderCases {
        @Test
        void shouldCreateAMapOfContentPropertyPathsToRequestMappingPaths() {
            RestResourceMappingBuilder visitor = new RestResourceMappingBuilder((restResourceAnnotation) -> restResourceAnnotation.path());
            ClassWalker walker = new ClassWalker(visitor);
            walker.accept(TestClass.class);

            assertThat(visitor.getMappings()).containsEntry("child/child/content", "one/two/three");
            assertThat(visitor.getMappings()).containsEntry("child/child/preview", "one/two/preview");
            assertThat(visitor.getMappings()).containsEntry("child/child/thumbnail", "one/two/thumbnail");
            assertThat(visitor.getMappings()).containsEntry("child/child/idcardFront", "one/two/idcard-front");
            assertThat(visitor.getMappings()).containsEntry("child/childWithout/content", "one/childWithout/three");
            assertThat(visitor.getMappings()).containsEntry("child/childWithout/preview", "one/childWithout/preview");
            assertThat(visitor.getMappings()).containsEntry("child/childWithout/thumbnail", "one/childWithout/thumbnail");
            assertThat(visitor.getMappings()).containsEntry("child/childWithout/idcardFront", "one/childWithout/idcard-front");
            assertThat(visitor.getMappings()).containsEntry("childWithout/child/content", "childWithout/two/three");
            assertThat(visitor.getMappings()).containsEntry("childWithout/child/preview", "childWithout/two/preview");
            assertThat(visitor.getMappings()).containsEntry("childWithout/child/thumbnail", "childWithout/two/thumbnail");
            assertThat(visitor.getMappings()).containsEntry("childWithout/child/idcardFront", "childWithout/two/idcard-front");
            assertThat(visitor.getMappings()).containsEntry("childWithout/childWithout/content", "childWithout/childWithout/three");
            assertThat(visitor.getMappings()).containsEntry("childWithout/childWithout/preview", "childWithout/childWithout/preview");
            assertThat(visitor.getMappings()).containsEntry("childWithout/childWithout/thumbnail", "childWithout/childWithout/thumbnail");
            assertThat(visitor.getMappings()).containsEntry("childWithout/childWithout/idcardFront", "childWithout/childWithout/idcard-front");

            assertThat(visitor.getInverseMappings()).containsEntry("one/two/three", "child/child/content");
            assertThat(visitor.getInverseMappings()).containsEntry("one/two/preview", "child/child/preview");
            assertThat(visitor.getInverseMappings()).containsEntry("one/two/thumbnail", "child/child/thumbnail");
            assertThat(visitor.getInverseMappings()).containsEntry("one/two/idcard-front", "child/child/idcardFront");
            assertThat(visitor.getInverseMappings()).containsEntry("one/childWithout/three", "child/childWithout/content");
            assertThat(visitor.getInverseMappings()).containsEntry("one/childWithout/preview", "child/childWithout/preview");
            assertThat(visitor.getInverseMappings()).containsEntry("one/childWithout/thumbnail", "child/childWithout/thumbnail");
            assertThat(visitor.getInverseMappings()).containsEntry("one/childWithout/idcard-front", "child/childWithout/idcardFront");
            assertThat(visitor.getInverseMappings()).containsEntry("childWithout/two/three", "childWithout/child/content");
            assertThat(visitor.getInverseMappings()).containsEntry("childWithout/two/preview", "childWithout/child/preview");
            assertThat(visitor.getInverseMappings()).containsEntry("childWithout/two/thumbnail", "childWithout/child/thumbnail");
            assertThat(visitor.getInverseMappings()).containsEntry("childWithout/two/idcard-front", "childWithout/child/idcardFront");
            assertThat(visitor.getInverseMappings()).containsEntry("childWithout/childWithout/three", "childWithout/childWithout/content");
            assertThat(visitor.getInverseMappings()).containsEntry("childWithout/childWithout/preview", "childWithout/childWithout/preview");
            assertThat(visitor.getInverseMappings()).containsEntry("childWithout/childWithout/thumbnail", "childWithout/childWithout/thumbnail");
            assertThat(visitor.getInverseMappings()).containsEntry("childWithout/childWithout/idcard-front", "childWithout/childWithout/idcardFront");
        }

    }

    @Disabled("This is not a test and must not be treated as such.")
    public static class TestSubClass {
        @RestResource(path = "two")
        private TestSubSubClass child;
        private TestSubSubClass childWithout;
    }

    @Disabled("This is not a test and must not be treated as such.")
    public static class TestClass {
        @RestResource(path = "one")
        private TestSubClass child;
        private TestSubClass childWithout;
    }

    @Disabled("This is not a test and must not be treated as such.")
    public static class TestSubSubClass {
        @RestResource(path = "three")
        private @ContentId UUID contentId;
        private @ContentLength Long contentLen;
        private @MimeType String contentMimeType;
        private @OriginalFileName String contentOriginalFileName;

        @RestResource(path = "should-be-ignored")
        private String shouldBeIgnored;

        private @ContentId UUID previewId;
        private @ContentLength Long previewLen;
        private @MimeType String previewMimeType;
        private @OriginalFileName String previewOriginalFileName;

        @RestResource(exported = true)
        private @ContentId UUID thumbnailId;
        private @ContentLength Long thumbnailLen;
        private @MimeType String thumbnailMimeType;

        @RestResource(path="idcard-front")
        private @ContentId UUID idcardFrontId;
        private @ContentLength Long idcardFrontLen;
        private @MimeType String idcardFrontMimeType;
    }
}
