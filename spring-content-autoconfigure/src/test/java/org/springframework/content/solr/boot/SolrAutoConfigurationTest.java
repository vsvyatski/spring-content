package org.springframework.content.solr.boot;

import org.junit.jupiter.api.extension.ExtendWith;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Disabled;
import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import internal.org.springframework.content.elasticsearch.boot.autoconfigure.ElasticsearchAutoConfiguration;
import internal.org.springframework.content.solr.SolrFulltextIndexServiceImpl;
import internal.org.springframework.content.solr.boot.autoconfigure.SolrAutoConfiguration;
import org.apache.solr.client.solrj.SolrClient;
import org.assertj.core.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.content.commons.search.IndexService;
import org.springframework.content.solr.SolrIndexerStoreEventHandler;
import org.springframework.content.solr.SolrProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;

@ExtendWith(SpringExtension.class)
public class SolrAutoConfigurationTest {

    private ApplicationContextRunner contextRunner;

    
    @Nested
    class Solr {
        @Nested
        class GivenAnApplicationContextWithASolrClientBeanAndSolrAutoConfiguration {
            @BeforeEach
            void setUp() throws Throwable {
                contextRunner = new ApplicationContextRunner()
                                        .withConfiguration(AutoConfigurations.of(SolrAutoConfiguration.class));
            }

            @Test
            void shouldIncludeTheAutoconfiguredAnnotatedEventHandlerBean() throws Throwable {
                contextRunner.withUserConfiguration(TestConfig.class)
                                                .run((context) ->
                                                        Assertions.assertThat(context).getBean("solrFulltextEventListener")
                                                                .isNotNull());
            }

        }

    }

    @Test
    public void test() throws Throwable {
    }

    @Disabled("This is not a test")
    @SpringBootApplication(exclude = ElasticsearchAutoConfiguration.class)
    public static class TestConfig {

        @Autowired
        private SolrProperties props;
        @Autowired
        private SolrClient solrClient;

        public TestConfig() {
        }

        @Bean
        public IndexService solrIndexService() {
            return new SolrFulltextIndexServiceImpl(solrClient, props);
        }

        @Bean
        public Object solrFulltextEventListener() {
            return new SolrIndexerStoreEventHandler(solrIndexService());
        }
    }
}
