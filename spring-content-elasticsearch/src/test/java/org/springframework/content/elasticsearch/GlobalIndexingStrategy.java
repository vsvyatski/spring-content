package org.springframework.content.elasticsearch;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import internal.org.springframework.content.elasticsearch.IndexManager;

@Configuration
public class GlobalIndexingStrategy implements IndexingStrategy {

    private static final String INDEX_NAME = IndexManager.INDEX_NAME;

    @Autowired
    private ElasticsearchClient client;

    public String indexName() {
        return INDEX_NAME;
    }

    public void setup() throws Exception {
        client.indices().create(c -> c.index(INDEX_NAME));
    }
}
