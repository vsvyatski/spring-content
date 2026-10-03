package it.internal.org.springframework.content.rest.controllers;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.assertj.core.api.Assertions.assertThat;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.commons.store.Store;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import internal.org.springframework.content.rest.support.ContentEntity;

public class Content {

    private MockMvc mvc;
    private String url;
    private String contextPath = "";
    private ContentEntity entity;
    private CrudRepository repository;
    private Store store;

    public static Content tests() {
        return new Content();
    }

    public MockMvc getMvc() {
        return mvc;
    }

    public void setMvc(MockMvc mvc) {
        this.mvc = mvc;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getContextPath() {
        return contextPath;
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

    public CrudRepository getRepository() {
        return repository;
    }

    public void setRepository(CrudRepository repository) {
        this.repository = repository;
    }

    public Store getStore() {
        return store;
    }

    public void setStore(Store store) {
        this.store = store;
    }

    @Nested
    class AGETToStoreIdAccepting {
        @Test
        void shouldReturn404() throws Exception {
            mvc.perform(get(url)
                                    .accept("*/*"))
                            .andExpect(status().isNotFound());
        }
    }
    @Nested
    class AGETToStoreIdAcceptingAContentMimeType {
        @Test
        void shouldReturn404() throws Exception {
            mvc.perform(get(url)
                                    .accept("text/plain"))
                            .andExpect(status().isNotFound());
        }
    }
    @Nested
    class APUTToStoreIdWithAContentBody {
        @Test
        void shouldSetTheContentAndReturn201() throws Exception {
            String content = "Hello New Spring Content World!";
                            mvc.perform(
                                    put(url)
                                    .contextPath(contextPath)
                                    .content(content)
                                    .contentType("text/plain"))
                            .andExpect(status().isCreated());

                            Optional<ContentEntity> fetched = repository.findById(entity.getId());
                            assertThat(fetched.isPresent()).isTrue();
                            assertThat(fetched.get().getContentId()).isNotNull();
                            assertThat(fetched.get().getLen()).isEqualTo(31L);
                            assertThat(fetched.get().getMimeType()).isEqualTo("text/plain");
                            assertThat(IOUtils.toString(((ContentStore)store).getContent(fetched.get()), Charset.defaultCharset())).isEqualTo(content);
        }
    }
    @Nested
    class ADELETEToStoreIdWithAMimeType {
        @Test
        void shouldReturn404() throws Exception {
            mvc.perform(delete(url)
                                    .contextPath(contextPath)
                                    .accept("text/plain")).andExpect(status().isNotFound());
        }
    }
    @Nested
    class APOSTToStoreIdWithAMultiPartFormDataRequest {
        @Test
        void shouldSetTheContentAndReturn200() throws Exception {
            String content = "This is Spring Content!";

                            mvc.perform(multipart(url)
                                    .file(new MockMultipartFile("file", "表单ID及字段.txt", "text/plain", content.getBytes()))
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
    class GivenTheEntityHasTextPlainContent {
        @Nested
        class AGETToStoreId {
            @BeforeEach
            void setUp() {
                String content = "Hello Spring Content World!";
                                entity = (ContentEntity) ((ContentStore)store).setContent(entity, new ByteArrayInputStream(content.getBytes()));
                                entity.setMimeType("text/plain");
                                entity.setOriginalFileName("表单ID及字段.txt");
                                entity = (ContentEntity) repository.save(entity);
            }
            @Test
            void shouldReturnTheOriginalContentFilenameAnd200() throws Exception {
                assertThat(Charset.defaultCharset()).isEqualTo(Charset.forName("UTF-8"));

                                    MockHttpServletResponse response = mvc
                                            .perform(get(url)
                                                    .contextPath(contextPath)
                                                    .accept("text/plain"))
                                            .andExpect(status().isOk()).andReturn().getResponse();

                                    assertThat(response).isNotNull();
                                    assertThat(response.getHeader("Content-Disposition")).contains("filename*=UTF-8''" + URLEncoder.encode("表单ID及字段.txt"));
                                    assertThat(response.getContentAsString()).isEqualTo("Hello Spring Content World!");
            }
        }
        @Nested
        class AGETToStoreIdWithNoAcceptHeader {
            @BeforeEach
            void setUp() {
                String content = "Hello Spring Content World!";
                                entity = (ContentEntity) ((ContentStore)store).setContent(entity, new ByteArrayInputStream(content.getBytes()));
                                entity.setMimeType("text/plain");
                                entity.setOriginalFileName("表单ID及字段.txt");
                                entity = (ContentEntity) repository.save(entity);
            }
            @Test
            void shouldReturnTheOriginalContent() throws Exception {
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
        class AGETToStoreIdWithAMimeTypeThatMatchesARenderer {
            @BeforeEach
            void setUp() {
                String content = "Hello Spring Content World!";
                                entity = (ContentEntity) ((ContentStore)store).setContent(entity, new ByteArrayInputStream(content.getBytes()));
                                entity.setMimeType("text/plain");
                                entity.setOriginalFileName("表单ID及字段.txt");
                                entity = (ContentEntity) repository.save(entity);
            }
            @Test
            void shouldReturnTheRenditionAnd200() throws Exception {
                MockHttpServletResponse response = mvc
                                            .perform(get(url)
                                                    .contextPath(contextPath)
                                                    .accept("text/html"))
                                            .andExpect(status().isOk()).andReturn()
                                            .getResponse();

                                    assertThat(response).isNotNull();
                                    assertThat(response.getContentAsString()).isEqualTo("<html><body>Hello Spring Content World!</body></html>");
                                    assertThat(response.getContentType()).isEqualTo("text/html");
            }
        }
        @Nested
        class AGETToStoreIdWithAMimeTypeThatMatchesARendererAndTheOriginalContentType {
            @BeforeEach
            void setUp() {
                String content = "Hello Spring Content World!";
                                entity = (ContentEntity) ((ContentStore)store).setContent(entity, new ByteArrayInputStream(content.getBytes()));
                                entity.setMimeType("text/plain");
                                entity.setOriginalFileName("表单ID及字段.txt");
                                entity = (ContentEntity) repository.save(entity);

                entity = (ContentEntity) ((ContentStore)store).setContent(entity, new ByteArrayInputStream("<html><body>original content</body></html>".getBytes()));
                                    entity.setMimeType("text/html");
                                    entity = (ContentEntity) repository.save(entity);
            }
            @Test
            void shouldReturnTheRenditionAnd200() throws Exception {
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
        class AGETToStoreIdWithMultipleMimeTypesTheLastOfWhichMatchesTheContent {
            @BeforeEach
            void setUp() {
                String content = "Hello Spring Content World!";
                                entity = (ContentEntity) ((ContentStore)store).setContent(entity, new ByteArrayInputStream(content.getBytes()));
                                entity.setMimeType("text/plain");
                                entity.setOriginalFileName("表单ID及字段.txt");
                                entity = (ContentEntity) repository.save(entity);
            }
            @Test
            void shouldReturnTheOriginalContentAnd200() throws Exception {
                MockHttpServletResponse response = mvc.perform(get(url)
                                            .contextPath(contextPath)
                                            .accept(new String[] { "text/xml", "text/plain" }))
                                            .andExpect(status().isOk()).andReturn()
                                            .getResponse();

                                    assertThat(response).isNotNull();
                                    assertThat(response.getContentAsString()).isEqualTo("Hello Spring Content World!");
                                    assertThat(response.getContentType()).isEqualTo("text/plain");
            }
        }
        @Nested
        class AGETToStoreIdWithMultipleMimeTypesTheMiddleOfWhichMatchesTheContent {
            @BeforeEach
            void setUp() {
                String content = "Hello Spring Content World!";
                                entity = (ContentEntity) ((ContentStore)store).setContent(entity, new ByteArrayInputStream(content.getBytes()));
                                entity.setMimeType("text/plain");
                                entity.setOriginalFileName("表单ID及字段.txt");
                                entity = (ContentEntity) repository.save(entity);
            }
            @Test
            void shouldReturnTheOriginalContentAnd200() throws Exception {
                MockHttpServletResponse response = mvc.perform(get(url)
                                            .contextPath(contextPath)
                                            .accept(new String[] { "text/xml", "text/html", "*/*" }))
                                            .andExpect(status().isOk()).andReturn()
                                            .getResponse();

                                    assertThat(response).isNotNull();
                                    assertThat(response.getContentAsString()).isEqualTo("<html><body>Hello Spring Content World!</body></html>");
                                    assertThat(response.getContentType()).isEqualTo("text/html");
            }
        }
        @Nested
        class AGETToStoreIdWithJustAnAcceptAllMimeType {
            @BeforeEach
            void setUp() {
                String content = "Hello Spring Content World!";
                                entity = (ContentEntity) ((ContentStore)store).setContent(entity, new ByteArrayInputStream(content.getBytes()));
                                entity.setMimeType("text/plain");
                                entity.setOriginalFileName("表单ID及字段.txt");
                                entity = (ContentEntity) repository.save(entity);
            }
            @Test
            void shouldReturnTheOriginalContentAnd200() throws Exception {
                MockHttpServletResponse response = mvc.perform(get(url)
                                            .contextPath(contextPath)
                                            .accept(new String[] { "*/*" }))
                                            .andExpect(status().isOk()).andReturn()
                                            .getResponse();

                                    assertThat(response).isNotNull();
                                    assertThat(response.getContentAsString()).isEqualTo("Hello Spring Content World!");
                                    assertThat(response.getContentType()).isEqualTo("text/plain");
            }
        }
        @Nested
        class AGETToStoreIdWithAMimeTypeSpecifyingCharset {
            @BeforeEach
            void setUp() {
                String content = "Hello Spring Content World!";
                                entity = (ContentEntity) ((ContentStore)store).setContent(entity, new ByteArrayInputStream(content.getBytes()));
                                entity.setMimeType("text/plain");
                                entity.setOriginalFileName("表单ID及字段.txt");
                                entity = (ContentEntity) repository.save(entity);
            }
            @Test
            void shouldReturnTheOriginalContentAnd200() throws Exception {
                MockHttpServletResponse response = mvc.perform(get(url)
                                            .contextPath(contextPath)
                                            .accept("text/html;charset=ISO-8859-1"))
                                            .andExpect(status().isOk()).andReturn()
                                            .getResponse();

                                    assertThat(response).isNotNull();
                                    assertThat(response.getContentAsString()).isEqualTo("<html><body>Hello Spring Content World!</body></html>");
                                    assertThat(response.getContentType()).isEqualTo("text/html;charset=ISO-8859-1");
            }
        }
        @Nested
        class AGETToStoreIdWhenTheOriginalMimeTypeHasACharset {
            @BeforeEach
            void setUp() {
                String content = "Hello Spring Content World!";
                                entity = (ContentEntity) ((ContentStore)store).setContent(entity, new ByteArrayInputStream(content.getBytes()));
                                entity.setMimeType("text/plain");
                                entity.setOriginalFileName("表单ID及字段.txt");
                                entity = (ContentEntity) repository.save(entity);

                entity.setMimeType("text/plain;charset=ISO-8859-1");
                                    entity = (ContentEntity) repository.save(entity);
            }
            @Test
            void shouldReturnTheOriginalContentAnd200() throws Exception {
                MockHttpServletResponse response = mvc.perform(get(url)
                                            .contextPath(contextPath)
                                            .accept(new String[] { "text/plain" }))
                                            .andExpect(status().isOk()).andReturn()
                                            .getResponse();

                                    assertThat(response).isNotNull();
                                    assertThat(response.getContentAsString()).isEqualTo("Hello Spring Content World!");
                                    assertThat(response.getContentType()).isEqualTo("text/plain;charset=ISO-8859-1");
            }
        }
        @Nested
        class AGETToStoreIdWithAMimeTypeThatDoesNotMatchARendererOrTheOriginalContent {
            @BeforeEach
            void setUp() {
                String content = "Hello Spring Content World!";
                                entity = (ContentEntity) ((ContentStore)store).setContent(entity, new ByteArrayInputStream(content.getBytes()));
                                entity.setMimeType("text/plain");
                                entity.setOriginalFileName("表单ID及字段.txt");
                                entity = (ContentEntity) repository.save(entity);
            }
            @Test
            void shouldReturnTheOriginalContentAnd200() throws Exception {
                mvc.perform(get(url)
                                        .contextPath(contextPath)
                                        .accept("text/css"))
                                    .andExpect(status().isNotFound());
            }
        }
        @Nested
        class AGETToStoreIdWithARangeHeader {
            @BeforeEach
            void setUp() {
                String content = "Hello Spring Content World!";
                                entity = (ContentEntity) ((ContentStore)store).setContent(entity, new ByteArrayInputStream(content.getBytes()));
                                entity.setMimeType("text/plain");
                                entity.setOriginalFileName("表单ID及字段.txt");
                                entity = (ContentEntity) repository.save(entity);
            }
            @Test
            void shouldReturnTheContentRangeAnd206() throws Exception {
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
        class APUTToStoreId {
            @BeforeEach
            void setUp() {
                String content = "Hello Spring Content World!";
                                entity = (ContentEntity) ((ContentStore)store).setContent(entity, new ByteArrayInputStream(content.getBytes()));
                                entity.setMimeType("text/plain");
                                entity.setOriginalFileName("表单ID及字段.txt");
                                entity = (ContentEntity) repository.save(entity);
            }
            @Test
            void shouldOverwriteTheContentAndReturn200() throws Exception {
                mvc.perform(put(url)
                                            .contextPath(contextPath)
                                            .content("Hello Modified Spring Content World!")
                                            .contentType("text/plain"))
                                    .andExpect(status().isOk());

                                    assertThat(IOUtils.toString(((ContentStore)store).getContent(entity), Charset.defaultCharset())).isEqualTo("Hello Modified Spring Content World!");
            }
        }
        @Nested
        class APOSTToStoreIdWithAMultiPartRequest {
            @BeforeEach
            void setUp() {
                String content = "Hello Spring Content World!";
                                entity = (ContentEntity) ((ContentStore)store).setContent(entity, new ByteArrayInputStream(content.getBytes()));
                                entity.setMimeType("text/plain");
                                entity.setOriginalFileName("表单ID及字段.txt");
                                entity = (ContentEntity) repository.save(entity);
            }
            @Test
            void shouldOverwriteTheContentAndReturn200() throws Exception {
                String content = "This is Modified Spring Content!";

                                    mvc.perform(multipart(url)
                                            .file(new MockMultipartFile("file",
                                                    "tests-file-modified.txt",
                                                    "text/plain", content.getBytes()))
                                            .contextPath(contextPath)
                                            )
                                    .andExpect(status().isOk());

                                    Optional<ContentEntity> fetched = repository.findById(entity.getId());
                                    assertThat(fetched.isPresent()).isTrue();
                                    assertThat(fetched.get().getContentId()).isNotNull();
                                    assertThat(fetched.get().getOriginalFileName()).isEqualTo("tests-file-modified.txt");
                                    assertThat(fetched.get().getMimeType()).isEqualTo("text/plain");
                                    assertThat(fetched.get().getLen()).isEqualTo((long) content.length());
            }
        }
        @Nested
        class APOSTToStoreIdWithAMissingContentType {
            @BeforeEach
            void setUp() {
                String content = "Hello Spring Content World!";
                                entity = (ContentEntity) ((ContentStore)store).setContent(entity, new ByteArrayInputStream(content.getBytes()));
                                entity.setMimeType("text/plain");
                                entity.setOriginalFileName("表单ID及字段.txt");
                                entity = (ContentEntity) repository.save(entity);
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
        class APUTToStoreIdWithAMultiPartRequest {
            @BeforeEach
            void setUp() {
                String content = "Hello Spring Content World!";
                                entity = (ContentEntity) ((ContentStore)store).setContent(entity, new ByteArrayInputStream(content.getBytes()));
                                entity.setMimeType("text/plain");
                                entity.setOriginalFileName("表单ID及字段.txt");
                                entity = (ContentEntity) repository.save(entity);
            }
            @Test
            void shouldOverwriteTheContentAndReturn200() throws Exception {
                String content = "This is Modified Spring Content!";

                                    mvc.perform(multipart(HttpMethod.PUT, url)
                                                    .file(new MockMultipartFile("file",
                                                            "tests-file-modified.txt",
                                                            "text/plain", content.getBytes()))
                                                    .contextPath(contextPath)
                                            )
                                            .andExpect(status().isOk());

                                    Optional<ContentEntity> fetched = repository.findById(entity.getId());
                                    assertThat(fetched.isPresent()).isTrue();
                                    assertThat(fetched.get().getContentId()).isNotNull();
                                    assertThat(fetched.get().getOriginalFileName()).isEqualTo("tests-file-modified.txt");
                                    assertThat(fetched.get().getMimeType()).isEqualTo("text/plain");
                                    assertThat(fetched.get().getLen()).isEqualTo((long) content.length());
            }
        }
        @Nested
        class ADELETEToStoreIdWithTheMimetype {
            @BeforeEach
            void setUp() {
                String content = "Hello Spring Content World!";
                                entity = (ContentEntity) ((ContentStore)store).setContent(entity, new ByteArrayInputStream(content.getBytes()));
                                entity.setMimeType("text/plain");
                                entity.setOriginalFileName("表单ID及字段.txt");
                                entity = (ContentEntity) repository.save(entity);
            }
            @Test
            void shouldDeleteTheContentAttributesAndReturnA200Response() throws Exception {
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
                                    assertThat(((ContentStore)store).getContent(entity)).isNull();
            }
        }
    }

}
