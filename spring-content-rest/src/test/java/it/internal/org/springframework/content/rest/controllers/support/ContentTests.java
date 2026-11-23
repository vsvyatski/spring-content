package it.internal.org.springframework.content.rest.controllers.support;

import internal.org.springframework.content.rest.support.ContentEntity;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.test.support.TestSupportData;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayInputStream;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ContentTests {

    private MockMvc mvc;
    private String url;
    private String contextPath = "";
    private ContentEntity entity;
    private CrudRepository<ContentEntity, Long> repository;
    private ContentStore<ContentEntity, UUID> store;

    public static ContentTests tests() {
        return new ContentTests();
    }

    public MockMvc getMvc() {
        return mvc;
    }

    public void setMvc(MockMvc mvc) {
        this.mvc = mvc;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    public ContentEntity getEntity() {
        return entity;
    }

    public void setEntity(ContentEntity entity) {
        this.entity = entity;
    }

    @SuppressWarnings("unchecked")
    public void setRepository(CrudRepository<? extends ContentEntity, Long> repository) {
        this.repository = (CrudRepository<ContentEntity, Long>) repository;
    }

    @SuppressWarnings("unchecked")
    public void setStore(ContentStore<?, ?> store) {
        this.store = (ContentStore<ContentEntity, UUID>) store;
    }

    @Nested
    @DisplayName("GET to {store ID} with Accept */*")
    class GetToStoreIdWithAcceptWildcard {
        @Test
        void shouldReturn404() throws Exception {
            mvc.perform(get(url).accept("*/*")).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET to {store ID} accepting a content MIME type")
    class GetToStoreIdAcceptingContentMimeType {
        @Test
        void shouldReturn404() throws Exception {
            mvc.perform(get(url).accept("text/plain")).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT to {store ID} with content body")
    class PutToStoreIdWithContentBody {
        @Test
        void shouldSetContentAndReturn201() throws Exception {
            String content = "Hello New Spring Content World!";
            mvc.perform(put(url).contextPath(contextPath).content(content).contentType("text/plain"))
                .andExpect(status().isCreated());

            Optional<ContentEntity> fetched = repository.findById(entity.getId());
            assertThat(fetched.isPresent()).isTrue();
            assertThat(fetched.get().getContentId()).isNotNull();
            assertThat(fetched.get().getLen()).isEqualTo(31L);
            assertThat(fetched.get().getMimeType()).isEqualTo("text/plain");
            assertThat(IOUtils.toString(store.getContent(fetched.get()), StandardCharsets.UTF_8)).isEqualTo(content);
        }
    }

    @Nested
    @DisplayName("DELETE to {store ID} with MIME type")
    class DeleteToStoreIdWithMimeType {
        @Test
        void shouldReturn404() throws Exception {
            mvc.perform(delete(url).contextPath(contextPath).accept("text/plain")).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST to {store ID} with multipart form data request")
    class PostToStoreIdWithMultipartFormData {
        @Test
        void shouldSetContentAndReturn200() throws Exception {
            String content = "This is Spring Content!";

            mvc.perform(multipart(url)
                    .file(new MockMultipartFile("file", "表单ID及字段.txt", "text/plain", content.getBytes(StandardCharsets.UTF_8)))
                    .contextPath(contextPath)
                )
                .andExpect(status().isCreated());

            Optional<ContentEntity> fetched = repository.findById(entity.getId());
            assertThat(fetched.isPresent()).isTrue();
            assertThat(fetched.get().getContentId()).isNotNull();
            assertThat(fetched.get().getOriginalFileName()).isEqualTo("表单ID及字段.txt");
            assertThat(fetched.get().getMimeType()).isEqualTo("text/plain");
            assertThat(fetched.get().getLen()).isEqualTo(Long.valueOf(content.length()));
        }
    }

    @Nested
    @DisplayName("Given the entity has text/plain content")
    class EntityHasTextPlainContent {
        @Nested
        @DisplayName("GET to {store ID}")
        class GetToStoreId {
            @BeforeEach
            void setUp() {
                String content = "Hello Spring Content World!";
                entity = store.setContent(entity, new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
                entity.setMimeType("text/plain");
                entity.setOriginalFileName("表单ID及字段.txt");
                entity = repository.save(entity);
            }

            @Test
            void shouldReturnOriginalContentFilenameAnd200() throws Exception {
                assertThat(Charset.defaultCharset()).isEqualTo(StandardCharsets.UTF_8);

                MockHttpServletResponse response = mvc
                    .perform(get(url)
                        .contextPath(contextPath)
                        .accept("text/plain"))
                    .andExpect(status().isOk()).andReturn().getResponse();

                assertThat(response).isNotNull();
                assertThat(response.getHeader("Content-Disposition")).contains(
                    "filename*=UTF-8''" + URLEncoder.encode("表单ID及字段.txt", StandardCharsets.UTF_8));
                assertThat(response.getContentAsString()).isEqualTo("Hello Spring Content World!");
            }
        }

        @Nested
        @DisplayName("GET to {store ID} with no Accept header")
        class GetToStoreIdWithNoAcceptHeader {
            @BeforeEach
            void setUp() {
                String content = "Hello Spring Content World!";
                entity = store.setContent(entity, new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
                entity.setMimeType("text/plain");
                entity.setOriginalFileName("表单ID及字段.txt");
                entity = repository.save(entity);
            }

            @Test
            void shouldReturnOriginalContent() throws Exception {
                MockHttpServletResponse response = mvc.perform(
                        get(url)
                            .contextPath(contextPath)
                    )
                    .andExpect(status().isOk())
                    .andReturn().getResponse();

                assertThat(response).isNotNull();
                assertThat(response.getContentAsString()).isEqualTo("Hello Spring Content World!");
            }
        }

        @Nested
        @DisplayName("GET to {store ID} with a MIME type that matches a renderer")
        class GetToStoreIdWithMimeTypeMatchingRenderer {
            @BeforeEach
            void setUp() {
                String content = "Hello Spring Content World!";
                entity = store.setContent(entity, new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
                entity.setMimeType("text/plain");
                entity.setOriginalFileName("表单ID及字段.txt");
                entity = repository.save(entity);
            }

            @Test
            void shouldReturnRenditionAnd200() throws Exception {
                MockHttpServletResponse response = mvc
                    .perform(get(url)
                        .contextPath(contextPath)
                        .accept("text/html"))
                    .andExpect(status().isOk()).andReturn()
                    .getResponse();

                assertThat(response).isNotNull();
                assertThat(response.getContentAsString()).isEqualTo(TestSupportData.SAMPLE_REST_HTML_RENDITION_BODY);
                assertThat(response.getContentType()).isEqualTo("text/html");
            }
        }

        @Nested
        @DisplayName("GET to {store ID} with a MIME type that matches a renderer and the original content type")
        class GetToStoreIdWithRendererMimeTypeAndOriginalContentType {
            @BeforeEach
            void setUp() {
                String content = "Hello Spring Content World!";
                entity = store.setContent(entity, new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
                entity.setMimeType("text/plain");
                entity.setOriginalFileName("表单ID及字段.txt");
                entity = repository.save(entity);

                entity = store.setContent(entity,
                    new ByteArrayInputStream("<html><body>original content</body></html>".getBytes(StandardCharsets.UTF_8)));
                entity.setMimeType("text/html");
                entity = repository.save(entity);
            }

            @Test
            void shouldReturnRenditionAnd200() throws Exception {
                MockHttpServletResponse response = mvc
                    .perform(get(url)
                        .contextPath(contextPath)
                        .accept("text/html"))
                    .andExpect(status().isOk()).andReturn()
                    .getResponse();

                assertThat(response).isNotNull();
                assertThat(response.getContentAsString()).isEqualTo("<html><body>original content</body></html>");
                assertThat(response.getContentType()).isEqualTo("text/html");
            }
        }

        @Nested
        @DisplayName("GET to {store ID} with multiple MIME types, last matches content")
        class GetToStoreIdWithMultipleMimeTypesLastMatchesContent {
            @BeforeEach
            void setUp() {
                String content = "Hello Spring Content World!";
                entity = store.setContent(entity, new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
                entity.setMimeType("text/plain");
                entity.setOriginalFileName("表单ID及字段.txt");
                entity = repository.save(entity);
            }

            @Test
            void shouldReturnOriginalContentAnd200() throws Exception {
                MockHttpServletResponse response = mvc.perform(get(url)
                        .contextPath(contextPath)
                        .accept("text/xml", "text/plain"))
                    .andExpect(status().isOk()).andReturn()
                    .getResponse();

                assertThat(response).isNotNull();
                assertThat(response.getContentAsString()).isEqualTo("Hello Spring Content World!");
                assertThat(response.getContentType()).isEqualTo("text/plain");
            }
        }

        @Nested
        @DisplayName("GET to {store ID} with multiple MIME types, middle matches content")
        class GetToStoreIdWithMultipleMimeTypesMiddleMatchesContent {
            @BeforeEach
            void setUp() {
                String content = "Hello Spring Content World!";
                entity = store.setContent(entity, new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
                entity.setMimeType("text/plain");
                entity.setOriginalFileName("表单ID及字段.txt");
                entity = repository.save(entity);
            }

            @Test
            void shouldReturnOriginalContentAnd200() throws Exception {
                MockHttpServletResponse response = mvc.perform(get(url)
                        .contextPath(contextPath)
                        .accept("text/xml", "text/html", "*/*"))
                    .andExpect(status().isOk()).andReturn()
                    .getResponse();

                assertThat(response).isNotNull();
                assertThat(response.getContentAsString()).isEqualTo(TestSupportData.SAMPLE_REST_HTML_RENDITION_BODY);
                assertThat(response.getContentType()).isEqualTo("text/html");
            }
        }

        @Nested
        @DisplayName("GET to {store ID} with accept-all MIME type")
        class GetToStoreIdWithAcceptAllMimeType {
            @BeforeEach
            void setUp() {
                String content = "Hello Spring Content World!";
                entity = store.setContent(entity, new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
                entity.setMimeType("text/plain");
                entity.setOriginalFileName("表单ID及字段.txt");
                entity = repository.save(entity);
            }

            @Test
            void shouldReturnOriginalContentAnd200() throws Exception {
                MockHttpServletResponse response = mvc.perform(get(url)
                        .contextPath(contextPath)
                        .accept("*/*"))
                    .andExpect(status().isOk()).andReturn()
                    .getResponse();

                assertThat(response).isNotNull();
                assertThat(response.getContentAsString()).isEqualTo("Hello Spring Content World!");
                assertThat(response.getContentType()).isEqualTo("text/plain");
            }
        }

        @Nested
        @DisplayName("GET to {store ID} with MIME type specifying charset")
        class GetToStoreIdWithMimeTypeSpecifyingCharset {
            @BeforeEach
            void setUp() {
                String content = "Hello Spring Content World!";
                entity = store.setContent(entity, new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
                entity.setMimeType("text/plain");
                entity.setOriginalFileName("表单ID及字段.txt");
                entity = repository.save(entity);
            }

            @Test
            void shouldReturnOriginalContentAnd200() throws Exception {
                MockHttpServletResponse response = mvc.perform(get(url)
                        .contextPath(contextPath)
                        .accept("text/html;charset=ISO-8859-1"))
                    .andExpect(status().isOk()).andReturn()
                    .getResponse();

                assertThat(response).isNotNull();
                assertThat(response.getContentAsString()).isEqualTo(TestSupportData.SAMPLE_REST_HTML_RENDITION_BODY);
                assertThat(response.getContentType()).isEqualTo("text/html;charset=ISO-8859-1");
            }
        }

        @Nested
        @DisplayName("GET to {store ID} when original MIME type has charset")
        class GetToStoreIdWhenOriginalMimeTypeHasCharset {
            @BeforeEach
            void setUp() {
                String content = "Hello Spring Content World!";
                entity = store.setContent(entity, new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
                entity.setMimeType("text/plain");
                entity.setOriginalFileName("表单ID及字段.txt");
                entity = repository.save(entity);

                entity.setMimeType("text/plain;charset=ISO-8859-1");
                entity = repository.save(entity);
            }

            @Test
            void shouldReturnOriginalContentAnd200() throws Exception {
                MockHttpServletResponse response = mvc.perform(get(url)
                        .contextPath(contextPath)
                        .accept("text/plain"))
                    .andExpect(status().isOk()).andReturn()
                    .getResponse();

                assertThat(response).isNotNull();
                assertThat(response.getContentAsString()).isEqualTo("Hello Spring Content World!");
                assertThat(response.getContentType()).isEqualTo("text/plain;charset=ISO-8859-1");
            }
        }

        @Nested
        @DisplayName("GET to {store ID} with MIME type that does not match renderer or original content")
        class GetToStoreIdWithMimeTypeNoMatch {
            @BeforeEach
            void setUp() {
                String content = "Hello Spring Content World!";
                entity = store.setContent(entity, new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
                entity.setMimeType("text/plain");
                entity.setOriginalFileName("表单ID及字段.txt");
                entity = repository.save(entity);
            }

            @Test
            void shouldReturnOriginalContentAnd200() throws Exception {
                mvc.perform(get(url)
                        .contextPath(contextPath)
                        .accept("text/css"))
                    .andExpect(status().isNotFound());
            }
        }

        @Nested
        @DisplayName("GET to {store ID} with Range header")
        class GetToStoreIdWithRangeHeader {
            @BeforeEach
            void setUp() {
                String content = "Hello Spring Content World!";
                entity = store.setContent(entity, new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
                entity.setMimeType("text/plain");
                entity.setOriginalFileName("表单ID及字段.txt");
                entity = repository.save(entity);
            }

            @Test
            void shouldReturnContentRangeAnd206() throws Exception {
                MockHttpServletResponse response = mvc
                    .perform(get(url)
                        .contextPath(contextPath)
                        .accept("text/plain")
                        .header("range", "bytes=6-19"))
                    .andExpect(status().isPartialContent()).andReturn()
                    .getResponse();

                assertThat(response).isNotNull();
                assertThat(response.getContentAsString()).isEqualTo("Spring Content");
            }
        }

        @Nested
        @DisplayName("PUT to {store ID}")
        class PutToStoreId {
            @BeforeEach
            void setUp() {
                String content = "Hello Spring Content World!";
                entity = store.setContent(entity, new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
                entity.setMimeType("text/plain");
                entity.setOriginalFileName("表单ID及字段.txt");
                entity = repository.save(entity);
            }

            @Test
            void shouldOverwriteContentAndReturn200() throws Exception {
                mvc.perform(put(url)
                        .contextPath(contextPath)
                        .content("Hello Modified Spring Content World!")
                        .contentType("text/plain"))
                    .andExpect(status().isOk());

                assertThat(IOUtils.toString(store.getContent(entity), StandardCharsets.UTF_8))
                    .isEqualTo("Hello Modified Spring Content World!");
            }
        }

        @Nested
        @DisplayName("POST to {store ID} with multipart request")
        class PostToStoreIdWithMultipartRequest {
            @BeforeEach
            void setUp() {
                String content = "Hello Spring Content World!";
                entity = store.setContent(entity, new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
                entity.setMimeType("text/plain");
                entity.setOriginalFileName("表单ID及字段.txt");
                entity = repository.save(entity);
            }

            @Test
            void shouldOverwriteContentAndReturn200() throws Exception {
                String content = "This is Modified Spring Content!";

                mvc.perform(multipart(url)
                        .file(new MockMultipartFile("file",
                            "tests-file-modified.txt",
                            "text/plain", content.getBytes(StandardCharsets.UTF_8)))
                        .contextPath(contextPath)
                    )
                    .andExpect(status().isOk());

                Optional<ContentEntity> fetched = repository.findById(entity.getId());
                assertThat(fetched.isPresent()).isTrue();
                assertThat(fetched.get().getContentId()).isNotNull();
                assertThat(fetched.get().getOriginalFileName()).isEqualTo("tests-file-modified.txt");
                assertThat(fetched.get().getMimeType()).isEqualTo("text/plain");
                assertThat(fetched.get().getLen()).isEqualTo(Long.valueOf(content.length()));
            }
        }

        @Nested
        @DisplayName("POST to {store ID} with missing content type")
        class PostToStoreIdWithMissingContentType {
            @BeforeEach
            void setUp() {
                String content = "Hello Spring Content World!";
                entity = store.setContent(entity, new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
                entity.setMimeType("text/plain");
                entity.setOriginalFileName("表单ID及字段.txt");
                entity = repository.save(entity);
            }

            @Test
            void shouldReturn400BadRequest() throws Exception {
                mvc.perform(post(url)
                        .content("some content")
                        .contextPath(contextPath)
                    )
                    .andExpect(status().isBadRequest());
            }
        }

        @Nested
        @DisplayName("PUT to {store ID} with multipart request")
        class PutToStoreIdWithMultipartRequest {
            @BeforeEach
            void setUp() {
                String content = "Hello Spring Content World!";
                entity = store.setContent(entity, new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
                entity.setMimeType("text/plain");
                entity.setOriginalFileName("表单ID及字段.txt");
                entity = repository.save(entity);
            }

            @Test
            void shouldOverwriteContentAndReturn200() throws Exception {
                String content = "This is Modified Spring Content!";

                mvc.perform(multipart(HttpMethod.PUT, url)
                        .file(new MockMultipartFile("file",
                            "tests-file-modified.txt",
                            "text/plain", content.getBytes(StandardCharsets.UTF_8)))
                        .contextPath(contextPath)
                    )
                    .andExpect(status().isOk());

                Optional<ContentEntity> fetched = repository.findById(entity.getId());
                assertThat(fetched.isPresent()).isTrue();
                assertThat(fetched.get().getContentId()).isNotNull();
                assertThat(fetched.get().getOriginalFileName()).isEqualTo("tests-file-modified.txt");
                assertThat(fetched.get().getMimeType()).isEqualTo("text/plain");
                assertThat(fetched.get().getLen()).isEqualTo(Long.valueOf(content.length()));
            }
        }

        @Nested
        @DisplayName("DELETE to {store ID} with MIME type")
        class DeleteToStoreIdWithMimeType {
            @BeforeEach
            void setUp() {
                String content = "Hello Spring Content World!";
                entity = store.setContent(entity, new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
                entity.setMimeType("text/plain");
                entity.setOriginalFileName("表单ID及字段.txt");
                entity = repository.save(entity);
            }

            @Test
            void shouldDeleteContentAttributesAndReturn204() throws Exception {
                mvc.perform(delete(url)
                        .contentType("text/plain")
                        .contextPath(contextPath)
                    )
                    .andExpect(status().isNoContent());

                Optional<ContentEntity> fetched = repository.findById(entity.getId());
                assertThat(fetched.isPresent()).isTrue();
                assertThat(fetched.get().getContentId()).isNull();
                assertThat(fetched.get().getLen()).isNull();
                assertThat(fetched.get().getMimeType()).isNull();
                assertThat(store.getContent(entity)).isNull();
            }
        }
    }
}
