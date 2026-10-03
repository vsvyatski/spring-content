package it.internal.org.springframework.content.rest.controllers;

import org.junit.jupiter.api.extension.ExtendWith;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.ByteArrayInputStream;
import java.util.Date;
import java.util.UUID;

import internal.org.springframework.content.rest.support.StoreConfig;
import internal.org.springframework.content.rest.support.TestStore;
import org.apache.commons.io.IOUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.core.io.Resource;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebAppConfiguration
@ContextConfiguration(classes = { StoreConfig.class, DelegatingWebMvcConfiguration.class,
		RepositoryRestMvcConfiguration.class, RestConfiguration.class })
@Transactional
@ActiveProfiles("store")
@ExtendWith(SpringExtension.class)
public class StoreRestEndpointsIT {

	@Autowired
	TestStore store;

	@Autowired
	private WebApplicationContext context;

	private MockMvc mvc;

	private String path;
	private String request;

	private final LastModifiedDate lastModifiedDate = new LastModifiedDate() {};

	
    @Nested
    class StoreRestEndpoints {
        @Nested
        class GivenARootResourcePath {
            @Nested
            class GivenAGETRequestToThatPath {
                @BeforeEach
                void setUp() throws Throwable {
                    mvc = MockMvcBuilders.webAppContextSetup(context).build();

                    path = "/" + UUID.randomUUID() + ".txt";
                    request = "/teststore" + path;

                }

                @Test
                void shouldReturn404() throws Throwable {
                    mvc.perform(get(request)).andExpect(status().isNotFound());

                }

            }

            @Nested
            class GivenAPOSTToThatPathWithContent {
                @BeforeEach
                void setUp() throws Throwable {
                    mvc = MockMvcBuilders.webAppContextSetup(context).build();

                    path = "/" + UUID.randomUUID() + ".txt";
                    request = "/teststore" + path;

                }

                @Test
                void shouldSetTheContentAndReturn201() throws Throwable {
                    String content = "New multi-part content";

                    mvc.perform(multipart(request).file(new MockMultipartFile("file",
                    		"test-file.txt", "text/plain", content.getBytes())))
                    		.andExpect(status().isCreated());

                    Resource r = store.getResource(path);
                    assertThat(IOUtils.contentEquals(
                    		new ByteArrayInputStream("New multi-part content".getBytes()),
                    		r.getInputStream())).isTrue();
                    assertThat(r.contentLength()).isEqualTo(Long.valueOf(content.length()));

                }

            }

            @Nested
            class GivenADELETERequestToThatPath {
                @BeforeEach
                void setUp() throws Throwable {
                    mvc = MockMvcBuilders.webAppContextSetup(context).build();

                    path = "/" + UUID.randomUUID() + ".txt";
                    request = "/teststore" + path;

                }

                @Test
                void shouldReturnA404() throws Throwable {
                    mvc.perform(delete(request)).andExpect(status().isNotFound());

                }

            }

        }

        @Nested
        class GivenARootResource {
            @BeforeEach
            void setUp() throws Throwable {
                mvc = MockMvcBuilders.webAppContextSetup(context).build();

                path = "/" + UUID.randomUUID() + ".txt";
                request = "/teststore" + path;
                Resource r = store.getResource(path);
                if (r instanceof WritableResource) {
                	IOUtils.copy(
                			new ByteArrayInputStream("Existing content".getBytes()),
                			((WritableResource) r).getOutputStream());
                }

                lastModifiedDate.setMvc(mvc);
                lastModifiedDate.setUrl("/teststore" + path);
                lastModifiedDate.setLastModifiedDate(new Date(r.lastModified()));
                lastModifiedDate.setContent("Existing content");
            }

            @Test
            void shouldReturnTheResourceSContent() throws Throwable {
                MockHttpServletResponse response = mvc.perform(get(request))
                		.andExpect(status().isOk()).andReturn().getResponse();

                assertThat(response).isNotNull();
                assertThat(response.getContentAsString()).isEqualTo("Existing content");

            }

            @Test
            void shouldReturnAByteRangeWhenRequested() throws Throwable {
                MockHttpServletResponse response = mvc
                		.perform(get(request).header("range", "bytes=9-12"))
                		.andExpect(status().isPartialContent()).andReturn()
                		.getResponse();

                assertThat(response).isNotNull();
                assertThat(response.getContentAsString()).isEqualTo("cont");

            }

            @Test
            void shouldOverwriteTheResourceSContent() throws Throwable {
                mvc.perform(put(request).content("New Existing content")
                		.contentType("text/plain")).andExpect(status().isOk());

                Resource r = store.getResource(path);
                assertThat(IOUtils.contentEquals(
                		new ByteArrayInputStream("New Existing content".getBytes()),
                		r.getInputStream())).isTrue();

            }

            @Test
            void shouldDeleteTheResource() throws Throwable {
                mvc.perform(delete(request)).andExpect(status().isNoContent());

                Resource r = store.getResource(path);
                assertThat(r.exists()).isFalse();

            }

            @Nested
            class APOSTToStorePathWithMultiPartFormData {
                @BeforeEach
                void setUp() throws Throwable {
                    mvc = MockMvcBuilders.webAppContextSetup(context).build();

                    path = "/" + UUID.randomUUID() + ".txt";
                    request = "/teststore" + path;
                    Resource r = store.getResource(path);
                    if (r instanceof WritableResource) {
                    	IOUtils.copy(
                    			new ByteArrayInputStream("Existing content".getBytes()),
                    			((WritableResource) r).getOutputStream());
                    }

                    lastModifiedDate.setMvc(mvc);
                    lastModifiedDate.setUrl("/teststore" + path);
                    lastModifiedDate.setLastModifiedDate(new Date(r.lastModified()));
                    lastModifiedDate.setContent("Existing content");
                }

                @Test
                void shouldOverwriteTheContentAndReturn200() throws Throwable {
                    String content = "New multi-part content";

                    mvc.perform(multipart(request).file(new MockMultipartFile("file",
                    		"tests-file.txt", "text/plain", content.getBytes())))
                    		.andExpect(status().isOk());

                    Resource r = store.getResource(path);
                    assertThat(IOUtils.contentEquals(
                    		new ByteArrayInputStream(
                    				"New multi-part content".getBytes()),
                    		r.getInputStream())).isTrue();
                    assertThat(r.contentLength()).isEqualTo(Long.valueOf(content.length()));

                }

            }

        }

        @Nested
        class GivenANestedResource {
            @BeforeEach
            void setUp() throws Throwable {
                mvc = MockMvcBuilders.webAppContextSetup(context).build();

                path = "/a/b/" + UUID.randomUUID() + ".txt";
                request = "/teststore" + path;
                Resource r = store.getResource(path);
                if (r instanceof WritableResource) {
                	IOUtils.copy(
                			new ByteArrayInputStream("Existing content".getBytes()),
                			((WritableResource) r).getOutputStream());
                }

            }

            @Test
            void shouldReturnTheResourceSContent() throws Throwable {
                MockHttpServletResponse response = mvc.perform(get(request))
                		.andExpect(status().isOk()).andReturn().getResponse();

                assertThat(response).isNotNull();
                assertThat(response.getContentAsString()).isEqualTo("Existing content");

            }

            @Test
            void shouldReturnAByteRangeWhenRequested() throws Throwable {
                MockHttpServletResponse response = mvc
                		.perform(get(request).header("range", "bytes=9-12"))
                		.andExpect(status().isPartialContent()).andReturn()
                		.getResponse();

                assertThat(response).isNotNull();
                assertThat(response.getContentAsString()).isEqualTo("cont");

            }

            @Test
            void shouldOverwriteTheResourceSContent() throws Throwable {
                mvc.perform(put(request).content("New Existing content")
                		.contentType("text/plain")).andExpect(status().isOk());

                Resource r = store.getResource(path);
                assertThat(IOUtils.contentEquals(
                		new ByteArrayInputStream("New Existing content".getBytes()),
                		r.getInputStream())).isTrue();

            }

            @Test
            void shouldDeleteTheResource() throws Throwable {
                mvc.perform(delete(request)).andExpect(status().isNoContent());

                Resource r = store.getResource(path);
                assertThat(r.exists()).isFalse();

            }

            @Nested
            class GivenATypicalBrowserRequest {
                @BeforeEach
                void setUp() throws Throwable {
                    mvc = MockMvcBuilders.webAppContextSetup(context).build();

                    path = "/a/b/" + UUID.randomUUID() + ".txt";
                    request = "/teststore" + path;
                    Resource r = store.getResource(path);
                    if (r instanceof WritableResource) {
                    	IOUtils.copy(
                    			new ByteArrayInputStream("Existing content".getBytes()),
                    			((WritableResource) r).getOutputStream());
                    }

                }

                @Test
                void shouldReturnTheResourceSContent() throws Throwable {
                    MockHttpServletResponse response = mvc
                    		.perform(get(request).accept(new String[] { "text/html",
                    				"application/xhtml+xml", "application/xml;q=0.9",
                    				"image/webp", "image/apng", "*/*;q=0.8" }))
                    		.andExpect(status().isOk()).andReturn().getResponse();

                    assertThat(response).isNotNull();
                    assertThat(response.getContentAsString()).isEqualTo("Existing content");

                }

            }

            @Nested
            class APOSTToStorePathWithMultiPartFormData {
                @BeforeEach
                void setUp() throws Throwable {
                    mvc = MockMvcBuilders.webAppContextSetup(context).build();

                    path = "/a/b/" + UUID.randomUUID() + ".txt";
                    request = "/teststore" + path;
                    Resource r = store.getResource(path);
                    if (r instanceof WritableResource) {
                    	IOUtils.copy(
                    			new ByteArrayInputStream("Existing content".getBytes()),
                    			((WritableResource) r).getOutputStream());
                    }

                }

                @Test
                void shouldOverwriteTheContentAndReturn200() throws Throwable {
                    String content = "New multi-part content";

                    mvc.perform(multipart(request).file(new MockMultipartFile("file",
                    		"tests-file.txt", "text/plain", content.getBytes())))
                    		.andExpect(status().isOk());

                    Resource r = store.getResource(path);
                    assertThat(IOUtils.contentEquals(
                    		new ByteArrayInputStream(
                    				"New multi-part content".getBytes()),
                    		r.getInputStream())).isTrue();
                    assertThat(r.contentLength()).isEqualTo(Long.valueOf(content.length()));

                }

            }

        }

    }

}
