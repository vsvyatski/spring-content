package org.springframework.content.solr;

import internal.org.springframework.content.fragments.SearchableImpl;
import internal.org.springframework.content.solr.SolrFulltextIndexServiceImpl;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpJdkSolrClient;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.search.IndexService;
import org.springframework.content.fs.config.EnableFileSystemStores;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@ContextConfiguration(classes = EnableFullTextSolrIndexingTest.TestConfiguration.class)
@ExtendWith(SpringExtension.class)
public class EnableFullTextSolrIndexingTest {

    @Autowired
    private ApplicationContext context;

    @Configuration
    @EnableFileSystemStores
    @EnableFullTextSolrIndexing
    public static class TestConfiguration {

        @Bean
        public SolrClient solrClient() {
            return new HttpJdkSolrClient.Builder("http://some/url").build();
        }

        @Bean
        FileSystemResourceLoader fileSystemResourceLoader() throws IOException {
            return new FileSystemResourceLoader(Files.createTempDirectory("").toFile().getAbsolutePath());
        }

        // Developer bean - would usually be supplied by app developer
        @Bean
        public ConversionService contentConversionService() {
            return mock(ConversionService.class);
        }
    }

    @Nested
    class EnableFullTextSolrIndexingCases {
        @Test
        void shouldHaveSolrPropertiesBean() {
            assertThat(context.getBean(SolrProperties.class)).isNotNull();
        }

        @Test
        void shouldHaveSolrIndexingStoreEventHandlerBean() {
            assertThat(context.getBean(SolrIndexer.class)).isNotNull();
        }

        @Test
        void shouldHaveSearchableImplementationBean() {
            assertThat(context.getBeansOfType(SearchableImpl.class)).isNotNull();
        }

        @Test
        void shouldHaveSolrBasedFulltextIndexServiceBean() {
            assertThat(context.getBean(IndexService.class)).isInstanceOf(SolrFulltextIndexServiceImpl.class);
        }
    }
}
