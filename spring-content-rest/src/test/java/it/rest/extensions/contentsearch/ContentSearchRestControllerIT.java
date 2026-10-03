package it.rest.extensions.contentsearch;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

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
import org.springframework.data.domain.Page;
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
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;

import com.theoryinpractise.halbuilder.api.ReadableRepresentation;
import com.theoryinpractise.halbuilder.api.RepresentationFactory;
import com.theoryinpractise.halbuilder.standard.StandardRepresentationFactory;

import internal.org.springframework.content.rest.support.config.JpaInfrastructureConfig;
import internal.org.springframework.data.rest.extensions.contentsearch.DefaultEntityLookupStrategy;
import internal.org.springframework.data.rest.extensions.contentsearch.QueryMethodsEntityLookupStrategy;

// because the controller bean is shared and we need to instruct the reflection service
// to behave differently in each tests
@WebAppConfiguration
@ContextConfiguration(classes = {
        ContentSearchRestControllerIT.TestConfig.class,
        DelegatingWebMvcConfiguration.class, RepositoryRestMvcConfiguration.class,
        RestConfiguration.class })
@Transactional
@ExtendWith(SpringExtension.class)
public class ContentSearchRestControllerIT {

    @Autowired
    private ContentSearchRestController controller;

    @Autowired
    private TestEntityWithSharedIdsRepository repository;

    @Autowired
    private TestEntityWithSeparateIdsRepository entityWithSeparateRepository;

    @Autowired
    private RepositoryWithNoLookupStrategy repoWithNoLookupStrategy;

    @Autowired
    private TestEntityWithSeparateIdsSearchableStore entityWithSeparateStore;

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;
    private RepresentationFactory representationFactory = new StandardRepresentationFactory();

    private TestEntityWithSharedId entity;
    private TestEntityWithSharedId entity2;
    private List<String> sharedIds;

    private TestEntityWithSeparateId entity3;
    private TestEntityWithSeparateId entity4;
    private List<String> contentIds;

    private TestEntity2 entity5;
    private TestEntity2 entity6;

    private List<InternalResult> internalResults;

    // mocks/spys
    private static ReflectionService reflectionService;
    private static DefaultEntityLookupStrategy defaultLookupStrategy;
    private static QueryMethodsEntityLookupStrategy queryMethodsLookupStrategy;

    
    @Nested
    class ContentSearchRestControllerCases {
        @Nested
        class SearchEndpointCases {
            @Nested
            class GivenAnEntityHasNoContentAssociations {
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

                                            assertThat(result.getResolvedException().getMessage()).contains("no content");
                }
            }
            @Nested
            class GivenAStoreThatIsNotSearchable {
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

                                            assertThat(result.getResolvedException().getMessage()).contains("not searchable");
                }
            }
            @Nested
            class GivenTheSearchMethodIsInvalid {
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
                    MvcResult result = mvc.perform(get(
                                                    "/testEntityWithSharedIds/searchContent/invalidSearchMethod?keyword=one")
                                                    .accept("application/hal+json"))
                                                    .andExpect(status().isNotFound()).andReturn();
                }
            }
            @Nested
            class GivenNoKeywordsAreSpecified {
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
            class GivenPagedResultsAreRequested {
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
                    MvcResult result = mvc.perform(get(
                                                    "/testEntityWithSeparateIds/searchContent?queryString=else&page=1&size=1")
                                                    .accept("application/hal+json"))
                                                    .andExpect(status().isOk()).andReturn();

                                            Method m = ReflectionUtils.findMethod(Searchable.class,"search", new Class<?>[] { String.class, Pageable.class });
                                            PageRequest pageable = PageRequest.of(1, 1);

                                            verify(reflectionService).invokeMethod(eq(m), any(), eq("else"), eq(pageable));
                }
            }
            @Nested
            class GivenAnEntityWithAnOverloadedIdField {
                @Nested
                class GivenNoResultsAreFound {
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
                                                            eq("one"), isA(Pageable.class), eq(InternalResult.class))).thenReturn(Collections.EMPTY_LIST);
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
                class GivenResultsAreFound {
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

                        entity = new TestEntityWithSharedId();
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

                                                    verify(defaultLookupStrategy, never()).lookup(any(RootResourceInformation.class), any(RepositoryInformation.class), any(List.class), any(List.class));
                                                    verify(queryMethodsLookupStrategy, never()).lookup(any(RootResourceInformation.class), any(RepositoryInformation.class), any(List.class), any(List.class));

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
                class GivenResultsContainOrphanedFulltextDocuments {
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
            class GivenAnEntityWithSeparateIdContentIdFields {
                @Nested
                class GivenNoResultsAreFound {
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
                                                            eq("something"), isA(Pageable.class), eq(InternalResult.class))).thenReturn(Collections.EMPTY_LIST);
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
                class GivenResultsAreFoundWithEntityIDs {
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

                                                    entity4 = new TestEntityWithSeparateId();
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

                                                    verify(defaultLookupStrategy, never()).lookup(any(RootResourceInformation.class), any(RepositoryInformation.class), any(List.class), any(List.class));
                                                    verify(queryMethodsLookupStrategy).lookup(any(RootResourceInformation.class), any(RepositoryInformation.class), any(List.class), any(List.class));

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
                class GivenResultsContainOrphanedFulltextDocuments {
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
            class GivenResultsAreFoundReturningACustomResultType {
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
            class GivenPagedResultsAreFoundReturningACustomResultType {
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
            class GivenARepositoryWithNoEntityLookupQueryMethod {
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

                    entity5 = new TestEntity2();
                                            repoWithNoLookupStrategy.save(entity5);

                                            entity6 = new TestEntity2();
                                            repoWithNoLookupStrategy.save(entity6);

                                            internalResults = new ArrayList<>();
                                            internalResults.add(new InternalResult(null, entity5.getContentId()));
                                            internalResults.add(new InternalResult(entity6.getId(), entity6.getContentId()));

                                            contentIds = new ArrayList<>();
                                            contentIds.add(entity5.getContentId().toString());
                                            contentIds.add(entity6.getContentId().toString());

                                            when(reflectionService.invokeMethod(any(), any(),
                                                    eq("else"))).thenReturn(internalResults);
                }
                @Test
                void shouldReturnAResponseWithTheEntity() throws Exception {
                    MvcResult result = mvc.perform(get(
                                                    "/repoWithNoLookupStrategy/searchContent?queryString=else")
                                                    .accept("application/hal+json"))
                                                    .andExpect(status().isOk()).andReturn();

                                            verify(defaultLookupStrategy).lookup(any(RootResourceInformation.class), any(RepositoryInformation.class), any(List.class), any(List.class));
                                            verify(queryMethodsLookupStrategy, never()).lookup(any(RootResourceInformation.class), any(RepositoryInformation.class), any(List.class), any(List.class));

                                            ReadableRepresentation halResponse = representationFactory
                                                    .readRepresentation("application/hal+json",
                                                            new StringReader(result.getResponse()
                                                                    .getContentAsString()));
                                            assertThat(halResponse
                                                    .getResourcesByRel("testEntity2s").size()).isEqualTo(2);
                                            String id1 = halResponse
                                                    .getResourcesByRel("testEntity2s").get(0)
                                                    .getValue("contentId").toString();
                                            String id2 = halResponse
                                                    .getResourcesByRel("testEntity2s").get(1)
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
                                    for (int i=0; i < 500; i++) {
                                        TestEntityWithSeparateId entity = new TestEntityWithSeparateId();
                                        entity = entityWithSeparateRepository.save(entity);
                                        ids.add(entity.getId());
                                    }

                                    List<TestEntityWithSeparateId> entities = new ArrayList<>();

                                    CrudRepository<?,?> repoSpy = mock(CrudRepository.class, AdditionalAnswers.delegatesTo(entityWithSeparateRepository));

                                    ContentSearchRestController.fetchEntitiesInBatches(repoSpy, ids, entities);

                                    verify(repoSpy, times(2)).findAllById(anyCollection());

                                    assertThat(entities.size()).isEqualTo(500);
            }
        }
    }


    @Test
    public void noop() {
    }

    @Configuration
    @EnableJpaRepositories( basePackages="it.rest.extensions.contentsearch",
                            considerNestedRepositories = true)
    @EnableTransactionManagement
    @EnableFileSystemStores(basePackages="it.rest.extensions.contentsearch")
    public static class TestConfig extends JpaInfrastructureConfig {

        @Bean
        FileSystemResourceLoader fileSystemResourceLoader() {
            return new FileSystemResourceLoader(filesystemRoot().getAbsolutePath());
        }

        @Bean
        public File filesystemRoot() {
            File baseDir = new File(System.getProperty("java.io.tmpdir"));
            File filesystemRoot = new File(baseDir,"spring-content-search-controller-tests");
            filesystemRoot.mkdirs();
            return filesystemRoot;
        }

        @Override
        protected String[] packagesToScan() {
            return new String[]{
                    "it.rest.extensions.contentsearch"
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

    @Entity(name = "testentitynocontent")
    public static class TestEntityNoContent extends AbstractTestEntity {
    }

    public interface TestEntityNoContentRepository extends CrudRepository<TestEntityNoContent, String> {
    }

    @Entity(name = "testentitynotsearchable")
    public static class TestEntityNotSearchable extends AbstractTestEntity {
    }

    public interface TestEntityNotSearchableRepository extends CrudRepository<TestEntityNotSearchable, String> {
    }

    public interface TestEntityNotSearchableStore extends FileSystemContentStore<TestEntityNotSearchable, String> {
    }

    @Entity(name = "testentitysharedid")
    public static class TestEntityWithSharedId extends AbstractTestEntity {
    }

    public interface TestEntityWithSharedIdsRepository extends CrudRepository<TestEntityWithSharedId, String> {
    }

    public interface TestEntityWithSharedIdsSearchableStore extends FileSystemContentStore<TestEntityWithSharedId, String>, Searchable<String> {
    }

    // stub out a Searchable implementation so that the content store can be instantiated
    // this wont actually get called because the test intercepts calls to the Searchable by mocking the reflection
    // service
    public static class SearchableImpl implements Searchable<String> {

        @Override
        public Iterable<String> search(String queryString) {
            return null;
        }

        @Override
        public Page<String> search(String queryString, Pageable pageable) {
            return null;
        }
    }

    @Entity(name = "testentityseparateid")
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

    public interface TestEntityWithSeparateIdsRepository
        extends CrudRepository<TestEntityWithSeparateId, String> {

        @Query("select e from it.rest.extensions.contentsearch.ContentSearchRestControllerIT$TestEntityWithSeparateId e")
        List<TestEntityWithSeparateId> randomQueryMethod();

        @FulltextEntityLookupQuery
        List<TestEntityWithSeparateId> findAllByContentIdIn(@Param("contentIds") List<String> contentIds);
    }

    public interface TestEntityWithSeparateIdsSearchableStore
        extends FileSystemContentStore<TestEntityWithSeparateId, String>, Searchable<String> {
    }

    @Entity
    public static class TestEntity2 {
        @Id
        private String id = UUID.randomUUID().toString();
        @ContentId
        private String contentId = UUID.randomUUID().toString();

        public TestEntity2() {
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

    @RepositoryRestResource(path="repoWithNoLookupStrategy")
    public interface RepositoryWithNoLookupStrategy
        extends CrudRepository<TestEntity2, String> {
    }

    public interface TestEntity2SearchableStore
        extends FileSystemContentStore<TestEntity2, String>, Searchable<String> {
    }

    @Entity
    public static class TestEntity3 {
        @Id
        private String id = UUID.randomUUID().toString();
        @ContentId
        private String contentId = UUID.randomUUID().toString();

        public TestEntity3() {
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

    @RepositoryRestResource(path="repoWithCustomSearchReturnType")
    public interface RepositoryWithCustomSearchReturnType
        extends CrudRepository<TestEntity3, String> {
    }

    public interface TestEntity3SearchableStore
        extends FileSystemContentStore<TestEntity3, String>, Searchable<CustomResult> {
    }

    public class CustomResult {

        @ContentId
        private String contentId;

        @Highlight
        private String highlight;

        @Attribute(name="foo")
        private String foo;

        @Attribute(name="bar")
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

        public String getHighlight() {
            return highlight;
        }

        public void setHighlight(String highlight) {
            this.highlight = highlight;
        }

        public String getFoo() {
            return foo;
        }

        public void setFoo(String foo) {
            this.foo = foo;
        }

        public String getBar() {
            return bar;
        }

        public void setBar(String bar) {
            this.bar = bar;
        }
    }
}
