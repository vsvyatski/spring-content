package internal.org.springframework.content.rest.links;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;


import java.util.UUID;

import internal.org.springframework.content.rest.support.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.rest.RestResource;
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
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;


import jakarta.persistence.*;

@WebAppConfiguration
@ContextConfiguration(classes = {
		BaseUriConfig.class,
		DelegatingWebMvcConfiguration.class,
		RepositoryRestMvcConfiguration.class,
		RestConfiguration.class,
		HypermediaConfiguration.class })
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

	@Autowired
	private WebApplicationContext context;

	private MockMvc mvc;

	private PersistentEntityResource resource;

	
    @Nested
    class GivenTheSpringContentBaseUriPropertyIsSetToContentApiCases {
        @Nested
        class GivenAnEntityWithASingleContentIdProperty {
            @Nested
            class Tests {
                @BeforeEach
                void setUp() {
                    mvc = MockMvcBuilders.webAppContextSetup(context).build();

                    PersistentEntity<?, ?> persistentEntity = repositories.getPersistentEntity(TestEntity4.class);

                    					TestEntity4 obj = new TestEntity4();
                    					obj.setId(999L);
                    					obj.setContentId(UUID.randomUUID());

                    					PersistentEntityResource.Builder build = PersistentEntityResource.build(obj, persistentEntity);
                    					resource = build.build();

                    MockHttpServletRequest request = new MockHttpServletRequest();
                    				RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

                    				processor.process(resource);
                }
                @Test
                void shouldAddAnEntityContentLinks() {
                    assertThat(resource.getLinks("content")).extracting("href").contains("http://localhost/contentApi/testEntity4s/999/content");
                }
            }
            @Nested
            class WhenFullyQualifiedLinksAreDisabledAndShortcutLinksAreEnabled {
                @BeforeEach
                void setUp() {
                    mvc = MockMvcBuilders.webAppContextSetup(context).build();

                    PersistentEntity<?, ?> persistentEntity = repositories.getPersistentEntity(TestEntity4.class);

                    					TestEntity4 obj = new TestEntity4();
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
                    assertThat(resource.getLinks("testEntity4s")).extracting("href").contains("http://localhost/contentApi/testEntity4s/999");
                    						assertThat(resource.getLinks("testEntity4")).extracting("href").contains("http://localhost/contentApi/testEntity4s/999");
                }
            }
            @Nested
            class WhenFullyQualifiedLinksAreDisabledAndShortcutLinksAreDisabled {
                @BeforeEach
                void setUp() {
                    mvc = MockMvcBuilders.webAppContextSetup(context).build();

                    PersistentEntity<?, ?> persistentEntity = repositories.getPersistentEntity(TestEntity4.class);

                    					TestEntity4 obj = new TestEntity4();
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
                    assertThat(resource.getLinks("testEntity4s")).extracting("href").doesNotContain("http://localhost/contentApi/testEntity4s/999");
                                            assertThat(resource.getLinks("testEntity4")).extracting("href").doesNotContain("http://localhost/contentApi/testEntity4s/999");
                }
            }
        }
        @Nested
        class GivenAnEntityWithMultipleContentIdProperties {
            @BeforeEach
            void setUp() {
                mvc = MockMvcBuilders.webAppContextSetup(context).build();

                PersistentEntity<?, ?> persistentEntity = repositories.getPersistentEntity(TestEntity5.class);

                					TestEntity5 obj = new TestEntity5();
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
                assertThat(resource.getLinks("content")).extracting("href").contains("http://localhost/contentApi/testEntity5s/999/content");
                					assertThat(resource.getLinks("rendition")).extracting("href").contains("http://localhost/contentApi/testEntity5s/999/rendition");
            }
        }
        @Nested
        class GivenAnEntityWithAnEmbeddedObjectContainingContentIdProperties {
            @BeforeEach
            void setUp() {
                mvc = MockMvcBuilders.webAppContextSetup(context).build();

                PersistentEntity<?, ?> persistentEntity = repositories.getPersistentEntity(TestEntity2.class);

                					TestEntity2 obj = new TestEntity2();
                					obj.setId(999L);
                					UUID contentId = UUID.randomUUID();
                					TestEntityChild child = new TestEntityChild();
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
        class GivenAnEntityWithEmbeddedObjectWithRestResourceCustomizationsIssue1049 {
            @BeforeEach
            void setUp() {
                mvc = MockMvcBuilders.webAppContextSetup(context).build();

                PersistentEntity<?, ?> persistentEntity = persistentEntities.getRequiredPersistentEntity(TestEntity11.class);

                					TestEntity11 testEntity11 = new TestEntity11();
                					testEntity11.setId(999L);

                					PersistentEntityResource.Builder build = PersistentEntityResource.build(testEntity11, persistentEntity);
                					resource = build.buildNested();

                MockHttpServletRequest request = new MockHttpServletRequest();
                				RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

                				processor.process(resource);
            }
            @Test
            void shouldAddContentPropertyLinks() {
                assertThat(resource.getLinks("package/content")).extracting("href").contains("http://localhost/contentApi/testEntity11s/999/package/content");
                					assertThat(resource.getLinks("package/preview")).extracting("href").contains("http://localhost/contentApi/testEntity11s/999/package/preview");
            }
        }
        @Nested
        class GivenTheEmbeddedObjectInAnEntityContainingContentIdProperties {
            @BeforeEach
            void setUp() {
                mvc = MockMvcBuilders.webAppContextSetup(context).build();

                PersistentEntity<?, ?> persistentEntity = persistentEntities.getRequiredPersistentEntity(TestEntityChild.class);

                					UUID contentId = UUID.randomUUID();
                					TestEntityChild child = new TestEntityChild();
                					child.setContentId(contentId);

                					PersistentEntityResource.Builder build = PersistentEntityResource.build(child, persistentEntity);
                					resource = build.buildNested();

                MockHttpServletRequest request = new MockHttpServletRequest();
                				RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

                				processor.process(resource);
            }
            @Test
            void shouldNotTryToGenerateContentPropertyLinksForTheEmbeddedObject() {
                assertThat(resource.getLinks().isEmpty()).isTrue();
            }
        }
    }


	@Test
	public void noop() {
	}
}
