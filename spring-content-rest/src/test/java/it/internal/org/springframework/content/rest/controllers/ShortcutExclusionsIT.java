package it.internal.org.springframework.content.rest.controllers;

import org.junit.jupiter.api.extension.ExtendWith;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.io.StringReader;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.fs.config.EnableFileSystemStores;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.content.rest.config.ContentRestConfigurer;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;

import com.theoryinpractise.halbuilder.api.ReadableRepresentation;
import com.theoryinpractise.halbuilder.api.RepresentationFactory;
import com.theoryinpractise.halbuilder.standard.StandardRepresentationFactory;

import internal.org.springframework.content.rest.support.TestEntity3;
import internal.org.springframework.content.rest.support.TestEntity3Repository;
import internal.org.springframework.content.rest.support.config.JpaInfrastructureConfig;

@WebAppConfiguration
@ContextConfiguration(classes = {
		ShortcutExclusionsIT.Config.class,
		DelegatingWebMvcConfiguration.class,
		RepositoryRestMvcConfiguration.class,
		RestConfiguration.class })
@Transactional
@ExtendWith(SpringExtension.class)
public class ShortcutExclusionsIT {

	@Autowired
	private TestEntity3Repository repo;

	@Autowired
	private WebApplicationContext context;

	private MockMvc mvc;

	private TestEntity3 testEntity;

	
    @Nested
    class ExcludedShortcutLinkTests {
        @Nested
        class WhenAllContentPropertyShortcutGETsAreExcludedAndANonJsonRequestIsMade {
            @BeforeEach
            void setUp() {
                mvc = MockMvcBuilders.webAppContextSetup(context).build();

                testEntity = repo.save(new TestEntity3());
                            testEntity = repo.save(testEntity);
            }

            @Test
            void shouldReturnTheEntity() throws Exception {
                MockHttpServletResponse response = mvc
                        .perform(get("/testEntity3s/" + testEntity.getId())
                                .accept("*/*"))
                        .andExpect(status().isOk())
                        .andReturn().getResponse();

                RepresentationFactory representationFactory = new StandardRepresentationFactory();
                ReadableRepresentation halResponse = representationFactory
                        .readRepresentation("application/hal+json",
                                new StringReader(response.getContentAsString()));
                assertThat(halResponse.getLinks().size()).isGreaterThan(2);
                assertThat(halResponse.getLinksByRel("testEntity3")).isNotNull();
            }

        }

    }


	@Configuration
	@EnableJpaRepositories(basePackages = "internal.org.springframework.content.rest.support")
	@EnableTransactionManagement
	@EnableFileSystemStores(basePackages = "internal.org.springframework.content.rest.support")
	public static class Config extends JpaInfrastructureConfig {

	    @Bean
	    public ContentRestConfigurer contentRestConfigurer() {

	        return new ContentRestConfigurer() {

	            @Override
	            public void configure(RestConfiguration config) {
	                config.shortcutExclusions().exclude("GET", MediaType.parseMediaType("*/*"));
	            }
	        };
	    }

	    @Bean
	    FileSystemResourceLoader fileSystemResourceLoader() {
	        return new FileSystemResourceLoader(filesystemRoot().getAbsolutePath());
	    }

	    @Bean
	    public File filesystemRoot() {
	        File baseDir = new File(System.getProperty("java.io.tmpdir"));
	        File filesystemRoot = new File(baseDir, "spring-content-excluded-shortcut-links");
	        filesystemRoot.mkdirs();
	        return filesystemRoot;
	    }
	}
}
