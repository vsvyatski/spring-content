package it.internal.org.springframework.content.rest.controllers;

import internal.org.springframework.content.rest.support.*;
import internal.org.springframework.content.rest.support.EventListenerConfig.TestEventListener;
import it.internal.org.springframework.content.rest.controllers.support.LastModifiedDateTests;
import it.internal.org.springframework.content.rest.controllers.support.VersionTests;
import jakarta.servlet.ServletException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.rest.config.HypermediaConfiguration;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.content.test.support.TestSupportData;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.util.Optional;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
    UuidSingleContentEntityRepository repository;

    // same exported URI
    @Autowired
    JsonMappedContentEntityRepository repo3;

    // same exported URI
    @Autowired
    OneToOneLinkedContentEntityRepository repo4;
    @Autowired
    OneToOneLinkedContentEntityStore store4;

    // shared @Id/@ContentId
    @Autowired
    SharedUuidContentIdEntityRepository repo6;

    // single content property, correlated attributes
    @Autowired
    JsonIgnoredContentEntityRepository repo9;
    @Autowired
    JsonIgnoredContentEntityStore store9;

    // mapped content property
    @Autowired
    RestResourcePackageEntityRepository repo11;

    @Autowired
    TestEventListener eventListener;

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    private UuidSingleContentEntity testEntity;
    private JsonMappedContentEntity testEntity3;
    private OneToOneLinkedContentEntity testEntity4;
    private SharedUuidContentIdEntity testEntity6;
    private JsonIgnoredContentEntity testEntity9;

    @Nested
    class ContentEntityRESTEndpoints {
        @Nested
        class EntityWithSingleUncorrelatedContentProperties {
            @Nested
            class RepositoryAndStorageAreExportedToSameURI {
                @Nested
                @DisplayName("DELETE to /softDelete custom handler")
                class DeleteCustomHandler {
                    @BeforeEach
                    void setUp() {
                        mvc = MockMvcBuilders.webAppContextSetup(context).build();

                        testEntity3 = repo3.save(new JsonMappedContentEntity());
                        testEntity3.name = "tests";
                        testEntity3 = repo3.save(testEntity3);
                    }

                    @Test
                    void shouldReturn200() throws Exception {
                        mvc.perform(delete("/jsonMappedContentEntities/" + testEntity3.id + "/softDelete"))
                            .andExpect(status().is2xxSuccessful());
                    }
                }
            }

            @Nested
            class RepositoryAndStorageAreExportedToDifferentURIs {
                @Nested
                class OptionsFromKnownHost {
                    @BeforeEach
                    void setUp() {
                        mvc = MockMvcBuilders.webAppContextSetup(context).build();

                        testEntity = repository.save(new UuidSingleContentEntity());
                    }

                    @Test
                    void shouldReturnRelevantCORSHeadersAndOK() throws Exception {
                        mvc.perform(options("/uuidSingleContentEntities/" + testEntity.getId())
                                .header("Access-Control-Request-Method", "PUT")
                                .header("Origin", "http://www.someurl.com"))
                            .andExpect(status().isOk())
                            .andExpect(header().string("Access-Control-Allow-Origin", "http://www.someurl.com"));
                    }
                }
            }

            @Nested
            @DisplayName("Given an entity with @Version")
            class EntityWithVersion extends VersionTests {
                @BeforeEach
                void setUp() {
                    mvc = MockMvcBuilders.webAppContextSetup(context).build();
                    testEntity4 = new OneToOneLinkedContentEntity();
                    testEntity4 = store4.setContent(testEntity4, TestSupportData.sampleTextStream());
                    testEntity4.mimeType = "text/plain";
                    testEntity4 = repo4.save(testEntity4);
                    String url = "/oneToOneLinkedContentEntities/" + testEntity4.getId();
                    this.setMvc(mvc);
                    this.setUrl(url);
                    this.setCollectionUrl("/oneToOneLinkedContentEntities");
                    this.setContentLinkRel("content");
                    this.setRepo(repo4);
                    this.setStore(store4);
                    this.setEtag(format("\"%s\"", testEntity4.getVersion()));
                    this.setEntity(testEntity4);
                }
            }

            @Nested
            @DisplayName("Given an entity with @LastModifiedDate")
            class EntityWithLastModifiedDate extends LastModifiedDateTests {
                @BeforeEach
                void setUp() {
                    mvc = MockMvcBuilders.webAppContextSetup(context).build();
                    String content = TestSupportData.SAMPLE_LAST_MODIFIED_DATE_PAYLOAD;
                    testEntity4 = new OneToOneLinkedContentEntity();
                    testEntity4 = store4.setContent(testEntity4, new ByteArrayInputStream(content.getBytes()));
                    testEntity4.mimeType = "text/plain";
                    testEntity4 = repo4.save(testEntity4);
                    String url = "/oneToOneLinkedContentEntities/" + testEntity4.getId();
                    this.setMvc(mvc);
                    this.setUrl(url);
                    this.setLastModifiedDate(testEntity4.getModifiedDate());
                    this.setEtag(testEntity4.getVersion().toString());
                    this.setContent(content);
                }
            }

            @Nested
            class EntityWithSharedIdAndContentIdField {
                @BeforeEach
                void setUp() {
                    mvc = MockMvcBuilders.webAppContextSetup(context).build();

                    testEntity6 = new SharedUuidContentIdEntity();
                    testEntity6 = repo6.save(testEntity6);
                }

                @Test
                void shouldReturn404WhenNoContentIsSet() throws Exception {
                    mvc.perform(get("/sharedUuidContentIdEntities/" + testEntity6.getId())
                            .accept("text/plain"))
                        .andExpect(status().isNotFound());
                }
            }
        }

        @Nested
        class PostMultipartSingleUncorrelatedContentProperty {
            @BeforeEach
            void setUp() {
                mvc = MockMvcBuilders.webAppContextSetup(context).build();
            }

            @Test
            void shouldCreateNewEntityAndItsContentAndRespondWith201Created() throws Exception {
                // assert content does not exist
                String newContent = "This is some new content";

                MockMultipartFile file = new MockMultipartFile("content", "filename.txt", "text/plain", newContent.getBytes());

                var testEntity4Id = repo4.save(new OneToOneLinkedContentEntity()).getId();

                // POST the new content
                MockHttpServletResponse response = mvc.perform(multipart("/jsonMappedContentEntities")
                        .file(file)
                        .contentType("multipart/form-data; boundary=c0de8278")
                        .param("name", "foo")
                        .param("hidden", "bar")
                        .param("ying", "yang")
                        .param("things", "one", "two")
                        .param("testEntity4", "/oneToOneLinkedContentEntities/" + testEntity4Id))

                    .andExpect(status().isCreated())
                    .andReturn().getResponse();

                String location = response.getHeader("Location");
                assertThat(location).isNotNull();

                Optional<JsonMappedContentEntity> fetchedEntity = repo3.findById(
                    Long.valueOf(StringUtils.substringAfterLast(location, "/")));
                assertThat(fetchedEntity).isPresent();
                JsonMappedContentEntity saved = fetchedEntity.orElseThrow();
                assertThat(saved.getName()).isEqualTo("foo");
                assertThat(saved.getHidden()).isNull();
                assertThat(saved.getYang()).isEqualTo("yang");
                assertThat(saved.getThings()).contains("one", "two");
                assertThat(saved.getLinkedContentEntity()).isNotNull();
                assertThat(saved.getLen()).isEqualTo(file.getSize());
                assertThat(saved.getOriginalFileName()).isEqualTo(file.getOriginalFilename());

                // assert that it now exists
                response = mvc.perform(get(location)
                        .accept("text/plain"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse();

                assertThat(response.getContentAsString()).isEqualTo(newContent);
            }
        }

        @Nested
        class PostMultipartNonDefaultInitializedVersionProperty {
            @Nested
            class WithContent {
                @BeforeEach
                void setUp() {
                    mvc = MockMvcBuilders.webAppContextSetup(context).build();
                }

                @Test
                void shouldCreateNewEntityAndItsContentAndRespondWith201Created() throws Exception {
                    String newContent = "This is some new content";

                    MockMultipartFile file = new MockMultipartFile("content", "filename.txt", "text/plain",
                        newContent.getBytes());

                    // POST the entity
                    MockHttpServletResponse response = mvc.perform(multipart("/oneToOneLinkedContentEntities")
                            .file(file)
                            .param("name", "foo")
                            .param("title", "bar"))

                        .andExpect(status().isCreated())
                        .andReturn().getResponse();

                    String location = response.getHeader("Location");
                    assertThat(location).isNotNull();

                    Optional<OneToOneLinkedContentEntity> fetchedEntity = repo4.findById(
                        Long.valueOf(StringUtils.substringAfterLast(location, "/")));
                    assertThat(fetchedEntity).isPresent();
                    OneToOneLinkedContentEntity saved = fetchedEntity.orElseThrow();
                    assertThat(saved.getName()).isEqualTo("foo");
                    assertThat(saved.getTitle()).isEqualTo("bar");
                    assertThat(saved.getContentId()).isNotNull();
                    assertThat(saved.getLen()).isEqualTo(file.getSize());
                    assertThat(saved.getOriginalFileName()).isEqualTo(file.getOriginalFilename());

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
                void shouldCreateNewEntityAndRespondWith201Created() throws Exception {
                    // POST the entity
                    MockHttpServletResponse response = mvc.perform(multipart("/oneToOneLinkedContentEntities")
                            .param("name", "foo")
                            .param("title", "bar"))

                        .andExpect(status().isCreated())
                        .andReturn().getResponse();

                    String location = response.getHeader("Location");
                    assertThat(location).isNotNull();

                    Optional<OneToOneLinkedContentEntity> fetchedEntity = repo4.findById(
                        Long.valueOf(StringUtils.substringAfterLast(location, "/")));
                    assertThat(fetchedEntity).isPresent();
                    OneToOneLinkedContentEntity saved = fetchedEntity.orElseThrow();
                    assertThat(saved.getName()).isEqualTo("foo");
                    assertThat(saved.getTitle()).isEqualTo("bar");
                    assertThat(saved.getContentId()).isNull();
                    assertThat(saved.getLen()).isNull();
                    assertThat(saved.getOriginalFileName()).isNull();
                }
            }
        }

        @Nested
        class EntityWithSingleCorrelatedContentProperty {
            @BeforeEach
            void setUp() {
                mvc = MockMvcBuilders.webAppContextSetup(context).build();

                testEntity9 = repo9.save(new JsonIgnoredContentEntity());
            }

            @Test
            void shouldSupportContentOperations() throws Exception {
                String content = TestSupportData.SAMPLE_TEXT_PAYLOAD;
                mvc.perform(
                        put("/jsonIgnoredContentEntities/" + testEntity9.id)
                            .contextPath("")
                            .content(content)
                            .contentType("text/plain"))
                    .andExpect(status().isCreated());

                Optional<JsonIgnoredContentEntity> fetched = repo9.findById(testEntity9.getId());
                assertThat(fetched).isPresent();
                JsonIgnoredContentEntity saved = fetched.orElseThrow();
                assertThat(saved.getContentId()).isNotNull();
                assertThat(saved.getContentLen()).isEqualTo(27L);
                assertThat(saved.getContentMimeType()).isEqualTo("text/plain");
                assertThat(IOUtils.toString(store9.getContent(saved), Charset.defaultCharset())).isEqualTo(content);

                MockHttpServletResponse response = mvc
                    .perform(get("/jsonIgnoredContentEntities/" + testEntity9.id)
                        .contextPath("")
                        .accept("text/plain"))
                    .andExpect(status().isOk()).andReturn().getResponse();

                assertThat(response).isNotNull();
                assertThat(response.getContentAsString()).isEqualTo(TestSupportData.SAMPLE_TEXT_PAYLOAD);
            }
        }

        @Nested
        class PostMultipartSingleCorrelatedContentProperty {
            @BeforeEach
            void setUp() {
                mvc = MockMvcBuilders.webAppContextSetup(context).build();
            }

            @Test
            void shouldCreateNewEntityAndItsContentAndRespondWith201Created() throws Exception {
                // assert content does not exist
                String newContent = "This is some new content";

                MockMultipartFile file = new MockMultipartFile("content", "filename.txt", "text/plain", newContent.getBytes());

                // POST the new content
                MockHttpServletResponse response = mvc.perform(multipart("/jsonIgnoredContentEntities")
                        .file(file)
                        .param("name", "foo")
                        .param("hidden", "bar"))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse();

                String location = response.getHeader("Location");
                assertThat(location).isNotNull();

                Optional<JsonIgnoredContentEntity> fetchedEntity = repo9.findById(
                    Long.valueOf(StringUtils.substringAfterLast(location, "/")));
                assertThat(fetchedEntity).isPresent();
                assertThat(fetchedEntity.orElseThrow().getHidden()).isNull();

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
        class PostMultipartWithoutContentProperty {
            @BeforeEach
            void setUp() {
                mvc = MockMvcBuilders.webAppContextSetup(context).build();
            }

            @Test
            void shouldCreateNewEntityWithNoContentAndRespondWith201Created() throws Exception {
                var testEntity4Id = repo4.save(new OneToOneLinkedContentEntity()).getId();

                // POST the entity
                MockHttpServletResponse response = mvc.perform(multipart("/jsonMappedContentEntities")
                        .param("name", "foo")
                        .param("hidden", "bar")
                        .param("ying", "yang")
                        .param("things", "one", "two")
                        .param("testEntity4", "/oneToOneLinkedContentEntities/" + testEntity4Id))

                    .andExpect(status().isCreated())
                    .andReturn().getResponse();

                String location = response.getHeader("Location");
                assertThat(location).isNotNull();

                Optional<JsonMappedContentEntity> fetchedEntity = repo3.findById(
                    Long.valueOf(StringUtils.substringAfterLast(location, "/")));
                assertThat(fetchedEntity).isPresent();
                JsonMappedContentEntity saved = fetchedEntity.orElseThrow();
                assertThat(saved.getName()).isEqualTo("foo");
                assertThat(saved.getHidden()).isNull();
                assertThat(saved.getYang()).isEqualTo("yang");
                assertThat(saved.getThings()).contains("one", "two");
                assertThat(saved.getLinkedContentEntity()).isNotNull();
                assertThat(saved.getContentId()).isNull();
                assertThat(saved.getLen()).isNull();
                assertThat(saved.getOriginalFileName()).isNull();
            }
        }

        @Nested
        class PostMultipartWithEventListener {
            @BeforeEach
            void setUp() {
                mvc = MockMvcBuilders.webAppContextSetup(context).build();

                eventListener.clear();
            }

            @Test
            void shouldCreateNewEntityAndFireOnBeforeCreateOnAfterCreateEvents() throws Exception {
                // POST the entity
                mvc.perform(multipart("/jsonMappedContentEntities")
                        .param("name", "foo foo")
                        .param("hidden", "bar bar")
                        .param("ying", "yang")
                        .param("things", "one", "two"))
                    .andExpect(status().isCreated());

                assertThat(eventListener.getBeforeCreate().size()).isEqualTo(1);
                assertThat(eventListener.getAfterCreate().size()).isEqualTo(1);
                assertThat(((JsonMappedContentEntity) eventListener.getBeforeCreate().get(0)).getName()).isEqualTo("foo foo");
                assertThat(((JsonMappedContentEntity) eventListener.getAfterCreate().get(0)).getName()).isEqualTo("foo foo");
            }
        }

        @Nested
        class PostMultipartWithWrongContentPropertyName {
            @BeforeEach
            void setUp() {
                mvc = MockMvcBuilders.webAppContextSetup(context).build();
            }

            @Test
            void shouldReturnErrorAndNotMakeEntity() {
                MockMultipartFile file = new MockMultipartFile("oopsDoesntExist", "filename.txt", "text/plain",
                    "foo".getBytes());

                var before = repo3.count();

                // POST the entity
                assertThrows(ServletException.class, () ->
                    mvc.perform(multipart("/jsonMappedContentEntities")
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
        class PostMultipartMappedContentProperty {
            @BeforeEach
            void setUp() {
                mvc = MockMvcBuilders.webAppContextSetup(context).build();
            }

            @Test
            void shouldCreateNewEntityAndItsContentAndRespondWith201Created() throws Exception {
                // assert content does not exist
                String newContent = "This is some new content";

                MockMultipartFile file = new MockMultipartFile("package/content", "filename.txt", "text/plain", newContent.getBytes());

                // POST the new content
                MockHttpServletResponse response = mvc.perform(multipart("/restResourcePackageEntities")
                        .file(file))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse();

                String location = response.getHeader("Location");
                assertThat(location).isNotNull();

                Optional<RestResourcePackageEntity> fetchedEntity = repo11.findById(
                    Long.valueOf(StringUtils.substringAfterLast(location, "/")));
                assertThat(fetchedEntity).isPresent();
                assertThat(fetchedEntity.orElseThrow().getContentPackage().getContentId()).isNotNull();

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
