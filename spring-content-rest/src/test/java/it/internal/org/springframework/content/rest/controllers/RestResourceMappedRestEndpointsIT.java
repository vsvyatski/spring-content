package it.internal.org.springframework.content.rest.controllers;

import org.junit.jupiter.api.extension.ExtendWith;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.theoryinpractise.halbuilder.api.ReadableRepresentation;
import com.theoryinpractise.halbuilder.api.RepresentationFactory;
import com.theoryinpractise.halbuilder.standard.StandardRepresentationFactory;
import internal.org.springframework.content.rest.support.*;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.rest.config.HypermediaConfiguration;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.core.io.WritableResource;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.*;
import com.fasterxml.jackson.core.JsonProcessingException;

import static java.lang.String.format;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.hamcrest.Matchers.is;
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
public class RestResourceMappedRestEndpointsIT {

   @Autowired private TestEntity11Repository repo;
   @Autowired private TestEntity11Store store;

   private TestEntity11 testEntity11;

	@Autowired
   private WebApplicationContext context;

   private Version versionTests;
   private LastModifiedDate lastModifiedDateTests;

   private MockMvc mvc;

   
    @Nested
    class RestResourceMappedRESTEndpoints {
        @Nested
        class GivenAnEntityWithASimpleContentProperty {
            @Nested
            class GivenARequestToANonExistentEntity {
                @BeforeEach
                void setUp() {
                    mvc = MockMvcBuilders.webAppContextSetup(context).build();

                    testEntity11 = repo.save(new TestEntity11());

                }

                @Test
                void shouldReturn404() throws Exception {
                    mvc.perform(
                            get("/testEntity11s/9999999/package/content"))
                            .andExpect(status().isNotFound());

                }

            }

            @Nested
            class GivenARequestToANonExistentContentProperty {
                @BeforeEach
                void setUp() {
                    mvc = MockMvcBuilders.webAppContextSetup(context).build();

                    testEntity11 = repo.save(new TestEntity11());

                }

                @Test
                void shouldReturn404() throws Exception {
                    mvc.perform(
                            get("/testEntity11s/" + testEntity11.getId() + "/doesnotexist"))
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

                        testEntity11 = repo.save(new TestEntity11());

                    }

                    @Test
                    void shouldReturn404() throws Exception {
                        mvc.perform(
                          get("/testEntity11s/" + testEntity11.getId() + "/package/content"))
                          .andExpect(status().isNotFound());

                    }

                }

                @Nested
                class APUTToRepositoryIdContentProperty {
                    @BeforeEach
                    void setUp() {
                        mvc = MockMvcBuilders.webAppContextSetup(context).build();

                        testEntity11 = repo.save(new TestEntity11());

                    }

                    @Test
                    void shouldCreateTheContent() throws Exception {
                        mvc.perform(
                          put("/testEntity11s/" + testEntity11.getId() + "/package/content")
                        		  .content("Hello New Spring Content World!")
                        		  .contentType("text/plain"))
                          .andExpect(status().is2xxSuccessful());

                        Optional<TestEntity11> fetched = repo.findById(testEntity11.getId());
                        assertThat(fetched.isPresent()).isTrue();
                        assertThat(fetched.get().get_package().contentId).isNotNull();
                        assertThat(fetched.get().get_package().contentLen).isEqualTo(31L);
                        assertThat(fetched.get().get_package().contentMimeType).isEqualTo("text/plain");
                        try (InputStream actual = store.getResource(fetched.get(), PropertyPath.from("_package/content")).getInputStream()) {
                            IOUtils.contentEquals(actual, new ByteArrayInputStream("Hello New Spring Content World!".getBytes()));
                        }

                    }

                }

                @Nested
                class APUTToStoreIdWithJsonContent {
                    @BeforeEach
                    void setUp() {
                        mvc = MockMvcBuilders.webAppContextSetup(context).build();

                        testEntity11 = repo.save(new TestEntity11());

                    }

                    @Test
                    void shouldSetTheContentAndReturn201() throws Exception {
                        String content = "{\"content\":\"Hello New Spring Content World!\"}";
                        mvc.perform(
                                       put("/testEntity11s/" + testEntity11.getId() + "/package/content")
                                       .content(content)
                                       .contentType("application/json"))
                        .andExpect(status().isCreated());

                        Optional<TestEntity11> fetched = repo.findById(testEntity11.getId());
                        assertThat(fetched.isPresent()).isTrue();
                        assertThat(fetched.get().get_package().getContentId()).isNotNull();
                        assertThat(fetched.get().get_package().getContentLen()).isEqualTo(45L);
                        assertThat(fetched.get().get_package().getContentMimeType()).isEqualTo("application/json");
                                 try (InputStream actual = store.getResource(fetched.get(), PropertyPath.from("_package/content")).getInputStream()) {
                                     IOUtils.contentEquals(actual, new ByteArrayInputStream(content.getBytes()));
                                 }

                    }

                }

            }

            @Nested
            class GivenThatItHasContent {
                @Nested
                class AGETToRepositoryIdForTheEntityJson {
                    @BeforeEach
                    void setUp() throws IOException {
                        mvc = MockMvcBuilders.webAppContextSetup(context).build();

                        testEntity11 = repo.save(new TestEntity11());

                        String content = "Hello Spring Content World!";
                        testEntity11.get_package().contentMimeType = "text/plain";
                        UUID contentId = UUID.randomUUID();
                        store.associate(testEntity11, PropertyPath.from("_package/content"), contentId);
                        WritableResource r = (WritableResource)store.getResource(testEntity11, PropertyPath.from("_package/content"));
                        try (OutputStream out = r.getOutputStream()) {
                        				      out.write(content.getBytes());
                        				  }
                        				  testEntity11 = repo.save(testEntity11);
                        versionTests.setMvc(mvc);
                        versionTests.setUrl("/testEntity11s/" + testEntity11.getId() + "/package/content");
                        versionTests.setCollectionUrl("/testEntity11s");
                        versionTests.setContentLinkRel("package/content");
                        versionTests.setRepo(repo);
                        versionTests.setStore(store);
                        versionTests.setEtag(format("\"%s\"", testEntity11.getVersion()));
                        lastModifiedDateTests.setMvc(mvc);
                        lastModifiedDateTests.setUrl("/testEntity11s/" + testEntity11.getId() + "/package/content");
                        lastModifiedDateTests.setLastModifiedDate(testEntity11.getModifiedDate());
                        lastModifiedDateTests.setEtag(testEntity11.getVersion().toString());
                        lastModifiedDateTests.setContent(content);
                    }

                    @Test
                    void shouldReturnTheMappedContentLinks() throws Exception, JsonProcessingException {
                        MockHttpServletResponse res = mvc.perform(
                        		  get("/testEntity11s/" + testEntity11.getId())
                        				  .accept("application/hal+json"))
                          .andExpect(status().is2xxSuccessful())
                          .andReturn().getResponse();

                        ObjectMapper mapper = new ObjectMapper();
                        Map<String,Object> obj = mapper.readValue(res.getContentAsString(), Map.class);

                        Object val = parse(obj, "_links", "package/content", "href");
                        assertThat(val).isNotNull();
                        assertThat(val.toString()).matches("http://localhost/testEntity11s/.*/package/content");

                    }

                }

                @Nested
                class AGETToRepositoryIdContentProperty {
                    @BeforeEach
                    void setUp() throws IOException {
                        mvc = MockMvcBuilders.webAppContextSetup(context).build();

                        testEntity11 = repo.save(new TestEntity11());

                        String content = "Hello Spring Content World!";
                        testEntity11.get_package().contentMimeType = "text/plain";
                        UUID contentId = UUID.randomUUID();
                        store.associate(testEntity11, PropertyPath.from("_package/content"), contentId);
                        WritableResource r = (WritableResource)store.getResource(testEntity11, PropertyPath.from("_package/content"));
                        try (OutputStream out = r.getOutputStream()) {
                        				      out.write(content.getBytes());
                        				  }
                        				  testEntity11 = repo.save(testEntity11);
                        versionTests.setMvc(mvc);
                        versionTests.setUrl("/testEntity11s/" + testEntity11.getId() + "/package/content");
                        versionTests.setCollectionUrl("/testEntity11s");
                        versionTests.setContentLinkRel("package/content");
                        versionTests.setRepo(repo);
                        versionTests.setStore(store);
                        versionTests.setEtag(format("\"%s\"", testEntity11.getVersion()));
                        lastModifiedDateTests.setMvc(mvc);
                        lastModifiedDateTests.setUrl("/testEntity11s/" + testEntity11.getId() + "/package/content");
                        lastModifiedDateTests.setLastModifiedDate(testEntity11.getModifiedDate());
                        lastModifiedDateTests.setEtag(testEntity11.getVersion().toString());
                        lastModifiedDateTests.setContent(content);
                    }

                    @Test
                    void shouldReturnTheContent() throws Exception {
                        MockHttpServletResponse response = mvc
                          .perform(get("/testEntity11s/" + testEntity11.getId() + "/package/content")
                        		  .accept("text/plain"))
                          .andExpect(status().isOk())
                          .andExpect(header().string("etag", is("\"1\"")))
                          .andExpect(header().string("last-modified", LastModifiedDate
                        		  .isWithinASecond(testEntity11.getModifiedDate())))
                          .andReturn().getResponse();

                        assertThat(response).isNotNull();
                        assertThat(response.getContentAsString()).isEqualTo("Hello Spring Content World!");

                    }

                }

                @Nested
                class AGETToRepositoryIdContentPropertyWithAMimeTypeThatMatchesARenderer {
                    @BeforeEach
                    void setUp() throws IOException {
                        mvc = MockMvcBuilders.webAppContextSetup(context).build();

                        testEntity11 = repo.save(new TestEntity11());

                        String content = "Hello Spring Content World!";
                        testEntity11.get_package().contentMimeType = "text/plain";
                        UUID contentId = UUID.randomUUID();
                        store.associate(testEntity11, PropertyPath.from("_package/content"), contentId);
                        WritableResource r = (WritableResource)store.getResource(testEntity11, PropertyPath.from("_package/content"));
                        try (OutputStream out = r.getOutputStream()) {
                        				      out.write(content.getBytes());
                        				  }
                        				  testEntity11 = repo.save(testEntity11);
                        versionTests.setMvc(mvc);
                        versionTests.setUrl("/testEntity11s/" + testEntity11.getId() + "/package/content");
                        versionTests.setCollectionUrl("/testEntity11s");
                        versionTests.setContentLinkRel("package/content");
                        versionTests.setRepo(repo);
                        versionTests.setStore(store);
                        versionTests.setEtag(format("\"%s\"", testEntity11.getVersion()));
                        lastModifiedDateTests.setMvc(mvc);
                        lastModifiedDateTests.setUrl("/testEntity11s/" + testEntity11.getId() + "/package/content");
                        lastModifiedDateTests.setLastModifiedDate(testEntity11.getModifiedDate());
                        lastModifiedDateTests.setEtag(testEntity11.getVersion().toString());
                        lastModifiedDateTests.setContent(content);
                    }

                    @Test
                    void shouldReturnTheRenditionAnd200() throws Exception {
                        MockHttpServletResponse response = mvc
                          .perform(get(
                        		  "/testEntity11s/" + testEntity11.getId() + "/package/content")
                        		  .accept("text/html"))
                          .andExpect(status().isOk()).andReturn()
                          .getResponse();

                        assertThat(response).isNotNull();
                        assertThat(response.getContentAsString()).isEqualTo("<html><body>Hello Spring Content World!</body></html>");

                    }

                }

                @Nested
                class AGETToRepositoryIdContentPropertyWithMultipleMimeTypesTheLastOfWhichMatchesTheCo {
                    @BeforeEach
                    void setUp() throws IOException {
                        mvc = MockMvcBuilders.webAppContextSetup(context).build();

                        testEntity11 = repo.save(new TestEntity11());

                        String content = "Hello Spring Content World!";
                        testEntity11.get_package().contentMimeType = "text/plain";
                        UUID contentId = UUID.randomUUID();
                        store.associate(testEntity11, PropertyPath.from("_package/content"), contentId);
                        WritableResource r = (WritableResource)store.getResource(testEntity11, PropertyPath.from("_package/content"));
                        try (OutputStream out = r.getOutputStream()) {
                        				      out.write(content.getBytes());
                        				  }
                        				  testEntity11 = repo.save(testEntity11);
                        versionTests.setMvc(mvc);
                        versionTests.setUrl("/testEntity11s/" + testEntity11.getId() + "/package/content");
                        versionTests.setCollectionUrl("/testEntity11s");
                        versionTests.setContentLinkRel("package/content");
                        versionTests.setRepo(repo);
                        versionTests.setStore(store);
                        versionTests.setEtag(format("\"%s\"", testEntity11.getVersion()));
                        lastModifiedDateTests.setMvc(mvc);
                        lastModifiedDateTests.setUrl("/testEntity11s/" + testEntity11.getId() + "/package/content");
                        lastModifiedDateTests.setLastModifiedDate(testEntity11.getModifiedDate());
                        lastModifiedDateTests.setEtag(testEntity11.getVersion().toString());
                        lastModifiedDateTests.setContent(content);
                    }

                    @Test
                    void shouldReturnTheOriginalContentAnd200() throws Exception {
                        MockHttpServletResponse response = mvc
                          .perform(get("/testEntity11s/" + testEntity11.getId() + "/package/content").accept(
                        		  new String[] {"text/xml",
                        				  "text/plain"}))
                          .andExpect(status().isOk()).andReturn()
                          .getResponse();

                        assertThat(response).isNotNull();
                        assertThat(response.getContentAsString()).isEqualTo("Hello Spring Content World!");

                    }

                }

                @Nested
                class APUTToRepositoryIdContentProperty {
                    @BeforeEach
                    void setUp() throws IOException {
                        mvc = MockMvcBuilders.webAppContextSetup(context).build();

                        testEntity11 = repo.save(new TestEntity11());

                        String content = "Hello Spring Content World!";
                        testEntity11.get_package().contentMimeType = "text/plain";
                        UUID contentId = UUID.randomUUID();
                        store.associate(testEntity11, PropertyPath.from("_package/content"), contentId);
                        WritableResource r = (WritableResource)store.getResource(testEntity11, PropertyPath.from("_package/content"));
                        try (OutputStream out = r.getOutputStream()) {
                        				      out.write(content.getBytes());
                        				  }
                        				  testEntity11 = repo.save(testEntity11);
                        versionTests.setMvc(mvc);
                        versionTests.setUrl("/testEntity11s/" + testEntity11.getId() + "/package/content");
                        versionTests.setCollectionUrl("/testEntity11s");
                        versionTests.setContentLinkRel("package/content");
                        versionTests.setRepo(repo);
                        versionTests.setStore(store);
                        versionTests.setEtag(format("\"%s\"", testEntity11.getVersion()));
                        lastModifiedDateTests.setMvc(mvc);
                        lastModifiedDateTests.setUrl("/testEntity11s/" + testEntity11.getId() + "/package/content");
                        lastModifiedDateTests.setLastModifiedDate(testEntity11.getModifiedDate());
                        lastModifiedDateTests.setEtag(testEntity11.getVersion().toString());
                        lastModifiedDateTests.setContent(content);
                    }

                    @Test
                    void shouldCreateTheContent() throws Exception {
                        mvc.perform(
                          put("/testEntity11s/" + testEntity11.getId() + "/package/content")
                        		  .content("Hello New Spring Content World!")
                        		  .contentType("text/plain"))
                          .andExpect(status().is2xxSuccessful());

                        Optional<TestEntity11> fetched = repo.findById(testEntity11.getId());
                        assertThat(fetched.isPresent()).isTrue();
                        assertThat(fetched.get().get_package().contentId).isNotNull();
                        assertThat(fetched.get().get_package().contentLen).isEqualTo(31L);
                        assertThat(fetched.get().get_package().contentMimeType).isEqualTo("text/plain");

                    }

                }

                @Nested
                class ADELETEToRepositoryIdContentProperty {
                    @BeforeEach
                    void setUp() throws IOException {
                        mvc = MockMvcBuilders.webAppContextSetup(context).build();

                        testEntity11 = repo.save(new TestEntity11());

                        String content = "Hello Spring Content World!";
                        testEntity11.get_package().contentMimeType = "text/plain";
                        UUID contentId = UUID.randomUUID();
                        store.associate(testEntity11, PropertyPath.from("_package/content"), contentId);
                        WritableResource r = (WritableResource)store.getResource(testEntity11, PropertyPath.from("_package/content"));
                        try (OutputStream out = r.getOutputStream()) {
                        				      out.write(content.getBytes());
                        				  }
                        				  testEntity11 = repo.save(testEntity11);
                        versionTests.setMvc(mvc);
                        versionTests.setUrl("/testEntity11s/" + testEntity11.getId() + "/package/content");
                        versionTests.setCollectionUrl("/testEntity11s");
                        versionTests.setContentLinkRel("package/content");
                        versionTests.setRepo(repo);
                        versionTests.setStore(store);
                        versionTests.setEtag(format("\"%s\"", testEntity11.getVersion()));
                        lastModifiedDateTests.setMvc(mvc);
                        lastModifiedDateTests.setUrl("/testEntity11s/" + testEntity11.getId() + "/package/content");
                        lastModifiedDateTests.setLastModifiedDate(testEntity11.getModifiedDate());
                        lastModifiedDateTests.setEtag(testEntity11.getVersion().toString());
                        lastModifiedDateTests.setContent(content);
                    }

                    @Test
                    void shouldDeleteTheContent() throws Exception {
                        mvc.perform(delete(
                          "/testEntity11s/" + testEntity11.getId() + "/package/content"))
                          .andExpect(status().isNoContent());

                        Optional<TestEntity11> fetched = repo.findById(testEntity11.getId());
                        assertThat(fetched.isPresent()).isTrue();
                        assertThat(fetched.get().get_package().contentId).isNull();
                        assertThat(fetched.get().get_package().contentLen).isNull();
                                       assertThat(fetched.get().get_package().contentMimeType).isNull();

                    }

                }

            }

        }

    }

	private Object parse(Map<String, Object> obj, String... path) {
		Object current = null;
		current = obj.get(path[0]);
		if (current instanceof Map) {
			current = parse((Map<String,Object>)current, Arrays.copyOfRange(path, 1, path.length));
		}
		return current;
	}

}
