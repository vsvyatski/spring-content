package internal.org.springframework.content.fragments;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.search.HighlightField;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.util.NamedValue;
import internal.org.springframework.content.elasticsearch.IndexManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.fragments.ContentStoreAware;
import org.springframework.content.commons.fulltext.Attribute;
import org.springframework.content.commons.fulltext.Highlight;
import org.springframework.content.commons.search.Searchable;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.commons.store.StoreAccessException;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.content.commons.utils.ContentPropertyUtils;
import org.springframework.content.elasticsearch.FilterQueryProvider;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.util.Assert;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static java.lang.String.format;

/**
 * {@link Searchable} store fragment for Elasticsearch. Wired through {@link ContentStoreAware}; not a {@code @NullMarked}
 * {@link ContentStore} wrapper.
 *
 * @author Vladimir Svyatski
 */
public class SearchableImpl implements Searchable<Object>, ContentStoreAware {

    private static final Log LOGGER = LogFactory.getLog(SearchableImpl.class);

    private final ElasticsearchClient client;
    private final IndexManager manager;
    private FilterQueryProvider filterProvider;
    private final ConversionService conversionService;

    private Class<?> domainClass;
    private Class<?> idClass;
    private Class<?>[] genericArguments;

    @Autowired
    public SearchableImpl(ElasticsearchClient client, IndexManager manager) {
        Assert.notNull(client, "client must be specified");
        Assert.notNull(manager, "manager must be specified");
        this.client = client;
        this.manager = manager;
        this.conversionService = new DefaultConversionService();
    }

    @Autowired(required = false)
    public void setFilterQueryProvider(FilterQueryProvider provider) {
        this.filterProvider = provider;
    }

    @Override
    public void setDomainClass(Class<?> domainClass) {
        this.domainClass = domainClass;
    }

    @Override
    public void setIdClass(Class<?> idClass) {
        this.idClass = idClass;
    }

    /**
     * Invoked by {@link org.springframework.content.commons.config.AbstractStoreBeanDefinitionRegistrar}.
     */
    @SuppressWarnings("unused")
    public void setGenericArguments(Class<?>[] genericArguments) {
        this.genericArguments = genericArguments;
    }

    @Override
    public void setContentStore(ContentStore<Object, Serializable> store) {
    }

    @Override
    public List<Object> search(String queryStr) {
        Class<?> searchType = requireSearchResultType();
        return materializeHits(executeSearch(queryStr, searchType), searchType);
    }

    @Override
    public Page<Object> search(String queryStr, Pageable pageable) {
        Assert.notNull(pageable, "pageable must not be null");
        Class<?> searchType = requireSearchResultType();
        HitsMetadata<Map<String, Object>> hits = executeSearch(queryStr, pageable, searchType);
        List<Object> content = materializeHits(hits, searchType);
        long total = hits == null || hits.total() == null ? 0 : hits.total().value();
        return new PageImpl<>(content, pageable, total);
    }

    private Class<?> requireSearchResultType() {
        Assert.notNull(domainClass, "domainClass must be configured on the Searchable fragment");
        Assert.notNull(genericArguments, "genericArguments must be configured on the Searchable fragment");
        Assert.isTrue(genericArguments.length > 0, "genericArguments must not be empty");
        return genericArguments[0];
    }

    private HitsMetadata<Map<String, Object>> executeSearch(String queryString, Class<?> searchType) {
        return runQuery(queryString, searchType, pagination -> {
        });
    }

    private HitsMetadata<Map<String, Object>> executeSearch(String queryString, Pageable pageable, Class<?> searchType) {
        return runQuery(queryString, searchType, s -> {
            s.from(pageable.getPageNumber() * pageable.getPageSize());
            s.size(pageable.getPageSize());
        });
    }

    /**
     * Elasticsearch's typed client still documents hits as {@link Map} at runtime ({@code Map.class}).
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private HitsMetadata<Map<String, Object>> runQuery(String queryString, Class<?> searchType,
                                                       Consumer<SearchRequest.Builder> pagination) {

        List<String> attributesToFetch = new ArrayList<>();
        if (!ContentPropertyUtils.isPrimitiveContentPropertyClass(searchType)) {
            for (Field field : BeanUtils.findFieldsWithAnnotation(searchType, Attribute.class, new BeanWrapperImpl(searchType))) {
                Attribute fieldAnnotation = field.getAnnotation(Attribute.class);
                attributesToFetch.add(fieldAnnotation.name());
            }
        }
        boolean highlight = !ContentPropertyUtils.isPrimitiveContentPropertyClass(searchType)
            && BeanUtils.findFieldWithAnnotation(searchType, Highlight.class) != null;

        try {
            var res = client.search(s -> {
                s.index(manager.indexName(domainClass));
                if (!attributesToFetch.isEmpty()) {
                    s.source(src -> src.filter(f -> f.includes(attributesToFetch)));
                }
                s.query(q -> q.bool(b -> {
                    b.must(m -> m.simpleQueryString(sq -> sq.query(queryString).fields("attachment.content")));
                    b.filter(f -> f.matchPhrase(mp -> mp.field("entityClass").query(domainClass.getName())));
                    if (filterProvider != null) {
                        Map<String, Object> filters = filterProvider.filterQueries(domainClass);
                        for (Map.Entry<String, Object> entry : filters.entrySet()) {
                            String attr = entry.getKey();
                            String value = String.valueOf(entry.getValue());
                            b.filter(f -> f.match(mq -> mq.field(attr).query(FieldValue.of(value))));
                        }
                    }
                    return b;
                }));
                pagination.accept(s);
                if (highlight) {
                    s.highlight(h -> h.fields(NamedValue.of("attachment.content", HighlightField.of(hf -> hf))));
                }
                return s;
            }, Map.class);
            return (HitsMetadata) res.hits();
        } catch (IOException | ElasticsearchException e) {
            LOGGER.error(format("Error searching indexed content for '%s'", queryString), e);
            throw new StoreAccessException(format("Error searching indexed content for '%s'", queryString), e);
        }
    }

    private List<Object> materializeHits(HitsMetadata<Map<String, Object>> result, Class<?> resultType) {

        List<Object> contents = new ArrayList<>();
        long total = result == null || result.total() == null ? 0 : result.total().value();

        if (result == null || total == 0) {
            return contents;
        }

        for (Hit<Map<String, Object>> hit : result.hits()) {

            try {
                if (ContentPropertyUtils.isPrimitiveContentPropertyClass(resultType)) {
                    contents.add(conversionService.convert(hit.id(), TypeDescriptor.valueOf(String.class),
                        TypeDescriptor.valueOf(this.idClass)));
                } else {
                    Object row = resultType.getDeclaredConstructor().newInstance();
                    BeanWrapper wrapper = new BeanWrapperImpl(row);

                    Field contentIdField = BeanUtils.findFieldWithAnnotation(resultType, ContentId.class);
                    if (contentIdField != null) {
                        wrapper.setPropertyValue(contentIdField.getName(), hit.id());
                    }

                    Field highlightField = BeanUtils.findFieldWithAnnotation(resultType, Highlight.class);
                    if (highlightField != null && hit.highlight() != null) {
                        List<String> snippets = hit.highlight().get("attachment.content");
                        if (snippets != null && !snippets.isEmpty()) {
                            wrapper.setPropertyValue(highlightField.getName(), snippets.get(0));
                        }
                    }

                    Map<String, Object> source = hit.source();
                    if (source != null) {
                        for (Field field : BeanUtils.findFieldsWithAnnotation(resultType, Attribute.class,
                            new BeanWrapperImpl(resultType))) {
                            Attribute fieldAnnotation = field.getAnnotation(Attribute.class);
                            wrapper.setPropertyValue(field.getName(), source.get(fieldAnnotation.name()));
                        }
                    }

                    contents.add(row);
                }
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException
                     | java.lang.reflect.InvocationTargetException e) {
                LOGGER.error("Error materializing search hit", e);
            }
        }

        return contents;
    }
}
