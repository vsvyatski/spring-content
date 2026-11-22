package it.internal.org.springframework.content.rest.controllers;

import internal.org.springframework.content.rest.support.JsonMappedContentEntity;
import internal.org.springframework.content.rest.support.JsonMappedContentEntityRepository;
import internal.org.springframework.content.rest.support.JsonMappedContentEntityStore;
import internal.org.springframework.content.rest.support.TestRenditionConfig;
import internal.org.springframework.content.rest.support.config.JpaInfrastructureConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.fs.config.EnableFileSystemStores;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;

import java.io.File;

@WebAppConfiguration
@ContextConfiguration(classes = {
    ContextPathIT.ContextPathConfig.class,
    DelegatingWebMvcConfiguration.class,
    RepositoryRestMvcConfiguration.class,
    RestConfiguration.class})
@Transactional
@ActiveProfiles("store")
@ExtendWith(SpringExtension.class)
public class ContextPathIT {

    @Autowired
    private JsonMappedContentEntityRepository repo3;

    @Autowired
    private JsonMappedContentEntityStore store3;

    @Autowired
    private WebApplicationContext context;

    @Configuration
    @Import(TestRenditionConfig.class)
    @EnableJpaRepositories(basePackages = "internal.org.springframework.content.rest.support")
    @EnableTransactionManagement
    @EnableFileSystemStores(basePackages = "internal.org.springframework.content.rest.support")
    @Profile("store")
    public static class ContextPathConfig extends JpaInfrastructureConfig {

        @Bean
        FileSystemResourceLoader fileSystemResourceLoader() {
            return new FileSystemResourceLoader(filesystemRoot().getAbsolutePath());
        }

        @Bean
        public File filesystemRoot() {
            File baseDir = new File(System.getProperty("java.io.tmpdir"));
            File filesystemRoot = new File(baseDir, "spring-content-controller-tests");
            if (!filesystemRoot.mkdirs() && !filesystemRoot.isDirectory()) {
                throw new IllegalStateException("Failed to create " + filesystemRoot);
            }
            return filesystemRoot;
        }
    }

    @Nested
    class ContextPathContentTests {
        @Nested
        class GivenAnEntityIsTheSubjectOfARepositoryAndStorage {
            @Nested
            @DisplayName("given the repository and storage are exported to the same URI")
            class GivenTheRepositoryAndStorageAreExportedToTheSameURIContent extends Content {
                @BeforeEach
                void setUp() {
                    MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
                    JsonMappedContentEntity testEntity3 = repo3.save(new JsonMappedContentEntity());
                    testEntity3.name = "tests";
                    testEntity3 = repo3.save(testEntity3);
                    this.setMvc(mvc);
                    this.setUrl("/contextPath/jsonMappedContentEntities/" + testEntity3.getId());
                    this.setEntity(testEntity3);
                    this.setRepository(repo3);
                    this.setStore(store3);
                    this.setContextPath("/contextPath");
                }
            }
        }
    }
}
