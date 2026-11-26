package org.springframework.content.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import internal.org.springframework.content.elasticsearch.ElasticsearchIndexServiceImpl;
import internal.org.springframework.content.elasticsearch.ElasticsearchIndexer;
import internal.org.springframework.content.elasticsearch.IndexManager;
import internal.org.springframework.content.fragments.SearchableImpl;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.search.IndexService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Smoke test for {@link EnableElasticsearchFulltextIndexing} wiring (mirrors Solr full-text enablement test).
 *
 * @author Vladimir Svyatski
 */
@ContextConfiguration(classes = EnableElasticsearchFulltextIndexingTest.TestConfiguration.class)
@ExtendWith(SpringExtension.class)
class EnableElasticsearchFulltextIndexingTest {

    @Autowired
    private ApplicationContext context;

    @Configuration
    @EnableElasticsearchFulltextIndexing
    static class TestConfiguration {

        @Bean
        ElasticsearchClient client() {
            return mock(ElasticsearchClient.class);
        }
    }

    @Nested
    class EnableElasticsearchFulltextIndexingCases {
        @Test
        void shouldHaveIndexManagerBean() {
            assertThat(context.getBean(IndexManager.class)).isNotNull();
        }

        @Test
        void shouldHaveElasticsearchIndexerBean() {
            assertThat(context.getBean(ElasticsearchIndexer.class)).isNotNull();
        }

        @Test
        void shouldHaveSearchableImplementationType() {
            assertThat(SearchableImpl.class).isNotNull();
            assertThat(context.getBeansOfType(SearchableImpl.class)).isNotNull();
        }

        @Test
        void shouldHaveElasticsearchBasedFulltextIndexServiceBean() {
            assertThat(context.getBean(IndexService.class)).isInstanceOf(ElasticsearchIndexServiceImpl.class);
        }
    }
}