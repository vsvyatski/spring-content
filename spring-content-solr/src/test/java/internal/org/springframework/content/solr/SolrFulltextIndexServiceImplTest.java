package internal.org.springframework.content.solr;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.TestFactory;
import static org.assertj.core.api.Assertions.assertThat;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.store.StoreAccessException;
import org.springframework.content.solr.SolrProperties;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static java.lang.String.format;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SolrFulltextIndexServiceImplTest {

    private SolrFulltextIndexServiceImpl indexer;

    private TEntity entity;
    private InputStream content;

    private Exception e;

    // mocks
    private SolrClient solr;
    private SolrProperties props;

    
    @Nested
    class IndexCases {
        @TestFactory
        java.util.stream.Stream<org.junit.jupiter.api.DynamicNode> generatedCases() {
            java.util.List<org.junit.jupiter.api.DynamicNode> tests = new java.util.ArrayList<>();
            for (Exception ex : new Exception[]{new SolrServerException("badness"), new IOException("badness")}) {
                {
                    java.util.List<org.junit.jupiter.api.DynamicNode> nodes1 = new java.util.ArrayList<>();
                    nodes1.add(org.junit.jupiter.api.DynamicTest.dynamicTest("should throw a StoreAccessException", () -> {
                        solr = mock(SolrClient.class);
                                        props = new SolrProperties();

                                        entity = new TEntity("12345");
                                        content = new ByteArrayInputStream("foo".getBytes());

                        when(solr.request(any(), any())).thenThrow(ex);

                        indexer = new SolrFulltextIndexServiceImpl(solr, props);

                                        try {
                                            indexer.index(entity, content);
                                        } catch (Exception e) {
                                            SolrFulltextIndexServiceImplTest.this.e = e;
                                        }
                        assertThat(e).isInstanceOf(StoreAccessException.class);
                                                assertThat(e.getCause().getMessage()).contains("badness");
                    }));
                    tests.add(org.junit.jupiter.api.DynamicContainer.dynamicContainer(format("when solr throws a %s", ex.getClass().getSimpleName()), nodes1.stream()));
                }
            }
            return tests.stream();
        }
    }
    @Nested
    class UnindexCases {
        @TestFactory
        java.util.stream.Stream<org.junit.jupiter.api.DynamicNode> generatedCases() {
            java.util.List<org.junit.jupiter.api.DynamicNode> tests = new java.util.ArrayList<>();
            for (Exception ex : new Exception[]{new SolrServerException("badness"), new IOException("badness")}) {
                {
                    java.util.List<org.junit.jupiter.api.DynamicNode> nodes2 = new java.util.ArrayList<>();
                    nodes2.add(org.junit.jupiter.api.DynamicTest.dynamicTest("should throw a StoreAccessException", () -> {
                        solr = mock(SolrClient.class);
                                        props = new SolrProperties();

                                        entity = new TEntity("12345");

                        when(solr.request(any(), any())).thenThrow(ex);

                        indexer = new SolrFulltextIndexServiceImpl(solr, props);

                                        try {
                                            indexer.unindex(entity);
                                        } catch (Exception e) {
                                            SolrFulltextIndexServiceImplTest.this.e = e;
                                        }
                        assertThat(e).isInstanceOf(StoreAccessException.class);
                                                assertThat(e.getCause().getMessage()).contains("badness");
                    }));
                    tests.add(org.junit.jupiter.api.DynamicContainer.dynamicContainer(format("when solr throws a %s", ex.getClass().getSimpleName()), nodes2.stream()));
                }
            }
            return tests.stream();
        }
    }


    private static class TEntity {

        @ContentId
        private String contentId;

        public TEntity(String contentId) {
            this.contentId = contentId;
        }

        public String getContentId() {
            return contentId;
        }

        public void setContentId(String contentId) {
            this.contentId = contentId;
        }
    }
}


