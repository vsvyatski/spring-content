package org.springframework.content.solr;

import org.junit.jupiter.api.extension.ExtendWith;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import internal.org.springframework.content.fragments.SearchableImpl;
import internal.org.springframework.content.solr.SolrFulltextIndexServiceImpl;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpJdkSolrClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.commons.search.IndexService;
import org.springframework.content.commons.search.Searchable;
import org.springframework.content.fs.config.EnableFileSystemStores;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.test.context.ContextConfiguration;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;

import static org.mockito.Mockito.mock;

@ContextConfiguration(classes = EnableFullTextSolrIndexingTest.TestConfiguration.class)
@ExtendWith(SpringExtension.class)
public class EnableFullTextSolrIndexingTest {

    @Autowired
    private ApplicationContext context;

    @Nested
    class EnableFullTextSolrIndexingCases {
        @Test
        void shouldHaveASolrPropertiesBean() {
            assertThat(context.getBean(SolrProperties.class)).isNotNull();
        }

        @Test
        void shouldHaveASolrIndexingStoreEventHandlerBean() {
            assertThat(context.getBean(SolrIndexerStoreEventHandler.class)).isNotNull();
        }

        @Test
        void shouldHaveASearchableImplementationBean() {
            assertThat(context.getBeansOfType(SearchableImpl.class)).isNotNull();
        }

        @Test
        void shouldHaveASolrBasedFulltextIndexServiceBean() {
            assertThat(context.getBean(IndexService.class)).isInstanceOf(SolrFulltextIndexServiceImpl.class);
        }

    }

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

    public interface TContentStore extends ContentStore<Object, Serializable>, Searchable<Serializable> {
    }

}
