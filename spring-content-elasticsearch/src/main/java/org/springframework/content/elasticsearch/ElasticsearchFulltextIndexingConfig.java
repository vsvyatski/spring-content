package org.springframework.content.elasticsearch;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.search.IndexService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import internal.org.springframework.content.elasticsearch.ElasticsearchIndexer;

@Configuration
public class ElasticsearchFulltextIndexingConfig {

	@Autowired
	private IndexService elasticFulltextIndexService;

	@Bean
	public ElasticsearchIndexer elasticFulltextIndexerEventListener() {
		return new ElasticsearchIndexer(elasticFulltextIndexService);
	}
}
