package internal.org.springframework.content.elasticsearch.boot.autoconfigure;

import java.net.URI;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.elasticsearch.autoconfigure.ElasticsearchClientAutoConfiguration;
import org.springframework.content.commons.search.IndexService;
import org.springframework.content.elasticsearch.EnableElasticsearchFulltextIndexing;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest5_client.Rest5ClientTransport;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import internal.org.springframework.content.elasticsearch.ElasticsearchConfig;
import internal.org.springframework.content.elasticsearch.ElasticsearchIndexer;

@AutoConfiguration
@AutoConfigureAfter(ElasticsearchClientAutoConfiguration.class)
@ConditionalOnClass({ElasticsearchClient.class, EnableElasticsearchFulltextIndexing.class})
@Import(ElasticsearchConfig.class)
public class ElasticsearchAutoConfiguration {

    @ConditionalOnProperty(prefix = "spring.content.elasticsearch", name = "autoindex",
            havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean(ElasticsearchIndexer.class)
    @Bean
    public ElasticsearchIndexer elasticFulltextIndexerEventListener(IndexService<?> elasticFulltextIndexService) {
        return new ElasticsearchIndexer(elasticFulltextIndexService);
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean(ElasticsearchClient.class)
    public ElasticsearchClient elasticsearchClient() {
        Rest5Client restClient = Rest5Client.builder(URI.create("http://localhost:9200")).build();
        return new ElasticsearchClient(new Rest5ClientTransport(restClient, new JacksonJsonpMapper()));
    }

    @Component
    @ConfigurationProperties(prefix = "spring.content.elasticsearch")
    public static class ElasticsearchProperties {

        /**
         * Whether to perform automatic indexing of content as it is added
         */
        boolean autoindex = true;

        public boolean getAutoindex() {
            return autoindex;
        }

        public void setAutoindex(boolean autoindex) {
            this.autoindex = autoindex;
        }
    }
}
