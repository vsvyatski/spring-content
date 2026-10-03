package it.internal.org.springframework.content.rest.controllers;

import org.junit.jupiter.api.extension.ExtendWith;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.DisplayName;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static java.lang.String.format;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import internal.org.springframework.content.rest.support.EventListenerConfig.TestEventListener;
import jakarta.servlet.ServletException;
import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.util.Optional;

import internal.org.springframework.content.rest.support.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.store.StoreAccessException;
import org.springframework.content.rest.config.HypermediaConfiguration;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;

@WebAppConfiguration
@ContextConfiguration(classes = {
		StoreConfig.class,
		EntityConfig.class,
		DelegatingWebMvcConfiguration.class,
		RepositoryRestMvcConfiguration.class,
		RestConfiguration.class,
		HypermediaConfiguration.class,
		EventListenerConfig.class
})
@Transactional
@ActiveProfiles("store")
@ExtendWith(SpringExtension.class)
public class ContentEntityRestEndpointsIT {

	// different exported URI
	@Autowired
	TestEntityRepository repository;
	@Autowired
	TestEntityContentRepository contentRepository;

	// same exported URI
	@Autowired
	TestEntity3Repository repo3;
	@Autowired
	TestEntity3ContentRepository store3;

	// same exported URI
	@Autowired
	TestEntity4Repository repo4;
	@Autowired
	TestEntity4ContentRepository store4;

	// shared @Id/@ContentId
	@Autowired
	TestEntity6Repository repo6;
	@Autowired
	TestEntity6Store store6;

    // single content property, correlated attributes
    @Autowired
    TestEntity9Repository repo9;
    @Autowired
    TestEntity9Store store9;

	// mapped content property
	@Autowired
	TestEntity11Repository repo11;
	@Autowired
	TestEntity11Store store11;

	@Autowired
	TestStore store;

	@Autowired
	TestEventListener eventListener;

	@Autowired
	private WebApplicationContext context;

	private MockMvc mvc;

	private TestEntity testEntity;
	private TestEntity3 testEntity3;
	private TestEntity4 testEntity4;
	private TestEntity6 testEntity6;
    private TestEntity9 testEntity9;
	private TestEntity11 testEntity11;

	private Version version;
	private LastModifiedDate lastModifiedDate;

	private Entity entityTests;
	private Content contentTests;
	private Cors corsTests;

	
    @Nested
    class ContentEntityRESTEndpoints {
        @Nested
        class GivenAnEntityWithASingleUncorrelatedContentProperties {
            @Nested
            class GivenTheRepositoryAndStorageAreExportedToTheSameURI {
                @Nested
                class ADELETEToStoreIdSoftDeleteCustomHandler {
                    @BeforeEach
                    void setUp() {
                        mvc = MockMvcBuilders.webAppContextSetup(context).build();

                        testEntity3 = repo3.save(new TestEntity3());
                        testEntity3.name = "tests";
                        testEntity3 = repo3.save(testEntity3);
                        entityTests.setMvc(mvc);
                        entityTests.setUrl("/testEntity3s/" + testEntity3.id);
                        entityTests.setEntity(testEntity3);
                        entityTests.setRepository(repo3);
                        entityTests.setLinkRel("testEntity3");
                        contentTests.setMvc(mvc);
                        contentTests.setUrl("/testEntity3s/" + testEntity3.getId());
                        contentTests.setEntity(testEntity3);
                        contentTests.setRepository(repo3);
                        contentTests.setStore(store3);
                    }

                    @Test
                    void shouldReturn200() throws Exception {
                        mvc.perform(delete("/testEntity3s/" + testEntity3.id + "/softDelete"))
                                .andExpect(status().is2xxSuccessful());
                    }

                }

            }

            @Nested
            class GivenTheRepositoryAndStorageAreExportedToDifferentURIs {
                @Nested
                class AnOPTIONSRequestToTheRepositoryFromAKnownHost {
                    @BeforeEach
                    void setUp() {
                        mvc = MockMvcBuilders.webAppContextSetup(context).build();

                        testEntity = repository.save(new TestEntity());
                        contentTests.setMvc(mvc);
                        contentTests.setUrl("/testEntitiesContent/" + testEntity.getId());
                        contentTests.setEntity(testEntity);
                        contentTests.setRepository(repository);
                        contentTests.setStore(contentRepository);
                        corsTests.setMvc(mvc);
                        corsTests.setUrl("/testEntitiesContent/" + testEntity.getId());
                    }

                    @Test
                    void shouldReturnTheRelevantCORSHeadersAndOK() throws Exception {
                        mvc.perform(options("/testEntities/" + testEntity.getId())
                        		.header("Access-Control-Request-Method", "PUT")
                        		.header("Origin", "http://www.someurl.com"))
                        		.andExpect(status().isOk())
                        		.andExpect(header().string("Access-Control-Allow-Origin","http://www.someurl.com"));
                    }

                }

            }

            @Nested
            @DisplayName("given an entity with @Version")
            class GivenAnEntityWithVersionVersion extends Version {
                @BeforeEach
                void setUp() {
                    mvc = MockMvcBuilders.webAppContextSetup(context).build();
                    testEntity4 = new TestEntity4();
                    testEntity4 = store4.setContent(testEntity4, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                    testEntity4.mimeType = "text/plain";
                    testEntity4 = repo4.save(testEntity4);
                    String url = "/testEntity4s/" + testEntity4.getId();
                    this.setMvc(mvc);
                    this.setUrl(url);
                    this.setCollectionUrl("/testEntity4s");
                    this.setContentLinkRel("content");
                    this.setRepo(repo4);
                    this.setStore(store4);
                    this.setEtag(format("\"%s\"", testEntity4.getVersion()));
                    this.setEntity(testEntity4);
                }
            }

            @Nested
            @DisplayName("given an entity with @LastModifiedDate")
            class GivenAnEntityWithLastModifiedDateLastModifiedDate extends LastModifiedDate {
                @BeforeEach
                void setUp() {
                    mvc = MockMvcBuilders.webAppContextSetup(context).build();
                    String content = "Hello Spring Content LastModifiedDate World!";
                    testEntity4 = new TestEntity4();
                    testEntity4 = store4.setContent(testEntity4, new ByteArrayInputStream(content.getBytes()));
                    testEntity4.mimeType = "text/plain";
                    testEntity4 = repo4.save(testEntity4);
                    String url = "/testEntity4s/" + testEntity4.getId();
                    this.setMvc(mvc);
                    this.setUrl(url);
                    this.setLastModifiedDate(testEntity4.getModifiedDate());
                    this.setEtag(testEntity4.getVersion().toString());
                    this.setContent(content);
                }
            }

            @Nested
            class GivenAnEntityWithASharedIdAndContentIdField {
                @BeforeEach
                void setUp() {
                    mvc = MockMvcBuilders.webAppContextSetup(context).build();

                    testEntity6 = new TestEntity6();
                    testEntity6 = repo6.save(testEntity6);
                }

                @Test
                void shouldReturn404WhenNoContentIsSet() throws Exception {
                    mvc.perform(get("/testEntity6s/" + testEntity6.getId())
                    			.accept("text/plain"))
                    	.andExpect(status().isNotFound());
                }

            }

        }

        @Nested
        class GivenAMultipartFormPOSTToAnEntityWithASingleUncorrelatedContentProperty {
            @BeforeEach
            void setUp() {
                mvc = MockMvcBuilders.webAppContextSetup(context).build();
            }

            @Test
            void shouldCreateANewEntityAndItsContentAndRespondWithA201Created() throws Exception {
                // assert content does not exist
                String newContent = "This is some new content";

                MockMultipartFile file = new MockMultipartFile("content", "filename.txt", "text/plain", newContent.getBytes());

                var testEntity4Id = repo4.save(new TestEntity4()).getId();

                // POST the new content
                MockHttpServletResponse response = mvc.perform(multipart("/testEntity3s")
                				.file(file)
                				.contentType("multipart/form-data; boundary=c0de8278")
                				.param("name", "foo")
                				.param("hidden", "bar")
                				.param("ying", "yang")
                				.param("things", "one", "two")
                				.param("testEntity4", "/testEntity4s/" + testEntity4Id))

                		.andExpect(status().isCreated())
                		.andReturn().getResponse();

                String location = response.getHeader("Location");

                Optional<TestEntity3> fetchedEntity = repo3.findById(Long.valueOf(StringUtils.substringAfterLast(location, "/")));
                assertThat(fetchedEntity.get().getName()).isEqualTo("foo");
                assertThat(fetchedEntity.get().getHidden()).isNull();
                assertThat(fetchedEntity.get().getYang()).isEqualTo("yang");
                assertThat(fetchedEntity.get().getThings()).contains("one", "two");
                assertThat(fetchedEntity.get().getTestEntity4()).isNotNull();
                assertThat(fetchedEntity.get().getLen()).isEqualTo(file.getSize());
                assertThat(fetchedEntity.get().getOriginalFileName()).isEqualTo(file.getOriginalFilename());

                // assert that it now exists
                response = mvc.perform(get(location)
                				.accept("text/plain"))
                		.andExpect(status().isOk())
                		.andReturn().getResponse();

                assertThat(response.getContentAsString()).isEqualTo(newContent);
            }

        }

        @Nested
        class GivenAMultipartFormPOSTToAnEntityWithANonDefaultInitializedVersionPropertyIssue2 {
            @Nested
            class WithContent {
                @BeforeEach
                void setUp() {
                    mvc = MockMvcBuilders.webAppContextSetup(context).build();
                }

                @Test
                void shouldCreateANewEntityAndItsContentAndRespondWithA201Created() throws Exception {
                    String newContent = "This is some new content";

                    MockMultipartFile file = new MockMultipartFile("content", "filename.txt", "text/plain",
                    		newContent.getBytes());

                    // POST the entity
                    MockHttpServletResponse response = mvc.perform(multipart("/testEntity4s")
                    				.file(file)
                    				.param("name", "foo")
                    				.param("title", "bar"))

                    		.andExpect(status().isCreated())
                    		.andReturn().getResponse();

                    String location = response.getHeader("Location");

                    // assert that the entity exists
                    Optional<TestEntity4> fetchedEntity = repo4.findById(
                    		Long.valueOf(StringUtils.substringAfterLast(location, "/")));
                    assertThat(fetchedEntity.get().getName()).isEqualTo("foo");
                    assertThat(fetchedEntity.get().getTitle()).isEqualTo("bar");
                    assertThat(fetchedEntity.get().getContentId()).isNotNull();
                    assertThat(fetchedEntity.get().getLen()).isEqualTo(file.getSize());
                    assertThat(fetchedEntity.get().getOriginalFileName()).isEqualTo(file.getOriginalFilename());

                    // assert that the content now exists
                    response = mvc.perform(get(location)
                    				.accept("text/plain"))
                    		.andExpect(status().isOk())
                    		.andReturn().getResponse();

                    assertThat(response.getContentAsString()).isEqualTo(newContent);
                }

            }

            @Nested
            class WithoutContent {
                @BeforeEach
                void setUp() {
                    mvc = MockMvcBuilders.webAppContextSetup(context).build();
                }

                @Test
                void shouldCreateANewEntityAndRespondWithA201Created() throws Exception {
                    // POST the entity
                    MockHttpServletResponse response = mvc.perform(multipart("/testEntity4s")
                    				.param("name", "foo")
                    				.param("title", "bar"))

                    		.andExpect(status().isCreated())
                    		.andReturn().getResponse();

                    String location = response.getHeader("Location");

                    // assert that the entity exists
                    Optional<TestEntity4> fetchedEntity = repo4.findById(
                    		Long.valueOf(StringUtils.substringAfterLast(location, "/")));
                    assertThat(fetchedEntity.get().getName()).isEqualTo("foo");
                    assertThat(fetchedEntity.get().getTitle()).isEqualTo("bar");
                    assertThat(fetchedEntity.get().getContentId()).isNull();
                    assertThat(fetchedEntity.get().getLen()).isNull();
                    assertThat(fetchedEntity.get().getOriginalFileName()).isNull();
                }

            }

        }

        @Nested
        class GivenAnEntityWithASingleCorrelatedContentProperty {
            @BeforeEach
            void setUp() {
                mvc = MockMvcBuilders.webAppContextSetup(context).build();

                testEntity9 = repo9.save(new TestEntity9());
            }

            @Test
            void shouldSupportContentOperations() throws Exception {
                String content = "Hello Spring Content World!";
                mvc.perform(
                        put("/testEntity9s/" + testEntity9.id)
                        .contextPath("")
                        .content(content)
                        .contentType("text/plain"))
                .andExpect(status().isCreated());

                Optional<TestEntity9> fetched = repo9.findById(testEntity9.getId());
                assertThat(fetched.isPresent()).isTrue();
                assertThat(fetched.get().getContentId()).isNotNull();
                assertThat(fetched.get().getContentLen()).isEqualTo(27L);
                assertThat(fetched.get().getContentMimeType()).isEqualTo("text/plain");
                assertThat(IOUtils.toString(store9.getContent(fetched.get()), Charset.defaultCharset())).isEqualTo(content);

                MockHttpServletResponse response = mvc
                        .perform(get("/testEntity9s/" + testEntity9.id)
                                .contextPath("")
                                .accept("text/plain"))
                        .andExpect(status().isOk()).andReturn().getResponse();

                assertThat(response).isNotNull();
                assertThat(response.getContentAsString()).isEqualTo("Hello Spring Content World!");
            }

        }

        @Nested
        class GivenAMultipartFormPOSTToAnEntityWithASingleCorrelatedContentProperty {
            @BeforeEach
            void setUp() {
                mvc = MockMvcBuilders.webAppContextSetup(context).build();
            }

            @Test
            void shouldCreateANewEntityAndItsContentAndRespondWithA201Created() throws Exception {
                // assert content does not exist
                String newContent = "This is some new content";

                MockMultipartFile file = new MockMultipartFile("content", "filename.txt", "text/plain", newContent.getBytes());

                // POST the new content
                MockHttpServletResponse response = mvc.perform(multipart("/testEntity9s")
                				.file(file)
                				.param("name", "foo")
                				.param("hidden", "bar"))
                		.andExpect(status().isCreated())
                		.andReturn().getResponse();

                String location = response.getHeader("Location");

                Optional<TestEntity9> fetchedEntity = repo9.findById(Long.valueOf(StringUtils.substringAfterLast(location, "/")));
                assertThat(fetchedEntity.get().getHidden()).isNull();

                // assert entity now exists
                mvc.perform(head(location))
                		.andExpect(status().is2xxSuccessful());

                // assert content now exists
                response = mvc.perform(get(location + "/content")
                				.accept("text/plain"))
                		.andExpect(status().isOk())
                		.andReturn().getResponse();
                assertThat(response.getContentAsString()).isEqualTo(newContent);
            }

        }

        @Nested
        class GivenAMultipartFormPOSTThatDoesnTIncludeTheContentProperty {
            @BeforeEach
            void setUp() {
                mvc = MockMvcBuilders.webAppContextSetup(context).build();
            }

            @Test
            void shouldCreateANewEntityWithNoContentAndRespondWithA201Created() throws Exception {
                var testEntity4Id = repo4.save(new TestEntity4()).getId();

                // POST the entity
                MockHttpServletResponse response = mvc.perform(multipart("/testEntity3s")
                				.param("name", "foo")
                				.param("hidden", "bar")
                				.param("ying", "yang")
                				.param("things", "one", "two")
                				.param("testEntity4", "/testEntity4s/" + testEntity4Id))

                		.andExpect(status().isCreated())
                		.andReturn().getResponse();

                String location = response.getHeader("Location");

                Optional<TestEntity3> fetchedEntity = repo3.findById(Long.valueOf(StringUtils.substringAfterLast(location, "/")));
                assertThat(fetchedEntity.get().getName()).isEqualTo("foo");
                assertThat(fetchedEntity.get().getHidden()).isNull();
                assertThat(fetchedEntity.get().getYang()).isEqualTo("yang");
                assertThat(fetchedEntity.get().getThings()).contains("one", "two");
                assertThat(fetchedEntity.get().getTestEntity4()).isNotNull();
                assertThat(fetchedEntity.get().getContentId()).isNull();
                assertThat(fetchedEntity.get().getLen()).isNull();
                assertThat(fetchedEntity.get().getOriginalFileName()).isNull();
            }

        }

        @Nested
        class GivenAMultipartFormPOSTAndAnEventListener {
            @BeforeEach
            void setUp() {
                mvc = MockMvcBuilders.webAppContextSetup(context).build();

                eventListener.clear();
            }

            @Test
            void shouldCreateANewEntityAndFireTheOnBeforeCreateOnAfterCreateEvents() throws Exception {
                // POST the entity
                MockHttpServletResponse response = mvc.perform(multipart("/testEntity3s")
                				.param("name", "foo foo")
                				.param("hidden", "bar bar")
                				.param("ying", "yang")
                				.param("things", "one", "two"))

                		.andExpect(status().isCreated())
                		.andReturn().getResponse();

                assertThat(eventListener.getBeforeCreate().size()).isEqualTo(1);
                assertThat(eventListener.getAfterCreate().size()).isEqualTo(1);
                assertThat(((TestEntity3) eventListener.getBeforeCreate().get(0)).getName()).isEqualTo("foo foo");
                assertThat(((TestEntity3) eventListener.getAfterCreate().get(0)).getName()).isEqualTo("foo foo");
            }

        }

        @Nested
        class GivenAMultipartFormPOSTButWithTheWrongContentPropertyName {
            @BeforeEach
            void setUp() {
                mvc = MockMvcBuilders.webAppContextSetup(context).build();
            }

            @Test
            void shouldReturnAnErrorAndNotMakeTheEntity() {
                MockMultipartFile file = new MockMultipartFile("oopsDoesntExist", "filename.txt", "text/plain",
                		"foo".getBytes());

                var before = repo3.count();

                // POST the entity
                assertThrows(ServletException.class, () ->
                		mvc.perform(multipart("/testEntity3s")
                						.file(file)
                						.param("name", "foo foo")
                						.param("hidden", "bar bar")
                						.param("ying", "yang")
                						.param("things", "one", "two"))

                				.andExpect(status().isCreated())
                				.andReturn().getResponse()
                );

                assertThat(repo3.count()).isEqualTo(before);
            }

        }

        @Nested
        class GivenAMultipartFormPOSTToAnEntityWithAMappedContentProperty {
            @BeforeEach
            void setUp() {
                mvc = MockMvcBuilders.webAppContextSetup(context).build();
            }

            @Test
            void shouldCreateANewEntityAndItsContentAndRespondWithA201Created() throws Exception {
                // assert content does not exist
                String newContent = "This is some new content";

                MockMultipartFile file = new MockMultipartFile("package/content", "filename.txt", "text/plain", newContent.getBytes());

                // POST the new content
                MockHttpServletResponse response = mvc.perform(multipart("/testEntity11s")
                				.file(file))
                		.andExpect(status().isCreated())
                		.andReturn().getResponse();

                String location = response.getHeader("Location");

                Optional<TestEntity11> fetchedEntity = repo11.findById(Long.valueOf(StringUtils.substringAfterLast(location, "/")));
                assertThat(fetchedEntity.get().get_package().getContentId()).isNotNull();

                // assert entity now exists
                mvc.perform(head(location))
                		.andExpect(status().is2xxSuccessful());

                // assert content now exists
                response = mvc.perform(get(location + "/package/content")
                				.accept("text/plain"))
                		.andExpect(status().isOk())
                		.andReturn().getResponse();
                assertThat(response.getContentAsString()).isEqualTo(newContent);
            }

        }

    }

}
