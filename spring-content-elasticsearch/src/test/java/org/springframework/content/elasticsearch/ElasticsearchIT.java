package org.springframework.content.elasticsearch;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.TestFactory;
import static org.assertj.core.api.Assertions.assertThat;

import static java.lang.String.format;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Iterator;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import org.apache.commons.io.IOUtils;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.fulltext.Attribute;
import org.springframework.content.commons.fulltext.Highlight;
import org.springframework.content.commons.renditions.Renderable;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.commons.search.IndexService;
import org.springframework.content.commons.search.Searchable;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.repository.CrudRepository;

public class ElasticsearchIT {

    private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

    private DocumentRepository repo;
    private DocumentContentStore store;
    private DocumentStoreSearchable searchableStore;

    private ElasticsearchClient client;

    private Document doc1, doc2;
    private UUID id1, id2 = null;

    private String indexName;

    private static Class<?>[] indexStrategyContexts = new Class<?>[]{GlobalIndexingStrategy.class/*, EntityIndexingStrategy.class*/};

    @TestFactory
    java.util.stream.Stream<org.junit.jupiter.api.DynamicNode> generatedCases() {
        java.util.List<org.junit.jupiter.api.DynamicNode> tests = new java.util.ArrayList<>();
        for (Class<?> indexStrategyContext : indexStrategyContexts) {
            {
                java.util.List<org.junit.jupiter.api.DynamicNode> nodes5 = new java.util.ArrayList<>();
                {
                    java.util.List<org.junit.jupiter.api.DynamicNode> nodes4 = new java.util.ArrayList<>();
                    nodes4.add(org.junit.jupiter.api.DynamicTest.dynamicTest("should index the documents", () -> {
                        try {
                            context = new AnnotationConfigApplicationContext();
                                                context.register(indexStrategyContext);
                                                context.register(ElasticsearchConfig.class);
                                                context.refresh();

                                                repo = context.getBean(DocumentRepository.class);
                                                store = context.getBean(DocumentContentStore.class);
                                                client = context.getBean(ElasticsearchClient.class);
                                                ((IndexingStrategy)context.getBean(indexStrategyContext)).setup();
                                                indexName = ((IndexingStrategy)context.getBean(indexStrategyContext)).indexName();

                            doc1 = new Document();
                                                    doc1.setTitle("doc 1");
                                                    doc1.setAuthor("author@email.com");
                                                    store.setContent(doc1, this.getClass().getResourceAsStream("/one.docx"));
                                                    doc1 = repo.save(doc1);

                                                    doc2 = new Document();
                                                    doc2.setTitle("doc 2");
                                                    doc2.setAuthor("author@email.com");
                                                    store.setContent(doc2, this.getClass().getResourceAsStream("/two.rtf"));
                                                    doc2 = repo.save(doc2);
                            var res = client.get(g -> g.index(indexName).id(doc1.getContentId().toString()), Void.class);
                                                    assertThat(res.found()).isTrue();

                                                    res = client.get(g -> g.index(indexName).id(doc2.getContentId().toString()), Void.class);
                                                    assertThat(res.found()).isTrue();
                        } finally {
                            if (doc1 != null) {
                                                        store.unsetContent(doc1);
                                                        repo.delete(doc1);
                                                    }

                                                    if (doc2 != null) {
                                                        store.unsetContent(doc2);
                                                        repo.delete(doc2);
                                                    }

                            assertThat(context).isNotNull();

                                                try {
                                                    // assert the right index exists as a double check we are testing the correct thing!
                                                    if (client != null) {
                                                        var resp = client.indices().get(g -> g.index(indexName));
                                                        assertThat(resp.indices().size()).isEqualTo(1);
                                                    }
                                                } catch (ElasticsearchException ese) {}

                                                try {
                                                    client.indices().delete(d -> d.index("_all"));
                                                } catch (ElasticsearchException ese) {}
                        }
                    }));
                    nodes4.add(org.junit.jupiter.api.DynamicTest.dynamicTest("should be possible to close the index", () -> {
                        try {
                            context = new AnnotationConfigApplicationContext();
                                                context.register(indexStrategyContext);
                                                context.register(ElasticsearchConfig.class);
                                                context.refresh();

                                                repo = context.getBean(DocumentRepository.class);
                                                store = context.getBean(DocumentContentStore.class);
                                                client = context.getBean(ElasticsearchClient.class);
                                                ((IndexingStrategy)context.getBean(indexStrategyContext)).setup();
                                                indexName = ((IndexingStrategy)context.getBean(indexStrategyContext)).indexName();

                            doc1 = new Document();
                                                    doc1.setTitle("doc 1");
                                                    doc1.setAuthor("author@email.com");
                                                    store.setContent(doc1, this.getClass().getResourceAsStream("/one.docx"));
                                                    doc1 = repo.save(doc1);

                                                    doc2 = new Document();
                                                    doc2.setTitle("doc 2");
                                                    doc2.setAuthor("author@email.com");
                                                    store.setContent(doc2, this.getClass().getResourceAsStream("/two.rtf"));
                                                    doc2 = repo.save(doc2);
                            IndexService indexer = (context.getBean(IndexService.class));
                                                    indexer.index(doc1, new ByteArrayInputStream("customized index".getBytes()));

                                                    var resp = client.indices().close(c -> c.index(indexName));
                                                    assertThat(resp.acknowledged()).isTrue();

                                                    String command = format("curl -X GET %s/_cat/indices/%s?h=status", ElasticsearchTestContainer.getUrl(), indexName);
                                                    Process process = Runtime.getRuntime().exec(command);

                                                    InputStream inputStream = process.getInputStream();
                                                    process.waitFor();

                                                    int exitCode = process.exitValue();
                                                    assertThat(exitCode).isEqualTo(0);

                                                    assertThat(IOUtils.toString(inputStream)).contains("close");
                        } finally {
                            if (doc1 != null) {
                                                        store.unsetContent(doc1);
                                                        repo.delete(doc1);
                                                    }

                                                    if (doc2 != null) {
                                                        store.unsetContent(doc2);
                                                        repo.delete(doc2);
                                                    }

                            assertThat(context).isNotNull();

                                                try {
                                                    // assert the right index exists as a double check we are testing the correct thing!
                                                    if (client != null) {
                                                        var resp = client.indices().get(g -> g.index(indexName));
                                                        assertThat(resp.indices().size()).isEqualTo(1);
                                                    }
                                                } catch (ElasticsearchException ese) {}

                                                try {
                                                    client.indices().delete(d -> d.index("_all"));
                                                } catch (ElasticsearchException ese) {}
                        }
                    }));
                    {
                        java.util.List<org.junit.jupiter.api.DynamicNode> nodes1 = new java.util.ArrayList<>();
                        nodes1.add(org.junit.jupiter.api.DynamicTest.dynamicTest("should return the matches", () -> {
                            try {
                                context = new AnnotationConfigApplicationContext();
                                                    context.register(indexStrategyContext);
                                                    context.register(ElasticsearchConfig.class);
                                                    context.refresh();

                                                    repo = context.getBean(DocumentRepository.class);
                                                    store = context.getBean(DocumentContentStore.class);
                                                    client = context.getBean(ElasticsearchClient.class);
                                                    ((IndexingStrategy)context.getBean(indexStrategyContext)).setup();
                                                    indexName = ((IndexingStrategy)context.getBean(indexStrategyContext)).indexName();

                                doc1 = new Document();
                                                        doc1.setTitle("doc 1");
                                                        doc1.setAuthor("author@email.com");
                                                        store.setContent(doc1, this.getClass().getResourceAsStream("/one.docx"));
                                                        doc1 = repo.save(doc1);

                                                        doc2 = new Document();
                                                        doc2.setTitle("doc 2");
                                                        doc2.setAuthor("author@email.com");
                                                        store.setContent(doc2, this.getClass().getResourceAsStream("/two.rtf"));
                                                        doc2 = repo.save(doc2);
                                org.awaitility.Awaitility.await().atMost(java.time.Duration.ofSeconds(10)).untilAsserted(() -> {
                                var result = store.search("one");
                                assertThat(result).contains(doc1.getContentId());
                                assertThat(result).doesNotContain(doc2.getContentId());
                                });

                                                            org.awaitility.Awaitility.await().atMost(java.time.Duration.ofSeconds(10)).untilAsserted(() -> {
                                var result = store.search("two");
                                assertThat(result).doesNotContain(doc1.getContentId());
                                assertThat(result).contains(doc2.getContentId());
                                });

                                                            org.awaitility.Awaitility.await().atMost(java.time.Duration.ofSeconds(10)).untilAsserted(() -> {
                                var result = store.search("one two");
                                assertThat(result).contains(doc1.getContentId(), doc2.getContentId());
                                });

                                                            org.awaitility.Awaitility.await().atMost(java.time.Duration.ofSeconds(10)).untilAsserted(() -> {
                                var result = store.search("+document +one -two");
                                assertThat(result).contains(doc1.getContentId());
                                assertThat(result).doesNotContain(doc2.getContentId());
                                });
                            } finally {
                                if (doc1 != null) {
                                                            store.unsetContent(doc1);
                                                            repo.delete(doc1);
                                                        }

                                                        if (doc2 != null) {
                                                            store.unsetContent(doc2);
                                                            repo.delete(doc2);
                                                        }

                                assertThat(context).isNotNull();

                                                    try {
                                                        // assert the right index exists as a double check we are testing the correct thing!
                                                        if (client != null) {
                                                            var resp = client.indices().get(g -> g.index(indexName));
                                                            assertThat(resp.indices().size()).isEqualTo(1);
                                                        }
                                                    } catch (ElasticsearchException ese) {}

                                                    try {
                                                        client.indices().delete(d -> d.index("_all"));
                                                    } catch (ElasticsearchException ese) {}
                            }
                        }));
                        nodes1.add(org.junit.jupiter.api.DynamicContainer.dynamicContainer("when the content is searched", nodes1.stream()));
                    }
                    {
                        java.util.List<org.junit.jupiter.api.DynamicNode> nodes2 = new java.util.ArrayList<>();
                        nodes2.add(org.junit.jupiter.api.DynamicTest.dynamicTest("should index the documents", () -> {
                            try {
                                context = new AnnotationConfigApplicationContext();
                                                    context.register(indexStrategyContext);
                                                    context.register(ElasticsearchConfig.class);
                                                    context.refresh();

                                                    repo = context.getBean(DocumentRepository.class);
                                                    store = context.getBean(DocumentContentStore.class);
                                                    client = context.getBean(ElasticsearchClient.class);
                                                    ((IndexingStrategy)context.getBean(indexStrategyContext)).setup();
                                                    indexName = ((IndexingStrategy)context.getBean(indexStrategyContext)).indexName();

                                doc1 = new Document();
                                                        doc1.setTitle("doc 1");
                                                        doc1.setAuthor("author@email.com");
                                                        store.setContent(doc1, this.getClass().getResourceAsStream("/one.docx"));
                                                        doc1 = repo.save(doc1);

                                                        doc2 = new Document();
                                                        doc2.setTitle("doc 2");
                                                        doc2.setAuthor("author@email.com");
                                                        store.setContent(doc2, this.getClass().getResourceAsStream("/two.rtf"));
                                                        doc2 = repo.save(doc2);

                                doc1 = new Document();
                                                            doc1.setTitle("doc 1");
                                                            doc1.setAuthor("author@email.com");
                                                            doc1.setMimeType("image/png");
                                                            doc1 = store.setContent(doc1, this.getClass().getResourceAsStream("/image.png"));
                                                            doc1 = repo.save(doc1);
                                org.awaitility.Awaitility.await().atMost(java.time.Duration.ofSeconds(10)).untilAsserted(() -> {
                                var result = store.search("wisdom");
                                assertThat(result).contains(doc1.getContentId());
                                });
                            } finally {
                                if (doc1 != null) {
                                                            store.unsetContent(doc1);
                                                            repo.delete(doc1);
                                                        }

                                                        if (doc2 != null) {
                                                            store.unsetContent(doc2);
                                                            repo.delete(doc2);
                                                        }

                                assertThat(context).isNotNull();

                                                    try {
                                                        // assert the right index exists as a double check we are testing the correct thing!
                                                        if (client != null) {
                                                            var resp = client.indices().get(g -> g.index(indexName));
                                                            assertThat(resp.indices().size()).isEqualTo(1);
                                                        }
                                                    } catch (ElasticsearchException ese) {}

                                                    try {
                                                        client.indices().delete(d -> d.index("_all"));
                                                    } catch (ElasticsearchException ese) {}
                            }
                        }));
                        nodes2.add(org.junit.jupiter.api.DynamicContainer.dynamicContainer("given a text extracting renderer", nodes2.stream()));
                    }
                    {
                        java.util.List<org.junit.jupiter.api.DynamicNode> nodes3 = new java.util.ArrayList<>();
                        nodes3.add(org.junit.jupiter.api.DynamicTest.dynamicTest("should delete the record of the content from the index", () -> {
                            try {
                                context = new AnnotationConfigApplicationContext();
                                                    context.register(indexStrategyContext);
                                                    context.register(ElasticsearchConfig.class);
                                                    context.refresh();

                                                    repo = context.getBean(DocumentRepository.class);
                                                    store = context.getBean(DocumentContentStore.class);
                                                    client = context.getBean(ElasticsearchClient.class);
                                                    ((IndexingStrategy)context.getBean(indexStrategyContext)).setup();
                                                    indexName = ((IndexingStrategy)context.getBean(indexStrategyContext)).indexName();

                                doc1 = new Document();
                                                        doc1.setTitle("doc 1");
                                                        doc1.setAuthor("author@email.com");
                                                        store.setContent(doc1, this.getClass().getResourceAsStream("/one.docx"));
                                                        doc1 = repo.save(doc1);

                                                        doc2 = new Document();
                                                        doc2.setTitle("doc 2");
                                                        doc2.setAuthor("author@email.com");
                                                        store.setContent(doc2, this.getClass().getResourceAsStream("/two.rtf"));
                                                        doc2 = repo.save(doc2);

                                id1 = doc1.getContentId();
                                                            store.unsetContent(doc1);
                                                            repo.delete(doc1);

                                                            id2 = doc2.getContentId();
                                                            store.unsetContent(doc2);
                                                            repo.delete(doc2);
                                var res = client.get(g -> g.index(indexName).id(id1.toString()), Void.class);
                                                            assertThat(res.found()).isFalse();

                                                            res = client.get(g -> g.index(indexName).id(id2.toString()), Void.class);
                                                            assertThat(res.found()).isFalse();
                            } finally {
                                doc1 = null;
                                                            doc2 = null;

                                if (doc1 != null) {
                                                            store.unsetContent(doc1);
                                                            repo.delete(doc1);
                                                        }

                                                        if (doc2 != null) {
                                                            store.unsetContent(doc2);
                                                            repo.delete(doc2);
                                                        }

                                assertThat(context).isNotNull();

                                                    try {
                                                        // assert the right index exists as a double check we are testing the correct thing!
                                                        if (client != null) {
                                                            var resp = client.indices().get(g -> g.index(indexName));
                                                            assertThat(resp.indices().size()).isEqualTo(1);
                                                        }
                                                    } catch (ElasticsearchException ese) {}

                                                    try {
                                                        client.indices().delete(d -> d.index("_all"));
                                                    } catch (ElasticsearchException ese) {}
                            }
                        }));
                        nodes3.add(org.junit.jupiter.api.DynamicContainer.dynamicContainer("given that document is deleted", nodes3.stream()));
                    }
                    nodes4.add(org.junit.jupiter.api.DynamicContainer.dynamicContainer("given some documents", nodes4.stream()));
                }
                tests.add(org.junit.jupiter.api.DynamicContainer.dynamicContainer(format("Index Strategy %s", strategyName(indexStrategyContext)), nodes5.stream()));
            }
        }
        return tests.stream();
    }
    @Nested
    class PagingCases {
        @BeforeEach
        void setUp() {
            context = new AnnotationConfigApplicationContext();
                            context.register(EntityIndexingStrategy.class);
                            context.register(ElasticsearchConfig.class);
                            context.refresh();

                            repo = context.getBean(DocumentRepository.class);
                            store = context.getBean(DocumentContentStore.class);
                            client = context.getBean(ElasticsearchClient.class);

                            for (int i=0; i < 10; i++) {
                                Document doc = new Document();
                                doc.setTitle(format("doc %s", i));
                                doc = store.setContent(doc, this.getClass().getResourceAsStream("/one.docx"));
                                repo.save(doc);
                            }
        }
        @AfterEach
        void tearDown() throws IOException {
            assertThat(context).isNotNull();

                            if (client != null) {
                                client.indices().delete(d -> d.index("_all"));
                            }
        }
        @Test
        void shouldReturnResultsInPages() {
            org.awaitility.Awaitility.await().atMost(java.time.Duration.ofSeconds(10)).untilAsserted(() -> {
            var page = store.search("one", PageRequest.of(0, 3));
            assertThat(page.getTotalElements()).isEqualTo(10L);
                                        assertThat(page.getTotalPages()).isEqualTo(4);
                                        assertThat(page.getNumberOfElements()).isEqualTo(3);
                                        assertThat(page.getContent().size()).isEqualTo(3);
            });

                            org.awaitility.Awaitility.await().atMost(java.time.Duration.ofSeconds(10)).untilAsserted(() -> {
            var page = store.search("one", PageRequest.of(1, 3));
            assertThat(page.getTotalElements()).isEqualTo(10L);
                                        assertThat(page.getTotalPages()).isEqualTo(4);
                                        assertThat(page.getNumberOfElements()).isEqualTo(3);
                                        assertThat(page.getContent().size()).isEqualTo(3);
            });

                            org.awaitility.Awaitility.await().atMost(java.time.Duration.ofSeconds(10)).untilAsserted(() -> {
            var page = store.search("one", PageRequest.of(2, 3));
            assertThat(page.getTotalElements()).isEqualTo(10L);
                                        assertThat(page.getTotalPages()).isEqualTo(4);
                                        assertThat(page.getNumberOfElements()).isEqualTo(3);
                                        assertThat(page.getContent().size()).isEqualTo(3);
            });

                            org.awaitility.Awaitility.await().atMost(java.time.Duration.ofSeconds(10)).untilAsserted(() -> {
            var page = store.search("one", PageRequest.of(3, 3));
            assertThat(page.getTotalElements()).isEqualTo(10L);
                                        assertThat(page.getTotalPages()).isEqualTo(4);
                                        assertThat(page.getNumberOfElements()).isEqualTo(1);
                                        assertThat(page.getContent().size()).isEqualTo(1);
            });
        }
    }
    @Nested
    class CustomAttributesCases {
        @Nested
        class GivenAContextConfiguredToSyncAttributesAndProvideAFilterQuery {
            @BeforeEach
            void setUp() {
                context = new AnnotationConfigApplicationContext();
                                    context.register(EntityIndexingStrategy.class);
                                    context.register(CustomAttributesConfig.class);
                                    context.refresh();

                                    repo = context.getBean(DocumentRepository.class);
                                    store = context.getBean(DocumentContentStore.class);
                                    client = context.getBean(ElasticsearchClient.class);

                                    doc1 = new Document();
                                    doc1.setTitle(format("doc 1"));
                                    doc1.setAuthor("Buck Rogers");
                                    doc1 = store.setContent(doc1, this.getClass().getResourceAsStream("/one.docx"));
                                    repo.save(doc1);

                                    doc2 = new Document();
                                    doc2.setTitle(format("doc 2"));
                                    doc1.setAuthor("Wilma Deering");
                                    doc2 = store.setContent(doc2, this.getClass().getResourceAsStream("/one.docx"));
                                    repo.save(doc2);
            }
            @AfterEach
            void tearDown() throws IOException {
                assertThat(context).isNotNull();

                                    if (client != null) {
                                        client.indices().delete(d -> d.index("_all"));
                                    }
            }
            @Test
            void shouldReturnTheSpecifiedAttributes() {
                org.awaitility.Awaitility.await().atMost(java.time.Duration.ofSeconds(10)).untilAsserted(() -> {
                    assertThat(store.search("one", PageRequest.of(0, 10)))
                            .contains(doc1.getContentId())
                            .doesNotContain(doc2.getContentId());
                });
            }
        }
    }
    @Nested
    class CustomReturnTypesCases {
        @BeforeEach
        void setUp() {
            context = new AnnotationConfigApplicationContext();
                            context.register(EntityIndexingStrategy.class);
                            context.register(CustomAttributesConfig.class);
                            context.refresh();

                            repo = context.getBean(DocumentRepository.class);
                            searchableStore = context.getBean(DocumentStoreSearchable.class);
                            client = context.getBean(ElasticsearchClient.class);

                            doc1 = new Document();
                            doc1.setTitle(format("A document about one"));
                            doc1.setAuthor("Buck Rogers");
                            doc1 = searchableStore.setContent(doc1, this.getClass().getResourceAsStream("/one.docx"));
                            repo.save(doc1);
        }
        @AfterEach
        void tearDown() throws IOException {
            assertThat(context).isNotNull();

                            if (client != null) {
                                client.indices().delete(d -> d.index("_all"));
                            }
        }
        @Test
        void shouldReturnResultsUsingTheCustomReturnType() {
            org.awaitility.Awaitility.await().atMost(java.time.Duration.ofSeconds(10)).untilAsserted(() -> {
            var result = searchableStore.search("one");
            assertThat(result).isNotNull();
                                    Iterator<FulltextInfo> iterator = result.iterator();
                                    assertThat(iterator.hasNext()).isTrue();
                                    FulltextInfo info = iterator.next();
                                    assertThat(info).extracting("contentId").isEqualTo(doc1.getContentId());
                                    assertThat(info).extracting("highlight").asString().contains("<em>one</em>");
                                    assertThat(info).extracting("author").asString().contains("Buck Rogers");
                                    assertThat(iterator.hasNext()).isFalse();
            });
        }
    }

    private String strategyName(Class<?> indexStrategyContext) {
        return indexStrategyContext.getSimpleName();
    }

    public interface DocumentRepository extends CrudRepository<Document, Long> {
        //
    }

    public interface DocumentContentStore extends ContentStore<Document, UUID>, Searchable<UUID>, Renderable<Document> {
        //
    }

    public interface DocumentStoreSearchable extends ContentStore<Document, UUID>, Searchable<FulltextInfo> {
    }

    @Entity
    public static class Document {

        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        private Long id;

        @ContentId
        private UUID contentId;

        @MimeType
        private String mimeType;

        private String title;
        private String author;

        public Document() {
        }

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

        public String getMimeType() {
            return mimeType;
        }

        public void setMimeType(String mimeType) {
            this.mimeType = mimeType;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getAuthor() {
            return author;
        }

        public void setAuthor(String author) {
            this.author = author;
        }
    }

    public static class FulltextInfo {

        @ContentId
        private UUID contentId;

        @Highlight
        private String highlight;

        @Attribute(name = "author")
        private String author;

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

        public String getAuthor() {
            return author;
        }

        public void setAuthor(String author) {
            this.author = author;
        }
    }
}
