package org.springframework.content.elasticsearch;

import internal.org.springframework.content.elasticsearch.IndexManager;
import org.springframework.content.test.support.fulltext.FulltextJpaUuidDocumentEntity;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;

@Configuration
public class EntityIndexingStrategy implements IndexingStrategy, ApplicationListener {

    private static final String INDEX_NAME = FulltextJpaUuidDocumentEntity.class.getName().toLowerCase();

    @Override
    public void setup() throws Exception {
    }

    public String indexName() {
        return INDEX_NAME;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ContextRefreshedEvent) {
            IndexManager.reset();
        }
    }
}
