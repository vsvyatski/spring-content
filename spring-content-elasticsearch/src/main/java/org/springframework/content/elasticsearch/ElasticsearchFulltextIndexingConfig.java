package org.springframework.content.elasticsearch;

import internal.org.springframework.content.elasticsearch.ElasticsearchIndexer;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.search.IndexService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticsearchFulltextIndexingConfig {

	@Autowired
	private RestHighLevelClient client;

	@Autowired
	private IndexService elasticFulltextIndexService;

	@Bean
	public ElasticsearchIndexer elasticFulltextIndexerEventListener() {
		return new ElasticsearchIndexer(elasticFulltextIndexService);
	}
}
