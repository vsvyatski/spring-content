package internal.org.springframework.content.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import org.springframework.content.commons.store.StoreAccessException;

import java.io.IOException;

public class IndexManager {

    public static final String INDEX_NAME = "spring-content-fulltext-index";
    private static Boolean globalIndexing = null;
    private final ElasticsearchClient client;

    public IndexManager(ElasticsearchClient client) {
        this.client = client;
    }

    public static void reset() {
        globalIndexing = null;
    }

    public String indexName(Class<?> entityClass) {

        if (globalIndexing == null) {
            try {
                globalIndexing = client.indices().exists(e -> e.index(INDEX_NAME)).value();
            } catch (IOException ioe) {
                throw new StoreAccessException("Unable to resolve elasticsearch index", ioe);
            }
        }

        if (globalIndexing) {
            return INDEX_NAME;
        } else {
            return entityClass.getName().toLowerCase();
        }
    }
}
