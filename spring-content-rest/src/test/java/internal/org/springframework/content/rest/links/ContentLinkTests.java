package internal.org.springframework.content.rest.links;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.assertj.core.api.Assertions.assertThat;

import com.theoryinpractise.halbuilder.api.ReadableRepresentation;
import com.theoryinpractise.halbuilder.api.RepresentationFactory;
import com.theoryinpractise.halbuilder.standard.StandardRepresentationFactory;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.commons.store.Store;
import org.springframework.data.repository.CrudRepository;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

import java.io.StringReader;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ContentLinkTests {

	private MockMvc mvc;

	private CrudRepository repository;
	private Store store;

	private Object testEntity;
	private String url;
	private String contextPath = "";
	private String linkRel;
	private String expectedLinkRegex;

	public void setMvc(MockMvc mvc) {
		this.mvc = mvc;
	}

	public void setRepository(CrudRepository repository) {
		this.repository = repository;
	}

	public void setStore(Store store) {
		this.store = store;
	}

	public void setTestEntity(Object testEntity) {
		this.testEntity = testEntity;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public void setContextPath(String contextPath) {
		this.contextPath = contextPath;
	}

	public void setLinkRel(String linkRel) {
		this.linkRel = linkRel;
	}

	public void setExpectedLinkRegex(String expectedLinkRegex) {
		this.expectedLinkRegex = expectedLinkRegex;
	}

    @Nested
    class GivenContentIsAssociated {
        @Nested
        class AGETToApiRepositoryId {
            @BeforeEach
            void setUp() {
            }
            @Test
            void shouldProvideAResponseWithAContentLink() throws Exception {
                MockHttpServletResponse response = mvc.perform(get(url)
                									.accept("application/hal+json")
                									.contextPath(contextPath))
                							.andExpect(status().isOk()).andReturn().getResponse();
                					assertThat(response).isNotNull();

                					RepresentationFactory representationFactory = new StandardRepresentationFactory();
                					ReadableRepresentation halResponse = representationFactory
                							.readRepresentation("application/hal+json",
                									new StringReader(response.getContentAsString()));

                					assertThat(halResponse).isNotNull();
                					assertThat(halResponse.getLinksByRel(linkRel)).isNotNull();
                					assertThat(halResponse.getLinksByRel(linkRel)).extracting("href").anySatisfy(href -> assertThat(href.toString()).matches(expectedLinkRegex));
            }
        }
    }

}
