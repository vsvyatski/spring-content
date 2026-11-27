package org.springframework.content.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import internal.org.springframework.content.elasticsearch.IndexManager;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;

@Configuration
public class GlobalIndexingStrategy implements IndexingStrategy, ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    private ElasticsearchClient client;

    public String indexName() {
        return IndexManager.INDEX_NAME;
    }

    @Override
    public void setup() throws Exception {
        if (client.indices().exists(e -> e.index(IndexManager.INDEX_NAME)).value()) {
            client.indices().delete(d -> d.index(IndexManager.INDEX_NAME));
        }
        client.indices().create(c -> c.index(IndexManager.INDEX_NAME));
    }

    @Override
    public void onApplicationEvent(@NonNull ContextRefreshedEvent event) {
        IndexManager.reset();
    }
}
