package internal.org.springframework.content.elasticsearch;

import static java.lang.String.format;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.renditions.RenditionService;
import org.springframework.content.commons.search.IndexService;
import org.springframework.content.commons.store.StoreAccessException;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.content.elasticsearch.AttributeProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;

/**
 * @author Vladimir Svyatski
 */
@Service
public class ElasticsearchIndexServiceImpl<T> implements IndexService<T> {

    // if original index exists, use it, otherwise use class-based index

    private static final Log LOGGER = LogFactory.getLog(ElasticsearchIndexServiceImpl.class);
    private static final String SPRING_CONTENT_ATTACHMENT = "spring-content-attachment-pipeline";
    private static final int BUFFER_SIZE = 3 * 1024;

    private final ElasticsearchClient client;
    private final RenditionService renditionService;
    private final IndexManager manager;
    private final AttributeProvider attributeProvider;

    private boolean pipelinedInitialized = false;

    public ElasticsearchIndexServiceImpl(ElasticsearchClient client, RenditionService renditionService, IndexManager manager, AttributeProvider attributeProvider) {

        this.client = client;
        this.renditionService = renditionService;
        this.manager = manager;
        this.attributeProvider = attributeProvider;
    }

    @Override
    public void index(T entity, InputStream stream) {

        if (!pipelinedInitialized) {
            try {
                ensureAttachmentPipeline();
            } catch (IOException ioe) {
                throw new StoreAccessException("Unable to initialize attachment pipeline", ioe);
            }
        }

        String id = BeanUtils.getFieldWithAnnotation(entity, ContentId.class).toString();

        if (renditionService != null) {
            Object mimeType = BeanUtils.getFieldWithAnnotation(entity, MimeType.class);
            if (mimeType != null) {
                String strMimeType = mimeType.toString();
                if (renditionService.canConvert(strMimeType, "text/plain")) {
                    stream = renditionService.convert(strMimeType, stream, "text/plain");
                }
            }
        }

        StringBuilder result = new StringBuilder();
        try {
            try (BufferedInputStream in = new BufferedInputStream(stream, BUFFER_SIZE)) {
                Base64.Encoder encoder = Base64.getEncoder();
                byte[] chunk = new byte[BUFFER_SIZE];
                int len = 0;
                while ( (len = in.read(chunk)) == BUFFER_SIZE ) {
                    result.append( encoder.encodeToString(chunk) );
                }
                if ( len > 0 ) {
                    chunk = Arrays.copyOf(chunk,len);
                    result.append( encoder.encodeToString(chunk) );
                }
            }
        }
        catch (IOException e) {
            throw new StoreAccessException(format("Error base64 encoding stream for content %s", id), e);
        }

        Map<String, String> attributesToSync = attributeProvider == null ? new HashMap<>() : attributeProvider.synchronize(entity);
        attributesToSync.put("data", result.toString());
        // mapping types are gone; this field is what searches filter on
        attributesToSync.put("entityClass", entity.getClass().getName());

        try {
            var res = client.index(i -> i
                    .index(manager.indexName(entity.getClass()))
                    .id(id)
                    .pipeline(SPRING_CONTENT_ATTACHMENT)
                    .document(attributesToSync));
            LOGGER.info(format("Content '%s' indexed with result %s", id, res.result()));
        }
        catch (IOException e) {
            throw new StoreAccessException(format("Error indexing content %s", id), e);
        }
    }

    @Override
    public void unindex(T entity) {

        if (!pipelinedInitialized) {
            try {
                ensureAttachmentPipeline();
            } catch (IOException ioe) {
                throw new StoreAccessException("Unable to initialize attachment pipeline", ioe);
            }
        }

        Object id = BeanUtils.getFieldWithAnnotation(entity, ContentId.class);
        if (id == null) {
            return;
        }

        try {
            var res = client.delete(d -> d.index(manager.indexName(entity.getClass())).id(id.toString()));
            LOGGER.info(format("Indexed content '%s' deleted with result %s", id, res.result()));
        }
        catch (ElasticsearchException ese) {
            if (ese.status() != 404) {
                // TODO: re-throw as StoreIndexException
            }
        }
        catch (IOException e) {
            throw new StoreAccessException(format("Error deleting indexed content %s", id), e);
        }
    }

    void ensureAttachmentPipeline() throws IOException {
        boolean found = false;
        try {
            var res = client.ingest().getPipeline(g -> g.id(SPRING_CONTENT_ATTACHMENT));
            found = res.get(SPRING_CONTENT_ATTACHMENT) != null;
        }
        catch (ElasticsearchException ese) {
            if (ese.status() != 404) {
                throw ese;
            }
        }
        if (!found) {
            var wpr = client.ingest().putPipeline(p -> p
                    .id(SPRING_CONTENT_ATTACHMENT)
                    .description("Extract attachment information encoded in Base64 with UTF-8 charset")
                    .processors(proc -> proc.attachment(a -> a.field("data"))));
            Assert.isTrue(wpr.acknowledged(), "Attachment pipeline not acknowledged by server");
        }
        pipelinedInitialized = true;
    }
}
