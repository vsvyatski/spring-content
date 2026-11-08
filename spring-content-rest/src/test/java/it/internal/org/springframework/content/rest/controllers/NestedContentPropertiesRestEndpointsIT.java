package it.internal.org.springframework.content.rest.controllers;

import org.junit.jupiter.api.extension.ExtendWith;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static java.lang.String.format;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Optional;
import java.util.TimeZone;
import java.util.UUID;

import internal.org.springframework.content.rest.support.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.rest.config.HypermediaConfiguration;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.core.io.WritableResource;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;

@WebAppConfiguration
@ContextConfiguration(classes = {
      StoreConfig.class,
      DelegatingWebMvcConfiguration.class,
      RepositoryRestMvcConfiguration.class,
      RestConfiguration.class,
	  HypermediaConfiguration.class
})
@Transactional
@ActiveProfiles("store")
@ExtendWith(SpringExtension.class)
public class NestedContentPropertiesRestEndpointsIT {

   @Autowired private TestEntity10Repository repository;
   @Autowired private TestEntity10Store store;

   private TestEntity10 testEntity10;

	@Autowired
   private WebApplicationContext context;

   private Version versionTests;
   private LastModifiedDate lastModifiedDateTests;

   private MockMvc mvc;

    @Nested
    class NestedContentPropertiesRESTEndpoints {
        @Nested
        class GivenAnEntityWithASimpleContentProperty {
            @Nested
            class GivenARequestToANonExistentEntity {
                @BeforeEach
                void setUp() {
                    mvc = MockMvcBuilders.webAppContextSetup(context).build();

                    testEntity10 = repository.save(new TestEntity10());
                }

                @Test
                void shouldReturn404() throws Exception {
                    mvc.perform(
                            get("/testEntity10s/9999999/foo"))
                            .andExpect(status().isNotFound());
                }

            }

            @Nested
            class GivenARequestToANonExistentContentProperty {
                @BeforeEach
                void setUp() {
                    mvc = MockMvcBuilders.webAppContextSetup(context).build();

                    testEntity10 = repository.save(new TestEntity10());
                }

                @Test
                void shouldReturn404() throws Exception {
                    mvc.perform(
                            get("/testEntity10s/" + testEntity10.getId() + "/doesnotexist"))
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

                        testEntity10 = repository.save(new TestEntity10());
                    }

                    @Test
                    void shouldReturn404() throws Exception {
                        mvc.perform(
                          get("/testEntity10s/" + testEntity10.getId() + "/child"))
                          .andExpect(status().isNotFound());
                    }

                }

                @Nested
                class APUTToRepositoryIdPropertyContentProperty {
                    @BeforeEach
                    void setUp() {
                        mvc = MockMvcBuilders.webAppContextSetup(context).build();

                        testEntity10 = repository.save(new TestEntity10());
                    }

                    @Test
                    void shouldCreateTheContent() throws Exception {
                        mvc.perform(
                          put("/testEntity10s/" + testEntity10.getId() + "/child/content")
                        		  .content("Hello New Spring Content World!")
                        		  .contentType("text/plain"))
                          .andExpect(status().is2xxSuccessful());

                        Optional<TestEntity10> fetched = repository.findById(testEntity10.getId());
                        assertThat(fetched.isPresent()).isTrue();
                        assertThat(fetched.get().getChild().contentId).isNotNull();
                        assertThat(fetched.get().getChild().contentLen).isEqualTo(31L);
                        assertThat(fetched.get().getChild().contentMimeType).isEqualTo("text/plain");
                        try (InputStream actual = store.getResource(fetched.get(), PropertyPath.from("child/content")).getInputStream()) {
                            IOUtils.contentEquals(actual, new ByteArrayInputStream("Hello New Spring Content World!".getBytes()));
                        }

                                       mvc.perform(
                                                 put("/testEntity10s/" + testEntity10.getId() + "/child/preview")
                                                         .content("Hello New Spring Content Preview World!")
                                                         .contentType("text/plain"))
                                                 .andExpect(status().is2xxSuccessful());

                                       fetched = repository.findById(testEntity10.getId());
                                       assertThat(fetched.isPresent()).isTrue();
                                       assertThat(fetched.get().getChild().getPreviewId()).isNotNull();
                                       assertThat(fetched.get().getChild().getPreviewLen()).isEqualTo(39L);
                                       assertThat(fetched.get().getChild().getPreviewMimeType()).isEqualTo("text/plain");
                                       try (InputStream actual = store.getResource(fetched.get(), PropertyPath.from("child/preview")).getInputStream()) {
                                           IOUtils.contentEquals(actual, new ByteArrayInputStream("Hello New Spring Content Preview World!".getBytes()));
                                       }
                    }

                }

                @Nested
                class APUTToStoreIdPropertyContentPropertyWithJsonContent {
                    @BeforeEach
                    void setUp() {
                        mvc = MockMvcBuilders.webAppContextSetup(context).build();

                        testEntity10 = repository.save(new TestEntity10());
                    }

                    @Test
                    void shouldSetTheContentAndReturn201() throws Exception {
                        String content = "{\"content\":\"Hello New Spring Content World!\"}";
                        mvc.perform(
                                       put("/testEntity10s/" + testEntity10.getId() + "/child/content")
                                       .content(content)
                                       .contentType("application/json"))
                        .andExpect(status().isCreated());

                        Optional<TestEntity10> fetched = repository.findById(testEntity10.getId());
                        assertThat(fetched.isPresent()).isTrue();
                        assertThat(fetched.get().getChild().getContentId()).isNotNull();
                        assertThat(fetched.get().getChild().getContentLen()).isEqualTo(45L);
                        assertThat(fetched.get().getChild().getContentMimeType()).isEqualTo("application/json");
                                 try (InputStream actual = store.getResource(fetched.get(), PropertyPath.from("child/content")).getInputStream()) {
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

                        testEntity10 = repository.save(new TestEntity10());

                        String content = "Hello Spring Content World!";
                        testEntity10.getChild().contentMimeType = "text/plain";
                        UUID contentId = UUID.randomUUID();
                        store.associate(testEntity10, PropertyPath.from("child/content"), contentId);
                        WritableResource r = (WritableResource)store.getResource(testEntity10, PropertyPath.from("child/content"));
                        try (OutputStream out = r.getOutputStream()) {
                        				      out.write(content.getBytes());
                        				  }
                        				  testEntity10 = repository.save(testEntity10);
                        versionTests.setMvc(mvc);
                        versionTests.setUrl("/testEntity10s/" + testEntity10.getId() + "/child/content");
                        versionTests.setCollectionUrl("/testEntity10s");
                        versionTests.setContentLinkRel("child/content");
                        versionTests.setRepo(repository);
                        versionTests.setStore(store);
                        versionTests.setEtag(format("\"%s\"", testEntity10.getVersion()));
                        lastModifiedDateTests.setMvc(mvc);
                        lastModifiedDateTests.setUrl("/testEntity10s/" + testEntity10.getId() + "/child/content");
                        lastModifiedDateTests.setLastModifiedDate(testEntity10.getModifiedDate());
                        lastModifiedDateTests.setEtag(testEntity10.getVersion().toString());
                        lastModifiedDateTests.setContent(content);
                    }

                    @Test
                    void shouldReturnTheContent() throws Exception {
                        MockHttpServletResponse response = mvc
                          .perform(get("/testEntity10s/" + testEntity10.getId() + "/child/content")
                        		  .accept("text/plain"))
                          .andExpect(status().isOk())
                          .andExpect(header().string("etag", is("\"1\"")))
                          .andExpect(header().string("last-modified", LastModifiedDate
                        		  .isWithinASecond(testEntity10.getModifiedDate())))
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

                        testEntity10 = repository.save(new TestEntity10());

                        String content = "Hello Spring Content World!";
                        testEntity10.getChild().contentMimeType = "text/plain";
                        UUID contentId = UUID.randomUUID();
                        store.associate(testEntity10, PropertyPath.from("child/content"), contentId);
                        WritableResource r = (WritableResource)store.getResource(testEntity10, PropertyPath.from("child/content"));
                        try (OutputStream out = r.getOutputStream()) {
                        				      out.write(content.getBytes());
                        				  }
                        				  testEntity10 = repository.save(testEntity10);
                        versionTests.setMvc(mvc);
                        versionTests.setUrl("/testEntity10s/" + testEntity10.getId() + "/child/content");
                        versionTests.setCollectionUrl("/testEntity10s");
                        versionTests.setContentLinkRel("child/content");
                        versionTests.setRepo(repository);
                        versionTests.setStore(store);
                        versionTests.setEtag(format("\"%s\"", testEntity10.getVersion()));
                        lastModifiedDateTests.setMvc(mvc);
                        lastModifiedDateTests.setUrl("/testEntity10s/" + testEntity10.getId() + "/child/content");
                        lastModifiedDateTests.setLastModifiedDate(testEntity10.getModifiedDate());
                        lastModifiedDateTests.setEtag(testEntity10.getVersion().toString());
                        lastModifiedDateTests.setContent(content);
                    }

                    @Test
                    void shouldReturnTheRenditionAnd200() throws Exception {
                        MockHttpServletResponse response = mvc
                          .perform(get(
                        		  "/testEntity10s/" + testEntity10.getId()
                        				  + "/child/content")
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

                        testEntity10 = repository.save(new TestEntity10());

                        String content = "Hello Spring Content World!";
                        testEntity10.getChild().contentMimeType = "text/plain";
                        UUID contentId = UUID.randomUUID();
                        store.associate(testEntity10, PropertyPath.from("child/content"), contentId);
                        WritableResource r = (WritableResource)store.getResource(testEntity10, PropertyPath.from("child/content"));
                        try (OutputStream out = r.getOutputStream()) {
                        				      out.write(content.getBytes());
                        				  }
                        				  testEntity10 = repository.save(testEntity10);
                        versionTests.setMvc(mvc);
                        versionTests.setUrl("/testEntity10s/" + testEntity10.getId() + "/child/content");
                        versionTests.setCollectionUrl("/testEntity10s");
                        versionTests.setContentLinkRel("child/content");
                        versionTests.setRepo(repository);
                        versionTests.setStore(store);
                        versionTests.setEtag(format("\"%s\"", testEntity10.getVersion()));
                        lastModifiedDateTests.setMvc(mvc);
                        lastModifiedDateTests.setUrl("/testEntity10s/" + testEntity10.getId() + "/child/content");
                        lastModifiedDateTests.setLastModifiedDate(testEntity10.getModifiedDate());
                        lastModifiedDateTests.setEtag(testEntity10.getVersion().toString());
                        lastModifiedDateTests.setContent(content);
                    }

                    @Test
                    void shouldReturnTheOriginalContentAnd200() throws Exception {
                        MockHttpServletResponse response = mvc
                          .perform(get("/testEntity10s/"
                        		  + testEntity10.getId()
                        		  + "/child/content").accept(
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

                        testEntity10 = repository.save(new TestEntity10());

                        String content = "Hello Spring Content World!";
                        testEntity10.getChild().contentMimeType = "text/plain";
                        UUID contentId = UUID.randomUUID();
                        store.associate(testEntity10, PropertyPath.from("child/content"), contentId);
                        WritableResource r = (WritableResource)store.getResource(testEntity10, PropertyPath.from("child/content"));
                        try (OutputStream out = r.getOutputStream()) {
                        				      out.write(content.getBytes());
                        				  }
                        				  testEntity10 = repository.save(testEntity10);
                        versionTests.setMvc(mvc);
                        versionTests.setUrl("/testEntity10s/" + testEntity10.getId() + "/child/content");
                        versionTests.setCollectionUrl("/testEntity10s");
                        versionTests.setContentLinkRel("child/content");
                        versionTests.setRepo(repository);
                        versionTests.setStore(store);
                        versionTests.setEtag(format("\"%s\"", testEntity10.getVersion()));
                        lastModifiedDateTests.setMvc(mvc);
                        lastModifiedDateTests.setUrl("/testEntity10s/" + testEntity10.getId() + "/child/content");
                        lastModifiedDateTests.setLastModifiedDate(testEntity10.getModifiedDate());
                        lastModifiedDateTests.setEtag(testEntity10.getVersion().toString());
                        lastModifiedDateTests.setContent(content);
                    }

                    @Test
                    void shouldCreateTheContent() throws Exception {
                        mvc.perform(
                          put("/testEntity10s/" + testEntity10.getId() + "/child/content")
                        		  .content("Hello New Spring Content World!")
                        		  .contentType("text/plain"))
                          .andExpect(status().is2xxSuccessful());

                        Optional<TestEntity10> fetched = repository
                          .findById(testEntity10.getId());
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

                        testEntity10 = repository.save(new TestEntity10());

                        String content = "Hello Spring Content World!";
                        testEntity10.getChild().contentMimeType = "text/plain";
                        UUID contentId = UUID.randomUUID();
                        store.associate(testEntity10, PropertyPath.from("child/content"), contentId);
                        WritableResource r = (WritableResource)store.getResource(testEntity10, PropertyPath.from("child/content"));
                        try (OutputStream out = r.getOutputStream()) {
                        				      out.write(content.getBytes());
                        				  }
                        				  testEntity10 = repository.save(testEntity10);
                        versionTests.setMvc(mvc);
                        versionTests.setUrl("/testEntity10s/" + testEntity10.getId() + "/child/content");
                        versionTests.setCollectionUrl("/testEntity10s");
                        versionTests.setContentLinkRel("child/content");
                        versionTests.setRepo(repository);
                        versionTests.setStore(store);
                        versionTests.setEtag(format("\"%s\"", testEntity10.getVersion()));
                        lastModifiedDateTests.setMvc(mvc);
                        lastModifiedDateTests.setUrl("/testEntity10s/" + testEntity10.getId() + "/child/content");
                        lastModifiedDateTests.setLastModifiedDate(testEntity10.getModifiedDate());
                        lastModifiedDateTests.setEtag(testEntity10.getVersion().toString());
                        lastModifiedDateTests.setContent(content);
                    }

                    @Test
                    void shouldDeleteTheContent() throws Exception {
                        mvc.perform(delete(
                          "/testEntity10s/" + testEntity10.getId() + "/child/content"))
                          .andExpect(status().isNoContent());

                        Optional<TestEntity10> fetched = repository.findById(testEntity10.getId());
                        assertThat(fetched.isPresent()).isTrue();
                        assertThat(fetched.get().getChild().contentId).isNull();
                        assertThat(fetched.get().getChild().contentLen).isNull();
                                       assertThat(fetched.get().getChild().contentMimeType).isNull();
                    }

                }

            }

        }

        @Nested
        class GivenAPOSTToTheEntityEndpointWithAMultipartFormRequest {
            @BeforeEach
            void setUp() {
                mvc = MockMvcBuilders.webAppContextSetup(context).build();
            }

            @Test
            void shouldCreateANewEntityAndItsContentAndRespondWithA201Created() throws Exception {
                // assert content does not exist
                String content = "This is some new content";
                String previewContent = "This is some new preview content";

                MockMultipartFile file1 = new MockMultipartFile("child/content", "filename.txt", "text/plain", content.getBytes());
                MockMultipartFile file2 = new MockMultipartFile("child/preview", "preview.txt", "text/plain", previewContent.getBytes());

                // POST the new content
                MockHttpServletResponse response = mvc.perform(multipart("/testEntity10s")
                		  .file(file1)
                		  .file(file2)
                		  .param("name", "foo")
                		  .param("hidden", "bar"))
                  .andExpect(status().isCreated())
                  .andReturn().getResponse();

                String location = response.getHeader("Location");

                Optional<TestEntity10> fetchedEntity = repository.findById(Long.valueOf(StringUtils.substringAfterLast(location, "/")));
                assertThat(fetchedEntity.get().getHidden()).isNull();

                // assert that it now exists
                response = mvc.perform(get(location + "/child/content")
                		  .accept("text/plain"))
                  .andExpect(status().isOk())
                  .andReturn().getResponse();

                assertThat(response.getContentAsString()).isEqualTo(content);

                response = mvc.perform(get(location + "/child/preview")
                		  .accept("text/plain"))
                  .andExpect(status().isOk())
                  .andReturn().getResponse();

                assertThat(response.getContentAsString()).isEqualTo(previewContent);
            }

        }

    }

   private static String toHeaderDateFormat(Date dt) {
      SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
      format.setTimeZone(TimeZone.getTimeZone("GMT"));
      return format.format(dt);
   }

}
