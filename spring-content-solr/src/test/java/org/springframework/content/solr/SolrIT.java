package org.springframework.content.solr;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.request.SolrQuery;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.test.support.fulltext.FulltextJpaUuidDocumentEntity;
import org.springframework.content.test.support.fulltext.FulltextJpaUuidDocumentRepository;
import org.springframework.content.test.support.fulltext.FulltextJpaUuidDocumentSearchableStore;
import org.springframework.content.test.support.fulltext.FulltextSearchResult;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

@ContextConfiguration(classes = {SolrITConfig.class})
@TestPropertySource(properties = "spring.content.storage.type.default=jpa")
@ExtendWith(SpringExtension.class)
public class SolrIT {

    @Autowired
    private FulltextJpaUuidDocumentRepository docRepo;

    @Autowired
    private FulltextJpaUuidDocumentSearchableStore store;

    @Autowired
    private SolrClient solr; //for tests

    @Autowired
    private SolrProperties solrProperties;

    private static InputStream testResource(String path) {
        return Objects.requireNonNull(SolrIT.class.getResourceAsStream(path),
            () -> "Missing classpath test resource: " + path);
    }

    private static UUID contentId(FulltextJpaUuidDocumentEntity entity) {
        return Objects.requireNonNull(entity.getContentId(), "contentId");
    }

    private void setContentAndSave(FulltextJpaUuidDocumentEntity entity, String classpathResource) {
        store.setContent(entity, testResource(classpathResource));
        docRepo.save(entity);
    }

    private void applySolrAuth(QueryRequest request) {
        if (solrProperties.getUser() != null) {
            request.setBasicAuthCredentials(solrProperties.getUser(), solrProperties.getPassword());
        }
    }

    private void applySolrAuth(UpdateRequest request) {
        if (solrProperties.getUser() != null) {
            request.setBasicAuthCredentials(solrProperties.getUser(), solrProperties.getPassword());
        }
    }

    private void purgeSolrIndex() throws IOException, SolrServerException {
        UpdateRequest req = new UpdateRequest().deleteByQuery("*");
        applySolrAuth(req);
        req.process(solr, null);
        req.commit(solr, null);
    }

    @Nested
    class Index {

        @Test
        void shouldIndexContentOfThatDocument() throws IOException, SolrServerException {
            FulltextJpaUuidDocumentEntity doc =
                FulltextJpaUuidDocumentEntity.withTitleAndAuthor("title of document 1", "author@email.com");
            setContentAndSave(doc, "/one.docx");

            UUID indexedContentId = contentId(doc);

            try {
                Awaitility.await().atMost(java.time.Duration.ofSeconds(10)).untilAsserted(() -> {
                    SolrQuery query = new SolrQuery().setQuery("foo");
                    String fq = format(
                        "id:%s\\:%s", FulltextJpaUuidDocumentEntity.class.getCanonicalName(), indexedContentId);
                    query.addFilterQuery(fq);
                    QueryRequest request = new QueryRequest(query);
                    applySolrAuth(request);
                    QueryResponse response = request.process(solr);
                    assertThat(response.getResults()).hasSize(1);
                });
            } finally {
                store.unsetContent(doc);
                docRepo.delete(doc);
                purgeSolrIndex();
            }
        }

        @Nested
        @DisplayName("When the content is searched")
        class ContentIsSearched {

            private FulltextJpaUuidDocumentEntity doc;

            @BeforeEach
            void setUp() {
                doc = FulltextJpaUuidDocumentEntity.withTitleAndAuthor("title of document 1", "author@email.com");
                setContentAndSave(doc, "/one.docx");
            }

            @AfterEach
            void tearDown() throws IOException, SolrServerException {
                store.unsetContent(doc);
                docRepo.delete(doc);
                purgeSolrIndex();
            }

            @Test
            void shouldReturnSearchedContent() {
                var content = store.search("one");
                assertThat(content).extracting(FulltextSearchResult::getContentId).contains(doc.getContentId());
            }
        }

        @Nested
        @DisplayName("When the document content is updated")
        class DocumentsContentIsUpdated {

            private FulltextJpaUuidDocumentEntity doc;

            @BeforeEach
            void setUp() {
                doc = FulltextJpaUuidDocumentEntity.withTitleAndAuthor("title of document 1", "author@email.com");
                setContentAndSave(doc, "/one.docx");
            }

            @AfterEach
            void tearDown() throws IOException, SolrServerException {
                store.unsetContent(doc);
                docRepo.delete(doc);
                purgeSolrIndex();
            }

            @Test
            void shouldIndexNewContent() {
                UUID initialContentId = contentId(doc);

                Awaitility.await().atMost(java.time.Duration.ofSeconds(10)).untilAsserted(() -> {
                    SolrQuery initialQuery = new SolrQuery("foo");
                    initialQuery.addFilterQuery(format(
                        "id:%s\\:%s", FulltextJpaUuidDocumentEntity.class.getCanonicalName(), initialContentId));
                    QueryRequest initialRequest = new QueryRequest(initialQuery);
                    applySolrAuth(initialRequest);
                    assertThat(initialRequest.process(solr).getResults()).hasSize(1);
                });

                setContentAndSave(doc, "/two.rtf");

                UUID indexedContentId = contentId(doc);

                Awaitility.await().atMost(java.time.Duration.ofSeconds(10)).untilAsserted(() -> {
                    SolrQuery query = new SolrQuery();
                    query.setQuery("bar");
                    String fq = format(
                        "id:%s\\:%s", FulltextJpaUuidDocumentEntity.class.getCanonicalName(), indexedContentId);
                    query.addFilterQuery(fq);
                    QueryRequest request = new QueryRequest(query);
                    applySolrAuth(request);
                    QueryResponse response = request.process(solr);
                    assertThat(response.getResults()).hasSize(1);
                });
            }
        }

        @Nested
        @DisplayName("When the document is deleted")
        class DocumentIsDeleted {

            private FulltextJpaUuidDocumentEntity doc;

            @BeforeEach
            void setUp() {
                doc = FulltextJpaUuidDocumentEntity.withTitleAndAuthor("title of document 1", "author@email.com");
                setContentAndSave(doc, "/one.docx");
            }

            @AfterEach
            void tearDown() throws IOException, SolrServerException {
                if (docRepo.existsById(doc.getId())) {
                    store.unsetContent(doc);
                    docRepo.delete(doc);
                }
                purgeSolrIndex();
            }

            @Test
            void shouldDeleteRecordOfContentFromIndex() {
                UUID indexedContentId = contentId(doc);

                Awaitility.await().atMost(java.time.Duration.ofSeconds(10)).untilAsserted(() -> {
                    SolrQuery indexedQuery = new SolrQuery("one");
                    indexedQuery.addFilterQuery(format(
                        "id:%s\\:%s", FulltextJpaUuidDocumentEntity.class.getCanonicalName(), indexedContentId));
                    QueryRequest indexedRequest = new QueryRequest(indexedQuery);
                    applySolrAuth(indexedRequest);
                    assertThat(indexedRequest.process(solr).getResults()).hasSize(1);
                });

                store.unsetContent(doc);
                docRepo.delete(doc);

                Awaitility.await().atMost(java.time.Duration.ofSeconds(10)).untilAsserted(() -> {
                    SolrQuery query = new SolrQuery("one");
                    query.addFilterQuery(format(
                        "id:%s\\:%s", FulltextJpaUuidDocumentEntity.class.getCanonicalName(), indexedContentId));
                    query.setFields("content");
                    QueryRequest request = new QueryRequest(query);
                    applySolrAuth(request);
                    assertThat(request.process(solr).getResults()).isEmpty();
                });
            }
        }
    }

    @Nested
    class Paging {
        @Nested
        class PagesOfResults {
            @BeforeEach
            void setUp() {
                for (int i = 0; i < 10; i++) {
                    FulltextJpaUuidDocumentEntity pageDoc =
                        FulltextJpaUuidDocumentEntity.withTitleAndAuthor(format("doc %s", i), "author@email.com");
                    setContentAndSave(pageDoc, "/one.docx");
                }
            }

            @AfterEach
            void tearDown() throws IOException, SolrServerException {
                for (FulltextJpaUuidDocumentEntity pageDoc : docRepo.findAll()) {
                    store.unsetContent(pageDoc);
                }
                for (FulltextJpaUuidDocumentEntity pageDoc : docRepo.findAll()) {
                    docRepo.deleteById(pageDoc.getId());
                }
                purgeSolrIndex();
            }

            @Test
            void shouldReturnResultsInPages() {
                Awaitility.await().untilAsserted(() -> {
                    var page = (store.search("foo", PageRequest.of(0, 3)));
                    assertThat(page.getNumberOfElements()).isEqualTo(3);

                });

                Awaitility.await().untilAsserted(() -> {
                    var page = (store.search("foo", PageRequest.of(1, 3)));
                    assertThat(page.getNumberOfElements()).isEqualTo(3);

                });

                Awaitility.await().untilAsserted(() -> {
                    var page = (store.search("foo", PageRequest.of(2, 3)));
                    assertThat(page.getNumberOfElements()).isEqualTo(3);

                });

                Awaitility.await().untilAsserted(() -> {
                    var page = (store.search("foo", PageRequest.of(3, 3)));
                    assertThat(page.getNumberOfElements()).isEqualTo(1);
                });
            }

            @Test
            void shouldReturnSpecificResultPage() {
                Awaitility.await().untilAsserted(() -> {
                    var page = (store.search("foo", PageRequest.of(3, 3)));
                    assertThat(page.getNumberOfElements()).isEqualTo(1);
                });
            }
        }
    }

    @Nested
    class CustomAttributes {

        private FulltextJpaUuidDocumentEntity doc;
        private FulltextJpaUuidDocumentEntity doc2;

        @BeforeEach
        void setUp() {
            doc = FulltextJpaUuidDocumentEntity.withTitleAndAuthor("title of document 1", "author@email.com");
            setContentAndSave(doc, "/one.docx");

            doc2 = FulltextJpaUuidDocumentEntity.withTitleAndAuthor("title of document 2", "author@abc.com");
            setContentAndSave(doc2, "/one.docx");
        }

        @AfterEach
        void tearDown() throws IOException, SolrServerException {
            store.unsetContent(doc);
            docRepo.delete(doc);
            store.unsetContent(doc2);
            docRepo.delete(doc2);
            purgeSolrIndex();
        }

        @Test
        void shouldApplyProvidedAttributesAndFilterQuery() {
            var tmp = store.search("one");
            assertThat(tmp).isNotNull();

            List<UUID> results = new ArrayList<>();
            tmp.forEach(hit -> results.add(hit.getContentId()));

            assertThat(results).contains(doc.getContentId());
            assertThat(results).doesNotContain(doc2.getContentId());
        }
    }

    @Nested
    class CustomReturnTypes {

        private FulltextJpaUuidDocumentEntity doc;

        @BeforeEach
        void setUp() {
            doc = FulltextJpaUuidDocumentEntity.withTitleAndAuthor("title of document 1", "author@email.com");
            setContentAndSave(doc, "/one.docx");
        }

        @AfterEach
        void tearDown() throws IOException, SolrServerException {
            store.unsetContent(doc);
            docRepo.delete(doc);
            purgeSolrIndex();
        }

        @Test
        void shouldReturnResultsUsingReturnType() {
            UUID indexedContentId = contentId(doc);

            Awaitility.await().untilAsserted(() -> {
                var result = (store.search("one"));
                Iterator<FulltextSearchResult> iterator = result.iterator();
                assertThat(iterator.hasNext()).isTrue();
                assertThat(iterator.next()).satisfies(actual -> {
                    assertThat(actual).hasFieldOrPropertyWithValue("contentId", indexedContentId);
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
