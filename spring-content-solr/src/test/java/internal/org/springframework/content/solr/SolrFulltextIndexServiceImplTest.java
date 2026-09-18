package internal.org.springframework.content.solr;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.junit.runner.RunWith;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.store.StoreAccessException;
import org.springframework.content.solr.SolrProperties;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Ginkgo4jRunner.class)
public class SolrFulltextIndexServiceImplTest {

    private SolrFulltextIndexServiceImpl indexer;

    private TEntity entity;
    private InputStream content;

    private Exception e;

    // mocks
    private SolrClient solr;
    private SolrProperties props;

    {
        Describe("#index", () -> {

            BeforeEach(() -> {
                solr = mock(SolrClient.class);
                props = new SolrProperties();

                entity = new TEntity("12345");
                content = new ByteArrayInputStream("foo".getBytes());
            });

            JustBeforeEach(() -> {
                indexer = new SolrFulltextIndexServiceImpl(solr, props);

                try {
                    indexer.index(entity, content);
                } catch (Exception e) {
                    this.e = e;
                }
            });

            for (Exception ex : new Exception[]{new SolrServerException("badness"), new IOException("badness")}) {

                Context(format("when solr throws a %s", ex.getClass().getSimpleName()), () -> {

                    BeforeEach(() -> {
                        when(solr.request(any(), any())).thenThrow(ex);
                    });

                    It("should throw a StoreAccessException", () -> {
                        assertThat(e, is(instanceOf(StoreAccessException.class)));
                        assertThat(e.getCause().getMessage(), containsString("badness"));
                    });
                });
            }
        });

        Describe("#unindex", () -> {

            BeforeEach(() -> {
                solr = mock(SolrClient.class);
                props = new SolrProperties();

                entity = new TEntity("12345");
            });

            JustBeforeEach(() -> {
                indexer = new SolrFulltextIndexServiceImpl(solr, props);

                try {
                    indexer.unindex(entity);
                } catch (Exception e) {
                    this.e = e;
                }
            });

            for (Exception ex : new Exception[]{new SolrServerException("badness"), new IOException("badness")}) {

                Context(format("when solr throws a %s", ex.getClass().getSimpleName()), () -> {

                    BeforeEach(() -> {
                        when(solr.request(any(), any())).thenThrow(ex);
                    });

                    It("should throw a StoreAccessException", () -> {
                        assertThat(e, is(instanceOf(StoreAccessException.class)));
                        assertThat(e.getCause().getMessage(), containsString("badness"));
                    });
                });
            }
        });
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


