package org.springframework.content.elasticsearch;

import internal.org.springframework.content.elasticsearch.ElasticsearchConfig;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import({ElasticsearchFulltextIndexingConfig.class, ElasticsearchConfig.class})
public @interface EnableElasticsearchFulltextIndexing {
}