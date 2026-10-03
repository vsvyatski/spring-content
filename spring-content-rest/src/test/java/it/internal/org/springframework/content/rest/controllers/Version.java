package it.internal.org.springframework.content.rest.controllers;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import internal.org.springframework.content.rest.support.ContentEntity;
import internal.org.springframework.content.rest.support.TestEntity2;
import internal.org.springframework.content.rest.support.TestEntity4;
import org.apache.commons.io.IOUtils;

import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.commons.store.Store;
import org.springframework.data.repository.CrudRepository;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class Version {

    private MockMvc mvc;
    private String url;
    private String collectionUrl;
    private String contentLinkRel;
    private CrudRepository repo;
    private Store store;
    private String etag;
    private ContentEntity entity;

    public static Version tests() {
        return new Version();
    }

    public void setMvc(MockMvc mvc) {
        this.mvc = mvc;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setCollectionUrl(String collectionUrl) {
        this.collectionUrl = collectionUrl;
    }

    public void setContentLinkRel(String contentLinkRel) {
        this.contentLinkRel = contentLinkRel;
    }

    public void setRepo(CrudRepository repo) {
        this.repo = repo;
    }

    public void setStore(Store store) {
        this.store = store;
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }

    public void setEntity(ContentEntity entity) {
        this.entity = entity;
    }

    
    @Nested
    class Issue1975 {
        @Test
        void shouldAlwaysEvaluateIfMatchHeaderEvenAfterContentDeletion() throws Throwable {
            String entityUrl = mvc.perform(post(collectionUrl).content("{}"))
                                    .andExpect(status().is2xxSuccessful()).andReturn().getResponse().getHeader("Location");
                            assertThat(entityUrl).isNotNull();

                            String body = mvc.perform(get(entityUrl).accept("application/json"))
                                    .andExpect(status().is2xxSuccessful()).andReturn().getResponse().getContentAsString();

                            ObjectMapper objectMapper = new ObjectMapper();
                            JsonNode responseJson = objectMapper.readTree(body);
                            JsonNode linksNode = responseJson.get("_links");
                            JsonNode contentNode = linksNode.get(contentLinkRel);
                            String contentHref = contentNode.get("href").asText();

                            mvc.perform(put(contentHref).header("If-Match", "\"0\"").contentType("text/plain").content("Hello world!"))
                                    .andExpect(status().is2xxSuccessful());  // Content created
                            mvc.perform(delete(contentHref).header("If-Match", "\"1\""))
                                    .andExpect(status().is2xxSuccessful());  // User A
                            mvc.perform(put(contentHref).header("If-Match", "\"1\"").contentType("text/plain").content("foo bar"))
                                    .andExpect(status().isPreconditionFailed());  // User B
        }
    }
    @Nested
    class AGETRequestToStoreId {
        @Test
        void shouldReturnAnEtagHeader() throws Throwable {
            MockHttpServletResponse response = mvc
                                    .perform(get(url)
                                            .accept("text/plain"))
                                    .andExpect(status().isOk())
                                    .andExpect(header().string("etag", is(etag)))
                                    .andReturn().getResponse();

                            assertThat(response).isNotNull();
                            assertThat(response.getContentAsString()).isEqualTo("Hello Spring Content World!");
        }
    }
    @Nested
    class AGETRequestToStoreIdWithAMatchingIfNoneMatchHeader {
        @Test
        void shouldRespondWithA304NotModified() throws Throwable {
            mvc.perform(get(url)
                                    .accept("text/plain")
                                    .header("if-none-match", etag))
                                    .andExpect(status().isNotModified());
        }
    }
    @Nested
    class AGETRequestToStoreIdWithAnNonMatchingIfNoneMatchHeader {
        @Test
        void shouldRespondWithTheContent() throws Throwable {
            MockHttpServletResponse response = mvc
                                    .perform(get(url)
                                            .accept("text/plain")
                                            .header("if-none-match", "\"999\""))
                                    .andExpect(status().isOk())
                                    .andExpect(header().string("etag", is(etag)))
                                    .andReturn().getResponse();

                            assertThat(response).isNotNull();
                            assertThat(response.getContentAsString()).isEqualTo("Hello Spring Content World!");
        }
    }
    @Nested
    class APUTToStoreIdWithAMatchingIfMatchHeader {
        @Test
        void shouldUpdateTheContent() throws Throwable {
            mvc.perform(put(url)
                                    .content("Hello Modified Spring Content World!")
                                    .contentType("text/other")
                                    .header("if-match", etag))
                                    .andExpect(status().isOk());
        }
        @Test
        void shouldUpdateTheContentAttributes() throws Throwable {
            mvc.perform(multipart(url)
                                    .file(new MockMultipartFile("file",
                                            "test-file-modified.txt",
                                            "text/other", "Hello Modified Spring Content World!".getBytes()))
                                    .header("if-match", etag))
                                    .andExpect(status().isOk());

                            if (entity != null) {
                                Optional<ContentEntity> fetched = repo.findById(entity.getId());
                                assertThat(fetched.isPresent()).isTrue();
                                assertThat(fetched.get().getLen()).isEqualTo(36L);
                                assertThat(fetched.get().getMimeType()).isEqualTo("text/other");
                                assertThat(fetched.get().getOriginalFileName()).isEqualTo("test-file-modified.txt");
                            }
        }
    }
    @Nested
    class ADELETEToStoreIdWithAMatchingIfMatchHeader {
        @Test
        void shouldDeleteTheContentAttributesAndReturnA200Response() throws Throwable {
            mvc.perform(delete(url)
                                    .contentType("text/plain"))
                                    .andExpect(status().isNoContent());

                            if (entity != null) {
                                Optional<ContentEntity> fetched = repo.findById(entity.getId());
                                assertThat(fetched.isPresent()).isTrue();
                                assertThat(fetched.get().getContentId()).isNull();
                                assertThat(fetched.get().getLen()).isNull();
                                assertThat(fetched.get().getMimeType()).isNull();
                                assertThat(((ContentStore)store).getContent(fetched.get())).isNull();
                            }
        }
    }
    @Nested
    class APUTToStoreIdWithANonMatchingIfMatchHeader {
        @Test
        void shouldRespondWith412PreconditionFailed() throws Throwable {
            mvc.perform(put(url)
                                    .content("Hello Modified Spring Content World!")
                                    .contentType("text/plain")
                                    .header("if-match", "\"999\""))
                                    .andExpect(status().isPreconditionFailed());
        }
    }
    @Nested
    class APUTToStoreIdWithAMatchingIfNoneMatchHeader {
        @Test
        void shouldRespondWithA412PreconditionFailed() throws Throwable {
            mvc.perform(put(url)
                                    .content("Hello Modified Spring Content World!")
                                    .contentType("text/plain")
                                    .header("if-none-match", etag))
                                    .andExpect(status().isPreconditionFailed());
        }
    }
    @Nested
    class APUTToStoreIdWithANonMatchingIfNoneMatchHeader {
        @Test
        void shouldRespondWith200OKAndSetTheContent() throws Throwable {
            mvc.perform(put(url)
                                    .content("Hello Modified Spring Content World!")
                                    .contentType("text/plain")
                                    .header("if-none-match", "\"999\""))
                                    .andExpect(status().isOk());
        }
    }
    @Nested
    class APUTToStoreIdWithAMatchingIfMatchHeaderAndAMatchingIfNoneMatchHeader {
        @Test
        void shouldRespondWithA412PreconditionFailed() throws Throwable {
            mvc.perform(put(url)
                                    .content("Hello Modified Spring Content World!")
                                    .contentType("text/plain")
                                    .header("if-match", etag)
                                    .header("if-none-match", etag))
                                    .andExpect(status().isPreconditionFailed());
        }
    }
    @Nested
    class APOSTToStoreIdWithANonMatchingIfMatchHeader {
        @Test
        void shouldRespondWith412PreconditionFailed() throws Throwable {
            mvc.perform(multipart(url)
                                    .file(new MockMultipartFile("file",
                                            "tests-file-modified.txt",
                                            "text/plain", "Hello Spring Content World!".getBytes()))
                                    .header("if-match", "\"999\""))
                                    .andExpect(status().isPreconditionFailed());
        }
    }
    @Nested
    class ADELETEToStoreIdWithANonMatchingIfMatchHeader {
        @Test
        void shouldRespondWith412PreconditionFailed() throws Throwable {
            mvc.perform(delete(url)
                                    .header("if-match", "\"999\""))
                                    .andExpect(status().isPreconditionFailed());
        }
    }

}
