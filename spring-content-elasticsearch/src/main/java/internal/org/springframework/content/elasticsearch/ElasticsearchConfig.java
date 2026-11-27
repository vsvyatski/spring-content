package internal.org.springframework.content.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import internal.org.springframework.content.commons.renditions.RenditionServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.renditions.RenditionProvider;
import org.springframework.content.commons.renditions.RenditionService;
import org.springframework.content.commons.search.IndexService;
import org.springframework.content.commons.search.AttributeProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration("springContentElasticsearchConfig")
public class ElasticsearchConfig {

    @Autowired
    private ElasticsearchClient client;

    @Autowired(required = false)
    private RenditionService renditionService;

    @Autowired(required = false)
    private AttributeProvider<?> attributeProvider;

    private final List<RenditionProvider> providers = new ArrayList<>();

    @Autowired(required = false)
    public void setRenditionProviders(RenditionProvider... providers) {
        if (providers != null && providers.length > 0) {
            this.providers.addAll(Arrays.asList(providers));
        }
    }

    public RenditionService getRenditionService() {
        if (this.renditionService == null) {
            this.renditionService = new RenditionServiceImpl(providers.toArray(RenditionProvider[]::new));
        }
        return this.renditionService;
    }

    @Bean
    public IndexService<Object> elasticFulltextIndexService() {
        return new ElasticsearchIndexServiceImpl<>(
            client, this.getRenditionService(), indexManager(), attributeProvider);
    }

    @Bean
    public IndexManager indexManager() {
        return new IndexManager(client);
    }
}
