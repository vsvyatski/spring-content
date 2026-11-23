package it.rest.extensions.content.search;

import com.theoryinpractise.halbuilder.api.ReadableRepresentation;
import com.theoryinpractise.halbuilder.api.RepresentationFactory;
import com.theoryinpractise.halbuilder.standard.StandardRepresentationFactory;
import internal.org.springframework.content.rest.support.config.JpaInfrastructureConfig;
import internal.org.springframework.data.rest.extensions.contentsearch.DefaultEntityLookupStrategy;
import internal.org.springframework.data.rest.extensions.contentsearch.QueryMethodsEntityLookupStrategy;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.AdditionalAnswers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.fulltext.Attribute;
import org.springframework.content.commons.fulltext.Highlight;
import org.springframework.content.commons.search.Searchable;
import org.springframework.content.commons.utils.ReflectionService;
import org.springframework.content.fs.config.EnableFileSystemStores;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.content.fs.store.FileSystemContentStore;
import org.springframework.content.rest.FulltextEntityLookupQuery;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.extensions.contentsearch.ContentSearchRestController;
import org.springframework.data.rest.extensions.contentsearch.ContentSearchRestController.InternalResult;
import org.springframework.data.rest.webmvc.RootResourceInformation;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;

import java.io.File;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// because the controller bean is shared, and we need to instruct the reflection service
// to behave differently in each tests
@WebAppConfiguration
@ContextConfiguration(classes = {
    ContentSearchRestControllerIT.TestConfig.class,
    DelegatingWebMvcConfiguration.class, RepositoryRestMvcConfiguration.class,
    RestConfiguration.class})
@Transactional
@ExtendWith(SpringExtension.class)
public class ContentSearchRestControllerIT {

    // mocks/spys
    private static ReflectionService reflectionService;
    private static DefaultEntityLookupStrategy defaultLookupStrategy;
    private static QueryMethodsEntityLookupStrategy queryMethodsLookupStrategy;
    private final RepresentationFactory representationFactory = new StandardRepresentationFactory();
    @Autowired
    private TestEntityWithSharedIdsRepository repository;
    @Autowired
    private TestEntityWithSeparateIdsRepository entityWithSeparateRepository;
    @Autowired
    private RepositoryWithNoLookupStrategy repoWithNoLookupStrategy;
    @Autowired
    private WebApplicationContext context;
    private MockMvc mvc;
    private TestEntityWithSharedId entity2;
    private List<String> sharedIds;
    private TestEntityWithSeparateId entity3;
    private List<String> contentIds;
    private List<InternalResult> internalResults;

    @SuppressWarnings("unused") // classpath / store component scan
    public interface TestEntityNoContentCrudRepository extends CrudRepository<TestEntityNoContent, String> {
    }

    @SuppressWarnings("unused") // classpath / store component scan
    public interface TestEntityNotSearchableRepository extends CrudRepository<TestEntityNotSearchable, String> {
    }

    @SuppressWarnings("unused") // classpath / store component scan
    public interface TestEntityNotSearchableStore extends FileSystemContentStore<TestEntityNotSearchable, String> {
    }

    public interface TestEntityWithSharedIdsRepository extends CrudRepository<TestEntityWithSharedId, String> {
    }

    @SuppressWarnings("unused") // classpath / store component scan
    public interface TestEntityWithSharedIdsSearchableStore extends FileSystemContentStore<TestEntityWithSharedId, String>, Searchable<String> {
    }

    public interface TestEntityWithSeparateIdsRepository
        extends CrudRepository<TestEntityWithSeparateId, String> {

        @Query("select e from testEntitySeparateId e")
        @SuppressWarnings("unused")
        List<TestEntityWithSeparateId> randomQueryMethod();

        @FulltextEntityLookupQuery
        @SuppressWarnings("unused")
        List<TestEntityWithSeparateId> findAllByContentIdIn(@Param("contentIds") List<String> contentIds);
    }

    @SuppressWarnings("unused") // classpath / store component scan
    public interface TestEntityWithSeparateIdsSearchableStore
        extends FileSystemContentStore<TestEntityWithSeparateId, String>, Searchable<String> {
    }

    @RepositoryRestResource(path = "repoWithNoLookupStrategy")
    public interface RepositoryWithNoLookupStrategy
        extends CrudRepository<NoLookupStrategySearchEntity, String> {
    }

    @SuppressWarnings("unused") // classpath / store component scan
    public interface NoLookupStrategySearchEntitySearchableStore
        extends FileSystemContentStore<NoLookupStrategySearchEntity, String>, Searchable<String> {
    }

    @RepositoryRestResource(path = "repoWithCustomSearchReturnType")
    @SuppressWarnings("unused") // classpath / store component scan
    public interface RepositoryWithCustomSearchReturnType
        extends CrudRepository<CustomSearchReturnTypeEntity, String> {
    }

    @SuppressWarnings("unused") // classpath / store component scan
    public interface CustomSearchReturnTypeEntitySearchableStore
        extends FileSystemContentStore<CustomSearchReturnTypeEntity, String>, Searchable<CustomResult> {
    }

    @Configuration
    @EnableJpaRepositories(basePackages = "it.rest.extensions.content.search",
        considerNestedRepositories = true)
    @EnableTransactionManagement
    @EnableFileSystemStores(basePackages = "it.rest.extensions.content.search")
    public static class TestConfig extends JpaInfrastructureConfig {

        @Bean
        FileSystemResourceLoader fileSystemResourceLoader() {
            return new FileSystemResourceLoader(filesystemRoot().getAbsolutePath());
        }

        @Bean
        public File filesystemRoot() {
            File baseDir = new File(System.getProperty("java.io.tmpdir"));
            File filesystemRoot = new File(baseDir, "spring-content-search-controller-tests");
            if (!filesystemRoot.mkdirs() && !filesystemRoot.isDirectory()) {
                throw new IllegalStateException("Failed to create " + filesystemRoot);
            }
            return filesystemRoot;
        }

        @Override
        protected String[] packagesToScan() {
            return new String[]{
                "it.rest.extensions.content.search"
            };
        }
    }

    @MappedSuperclass
    public static class AbstractTestEntity {
        @Id
        @ContentId
        private String id = UUID.randomUUID().toString();

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getContentId() {
            return id;
        }

        public void setContentId(String id) {
            this.id = id;
        }
    }

    @Entity(name = "testEntityNoContent")
    public static class TestEntityNoContent extends AbstractTestEntity {
    }

    @Entity(name = "testEntityNotSearchable")
    public static class TestEntityNotSearchable extends AbstractTestEntity {
    }

    @Entity(name = "testEntitySharedId")
    public static class TestEntityWithSharedId extends AbstractTestEntity {
    }

    @Entity(name = "testEntitySeparateId")
    public static class TestEntityWithSeparateId {
        @Id
        private String id = UUID.randomUUID().toString();
        @ContentId
        private String contentId = UUID.randomUUID().toString();

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getContentId() {
            return contentId;
        }

        public void setContentId(String id) {
            this.contentId = id;
        }
    }

    @Entity
    public static class NoLookupStrategySearchEntity {
        @Id
        private String id = UUID.randomUUID().toString();
        @ContentId
        private String contentId = UUID.randomUUID().toString();

        public NoLookupStrategySearchEntity() {
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getContentId() {
            return contentId;
        }

        public void setContentId(String contentId) {
            this.contentId = contentId;
        }
    }

    @Entity
    public static class CustomSearchReturnTypeEntity {
        @Id
        private String id = UUID.randomUUID().toString();
        @ContentId
        private String contentId = UUID.randomUUID().toString();

        public CustomSearchReturnTypeEntity() {
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getContentId() {
            return contentId;
        }

        public void setContentId(String contentId) {
            this.contentId = contentId;
        }
    }

    public static class CustomResult {

        @ContentId
        private String contentId;

        @Highlight
        private String highlight;

        @Attribute(name = "foo")
        private String foo;

        @Attribute(name = "bar")
        private String bar;

        public CustomResult(String contentId, String highlight, String foo, String bar) {
            this.contentId = contentId;
            this.highlight = highlight;
            this.foo = foo;
            this.bar = bar;
        }

        public String getContentId() {
            return contentId;
        }

        public void setContentId(String contentId) {
            this.contentId = contentId;
        }

        @SuppressWarnings("unused")
        public String getHighlight() {
            return highlight;
        }

        @SuppressWarnings("unused")
        public void setHighlight(String highlight) {
            this.highlight = highlight;
        }

        public String getFoo() {
            return foo;
        }

        public void setFoo(String foo) {
            this.foo = foo;
        }

        @SuppressWarnings("unused")
        public String getBar() {
            return bar;
        }

        @SuppressWarnings("unused")
        public void setBar(String bar) {
            this.bar = bar;
        }
    }

    @Nested
   class ContentSearchRestControllerCases {
        @Nested
        class SearchEndpointCases {
            @Nested
            @DisplayName("Given an entity has no content associations")
            class EntityHasNoContentAssociations {
                @BeforeEach
                void setUp() {
                    mvc = MockMvcBuilders.webAppContextSetup(context).build();

                    reflectionService = mock(ReflectionService.class);
                    ContentSearchRestController controller = context.getBean(ContentSearchRestController.class);
                    controller.setReflectionService(reflectionService);

                    defaultLookupStrategy = spy(new DefaultEntityLookupStrategy());
                    controller.setDefaultEntityLookupStrategy(defaultLookupStrategy);
                    queryMethodsLookupStrategy = spy(new QueryMethodsEntityLookupStrategy());
                    controller.setQueryMethodsEntityLookupStrategy(queryMethodsLookupStrategy);
                }

                @Test
                void shouldThrowAnException() throws Exception {
                    MvcResult result = mvc.perform(get(
                            "/testEntityNoContents/searchContent?queryString=one")
                            .accept("application/hal+json"))
                        .andExpect(status().isNotFound()).andReturn();

                    assertThat(result.getResolvedException()).isNotNull();
                    assertThat(result.getResolvedException().getMessage()).contains("no content");
                }
            }

            @Nested

            @DisplayName("Given a store that is not searchable")

            class StoreThatIsNotSearchable {
                @BeforeEach
                void setUp() {
                    mvc = MockMvcBuilders.webAppContextSetup(context).build();

                    reflectionService = mock(ReflectionService.class);
                    ContentSearchRestController controller = context.getBean(ContentSearchRestController.class);
                    controller.setReflectionService(reflectionService);

                    defaultLookupStrategy = spy(new DefaultEntityLookupStrategy());
                    controller.setDefaultEntityLookupStrategy(defaultLookupStrategy);
                    queryMethodsLookupStrategy = spy(new QueryMethodsEntityLookupStrategy());
                    controller.setQueryMethodsEntityLookupStrategy(queryMethodsLookupStrategy);
                }

                @Test
                void shouldThrowAResourceNotFoundException() throws Exception {
                    MvcResult result = mvc.perform(get(
                            "/testEntityNotSearchables/searchContent?queryString=one")
                            .accept("application/hal+json"))
                        .andExpect(status().isNotFound()).andReturn();

                    assertThat(result.getResolvedException()).isNotNull();
                    assertThat(result.getResolvedException().getMessage()).contains("not searchable");
                }
            }

            @Nested

            @DisplayName("Given the search method is invalid")

            class SearchMethodIsInvalid {
                @BeforeEach
                void setUp() {
                    mvc = MockMvcBuilders.webAppContextSetup(context).build();

                    reflectionService = mock(ReflectionService.class);
                    ContentSearchRestController controller = context.getBean(ContentSearchRestController.class);
                    controller.setReflectionService(reflectionService);

                    defaultLookupStrategy = spy(new DefaultEntityLookupStrategy());
                    controller.setDefaultEntityLookupStrategy(defaultLookupStrategy);
                    queryMethodsLookupStrategy = spy(new QueryMethodsEntityLookupStrategy());
                    controller.setQueryMethodsEntityLookupStrategy(queryMethodsLookupStrategy);
                }

                @Test
                void shouldReturnAResourceNotFoundException() throws Exception {
                    mvc.perform(get(
                            "/testEntityWithSharedIds/searchContent/invalidSearchMethod?keyword=one")
                            .accept("application/hal+json"))
                        .andExpect(status().isNotFound()).andReturn();
                }
            }

            @Nested

            @DisplayName("Given no keywords are specified")

            class NoKeywordsAreSpecified {
                @BeforeEach
                void setUp() {
                    mvc = MockMvcBuilders.webAppContextSetup(context).build();

                    reflectionService = mock(ReflectionService.class);
                    ContentSearchRestController controller = context.getBean(ContentSearchRestController.class);
                    controller.setReflectionService(reflectionService);

                    defaultLookupStrategy = spy(new DefaultEntityLookupStrategy());
                    controller.setDefaultEntityLookupStrategy(defaultLookupStrategy);
                    queryMethodsLookupStrategy = spy(new QueryMethodsEntityLookupStrategy());
                    controller.setQueryMethodsEntityLookupStrategy(queryMethodsLookupStrategy);
                }

                @Test
                void shouldReturnABadRequestException() throws Exception {
                    mvc.perform(get("/testEntityWithSharedIds/searchContent")
                            .accept("application/hal+json"))
                        .andExpect(status().isBadRequest());
                }
            }

            @Nested

            @DisplayName("Given paged results are requested")

            class PagedResultsAreRequested {
                @BeforeEach
                void setUp() {
                    mvc = MockMvcBuilders.webAppContextSetup(context).build();

                    reflectionService = mock(ReflectionService.class);
                    ContentSearchRestController controller = context.getBean(ContentSearchRestController.class);
                    controller.setReflectionService(reflectionService);

                    defaultLookupStrategy = spy(new DefaultEntityLookupStrategy());
                    controller.setDefaultEntityLookupStrategy(defaultLookupStrategy);
                    queryMethodsLookupStrategy = spy(new QueryMethodsEntityLookupStrategy());
                    controller.setQueryMethodsEntityLookupStrategy(queryMethodsLookupStrategy);
                }

                @Test
                void shouldInvokeSearchWithThePageRequest() throws Exception {
                    mvc.perform(get("/testEntityWithSeparateIds/searchContent?queryString=else&page=1&size=1")
                            .accept("application/hal+json"))
                        .andExpect(status().isOk()).andReturn();

                    Method m = ReflectionUtils.findMethod(Searchable.class, "search", String.class, Pageable.class);
                    PageRequest pageable = PageRequest.of(1, 1);

                    verify(reflectionService).invokeMethod(eq(m), any(), eq("else"), eq(pageable));
                }
            }

            @Nested

            @DisplayName("Given an entity with an overloaded id field")

            class EntityWithAnOverloadedIdField {
                @Nested
                @DisplayName("Given no results are found")
                class NoResultsAreFound {
                    @BeforeEach
                    void setUp() {
                        mvc = MockMvcBuilders.webAppContextSetup(context).build();

                        reflectionService = mock(ReflectionService.class);
                        ContentSearchRestController controller = context.getBean(ContentSearchRestController.class);
                        controller.setReflectionService(reflectionService);

                        defaultLookupStrategy = spy(new DefaultEntityLookupStrategy());
                        controller.setDefaultEntityLookupStrategy(defaultLookupStrategy);
                        queryMethodsLookupStrategy = spy(new QueryMethodsEntityLookupStrategy());
                        controller.setQueryMethodsEntityLookupStrategy(queryMethodsLookupStrategy);

                        when(reflectionService.invokeMethod(any(), any(),
                            eq("one"), isA(Pageable.class), eq(InternalResult.class)))
                            .thenReturn(Collections.emptyList());
                    }

                    @Test
                    void shouldReturnAnEmptyResponseEntity() throws Exception {
                        MvcResult result = mvc.perform(get(
                                "/testEntityWithSharedIds/searchContent?queryString=one")
                                .accept("application/hal+json"))
                            .andExpect(status().isOk()).andReturn();

                        ReadableRepresentation halResponse = representationFactory
                            .readRepresentation("application/hal+json",
                                new StringReader(result.getResponse()
                                    .getContentAsString()));
                        assertThat(halResponse.getResources().size()).isEqualTo(0);
                    }
                }

                @Nested

                @DisplayName("Given results are found")

                class ResultsAreFound {
                    @BeforeEach
                    void setUp() {
                        mvc = MockMvcBuilders.webAppContextSetup(context).build();

                        reflectionService = mock(ReflectionService.class);
                        ContentSearchRestController controller = context.getBean(ContentSearchRestController.class);
                        controller.setReflectionService(reflectionService);

                        defaultLookupStrategy = spy(new DefaultEntityLookupStrategy());
                        controller.setDefaultEntityLookupStrategy(defaultLookupStrategy);
                        queryMethodsLookupStrategy = spy(new QueryMethodsEntityLookupStrategy());
                        controller.setQueryMethodsEntityLookupStrategy(queryMethodsLookupStrategy);

                        TestEntityWithSharedId entity = new TestEntityWithSharedId();
                        repository.save(entity);

                        entity2 = new TestEntityWithSharedId();
                        repository.save(entity2);

                        internalResults = new ArrayList<>();
                        internalResults.add(new InternalResult(null, entity.getContentId()));
                        internalResults.add(new InternalResult(entity2.getId(), entity2.getContentId()));

                        sharedIds = new ArrayList<>();
                        sharedIds.add(entity.getId());
                        sharedIds.add(entity2.getId());

                        when(reflectionService.invokeMethod(any(), any(),
                            eq("two"))).thenReturn(internalResults);
                    }

                    @Test
                    void shouldReturnAResponseEntityWithTheEntity() throws Exception {
                        MvcResult result = mvc.perform(get(
                                "/testEntityWithSharedIds/searchContent?queryString=two")
                                .accept("application/hal+json"))
                            .andExpect(status().isOk()).andReturn();

                        verify(defaultLookupStrategy, never()).lookup(any(RootResourceInformation.class), any(RepositoryInformation.class), anyList(),
                            anyList());
                        verify(queryMethodsLookupStrategy, never()).lookup(any(RootResourceInformation.class), any(RepositoryInformation.class), anyList(),
                            anyList());

                        ReadableRepresentation halResponse = representationFactory
                            .readRepresentation("application/hal+json",
                                new StringReader(result.getResponse()
                                    .getContentAsString()));
                        assertThat(halResponse
                            .getResourcesByRel("testEntityWithSharedIds").size()).isEqualTo(2);
                        String id1 = halResponse
                            .getResourcesByRel("testEntityWithSharedIds").get(0)
                            .getValue("contentId").toString();
                        String id2 = halResponse
                            .getResourcesByRel("testEntityWithSharedIds").get(1)
                            .getValue("contentId").toString();
                        assertThat(sharedIds).contains(id1);
                        assertThat(sharedIds).contains(id2);
                        assertThat(id1).isNotEqualTo(id2);
                    }
                }

                @Nested

                @DisplayName("Given results contain orphaned fulltext documents")

                class ResultsContainOrphanedFulltextDocuments {
                    @BeforeEach
                    void setUp() {
                        mvc = MockMvcBuilders.webAppContextSetup(context).build();

                        reflectionService = mock(ReflectionService.class);
                        ContentSearchRestController controller = context.getBean(ContentSearchRestController.class);
                        controller.setReflectionService(reflectionService);

                        defaultLookupStrategy = spy(new DefaultEntityLookupStrategy());
                        controller.setDefaultEntityLookupStrategy(defaultLookupStrategy);
                        queryMethodsLookupStrategy = spy(new QueryMethodsEntityLookupStrategy());
                        controller.setQueryMethodsEntityLookupStrategy(queryMethodsLookupStrategy);

                        entity2 = new TestEntityWithSharedId();
                        repository.save(entity2);

                        String orphanedContentId = UUID.randomUUID().toString();

                        internalResults = new ArrayList<>();
                        internalResults.add(new InternalResult(null, orphanedContentId));
                        internalResults.add(new InternalResult(entity2.getId(), entity2.getContentId()));

                        contentIds = new ArrayList<>();
                        contentIds.add(orphanedContentId); // invalid id
                        contentIds.add(entity2.getContentId());

                        when(reflectionService.invokeMethod(any(), any(),
                            eq("else"))).thenReturn(internalResults);
                    }

                    @Test
                    void shouldFilterOutInvalidIDs() throws Exception {
                        MvcResult result = mvc.perform(get(
                                "/testEntityWithSharedIds/searchContent?queryString=else")
                                .accept("application/hal+json"))
                            .andExpect(status().isOk()).andReturn();

                        ReadableRepresentation halResponse = representationFactory
                            .readRepresentation("application/hal+json",
                                new StringReader(result.getResponse()
                                    .getContentAsString()));
                        assertThat(halResponse
                            .getResourcesByRel("testEntityWithSharedIds").size()).isEqualTo(1);
                        String id1 = halResponse
                            .getResourcesByRel("testEntityWithSharedIds").get(0)
                            .getValue("contentId").toString();
                        assertThat(contentIds).contains(id1);
                    }
                }
            }

            @Nested

            @DisplayName("Given an entity with separate id content ID fields")

            class EntityWithSeparateIdContentIdFields {
                @Nested
                @DisplayName("Given no results are found")
                class NoResultsAreFound {
                    @BeforeEach
                    void setUp() {
                        mvc = MockMvcBuilders.webAppContextSetup(context).build();

                        reflectionService = mock(ReflectionService.class);
                        ContentSearchRestController controller = context.getBean(ContentSearchRestController.class);
                        controller.setReflectionService(reflectionService);

                        defaultLookupStrategy = spy(new DefaultEntityLookupStrategy());
                        controller.setDefaultEntityLookupStrategy(defaultLookupStrategy);
                        queryMethodsLookupStrategy = spy(new QueryMethodsEntityLookupStrategy());
                        controller.setQueryMethodsEntityLookupStrategy(queryMethodsLookupStrategy);

                        when(reflectionService.invokeMethod(any(), any(),
                            eq("something"), isA(Pageable.class), eq(InternalResult.class)))
                            .thenReturn(Collections.emptyList());
                    }

                    @Test
                    void shouldReturnAnEmptyResponseEntity() throws Exception {
                        MvcResult result = mvc.perform(get(
                                "/testEntityWithSeparateIds/searchContent?queryString=something")
                                .accept("application/hal+json"))
                            .andExpect(status().isOk()).andReturn();

                        ReadableRepresentation halResponse = representationFactory
                            .readRepresentation("application/hal+json",
                                new StringReader(result.getResponse()
                                    .getContentAsString()));
                        assertThat(halResponse.getResources().size()).isEqualTo(0);
                    }
                }

                @Nested

                @DisplayName("Given results are found with entity i ds")

                class ResultsAreFoundWithEntityIDs {
                    @BeforeEach
                    void setUp() {
                        mvc = MockMvcBuilders.webAppContextSetup(context).build();

                        reflectionService = mock(ReflectionService.class);
                        ContentSearchRestController controller = context.getBean(ContentSearchRestController.class);
                        controller.setReflectionService(reflectionService);

                        defaultLookupStrategy = spy(new DefaultEntityLookupStrategy());
                        controller.setDefaultEntityLookupStrategy(defaultLookupStrategy);
                        queryMethodsLookupStrategy = spy(new QueryMethodsEntityLookupStrategy());
                        controller.setQueryMethodsEntityLookupStrategy(queryMethodsLookupStrategy);

                        entity3 = new TestEntityWithSeparateId();
                        entityWithSeparateRepository.save(entity3);

                        TestEntityWithSeparateId entity4 = new TestEntityWithSeparateId();
                        entityWithSeparateRepository.save(entity4);

                        internalResults = new ArrayList<>();
                        internalResults.add(new InternalResult(null, entity3.getContentId()));
                        internalResults.add(new InternalResult(entity4.getId(), entity4.getContentId()));

                        contentIds = new ArrayList<>();
                        contentIds.add(entity3.getContentId());
                        contentIds.add(entity4.getContentId());

                        when(reflectionService.invokeMethod(any(), any(),
                            eq("else"))).thenReturn(internalResults);
                    }

                    @Test
                    void shouldReturnAResponseEntityWithTheEntity() throws Exception {
                        MvcResult result = mvc.perform(get(
                                "/testEntityWithSeparateIds/searchContent?queryString=else")
                                .accept("application/hal+json"))
                            .andExpect(status().isOk()).andReturn();

                        verify(defaultLookupStrategy, never()).lookup(any(RootResourceInformation.class), any(RepositoryInformation.class), anyList(),
                            anyList());
                        verify(queryMethodsLookupStrategy).lookup(any(RootResourceInformation.class), any(RepositoryInformation.class), anyList(),
                            anyList());

                        ReadableRepresentation halResponse = representationFactory
                            .readRepresentation("application/hal+json",
                                new StringReader(result.getResponse()
                                    .getContentAsString()));
                        assertThat(halResponse
                            .getResourcesByRel("testEntityWithSeparateIds").size()).isEqualTo(2);
                        String id1 = halResponse
                            .getResourcesByRel("testEntityWithSeparateIds").get(0)
                            .getValue("contentId").toString();
                        String id2 = halResponse
                            .getResourcesByRel("testEntityWithSeparateIds").get(1)
                            .getValue("contentId").toString();
                        assertThat(contentIds).contains(id1);
                        assertThat(contentIds).contains(id2);
                        assertThat(id1).isNotEqualTo(id2);
                    }
                }

                @Nested

                @DisplayName("Given results contain orphaned fulltext documents")

                class ResultsContainOrphanedFulltextDocuments {
                    @BeforeEach
                    void setUp() {
                        mvc = MockMvcBuilders.webAppContextSetup(context).build();

                        reflectionService = mock(ReflectionService.class);
                        ContentSearchRestController controller = context.getBean(ContentSearchRestController.class);
                        controller.setReflectionService(reflectionService);

                        defaultLookupStrategy = spy(new DefaultEntityLookupStrategy());
                        controller.setDefaultEntityLookupStrategy(defaultLookupStrategy);
                        queryMethodsLookupStrategy = spy(new QueryMethodsEntityLookupStrategy());
                        controller.setQueryMethodsEntityLookupStrategy(queryMethodsLookupStrategy);

                        entity3 = new TestEntityWithSeparateId();
                        entityWithSeparateRepository.save(entity3);

                        String orphanedContentId = UUID.randomUUID().toString();

                        internalResults = new ArrayList<>();
                        internalResults.add(new InternalResult(null, orphanedContentId));
                        internalResults.add(new InternalResult(entity3.getId(), entity3.getContentId()));

                        contentIds = new ArrayList<>();
                        contentIds.add(orphanedContentId); // invalid id
                        contentIds.add(entity3.getContentId());

                        when(reflectionService.invokeMethod(any(), any(),
                            eq("else"))).thenReturn(internalResults);
                    }

                    @Test
                    void shouldFilterOutInvalidIDs() throws Exception {
                        MvcResult result = mvc.perform(get(
                                "/testEntityWithSeparateIds/searchContent?queryString=else")
                                .accept("application/hal+json"))
                            .andExpect(status().isOk()).andReturn();

                        ReadableRepresentation halResponse = representationFactory
                            .readRepresentation("application/hal+json",
                                new StringReader(result.getResponse()
                                    .getContentAsString()));
                        assertThat(halResponse
                            .getResourcesByRel("testEntityWithSeparateIds").size()).isEqualTo(1);
                        String id1 = halResponse
                            .getResourcesByRel("testEntityWithSeparateIds").get(0)
                            .getValue("contentId").toString();
                        assertThat(contentIds).contains(id1);
                    }
                }
            }

            @Nested

            @DisplayName("Given results are found returning a custom result type")

            class ResultsAreFoundReturningACustomResultType {
                @BeforeEach
                void setUp() {
                    mvc = MockMvcBuilders.webAppContextSetup(context).build();

                    reflectionService = mock(ReflectionService.class);
                    ContentSearchRestController controller = context.getBean(ContentSearchRestController.class);
                    controller.setReflectionService(reflectionService);

                    defaultLookupStrategy = spy(new DefaultEntityLookupStrategy());
                    controller.setDefaultEntityLookupStrategy(defaultLookupStrategy);
                    queryMethodsLookupStrategy = spy(new QueryMethodsEntityLookupStrategy());
                    controller.setQueryMethodsEntityLookupStrategy(queryMethodsLookupStrategy);

                    List<CustomResult> results = new ArrayList<>();

                    results.add(new CustomResult("12345", "<em>something else</em>", "foo1", "bar1"));
                    results.add(new CustomResult("67890", "<em>else altogether</em>", "foo2", "bar2"));

                    when(reflectionService.invokeMethod(any(), any(),
                        eq("else"))).thenReturn(results);
                }

                @Test
                void shouldReturnAResponseEntityWithTheEntity() throws Exception {
                    MvcResult result = mvc.perform(get(
                            "/repoWithCustomSearchReturnType/searchContent?queryString=else")
                            .accept("application/hal+json"))
                        .andExpect(status().isOk()).andReturn();

                    ReadableRepresentation halResponse = representationFactory
                        .readRepresentation("application/hal+json",
                            new StringReader(result.getResponse()
                                .getContentAsString()));
                    assertThat(halResponse
                        .getResourcesByRel("customResults").size()).isEqualTo(2);
                    assertThat(halResponse
                        .getResourcesByRel("customResults").get(0)
                        .getValue("highlight").toString()).isEqualTo("<em>something else</em>");
                    assertThat(halResponse
                        .getResourcesByRel("customResults").get(0)
                        .getValue("foo").toString()).isEqualTo("foo1");
                    assertThat(halResponse
                        .getResourcesByRel("customResults").get(0)
                        .getValue("bar").toString()).isEqualTo("bar1");
                    assertThat(halResponse
                        .getResourcesByRel("customResults").get(1)
                        .getValue("highlight").toString()).isEqualTo("<em>else altogether</em>");
                    assertThat(halResponse
                        .getResourcesByRel("customResults").get(1)
                        .getValue("foo").toString()).isEqualTo("foo2");
                    assertThat(halResponse
                        .getResourcesByRel("customResults").get(1)
                        .getValue("bar").toString()).isEqualTo("bar2");
                }
            }

            @Nested

            @DisplayName("Given paged results are found returning a custom result type")

            class PagedResultsAreFoundReturningACustomResultType {
                @BeforeEach
                void setUp() {
                    mvc = MockMvcBuilders.webAppContextSetup(context).build();

                    reflectionService = mock(ReflectionService.class);
                    ContentSearchRestController controller = context.getBean(ContentSearchRestController.class);
                    controller.setReflectionService(reflectionService);

                    defaultLookupStrategy = spy(new DefaultEntityLookupStrategy());
                    controller.setDefaultEntityLookupStrategy(defaultLookupStrategy);
                    queryMethodsLookupStrategy = spy(new QueryMethodsEntityLookupStrategy());
                    controller.setQueryMethodsEntityLookupStrategy(queryMethodsLookupStrategy);

                    List<CustomResult> results = new ArrayList<>();

                    results.add(new CustomResult("12345", "<em>something else</em>", "foo1", "bar1"));

                    when(reflectionService.invokeMethod(any(), any(),
                        eq("else"), isA(Pageable.class))).thenReturn(results);
                }

                @Test
                void shouldReturnAResponseEntityWithTheEntity() throws Exception {
                    MvcResult result = mvc.perform(get(
                            "/repoWithCustomSearchReturnType/searchContent?queryString=else&page=1&size=1")
                            .accept("application/hal+json"))
                        .andExpect(status().isOk()).andReturn();

                    ReadableRepresentation halResponse = representationFactory
                        .readRepresentation("application/hal+json",
                            new StringReader(result.getResponse()
                                .getContentAsString()));
                    assertThat(halResponse
                        .getResourcesByRel("customResults").size()).isEqualTo(1);
                    assertThat(halResponse
                        .getResourcesByRel("customResults").get(0)
                        .getValue("highlight").toString()).isEqualTo("<em>something else</em>");
                    assertThat(halResponse
                        .getResourcesByRel("customResults").get(0)
                        .getValue("foo").toString()).isEqualTo("foo1");
                    assertThat(halResponse
                        .getResourcesByRel("customResults").get(0)
                        .getValue("bar").toString()).isEqualTo("bar1");
                }
            }

            @Nested

            @DisplayName("Given a repository with no entity lookup query method")

            class RepositoryWithNoEntityLookupQueryMethod {
                @BeforeEach
                void setUp() {
                    mvc = MockMvcBuilders.webAppContextSetup(context).build();

                    reflectionService = mock(ReflectionService.class);
                    ContentSearchRestController controller = context.getBean(ContentSearchRestController.class);
                    controller.setReflectionService(reflectionService);

                    defaultLookupStrategy = spy(new DefaultEntityLookupStrategy());
                    controller.setDefaultEntityLookupStrategy(defaultLookupStrategy);
                    queryMethodsLookupStrategy = spy(new QueryMethodsEntityLookupStrategy());
                    controller.setQueryMethodsEntityLookupStrategy(queryMethodsLookupStrategy);

                    NoLookupStrategySearchEntity entity5 = new NoLookupStrategySearchEntity();
                    repoWithNoLookupStrategy.save(entity5);

                    NoLookupStrategySearchEntity entity6 = new NoLookupStrategySearchEntity();
                    repoWithNoLookupStrategy.save(entity6);

                    internalResults = new ArrayList<>();
                    internalResults.add(new InternalResult(null, entity5.getContentId()));
                    internalResults.add(new InternalResult(entity6.getId(), entity6.getContentId()));

                    contentIds = new ArrayList<>();
                    contentIds.add(entity5.getContentId());
                    contentIds.add(entity6.getContentId());

                    when(reflectionService.invokeMethod(any(), any(), eq("else"))).thenReturn(internalResults);
                }

                @Test
                void shouldReturnAResponseWithTheEntity() throws Exception {
                    MvcResult result = mvc.perform(get(
                            "/repoWithNoLookupStrategy/searchContent?queryString=else")
                            .accept("application/hal+json"))
                        .andExpect(status().isOk()).andReturn();

                    verify(defaultLookupStrategy).lookup(any(RootResourceInformation.class), any(RepositoryInformation.class), anyList(),
                        anyList());
                    verify(queryMethodsLookupStrategy, never()).lookup(any(RootResourceInformation.class), any(RepositoryInformation.class), anyList(),
                        anyList());

                    ReadableRepresentation halResponse = representationFactory
                        .readRepresentation("application/hal+json",
                            new StringReader(result.getResponse()
                                .getContentAsString()));
                    assertThat(halResponse
                        .getResourcesByRel("noLookupStrategySearchEntities").size()).isEqualTo(2);
                    String id1 = halResponse
                        .getResourcesByRel("noLookupStrategySearchEntities").get(0)
                        .getValue("contentId").toString();
                    String id2 = halResponse
                        .getResourcesByRel("noLookupStrategySearchEntities").get(1)
                        .getValue("contentId").toString();
                    assertThat(contentIds).contains(id1);
                    assertThat(contentIds).contains(id2);
                    assertThat(id1).isNotEqualTo(id2);
                }
            }
        }

        @Nested
        class FetchEntitiesInBatchesCases {
            @BeforeEach
            void setUp() {
                mvc = MockMvcBuilders.webAppContextSetup(context).build();

                reflectionService = mock(ReflectionService.class);
                ContentSearchRestController controller = context.getBean(ContentSearchRestController.class);
                controller.setReflectionService(reflectionService);

                defaultLookupStrategy = spy(new DefaultEntityLookupStrategy());
                controller.setDefaultEntityLookupStrategy(defaultLookupStrategy);
                queryMethodsLookupStrategy = spy(new QueryMethodsEntityLookupStrategy());
                controller.setQueryMethodsEntityLookupStrategy(queryMethodsLookupStrategy);
            }

            @Test
            void shouldBatchQueriesAppropriately() {
                List<String> ids = new ArrayList<>();
                for (int i = 0; i < 500; i++) {
                    TestEntityWithSeparateId entity = new TestEntityWithSeparateId();
                    entity = entityWithSeparateRepository.save(entity);
                    ids.add(entity.getId());
                }

                List<TestEntityWithSeparateId> entities = new ArrayList<>();

                CrudRepository<?, ?> repoSpy = mock(CrudRepository.class, AdditionalAnswers.delegatesTo(entityWithSeparateRepository));

                ContentSearchRestController.fetchEntitiesInBatches(repoSpy, ids, entities);

                verify(repoSpy, times(2)).findAllById(anyCollection());

                assertThat(entities.size()).isEqualTo(500);
            }
        }
    }
}
