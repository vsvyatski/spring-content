package internal.org.springframework.content.fragments;

import static java.lang.String.format;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.fulltext.Attribute;
import org.springframework.content.commons.fulltext.Highlight;
import org.springframework.content.commons.search.Searchable;
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

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch.core.search.HighlightField;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.util.NamedValue;
import internal.org.springframework.content.elasticsearch.IndexManager;

public class SearchableImpl implements Searchable<Object> {

    private static final Log LOGGER = LogFactory.getLog(SearchableImpl.class);

    private final ElasticsearchClient client;
    private final IndexManager manager;
    private FilterQueryProvider filterProvider;
    private ConversionService conversionService;

    private Class<?> domainClass;
    private Class<?> idClass;
    private Class<?>[] genericArguments;

    public SearchableImpl() {
        client = null;
        manager = null;
        domainClass = null;
        idClass = null;
        filterProvider = null;
    }

    @Autowired
    public SearchableImpl(ElasticsearchClient client, IndexManager manager) {
        this.client = client;
        this.manager = manager;
        this.filterProvider = null;
        this.conversionService = new DefaultConversionService();
    }

    @Autowired(required=false)
    public void setFilterQueryProvider(FilterQueryProvider provider) {
        this.filterProvider = provider;
    }

    public void setDomainClass(Class<?> domainClass) {
        this.domainClass = domainClass;
    }

    public void setIdClass(Class<?> idClass) {
        this.idClass = idClass;
    }

    public void setGenericArguments(Class<?>[] genericArguments) {
        this.genericArguments = genericArguments;
    }

    @Override
    public Iterable<Object> search(String queryStr) {
        return search(queryStr, null, genericArguments[0], ArrayList.class);
    }

    @Override
    public Page<Object> search(String queryStr, Pageable pageable) {
        return search(queryStr, pageable, genericArguments[0], Page.class);
    }

    @SuppressWarnings("unchecked")
    private <R> R search(String queryString, Pageable pageable, Class<? extends Object> searchType, Class<R> returnType) {

        List<String> attributesToFetch = new ArrayList<>();
        if (!ContentPropertyUtils.isPrimitiveContentPropertyClass(searchType)) {
            for (java.lang.reflect.Field field : BeanUtils.findFieldsWithAnnotation(searchType, Attribute.class, new BeanWrapperImpl(searchType))) {
                Attribute fieldAnnotation = field.getAnnotation(Attribute.class);
                attributesToFetch.add(fieldAnnotation.name());
            }
        }
        boolean highlight = !ContentPropertyUtils.isPrimitiveContentPropertyClass(searchType)
                && BeanUtils.findFieldWithAnnotation(searchType, Highlight.class) != null;

        try {
            var res = client.search(s -> {
                s.index(manager.indexName(domainClass));
                if (attributesToFetch.size() > 0) {
                    s.source(src -> src.filter(f -> f.includes(attributesToFetch)));
                }
                s.query(q -> q.bool(b -> {
                    b.must(m -> m.simpleQueryString(sq -> sq.query(queryString).fields("attachment.content")));
                    b.filter(f -> f.matchPhrase(mp -> mp.field("entityClass").query(domainClass.getName())));
                    if (filterProvider != null) {
                        Map<String, Object> filters = filterProvider.filterQueries(domainClass);
                        for (String attr : filters.keySet()) {
                            String value = String.valueOf(filters.get(attr));
                            b.filter(f -> f.match(mq -> mq.field(attr).query(FieldValue.of(value))));
                        }
                    }
                    return b;
                }));
                if (pageable != null) {
                    s.from(pageable.getPageNumber() * pageable.getPageSize());
                    s.size(pageable.getPageSize());
                }
                if (highlight) {
                    s.highlight(h -> h.fields(NamedValue.of("attachment.content", HighlightField.of(hf -> hf))));
                }
                return s;
            }, Map.class);
            return getResults(res.hits(), pageable, searchType, returnType);
        }
        catch (IOException | ElasticsearchException e) {
            LOGGER.error(format("Error searching indexed content for '%s'", queryString), e);
            throw new StoreAccessException(format("Error searching indexed content for '%s'", queryString), e);
        }
    }

    private <R> R getResults(HitsMetadata<Map> result, Pageable pageable, Class<?> resultType, Class<R> returnType) {

        List<Object> contents = new ArrayList<>();
        long total = result == null || result.total() == null ? 0 : result.total().value();

        if (result == null || total == 0) {
            return wrapResult(returnType, contents, pageable, 0);
        }

        for (Hit<Map> hit : result.hits()) {

            try {
                if (ContentPropertyUtils.isPrimitiveContentPropertyClass(resultType)) {
                    contents.add(conversionService.convert(hit.id(), TypeDescriptor.valueOf(String.class), TypeDescriptor.valueOf(this.idClass)));
                } else {
                    Object row = resultType.newInstance();
                    BeanWrapper wrapper = new BeanWrapperImpl(row);

                    Field contentIdField = BeanUtils.findFieldWithAnnotation(resultType, ContentId.class);
                    if (contentIdField != null) {
                        wrapper.setPropertyValue(contentIdField.getName(), hit.id());
                    }

                    Field highlightField = BeanUtils.findFieldWithAnnotation(resultType, Highlight.class);
                    if (highlightField != null) {
                        wrapper.setPropertyValue(highlightField.getName(), hit.highlight().get("attachment.content").get(0));
                    }

                    for (java.lang.reflect.Field field : BeanUtils.findFieldsWithAnnotation(resultType, Attribute.class, new BeanWrapperImpl(resultType))) {
                        Attribute fieldAnnotation = field.getAnnotation(Attribute.class);
                        wrapper.setPropertyValue(field.getName(), hit.source().get(fieldAnnotation.name()));
                    }

                    contents.add(row);
                }
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        return wrapResult(returnType, contents, pageable, total);
    }

    @SuppressWarnings("unchecked")
    private <R> R wrapResult(Class<R> returnType, List<Object> content, Pageable pageable, long total) {

        R rc = null;
        if (Page.class.isAssignableFrom(returnType)) {
            LOGGER.debug("Wrapping result in Page");
            rc = (R) new PageImpl<Object>(content, pageable, total);
        } else {
            LOGGER.debug("Returning result as-is");
            rc = (R) content;
        }
        return rc;
    }
}
