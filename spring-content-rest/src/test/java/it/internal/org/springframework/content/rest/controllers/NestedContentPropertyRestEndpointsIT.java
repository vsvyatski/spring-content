package it.internal.org.springframework.content.rest.controllers;

import internal.org.springframework.content.rest.support.EmbeddedChildContentEntity;
import internal.org.springframework.content.rest.support.EmbeddedChildContentEntityRepository;
import internal.org.springframework.content.rest.support.EmbeddedChildContentEntityStore;
import internal.org.springframework.content.rest.support.StoreConfig;
import it.internal.org.springframework.content.rest.controllers.support.LastModifiedDateTests;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
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
    StoreConfig.class,
    DelegatingWebMvcConfiguration.class,
    RepositoryRestMvcConfiguration.class,
    RestConfiguration.class,
    HypermediaConfiguration.class})
@Transactional
@ActiveProfiles("store")
@ExtendWith(SpringExtension.class)
public class NestedContentPropertyRestEndpointsIT {

    @Autowired
    private EmbeddedChildContentEntityRepository repository2;
    @Autowired
    private EmbeddedChildContentEntityStore store;

    private EmbeddedChildContentEntity testEntity8;

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @Nested
    class NestedContentPropertyRESTEndpoints {
        @Nested
        class GivenAnEntityWithASimpleContentProperty {
            @Nested
            class GivenARequestToANonExistentEntity {
                @BeforeEach
                void setUp() {
                    mvc = MockMvcBuilders.webAppContextSetup(context).build();

                    testEntity8 = repository2.save(new EmbeddedChildContentEntity());
                }

                @Test
                void shouldReturn404() throws Exception {
                    mvc.perform(
                            get("/embeddedChildContentEntities/9999999/foo"))
                        .andExpect(status().isNotFound());
                }
            }

            @Nested
            class GivenAPOSTToTheEntityEndpointWithAMultipartFormRequest {
                @BeforeEach
                void setUp() {
                    mvc = MockMvcBuilders.webAppContextSetup(context).build();

                    testEntity8 = repository2.save(new EmbeddedChildContentEntity());
                }

                @Test
                void shouldCreateANewEntityAndItsContentAndRespondWithA201Created() throws Exception {
                    // assert content does not exist
                    String newContent = "This is some new content";

                    MockMultipartFile file = new MockMultipartFile("child", "filename.txt", "text/plain", newContent.getBytes());

                    // POST the new content
                    MockHttpServletResponse response = mvc.perform(multipart("/embeddedChildContentEntities")
                            .file(file)
                            .param("name", "foo"))
                        .andExpect(status().isCreated())
                        .andReturn().getResponse();

                    String location = response.getHeader("Location");

                    // assert that it now exists
                    response = mvc.perform(get(location + "/child")
                            .accept("text/plain"))
                        .andExpect(status().isOk())
                        .andReturn().getResponse();

                    assertThat(response.getContentAsString()).isEqualTo(newContent);
                }
            }

            @Nested
            class GivenARequestToANonExistentContentProperty {
                @BeforeEach
                void setUp() {
                    mvc = MockMvcBuilders.webAppContextSetup(context).build();

                    testEntity8 = repository2.save(new EmbeddedChildContentEntity());
                }

                @Test
                void shouldReturn404() throws Exception {
                    mvc.perform(
                            get("/embeddedChildContentEntities/" + testEntity8.getId() + "/doesnotexist"))
                        .andExpect(status().isNotFound());
                }
            }

            @Nested
            class GivenThatIsHasNoContent {
                @Nested
                class AGETToRepositoryIdContentProperty {
                    @BeforeEach
                    void setUp() {
                        mvc = MockMvcBuilders.webAppContextSetup(context).build();

                        testEntity8 = repository2.save(new EmbeddedChildContentEntity());
                    }

                    @Test
                    void shouldReturn404() throws Exception {
                        mvc.perform(
                                get("/embeddedChildContentEntities/" + testEntity8.getId() + "/child"))
                            .andExpect(status().isNotFound());
                    }
                }

                @Nested
                class APUTToRepositoryIdContentProperty {
                    @BeforeEach
                    void setUp() {
                        mvc = MockMvcBuilders.webAppContextSetup(context).build();

                        testEntity8 = repository2.save(new EmbeddedChildContentEntity());
                    }

                    @Test
                    void shouldCreateTheContent() throws Exception {
                        mvc.perform(
                                put("/embeddedChildContentEntities/" + testEntity8.getId() + "/child")
                                    .content("Hello New Spring Content World!")
                                    .contentType("text/plain"))
                            .andExpect(status().is2xxSuccessful());

                        Optional<EmbeddedChildContentEntity> fetched = repository2.findById(testEntity8.getId());
                        assertThat(fetched.isPresent()).isTrue();
                        assertThat(fetched.get().getChild().contentId).isNotNull();
                        assertThat(fetched.get().getChild().contentLen).isEqualTo(31L);
                        assertThat(fetched.get().getChild().contentMimeType).isEqualTo("text/plain");
                        try (InputStream actual = store.getResource(fetched.get(), PropertyPath.from("child")).getInputStream()) {
                            IOUtils.contentEquals(actual, new ByteArrayInputStream("Hello New Spring Content World!".getBytes()));
                        }
                    }
                }

                @Nested
                class APUTToStoreIdWithJsonContent {
                    @BeforeEach
                    void setUp() {
                        mvc = MockMvcBuilders.webAppContextSetup(context).build();

                        testEntity8 = repository2.save(new EmbeddedChildContentEntity());
                    }

                    @Test
                    void shouldSetTheContentAndReturn201() throws Exception {
                        String content = "{\"content\":\"Hello New Spring Content World!\"}";
                        mvc.perform(
                                put("/embeddedChildContentEntities/" + testEntity8.getId() + "/child")
                                    .content(content)
                                    .contentType("application/json"))
                            .andExpect(status().isCreated());

                        Optional<EmbeddedChildContentEntity> fetched = repository2.findById(testEntity8.getId());
                        assertThat(fetched.isPresent()).isTrue();
                        assertThat(fetched.get().getChild().getContentId()).isNotNull();
                        assertThat(fetched.get().getChild().getContentLen()).isEqualTo(45L);
                        assertThat(fetched.get().getChild().getContentMimeType()).isEqualTo("application/json");
                        try (InputStream actual = store.getResource(fetched.get(), PropertyPath.from("child")).getInputStream()) {
                            IOUtils.contentEquals(actual, new ByteArrayInputStream(content.getBytes()));
                        }
                    }
                }
            }

            @Nested
            class GivenThatItHasContent {
                @Nested
                class AGETToRepositoryIdContentProperty {
                    @BeforeEach
                    void setUp() throws IOException {
                        mvc = MockMvcBuilders.webAppContextSetup(context).build();

                        testEntity8 = repository2.save(new EmbeddedChildContentEntity());

                        String content = TestSupportData.SAMPLE_TEXT_PAYLOAD;
                        testEntity8.getChild().contentMimeType = "text/plain";
                        UUID contentId = UUID.randomUUID();
                        store.associate(testEntity8, PropertyPath.from("child"), contentId);
                        WritableResource r = (WritableResource) store.getResource(testEntity8, PropertyPath.from("child"));
                        try (OutputStream out = r.getOutputStream()) {
                            out.write(content.getBytes());
                        }
                        testEntity8 = repository2.save(testEntity8);
                    }

                    @Test
                    void shouldReturnTheContent() throws Exception {
                        MockHttpServletResponse response = mvc
                            .perform(get("/embeddedChildContentEntities/" + testEntity8.getId() + "/child")
                                .accept("text/plain"))
                            .andExpect(status().isOk())
                            .andExpect(header().string("etag", is("\"1\"")))
                            .andExpect(header().string("last-modified", LastModifiedDateTests
                                .isWithinASecond(testEntity8.getModifiedDate())))
                            .andReturn().getResponse();

                        assertThat(response).isNotNull();
                        assertThat(response.getContentAsString()).isEqualTo(TestSupportData.SAMPLE_TEXT_PAYLOAD);
                    }
                }

                @Nested
                class AGETToRepositoryIdContentPropertyWithAMimeTypeThatMatchesARenderer {
                    @BeforeEach
                    void setUp() throws IOException {
                        mvc = MockMvcBuilders.webAppContextSetup(context).build();

                        testEntity8 = repository2.save(new EmbeddedChildContentEntity());

                        String content = TestSupportData.SAMPLE_TEXT_PAYLOAD;
                        testEntity8.getChild().contentMimeType = "text/plain";
                        UUID contentId = UUID.randomUUID();
                        store.associate(testEntity8, PropertyPath.from("child"), contentId);
                        WritableResource r = (WritableResource) store.getResource(testEntity8, PropertyPath.from("child"));
                        try (OutputStream out = r.getOutputStream()) {
                            out.write(content.getBytes());
                        }
                        testEntity8 = repository2.save(testEntity8);
                    }

                    @Test
                    void shouldReturnTheRenditionAnd200() throws Exception {
                        MockHttpServletResponse response = mvc
                            .perform(get(
                                "/embeddedChildContentEntities/" + testEntity8.getId()
                                    + "/child")
                                .accept("text/html"))
                            .andExpect(status().isOk()).andReturn()
                            .getResponse();

                        assertThat(response).isNotNull();
                        assertThat(response.getContentAsString()).isEqualTo(TestSupportData.SAMPLE_REST_HTML_RENDITION_BODY);
                    }
                }

                @Nested
                class AGETToRepositoryIdContentPropertyWithMultipleMimeTypesTheLastOfWhichMatchesTheCo {
                    @BeforeEach
                    void setUp() throws IOException {
                        mvc = MockMvcBuilders.webAppContextSetup(context).build();

                        testEntity8 = repository2.save(new EmbeddedChildContentEntity());

                        String content = TestSupportData.SAMPLE_TEXT_PAYLOAD;
                        testEntity8.getChild().contentMimeType = "text/plain";
                        UUID contentId = UUID.randomUUID();
                        store.associate(testEntity8, PropertyPath.from("child"), contentId);
                        WritableResource r = (WritableResource) store.getResource(testEntity8, PropertyPath.from("child"));
                        try (OutputStream out = r.getOutputStream()) {
                            out.write(content.getBytes());
                        }
                        testEntity8 = repository2.save(testEntity8);
                    }

                    @Test
                    void shouldReturnTheOriginalContentAnd200() throws Exception {
                        MockHttpServletResponse response = mvc
                            .perform(get("/embeddedChildContentEntities/"
                                + testEntity8.getId()
                                + "/child").accept(
                                "text/xml",
                                "text/plain"))
                            .andExpect(status().isOk()).andReturn()
                            .getResponse();

                        assertThat(response).isNotNull();
                        assertThat(response.getContentAsString()).isEqualTo(TestSupportData.SAMPLE_TEXT_PAYLOAD);
                    }
                }

                @Nested
                class APUTToRepositoryIdContentProperty {
                    @BeforeEach
                    void setUp() throws IOException {
                        mvc = MockMvcBuilders.webAppContextSetup(context).build();

                        testEntity8 = repository2.save(new EmbeddedChildContentEntity());

                        String content = TestSupportData.SAMPLE_TEXT_PAYLOAD;
                        testEntity8.getChild().contentMimeType = "text/plain";
                        UUID contentId = UUID.randomUUID();
                        store.associate(testEntity8, PropertyPath.from("child"), contentId);
                        WritableResource r = (WritableResource) store.getResource(testEntity8, PropertyPath.from("child"));
                        try (OutputStream out = r.getOutputStream()) {
                            out.write(content.getBytes());
                        }
                        testEntity8 = repository2.save(testEntity8);
                    }

                    @Test
                    void shouldCreateTheContent() throws Exception {
                        mvc.perform(
                                put("/embeddedChildContentEntities/" + testEntity8.getId() + "/child")
                                    .content("Hello New Spring Content World!")
                                    .contentType("text/plain"))
                            .andExpect(status().is2xxSuccessful());

                        Optional<EmbeddedChildContentEntity> fetched = repository2
                            .findById(testEntity8.getId());
                        assertThat(fetched.isPresent()).isTrue();
                        assertThat(fetched.get().getChild().contentId).isNotNull();
                        assertThat(fetched.get().getChild().contentLen).isEqualTo(31L);
                        assertThat(fetched.get().getChild().contentMimeType).isEqualTo("text/plain");
                    }
                }

                @Nested
                class ADELETEToRepositoryIdContentProperty {
                    @BeforeEach
                    void setUp() throws IOException {
                        mvc = MockMvcBuilders.webAppContextSetup(context).build();

                        testEntity8 = repository2.save(new EmbeddedChildContentEntity());

                        String content = TestSupportData.SAMPLE_TEXT_PAYLOAD;
                        testEntity8.getChild().contentMimeType = "text/plain";
                        UUID contentId = UUID.randomUUID();
                        store.associate(testEntity8, PropertyPath.from("child"), contentId);
                        WritableResource r = (WritableResource) store.getResource(testEntity8, PropertyPath.from("child"));
                        try (OutputStream out = r.getOutputStream()) {
                            out.write(content.getBytes());
                        }
                        testEntity8 = repository2.save(testEntity8);
                    }

                    @Test
                    void shouldDeleteTheContent() throws Exception {
                        mvc.perform(delete(
                                "/embeddedChildContentEntities/" + testEntity8.getId() + "/child"))
                            .andExpect(status().isNoContent());

                        Optional<EmbeddedChildContentEntity> fetched = repository2.findById(testEntity8.getId());
                        assertThat(fetched.isPresent()).isTrue();
                        assertThat(fetched.get().getChild().contentId).isNull();
                        assertThat(fetched.get().getChild().contentLen).isNull();
                        assertThat(fetched.get().getChild().contentMimeType).isNull();
                    }
                }
            }
        }
    }
}
