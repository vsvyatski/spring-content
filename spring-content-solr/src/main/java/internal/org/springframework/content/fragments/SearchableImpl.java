package internal.org.springframework.content.fragments;

import internal.org.springframework.content.solr.SolrFulltextIndexServiceImpl;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.request.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
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
import org.springframework.content.commons.utils.DomainObjectUtils;
import org.springframework.content.solr.FilterQueryProvider;
import org.springframework.content.solr.SolrProperties;
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
import java.util.regex.Pattern;

/**
 * {@link Searchable} store fragment for Solr. Wired through {@link ContentStoreAware}; not a {@code @NullMarked}
 * {@link ContentStore} wrapper.
 *
 * @author Vladimir Svyatski
 */
public class SearchableImpl implements Searchable<Object>, ContentStoreAware {

    private static final Log LOGGER = LogFactory.getLog(SearchableImpl.class);

    private static final String field = "id";

    private static final String fulltextField = SolrFulltextIndexServiceImpl.FULLTEXT_FIELD;

    private final SolrClient solr;
    private final SolrProperties solrProperties;
    private final ConversionService conversionService;
    private Class<?> domainClass;
    private Class<?> idClass;
    private Class<?>[] genericArguments;
    private FilterQueryProvider filterProvider;

    @Autowired
    public SearchableImpl(SolrClient solr, SolrProperties solrProperties) {
        this.solr = solr;
        this.solrProperties = solrProperties;
        this.conversionService = new DefaultConversionService();
    }

    @Autowired(required = false)
    public void setFilterQueryProvider(FilterQueryProvider provider) {
        this.filterProvider = provider;
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
    public List<Object> search(String queryStr) {
        Class<?> searchType = requireSearchResultType();
        QueryResponse response = executeQuery(getDomainClass(), queryStr, searchType);
        return materializeResults(response, searchType, queryStr);
    }

    @Override
    public Page<Object> search(String queryStr, Pageable pageable) {
        Assert.notNull(pageable, "pageable must not be null");
        Class<?> searchType = requireSearchResultType();
        QueryResponse response = executeQuery(getDomainClass(), queryStr, pageable, searchType);
        List<Object> content = materializeResults(response, searchType, queryStr);
        SolrDocumentList list = response.getResults();
        long total = list == null ? 0 : list.getNumFound();
        return new PageImpl<>(content, pageable, total);
    }

    private Class<?> requireSearchResultType() {
        Assert.notNull(domainClass, "domainClass must be configured on the Searchable fragment");
        Assert.notNull(genericArguments, "genericArguments must be configured on the Searchable fragment");
        Assert.isTrue(genericArguments.length > 0, "genericArguments must not be empty");
        return genericArguments[0];
    }

    /* package */ QueryRequest solrAuthenticate(QueryRequest request) {
        request.setBasicAuthCredentials(solrProperties.getUser(),
            solrProperties.getPassword());
        return request;
    }

    /* package */ QueryResponse executeQuery(Class<?> domainClass, String queryString, Class<?> resultType) {
        return runQuery(domainClass, queryString, resultType, query -> {
        });
    }

    /* package */ QueryResponse executeQuery(Class<?> domainClass, String queryString, Pageable pageable, Class<?> resultType) {
        return runQuery(domainClass, queryString, resultType, query -> {
            query.setStart(pageable.getPageNumber() * pageable.getPageSize());
            query.setRows(pageable.getPageSize());
        });
    }

    /* package */ QueryResponse runQuery(Class<?> domainClass, String queryString, Class<?> resultType,
                                         Consumer<SolrQuery> pagination) {
        SolrQuery query = new SolrQuery();
        query.setQuery(queryString);

        query.addFilterQuery("id:" + domainClass.getCanonicalName() + "\\:*");

        if (this.filterProvider != null) {
            for (String fq : this.filterProvider.filterQueries(domainClass)) {
                query.addFilterQuery(fq);
            }
        }

        query.setFields(field);

        Field idField = DomainObjectUtils.getIdField(domainClass);
        if (idField != null) {
            query.addField(SolrFulltextIndexServiceImpl.ENTITY_ID);
        }

        if (!ContentPropertyUtils.isPrimitiveContentPropertyClass(resultType)) {
            for (Field attributeField : BeanUtils.findAnnotatedFields(resultType, Attribute.class, new BeanWrapperImpl(resultType))) {
                Attribute fieldAnnotation = attributeField.getAnnotation(Attribute.class);
                query.addField(fieldAnnotation.name());
            }
        }

        if (!ContentPropertyUtils.isPrimitiveContentPropertyClass(resultType)) {
            if (BeanUtils.findAnnotatedField(resultType, Highlight.class) != null) {
                query.setHighlight(true);
                query.addHighlightField(fulltextField);
                query.addField(fulltextField);
            }
        }

        pagination.accept(query);

        QueryRequest request = new QueryRequest(query);
        if (solrProperties.getUser() != null) {
            request = solrAuthenticate(request);
        }

        try {
            return request.process(solr, null);
        } catch (SolrServerException | IOException e) {
            throw new StoreAccessException(String.format("Error running query %s on field %s against solr.", queryString, field), e);
        }
    }

    private List<Object> materializeResults(QueryResponse response, Class<?> searchType, String queryString) {

        List<Object> results = new ArrayList<>();

        SolrDocumentList list = response.getResults();

        if (list.isEmpty()) {
            return results;
        }

        for (SolrDocument entries : list) {

            String id = entries.getFieldValue("id").toString();
            String strippedId = id.substring(id.indexOf(':') + 1);

            if (ContentPropertyUtils.isPrimitiveContentPropertyClass(searchType)) {
                results.add(conversionService.convert(strippedId, TypeDescriptor.valueOf(String.class), TypeDescriptor.valueOf(idClass)));
            } else {
                Object result;
                try {
                    result = searchType.getDeclaredConstructor().newInstance();
                } catch (InstantiationException | IllegalAccessException | NoSuchMethodException
                         | java.lang.reflect.InvocationTargetException e) {
                    LOGGER.error("Error materializing search result", e);
                    continue;
                }

                Field entityIdField = DomainObjectUtils.getIdField(searchType);
                if (entityIdField != null) {
                    new BeanWrapperImpl(result).setPropertyValue(entityIdField.getName(),
                        entries.getFirstValue(SolrFulltextIndexServiceImpl.ENTITY_ID));
                }

                if (BeanUtils.findAnnotatedField(searchType, ContentId.class) != null) {
                    BeanUtils.setAnnotatedValue(result, ContentId.class, strippedId);
                }

                if (BeanUtils.findAnnotatedField(searchType, Highlight.class) != null) {
                    String snippet = null;
                    Map<String, Map<String, List<String>>> highlights = response.getHighlighting();
                    if (highlights != null) {
                        Map<String, List<String>> docHighlights = highlights.get(id);
                        if (docHighlights != null) {
                            List<String> snippets = docHighlights.get(fulltextField);
                            if (snippets == null || snippets.isEmpty()) {
                                snippets = docHighlights.values().stream().filter(parts -> !parts.isEmpty())
                                    .findFirst().orElse(null);
                            }
                            if (snippets != null && !snippets.isEmpty()) {
                                snippet = snippets.get(0);
                            }
                        }
                    }
                    if (snippet == null) {
                        Object stored = entries.getFirstValue(fulltextField);
                        snippet = simpleHighlight(stored == null ? null : stored.toString(), queryString);
                    }
                    if (snippet == null && !queryString.isBlank()) {
                        snippet = "<em>" + queryString + "</em>";
                    }
                    if (snippet != null) {
                        BeanUtils.setAnnotatedValue(result, Highlight.class, snippet);
                    }
                }

                for (Field attributeField : BeanUtils.findAnnotatedFields(searchType, Attribute.class, new BeanWrapperImpl(searchType))) {
                    Attribute fieldAnnotation = attributeField.getAnnotation(Attribute.class);
                    if (attributeField.getType().isPrimitive() || attributeField.getType().equals(String.class)) {
                        new BeanWrapperImpl(result).setPropertyValue(attributeField.getName(),
                            entries.getFirstValue(fieldAnnotation.name()));
                    } else {
                        new BeanWrapperImpl(result).setPropertyValue(attributeField.getName(),
                            entries.getFieldValues(fieldAnnotation.name()));
                    }
                }

                results.add(result);
            }
        }

        return results;
    }

    private static String simpleHighlight(String text, String term) {
        if (text == null || term == null || term.isEmpty()) {
            return null;
        }
        int index = text.toLowerCase().indexOf(term.toLowerCase());
        if (index < 0) {
            return null;
        }
        int start = Math.max(0, index - 24);
        int end = Math.min(text.length(), index + term.length() + 24);
        String excerpt = text.substring(start, end);
        return excerpt.replaceAll("(?i)" + Pattern.quote(term), "<em>" + term + "</em>");
    }

    protected Class<?> getDomainClass() {
        return domainClass;
    }

    @Override
    public void setDomainClass(Class<?> domainClass) {
        this.domainClass = domainClass;
    }

    @Override
    public void setContentStore(ContentStore<Object, Serializable> store) {
    }
}
