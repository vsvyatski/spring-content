package it.internal.org.springframework.content.rest.controllers;

import org.junit.jupiter.api.extension.ExtendWith;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.DisplayName;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static java.lang.String.format;

import java.io.ByteArrayInputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.rest.config.HypermediaConfiguration;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;

import internal.org.springframework.content.rest.support.BaseUriConfig;
import internal.org.springframework.content.rest.support.EntityConfig;
import internal.org.springframework.content.rest.support.TestEntity;
import internal.org.springframework.content.rest.support.TestEntity3;
import internal.org.springframework.content.rest.support.TestEntity3ContentRepository;
import internal.org.springframework.content.rest.support.TestEntity3Repository;
import internal.org.springframework.content.rest.support.TestEntity4;
import internal.org.springframework.content.rest.support.TestEntity4ContentRepository;
import internal.org.springframework.content.rest.support.TestEntity4Repository;
import internal.org.springframework.content.rest.support.TestEntityContentRepository;
import internal.org.springframework.content.rest.support.TestEntityRepository;
import internal.org.springframework.content.rest.support.TestStore;

@WebAppConfiguration
@ContextConfiguration(classes = {
		BaseUriConfig.class,
		EntityConfig.class,
		DelegatingWebMvcConfiguration.class,
		RepositoryRestMvcConfiguration.class,
		RestConfiguration.class,
		HypermediaConfiguration.class })
@Transactional
@ActiveProfiles("store")
@ExtendWith(SpringExtension.class)
public class BaseUriIT {

	// different exported URI
	@Autowired
	private TestEntityRepository repository;
	@Autowired
	private TestEntityContentRepository contentRepository;

	// same exported URI
	@Autowired
	private TestEntity3Repository repo3;
	@Autowired
	private TestEntity3ContentRepository store3;

	// same exported URI
	@Autowired
	private TestEntity4Repository repo4;
	@Autowired
	private TestEntity4ContentRepository store4;

	@Autowired
	private TestStore store;

	@Autowired
	private WebApplicationContext context;

	private MockMvc mvc;

	private TestEntity testEntity;
	private TestEntity3 testEntity3;
	private TestEntity4 testEntity4;

	private Version version;
	private LastModifiedDate lastModifiedDate;

	private Entity entityTests;
	private Content contentTests;
	private Cors corsTests;

    @Nested
    class BaseUriContentTests {
        @Nested
        class GivenAnEntityIsTheSubjectOfARepositoryAndStorage {
            @Nested
            @DisplayName("given the repository and storage are exported to the same URI")
            class GivenTheRepositoryAndStorageAreExportedToTheSameURIEntity extends Entity {
                @BeforeEach
                void setUp() {
                    mvc = MockMvcBuilders.webAppContextSetup(context).build();
                    testEntity3 = repo3.save(new TestEntity3());
                    testEntity3.name = "tests";
                    testEntity3 = repo3.save(testEntity3);
                    this.setMvc(mvc);
                    this.setUrl("/api/testEntity3s/" + testEntity3.id);
                    this.setEntity(testEntity3);
                    this.setRepository(repo3);
                    this.setLinkRel("testEntity3");
                }
            }

            @Nested
            @DisplayName("given the repository and storage are exported to the same URI")
            class GivenTheRepositoryAndStorageAreExportedToTheSameURIContent extends Content {
                @BeforeEach
                void setUp() {
                    mvc = MockMvcBuilders.webAppContextSetup(context).build();
                    testEntity3 = repo3.save(new TestEntity3());
                    testEntity3.name = "tests";
                    testEntity3 = repo3.save(testEntity3);
                    this.setMvc(mvc);
                    this.setUrl("/contentApi/testEntity3s/" + testEntity3.getId());
                    this.setEntity(testEntity3);
                    this.setRepository(repo3);
                    this.setStore(store3);
                }
            }

            @Nested
            @DisplayName("given the repository and storage are exported to different URIs")
            class GivenTheRepositoryAndStorageAreExportedToDifferentURIsContent extends Content {
                @BeforeEach
                void setUp() {
                    mvc = MockMvcBuilders.webAppContextSetup(context).build();
                    testEntity = repository.save(new TestEntity());
                    this.setMvc(mvc);
                    this.setUrl("/contentApi/testEntitiesContent/" + testEntity.getId());
                    this.setEntity(testEntity);
                    this.setRepository(repository);
                    this.setStore(contentRepository);
                }
            }

            @Nested
            @DisplayName("given the repository and storage are exported to different URIs")
            class GivenTheRepositoryAndStorageAreExportedToDifferentURIsCors extends Cors {
                @BeforeEach
                void setUp() {
                    mvc = MockMvcBuilders.webAppContextSetup(context).build();
                    testEntity = repository.save(new TestEntity());
                    this.setMvc(mvc);
                    this.setUrl("/contentApi/testEntitiesContent/" + testEntity.getId());
                }
            }

            @Nested
            @DisplayName("given an entity with @Version")
            class GivenAnEntityWithVersionVersion extends Version {
                @BeforeEach
                void setUp() {
                    mvc = MockMvcBuilders.webAppContextSetup(context).build();
                    testEntity4 = new TestEntity4();
                    testEntity4 = store4.setContent(testEntity4, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                    testEntity4.mimeType = "text/plain";
                    testEntity4 = repo4.save(testEntity4);
                    String url = "/contentApi/testEntity4s/" + testEntity4.getId();
                    this.setEntity(testEntity4);
                    this.setMvc(mvc);
                    this.setUrl(url);
                    this.setCollectionUrl("/api/testEntity4s");
                    this.setContentLinkRel("content");
                    this.setRepo(repo4);
                    this.setStore(store4);
                    this.setEtag(format("\"%s\"", testEntity4.getVersion()));
                }
            }

            @Nested
            @DisplayName("given an entity with @LastModifiedDate")
            class GivenAnEntityWithLastModifiedDateLastModifiedDate extends LastModifiedDate {
                @BeforeEach
                void setUp() {
                    mvc = MockMvcBuilders.webAppContextSetup(context).build();
                    String content = "Hello Spring Content LastModifiedDate World!";
                    testEntity4 = new TestEntity4();
                    testEntity4 = store4.setContent(testEntity4, new ByteArrayInputStream(content.getBytes()));
                    testEntity4.mimeType = "text/plain";
                    testEntity4 = repo4.save(testEntity4);
                    String url = "/contentApi/testEntity4s/" + testEntity4.getId();
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
