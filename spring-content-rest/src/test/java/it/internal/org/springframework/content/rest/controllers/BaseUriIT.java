package it.internal.org.springframework.content.rest.controllers;

import internal.org.springframework.content.rest.support.*;
import it.internal.org.springframework.content.rest.controllers.support.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.rest.config.HypermediaConfiguration;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.content.test.support.TestSupportData;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;

import java.io.ByteArrayInputStream;

import static java.lang.String.format;

@WebAppConfiguration
@ContextConfiguration(classes = {
    BaseUriConfig.class, EntityConfig.class, DelegatingWebMvcConfiguration.class,
    RepositoryRestMvcConfiguration.class, RestConfiguration.class, HypermediaConfiguration.class
})
@Transactional
@ActiveProfiles("store")
@ExtendWith(SpringExtension.class)
public class BaseUriIT {

    // different exported URI
    @Autowired
    private UuidSingleContentEntityRepository repository;
    @Autowired
    private UuidSingleContentEntityStore contentStore;

    // same exported URI
    @Autowired
    private JsonMappedContentEntityRepository repo3;
    @Autowired
    private JsonMappedContentEntityStore store3;

    // same exported URI
    @Autowired
    private OneToOneLinkedContentEntityRepository repo4;
    @Autowired
    private OneToOneLinkedContentEntityStore store4;

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    private UuidSingleContentEntity testEntity;
    private JsonMappedContentEntity testEntity3;
    private OneToOneLinkedContentEntity testEntity4;

    @Nested
   class BaseUriContentTests {
        @Nested
        @DisplayName("Given an entity is the subject of a repository and storage")
        class EntityIsTheSubjectOfARepositoryAndStorage {
            @Nested
            @DisplayName("Given the repository and storage are exported to the same uri")
            class RepositoryAndStorageAreExportedToTheSameURIEntity extends EntityTests {
                @BeforeEach
                void setUp() {
                    mvc = MockMvcBuilders.webAppContextSetup(context).build();
                    testEntity3 = repo3.save(new JsonMappedContentEntity());
                    testEntity3.name = "tests";
                    testEntity3 = repo3.save(testEntity3);
                    this.setMvc(mvc);
                    this.setUrl("/api/jsonMappedContentEntities/" + testEntity3.id);
                    this.setEntity(testEntity3);
                    this.setRepository(repo3);
                    this.setLinkRel("testEntity3");
                }
            }

            @Nested

            @DisplayName("Given the repository and storage are exported to the same uri")

            class RepositoryAndStorageAreExportedToTheSameURIContent extends ContentTests {
                @BeforeEach
                void setUp() {
                    mvc = MockMvcBuilders.webAppContextSetup(context).build();
                    testEntity3 = repo3.save(new JsonMappedContentEntity());
                    testEntity3.name = "tests";
                    testEntity3 = repo3.save(testEntity3);
                    this.setMvc(mvc);
                    this.setUrl("/contentApi/jsonMappedContentEntities/" + testEntity3.getId());
                    this.setEntity(testEntity3);
                    this.setRepository(repo3);
                    this.setStore(store3);
                }
            }

            @Nested

            @DisplayName("Given the repository and storage are exported to different uris")

            class RepositoryAndStorageAreExportedToDifferentURIsContent extends ContentTests {
                @BeforeEach
                void setUp() {
                    mvc = MockMvcBuilders.webAppContextSetup(context).build();
                    testEntity = repository.save(new UuidSingleContentEntity());
                    this.setMvc(mvc);
                    this.setUrl("/contentApi/uuidSingleContentEntitiesContent/" + testEntity.getId());
                    this.setEntity(testEntity);
                    this.setRepository(repository);
                    this.setStore(contentStore);
                }
            }

            @Nested

            @DisplayName("Given the repository and storage are exported to different uris")

            class RepositoryAndStorageAreExportedToDifferentURIsCors extends CorsTests {
                @BeforeEach
                void setUp() {
                    mvc = MockMvcBuilders.webAppContextSetup(context).build();
                    testEntity = repository.save(new UuidSingleContentEntity());
                    this.setMvc(mvc);
                    this.setUrl("/contentApi/uuidSingleContentEntitiesContent/" + testEntity.getId());
                }
            }

            @Nested

            @DisplayName("Given an entity with @version")

            class EntityWithVersion extends VersionTests {
                @BeforeEach
                void setUp() {
                    mvc = MockMvcBuilders.webAppContextSetup(context).build();
                    testEntity4 = new OneToOneLinkedContentEntity();
                    testEntity4 = store4.setContent(testEntity4, TestSupportData.sampleTextStream());
                    testEntity4.mimeType = "text/plain";
                    testEntity4 = repo4.save(testEntity4);
                    String url = "/contentApi/oneToOneLinkedContentEntities/" + testEntity4.getId();
                    this.setEntity(testEntity4);
                    this.setMvc(mvc);
                    this.setUrl(url);
                    this.setCollectionUrl("/api/oneToOneLinkedContentEntities");
                    this.setContentLinkRel("content");
                    this.setRepo(repo4);
                    this.setStore(store4);
                    this.setEtag(format("\"%s\"", testEntity4.getVersion()));
                }
            }

            @Nested

            @DisplayName("Given an entity with @lastmodifieddate")

            class EntityWithLastModifiedDate extends LastModifiedDateTests {
                @BeforeEach
                void setUp() {
                    mvc = MockMvcBuilders.webAppContextSetup(context).build();
                    String content = TestSupportData.SAMPLE_LAST_MODIFIED_DATE_PAYLOAD;
                    testEntity4 = new OneToOneLinkedContentEntity();
                    testEntity4 = store4.setContent(testEntity4, new ByteArrayInputStream(content.getBytes()));
                    testEntity4.mimeType = "text/plain";
                    testEntity4 = repo4.save(testEntity4);
                    String url = "/contentApi/oneToOneLinkedContentEntities/" + testEntity4.getId();
                    this.setMvc(mvc);
                    this.setUrl(url);
                    this.setLastModifiedDate(testEntity4.getModifiedDate());
                    this.setEtag(testEntity4.getVersion().toString());
                    this.setContent(content);
                }
            }
        }
    }
}
