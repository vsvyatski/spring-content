package internal.org.springframework.content.solr;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.common.util.ContentStreamBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.search.IndexService;
import org.springframework.content.commons.store.StoreAccessException;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.content.commons.utils.DomainObjectUtils;
import org.springframework.content.solr.AttributeProvider;
import org.springframework.content.solr.SolrProperties;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import static java.lang.String.format;
import static org.apache.solr.client.solrj.request.AbstractUpdateRequest.ACTION.COMMIT;

public class SolrFulltextIndexServiceImpl implements IndexService<Object> {

    public static final String ENTITY_ID = "entity_id";

    /**
     * Catch-all Solr text field for schemas that use {@code text} instead of {@code _text_}.
     */
    public static final String FULLTEXT_FIELD = "text";

    private final SolrClient solrClient;
    private final SolrProperties properties;
    private final AttributeProvider<Object> builtinSyncer;
    private AttributeProvider<Object> syncer;

    @Autowired
    public SolrFulltextIndexServiceImpl(SolrClient solrClient, SolrProperties properties) {
        this.solrClient = solrClient;
        this.properties = properties;
        builtinSyncer = entity -> {
            Map<String, String> attributes = new HashMap<>();
            Object entityId = DomainObjectUtils.getId(entity);
            if (entityId != null) {
                attributes.put(ENTITY_ID, entityId.toString());
            }
            return attributes;
        };
    }

    @Autowired(required = false)
    public void setAttributeSyncer(AttributeProvider<Object> syncer) {
        this.syncer = syncer;
    }

    @Override
    public void index(Object entity, InputStream content) {
        ContentStreamUpdateRequest up = new ContentStreamUpdateRequest("/update/extract");
        if (properties.getUser() != null) {
            up.setBasicAuthCredentials(properties.getUser(), properties.getPassword());
        }

        ContentEntityStream entityStream = new ContentEntityStream(content);
        entityStream.setContentType(resolveMimeType(entity));
        entityStream.setName("file");
        up.addContentStream(entityStream);
        String id = contentIdString(entity);
        up.setParam("literal.id", entity.getClass().getCanonicalName() + ":" + id);
        up.setParam("defaultField", FULLTEXT_FIELD);
        up.setParam("fmap.content", FULLTEXT_FIELD);
        up.setParam("literalsOverride", "true");

        Map<String, String> attributesToSync = builtinSyncer.synchronize(entity);
        if (syncer != null) {
            attributesToSync.putAll(syncer.synchronize(entity));
        }

        for (Entry<String, String> entry : attributesToSync.entrySet()) {
            up.setParam(format("literal.%s", entry.getKey()), entry.getValue());
        }

        up.setAction(COMMIT, true, true);

        try {
            solrClient.request(up, null);
        } catch (SolrServerException | IOException e) {
            throw new StoreAccessException(format("Error indexing entity with id '%s'", id), e);
        }
    }

    @Override
    public void unindex(Object entity) {
        String id = contentIdString(entity);

        UpdateRequest up = new UpdateRequest();
        up.setAction(COMMIT, true, true);
        up.deleteById(entity.getClass().getCanonicalName() + ":" + id);

        if (properties.getUser() != null) {
            up.setBasicAuthCredentials(properties.getUser(), properties.getPassword());
        }

        try {
            solrClient.request(up, null);
        } catch (SolrServerException | IOException e) {
            throw new StoreAccessException(format("Error unindexing entity with id '%s'", id), e);
        }
    }

    private static String contentIdString(Object entity) {
        return Objects.requireNonNull(
            BeanUtils.getAnnotatedValue(entity, ContentId.class), "contentId").toString();
    }

    private static String resolveMimeType(Object entity) {
        Object mimeType = BeanUtils.getAnnotatedValue(entity, MimeType.class);
        if (mimeType instanceof String mime && !mime.isBlank()) {
            return mime;
        }
        return ContentStreamBase.APPLICATION_OCTET_STREAM;
    }

    private static class ContentEntityStream extends ContentStreamBase {

        private final InputStream stream;

        public ContentEntityStream(InputStream stream) {
            this.stream = stream;
        }

        @Override
        public InputStream getStream() {
            return stream;
        }
    }
}
