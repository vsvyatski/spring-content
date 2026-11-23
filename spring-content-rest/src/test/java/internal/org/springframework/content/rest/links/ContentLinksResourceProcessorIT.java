package internal.org.springframework.content.rest.links;

import internal.org.springframework.content.rest.support.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.rest.config.HypermediaConfiguration;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.webmvc.PersistentEntityResource;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@WebAppConfiguration
@ContextConfiguration(classes = {
    BaseUriConfig.class,
    DelegatingWebMvcConfiguration.class,
    RepositoryRestMvcConfiguration.class,
    RestConfiguration.class,
    HypermediaConfiguration.class})
@Transactional
@ActiveProfiles("store")
@ExtendWith(SpringExtension.class)
public class ContentLinksResourceProcessorIT {

    @Autowired
    private Repositories repositories;

    @Autowired
    private PersistentEntities persistentEntities;

    @Autowired
    private ContentLinksResourceProcessor processor;

    private PersistentEntityResource resource;

    @Nested
    @DisplayName("Given the Spring Content base URI property is set to /contentApi")
    class SpringContentBaseUriPropertyIsSetToContentApi {
        @Nested
        @DisplayName("Given an entity with a single content ID property")
        class EntityWithSingleContentIdProperty {
            @Nested
            class Tests {
                @BeforeEach
                void setUp() {
                    PersistentEntity<?, ?> persistentEntity = repositories.getPersistentEntity(OneToOneLinkedContentEntity.class);

                    OneToOneLinkedContentEntity obj = new OneToOneLinkedContentEntity();
                    obj.setId(999L);
                    obj.setContentId(UUID.randomUUID());

                    PersistentEntityResource.Builder build = PersistentEntityResource.build(obj, persistentEntity);
                    resource = build.build();

                    MockHttpServletRequest request = new MockHttpServletRequest();
                    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

                    processor.process(resource);
                }

                @Test
                void shouldAddEntityContentLinks() {
                    assertThat(resource.getLinks("content")).extracting("href").contains("http://localhost/contentApi/oneToOneLinkedContentEntities/999/content");
                }
            }

            @Nested
            @DisplayName("When fully qualified links are disabled and shortcut links are enabled")
            class FullyQualifiedLinksAreDisabledAndShortcutLinksAreEnabled {
                @BeforeEach
                void setUp() {
                    PersistentEntity<?, ?> persistentEntity = repositories.getPersistentEntity(OneToOneLinkedContentEntity.class);

                    OneToOneLinkedContentEntity obj = new OneToOneLinkedContentEntity();
                    obj.setId(999L);
                    obj.setContentId(UUID.randomUUID());

                    PersistentEntityResource.Builder build = PersistentEntityResource.build(obj, persistentEntity);
                    resource = build.build();

                    processor.getRestConfiguration().setFullyQualifiedLinks(false);
                    processor.getRestConfiguration().setShortcutLinks(true);

                    MockHttpServletRequest request = new MockHttpServletRequest();
                    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

                    processor.process(resource);
                }

                @AfterEach
                void tearDown() {
                    processor.getRestConfiguration().setFullyQualifiedLinks(true);
                }

                @Test
                void shouldAddOriginalAndShortcutLinks() {
                    assertThat(resource.getLinks("oneToOneLinkedContentEntities")).extracting("href").contains("http://localhost/contentApi/oneToOneLinkedContentEntities/999");
                    assertThat(resource.getLinks("oneToOneLinkedContentEntity")).extracting("href").contains("http://localhost/contentApi/oneToOneLinkedContentEntities/999");
                }
            }

            @Nested
            @DisplayName("When fully qualified links are disabled and shortcut links are disabled")
            class FullyQualifiedLinksAreDisabledAndShortcutLinksAreDisabled {
                @BeforeEach
                void setUp() {
                    PersistentEntity<?, ?> persistentEntity = repositories.getPersistentEntity(OneToOneLinkedContentEntity.class);

                    OneToOneLinkedContentEntity obj = new OneToOneLinkedContentEntity();
                    obj.setId(999L);
                    obj.setContentId(UUID.randomUUID());

                    PersistentEntityResource.Builder build = PersistentEntityResource.build(obj, persistentEntity);
                    resource = build.build();

                    processor.getRestConfiguration().setFullyQualifiedLinks(false);
                    processor.getRestConfiguration().setShortcutLinks(false);

                    MockHttpServletRequest request = new MockHttpServletRequest();
                    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

                    processor.process(resource);
                }

                @AfterEach
                void tearDown() {
                    processor.getRestConfiguration().setFullyQualifiedLinks(true);
                    processor.getRestConfiguration().setShortcutLinks(true);
                }

                @Test
                void shouldAddOriginalAndShortcutLinks() {
                    assertThat(resource.getLinks("oneToOneLinkedContentEntities")).extracting("href").doesNotContain("http://localhost/contentApi/oneToOneLinkedContentEntities/999");
                    assertThat(resource.getLinks("oneToOneLinkedContentEntity")).extracting("href").doesNotContain("http://localhost/contentApi/oneToOneLinkedContentEntities/999");
                }
            }
        }

        @Nested
        @DisplayName("Given an entity with multiple content ID properties")
        class EntityWithMultipleContentIdProperties {
            @BeforeEach
            void setUp() {
                PersistentEntity<?, ?> persistentEntity = repositories.getPersistentEntity(DualContentRenditionEntity.class);

                DualContentRenditionEntity obj = new DualContentRenditionEntity();
                obj.setId(999L);
                UUID contentId = UUID.randomUUID();
                obj.setContentId(contentId);
                obj.setRenditionId(UUID.randomUUID());

                PersistentEntityResource.Builder build = PersistentEntityResource.build(obj, persistentEntity);
                resource = build.build();

                MockHttpServletRequest request = new MockHttpServletRequest();
                RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

                processor.process(resource);
            }

            @Test
            void shouldAddContentPropertyLinks() {
                assertThat(resource.getLinks("content")).extracting("href").contains("http://localhost/contentApi/dualContentRenditionEntities/999/content");
                assertThat(resource.getLinks("rendition")).extracting("href").contains("http://localhost/contentApi/dualContentRenditionEntities/999/rendition");
            }
        }

        @Nested
        @DisplayName("Given an entity with an embedded object containing content ID properties")
        class EntityWithEmbeddedObjectContainingContentIdProperties {
            @BeforeEach
            void setUp() {
                PersistentEntity<?, ?> persistentEntity = repositories.getPersistentEntity(AuditedNestedContentEntity.class);

                AuditedNestedContentEntity obj = new AuditedNestedContentEntity();
                obj.setId(999L);
                UUID contentId = UUID.randomUUID();
                AuditedNestedContentChild child = new AuditedNestedContentChild();
                child.setContentId(contentId);
                obj.setChild(child);

                PersistentEntityResource.Builder build = PersistentEntityResource.build(obj, persistentEntity);
                resource = build.build();

                MockHttpServletRequest request = new MockHttpServletRequest();
                RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

                processor.process(resource);
            }

            @Test
            void shouldAddContentPropertyLinks() {
                assertThat(resource.getLinks("child")).extracting("href").contains("http://localhost/contentApi/files/999/child");
            }
        }

        @Nested
        @DisplayName("Given an entity with embedded object with REST resource customizations")
        class EntityWithEmbeddedObjectWithRestResourceCustomizations {
            @BeforeEach
            void setUp() {
                PersistentEntity<?, ?> persistentEntity = persistentEntities.getRequiredPersistentEntity(RestResourcePackageEntity.class);

                RestResourcePackageEntity testEntity11 = new RestResourcePackageEntity();
                testEntity11.setId(999L);

                PersistentEntityResource.Builder build = PersistentEntityResource.build(testEntity11, persistentEntity);
                resource = build.buildNested();

                MockHttpServletRequest request = new MockHttpServletRequest();
                RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

                processor.process(resource);
            }

            @Test
            void shouldAddContentPropertyLinks() {
                assertThat(resource.getLinks("package/content")).extracting("href").contains("http://localhost/contentApi/restResourcePackageEntities/999/package/content");
                assertThat(resource.getLinks("package/preview")).extracting("href").contains("http://localhost/contentApi/restResourcePackageEntities/999/package/preview");
            }
        }

        @Nested
        @DisplayName("Given the embedded object in an entity containing content ID properties")
        class EmbeddedObjectInEntityContainingContentIdProperties {
            @BeforeEach
            void setUp() {
                PersistentEntity<?, ?> persistentEntity = persistentEntities.getRequiredPersistentEntity(AuditedNestedContentChild.class);

                UUID contentId = UUID.randomUUID();
                AuditedNestedContentChild child = new AuditedNestedContentChild();
                child.setContentId(contentId);

                PersistentEntityResource.Builder build = PersistentEntityResource.build(child, persistentEntity);
                resource = build.buildNested();

                MockHttpServletRequest request = new MockHttpServletRequest();
                RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

                processor.process(resource);
            }

            @Test
            void shouldNotTryToGenerateContentPropertyLinksForEmbeddedObject() {
                assertThat(resource.getLinks().isEmpty()).isTrue();
            }
        }
    }
}
