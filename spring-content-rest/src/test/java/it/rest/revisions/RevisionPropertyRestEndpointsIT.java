package it.rest.revisions;

import internal.org.springframework.content.rest.scoped.revision.RevisionPropertyEntity;
import internal.org.springframework.content.rest.scoped.revision.RevisionPropertyEntityRepository;
import internal.org.springframework.content.rest.scoped.revision.RevisionPropertyEntityStore;
import internal.org.springframework.content.rest.support.config.JpaInfrastructureConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.fs.config.EnableFileSystemStores;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.content.test.support.TestSupportData;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.envers.repository.support.EnversRevisionRepositoryFactoryBean;
import org.springframework.data.history.Revisions;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebAppConfiguration
@ContextConfiguration(classes = {RevisionPropertyRestEndpointsIT.TestConfig.class, DelegatingWebMvcConfiguration.class, RepositoryRestMvcConfiguration.class, RestConfiguration.class})
@ExtendWith(SpringExtension.class)
public class RevisionPropertyRestEndpointsIT {

    @Autowired
    private RevisionPropertyEntityRepository repository;

    @Autowired
    private RevisionPropertyEntityStore store;

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    private RevisionPropertyEntity testEntity;

    @Configuration
    @EnableJpaRepositories(
        basePackages = "internal.org.springframework.content.rest.scoped.revision",
        considerNestedRepositories = true,
        repositoryFactoryBeanClass = EnversRevisionRepositoryFactoryBean.class)
    @EnableFileSystemStores(basePackages = "internal.org.springframework.content.rest.scoped.revision")
    public static class TestConfig extends JpaInfrastructureConfig {

        @Override
        protected String[] packagesToScan() {
            return new String[]{"internal.org.springframework.content.rest.scoped.revision"};
        }

        @Bean
        FileSystemResourceLoader fileSystemResourceLoader() {
            return new FileSystemResourceLoader(filesystemRoot().getAbsolutePath());
        }

        @Bean
        public File filesystemRoot() {
            File baseDir = new File(System.getProperty("java.io.tmpdir"));
            File filesystemRoot = new File(baseDir, "spring-content-controller-revisions-tests");
            if (!filesystemRoot.mkdirs() && !filesystemRoot.isDirectory()) {
                throw new IllegalStateException("Failed to create " + filesystemRoot);
            }
            return filesystemRoot;
        }
    }

    @Nested
    class RevisionPropertyRESTEndpoints {
        @Nested
        class GivenAnEntityWithRevisions {
            @Nested
            class AGETToRepositoryIdRevisions1Content {
                @BeforeEach
                void setUp() {
                    mvc = MockMvcBuilders.webAppContextSetup(context).build();

                    testEntity = repository.save(new RevisionPropertyEntity());
                    testEntity = store.setContent(testEntity, TestSupportData.sampleTextStream());
                    testEntity.setMimeType("text/plain");
                    testEntity = repository.save(testEntity);

                    assertThat(repository.findRevisions(testEntity.getId()).toList().size()).isEqualTo(2);
                }

                @Test
                void shouldReturnA404() throws Exception {
                    mvc.perform(
                            get("/revisionPropertyEntities/" + testEntity.getId() + "/revisions/1/content").
                                accept("text/plain")).
                        andExpect(status().isNotFound());
                }
            }

            @Nested
            class AGETToRepositoryIdRevisionsLatestContent {
                @BeforeEach
                void setUp() {
                    mvc = MockMvcBuilders.webAppContextSetup(context).build();

                    testEntity = repository.save(new RevisionPropertyEntity());
                    testEntity = store.setContent(testEntity, TestSupportData.sampleTextStream());
                    testEntity.setMimeType("text/plain");
                    testEntity = repository.save(testEntity);

                    assertThat(repository.findRevisions(testEntity.getId()).toList().size()).isEqualTo(2);
                }

                @Test
                void shouldReturnTheContent() throws Exception {
                    Revisions<Integer, RevisionPropertyEntity> revisions = repository.findRevisions(testEntity.getId());
                    Integer revisionId = revisions.getLatestRevision().getRequiredRevisionNumber();

                    MockHttpServletResponse response =
                        mvc.perform(
                                get("/revisionPropertyEntities/" + testEntity.getId() + "/revisions/" + revisionId + "/content").
                                    accept("text/plain")).
                            andExpect(status().isOk()).
                            andReturn().getResponse();

                    assertThat(response).isNotNull();
                    assertThat(response.getContentAsString()).isEqualTo(TestSupportData.SAMPLE_TEXT_PAYLOAD);
                }
            }
        }
    }
}
