package org.springframework.data.rest.extensions.contentsearch;

import static java.lang.String.format;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

import jakarta.persistence.Id;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.fragments.ParameterTypeAware;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.commons.search.Searchable;
import org.springframework.content.commons.storeservice.StoreFilter;
import org.springframework.content.commons.storeservice.StoreInfo;
import org.springframework.content.commons.storeservice.Stores;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.content.commons.utils.ContentPropertyUtils;
import org.springframework.content.commons.utils.DomainObjectUtils;
import org.springframework.content.commons.utils.ReflectionService;
import org.springframework.content.commons.utils.ReflectionServiceImpl;
import org.springframework.content.rest.FulltextEntityLookupQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.webmvc.PersistentEntityResourceAssembler;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.data.rest.webmvc.RootResourceInformation;
import org.springframework.data.rest.webmvc.support.DefaultedPageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.CollectionModel;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import internal.org.springframework.content.rest.controllers.BadRequestException;
import internal.org.springframework.content.rest.mappings.ContentHandlerMapping.StoreType;
import internal.org.springframework.content.rest.utils.ControllerUtils;
import internal.org.springframework.content.rest.utils.RepositoryUtils;
import internal.org.springframework.data.rest.extensions.contentsearch.DefaultEntityLookupStrategy;
import internal.org.springframework.data.rest.extensions.contentsearch.QueryMethodsEntityLookupStrategy;

@RepositoryRestController
public class ContentSearchRestController {

    private static final String ENTITY_CONTENT_SEARCH_MAPPING = "/{repository}/searchContent";

    private static final Map<String, Method> searchMethods = new HashMap<>();

    private final Repositories repositories;
    private final Stores stores;
    private final PagedResourcesAssembler<Object> pagedResourcesAssembler;
    private DefaultEntityLookupStrategy defaultLookupStrategy;
    private QueryMethodsEntityLookupStrategy qmLookupStrategy;

    private ReflectionService reflectionService;

    static {
        put("search", ReflectionUtils.findMethod(Searchable.class, "search", String.class));
        put("search", ReflectionUtils.findMethod(Searchable.class, "search", String.class, Pageable.class));
    }

    private static void put(String key, Method findMethod) {
        ContentSearchRestController.searchMethods.put(format("%s:%s", key, Arrays.toString(findMethod.getParameterTypes())), findMethod);
    }

    @Autowired
    public ContentSearchRestController(@Autowired(required = false) Repositories repositories, Stores stores, PagedResourcesAssembler<Object> assembler) {

        this.repositories = repositories;
        this.stores = stores;
        this.pagedResourcesAssembler = assembler;

        this.reflectionService = new ReflectionServiceImpl();
        this.defaultLookupStrategy = new DefaultEntityLookupStrategy();
        this.qmLookupStrategy = new QueryMethodsEntityLookupStrategy();
    }

    public void setReflectionService(ReflectionService reflectionService) {
        this.reflectionService = reflectionService;
    }

    public void setDefaultEntityLookupStrategy(DefaultEntityLookupStrategy lookupStrategy) {
        this.defaultLookupStrategy = lookupStrategy;
    }

    public void setQueryMethodsEntityLookupStrategy(QueryMethodsEntityLookupStrategy lookupStrategy) {
        this.qmLookupStrategy = lookupStrategy;
    }

    @StoreType("contentstore")
    @ResponseBody
    @RequestMapping(value = ENTITY_CONTENT_SEARCH_MAPPING, method = RequestMethod.GET)
    public CollectionModel<?> searchContent(
            RootResourceInformation repoInfo,
            DefaultedPageable pageable,
            PersistentEntityResourceAssembler assembler,
            @PathVariable("repository") String repository,
            @RequestParam(name = "queryString") String queryString) {

        return searchContentInternal(repoInfo, repository, pageable, assembler, "search", new String[]{queryString});
    }

    private CollectionModel<?> searchContentInternal(RootResourceInformation repoInfo, String repository, DefaultedPageable pageable, PersistentEntityResourceAssembler assembler, String searchMethod, String[] keywords) {

        if (repositories == null) {
            throw new ResourceNotFoundException("Entity has no content associations");
        }
        StoreInfo[] infos = stores.getStores(ContentStore.class, new StoreFilter() {
            @Override
            public String name() {
                return "test";
            }

            @Override
            public boolean matches(StoreInfo info) {
                return repoInfo.getDomainType().equals(info.getDomainObjectClass());
            }
        });

        if (infos.length == 0) {
            throw new ResourceNotFoundException("Entity has no content associations");
        }

        if (infos.length > 1) {
            throw new IllegalStateException(String.format("Too many content association for Entity %s", repoInfo.getDomainType().getCanonicalName()));
        }

        StoreInfo info = infos[0];

        ContentStore<Object, Serializable> store = info.getImplementation(ContentStore.class);
        if (!(store instanceof Searchable)) {
            throw new ResourceNotFoundException("Entity content is not searchable");
        }

        Class<?>[] searchMethodArgTypes = (pageable.unpagedIfDefault().isUnpaged() ? new Class<?>[]{String.class} : new Class<?>[]{String.class, Pageable.class});
        Method method = searchMethods.get(format("%s:%s", searchMethod, Arrays.toString(searchMethodArgTypes)));

        if (method == null) {
            throw new BadRequestException(String.format("Invalid search: %s", searchMethod));
        }

        if (keywords == null || keywords.length == 0) {
            throw new BadRequestException();
        }

        Class<?> returnType = returnType(info);
        if (ContentPropertyUtils.isPrimitiveContentPropertyClass(returnType)) {

            Method parameterTypeAwareMethod = ReflectionUtils.findMethod(ParameterTypeAware.class, "setGenericArguments", Class[].class);
            if (parameterTypeAwareMethod != null) {
                reflectionService.invokeMethod(parameterTypeAwareMethod, store, new Object[]{new Class[]{InternalResult.class}});
            }

            returnType = InternalResult.class;
        }

        Object[] argValues = (pageable.unpagedIfDefault().isUnpaged()) ? new String[]{keywords[0]} : new Object[]{keywords[0], pageable.getPageable()};
        Iterable<?> intermediateResults = (Iterable<?>) reflectionService.invokeMethod(method, store, argValues);

        if (intermediateResults == null || !intermediateResults.iterator().hasNext()) {
            return CollectionModel.empty();
        }

        final List<Object> results = new ArrayList<>();

        RepositoryInformation ri = RepositoryUtils.findRepositoryInformation(repositories, repository);
        Class<?> domainClass = ri.getDomainType();

        if (returnType.equals(InternalResult.class)) {

            boolean idFieldEqualsContentIdField = isIdFieldOverloaded(repoInfo.getDomainType());

            List<Object> entityIds = new ArrayList<>();
            List<Object> contentIds = new ArrayList<>();

            for (Object tempResult : intermediateResults) {
                InternalResult internalResult = (InternalResult) tempResult;
                if (internalResult.getId() != null) {
                    entityIds.add(internalResult.getId());
                } else if (idFieldEqualsContentIdField) {
                    entityIds.add(internalResult.getContentId());
                } else if (internalResult.getContentId() != null) {
                    contentIds.add(internalResult.getContentId());
                }
            }

            if (!entityIds.isEmpty() && repositories != null) {
                repositories.getRepositoryFor(domainClass)
                        .ifPresent(r -> fetchEntitiesInBatches((CrudRepository<?, ?>) r, entityIds, results));
            }

            if (!contentIds.isEmpty()) {
                if (ri.getQueryMethods()
                        .stream().noneMatch(m -> m.getAnnotation(FulltextEntityLookupQuery.class) != null)) {

                    defaultLookupStrategy.lookup(repoInfo, ri, contentIds, results);
                } else {

                    qmLookupStrategy.lookup(repoInfo, ri, contentIds, results);
                }
            }

            Iterable<?> wrappedResults = convertToFinalResultType(results, pageable, intermediateResults);
            return ControllerUtils.toCollectionModel(wrappedResults, pagedResourcesAssembler, assembler, domainClass);
        } else {
            intermediateResults.forEach(results::add);
            Iterable<?> wrappedResults = convertToFinalResultType(results, pageable, intermediateResults);
            return ControllerUtils.toCollectionModel(wrappedResults, pagedResourcesAssembler, null, results.get(0).getClass());
        }
    }

    private Iterable<?> convertToFinalResultType(List<Object> results, DefaultedPageable pageable, Iterable intermediateResults) {

        if (pageable.unpagedIfDefault().isUnpaged()) {
            return results;
        } else if (intermediateResults instanceof Page) {
            return new PageImpl<>(results, pageable.getPageable(), ((Page) intermediateResults).getTotalPages());
        } else {
            return new PageImpl<>(results);
        }
    }

    public static void fetchEntitiesInBatches(CrudRepository<?, ?> r, List entityIds, List results) {

        int size = entityIds.size();

        Method findAllByIdMethod = ReflectionUtils.findMethod(CrudRepository.class, "findAllById", Iterable.class);

        for (int i = 0; i < size; ) {
            int lowerBound = i;
            int upperBound = (lowerBound + 250 <= entityIds.size() ? lowerBound + 250 : size);

            List<Object> subset = entityIds.subList(lowerBound, upperBound);

            Iterable<?> entities = (Iterable<?>) ReflectionUtils.invokeMethod(findAllByIdMethod, r, subset);
            for (Object entity : entities) {
                results.add(entity);
            }

            i += 250;
        }
    }

    private Class<?> returnType(StoreInfo info) {
        Class<?> storeInterfaceClass = info.getInterface();
        Class<?> searchReturnType = String.class;
        for (Type t : storeInterfaceClass.getGenericInterfaces()) {
            if (t.getTypeName().startsWith("org.springframework.content.commons.search")) {
                if (t instanceof ParameterizedType) {
                    Type[] fragmentGenericTypes = ((ParameterizedType) t).getActualTypeArguments();
                    searchReturnType = (Class<?>) fragmentGenericTypes[0];
                }
            }
        }
        return searchReturnType;
    }

    private boolean isIdFieldOverloaded(Class<?> domainClass) {

        Field idField = DomainObjectUtils.getIdField(domainClass);
        Field contentIdField = BeanUtils.findFieldWithAnnotation(domainClass, ContentId.class);

        return Objects.equals(idField, contentIdField);
    }

    public static class InternalResult {

        @Id
        private Object id;

        @ContentId
        private Object contentId;

        public InternalResult() {
        }

        public InternalResult(Object id, Object contentId) {
            this.id = id;
            this.contentId = contentId;
        }

        public Object getId() {
            return id;
        }

        public void setId(Object id) {
            this.id = id;
        }

        public Object getContentId() {
            return contentId;
        }

        public void setContentId(Object contentId) {
            this.contentId = contentId;
        }
    }
}
