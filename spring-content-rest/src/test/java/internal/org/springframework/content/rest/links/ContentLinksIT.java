package internal.org.springframework.content.rest.links;

import org.junit.jupiter.api.extension.ExtendWith;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.DisplayName;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static java.lang.String.format;

import java.io.ByteArrayInputStream;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.property.PropertyPath;
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
import internal.org.springframework.content.rest.support.TestEntity;
import internal.org.springframework.content.rest.support.TestEntity10;
import internal.org.springframework.content.rest.support.TestEntity10Repository;
import internal.org.springframework.content.rest.support.TestEntity10Store;
import internal.org.springframework.content.rest.support.TestEntity2;
import internal.org.springframework.content.rest.support.TestEntity2Repository;
import internal.org.springframework.content.rest.support.TestEntity2Store;
import internal.org.springframework.content.rest.support.TestEntity3;
import internal.org.springframework.content.rest.support.TestEntity3ContentRepository;
import internal.org.springframework.content.rest.support.TestEntity3Repository;
import internal.org.springframework.content.rest.support.TestEntity5;
import internal.org.springframework.content.rest.support.TestEntity5Repository;
import internal.org.springframework.content.rest.support.TestEntity5Store;
import internal.org.springframework.content.rest.support.TestEntityContentRepository;
import internal.org.springframework.content.rest.support.TestEntityRepository;

@WebAppConfiguration
@ContextConfiguration(classes = {
		BaseUriConfig.class,
		DelegatingWebMvcConfiguration.class,
		RepositoryRestMvcConfiguration.class,
		RestConfiguration.class,
		HypermediaConfiguration.class })
@Transactional
@ActiveProfiles("store")
@ExtendWith(SpringExtension.class)
public class ContentLinksIT {

	@Autowired
	TestEntityRepository repository;
	@Autowired
	TestEntityContentRepository contentRepository;

    @Autowired
    TestEntity5Repository repository5;
    @Autowired
    TestEntity5Store store5;

    @Autowired
    TestEntity2Repository repository2;
    @Autowired
    TestEntity2Store store2;

	@Autowired
	TestEntity10Repository repository10;
	@Autowired
	TestEntity10Store store10;

    @Autowired
    TestEntity3Repository repository3;
    @Autowired
    TestEntity3ContentRepository contentRepository3;

	@Autowired
	private WebApplicationContext context;

	private MockMvc mvc;

	private TestEntity testEntity;
	private TestEntity3 testEntity3;
    private TestEntity5 testEntity5;
    private TestEntity2 testEntity2;
	private TestEntity10 testEntity10;

	private ContentLinkTests contentLinkTests;

    @Nested
    class NoLinkrel {
        @Nested
        @DisplayName("given a store and an entity with a top-level uncorrelated content property")
        class GivenAStoreAndAnEntityWithATopLevelUncorrelatedContentPropertyContentLinkTests extends ContentLinkTests {
            @BeforeEach
            void setUp() {
                mvc = MockMvcBuilders.webAppContextSetup(context).build();
                testEntity3 = new TestEntity3();
                contentRepository3.setContent(testEntity3, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                testEntity3 = repository3.save(testEntity3);
                this.setMvc(mvc);
                this.setRepository(repository3);
                this.setStore(contentRepository3);
                this.setTestEntity(testEntity3);
                this.setUrl("/api/testEntity3s/" + testEntity3.getId());
                this.setLinkRel("testEntity3");
                this.setExpectedLinkRegex("http://localhost/api/testEntity3s/" + testEntity3.getId());
            }
        }

        @Nested
        @DisplayName("given a store and an entity with top-level correlated content properties")
        class GivenAStoreAndAnEntityWithTopLevelCorrelatedContentPropertiesContentLinkTests extends ContentLinkTests {
            @BeforeEach
            void setUp() {
                mvc = MockMvcBuilders.webAppContextSetup(context).build();
                testEntity5 = new TestEntity5();
                store5.setContent(testEntity5, PropertyPath.from("content"), new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                testEntity5 = repository5.save(testEntity5);
                this.setMvc(mvc);
                this.setRepository(repository5);
                this.setStore(store5);
                this.setTestEntity(testEntity5);
                this.setUrl("/api/testEntity5s/" + testEntity5.getId());
                this.setLinkRel("content");
                this.setExpectedLinkRegex(format("http://localhost/contentApi/testEntity5s/%s/content", testEntity5.getId()));
            }
        }

        @Nested
        @DisplayName("given a store specifying a linkrel and an entity a nested content property")
        class GivenAStoreSpecifyingALinkrelAndAnEntityANestedContentPropertyContentLinkTests extends ContentLinkTests {
            @BeforeEach
            void setUp() {
                mvc = MockMvcBuilders.webAppContextSetup(context).build();
                testEntity2 = new TestEntity2();
                testEntity2.getChild().setMimeType("text/plain");
                store2.setContent(testEntity2, PropertyPath.from("child"), new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                testEntity2 = repository2.save(testEntity2);
                this.setMvc(mvc);
                this.setRepository(repository2);
                this.setStore(store2);
                this.setTestEntity(testEntity2);
                this.setUrl("/api/files/" + testEntity2.getId());
                this.setLinkRel("child");
                this.setExpectedLinkRegex(format("http://localhost/contentApi/files/%s/child", testEntity2.getId()));
            }
        }

        @Nested
        @DisplayName("given a store specifying a linkrel and an entity with nested content properties")
        class GivenAStoreSpecifyingALinkrelAndAnEntityWithNestedContentPropertiesContentLinkTe extends ContentLinkTests {
            @BeforeEach
            void setUp() {
                mvc = MockMvcBuilders.webAppContextSetup(context).build();
                testEntity10 = new TestEntity10();
                store10.setContent(testEntity10, PropertyPath.from("child/content"), new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                testEntity10.getChild().setContentMimeType("text/plain");
                testEntity10.getChild().setContentFileName("test");
                store10.setContent(testEntity10, PropertyPath.from("child/preview"), new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                testEntity10.getChild().setPreviewMimeType("text/plain");
                testEntity10 = repository10.save(testEntity10);
                this.setMvc(mvc);
                this.setRepository(repository10);
                this.setStore(store10);
                this.setTestEntity(testEntity10);
                this.setUrl("/api/testEntity10s/" + testEntity10.getId());
                this.setLinkRel("child/content");
                this.setExpectedLinkRegex(format("http://localhost/contentApi/testEntity10s/%s/child/content", testEntity10.getId()));
            }
        }

    }

}
