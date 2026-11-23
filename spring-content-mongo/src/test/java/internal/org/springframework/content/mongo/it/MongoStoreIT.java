package internal.org.springframework.content.mongo.it;

import com.mongodb.client.MongoClient;
import org.apache.commons.io.IOUtils;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.content.commons.io.DeletableResource;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.GetResourceParams;
import org.springframework.content.commons.store.SetContentParams;
import org.springframework.content.commons.store.StoreAccessException;
import org.springframework.content.commons.store.UnsetContentParams;
import org.springframework.content.mongo.config.EnableMongoStores;
import org.springframework.content.test.support.NullableAssertions;
import org.springframework.content.test.support.TestSupportData;
import org.springframework.content.test.support.TestSupportPackages;
import org.springframework.content.test.support.mongo.*;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

public class MongoStoreIT {
    private GridFsTemplate gridFsTemplate;
    private DualContentMongoDocument entity;
    private Resource genericResource;

    private Exception e;

    private AnnotationConfigApplicationContext context;

    private DualContentMongoRepository repo;
    private DualContentMongoStore store;

    private String resourceLocation;

    @Configuration
    @EnableMongoRepositories(basePackages = TestSupportPackages.MONGO, considerNestedRepositories = true)
    @EnableMongoStores(basePackages = TestSupportPackages.MONGO)
    @Import(InfrastructureConfig.class)
    public static class TestConfig {
    }

    @Configuration
    public static class InfrastructureConfig extends AbstractMongoClientConfiguration {
        @Override
        protected @NonNull String getDatabaseName() {
            return MongoTestContainer.getTestDbName();
        }

        @Override
        @Bean
        public @NonNull MongoClient mongoClient() {
            return MongoTestContainer.getMongoClient();
        }

        @Bean
        public GridFsTemplate gridFsTemplate(MappingMongoConverter mongoConverter) {
            return new GridFsTemplate(mongoDbFactory(), mongoConverter);
        }

        @Override
        @Bean
        public @NonNull MongoDatabaseFactory mongoDbFactory() {
            return new SimpleMongoClientDatabaseFactory(mongoClient(), getDatabaseName());
        }
    }

    @Nested
   class DefaultMongoStoreImplCases {
        @Nested
        class StoreCases {
            @Nested
            class GetResource {
                @Nested
               class Tests {
                    @BeforeEach
                    void setUp() {
                        context = new AnnotationConfigApplicationContext();
                        context.register(TestConfig.class);
                        context.refresh();

                        gridFsTemplate = context.getBean(GridFsTemplate.class);

                        repo = context.getBean(DualContentMongoRepository.class);
                        store = context.getBean(DualContentMongoStore.class);

                        resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                        genericResource = store.getResource(resourceLocation);
                    }

                    @AfterEach
                    void tearDown() throws IOException {
                        ((DeletableResource) genericResource).delete();

                        context.close();
                    }

                    @Test
                    void shouldGetResource() {
                        assertThat(genericResource).isInstanceOf(Resource.class);
                    }

                    @Test
                    void shouldNotExist() {
                        assertThat(genericResource.exists()).isFalse();
                    }
                }

                @Nested

                @DisplayName("Given content is added to that resource")

                class ContentIsAddedToThatResource {
                    @Nested
                   class Tests {
                        @BeforeEach
                        void setUp() throws IOException {
                            context = new AnnotationConfigApplicationContext();
                            context.register(TestConfig.class);
                            context.refresh();

                            gridFsTemplate = context.getBean(GridFsTemplate.class);

                            repo = context.getBean(DualContentMongoRepository.class);
                            store = context.getBean(DualContentMongoStore.class);

                            resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                            genericResource = store.getResource(resourceLocation);

                            try (InputStream is = TestSupportData.sampleTextStream()) {
                                try (OutputStream os = ((WritableResource) genericResource).getOutputStream()) {
                                    IOUtils.copy(is, os);
                                }
                            }
                        }

                        @AfterEach
                        void tearDown() throws IOException {
                            ((DeletableResource) genericResource).delete();

                            context.close();
                        }

                        @Test
                        void shouldStoreThatContent() throws IOException {
                            assertThat(genericResource.exists()).isTrue();

                            try (InputStream expected = TestSupportData.sampleTextStream()) {
                                try (InputStream actual = genericResource.getInputStream()) {
                                    boolean matches = IOUtils.contentEquals(expected, actual);
                                    assertThat(matches).isTrue();
                                }
                            }
                        }
                    }

                    @Nested

                    @DisplayName("Given that resource is then updated")

                    class ResourceIsThenUpdated {
                        @BeforeEach
                        void setUp() throws IOException {
                            context = new AnnotationConfigApplicationContext();
                            context.register(TestConfig.class);
                            context.refresh();

                            gridFsTemplate = context.getBean(GridFsTemplate.class);

                            repo = context.getBean(DualContentMongoRepository.class);
                            store = context.getBean(DualContentMongoStore.class);

                            resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                            genericResource = store.getResource(resourceLocation);

                            try (InputStream is = TestSupportData.sampleTextStream()) {
                                try (OutputStream os = ((WritableResource) genericResource).getOutputStream()) {
                                    IOUtils.copy(is, os);
                                }
                            }

                            try (InputStream is = TestSupportData.sampleUpdatedTextStream()) {
                                try (OutputStream os = ((WritableResource) genericResource).getOutputStream()) {
                                    IOUtils.copy(is, os);
                                }
                            }
                        }

                        @AfterEach
                        void tearDown() throws IOException {
                            ((DeletableResource) genericResource).delete();

                            context.close();
                        }

                        @Test
                        void shouldStoreThatUpdatedContent() throws IOException {
                            assertThat(genericResource.exists()).isTrue();

                            try (InputStream expected = TestSupportData.sampleUpdatedTextStream()) {
                                try (InputStream actual = genericResource.getInputStream()) {
                                    assertThat(IOUtils.contentEquals(expected, actual)).isTrue();
                                }
                            }
                        }
                    }

                    @Nested

                    @DisplayName("Given that resource is then deleted")

                    class ResourceIsThenDeleted {
                        @BeforeEach
                        void setUp() throws IOException {
                            context = new AnnotationConfigApplicationContext();
                            context.register(TestConfig.class);
                            context.refresh();

                            gridFsTemplate = context.getBean(GridFsTemplate.class);

                            repo = context.getBean(DualContentMongoRepository.class);
                            store = context.getBean(DualContentMongoStore.class);

                            resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                            genericResource = store.getResource(resourceLocation);

                            try (InputStream is = TestSupportData.sampleTextStream()) {
                                try (OutputStream os = ((WritableResource) genericResource).getOutputStream()) {
                                    IOUtils.copy(is, os);
                                }
                            }

                            try {
                                ((DeletableResource) genericResource).delete();
                            } catch (Exception e) {
                                MongoStoreIT.this.e = e;
                            }
                        }

                        @AfterEach
                        void tearDown() throws IOException {
                            ((DeletableResource) genericResource).delete();

                            context.close();
                        }

                        @Test
                        void shouldNotExist() {
                            assertThat(e).isNull();
                        }
                    }
                }
            }
        }

        @Nested
        class AssociativeStoreCases {
            @Nested
            @DisplayName("Given a new entity")
            class NewEntity {
                @Nested
               class Tests {
                    @BeforeEach
                    void setUp() {
                        context = new AnnotationConfigApplicationContext();
                        context.register(TestConfig.class);
                        context.refresh();

                        gridFsTemplate = context.getBean(GridFsTemplate.class);

                        repo = context.getBean(DualContentMongoRepository.class);
                        store = context.getBean(DualContentMongoStore.class);

                        resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                        entity = new DualContentMongoDocument();
                        entity = repo.save(entity);
                    }

                    @AfterEach
                    void tearDown() {
                        context.close();
                    }

                    @Test
                    void shouldNotHaveAnAssociatedResource() {
                        assertThat(entity.getContentId()).isNull();
                        NullableAssertions.assertNull(store.getResource(entity));
                    }
                }

                @Nested

                @DisplayName("Given a resource")

                class AssociatedResource {
                    @Nested
                    @DisplayName("When the resource is associated")
                    class ResourceIsAssociated {
                        @Nested
                       class Tests {
                            @BeforeEach
                            void setUp() {
                                context = new AnnotationConfigApplicationContext();
                                context.register(TestConfig.class);
                                context.refresh();

                                gridFsTemplate = context.getBean(GridFsTemplate.class);

                                repo = context.getBean(DualContentMongoRepository.class);
                                store = context.getBean(DualContentMongoStore.class);

                                resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                                entity = new DualContentMongoDocument();
                                entity = repo.save(entity);

                                genericResource = store.getResource(resourceLocation);

                                store.associate(entity, resourceLocation);
                                store.associate(entity, PropertyPath.from("rendition"), resourceLocation);
                            }

                            @AfterEach
                            void tearDown() {
                                context.close();
                            }

                            @Test
                            void shouldBeRecordedAsSuchOnTheEntitySContentId() {
                                assertThat(entity.getContentId()).isEqualTo(resourceLocation);
                                assertThat(entity.getRenditionId()).isEqualTo(resourceLocation);
                            }
                        }

                        @Nested

                        @DisplayName("When the resource has content")

                        class ResourceHasContent {
                            @BeforeEach
                            void setUp() throws IOException {
                                context = new AnnotationConfigApplicationContext();
                                context.register(TestConfig.class);
                                context.refresh();

                                gridFsTemplate = context.getBean(GridFsTemplate.class);

                                repo = context.getBean(DualContentMongoRepository.class);
                                store = context.getBean(DualContentMongoStore.class);

                                resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                                entity = new DualContentMongoDocument();
                                entity = repo.save(entity);

                                genericResource = store.getResource(resourceLocation);

                                store.associate(entity, resourceLocation);
                                store.associate(entity, PropertyPath.from("rendition"), resourceLocation);

                                try (OutputStream os = ((WritableResource) genericResource).getOutputStream()) {
                                    os.write("Hello Client-side World!".getBytes());
                                }
                            }

                            @AfterEach
                            void tearDown() {
                                context.close();
                            }

                            @Test
                            void shouldNotHonorByteRanges() throws IOException {
                                // relies on REST-layer to serve byte range
                                Resource r = store.getResource(entity, PropertyPath.from("content"), new GetResourceParams("5-10"));
                                try (InputStream is = r.getInputStream()) {
                                    assertThat(IOUtils.toString(is, StandardCharsets.UTF_8)).isEqualTo("Hello Client-side World!");
                                }
                            }
                        }

                        @Nested

                        @DisplayName("When the resource is unassociated")

                        class ResourceIsUnassociated {
                            @BeforeEach
                            void setUp() {
                                context = new AnnotationConfigApplicationContext();
                                context.register(TestConfig.class);
                                context.refresh();

                                gridFsTemplate = context.getBean(GridFsTemplate.class);

                                repo = context.getBean(DualContentMongoRepository.class);
                                store = context.getBean(DualContentMongoStore.class);

                                resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                                entity = new DualContentMongoDocument();
                                entity = repo.save(entity);

                                genericResource = store.getResource(resourceLocation);

                                store.associate(entity, resourceLocation);
                                store.associate(entity, PropertyPath.from("rendition"), resourceLocation);

                                store.unassociate(entity);
                                store.unassociate(entity, PropertyPath.from("rendition"));
                            }

                            @AfterEach
                            void tearDown() {
                                context.close();
                            }

                            @Test
                            void shouldResetTheEntitySContentId() {
                                assertThat(entity.getContentId()).isNull();
                                assertThat(entity.getRenditionId()).isNull();
                            }
                        }

                        @Nested

                        @DisplayName("When a invalid property path is used to associate a resource")

                        class InvalidPropertyPathIsUsedToAssociateAResource {
                            @BeforeEach
                            void setUp() {
                                context = new AnnotationConfigApplicationContext();
                                context.register(TestConfig.class);
                                context.refresh();

                                gridFsTemplate = context.getBean(GridFsTemplate.class);

                                repo = context.getBean(DualContentMongoRepository.class);
                                store = context.getBean(DualContentMongoStore.class);

                                resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                                entity = new DualContentMongoDocument();
                                entity = repo.save(entity);

                                genericResource = store.getResource(resourceLocation);

                                store.associate(entity, resourceLocation);
                                store.associate(entity, PropertyPath.from("rendition"), resourceLocation);
                            }

                            @AfterEach
                            void tearDown() {
                                context.close();
                            }

                            @Test
                            void shouldThrowAnError() {
                                try {
                                    store.associate(entity, PropertyPath.from("does.not.exist"), resourceLocation);
                                } catch (Exception sae) {
                                    MongoStoreIT.this.e = sae;
                                }
                                assertThat(e).isInstanceOf(StoreAccessException.class);
                            }
                        }

                        @Nested

                        @DisplayName("When a invalid property path is used to load a resource")

                        class InvalidPropertyPathIsUsedToLoadAResource {
                            @BeforeEach
                            void setUp() {
                                context = new AnnotationConfigApplicationContext();
                                context.register(TestConfig.class);
                                context.refresh();

                                gridFsTemplate = context.getBean(GridFsTemplate.class);

                                repo = context.getBean(DualContentMongoRepository.class);
                                store = context.getBean(DualContentMongoStore.class);

                                resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                                entity = new DualContentMongoDocument();
                                entity = repo.save(entity);

                                genericResource = store.getResource(resourceLocation);

                                store.associate(entity, resourceLocation);
                                store.associate(entity, PropertyPath.from("rendition"), resourceLocation);
                            }

                            @AfterEach
                            void tearDown() {
                                context.close();
                            }

                            @Test
                            void shouldThrowAnError() {
                                try {
                                    store.getResource(entity, PropertyPath.from("does.not.exist"));
                                } catch (Exception sae) {
                                    MongoStoreIT.this.e = sae;
                                }
                                assertThat(e).isInstanceOf(StoreAccessException.class);
                            }
                        }

                        @Nested

                        @DisplayName("When a invalid property path is used to unassociate a resource")

                        class InvalidPropertyPathIsUsedToUnassociateAResource {
                            @BeforeEach
                            void setUp() {
                                context = new AnnotationConfigApplicationContext();
                                context.register(TestConfig.class);
                                context.refresh();

                                gridFsTemplate = context.getBean(GridFsTemplate.class);

                                repo = context.getBean(DualContentMongoRepository.class);
                                store = context.getBean(DualContentMongoStore.class);

                                resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                                entity = new DualContentMongoDocument();
                                entity = repo.save(entity);

                                genericResource = store.getResource(resourceLocation);

                                store.associate(entity, resourceLocation);
                                store.associate(entity, PropertyPath.from("rendition"), resourceLocation);
                            }

                            @AfterEach
                            void tearDown() {
                                context.close();
                            }

                            @Test
                            void shouldThrowAnError() {
                                try {
                                    store.unassociate(entity, PropertyPath.from("does.not.exist"));
                                } catch (Exception sae) {
                                    MongoStoreIT.this.e = sae;
                                }
                                assertThat(e).isInstanceOf(StoreAccessException.class);
                            }
                        }
                    }
                }
            }
        }

        @Nested
        class ContentStoreCases {
            @Nested
            class Tests {
                @BeforeEach
                void setUp() {
                    context = new AnnotationConfigApplicationContext();
                    context.register(TestConfig.class);
                    context.refresh();

                    gridFsTemplate = context.getBean(GridFsTemplate.class);

                    repo = context.getBean(DualContentMongoRepository.class);
                    store = context.getBean(DualContentMongoStore.class);

                    resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                    entity = new DualContentMongoDocument();
                    entity = repo.save(entity);

                    store.setContent(entity, TestSupportData.sampleTextStream());
                    store.setContent(entity, PropertyPath.from("rendition"), TestSupportData.sampleTextStream());
                }

                @AfterEach
                void tearDown() {
                    context.close();
                }

                @Test
                void shouldBeAbleToStoreNewContent() {
                    // content
                    try (InputStream content = store.getContent(entity)) {
                        assertThat(IOUtils.contentEquals(TestSupportData.sampleTextStream(), content)).isTrue();
                    } catch (IOException ignored) {
                    }

                    //rendition
                    try (InputStream content = store.getContent(entity, PropertyPath.from("rendition"))) {
                        assertThat(IOUtils.contentEquals(TestSupportData.sampleTextStream(), content)).isTrue();
                    } catch (IOException ignored) {
                    }
                }

                @Test
                void shouldHaveContentMetadata() {
                    // content
                    String contentId = entity.getContentId();
                    assertThat(contentId).isNotNull();
                    assertThat(contentId.trim().length()).isGreaterThan(0);
                    assertThat(entity.getContentLen()).isEqualTo(27L);

                    //rendition
                    String renditionId = entity.getRenditionId();
                    assertThat(renditionId).isNotNull();
                    assertThat(renditionId.trim().length()).isGreaterThan(0);
                    assertThat(entity.getRenditionLen()).isEqualTo(27L);
                }
            }

            @Nested

            @DisplayName("When content is updated")

            class ContentIsUpdated {
                @BeforeEach
                void setUp() {
                    context = new AnnotationConfigApplicationContext();
                    context.register(TestConfig.class);
                    context.refresh();

                    gridFsTemplate = context.getBean(GridFsTemplate.class);

                    repo = context.getBean(DualContentMongoRepository.class);
                    store = context.getBean(DualContentMongoStore.class);

                    resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                    entity = new DualContentMongoDocument();
                    entity = repo.save(entity);

                    store.setContent(entity, TestSupportData.sampleTextStream());
                    store.setContent(entity, PropertyPath.from("rendition"), TestSupportData.sampleTextStream());

                    store.setContent(entity, TestSupportData.sampleUpdatedTextStream());
                    store.setContent(entity, PropertyPath.from("rendition"), TestSupportData.sampleUpdatedTextStream());
                    entity = repo.save(entity);
                }

                @AfterEach
                void tearDown() {
                    context.close();
                }

                @Test
                void shouldHaveTheUpdatedContent() throws IOException {
                    //content
                    try (InputStream content = store.getContent(entity)) {
                        boolean matches = IOUtils.contentEquals(TestSupportData.sampleUpdatedTextStream(), content);
                        assertThat(matches).isTrue();
                    }

                    //rendition
                    try (InputStream content = store.getContent(entity, PropertyPath.from("rendition"))) {
                        boolean matches = IOUtils.contentEquals(TestSupportData.sampleUpdatedTextStream(), content);
                        assertThat(matches).isTrue();
                    }
                }
            }

            @Nested

            @DisplayName("When content is updated with shorter content")

            class ContentIsUpdatedWithShorterContent {
                @BeforeEach
                void setUp() {
                    context = new AnnotationConfigApplicationContext();
                    context.register(TestConfig.class);
                    context.refresh();

                    gridFsTemplate = context.getBean(GridFsTemplate.class);

                    repo = context.getBean(DualContentMongoRepository.class);
                    store = context.getBean(DualContentMongoStore.class);

                    resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                    entity = new DualContentMongoDocument();
                    entity = repo.save(entity);

                    store.setContent(entity, TestSupportData.sampleTextStream());
                    store.setContent(entity, PropertyPath.from("rendition"), TestSupportData.sampleTextStream());

                    store.setContent(entity, TestSupportData.samplePartialUpdateStream());
                    store.setContent(entity, PropertyPath.from("rendition"), TestSupportData.samplePartialUpdateStream());
                    entity = repo.save(entity);
                }

                @AfterEach
                void tearDown() {
                    context.close();
                }

                @Test
                void shouldStoreOnlyTheNewContent() throws IOException {
                    //content
                    try (InputStream content = store.getContent(entity)) {
                        boolean matches = IOUtils.contentEquals(TestSupportData.samplePartialUpdateStream(), content);
                        assertThat(matches).isTrue();
                    }

                    //rendition
                    try (InputStream content = store.getContent(entity, PropertyPath.from("rendition"))) {
                        boolean matches = IOUtils.contentEquals(TestSupportData.samplePartialUpdateStream(), content);
                        assertThat(matches).isTrue();
                    }
                }
            }

            @Nested

            @DisplayName("When content is updated and not overwritten")

            class ContentIsUpdatedAndNotOverwritten {
                @BeforeEach
                void setUp() {
                    context = new AnnotationConfigApplicationContext();
                    context.register(TestConfig.class);
                    context.refresh();

                    gridFsTemplate = context.getBean(GridFsTemplate.class);

                    repo = context.getBean(DualContentMongoRepository.class);
                    store = context.getBean(DualContentMongoStore.class);

                    resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                    entity = new DualContentMongoDocument();
                    entity = repo.save(entity);

                    store.setContent(entity, TestSupportData.sampleTextStream());
                    store.setContent(entity, PropertyPath.from("rendition"), TestSupportData.sampleTextStream());
                }

                @AfterEach
                void tearDown() {
                    context.close();
                }

                @Test
                void shouldHaveTheUpdatedContent() throws IOException {
                    String contentId = entity.getContentId();
                    assertThat(contentId).isNotNull();
                    assertThat(gridFsTemplate.getResource(contentId).exists()).isTrue();

                    store.setContent(entity, PropertyPath.from("content"), TestSupportData.sampleUpdatedTextStream(), new SetContentParams(-1, true, SetContentParams.ContentDisposition.CreateNew));
                    entity = repo.save(entity);

                    try (InputStream content = store.getContent(entity)) {
                        boolean matches = IOUtils.contentEquals(TestSupportData.sampleUpdatedTextStream(), content);
                        assertThat(matches).isTrue();
                    }

                    assertThat(gridFsTemplate.getResource(contentId).exists()).isTrue();

                    assertThat(entity.getContentId()).isNotEqualTo(contentId);

                    String newContentId = entity.getContentId();
                    assertThat(newContentId).isNotNull();
                    assertThat(gridFsTemplate.getResource(newContentId).exists()).isTrue();
                }
            }

            @Nested

            @DisplayName("When content is unset")

            class ContentIsUnset {
                @BeforeEach
                void setUp() {
                    context = new AnnotationConfigApplicationContext();
                    context.register(TestConfig.class);
                    context.refresh();

                    gridFsTemplate = context.getBean(GridFsTemplate.class);

                    repo = context.getBean(DualContentMongoRepository.class);
                    store = context.getBean(DualContentMongoStore.class);

                    resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                    entity = new DualContentMongoDocument();
                    entity = repo.save(entity);

                    store.setContent(entity, TestSupportData.sampleTextStream());
                    store.setContent(entity, PropertyPath.from("rendition"), TestSupportData.sampleTextStream());

                    String storedContentId = entity.getContentId();
                    assertThat(storedContentId).isNotNull();
                    resourceLocation = storedContentId;
                    entity = store.unsetContent(entity);
                    entity = store.unsetContent(entity, PropertyPath.from("rendition"));
                    entity = repo.save(entity);
                }

                @AfterEach
                void tearDown() {
                    context.close();
                }

                @Test
                void shouldHaveNoContent() throws IOException {
                    //content
                    try (InputStream content = store.getContent(entity)) {
                        NullableAssertions.assertNull(content);
                    }

                    assertThat(entity.getContentId()).isNull();
                    assertThat(entity.getContentLen()).isNull();

                    assertThat(gridFsTemplate.getResource(resourceLocation).exists()).isFalse();

                    //rendition
                    try (InputStream content = store.getContent(entity, PropertyPath.from("rendition"))) {
                        NullableAssertions.assertNull(content);
                    }

                    assertThat(entity.getRenditionId()).isNull();
                    assertThat(entity.getRenditionLen()).isNull();
                }
            }

            @Nested

            @DisplayName("When content is unset but kept")

            class ContentIsUnsetButKept {
                @BeforeEach
                void setUp() {
                    context = new AnnotationConfigApplicationContext();
                    context.register(TestConfig.class);
                    context.refresh();

                    gridFsTemplate = context.getBean(GridFsTemplate.class);

                    repo = context.getBean(DualContentMongoRepository.class);
                    store = context.getBean(DualContentMongoStore.class);

                    resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                    entity = new DualContentMongoDocument();
                    entity = repo.save(entity);

                    store.setContent(entity, TestSupportData.sampleTextStream());
                    store.setContent(entity, PropertyPath.from("rendition"), TestSupportData.sampleTextStream());

                    String storedContentId = entity.getContentId();
                    assertThat(storedContentId).isNotNull();
                    resourceLocation = storedContentId;
                    entity = store.unsetContent(entity, PropertyPath.from("content"), new UnsetContentParams(UnsetContentParams.Disposition.Keep));
                    entity = repo.save(entity);
                }

                @AfterEach
                void tearDown() {
                    context.close();
                }

                @Test
                void shouldHaveNoContent() throws IOException {
                    //content
                    try (InputStream content = store.getContent(entity)) {
                        NullableAssertions.assertNull(content);
                    }

                    assertThat(entity.getContentId()).isNull();
                    assertThat(entity.getContentLen()).isNull();

                    assertThat(gridFsTemplate.getResource(resourceLocation).exists()).isTrue();
                }
            }

            @Nested

            @DisplayName("When an invalid property path is used to set content")

            class InvalidPropertyPathIsUsedToSetContent {
                @BeforeEach
                void setUp() {
                    context = new AnnotationConfigApplicationContext();
                    context.register(TestConfig.class);
                    context.refresh();

                    gridFsTemplate = context.getBean(GridFsTemplate.class);

                    repo = context.getBean(DualContentMongoRepository.class);
                    store = context.getBean(DualContentMongoStore.class);

                    resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                    entity = new DualContentMongoDocument();
                    entity = repo.save(entity);

                    store.setContent(entity, TestSupportData.sampleTextStream());
                    store.setContent(entity, PropertyPath.from("rendition"), TestSupportData.sampleTextStream());
                }

                @AfterEach
                void tearDown() {
                    context.close();
                }

                @Test
                void shouldThrowAnError() {
                    try {
                        store.setContent(entity, PropertyPath.from("does.not.exist"), new ByteArrayInputStream("foo".getBytes()));
                    } catch (Exception sae) {
                        MongoStoreIT.this.e = sae;
                    }
                    assertThat(e).isInstanceOf(StoreAccessException.class);
                }
            }

            @Nested

            @DisplayName("When an invalid property path is used to GET content")

            class InvalidPropertyPathIsUsedToGetContent {
                @BeforeEach
                void setUp() {
                    context = new AnnotationConfigApplicationContext();
                    context.register(TestConfig.class);
                    context.refresh();

                    gridFsTemplate = context.getBean(GridFsTemplate.class);

                    repo = context.getBean(DualContentMongoRepository.class);
                    store = context.getBean(DualContentMongoStore.class);

                    resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                    entity = new DualContentMongoDocument();
                    entity = repo.save(entity);

                    store.setContent(entity, TestSupportData.sampleTextStream());
                    store.setContent(entity, PropertyPath.from("rendition"), TestSupportData.sampleTextStream());
                }

                @AfterEach
                void tearDown() {
                    context.close();
                }

                @Test
                void shouldThrowAnError() {
                    try {
                        store.getContent(entity, PropertyPath.from("does.not.exist"));
                    } catch (Exception sae) {
                        MongoStoreIT.this.e = sae;
                    }
                    assertThat(e).isInstanceOf(StoreAccessException.class);
                }
            }

            @Nested

            @DisplayName("When an invalid property path is used to unset content")

            class InvalidPropertyPathIsUsedToUnsetContent {
                @BeforeEach
                void setUp() {
                    context = new AnnotationConfigApplicationContext();
                    context.register(TestConfig.class);
                    context.refresh();

                    gridFsTemplate = context.getBean(GridFsTemplate.class);

                    repo = context.getBean(DualContentMongoRepository.class);
                    store = context.getBean(DualContentMongoStore.class);

                    resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                    entity = new DualContentMongoDocument();
                    entity = repo.save(entity);

                    store.setContent(entity, TestSupportData.sampleTextStream());
                    store.setContent(entity, PropertyPath.from("rendition"), TestSupportData.sampleTextStream());
                }

                @AfterEach
                void tearDown() {
                    context.close();
                }

                @Test
                void shouldThrowAnError() {
                    try {
                        store.unsetContent(entity, PropertyPath.from("does.not.exist"));
                    } catch (Exception sae) {
                        MongoStoreIT.this.e = sae;
                    }
                    assertThat(e).isInstanceOf(StoreAccessException.class);
                }
            }

            @Nested

            @DisplayName("When content is deleted and the id field is shared with jakarta id")

            class ContentIsDeletedAndTheIdFieldIsSharedWithJakartaId {
                @BeforeEach
                void setUp() {
                    context = new AnnotationConfigApplicationContext();
                    context.register(TestConfig.class);
                    context.refresh();

                    gridFsTemplate = context.getBean(GridFsTemplate.class);

                    repo = context.getBean(DualContentMongoRepository.class);
                    store = context.getBean(DualContentMongoStore.class);

                    resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                    entity = new DualContentMongoDocument();
                    entity = repo.save(entity);

                    store.setContent(entity, TestSupportData.sampleTextStream());
                    store.setContent(entity, PropertyPath.from("rendition"), TestSupportData.sampleTextStream());
                }

                @AfterEach
                void tearDown() {
                    context.close();
                }

                @Test
                void shouldNotResetTheIdField() {
                    SharedIdJakartaMongoRepository sharedIdRepository = context.getBean(SharedIdJakartaMongoRepository.class);
                    SharedIdJakartaMongoStore sharedIdStore = context.getBean(SharedIdJakartaMongoStore.class);

                    SharedIdJakartaMongoDocument sharedIdContentIdEntity = sharedIdRepository.save(new SharedIdJakartaMongoDocument());

                    sharedIdContentIdEntity = sharedIdStore.setContent(sharedIdContentIdEntity, TestSupportData.sampleTextStream());
                    sharedIdContentIdEntity = sharedIdRepository.save(sharedIdContentIdEntity);
                    String id = sharedIdContentIdEntity.getContentId();
                    assertThat(id).isNotNull();
                    sharedIdContentIdEntity = sharedIdStore.unsetContent(sharedIdContentIdEntity);
                    assertThat(sharedIdContentIdEntity.getContentId()).isEqualTo(id);
                    assertThat(sharedIdContentIdEntity.getContentLen()).isNull();
                }
            }

            @Nested

            @DisplayName("When content is deleted and the id field is shared with spring id")

            class ContentIsDeletedAndTheIdFieldIsSharedWithSpringId {
                @BeforeEach
                void setUp() {
                    context = new AnnotationConfigApplicationContext();
                    context.register(TestConfig.class);
                    context.refresh();

                    gridFsTemplate = context.getBean(GridFsTemplate.class);

                    repo = context.getBean(DualContentMongoRepository.class);
                    store = context.getBean(DualContentMongoStore.class);

                    resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                    entity = new DualContentMongoDocument();
                    entity = repo.save(entity);

                    store.setContent(entity, TestSupportData.sampleTextStream());
                    store.setContent(entity, PropertyPath.from("rendition"), TestSupportData.sampleTextStream());
                }

                @AfterEach
                void tearDown() {
                    context.close();
                }

                @Test
                void shouldNotResetTheIdField() {
                    SharedIdSpringMongoRepository SharedIdSpringMongoRepository = context.getBean(SharedIdSpringMongoRepository.class);
                    SharedIdSpringMongoStore SharedIdSpringMongoStore = context.getBean(SharedIdSpringMongoStore.class);

                    SharedIdSpringMongoDocument SharedIdSpringMongoDocument = SharedIdSpringMongoRepository.save(new SharedIdSpringMongoDocument());

                    SharedIdSpringMongoDocument = SharedIdSpringMongoStore.setContent(SharedIdSpringMongoDocument, TestSupportData.sampleTextStream());
                    SharedIdSpringMongoDocument = SharedIdSpringMongoRepository.save(SharedIdSpringMongoDocument);
                    String id = SharedIdSpringMongoDocument.getContentId();
                    assertThat(id).isNotNull();
                    SharedIdSpringMongoDocument = SharedIdSpringMongoStore.unsetContent(SharedIdSpringMongoDocument);
                    assertThat(SharedIdSpringMongoDocument.getContentId()).isEqualTo(id);
                    assertThat(SharedIdSpringMongoDocument.getContentLen()).isNull();
                }
            }
        }
    }
}
