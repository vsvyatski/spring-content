package org.springframework.content.solr;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.request.SolrQuery;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.test.support.fulltext.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

@ContextConfiguration(classes = {SolrITConfig.class})
@ExtendWith(SpringExtension.class)
public class SolrIT {

    @Autowired
    private FulltextJpaUuidDocumentRepository docRepo;
    @Autowired
    private FulltextJpaUuidDocumentStore docContentStore;
    @Autowired
    private FulltextJpaUuidDocumentSearchableStore store;
    @Autowired
    private SolrClient solr; //for tests
    @Autowired
    private SolrProperties solrProperties;
    private FulltextJpaUuidDocumentEntity doc, doc2;
    private UUID id = null;

    private void configureSolrAuth() {
        solrProperties.setUser("solr");
        solrProperties.setPassword("SolrRocks");
    }

    private void purgeSolrIndex() throws IOException, SolrServerException {
        if (solr != null) {
            UpdateRequest req = new UpdateRequest();
            req.deleteByQuery("*");
            req.setBasicAuthCredentials(solrProperties.getUser(), solrProperties.getPassword());
            req.process(solr, null);
            req.commit(solr, null);
        }
    }

    @Nested
    class Index {
        @BeforeEach
        void setUp() {
            configureSolrAuth();

            doc = new FulltextJpaUuidDocumentEntity();
            doc.setTitle("title of document 1");
            doc.setAuthor("author@email.com");
            doc = docRepo.save(doc);
            doc = docContentStore.setContent(doc, this.getClass().getResourceAsStream("/one.docx"));
            doc = docRepo.save(doc);
        }

        @AfterEach
        void tearDown() throws IOException, SolrServerException {
            if (docContentStore != null) {
                docContentStore.unsetContent(doc);
            }
            if (docRepo != null) {
                docRepo.delete(doc);
            }
            purgeSolrIndex();
        }

        @Test
        void shouldIndexTheContentOfThatDocument() {
            org.awaitility.Awaitility.await().atMost(java.time.Duration.ofSeconds(10)).untilAsserted(() -> {
                SolrQuery query = new SolrQuery();
                query.setQuery("foo");
                String fq = format(
                    "id:%s\\:%s", FulltextJpaUuidDocumentEntity.class.getCanonicalName(), doc.getContentId());
                query.addFilterQuery(fq);
                QueryRequest request = new QueryRequest(query);
                request.setBasicAuthCredentials(solrProperties.getUser(), solrProperties.getPassword());
                QueryResponse response = request.process(solr);
                assertThat(response.getResults()).hasSize(1);
            });
        }

        @Nested
        class WhenTheContentIsSearched {
            @BeforeEach
            void setUp() {
                configureSolrAuth();

                doc = new FulltextJpaUuidDocumentEntity();
                doc.setTitle("title of document 1");
                doc.setAuthor("author@email.com");
                doc = docRepo.save(doc);
                doc = docContentStore.setContent(doc, this.getClass().getResourceAsStream("/one.docx"));
                doc = docRepo.save(doc);
            }

            @AfterEach
            void tearDown() throws IOException, SolrServerException {
                if (docContentStore != null) {
                    docContentStore.unsetContent(doc);
                }
                if (docRepo != null) {
                    docRepo.delete(doc);
                }
                purgeSolrIndex();
            }

            @Test
            void shouldReturnTheSearchedContent() {
                Iterable<UUID> content = docContentStore.search("one");
                assertThat(content).contains(doc.getContentId());
            }
        }

        @Nested
        class GivenThatDocumentsContentIsUpdated {
            @BeforeEach
            void setUp() {
                configureSolrAuth();

                doc = new FulltextJpaUuidDocumentEntity();
                doc.setTitle("title of document 1");
                doc.setAuthor("author@email.com");
                doc = docRepo.save(doc);
                doc = docContentStore.setContent(doc, this.getClass().getResourceAsStream("/one.docx"));
                doc = docRepo.save(doc);

                docContentStore.setContent(doc, this.getClass().getResourceAsStream("/two.rtf"));
                docRepo.save(doc);
            }

            @AfterEach
            void tearDown() throws IOException, SolrServerException {
                if (docContentStore != null) {
                    docContentStore.unsetContent(doc);
                }
                if (docRepo != null) {
                    docRepo.delete(doc);
                }
                purgeSolrIndex();
            }

            @Test
            void shouldIndexTheNewContent() {
                org.awaitility.Awaitility.await().atMost(java.time.Duration.ofSeconds(10)).untilAsserted(() -> {
                    SolrQuery query = new SolrQuery();
                    query.setQuery("bar");
                    String fq = format(
                        "id:%s\\:%s", FulltextJpaUuidDocumentEntity.class.getCanonicalName(), doc.getContentId());
                    query.addFilterQuery(fq);
                    QueryRequest request = new QueryRequest(query);
                    request.setBasicAuthCredentials(solrProperties.getUser(), solrProperties.getPassword());
                    QueryResponse response = request.process(solr);
                    assertThat(response.getResults()).hasSize(1);
                });
            }
        }

        @Nested
        class GivenThatDocumentIsDeleted {
            @BeforeEach
            void setUp() {
                configureSolrAuth();

                doc = new FulltextJpaUuidDocumentEntity();
                doc.setTitle("title of document 1");
                doc.setAuthor("author@email.com");
                doc = docRepo.save(doc);
                doc = docContentStore.setContent(doc, this.getClass().getResourceAsStream("/one.docx"));
                doc = docRepo.save(doc);

                id = doc.getContentId();
                docContentStore.unsetContent(doc);
                docRepo.delete(doc);
                doc = null;
            }

            @AfterEach
            void tearDown() throws IOException, SolrServerException {
                purgeSolrIndex();
            }

            @Test
            void shouldDeleteTheRecordOfTheContentFromTheIndex() throws SolrServerException, IOException {
                SolrQuery query = new SolrQuery();
                query.setQuery("one");
                query.addFilterQuery(format(
                    "id:%s\\:%s", FulltextJpaUuidDocumentEntity.class.getCanonicalName(), id));
                query.setFields("content");

                QueryRequest request = new QueryRequest(query);
                request.setBasicAuthCredentials(solrProperties.getUser(), solrProperties.getPassword());

                QueryResponse response = request.process(solr);
                SolrDocumentList results = response.getResults();

                assertThat(results.size()).isEqualTo(0);
            }
        }
    }

    @Nested
    class Paging {
        @Nested
        class PagesOfResults {
            @BeforeEach
            void setUp() {
                configureSolrAuth();

                for (int i = 0; i < 10; i++) {
                    FulltextJpaUuidDocumentEntity pageDoc = new FulltextJpaUuidDocumentEntity();
                    pageDoc.setTitle(format("doc %s", i));
                    pageDoc.setAuthor("author@email.com");
                    pageDoc = docRepo.save(pageDoc);
                    pageDoc = docContentStore.setContent(pageDoc, this.getClass().getResourceAsStream("/one.docx"));
                    docRepo.save(pageDoc);
                }
            }

            @AfterEach
            void tearDown() throws IOException, SolrServerException {
                if (docRepo != null && docContentStore != null) {
                    for (FulltextJpaUuidDocumentEntity pageDoc : docRepo.findAll()) {
                        docContentStore.unsetContent(pageDoc);
                    }
                    for (FulltextJpaUuidDocumentEntity pageDoc : docRepo.findAll()) {
                        docRepo.deleteById(pageDoc.getId());
                    }
                }
                purgeSolrIndex();
            }

            @Test
            void shouldReturnResultsInPages() {
                org.awaitility.Awaitility.await().untilAsserted(() -> {
                    var page = (docContentStore.search("foo", PageRequest.of(0, 3)));
                    assertThat(page.getNumberOfElements()).isEqualTo(3);

                });

                org.awaitility.Awaitility.await().untilAsserted(() -> {
                    var page = (docContentStore.search("foo", PageRequest.of(1, 3)));
                    assertThat(page.getNumberOfElements()).isEqualTo(3);

                });

                org.awaitility.Awaitility.await().untilAsserted(() -> {
                    var page = (docContentStore.search("foo", PageRequest.of(2, 3)));
                    assertThat(page.getNumberOfElements()).isEqualTo(3);

                });

                org.awaitility.Awaitility.await().untilAsserted(() -> {
                    var page = (docContentStore.search("foo", PageRequest.of(3, 3)));
                    assertThat(page.getNumberOfElements()).isEqualTo(1);

                });
            }

            @Test
            void shouldReturnSpecificResultPage() {
                org.awaitility.Awaitility.await().untilAsserted(() -> {
                    var page = (docContentStore.search("foo", PageRequest.of(3, 3)));
                    assertThat(page.getNumberOfElements()).isEqualTo(1);

                });
            }
        }
    }

    @Nested
    class CustomAttributes {
        @BeforeEach
        void setUp() {
            configureSolrAuth();

            doc = new FulltextJpaUuidDocumentEntity();
            doc.setTitle("title of document 1");
            doc.setAuthor("author@email.com");
            doc = docRepo.save(doc);
            doc = store.setContent(doc, this.getClass().getResourceAsStream("/one.docx"));
            doc = docRepo.save(doc);

            doc2 = new FulltextJpaUuidDocumentEntity();
            doc2.setTitle("title of document 2");
            doc2.setAuthor("author@abc.com");
            doc2 = docRepo.save(doc2);
            doc2 = store.setContent(doc2, this.getClass().getResourceAsStream("/one.docx"));
            doc2 = docRepo.save(doc2);
        }

        @AfterEach
        void tearDown() throws IOException, SolrServerException {
            if (doc != null) {
                store.unsetContent(doc);
                docRepo.delete(doc);
            }
            if (doc2 != null) {
                store.unsetContent(doc2);
                docRepo.delete(doc2);
            }
            purgeSolrIndex();
        }

        @Test
        void shouldApplyTheProvidedAttributesAndFilterQuery() {
            Iterable<UUID> tmp = docContentStore.search("one");
            assertThat(tmp).isNotNull();

            List<UUID> results = new ArrayList<>();
            tmp.forEach(results::add);

            assertThat(results).contains(doc.getContentId());
            assertThat(results).doesNotContain(doc2.getContentId());
        }
    }

    @Nested
    class CustomReturnTypes {
        @BeforeEach
        void setUp() {
            configureSolrAuth();

            doc = new FulltextJpaUuidDocumentEntity();
            doc.setTitle("title of document 1");
            doc.setAuthor("author@email.com");
            doc = docRepo.save(doc);
            doc = store.setContent(doc, this.getClass().getResourceAsStream("/one.docx"));
            doc = docRepo.save(doc);
        }

        @AfterEach
        void tearDown() throws IOException, SolrServerException {
            if (doc != null) {
                store.unsetContent(doc);
                docRepo.delete(doc);
            }
            purgeSolrIndex();
        }

        @Test
        void shouldReturnResultsUsingTheReturnType() {
            org.awaitility.Awaitility.await().untilAsserted(() -> {
                var result = (store.search("one"));
                Iterator<FulltextSearchResult> iterator = result.iterator();
                assertThat(iterator.hasNext()).isTrue();
                assertThat(iterator.next()).satisfies(actual -> {
                    assertThat(actual).hasFieldOrPropertyWithValue("contentId", doc.getContentId());
                    assertThat(actual).extracting("highlight", org.assertj.core.api.InstanceOfAssertFactories.STRING)
                        .contains("<em>one</em>");
                    assertThat(actual).extracting("author", org.assertj.core.api.InstanceOfAssertFactories.STRING)
                        .contains("author@email.com");
                });
                assertThat(iterator.hasNext()).isFalse();

            });
        }
    }
}
