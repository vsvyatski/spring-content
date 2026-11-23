package internal.org.springframework.content.rest.links;

import internal.org.springframework.content.rest.support.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.test.support.TestSupportData;
import org.springframework.test.context.NestedTestConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static java.lang.String.format;

@ContentLinkIntegrationTest
public class ContentLinksIT {

    @Nested
    @NestedTestConfiguration(NestedTestConfiguration.EnclosingConfiguration.INHERIT)
    class NoLinkRel {
        @Nested
        @NestedTestConfiguration(NestedTestConfiguration.EnclosingConfiguration.INHERIT)
        @DisplayName("Given a store and an entity with a top-level uncorrelated content property")
        class StoreAndEntityWithTopLevelUncorrelatedContentPropertyContentLinkTests extends ContentLinkTests {
            @Autowired
            private WebApplicationContext context;

            @BeforeEach
            void setUp() {
                MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
                JsonMappedContentEntityRepository repository3 = context.getBean(JsonMappedContentEntityRepository.class);
                JsonMappedContentEntityStore store3 = context.getBean(JsonMappedContentEntityStore.class);
                JsonMappedContentEntity testEntity3 = new JsonMappedContentEntity();
                store3.setContent(testEntity3, TestSupportData.sampleTextStream());
                testEntity3 = repository3.save(testEntity3);
                this.setMvc(mvc);
                this.setUrl("/api/jsonMappedContentEntities/" + testEntity3.getId());
                this.setLinkRel("jsonMappedContentEntity");
                this.setExpectedLinkRegex("http://localhost/api/jsonMappedContentEntities/" + testEntity3.getId());
            }

            @Test
            void shouldProvideResponseWithContentLink() throws Exception {
                assertContentLinkInHalResponse();
            }
        }

        @Nested
        @NestedTestConfiguration(NestedTestConfiguration.EnclosingConfiguration.INHERIT)
        @DisplayName("Given a store and an entity with top-level correlated content properties")
        class StoreAndEntityWithTopLevelCorrelatedContentPropertiesContentLinkTests extends ContentLinkTests {
            @Autowired
            private WebApplicationContext context;

            @BeforeEach
            void setUp() {
                MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
                DualContentRenditionEntityRepository repository5 = context.getBean(DualContentRenditionEntityRepository.class);
                DualContentRenditionEntityStore store5 = context.getBean(DualContentRenditionEntityStore.class);
                DualContentRenditionEntity testEntity5 = new DualContentRenditionEntity();
                store5.setContent(testEntity5, PropertyPath.from("content"), TestSupportData.sampleTextStream());
                testEntity5 = repository5.save(testEntity5);
                this.setMvc(mvc);
                this.setUrl("/api/dualContentRenditionEntities/" + testEntity5.getId());
                this.setLinkRel("content");
                this.setExpectedLinkRegex(format("http://localhost/contentApi/dualContentRenditionEntities/%s/content", testEntity5.getId()));
            }

            @Test
            void shouldProvideResponseWithContentLink() throws Exception {
                assertContentLinkInHalResponse();
            }
        }

        @Nested
        @NestedTestConfiguration(NestedTestConfiguration.EnclosingConfiguration.INHERIT)
        @DisplayName("Given a store and an entity with a nested content property")
        class StoreSpecifyingLinkrelAndEntityNestedContentPropertyContentLinkTests extends ContentLinkTests {
            @Autowired
            private WebApplicationContext context;

            @BeforeEach
            void setUp() {
                MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
                AuditedNestedContentEntityRepository repository2 = context.getBean(AuditedNestedContentEntityRepository.class);
                AuditedNestedContentEntityStore store2 = context.getBean(AuditedNestedContentEntityStore.class);
                AuditedNestedContentEntity testEntity2 = new AuditedNestedContentEntity();
                testEntity2.getChild().setMimeType("text/plain");
                store2.setContent(testEntity2, PropertyPath.from("child"), TestSupportData.sampleTextStream());
                testEntity2 = repository2.save(testEntity2);
                this.setMvc(mvc);
                this.setUrl("/api/files/" + testEntity2.getId());
                this.setLinkRel("child");
                this.setExpectedLinkRegex(format("http://localhost/contentApi/files/%s/child", testEntity2.getId()));
            }

            @Test
            void shouldProvideResponseWithContentLink() throws Exception {
                assertContentLinkInHalResponse();
            }
        }

        @Nested
        @NestedTestConfiguration(NestedTestConfiguration.EnclosingConfiguration.INHERIT)
        @DisplayName("Given a store and an entity with nested content properties")
        class StoreSpecifyingLinkRelAndEntityWithNestedContentPropertiesContentLinkTest extends ContentLinkTests {
            @Autowired
            private WebApplicationContext context;

            @BeforeEach
            void setUp() {
                MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
                EmbeddedDualContentEntityRepository repository10 = context.getBean(EmbeddedDualContentEntityRepository.class);
                EmbeddedDualContentEntityStore store10 = context.getBean(EmbeddedDualContentEntityStore.class);
                EmbeddedDualContentEntity testEntity10 = new EmbeddedDualContentEntity();
                store10.setContent(testEntity10, PropertyPath.from("child/content"), TestSupportData.sampleTextStream());
                testEntity10.getChild().setContentMimeType("text/plain");
                testEntity10.getChild().setContentFileName("test");
                store10.setContent(testEntity10, PropertyPath.from("child/preview"), TestSupportData.sampleTextStream());
                testEntity10.getChild().setPreviewMimeType("text/plain");
                testEntity10 = repository10.save(testEntity10);
                this.setMvc(mvc);
                this.setUrl("/api/embeddedDualContentEntities/" + testEntity10.getId());
                this.setLinkRel("child/content");
                this.setExpectedLinkRegex(format("http://localhost/contentApi/embeddedDualContentEntities/%s/child/content", testEntity10.getId()));
            }

            @Test
            void shouldProvideResponseWithContentLink() throws Exception {
                assertContentLinkInHalResponse();
            }
        }
    }
}
