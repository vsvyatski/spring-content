package it.internal.org.springframework.content.rest.controllers.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import internal.org.springframework.content.rest.support.ContentEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.data.repository.CrudRepository;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class VersionTests {

    private MockMvc mvc;
    private String url;
    private String collectionUrl;
    private String contentLinkRel;
    private CrudRepository<ContentEntity, Long> repo;
    private ContentStore<ContentEntity, UUID> store;
    private String etag;
    private ContentEntity entity;

    public static VersionTests tests() {
        return new VersionTests();
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

    @SuppressWarnings("unchecked")
    public void setRepo(CrudRepository<?, Long> repo) {
        this.repo = (CrudRepository<ContentEntity, Long>) repo;
    }

    @SuppressWarnings("unchecked")
    public void setStore(ContentStore<?, ?> store) {
        this.store = (ContentStore<ContentEntity, UUID>) store;
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }

    public void setEntity(ContentEntity entity) {
        this.entity = entity;
    }

    @Nested
    @DisplayName("If-Match evaluated after content deletion")
    class IfMatchAfterContentDeletion {
        @Test
        void shouldAlwaysEvaluateIfMatchHeaderEvenAfterContentDeletion() throws Exception {
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
    @DisplayName("GET request to {store ID}")
    class Get {
        @Test
        void shouldReturnEtagHeader() throws Exception {
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
    @DisplayName("GET request to {store ID} with a matching If-None-Match header")
    class GetWithMatchingIfNoneMatchHeader {
        @Test
        void shouldRespondWith304NotModified() throws Exception {
            mvc.perform(get(url)
                    .accept("text/plain")
                    .header("if-none-match", etag))
                .andExpect(status().isNotModified());
        }
    }

    @Nested
    @DisplayName("GET request to {store ID} with a non-matching If-None-Match header")
    class GetWithNonMatchingIfNoneMatchHeader {
        @Test
        void shouldRespondWithContent() throws Exception {
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
    @DisplayName("PUT to {store ID} with a matching If-Match header")
    class PutWithMatchingIfMatchHeader {
        @Test
        void shouldUpdateContent() throws Exception {
            mvc.perform(put(url)
                    .content("Hello Modified Spring Content World!")
                    .contentType("text/other")
                    .header("if-match", etag))
                .andExpect(status().isOk());
        }

        @Test
        void shouldUpdateContentAttributes() throws Exception {
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
    @DisplayName("DELETE to {store ID} with a matching If-Match header")
    class DeleteWithMatchingIfMatchHeader {
        @Test
        void shouldDeleteContentAttributesAndReturn204() throws Exception {
            mvc.perform(delete(url)
                    .contentType("text/plain"))
                .andExpect(status().isNoContent());

            if (entity != null) {
                Optional<ContentEntity> fetched = repo.findById(entity.getId());
                assertThat(fetched.isPresent()).isTrue();
                assertThat(fetched.get().getContentId()).isNull();
                assertThat(fetched.get().getLen()).isNull();
                assertThat(fetched.get().getMimeType()).isNull();
                assertThat(store.getContent(fetched.get())).isNull();
            }
        }
    }

    @Nested
    @DisplayName("PUT to {store ID} with a non-matching If-Match header")
    class PutWithNonMatchingIfMatchHeader {
        @Test
        void shouldRespondWith412PreconditionFailed() throws Exception {
            mvc.perform(put(url)
                    .content("Hello Modified Spring Content World!")
                    .contentType("text/plain")
                    .header("if-match", "\"999\""))
                .andExpect(status().isPreconditionFailed());
        }
    }

    @Nested
    @DisplayName("PUT to {store ID} with a matching If-None-Match header")
    class PutWithMatchingIfNoneMatchHeader {
        @Test
        void shouldRespondWith412PreconditionFailed() throws Exception {
            mvc.perform(put(url)
                    .content("Hello Modified Spring Content World!")
                    .contentType("text/plain")
                    .header("if-none-match", etag))
                .andExpect(status().isPreconditionFailed());
        }
    }

    @Nested
    @DisplayName("PUT to {store ID} with a non-matching If-None-Match header")
    class PutWithNonMatchingIfNoneMatchHeader {
        @Test
        void shouldRespondWith200OKAndSetContent() throws Exception {
            mvc.perform(put(url)
                    .content("Hello Modified Spring Content World!")
                    .contentType("text/plain")
                    .header("if-none-match", "\"999\""))
                .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("PUT to {store ID} with matching If-Match and If-None-Match headers")
    class PutWithMatchingIfMatchAndIfNoneMatchHeaders {
        @Test
        void shouldRespondWith412PreconditionFailed() throws Exception {
            mvc.perform(put(url)
                    .content("Hello Modified Spring Content World!")
                    .contentType("text/plain")
                    .header("if-match", etag)
                    .header("if-none-match", etag))
                .andExpect(status().isPreconditionFailed());
        }
    }

    @Nested
    @DisplayName("POST to {store ID} with a non-matching If-Match header")
    class PostWithNonMatchingIfMatchHeader {
        @Test
        void shouldRespondWith412PreconditionFailed() throws Exception {
            mvc.perform(multipart(url)
                    .file(new MockMultipartFile("file",
                        "tests-file-modified.txt",
                        "text/plain", "Hello Spring Content World!".getBytes()))
                    .header("if-match", "\"999\""))
                .andExpect(status().isPreconditionFailed());
        }
    }

    @Nested
    @DisplayName("DELETE to {store ID} with a non-matching If-Match header")
    class DeleteWithNonMatchingIfMatchHeader {
        @Test
        void shouldRespondWith412PreconditionFailed() throws Exception {
            mvc.perform(delete(url)
                    .header("if-match", "\"999\""))
                .andExpect(status().isPreconditionFailed());
        }
    }
}
