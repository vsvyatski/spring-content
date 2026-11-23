package it.internal.org.springframework.content.rest.controllers.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.theoryinpractise.halbuilder.api.ReadableRepresentation;
import com.theoryinpractise.halbuilder.api.RepresentationFactory;
import com.theoryinpractise.halbuilder.standard.StandardRepresentationFactory;
import internal.org.springframework.content.rest.support.ContentEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.repository.CrudRepository;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

import java.io.StringReader;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class EntityTests {

    private MockMvc mvc;
    private String url;
    private String linkRel;
    private ContentEntity entity;
    private CrudRepository<ContentEntity, Long> repository;

    public static EntityTests tests() {
        return new EntityTests();
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

    public void setLinkRel(String linkRel) {
        this.linkRel = linkRel;
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

    @Nested
    @DisplayName("GET to {store ID} accepting HAL JSON")
    class GetAcceptingHalJson {
        @Test
        void shouldReturnEntity() throws Exception {
            MockHttpServletResponse response = mvc
                .perform(get(url)
                    .accept("application/hal+json"))
                .andExpect(status().isOk())
                .andReturn().getResponse();

            RepresentationFactory representationFactory = new StandardRepresentationFactory();
            ReadableRepresentation halResponse = representationFactory
                .readRepresentation("application/hal+json",
                    new StringReader(response.getContentAsString()));
            assertThat(halResponse.getLinksByRel(linkRel)).isNotNull();
            assertThat(halResponse.getLinksByRel(linkRel).size()).isEqualTo(1);
            assertThat(halResponse.getLinksByRel(linkRel).get(0).getHref()).matches("http://localhost" + url);
        }
    }

    @Nested
    @DisplayName("PUT to {store ID} with a JSON body")
    class PutWithJsonBody {
        @Test
        void shouldSetEntitiesDataAndReturn200() throws Exception {
            entity.setTitle("Spring Content");
            mvc.perform(put(url)
                    .content(new ObjectMapper().writeValueAsString(entity))
                    .contentType("application/hal+json"))
                .andExpect(status().is2xxSuccessful());

            Optional<ContentEntity> fetched = repository.findById(entity.getId());
            assertThat(fetched.isPresent()).isTrue();
            assertThat(fetched.get().getTitle()).isEqualTo("Spring Content");
            assertThat(fetched.get().getContentId()).isNull();
            assertThat(fetched.get().getLen()).isNull();
        }
    }

    @Nested
    @DisplayName("PATCH to {store ID} with a JSON body")
    class PatchWithJsonBody {
        @Test
        void shouldPatchEntityDataAndReturn200() throws Exception {
            mvc.perform(patch(url)
                    .content("{\"title\":\"Spring Content Modified\"}")
                    .contentType("application/hal+json"))
                .andExpect(status().is2xxSuccessful());

            Optional<ContentEntity> fetched = repository.findById(entity.getId());
            assertThat(fetched.isPresent()).isTrue();
            assertThat(fetched.get().getTitle()).isEqualTo("Spring Content Modified");
            assertThat(fetched.get().getContentId()).isNull();
            assertThat(fetched.get().getLen()).isNull();
            assertThat(fetched.get().getMimeType()).isNull();
        }
    }

    @Nested
    @DisplayName("HEAD to {store ID} with a JSON body")
    class HeadWithJsonBody {
        @Test
        void shouldReturn200() throws Exception {
            mvc.perform(head(url))
                .andExpect(status().is2xxSuccessful());
        }
    }
}
