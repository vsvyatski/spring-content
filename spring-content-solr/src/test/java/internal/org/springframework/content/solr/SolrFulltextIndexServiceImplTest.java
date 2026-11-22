package internal.org.springframework.content.solr;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.content.commons.store.StoreAccessException;
import org.springframework.content.solr.SolrProperties;
import org.springframework.content.test.support.SingleContentFields;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.content.test.support.TestSupportData.SAMPLE_SHORT_CONTENT_ID;
import static org.springframework.content.test.support.TestSupportData.sampleTextStream;

public class SolrFulltextIndexServiceImplTest {

    private SolrFulltextIndexServiceImpl indexer;

    private SingleContentFields entity;
    private InputStream content;

    private Exception e;

    // mocks
    private SolrClient solr;
    private SolrProperties props;

    @Nested
    class IndexCases {
        @Nested
        class WhenSolrThrowsSolrServerException {
            @Test
            void shouldThrowStoreAccessException() throws Exception {
                Exception ex = new SolrServerException("badness");
                solr = mock(SolrClient.class);
                props = new SolrProperties();

                entity = new SingleContentFields();
                entity.setContentId(SAMPLE_SHORT_CONTENT_ID);
                content = sampleTextStream();

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
                solr = mock(SolrClient.class);
                props = new SolrProperties();

                entity = new SingleContentFields();
                entity.setContentId(SAMPLE_SHORT_CONTENT_ID);
                content = sampleTextStream();

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
                solr = mock(SolrClient.class);
                props = new SolrProperties();

                entity = new SingleContentFields();
                entity.setContentId(SAMPLE_SHORT_CONTENT_ID);

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
                solr = mock(SolrClient.class);
                props = new SolrProperties();

                entity = new SingleContentFields();
                entity.setContentId(SAMPLE_SHORT_CONTENT_ID);

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
