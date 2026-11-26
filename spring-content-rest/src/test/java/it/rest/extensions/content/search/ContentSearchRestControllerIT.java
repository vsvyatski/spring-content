package it.rest.extensions.content.search;

import com.theoryinpractise.halbuilder.api.ReadableRepresentation;
import com.theoryinpractise.halbuilder.api.RepresentationFactory;
import com.theoryinpractise.halbuilder.standard.StandardRepresentationFactory;
import internal.org.springframework.content.rest.support.RestTestMediaTypes;
import internal.org.springframework.content.rest.support.config.JpaInfrastructureConfig;
import internal.org.springframework.data.rest.extensions.contentsearch.DefaultEntityLookupStrategy;
import internal.org.springframework.data.rest.extensions.contentsearch.QueryMethodsEntityLookupStrategy;
import it.rest.extensions.content.search.support.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.AdditionalAnswers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.search.Searchable;
import org.springframework.content.commons.utils.ReflectionService;
import org.springframework.content.fs.config.EnableFileSystemStores;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.core.RepositoryInformation;
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
    ContentSearchRestControllerIT.TestConfig.class, DelegatingWebMvcConfiguration.class,
    RepositoryRestMvcConfiguration.class, RestConfiguration.class
})
@Transactional
@ExtendWith(SpringExtension.class)
public class ContentSearchRestControllerIT {

    // mocks/spys
    private static ReflectionService reflectionService;
    private static DefaultEntityLookupStrategy defaultLookupStrategy;
    private static QueryMethodsEntityLookupStrategy queryMethodsLookupStrategy;
    private final RepresentationFactory representationFactory = new StandardRepresentationFactory();
    @Autowired
    private SearchSharedContentIdEntityRepository repository;
    @Autowired
    private SearchSeparateContentIdEntityRepository entityWithSeparateRepository;
    @Autowired
    private NoLookupStrategySearchEntityRepository repoWithNoLookupStrategy;
    @Autowired
    private WebApplicationContext context;
    private MockMvc mvc;
    private SearchSharedContentIdEntity entity2;
    private List<String> sharedIds;
    private SearchSeparateContentIdEntity entity3;
    private List<String> contentIds;
    private List<InternalResult> internalResults;

    @Configuration
    @EnableJpaRepositories(basePackages = {
        "it.rest.extensions.content.search",
        "it.rest.extensions.content.search.support"
    })
    @EnableTransactionManagement
    @EnableFileSystemStores(basePackages = {
        "it.rest.extensions.content.search",
        "it.rest.extensions.content.search.support"
    })
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
                "it.rest.extensions.content.search",
                "it.rest.extensions.content.search.support"
            };
        }
    }

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
        void shouldThrowException() throws Exception {
            MvcResult result = mvc.perform(get(
                    "/searchNoContentEntities/searchContent?queryString=one")
                    .accept(RestTestMediaTypes.APPLICATION_HAL_JSON_VALUE))
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
        void shouldThrowResourceNotFoundException() throws Exception {
            MvcResult result = mvc.perform(get(
                    "/searchNotSearchableEntities/searchContent?queryString=one")
                    .accept(RestTestMediaTypes.APPLICATION_HAL_JSON_VALUE))
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
        void shouldReturnResourceNotFoundException() throws Exception {
            mvc.perform(get(
                    "/searchSharedContentIdEntities/searchContent/invalidSearchMethod?keyword=one")
                    .accept(RestTestMediaTypes.APPLICATION_HAL_JSON_VALUE))
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
        void shouldReturnBadRequestException() throws Exception {
            mvc.perform(get("/searchSharedContentIdEntities/searchContent")
                    .accept(RestTestMediaTypes.APPLICATION_HAL_JSON_VALUE))
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
        void shouldInvokeSearchWithPageRequest() throws Exception {
            mvc.perform(get("/searchSeparateContentIdEntities/searchContent?queryString=else&page=1&size=1")
                    .accept(RestTestMediaTypes.APPLICATION_HAL_JSON_VALUE))
                .andExpect(status().isOk()).andReturn();

            Method m = ReflectionUtils.findMethod(Searchable.class, "search", String.class, Pageable.class);
            PageRequest pageable = PageRequest.of(1, 1);

            verify(reflectionService).invokeMethod(eq(m), any(), eq("else"), eq(pageable));
        }
    }

    @Nested
    @DisplayName("Given an entity with an overloaded ID field")
    class EntityWithOverloadedIdField {
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
            void shouldReturnEmptyResponseEntity() throws Exception {
                MvcResult result = mvc.perform(get(
                        "/searchSharedContentIdEntities/searchContent?queryString=one")
                        .accept(RestTestMediaTypes.APPLICATION_HAL_JSON_VALUE))
                    .andExpect(status().isOk()).andReturn();

                ReadableRepresentation halResponse = representationFactory
                    .readRepresentation(RestTestMediaTypes.APPLICATION_HAL_JSON_VALUE,
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

                SearchSharedContentIdEntity entity = new SearchSharedContentIdEntity();
                repository.save(entity);

                entity2 = new SearchSharedContentIdEntity();
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
            void shouldReturnResponseEntityWithEntity() throws Exception {
                MvcResult result = mvc.perform(get(
                        "/searchSharedContentIdEntities/searchContent?queryString=two")
                        .accept(RestTestMediaTypes.APPLICATION_HAL_JSON_VALUE))
                    .andExpect(status().isOk()).andReturn();

                verify(defaultLookupStrategy, never()).lookup(any(RootResourceInformation.class), any(RepositoryInformation.class), anyList(),
                    anyList());
                verify(queryMethodsLookupStrategy, never()).lookup(any(RootResourceInformation.class), any(RepositoryInformation.class), anyList(),
                    anyList());

                ReadableRepresentation halResponse = representationFactory
                    .readRepresentation(RestTestMediaTypes.APPLICATION_HAL_JSON_VALUE,
                        new StringReader(result.getResponse()
                            .getContentAsString()));
                assertThat(halResponse
                    .getResourcesByRel("searchSharedContentIdEntities").size()).isEqualTo(2);
                String id1 = halResponse
                    .getResourcesByRel("searchSharedContentIdEntities").get(0)
                    .getValue("contentId").toString();
                String id2 = halResponse
                    .getResourcesByRel("searchSharedContentIdEntities").get(1)
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

                entity2 = new SearchSharedContentIdEntity();
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
                        "/searchSharedContentIdEntities/searchContent?queryString=else")
                        .accept(RestTestMediaTypes.APPLICATION_HAL_JSON_VALUE))
                    .andExpect(status().isOk()).andReturn();

                ReadableRepresentation halResponse = representationFactory
                    .readRepresentation(RestTestMediaTypes.APPLICATION_HAL_JSON_VALUE,
                        new StringReader(result.getResponse()
                            .getContentAsString()));
                assertThat(halResponse
                    .getResourcesByRel("searchSharedContentIdEntities").size()).isEqualTo(1);
                String id1 = halResponse
                    .getResourcesByRel("searchSharedContentIdEntities").get(0)
                    .getValue("contentId").toString();
                assertThat(contentIds).contains(id1);
            }
        }
    }

    @Nested
    @DisplayName("Given an entity with separate ID content ID fields")
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
            void shouldReturnEmptyResponseEntity() throws Exception {
                MvcResult result = mvc.perform(get(
                        "/searchSeparateContentIdEntities/searchContent?queryString=something")
                        .accept(RestTestMediaTypes.APPLICATION_HAL_JSON_VALUE))
                    .andExpect(status().isOk()).andReturn();

                ReadableRepresentation halResponse = representationFactory
                    .readRepresentation(RestTestMediaTypes.APPLICATION_HAL_JSON_VALUE,
                        new StringReader(result.getResponse()
                            .getContentAsString()));
                assertThat(halResponse.getResources().size()).isEqualTo(0);
            }
        }

        @Nested
        @DisplayName("Given results are found with entity IDs")
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

                entity3 = new SearchSeparateContentIdEntity();
                entityWithSeparateRepository.save(entity3);

                SearchSeparateContentIdEntity entity4 = new SearchSeparateContentIdEntity();
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
            void shouldReturnResponseEntityWithEntity() throws Exception {
                MvcResult result = mvc.perform(get(
                        "/searchSeparateContentIdEntities/searchContent?queryString=else")
                        .accept(RestTestMediaTypes.APPLICATION_HAL_JSON_VALUE))
                    .andExpect(status().isOk()).andReturn();

                verify(defaultLookupStrategy, never()).lookup(any(RootResourceInformation.class), any(RepositoryInformation.class), anyList(),
                    anyList());
                verify(queryMethodsLookupStrategy).lookup(any(RootResourceInformation.class), any(RepositoryInformation.class), anyList(),
                    anyList());

                ReadableRepresentation halResponse = representationFactory
                    .readRepresentation(RestTestMediaTypes.APPLICATION_HAL_JSON_VALUE,
                        new StringReader(result.getResponse()
                            .getContentAsString()));
                assertThat(halResponse
                    .getResourcesByRel("searchSeparateContentIdEntities").size()).isEqualTo(2);
                String id1 = halResponse
                    .getResourcesByRel("searchSeparateContentIdEntities").get(0)
                    .getValue("contentId").toString();
                String id2 = halResponse
                    .getResourcesByRel("searchSeparateContentIdEntities").get(1)
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

                entity3 = new SearchSeparateContentIdEntity();
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
                        "/searchSeparateContentIdEntities/searchContent?queryString=else")
                        .accept(RestTestMediaTypes.APPLICATION_HAL_JSON_VALUE))
                    .andExpect(status().isOk()).andReturn();

                ReadableRepresentation halResponse = representationFactory
                    .readRepresentation(RestTestMediaTypes.APPLICATION_HAL_JSON_VALUE,
                        new StringReader(result.getResponse()
                            .getContentAsString()));
                assertThat(halResponse
                    .getResourcesByRel("searchSeparateContentIdEntities").size()).isEqualTo(1);
                String id1 = halResponse
                    .getResourcesByRel("searchSeparateContentIdEntities").get(0)
                    .getValue("contentId").toString();
                assertThat(contentIds).contains(id1);
            }
        }
    }

    @Nested
    @DisplayName("Given results are found returning a custom result type")
    class ResultsAreFoundReturningCustomResultType {
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
        void shouldReturnResponseEntityWithEntity() throws Exception {
            MvcResult result = mvc.perform(get(
                    "/customSearchReturnTypeEntities/searchContent?queryString=else")
                    .accept(RestTestMediaTypes.APPLICATION_HAL_JSON_VALUE))
                .andExpect(status().isOk()).andReturn();

            ReadableRepresentation halResponse = representationFactory
                .readRepresentation(RestTestMediaTypes.APPLICATION_HAL_JSON_VALUE,
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
    class PagedResultsAreFoundReturningCustomResultType {
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
        void shouldReturnResponseEntityWithEntity() throws Exception {
            MvcResult result = mvc.perform(get(
                    "/customSearchReturnTypeEntities/searchContent?queryString=else&page=1&size=1")
                    .accept(RestTestMediaTypes.APPLICATION_HAL_JSON_VALUE))
                .andExpect(status().isOk()).andReturn();

            ReadableRepresentation halResponse = representationFactory
                .readRepresentation(RestTestMediaTypes.APPLICATION_HAL_JSON_VALUE,
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
        void shouldReturnResponseWithEntity() throws Exception {
            MvcResult result = mvc.perform(get(
                    "/noLookupStrategySearchEntities/searchContent?queryString=else")
                    .accept(RestTestMediaTypes.APPLICATION_HAL_JSON_VALUE))
                .andExpect(status().isOk()).andReturn();

            verify(defaultLookupStrategy).lookup(any(RootResourceInformation.class), any(RepositoryInformation.class), anyList(),
                anyList());
            verify(queryMethodsLookupStrategy, never()).lookup(any(RootResourceInformation.class), any(RepositoryInformation.class), anyList(),
                anyList());

            ReadableRepresentation halResponse = representationFactory
                .readRepresentation(RestTestMediaTypes.APPLICATION_HAL_JSON_VALUE,
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

    @Nested
    @DisplayName("When fetching entities in batches")
    class FetchEntitiesInBatches {
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
                SearchSeparateContentIdEntity entity = new SearchSeparateContentIdEntity();
                entity = entityWithSeparateRepository.save(entity);
                ids.add(entity.getId());
            }

            List<SearchSeparateContentIdEntity> entities = new ArrayList<>();

            @SuppressWarnings("unchecked")
            CrudRepository<SearchSeparateContentIdEntity, String> repoSpy = mock(CrudRepository.class,
                AdditionalAnswers.delegatesTo(entityWithSeparateRepository));

            ContentSearchRestController.fetchEntitiesInBatches(repoSpy, ids, entities);

            verify(repoSpy, times(2)).findAllById(anyCollection());

            assertThat(entities.size()).isEqualTo(500);
        }
    }
}
