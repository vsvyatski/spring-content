package internal.org.springframework.content.rest.links;

import internal.org.springframework.content.rest.links.linkrel.LinkRelAuditedNestedContentEntityStore;
import internal.org.springframework.content.rest.links.linkrel.LinkRelDualContentRenditionEntityStore;
import internal.org.springframework.content.rest.links.linkrel.LinkRelEmbeddedDualContentEntityStore;
import internal.org.springframework.content.rest.links.linkrel.LinkRelUuidSingleContentEntityStore;
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

@ContentLinkRelIntegrationTest
public class ContentLinkRelIT {

    @Nested
    @NestedTestConfiguration(NestedTestConfiguration.EnclosingConfiguration.INHERIT)
    class ExportingStoreSpecifyingLinkRelOfFoo {
        @Nested
        @NestedTestConfiguration(NestedTestConfiguration.EnclosingConfiguration.INHERIT)
        class LinkRel {
            @Nested
            @DisplayName("Given a store with @StoreRestResource(linkRel = \"foo\") and an entity with a top-level uncorrelated content property")
            class StoreSpecifyingLinkRelAndEntityWithTopLevelUncorrelatedContentProperty extends ContentLinkTests {
                @Autowired
                private WebApplicationContext context;

                @BeforeEach
                void setUp() {
                    MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
                    UuidSingleContentEntityRepository repository = context.getBean(UuidSingleContentEntityRepository.class);
                    LinkRelUuidSingleContentEntityStore contentStore = context.getBean(LinkRelUuidSingleContentEntityStore.class);
                    UuidSingleContentEntity entity = new UuidSingleContentEntity();
                    contentStore.setContent(entity, TestSupportData.sampleTextStream());
                    entity = repository.save(entity);
                    this.setMvc(mvc);
                    this.setUrl("/api/uuidSingleContentEntities/" + entity.getId());
                    this.setLinkRel("foo/content");
                    this.setExpectedLinkRegex(
                        format("http://localhost/contentApi/uuidSingleContentEntitiesContent/%s/content", entity.getId()));
                }

                @Test
                void shouldProvideResponseWithContentLink() throws Exception {
                    assertContentLinkInHalResponse();
                }
            }

            @Nested
            @DisplayName("Given a store with @StoreRestResource(linkRel = \"foo\") and an entity with top-level correlated content properties")

            class StoreSpecifyingLinkRelAndEntityWithTopLevelCorrelatedContentPropertiesC extends ContentLinkTests {
                @Autowired
                private WebApplicationContext context;

                @BeforeEach
                void setUp() {
                    MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
                    DualContentRenditionEntityRepository repository5 = context.getBean(DualContentRenditionEntityRepository.class);
                    LinkRelDualContentRenditionEntityStore store5 = context.getBean(LinkRelDualContentRenditionEntityStore.class);
                    DualContentRenditionEntity entity = new DualContentRenditionEntity();
                    store5.setContent(entity, PropertyPath.from("content"),
                        TestSupportData.sampleTextStream());
                    entity = repository5.save(entity);
                    this.setMvc(mvc);
                    this.setUrl("/api/dualContentRenditionEntities/" + entity.getId());
                    this.setLinkRel("foo/content");
                    this.setExpectedLinkRegex(
                        format("http://localhost/contentApi/dualContentRenditionEntities/%s/content", entity.getId()));
                }

                @Test
                void shouldProvideResponseWithContentLink() throws Exception {
                    assertContentLinkInHalResponse();
                }
            }

            @Nested
            @DisplayName("Given a store with @StoreRestResource(linkRel = \"foo\") and an entity with a nested content property")

            class StoreSpecifyingLinkrelAndEntityNestedContentPropertyContentLinkTests extends ContentLinkTests {
                @Autowired
                private WebApplicationContext context;

                @BeforeEach
                void setUp() {
                    MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
                    AuditedNestedContentEntityRepository repository2 = context.getBean(AuditedNestedContentEntityRepository.class);
                    LinkRelAuditedNestedContentEntityStore store2 = context.getBean(LinkRelAuditedNestedContentEntityStore.class);
                    AuditedNestedContentEntity entity = new AuditedNestedContentEntity();
                    entity.getChild().setMimeType("text/plain");
                    store2.setContent(entity, PropertyPath.from("child"),
                        TestSupportData.sampleTextStream());
                    entity = repository2.save(entity);
                    this.setMvc(mvc);
                    this.setUrl("/api/files/" + entity.getId());
                    this.setLinkRel("foo/child");
                    this.setExpectedLinkRegex(format("http://localhost/contentApi/files/%s/child", entity.getId()));
                }

                @Test
                void shouldProvideResponseWithContentLink() throws Exception {
                    assertContentLinkInHalResponse();
                }
            }

            @Nested
            @DisplayName("Given a store with @StoreRestResource(linkRel = \"foo\") and an entity with nested content properties")

            class StoreSpecifyingLinkRelAndEntityWithNestedContentPropertiesContentLink extends ContentLinkTests {
                @Autowired
                private WebApplicationContext context;

                @BeforeEach
                void setUp() {
                    MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
                    EmbeddedDualContentEntityRepository repository10 = context.getBean(EmbeddedDualContentEntityRepository.class);
                    LinkRelEmbeddedDualContentEntityStore store10 = context.getBean(LinkRelEmbeddedDualContentEntityStore.class);
                    EmbeddedDualContentEntity entity = new EmbeddedDualContentEntity();
                    entity.getChild().setContentMimeType("text/plain");
                    entity.getChild().setContentFileName("test");
                    store10.setContent(entity, PropertyPath.from("child/content"),
                        TestSupportData.sampleTextStream());
                    entity.getChild().setPreviewMimeType("text/plain");
                    store10.setContent(entity, PropertyPath.from("child/preview"),
                        TestSupportData.sampleTextStream());
                    entity = repository10.save(entity);
                    this.setMvc(mvc);
                    this.setUrl("/api/embeddedDualContentEntities/" + entity.getId());
                    this.setLinkRel("foo/child/content");
                    this.setExpectedLinkRegex(
                        format("http://localhost/contentApi/embeddedDualContentEntities/%s/child/content", entity.getId()));
                }

                @Test
                void shouldProvideResponseWithContentLink() throws Exception {
                    assertContentLinkInHalResponse();
                }
            }
        }
    }
}
