package it.internal.org.springframework.content.rest.controllers;

import internal.org.springframework.content.rest.support.EmbeddedDualContentEntity;
import internal.org.springframework.content.rest.support.EmbeddedDualContentEntityRepository;
import internal.org.springframework.content.rest.support.EmbeddedDualContentEntityStore;
import internal.org.springframework.content.rest.support.StoreConfig;
import it.internal.org.springframework.content.rest.controllers.support.LastModifiedDateTests;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.rest.config.HypermediaConfiguration;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.content.test.support.TestSupportData;
import org.springframework.core.io.WritableResource;
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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebAppConfiguration
@ContextConfiguration(classes = {
    StoreConfig.class, DelegatingWebMvcConfiguration.class, RepositoryRestMvcConfiguration.class,
    RestConfiguration.class, HypermediaConfiguration.class
})
@Transactional
@ActiveProfiles("store")
@ExtendWith(SpringExtension.class)
public class NestedContentPropertiesRestEndpointsIT {

    @Autowired
    private EmbeddedDualContentEntityRepository repository;
    @Autowired
    private EmbeddedDualContentEntityStore store;

    private EmbeddedDualContentEntity testEntity10;

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @Nested
   class NestedContentPropertiesRESTEndpoints {
        @Nested
        @DisplayName("Given an entity with a simple content property")
        class EntityWithSimpleContentProperty {
            @Nested
            @DisplayName("Given a request to a non existent entity")
            class RequestToNonExistentEntity {
                @BeforeEach
                void setUp() {
                    mvc = MockMvcBuilders.webAppContextSetup(context).build();

                    testEntity10 = repository.save(new EmbeddedDualContentEntity());
                }

                @Test
                void shouldReturn404() throws Exception {
                    mvc.perform(
                            get("/embeddedDualContentEntities/9999999/foo"))
                        .andExpect(status().isNotFound());
                }
            }

            @Nested

            @DisplayName("Given a request to a non existent content property")

            class RequestToNonExistentContentProperty {
                @BeforeEach
                void setUp() {
                    mvc = MockMvcBuilders.webAppContextSetup(context).build();

                    testEntity10 = repository.save(new EmbeddedDualContentEntity());
                }

                @Test
                void shouldReturn404() throws Exception {
                    mvc.perform(
                            get("/embeddedDualContentEntities/" + testEntity10.getId() + "/doesnotexist"))
                        .andExpect(status().isNotFound());
                }
            }

            @Nested

            @DisplayName("Given that is has no content")

            class IsHasNoContent {
                @Nested
                @DisplayName("GET to repository id content property")
                class GetToRepositoryIdContentProperty {
                    @BeforeEach
                    void setUp() {
                        mvc = MockMvcBuilders.webAppContextSetup(context).build();

                        testEntity10 = repository.save(new EmbeddedDualContentEntity());
                    }

                    @Test
                    void shouldReturn404() throws Exception {
                        mvc.perform(
                                get("/embeddedDualContentEntities/" + testEntity10.getId() + "/child"))
                            .andExpect(status().isNotFound());
                    }
                }

                @Nested

                @DisplayName("PUT to repository id property content property")

                class PutToRepositoryIdPropertyContentProperty {
                    @BeforeEach
                    void setUp() {
                        mvc = MockMvcBuilders.webAppContextSetup(context).build();

                        testEntity10 = repository.save(new EmbeddedDualContentEntity());
                    }

                    @Test
                    void shouldCreateTheContent() throws Exception {
                        mvc.perform(
                                put("/embeddedDualContentEntities/" + testEntity10.getId() + "/child/content")
                                    .content("Hello New Spring Content World!")
                                    .contentType("text/plain"))
                            .andExpect(status().is2xxSuccessful());

                        Optional<EmbeddedDualContentEntity> fetched = repository.findById(testEntity10.getId());
                        assertThat(fetched.isPresent()).isTrue();
                        assertThat(fetched.get().getChild().contentId).isNotNull();
                        assertThat(fetched.get().getChild().contentLen).isEqualTo(31L);
                        assertThat(fetched.get().getChild().contentMimeType).isEqualTo("text/plain");
                        try (InputStream actual = store.getResource(fetched.get(), PropertyPath.from("child/content")).getInputStream()) {
                            IOUtils.contentEquals(actual, new ByteArrayInputStream("Hello New Spring Content World!".getBytes()));
                        }

                        mvc.perform(
                                put("/embeddedDualContentEntities/" + testEntity10.getId() + "/child/preview")
                                    .content("Hello New Spring Content Preview World!")
                                    .contentType("text/plain"))
                            .andExpect(status().is2xxSuccessful());

                        fetched = repository.findById(testEntity10.getId());
                        assertThat(fetched.isPresent()).isTrue();
                        assertThat(fetched.get().getChild().getPreviewId()).isNotNull();
                        assertThat(fetched.get().getChild().getPreviewLen()).isEqualTo(39L);
                        assertThat(fetched.get().getChild().getPreviewMimeType()).isEqualTo("text/plain");
                        try (InputStream actual = store.getResource(fetched.get(), PropertyPath.from("child/preview")).getInputStream()) {
                            IOUtils.contentEquals(actual, new ByteArrayInputStream("Hello New Spring Content Preview World!".getBytes()));
                        }
                    }
                }

                @Nested

                @DisplayName("PUT to store ID property content property with json content")

                class PutToStoreIdPropertyContentPropertyWithJsonContent {
                    @BeforeEach
                    void setUp() {
                        mvc = MockMvcBuilders.webAppContextSetup(context).build();

                        testEntity10 = repository.save(new EmbeddedDualContentEntity());
                    }

                    @Test
                    void shouldSetTheContentAndReturn201() throws Exception {
                        String content = "{\"content\":\"Hello New Spring Content World!\"}";
                        mvc.perform(
                                put("/embeddedDualContentEntities/" + testEntity10.getId() + "/child/content")
                                    .content(content)
                                    .contentType("application/json"))
                            .andExpect(status().isCreated());

                        Optional<EmbeddedDualContentEntity> fetched = repository.findById(testEntity10.getId());
                        assertThat(fetched.isPresent()).isTrue();
                        assertThat(fetched.get().getChild().getContentId()).isNotNull();
                        assertThat(fetched.get().getChild().getContentLen()).isEqualTo(45L);
                        assertThat(fetched.get().getChild().getContentMimeType()).isEqualTo("application/json");
                        try (InputStream actual = store.getResource(fetched.get(), PropertyPath.from("child/content")).getInputStream()) {
                            IOUtils.contentEquals(actual, new ByteArrayInputStream(content.getBytes()));
                        }
                    }
                }
            }

            @Nested

            @DisplayName("Given that it has content")

            class ItHasContent {
                @Nested
                @DisplayName("GET to repository id content property")
                class GetToRepositoryIdContentProperty {
                    @BeforeEach
                    void setUp() throws IOException {
                        mvc = MockMvcBuilders.webAppContextSetup(context).build();

                        testEntity10 = repository.save(new EmbeddedDualContentEntity());

                        String content = TestSupportData.SAMPLE_TEXT_PAYLOAD;
                        testEntity10.getChild().contentMimeType = "text/plain";
                        UUID contentId = UUID.randomUUID();
                        store.associate(testEntity10, PropertyPath.from("child/content"), contentId);
                        WritableResource r = (WritableResource) store.getResource(testEntity10, PropertyPath.from("child/content"));
                        try (OutputStream out = r.getOutputStream()) {
                            out.write(content.getBytes());
                        }
                        testEntity10 = repository.save(testEntity10);
                    }

                    @Test
                    void shouldReturnTheContent() throws Exception {
                        MockHttpServletResponse response = mvc
                            .perform(get("/embeddedDualContentEntities/" + testEntity10.getId() + "/child/content")
                                .accept("text/plain"))
                            .andExpect(status().isOk())
                            .andExpect(header().string("etag", is("\"1\"")))
                            .andExpect(header().string("last-modified", LastModifiedDateTests
                                .isWithinASecond(testEntity10.getModifiedDate())))
                            .andReturn().getResponse();

                        assertThat(response).isNotNull();
                        assertThat(response.getContentAsString()).isEqualTo(TestSupportData.SAMPLE_TEXT_PAYLOAD);
                    }
                }

                @Nested

                @DisplayName("GET to repository id content property with a MIME type that matches a renderer")

                class GetToRepositoryIdContentPropertyWithMimeTypeThatMatchesRenderer {
                    @BeforeEach
                    void setUp() throws IOException {
                        mvc = MockMvcBuilders.webAppContextSetup(context).build();

                        testEntity10 = repository.save(new EmbeddedDualContentEntity());

                        String content = TestSupportData.SAMPLE_TEXT_PAYLOAD;
                        testEntity10.getChild().contentMimeType = "text/plain";
                        UUID contentId = UUID.randomUUID();
                        store.associate(testEntity10, PropertyPath.from("child/content"), contentId);
                        WritableResource r = (WritableResource) store.getResource(testEntity10, PropertyPath.from("child/content"));
                        try (OutputStream out = r.getOutputStream()) {
                            out.write(content.getBytes());
                        }
                        testEntity10 = repository.save(testEntity10);
                    }

                    @Test
                    void shouldReturnTheRenditionAnd200() throws Exception {
                        MockHttpServletResponse response = mvc
                            .perform(get(
                                "/embeddedDualContentEntities/" + testEntity10.getId()
                                    + "/child/content")
                                .accept("text/html"))
                            .andExpect(status().isOk()).andReturn()
                            .getResponse();

                        assertThat(response).isNotNull();
                        assertThat(response.getContentAsString()).isEqualTo(TestSupportData.SAMPLE_REST_HTML_RENDITION_BODY);
                    }
                }

                @Nested

                @DisplayName("GET to repository ID content property with multiple MIME types the last of which matches content")

                class GetToRepositoryIdWhenLastAcceptedMimeTypeMatches {
                    @BeforeEach
                    void setUp() throws IOException {
                        mvc = MockMvcBuilders.webAppContextSetup(context).build();

                        testEntity10 = repository.save(new EmbeddedDualContentEntity());

                        String content = TestSupportData.SAMPLE_TEXT_PAYLOAD;
                        testEntity10.getChild().contentMimeType = "text/plain";
                        UUID contentId = UUID.randomUUID();
                        store.associate(testEntity10, PropertyPath.from("child/content"), contentId);
                        WritableResource r = (WritableResource) store.getResource(testEntity10, PropertyPath.from("child/content"));
                        try (OutputStream out = r.getOutputStream()) {
                            out.write(content.getBytes());
                        }
                        testEntity10 = repository.save(testEntity10);
                    }

                    @Test
                    void shouldReturnTheOriginalContentAnd200() throws Exception {
                        MockHttpServletResponse response = mvc
                            .perform(get("/embeddedDualContentEntities/"
                                + testEntity10.getId()
                                + "/child/content").accept(
                                "text/xml",
                                "text/plain"))
                            .andExpect(status().isOk()).andReturn()
                            .getResponse();

                        assertThat(response).isNotNull();
                        assertThat(response.getContentAsString()).isEqualTo(TestSupportData.SAMPLE_TEXT_PAYLOAD);
                    }
                }

                @Nested

                @DisplayName("PUT to repository id content property")

                class PutToRepositoryIdContentProperty {
                    @BeforeEach
                    void setUp() throws IOException {
                        mvc = MockMvcBuilders.webAppContextSetup(context).build();

                        testEntity10 = repository.save(new EmbeddedDualContentEntity());

                        String content = TestSupportData.SAMPLE_TEXT_PAYLOAD;
                        testEntity10.getChild().contentMimeType = "text/plain";
                        UUID contentId = UUID.randomUUID();
                        store.associate(testEntity10, PropertyPath.from("child/content"), contentId);
                        WritableResource r = (WritableResource) store.getResource(testEntity10, PropertyPath.from("child/content"));
                        try (OutputStream out = r.getOutputStream()) {
                            out.write(content.getBytes());
                        }
                        testEntity10 = repository.save(testEntity10);
                    }

                    @Test
                    void shouldCreateTheContent() throws Exception {
                        mvc.perform(
                                put("/embeddedDualContentEntities/" + testEntity10.getId() + "/child/content")
                                    .content("Hello New Spring Content World!")
                                    .contentType("text/plain"))
                            .andExpect(status().is2xxSuccessful());

                        Optional<EmbeddedDualContentEntity> fetched = repository
                            .findById(testEntity10.getId());
                        assertThat(fetched.isPresent()).isTrue();
                        assertThat(fetched.get().getChild().contentId).isNotNull();
                        assertThat(fetched.get().getChild().contentLen).isEqualTo(31L);
                        assertThat(fetched.get().getChild().contentMimeType).isEqualTo("text/plain");
                    }
                }

                @Nested

                @DisplayName("DELETE to repository id content property")

                class DeleteToRepositoryIdContentProperty {
                    @BeforeEach
                    void setUp() throws IOException {
                        mvc = MockMvcBuilders.webAppContextSetup(context).build();

                        testEntity10 = repository.save(new EmbeddedDualContentEntity());

                        String content = TestSupportData.SAMPLE_TEXT_PAYLOAD;
                        testEntity10.getChild().contentMimeType = "text/plain";
                        UUID contentId = UUID.randomUUID();
                        store.associate(testEntity10, PropertyPath.from("child/content"), contentId);
                        WritableResource r = (WritableResource) store.getResource(testEntity10, PropertyPath.from("child/content"));
                        try (OutputStream out = r.getOutputStream()) {
                            out.write(content.getBytes());
                        }
                        testEntity10 = repository.save(testEntity10);
                    }

                    @Test
                    void shouldDeleteTheContent() throws Exception {
                        mvc.perform(delete(
                                "/embeddedDualContentEntities/" + testEntity10.getId() + "/child/content"))
                            .andExpect(status().isNoContent());

                        Optional<EmbeddedDualContentEntity> fetched = repository.findById(testEntity10.getId());
                        assertThat(fetched.isPresent()).isTrue();
                        assertThat(fetched.get().getChild().contentId).isNull();
                        assertThat(fetched.get().getChild().contentLen).isNull();
                        assertThat(fetched.get().getChild().contentMimeType).isNull();
                    }
                }
            }
        }

        @Nested

        @DisplayName("Given apost to the entity endpoint with a multipart form request")

        class PostToEntityEndpointWithMultipartFormRequest {
            @BeforeEach
            void setUp() {
                mvc = MockMvcBuilders.webAppContextSetup(context).build();
            }

            @Test
            void shouldCreateANewEntityAndItsContentAndRespondWithA201Created() throws Exception {
                // assert content does not exist
                String content = "This is some new content";
                String previewContent = "This is some new preview content";

                MockMultipartFile file1 = new MockMultipartFile("child/content", "filename.txt", "text/plain", content.getBytes());
                MockMultipartFile file2 = new MockMultipartFile("child/preview", "preview.txt", "text/plain", previewContent.getBytes());

                // POST the new content
                MockHttpServletResponse response = mvc.perform(multipart("/embeddedDualContentEntities")
                        .file(file1)
                        .file(file2)
                        .param("name", "foo"))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse();

                String location = response.getHeader("Location");

                // assert that it now exists
                response = mvc.perform(get(location + "/child/content")
                        .accept("text/plain"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse();

                assertThat(response.getContentAsString()).isEqualTo(content);

                response = mvc.perform(get(location + "/child/preview")
                        .accept("text/plain"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse();

                assertThat(response.getContentAsString()).isEqualTo(previewContent);
            }
        }
    }
}
