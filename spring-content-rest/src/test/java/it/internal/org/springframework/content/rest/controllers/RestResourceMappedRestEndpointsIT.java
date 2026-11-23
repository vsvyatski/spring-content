package it.internal.org.springframework.content.rest.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import internal.org.springframework.content.rest.support.RestResourcePackageEntity;
import internal.org.springframework.content.rest.support.RestResourcePackageEntityRepository;
import internal.org.springframework.content.rest.support.RestResourcePackageEntityStore;
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
import java.util.Arrays;
import java.util.Map;
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
public class RestResourceMappedRestEndpointsIT {

    @Autowired
    private RestResourcePackageEntityRepository repo;
    @Autowired
    private RestResourcePackageEntityStore store;

    private RestResourcePackageEntity testEntity11;

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    private Object parse(Map<String, Object> obj, String... path) {
        if (path.length == 0) {
            return obj;
        }
        Object current = obj.get(path[0]);
        if (path.length == 1) {
            return current;
        }
        if (current instanceof Map<?, ?> nested) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) nested;
            return parse(map, Arrays.copyOfRange(path, 1, path.length));
        }
        return current;
    }

    @Nested
   class RestResourceMappedRESTEndpoints {
        @Nested
        @DisplayName("Given an entity with a simple content property")
        class EntityWithASimpleContentProperty {
            @Nested
            @DisplayName("Given a request to a non existent entity")
            class RequestToANonExistentEntity {
                @BeforeEach
                void setUp() {
                    mvc = MockMvcBuilders.webAppContextSetup(context).build();

                    testEntity11 = repo.save(new RestResourcePackageEntity());
                }

                @Test
                void shouldReturn404() throws Exception {
                    mvc.perform(
                            get("/restResourcePackageEntities/9999999/package/content"))
                        .andExpect(status().isNotFound());
                }
            }

            @Nested

            @DisplayName("Given a request to a non existent content property")

            class RequestToANonExistentContentProperty {
                @BeforeEach
                void setUp() {
                    mvc = MockMvcBuilders.webAppContextSetup(context).build();

                    testEntity11 = repo.save(new RestResourcePackageEntity());
                }

                @Test
                void shouldReturn404() throws Exception {
                    mvc.perform(
                            get("/restResourcePackageEntities/" + testEntity11.getId() + "/doesnotexist"))
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

                        testEntity11 = repo.save(new RestResourcePackageEntity());
                    }

                    @Test
                    void shouldReturn404() throws Exception {
                        mvc.perform(
                                get("/restResourcePackageEntities/" + testEntity11.getId() + "/package/content"))
                            .andExpect(status().isNotFound());
                    }
                }

                @Nested

                @DisplayName("PUT to repository id content property")

                class PutToRepositoryIdContentProperty {
                    @BeforeEach
                    void setUp() {
                        mvc = MockMvcBuilders.webAppContextSetup(context).build();

                        testEntity11 = repo.save(new RestResourcePackageEntity());
                    }

                    @Test
                    void shouldCreateTheContent() throws Exception {
                        mvc.perform(
                                put("/restResourcePackageEntities/" + testEntity11.getId() + "/package/content")
                                    .content("Hello New Spring Content World!")
                                    .contentType("text/plain"))
                            .andExpect(status().is2xxSuccessful());

                        Optional<RestResourcePackageEntity> fetched = repo.findById(testEntity11.getId());
                        assertThat(fetched.isPresent()).isTrue();
                        assertThat(fetched.get().getContentPackage().contentId).isNotNull();
                        assertThat(fetched.get().getContentPackage().contentLen).isEqualTo(31L);
                        assertThat(fetched.get().getContentPackage().contentMimeType).isEqualTo("text/plain");
                        try (InputStream actual = store.getResource(fetched.get(), PropertyPath.from("contentPackage/content")).getInputStream()) {
                            IOUtils.contentEquals(actual, new ByteArrayInputStream("Hello New Spring Content World!".getBytes()));
                        }
                    }
                }

                @Nested

                @DisplayName("PUT to store ID with json content")

                class PutToStoreIdWithJsonContent {
                    @BeforeEach
                    void setUp() {
                        mvc = MockMvcBuilders.webAppContextSetup(context).build();

                        testEntity11 = repo.save(new RestResourcePackageEntity());
                    }

                    @Test
                    void shouldSetTheContentAndReturn201() throws Exception {
                        String content = "{\"content\":\"Hello New Spring Content World!\"}";
                        mvc.perform(
                                put("/restResourcePackageEntities/" + testEntity11.getId() + "/package/content")
                                    .content(content)
                                    .contentType("application/json"))
                            .andExpect(status().isCreated());

                        Optional<RestResourcePackageEntity> fetched = repo.findById(testEntity11.getId());
                        assertThat(fetched.isPresent()).isTrue();
                        assertThat(fetched.get().getContentPackage().getContentId()).isNotNull();
                        assertThat(fetched.get().getContentPackage().getContentLen()).isEqualTo(45L);
                        assertThat(fetched.get().getContentPackage().getContentMimeType()).isEqualTo("application/json");
                        try (InputStream actual = store.getResource(fetched.get(), PropertyPath.from("contentPackage/content")).getInputStream()) {
                            IOUtils.contentEquals(actual, new ByteArrayInputStream(content.getBytes()));
                        }
                    }
                }
            }

            @Nested

            @DisplayName("Given that it has content")

            class ItHasContent {
                @Nested
                @DisplayName("GET to repository id for the entity json")
                class GetToRepositoryIdForTheEntityJson {
                    @BeforeEach
                    void setUp() throws IOException {
                        mvc = MockMvcBuilders.webAppContextSetup(context).build();

                        testEntity11 = repo.save(new RestResourcePackageEntity());

                        String content = TestSupportData.SAMPLE_TEXT_PAYLOAD;
                        testEntity11.getContentPackage().contentMimeType = "text/plain";
                        UUID contentId = UUID.randomUUID();
                        store.associate(testEntity11, PropertyPath.from("contentPackage/content"), contentId);
                        WritableResource r = (WritableResource) store.getResource(testEntity11, PropertyPath.from("contentPackage/content"));
                        try (OutputStream out = r.getOutputStream()) {
                            out.write(content.getBytes());
                        }
                        testEntity11 = repo.save(testEntity11);
                    }

                    @Test
                    void shouldReturnTheMappedContentLinks() throws Exception {
                        MockHttpServletResponse res = mvc.perform(
                                get("/restResourcePackageEntities/" + testEntity11.getId())
                                    .accept("application/hal+json"))
                            .andExpect(status().is2xxSuccessful())
                            .andReturn().getResponse();

                        ObjectMapper mapper = new ObjectMapper();
                        Map<String, Object> obj = mapper.readValue(res.getContentAsString(),
                            new TypeReference<>() {
                            });

                        Object val = parse(obj, "_links", "package/content", "href");
                        assertThat(val).isNotNull();
                        assertThat(val.toString()).matches("http://localhost/restResourcePackageEntities/.*/package/content");
                    }
                }

                @Nested

                @DisplayName("GET to repository id content property")

                class GetToRepositoryIdContentProperty {
                    @BeforeEach
                    void setUp() throws IOException {
                        mvc = MockMvcBuilders.webAppContextSetup(context).build();

                        testEntity11 = repo.save(new RestResourcePackageEntity());

                        String content = TestSupportData.SAMPLE_TEXT_PAYLOAD;
                        testEntity11.getContentPackage().contentMimeType = "text/plain";
                        UUID contentId = UUID.randomUUID();
                        store.associate(testEntity11, PropertyPath.from("contentPackage/content"), contentId);
                        WritableResource r = (WritableResource) store.getResource(testEntity11, PropertyPath.from("contentPackage/content"));
                        try (OutputStream out = r.getOutputStream()) {
                            out.write(content.getBytes());
                        }
                        testEntity11 = repo.save(testEntity11);
                    }

                    @Test
                    void shouldReturnTheContent() throws Exception {
                        MockHttpServletResponse response = mvc
                            .perform(get("/restResourcePackageEntities/" + testEntity11.getId() + "/package/content")
                                .accept("text/plain"))
                            .andExpect(status().isOk())
                            .andExpect(header().string("etag", is("\"1\"")))
                            .andExpect(header().string("last-modified", LastModifiedDateTests
                                .isWithinASecond(testEntity11.getModifiedDate())))
                            .andReturn().getResponse();

                        assertThat(response).isNotNull();
                        assertThat(response.getContentAsString()).isEqualTo(TestSupportData.SAMPLE_TEXT_PAYLOAD);
                    }
                }

                @Nested

                @DisplayName("GET to repository id content property with a MIME type that matches a renderer")

                class GetToRepositoryIdContentPropertyWithAMimeTypeThatMatchesARenderer {
                    @BeforeEach
                    void setUp() throws IOException {
                        mvc = MockMvcBuilders.webAppContextSetup(context).build();

                        testEntity11 = repo.save(new RestResourcePackageEntity());

                        String content = TestSupportData.SAMPLE_TEXT_PAYLOAD;
                        testEntity11.getContentPackage().contentMimeType = "text/plain";
                        UUID contentId = UUID.randomUUID();
                        store.associate(testEntity11, PropertyPath.from("contentPackage/content"), contentId);
                        WritableResource r = (WritableResource) store.getResource(testEntity11, PropertyPath.from("contentPackage/content"));
                        try (OutputStream out = r.getOutputStream()) {
                            out.write(content.getBytes());
                        }
                        testEntity11 = repo.save(testEntity11);
                    }

                    @Test
                    void shouldReturnTheRenditionAnd200() throws Exception {
                        MockHttpServletResponse response = mvc
                            .perform(get(
                                "/restResourcePackageEntities/" + testEntity11.getId() + "/package/content")
                                .accept("text/html"))
                            .andExpect(status().isOk()).andReturn()
                            .getResponse();

                        assertThat(response).isNotNull();
                        assertThat(response.getContentAsString()).isEqualTo(TestSupportData.SAMPLE_REST_HTML_RENDITION_BODY);
                    }
                }

                @Nested

                @DisplayName("GET to repository id content property with multiple MIME types the last of which matches the co")

                class GetToRepositoryIdContentPropertyWithMultipleMimeTypesTheLastOfWhichMatchesTheCo {
                    @BeforeEach
                    void setUp() throws IOException {
                        mvc = MockMvcBuilders.webAppContextSetup(context).build();

                        testEntity11 = repo.save(new RestResourcePackageEntity());

                        String content = TestSupportData.SAMPLE_TEXT_PAYLOAD;
                        testEntity11.getContentPackage().contentMimeType = "text/plain";
                        UUID contentId = UUID.randomUUID();
                        store.associate(testEntity11, PropertyPath.from("contentPackage/content"), contentId);
                        WritableResource r = (WritableResource) store.getResource(testEntity11, PropertyPath.from("contentPackage/content"));
                        try (OutputStream out = r.getOutputStream()) {
                            out.write(content.getBytes());
                        }
                        testEntity11 = repo.save(testEntity11);
                    }

                    @Test
                    void shouldReturnTheOriginalContentAnd200() throws Exception {
                        MockHttpServletResponse response = mvc
                            .perform(get("/restResourcePackageEntities/" + testEntity11.getId() + "/package/content").accept(
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

                        testEntity11 = repo.save(new RestResourcePackageEntity());

                        String content = TestSupportData.SAMPLE_TEXT_PAYLOAD;
                        testEntity11.getContentPackage().contentMimeType = "text/plain";
                        UUID contentId = UUID.randomUUID();
                        store.associate(testEntity11, PropertyPath.from("contentPackage/content"), contentId);
                        WritableResource r = (WritableResource) store.getResource(testEntity11, PropertyPath.from("contentPackage/content"));
                        try (OutputStream out = r.getOutputStream()) {
                            out.write(content.getBytes());
                        }
                        testEntity11 = repo.save(testEntity11);
                    }

                    @Test
                    void shouldCreateTheContent() throws Exception {
                        mvc.perform(
                                put("/restResourcePackageEntities/" + testEntity11.getId() + "/package/content")
                                    .content("Hello New Spring Content World!")
                                    .contentType("text/plain"))
                            .andExpect(status().is2xxSuccessful());

                        Optional<RestResourcePackageEntity> fetched = repo.findById(testEntity11.getId());
                        assertThat(fetched.isPresent()).isTrue();
                        assertThat(fetched.get().getContentPackage().contentId).isNotNull();
                        assertThat(fetched.get().getContentPackage().contentLen).isEqualTo(31L);
                        assertThat(fetched.get().getContentPackage().contentMimeType).isEqualTo("text/plain");
                    }
                }

                @Nested

                @DisplayName("DELETE to repository id content property")

                class DeleteToRepositoryIdContentProperty {
                    @BeforeEach
                    void setUp() throws IOException {
                        mvc = MockMvcBuilders.webAppContextSetup(context).build();

                        testEntity11 = repo.save(new RestResourcePackageEntity());

                        String content = TestSupportData.SAMPLE_TEXT_PAYLOAD;
                        testEntity11.getContentPackage().contentMimeType = "text/plain";
                        UUID contentId = UUID.randomUUID();
                        store.associate(testEntity11, PropertyPath.from("contentPackage/content"), contentId);
                        WritableResource r = (WritableResource) store.getResource(testEntity11, PropertyPath.from("contentPackage/content"));
                        try (OutputStream out = r.getOutputStream()) {
                            out.write(content.getBytes());
                        }
                        testEntity11 = repo.save(testEntity11);
                    }

                    @Test
                    void shouldDeleteTheContent() throws Exception {
                        mvc.perform(delete(
                                "/restResourcePackageEntities/" + testEntity11.getId() + "/package/content"))
                            .andExpect(status().isNoContent());

                        Optional<RestResourcePackageEntity> fetched = repo.findById(testEntity11.getId());
                        assertThat(fetched.isPresent()).isTrue();
                        assertThat(fetched.get().getContentPackage().contentId).isNull();
                        assertThat(fetched.get().getContentPackage().contentLen).isNull();
                        assertThat(fetched.get().getContentPackage().contentMimeType).isNull();
                    }
                }
            }
        }
    }
}
