package it.rest.revisions;

import org.junit.jupiter.api.extension.ExtendWith;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.io.File;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import org.hibernate.envers.Audited;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.fs.config.EnableFileSystemStores;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.content.fs.store.FileSystemContentStore;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.envers.repository.support.EnversRevisionRepositoryFactoryBean;
import org.springframework.data.history.Revisions;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;

import internal.org.springframework.content.rest.support.config.JpaInfrastructureConfig;

@WebAppConfiguration
@ContextConfiguration(classes = { RevisionPropertyRestEndpointsIT.TestConfig.class, DelegatingWebMvcConfiguration.class, RepositoryRestMvcConfiguration.class, RestConfiguration.class })
@ExtendWith(SpringExtension.class)
public class RevisionPropertyRestEndpointsIT {

    @Autowired
    private RevisionPropertyRestEndpointsIT.TEntityRepository repository;

    @Autowired
    private RevisionPropertyRestEndpointsIT.TEntityContentStore store;

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    private TEntity testEntity;

    
    @Nested
    class RevisionPropertyRESTEndpoints {
        @Nested
        class GivenAnEntityWithRevisions {
            @Nested
            class AGETToRepositoryIdRevisions1Content {
                @BeforeEach
                void setUp() {
                    mvc = MockMvcBuilders.webAppContextSetup(context).build();

                    testEntity = repository.save(new TEntity());
                    testEntity = store.setContent(testEntity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                    testEntity.setMimeType("text/plain");
                    testEntity = repository.save(testEntity);

                    assertThat(repository.findRevisions(testEntity.getId()).toList().size()).isEqualTo(2);

                }

                @Test
                void shouldReturnA404() throws Exception {
                    mvc.perform(
                        get("/tEntities/" + testEntity.getId() + "/revisions/1/content").
                            accept("text/plain")).
                        andExpect(status().isNotFound());

                }

            }

            @Nested
            class AGETToRepositoryIdRevisionsLatestContent {
                @BeforeEach
                void setUp() {
                    mvc = MockMvcBuilders.webAppContextSetup(context).build();

                    testEntity = repository.save(new TEntity());
                    testEntity = store.setContent(testEntity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                    testEntity.setMimeType("text/plain");
                    testEntity = repository.save(testEntity);

                    assertThat(repository.findRevisions(testEntity.getId()).toList().size()).isEqualTo(2);

                }

                @Test
                void shouldReturnTheContent() throws Exception {
                    Revisions<Integer, TEntity> revisions = repository.findRevisions(testEntity.getId());
                    Integer revisionId = revisions.getLatestRevision().getRequiredRevisionNumber();

                    MockHttpServletResponse response =
                        mvc.perform(
                            get("/tEntities/" + testEntity.getId() + "/revisions/" + revisionId + "/content").
                                accept("text/plain")).
                            andExpect(status().isOk()).
                            andReturn().getResponse();

                    assertThat(response).isNotNull();
                    assertThat(response.getContentAsString()).isEqualTo("Hello Spring Content World!");

                }

            }

        }

    }


    @Configuration
    @EnableJpaRepositories(basePackages = "it.rest.revisions", considerNestedRepositories = true, repositoryFactoryBeanClass = EnversRevisionRepositoryFactoryBean.class)
    @EnableFileSystemStores(basePackages = "it.rest.revisions")
    public static class TestConfig extends JpaInfrastructureConfig {

        @Override
        protected String[] packagesToScan() {
            return new String[] { "it.rest.revisions" };
        }

        @Bean
        FileSystemResourceLoader fileSystemResourceLoader() {
            return new FileSystemResourceLoader(filesystemRoot().getAbsolutePath());
        }

        @Bean
        public File filesystemRoot() {
            File baseDir = new File(System.getProperty("java.io.tmpdir"));
            File filesystemRoot = new File(baseDir, "spring-content-controller-revisions-tests");
            filesystemRoot.mkdirs();
            return filesystemRoot;
        }
    }

    public interface TEntityRepository extends JpaRepository<TEntity, Long>, RevisionRepository<TEntity, Long, Integer> {
    }

    public interface TEntityContentStore extends FileSystemContentStore<TEntity, String> {
    }

    @Entity
    @Audited
    public static class TEntity {

        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        private Long id;

        @ContentId
        private String contentId;

        @ContentLength
        private Long contentLen;

        @MimeType
        private String mimeType;

        public TEntity() {
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getContentId() {
            return contentId;
        }

        public void setContentId(String contentId) {
            this.contentId = contentId;
        }

        public Long getContentLen() {
            return contentLen;
        }

        public void setContentLen(Long contentLen) {
            this.contentLen = contentLen;
        }

        public String getMimeType() {
            return mimeType;
        }

        public void setMimeType(String mimeType) {
            this.mimeType = mimeType;
        }
    }
}
