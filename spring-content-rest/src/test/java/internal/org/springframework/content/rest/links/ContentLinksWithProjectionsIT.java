package internal.org.springframework.content.rest.links;

import com.theoryinpractise.halbuilder.api.ReadableRepresentation;
import com.theoryinpractise.halbuilder.api.RepresentationFactory;
import com.theoryinpractise.halbuilder.standard.StandardRepresentationFactory;
import internal.org.springframework.content.rest.scoped.projection.ProjectedContentEntityRepository;
import internal.org.springframework.content.rest.scoped.projection.ProjectedContentEntityStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.fs.config.EnableFileSystemStores;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.content.rest.config.HypermediaConfiguration;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.content.test.support.EmbeddedJpa;
import org.springframework.content.test.support.TestSupportData;
import org.springframework.content.test.support.jpa.fixture.SingleContentJpaLongIdUuidNamedEntity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;

import javax.sql.DataSource;
import java.io.File;
import java.io.StringReader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebAppConfiguration
@ContextConfiguration(classes = {
    ContentLinksWithProjectionsIT.StoreConfig.class, DelegatingWebMvcConfiguration.class,
    RepositoryRestMvcConfiguration.class, RestConfiguration.class, HypermediaConfiguration.class
})
@Transactional
@ActiveProfiles("store")
@ExtendWith(SpringExtension.class)
public class ContentLinksWithProjectionsIT {

    @Autowired
    ProjectedContentEntityRepository repository;

    @Autowired
    ProjectedContentEntityStore store;

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    private SingleContentJpaLongIdUuidNamedEntity testEntity;

    @Projection(name = "customTEntity", types = {SingleContentJpaLongIdUuidNamedEntity.class})
    @SuppressWarnings("unused") // Spring Data REST projection
    public interface CustomTEntity {

        String getName();
    }

    @Configuration
    @EnableJpaRepositories(basePackages = "internal.org.springframework.content.rest.scoped.projection",
        considerNestedRepositories = true)
    @EnableTransactionManagement
    @EnableFileSystemStores(basePackages = "internal.org.springframework.content.rest.scoped.projection")
//    @Profile("store")
    public static class StoreConfig /*extends JpaInfrastructureConfig*/ {

        @Bean
        public DataSource dataSource() {
            return EmbeddedJpa.embeddedDataSource(EmbeddedDatabaseType.H2);
        }

        @Bean
        public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
            LocalContainerEntityManagerFactoryBean factory =
                EmbeddedJpa.entityManagerFactory(dataSource(), Database.H2, packagesToScan());
            factory.setPersistenceUnitName("content-links-with-projections");
            factory.afterPropertiesSet();
            return factory;
        }

        protected String[] packagesToScan() {
            return new String[]{"org.springframework.content.test.support.jpa.fixture"};
        }

        @Bean
        public PlatformTransactionManager transactionManager() {
            return new JpaTransactionManager();
        }

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
    class ContentLinksWithEntityProjectionCases {
        @Nested
        class GivenContentIsAssociated {
            @Nested
            class AGETToApiRepositoryIdProjectionId {
                @BeforeEach
                void setUp() {
                    mvc = MockMvcBuilders.webAppContextSetup(context).build();

                    testEntity = new SingleContentJpaLongIdUuidNamedEntity();
                    testEntity.setName("foo");
                    testEntity = store.setContent(testEntity, TestSupportData.sampleTextStream());
                    testEntity = repository.save(testEntity);
                }

                @Test
                void shouldProvideAResponseWithAContentLink() throws Exception {
                    MockHttpServletResponse response = mvc.perform(get("/projectedContentEntities/" + testEntity.getId() + "?projection=customTEntity")
                            .accept("application/hal+json"))
                        .andExpect(status().isOk()).andReturn().getResponse();
                    assertThat(response).isNotNull();

                    RepresentationFactory representationFactory = new StandardRepresentationFactory();
                    ReadableRepresentation halResponse = representationFactory
                        .readRepresentation("application/hal+json", new StringReader(response.getContentAsString()));

                    assertThat(halResponse).isNotNull();
                    assertThat(halResponse.getLinksByRel("content")).isNotNull();
                    assertThat(halResponse.getLinksByRel("content")).extracting("href").contains("http://localhost/projectedContentEntities/" + testEntity.getId() + "/content");
                }
            }
        }
    }
}
