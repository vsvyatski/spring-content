package org.springframework.content.solr;

import org.junit.jupiter.api.extension.ExtendWith;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.request.SolrQuery;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.fulltext.Attribute;
import org.springframework.content.commons.fulltext.Highlight;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.commons.search.Searchable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.repository.CrudRepository;
import org.springframework.test.context.ContextConfiguration;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import static java.lang.String.format;

@ContextConfiguration(classes = { SolrITConfig.class })
@ExtendWith(SpringExtension.class)
public class SolrIT {

    @Autowired
    private DocumentRepository docRepo;
    @Autowired
    private DocumentContentRepository docContentRepo;
    @Autowired
    private DocumentStoreSearchable store;
    @Autowired
    private SolrClient solr; //for tests
    @Autowired
    private SolrProperties solrProperties;
    private Document doc, doc2, doc3;
    private UUID id = null;

    
    @Nested
    class Index {
        @BeforeEach
        void setUp() throws Throwable {
            solrProperties.setUser("solr");
            solrProperties.setPassword("SolrRocks");

            doc = new Document();
            doc.setTitle("title of document 1");
            doc.setEmail("author@email.com");
            doc = docRepo.save(doc);
            doc = docContentRepo.setContent(doc, this.getClass().getResourceAsStream("/one.docx"));
            doc = docRepo.save(doc);

        }

        @AfterEach
        void tearDown() throws Throwable {
            if (docContentRepo != null) {
                docContentRepo.unsetContent(doc);
            }
            if (docRepo != null) {
                docRepo.delete(doc);
            }
            if (solr != null) {
                UpdateRequest req = new UpdateRequest();
                req.deleteByQuery("*");
                req.setBasicAuthCredentials(solrProperties.getUser(), solrProperties.getPassword());
                req.process(solr, null);
                req.commit(solr, null);
            }

        }

        @Test
        void shouldIndexTheContentOfThatDocument() throws Throwable {
            org.awaitility.Awaitility.await().atMost(java.time.Duration.ofSeconds(10)).untilAsserted(() -> {
                SolrQuery query = new SolrQuery();
                query.setQuery("foo");
                String fq = format("id:%s\\:%s", Document.class.getCanonicalName(), doc.getContentId());
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
            void setUp() throws Throwable {
                solrProperties.setUser("solr");
                solrProperties.setPassword("SolrRocks");

                doc = new Document();
                doc.setTitle("title of document 1");
                doc.setEmail("author@email.com");
                doc = docRepo.save(doc);
                doc = docContentRepo.setContent(doc, this.getClass().getResourceAsStream("/one.docx"));
                doc = docRepo.save(doc);

            }

            @AfterEach
            void tearDown() throws Throwable {
                if (docContentRepo != null) {
                    docContentRepo.unsetContent(doc);
                }
                if (docRepo != null) {
                    docRepo.delete(doc);
                }
                if (solr != null) {
                    UpdateRequest req = new UpdateRequest();
                    req.deleteByQuery("*");
                    req.setBasicAuthCredentials(solrProperties.getUser(), solrProperties.getPassword());
                    req.process(solr, null);
                    req.commit(solr, null);
                }

            }

            @Test
            void shouldReturnTheSearchedContent() throws Throwable {
                Iterable<UUID> content = docContentRepo.search("one");
                assertThat(content).contains(doc.getContentId());

            }

        }

        @Nested
        class GivenThatDocumentsContentIsUpdated {
            @BeforeEach
            void setUp() throws Throwable {
                solrProperties.setUser("solr");
                solrProperties.setPassword("SolrRocks");

                doc = new Document();
                doc.setTitle("title of document 1");
                doc.setEmail("author@email.com");
                doc = docRepo.save(doc);
                doc = docContentRepo.setContent(doc, this.getClass().getResourceAsStream("/one.docx"));
                doc = docRepo.save(doc);

                docContentRepo.setContent(doc, this.getClass().getResourceAsStream("/two.rtf"));
                docRepo.save(doc);

            }

            @AfterEach
            void tearDown() throws Throwable {
                if (docContentRepo != null) {
                    docContentRepo.unsetContent(doc);
                }
                if (docRepo != null) {
                    docRepo.delete(doc);
                }
                if (solr != null) {
                    UpdateRequest req = new UpdateRequest();
                    req.deleteByQuery("*");
                    req.setBasicAuthCredentials(solrProperties.getUser(), solrProperties.getPassword());
                    req.process(solr, null);
                    req.commit(solr, null);
                }

            }

            @Test
            void shouldIndexTheNewContent() throws Throwable {
                org.awaitility.Awaitility.await().atMost(java.time.Duration.ofSeconds(10)).untilAsserted(() -> {
                    SolrQuery query = new SolrQuery();
                    query.setQuery("bar");
                    String fq = format("id:%s\\:%s", Document.class.getCanonicalName(), doc.getContentId());
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
            void setUp() throws Throwable {
                solrProperties.setUser("solr");
                solrProperties.setPassword("SolrRocks");

                doc = new Document();
                doc.setTitle("title of document 1");
                doc.setEmail("author@email.com");
                doc = docRepo.save(doc);
                doc = docContentRepo.setContent(doc, this.getClass().getResourceAsStream("/one.docx"));
                doc = docRepo.save(doc);

                id = doc.getContentId();
                docContentRepo.unsetContent(doc);
                docRepo.delete(doc);

            }

            @AfterEach
            void tearDown() throws Throwable {
                if (docContentRepo != null) {
                    docContentRepo.unsetContent(doc);
                }
                if (docRepo != null) {
                    docRepo.delete(doc);
                }
                if (solr != null) {
                    UpdateRequest req = new UpdateRequest();
                    req.deleteByQuery("*");
                    req.setBasicAuthCredentials(solrProperties.getUser(), solrProperties.getPassword());
                    req.process(solr, null);
                    req.commit(solr, null);
                }

            }

            @Test
            void shouldDeleteTheRecordOfTheContentFromTheIndex() throws Throwable {
                SolrQuery query = new SolrQuery();
                query.setQuery("one");
                query.addFilterQuery("id:" + "examples.models.Document\\:" + id);
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
            void setUp() throws Throwable {
                solrProperties.setUser(System.getenv("SOLR_USER"));
                solrProperties.setPassword(System.getenv("SOLR_PASSWORD"));

                for (int i=0; i < 10; i++) {
                    Document doc = new Document();
                    doc.setTitle(format("doc %s", i));
                    doc.setEmail("author@email.com");
                    doc = docContentRepo.setContent(doc, this.getClass().getResourceAsStream("/one.docx"));
                    docRepo.save(doc);
                }

            }

            @AfterEach
            void tearDown() throws Throwable {
                if (docRepo != null && docContentRepo != null) {
                    for (Document doc : docRepo.findAll()) {
                        doc = docContentRepo.unsetContent(doc);
                    }
                    for (Document doc : docRepo.findAll()) {
                        docRepo.deleteById(doc.getId());
                    }
                }
                if (solr != null) {
                    UpdateRequest req = new UpdateRequest();
                    req.deleteByQuery("*");
                    req.setBasicAuthCredentials(solrProperties.getUser(), solrProperties.getPassword());
                    req.process(solr, null);
                    req.commit(solr, null);
                }

            }

            @Test
            void shouldReturnResultsInPages() throws Throwable {
                org.awaitility.Awaitility.await().untilAsserted(() -> {
    var page = (docContentRepo.search("foo", PageRequest.of(0, 3)));
    assertThat(page.getNumberOfElements()).isEqualTo(3);

});

                org.awaitility.Awaitility.await().untilAsserted(() -> {
    var page = (docContentRepo.search("foo", PageRequest.of(1, 3)));
    assertThat(page.getNumberOfElements()).isEqualTo(3);

});

                org.awaitility.Awaitility.await().untilAsserted(() -> {
    var page = (docContentRepo.search("foo", PageRequest.of(2, 3)));
    assertThat(page.getNumberOfElements()).isEqualTo(3);

});

                org.awaitility.Awaitility.await().untilAsserted(() -> {
    var page = (docContentRepo.search("foo", PageRequest.of(3, 3)));
    assertThat(page.getNumberOfElements()).isEqualTo(1);

});

            }

            @Test
            void shouldReturnSpecificResultPage() throws Throwable {
                org.awaitility.Awaitility.await().untilAsserted(() -> {
    var page = (docContentRepo.search("foo", PageRequest.of(3, 3)));
    assertThat(page.getNumberOfElements()).isEqualTo(1);

});

            }

        }

    }

    @Nested
    class CustomAttributes {
        @BeforeEach
        void setUp() throws Throwable {
            solrProperties.setUser(System.getenv("SOLR_USER"));
            solrProperties.setPassword(System.getenv("SOLR_PASSWORD"));

            doc = new Document();
            doc.setTitle("title of document 1");
            doc.setEmail("author@email.com");
            store.setContent(doc, this.getClass().getResourceAsStream("/one.docx"));
            doc = docRepo.save(doc);

            doc2 = new Document();
            doc2.setTitle("title of document 2");
            doc2.setEmail("author@abc.com");
            store.setContent(doc2, this.getClass().getResourceAsStream("/one.docx"));
            doc2 = docRepo.save(doc2);

        }

        @AfterEach
        void tearDown() throws Throwable {
            if (docContentRepo != null) {
                docContentRepo.unsetContent(doc);
            }
            if (docRepo != null) {
                docRepo.delete(doc);
            }
            if (solr != null) {
                UpdateRequest req = new UpdateRequest();
                req.deleteByQuery("*");
                req.setBasicAuthCredentials(solrProperties.getUser(), solrProperties.getPassword());
                req.process(solr, null);
                req.commit(solr, null);
            }

        }

        @Test
        void shouldApplyTheProvidedAttributesAndFilterQuery() throws Throwable {
            Iterable<UUID> tmp = docContentRepo.search("one");
            assertThat(tmp).isNotNull();

            List<UUID> results = new ArrayList<UUID>();
            tmp.forEach(results::add);

            assertThat(results).contains(doc.getContentId());
            assertThat(results).doesNotContain(doc2.getContentId());

        }

    }

    @Nested
    class CustomReturnTypes {
        @BeforeEach
        void setUp() throws Throwable {
            solrProperties.setUser(System.getenv("SOLR_USER"));
            solrProperties.setPassword(System.getenv("SOLR_PASSWORD"));

            doc = new Document();
            doc.setTitle("title of document 1");
            doc.setEmail("author@email.com");
            doc = docRepo.save(doc);
            doc = store.setContent(doc, this.getClass().getResourceAsStream("/one.docx"));

        }

        @AfterEach
        void tearDown() throws Throwable {
            if (docContentRepo != null) {
                docContentRepo.unsetContent(doc);
            }
            if (docRepo != null) {
                docRepo.delete(doc);
            }
            if (solr != null) {
                UpdateRequest req = new UpdateRequest();
                req.deleteByQuery("*");
                req.setBasicAuthCredentials(solrProperties.getUser(), solrProperties.getPassword());
                req.process(solr, null);
                req.commit(solr, null);
            }

        }

        @Test
        void shouldReturnResultsUsingTheReturnType() throws Throwable {
            org.awaitility.Awaitility.await().untilAsserted(() -> {
    var result = (store.search("one"));
    Iterator<FulltextInfo> iterator = result.iterator();
    assertThat(iterator.hasNext()).isTrue();
    assertThat(iterator.next()).satisfies(actual -> { assertThat(actual).hasFieldOrPropertyWithValue("contentId", doc.getContentId()); assertThat(actual).extracting("highlight", org.assertj.core.api.InstanceOfAssertFactories.STRING).contains("<em>one</em>"); assertThat(actual).extracting("email", org.assertj.core.api.InstanceOfAssertFactories.STRING).contains("author@email.com"); });
    assertThat(iterator.hasNext()).isFalse();

});

        }

    }


    @Entity
    public static class Document {

        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        private Long id;

        private String title;
        private String email;

        @ContentId
        private UUID contentId;

        public Document() {
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public UUID getContentId() {
            return contentId;
        }

        public void setContentId(UUID contentId) {
            this.contentId = contentId;
        }
    }

    public interface DocumentRepository extends CrudRepository<Document, Long> {
    }

    public interface DocumentContentRepository extends ContentStore<Document, UUID>, Searchable<UUID> {
    }

    public interface DocumentStoreSearchable extends ContentStore<Document, UUID>, Searchable<FulltextInfo> {
    }

    public static class FulltextInfo {

        @Id
        private Long id;

        @ContentId
        private UUID contentId;

        @Highlight
        private String highlight;

        @Attribute(name = "email")
        private String email;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public UUID getContentId() {
            return contentId;
        }

        public void setContentId(UUID contentId) {
            this.contentId = contentId;
        }

        public String getHighlight() {
            return highlight;
        }

        public void setHighlight(String highlight) {
            this.highlight = highlight;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }
}