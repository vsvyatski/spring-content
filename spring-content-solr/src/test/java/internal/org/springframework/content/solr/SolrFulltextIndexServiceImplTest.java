package internal.org.springframework.content.solr;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.store.StoreAccessException;
import org.springframework.content.solr.SolrProperties;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
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

    @Nested
    class IndexCases {
        @Nested
        class WhenSolrThrowsSolrServerException {
            @Test
            void shouldThrowStoreAccessException() throws Exception {
                Exception ex = new SolrServerException("badness");
                // noinspection resource
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
            }
        }

        @Nested
        class WhenSolrThrowsIOException {
            @Test
            void shouldThrowStoreAccessException() throws Exception {
                Exception ex = new IOException("badness");
                // noinspection resource
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
            }
        }
    }

    @Nested
    class UnindexCases {
        @Nested
        class WhenSolrThrowsSolrServerException {
            @Test
            void shouldThrowStoreAccessException() throws Exception {
                Exception ex = new SolrServerException("badness");
                // noinspection resource
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
            }
        }

        @Nested
        class WhenSolrThrowsIOException {
            @Test
            void shouldThrowStoreAccessException() throws Exception {
                Exception ex = new IOException("badness");
                // noinspection resource
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
            }
        }
    }
}
