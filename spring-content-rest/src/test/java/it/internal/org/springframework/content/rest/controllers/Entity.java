package it.internal.org.springframework.content.rest.controllers;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.assertj.core.api.Assertions.assertThat;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.StringReader;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.theoryinpractise.halbuilder.api.ReadableRepresentation;
import com.theoryinpractise.halbuilder.api.RepresentationFactory;
import com.theoryinpractise.halbuilder.standard.StandardRepresentationFactory;

import internal.org.springframework.content.rest.support.ContentEntity;
import com.fasterxml.jackson.core.JsonProcessingException;

public class Entity {

	private MockMvc mvc;
	private String url;
	private String linkRel;
	private ContentEntity entity;
	private CrudRepository repository;

	public static Entity tests() {
		return new Entity();
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

	public String getLinkRel() {
		return linkRel;
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

	public CrudRepository getRepository() {
		return repository;
	}

	public void setRepository(CrudRepository repository) {
		this.repository = repository;
	}

	
    @Nested
    class AGETToStoreIdAcceptingHalJson {
        @Test
        void shouldReturnTheEntity() throws Exception {
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
    class APUTToStoreIdWithAJsonBody {
        @Test
        void shouldSetEntitiesDataAndReturn200() throws Exception, JsonProcessingException {
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
    class APATCHToStoreIdWithAJsonBody {
        @Test
        void shouldPatchTheEntityDataAndReturn200() throws Exception {
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
    class AHEADToStoreIdWithAJsonBody {
        @Test
        void shouldReturn200() throws Exception {
            mvc.perform(head(url))
            						.andExpect(status().is2xxSuccessful());
        }
    }

}
