package org.springframework.content.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import internal.org.springframework.content.elasticsearch.IndexManager;
import org.apache.commons.io.IOUtils;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.*;
import org.springframework.content.commons.search.IndexService;
import org.springframework.content.test.support.fulltext.FulltextJpaUuidDocumentEntity;
import org.springframework.content.test.support.fulltext.FulltextJpaUuidDocumentRepository;
import org.springframework.content.test.support.fulltext.FulltextJpaUuidDocumentSearchableStore;
import org.springframework.content.test.support.fulltext.FulltextSearchResult;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.domain.PageRequest;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Objects;
import java.util.UUID;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

public class ElasticsearchIT {

    private AnnotationConfigApplicationContext context;

    private FulltextJpaUuidDocumentRepository repo;
    private FulltextJpaUuidDocumentSearchableStore store;
    private FulltextJpaUuidDocumentSearchableStore searchableStore;

    private ElasticsearchClient client;

    private String indexName;

    @SuppressWarnings("unchecked")
    private static IndexService<FulltextJpaUuidDocumentEntity> indexService(AnnotationConfigApplicationContext context) {
        return (IndexService<FulltextJpaUuidDocumentEntity>) context.getBean(IndexService.class);
    }

    private static InputStream testResource(String path) {
        return Objects.requireNonNull(ElasticsearchIT.class.getResourceAsStream(path),
            () -> "Missing classpath test resource: " + path);
    }

    private static UUID contentId(FulltextJpaUuidDocumentEntity entity) {
        return Objects.requireNonNull(entity.getContentId(), "contentId");
    }

    private static String contentIdString(FulltextJpaUuidDocumentEntity entity) {
        return contentId(entity).toString();
    }

    private static void deleteIndexIfExists(ElasticsearchClient client, String name) throws IOException {
        if (client.indices().exists(e -> e.index(name)).value()) {
            client.indices().delete(d -> d.index(name));
        }
    }

    private static String entityIndexName() {
        return FulltextJpaUuidDocumentEntity.class.getName().toLowerCase();
    }

    private static void tearDownEntityElasticsearch(ElasticsearchClient client) throws IOException {
        deleteIndexIfExists(client, entityIndexName());
        deleteIndexIfExists(client, IndexManager.INDEX_NAME);
    }

    private void closeContext() {
        context.close();
    }

    private void tearDownEntityIndexingScenario() throws IOException {
        try {
            tearDownEntityElasticsearch(client);
        } finally {
            closeContext();
        }
    }

    private void tearDownGlobalIndexingScenario() throws IOException {
        try {
            deleteIndexIfExists(client, indexName);
        } finally {
            closeContext();
        }
    }

    private void refreshElasticsearchContext(Class<?>... configurationClasses) {
        context = new AnnotationConfigApplicationContext();
        for (Class<?> configurationClass : configurationClasses) {
            context.register(configurationClass);
        }
        context.refresh();

        repo = context.getBean(FulltextJpaUuidDocumentRepository.class);
        store = context.getBean(FulltextJpaUuidDocumentSearchableStore.class);
        searchableStore = store;
        client = context.getBean(ElasticsearchClient.class);
    }

    private void setUpGlobalIndexingContext() throws Exception {
        refreshElasticsearchContext(GlobalIndexingStrategy.class, ElasticsearchConfig.class);
        GlobalIndexingStrategy globalIndexing = context.getBean(GlobalIndexingStrategy.class);
        globalIndexing.setup();
        indexName = globalIndexing.indexName();
    }

    private void setUpEntityIndexingContext() {
        refreshElasticsearchContext(EntityIndexingStrategy.class, ElasticsearchConfig.class);
    }

    private void setUpEntityIndexingContextWithCustomAttributes() {
        refreshElasticsearchContext(EntityIndexingStrategy.class, CustomAttributesConfig.class);
    }

    private boolean isIndexedInGlobalIndex(UUID contentId) {
        try {
            return client.get(g -> g.index(indexName).id(contentId.toString()), Void.class).found();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Nested
    class GlobalIndexingStrategyScenarios {

        @BeforeEach
        void setUp() throws Exception {
            setUpGlobalIndexingContext();
        }

        @AfterEach
        void tearDown() throws IOException {
            tearDownGlobalIndexingScenario();
        }

        @Test
        void shouldIndexDocuments() throws Exception {
            FulltextJpaUuidDocumentEntity wordDoc =
                FulltextJpaUuidDocumentEntity.withTitleAndAuthor("doc 1", "author@email.com");
            store.setContent(wordDoc, testResource("/one.docx"));
            repo.save(wordDoc);

            FulltextJpaUuidDocumentEntity rtfDoc =
                FulltextJpaUuidDocumentEntity.withTitleAndAuthor("doc 2", "author@email.com");
            store.setContent(rtfDoc, testResource("/two.rtf"));
            repo.save(rtfDoc);

            String index = indexName;
            var res = client.get(g -> g.index(index).id(contentIdString(wordDoc)), Void.class);
            assertThat(res.found()).isTrue();

            res = client.get(g -> g.index(index).id(contentIdString(rtfDoc)), Void.class);
            assertThat(res.found()).isTrue();
        }

        @Test
        void shouldBePossibleToCloseIndex() throws Exception {
            FulltextJpaUuidDocumentEntity wordDoc =
                FulltextJpaUuidDocumentEntity.withTitleAndAuthor("doc 1", "author@email.com");
            store.setContent(wordDoc, testResource("/one.docx"));
            repo.save(wordDoc);

            FulltextJpaUuidDocumentEntity rtfDoc =
                FulltextJpaUuidDocumentEntity.withTitleAndAuthor("doc 2", "author@email.com");
            store.setContent(rtfDoc, testResource("/two.rtf"));
            repo.save(rtfDoc);
            IndexService<FulltextJpaUuidDocumentEntity> indexer = indexService(context);
            indexer.index(wordDoc, new ByteArrayInputStream("customized index".getBytes()));

            var resp = client.indices().close(c -> c.index(indexName));
            assertThat(resp.acknowledged()).isTrue();

            String statusUrl = format("%s/_cat/indices/%s?h=status", ElasticsearchTestContainer.getUrl(), indexName);
            String elasticUser = format("elastic:%s", ElasticsearchContainer.ELASTICSEARCH_DEFAULT_PASSWORD);
            Process process = new ProcessBuilder("curl", "-k", "-u", elasticUser, "-X", "GET", statusUrl).start();

            InputStream inputStream = process.getInputStream();
            process.waitFor();

            assertThat(process.exitValue()).isEqualTo(0);
            assertThat(IOUtils.toString(inputStream, StandardCharsets.UTF_8)).contains("close");
        }

        @Test
        void shouldReturnMatches() {
            FulltextJpaUuidDocumentEntity wordDoc =
                FulltextJpaUuidDocumentEntity.withTitleAndAuthor("doc 1", "author@email.com");
            store.setContent(wordDoc, testResource("/one.docx"));
            repo.save(wordDoc);

            FulltextJpaUuidDocumentEntity rtfDoc =
                FulltextJpaUuidDocumentEntity.withTitleAndAuthor("doc 2", "author@email.com");
            store.setContent(rtfDoc, testResource("/two.rtf"));
            repo.save(rtfDoc);

            UUID wordContentId = contentId(wordDoc);
            UUID rtfContentId = contentId(rtfDoc);

            Awaitility.await().atMost(java.time.Duration.ofSeconds(10)).untilAsserted(() -> {
                var result = store.search("one");
                assertThat(result).extracting(FulltextSearchResult::getContentId)
                    .contains(wordContentId)
                    .doesNotContain(rtfContentId);
            });

            Awaitility.await().atMost(java.time.Duration.ofSeconds(10)).untilAsserted(() -> {
                var result = store.search("two");
                assertThat(result).extracting(FulltextSearchResult::getContentId)
                    .doesNotContain(wordContentId)
                    .contains(rtfContentId);
            });

            Awaitility.await().atMost(java.time.Duration.ofSeconds(10)).untilAsserted(() -> {
                var result = store.search("one two");
                assertThat(result).extracting(FulltextSearchResult::getContentId)
                    .contains(wordContentId, rtfContentId);
            });

            Awaitility.await().atMost(java.time.Duration.ofSeconds(10)).untilAsserted(() -> {
                var result = store.search("+document +one -two");
                assertThat(result).extracting(FulltextSearchResult::getContentId)
                    .contains(wordContentId)
                    .doesNotContain(rtfContentId);
            });
        }

        @Test
        void shouldIndexDocumentsUsingTextExtractingRenderer() {
            FulltextJpaUuidDocumentEntity wordDoc =
                FulltextJpaUuidDocumentEntity.withTitleAndAuthor("doc 1", "author@email.com");
            store.setContent(wordDoc, testResource("/one.docx"));
            repo.save(wordDoc);

            FulltextJpaUuidDocumentEntity rtfDoc =
                FulltextJpaUuidDocumentEntity.withTitleAndAuthor("doc 2", "author@email.com");
            store.setContent(rtfDoc, testResource("/two.rtf"));
            repo.save(rtfDoc);

            FulltextJpaUuidDocumentEntity pngDoc =
                FulltextJpaUuidDocumentEntity.withTitleAndAuthor("doc 1", "author@email.com");
            pngDoc.setMimeType("image/png");
            store.setContent(pngDoc, testResource("/image.png"));
            repo.save(pngDoc);

            UUID pngContentId = contentId(pngDoc);

            Awaitility.await().atMost(java.time.Duration.ofSeconds(10)).untilAsserted(() -> {
                var result = store.search("wisdom");
                assertThat(result).extracting(FulltextSearchResult::getContentId).contains(pngContentId);
            });
        }

        @Test
        void shouldDeleteRecordOfContentFromIndex() {
            FulltextJpaUuidDocumentEntity wordDoc =
                FulltextJpaUuidDocumentEntity.withTitleAndAuthor("doc 1", "author@email.com");
            store.setContent(wordDoc, testResource("/one.docx"));
            repo.save(wordDoc);

            FulltextJpaUuidDocumentEntity rtfDoc =
                FulltextJpaUuidDocumentEntity.withTitleAndAuthor("doc 2", "author@email.com");
            store.setContent(rtfDoc, testResource("/two.rtf"));
            repo.save(rtfDoc);

            UUID id1 = contentId(wordDoc);
            UUID id2 = contentId(rtfDoc);

            Awaitility.await().atMost(java.time.Duration.ofSeconds(10)).untilAsserted(() -> {
                assertThat(isIndexedInGlobalIndex(id1)).isTrue();
                assertThat(isIndexedInGlobalIndex(id2)).isTrue();
            });

            store.unsetContent(wordDoc);
            repo.delete(wordDoc);
            store.unsetContent(rtfDoc);
            repo.delete(rtfDoc);

            Awaitility.await().atMost(java.time.Duration.ofSeconds(10)).untilAsserted(() -> {
                assertThat(isIndexedInGlobalIndex(id1)).isFalse();
                assertThat(isIndexedInGlobalIndex(id2)).isFalse();
            });
        }
    }

    @Nested
    class Paging {
        @BeforeEach
        void setUp() {
            setUpEntityIndexingContext();

            for (int i = 0; i < 10; i++) {
                FulltextJpaUuidDocumentEntity doc =
                    FulltextJpaUuidDocumentEntity.withTitleAndAuthor(format("doc %s", i), "author@email.com");
                store.setContent(doc, testResource("/one.docx"));
                repo.save(doc);
            }
        }

        @AfterEach
        void tearDown() throws IOException {
            tearDownEntityIndexingScenario();
        }

        @Test
        void shouldReturnResultsInPages() {
            Awaitility.await().atMost(java.time.Duration.ofSeconds(10)).untilAsserted(() -> {
                var page = store.search("one", PageRequest.of(0, 3));
                assertThat(page.getTotalElements()).isEqualTo(10L);
                assertThat(page.getTotalPages()).isEqualTo(4);
                assertThat(page.getNumberOfElements()).isEqualTo(3);
                assertThat(page.getContent().size()).isEqualTo(3);
            });

            Awaitility.await().atMost(java.time.Duration.ofSeconds(10)).untilAsserted(() -> {
                var page = store.search("one", PageRequest.of(1, 3));
                assertThat(page.getTotalElements()).isEqualTo(10L);
                assertThat(page.getTotalPages()).isEqualTo(4);
                assertThat(page.getNumberOfElements()).isEqualTo(3);
                assertThat(page.getContent().size()).isEqualTo(3);
            });

            Awaitility.await().atMost(java.time.Duration.ofSeconds(10)).untilAsserted(() -> {
                var page = store.search("one", PageRequest.of(2, 3));
                assertThat(page.getTotalElements()).isEqualTo(10L);
                assertThat(page.getTotalPages()).isEqualTo(4);
                assertThat(page.getNumberOfElements()).isEqualTo(3);
                assertThat(page.getContent().size()).isEqualTo(3);
            });

            Awaitility.await().atMost(java.time.Duration.ofSeconds(10)).untilAsserted(() -> {
                var page = store.search("one", PageRequest.of(3, 3));
                assertThat(page.getTotalElements()).isEqualTo(10L);
                assertThat(page.getTotalPages()).isEqualTo(4);
                assertThat(page.getNumberOfElements()).isEqualTo(1);
                assertThat(page.getContent().size()).isEqualTo(1);
            });
        }
    }

    @Nested
    @DisplayName("Given a context configured to sync attributes and provide a filter query")
    class ContextConfiguredToSyncAttributesAndProvideFilterQuery {

        private FulltextJpaUuidDocumentEntity buckRogersDoc;
        private FulltextJpaUuidDocumentEntity wilmaDeeringDoc;

        @BeforeEach
        void setUp() {
            setUpEntityIndexingContextWithCustomAttributes();

            buckRogersDoc = FulltextJpaUuidDocumentEntity.withTitleAndAuthor("doc 1", "Buck Rogers");
            store.setContent(buckRogersDoc, testResource("/one.docx"));
            repo.save(buckRogersDoc);

            wilmaDeeringDoc = FulltextJpaUuidDocumentEntity.withTitleAndAuthor("doc 2", "Wilma Deering");
            store.setContent(wilmaDeeringDoc, testResource("/one.docx"));
            repo.save(wilmaDeeringDoc);
        }

        @AfterEach
        void tearDown() throws IOException {
            tearDownEntityIndexingScenario();
        }

        @Test
        void shouldReturnSpecifiedAttributes() {
            UUID buckContentId = contentId(buckRogersDoc);
            UUID wilmaContentId = contentId(wilmaDeeringDoc);

            Awaitility.await().atMost(java.time.Duration.ofSeconds(10)).untilAsserted(() -> assertThat(
                store.search("one", PageRequest.of(0, 10)).map(FulltextSearchResult::getContentId))
                .contains(buckContentId)
                .doesNotContain(wilmaContentId));
        }
    }

    @Nested
    class CustomReturnTypes {

        private FulltextJpaUuidDocumentEntity indexedDoc;

        @BeforeEach
        void setUp() {
            setUpEntityIndexingContextWithCustomAttributes();

            indexedDoc = FulltextJpaUuidDocumentEntity.withTitleAndAuthor("A document about one", "Buck Rogers");
            searchableStore.setContent(indexedDoc, testResource("/one.docx"));
            repo.save(indexedDoc);
        }

        @AfterEach
        void tearDown() throws IOException {
            tearDownEntityIndexingScenario();
        }

        @Test
        void shouldReturnResultsUsingCustomReturnType() {
            UUID indexedContentId = contentId(indexedDoc);

            Awaitility.await().atMost(java.time.Duration.ofSeconds(10)).untilAsserted(() -> {
                var result = searchableStore.search("one");
                assertThat(result).isNotNull();
                Iterator<FulltextSearchResult> iterator = result.iterator();
                assertThat(iterator.hasNext()).isTrue();
                FulltextSearchResult info = iterator.next();
                assertThat(info).extracting("contentId").isEqualTo(indexedContentId);
                assertThat(info).extracting("highlight").asString().contains("<em>one</em>");
                assertThat(info).extracting("author").asString().contains("Buck Rogers");
                assertThat(iterator.hasNext()).isFalse();
            });
        }
    }
}
