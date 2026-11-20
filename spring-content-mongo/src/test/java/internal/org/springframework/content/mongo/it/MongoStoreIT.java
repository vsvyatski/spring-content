package internal.org.springframework.content.mongo.it;

import com.mongodb.client.MongoClient;
import net.bytebuddy.utility.RandomString;
import org.apache.commons.io.IOUtils;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.content.commons.io.DeletableResource;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.*;
import org.springframework.content.mongo.config.EnableMongoStores;
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
import org.springframework.content.test.support.TestSupportPackages;
import org.springframework.content.test.support.mongo.*;
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

                        RandomString random = new RandomString(5);
                        resourceLocation = random.nextString();

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
                class GivenContentIsAddedToThatResource {
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

                            RandomString random = new RandomString(5);
                            resourceLocation = random.nextString();

                            genericResource = store.getResource(resourceLocation);

                            try (InputStream is = new ByteArrayInputStream("Hello Spring Content World!".getBytes())) {
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

                            try (InputStream expected = new ByteArrayInputStream("Hello Spring Content World!".getBytes())) {
                                try (InputStream actual = genericResource.getInputStream()) {
                                    boolean matches = IOUtils.contentEquals(expected, actual);
                                    assertThat(matches).isTrue();
                                }
                            }
                        }
                    }

                    @Nested
                    class GivenThatResourceIsThenUpdated {
                        @BeforeEach
                        void setUp() throws IOException {
                            context = new AnnotationConfigApplicationContext();
                            context.register(TestConfig.class);
                            context.refresh();

                            gridFsTemplate = context.getBean(GridFsTemplate.class);

                            repo = context.getBean(DualContentMongoRepository.class);
                            store = context.getBean(DualContentMongoStore.class);

                            RandomString random = new RandomString(5);
                            resourceLocation = random.nextString();

                            genericResource = store.getResource(resourceLocation);

                            try (InputStream is = new ByteArrayInputStream("Hello Spring Content World!".getBytes())) {
                                try (OutputStream os = ((WritableResource) genericResource).getOutputStream()) {
                                    IOUtils.copy(is, os);
                                }
                            }

                            try (InputStream is = new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes())) {
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

                            try (InputStream expected = new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes())) {
                                try (InputStream actual = genericResource.getInputStream()) {
                                    assertThat(IOUtils.contentEquals(expected, actual)).isTrue();
                                }
                            }
                        }
                    }

                    @Nested
                    class GivenThatResourceIsThenDeleted {
                        @BeforeEach
                        void setUp() throws IOException {
                            context = new AnnotationConfigApplicationContext();
                            context.register(TestConfig.class);
                            context.refresh();

                            gridFsTemplate = context.getBean(GridFsTemplate.class);

                            repo = context.getBean(DualContentMongoRepository.class);
                            store = context.getBean(DualContentMongoStore.class);

                            RandomString random = new RandomString(5);
                            resourceLocation = random.nextString();

                            genericResource = store.getResource(resourceLocation);

                            try (InputStream is = new ByteArrayInputStream("Hello Spring Content World!".getBytes())) {
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
            class GivenANewEntity {
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

                        RandomString random = new RandomString(5);
                        resourceLocation = random.nextString();

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
                        assertThat(store.getResource(entity)).isNull();
                    }
                }

                @Nested
                class GivenAResource {
                    @Nested
                    class WhenTheResourceIsAssociated {
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

                                RandomString random = new RandomString(5);
                                resourceLocation = random.nextString();

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
                        class WhenTheResourceHasContent {
                            @BeforeEach
                            void setUp() throws IOException {
                                context = new AnnotationConfigApplicationContext();
                                context.register(TestConfig.class);
                                context.refresh();

                                gridFsTemplate = context.getBean(GridFsTemplate.class);

                                repo = context.getBean(DualContentMongoRepository.class);
                                store = context.getBean(DualContentMongoStore.class);

                                RandomString random = new RandomString(5);
                                resourceLocation = random.nextString();

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
                        class WhenTheResourceIsUnassociated {
                            @BeforeEach
                            void setUp() {
                                context = new AnnotationConfigApplicationContext();
                                context.register(TestConfig.class);
                                context.refresh();

                                gridFsTemplate = context.getBean(GridFsTemplate.class);

                                repo = context.getBean(DualContentMongoRepository.class);
                                store = context.getBean(DualContentMongoStore.class);

                                RandomString random = new RandomString(5);
                                resourceLocation = random.nextString();

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
                        class WhenAInvalidPropertyPathIsUsedToAssociateAResource {
                            @BeforeEach
                            void setUp() {
                                context = new AnnotationConfigApplicationContext();
                                context.register(TestConfig.class);
                                context.refresh();

                                gridFsTemplate = context.getBean(GridFsTemplate.class);

                                repo = context.getBean(DualContentMongoRepository.class);
                                store = context.getBean(DualContentMongoStore.class);

                                RandomString random = new RandomString(5);
                                resourceLocation = random.nextString();

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
                        class WhenAInvalidPropertyPathIsUsedToLoadAResource {
                            @BeforeEach
                            void setUp() {
                                context = new AnnotationConfigApplicationContext();
                                context.register(TestConfig.class);
                                context.refresh();

                                gridFsTemplate = context.getBean(GridFsTemplate.class);

                                repo = context.getBean(DualContentMongoRepository.class);
                                store = context.getBean(DualContentMongoStore.class);

                                RandomString random = new RandomString(5);
                                resourceLocation = random.nextString();

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
                        class WhenAInvalidPropertyPathIsUsedToUnassociateAResource {
                            @BeforeEach
                            void setUp() {
                                context = new AnnotationConfigApplicationContext();
                                context.register(TestConfig.class);
                                context.refresh();

                                gridFsTemplate = context.getBean(GridFsTemplate.class);

                                repo = context.getBean(DualContentMongoRepository.class);
                                store = context.getBean(DualContentMongoStore.class);

                                RandomString random = new RandomString(5);
                                resourceLocation = random.nextString();

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

                    RandomString random = new RandomString(5);
                    resourceLocation = random.nextString();

                    entity = new DualContentMongoDocument();
                    entity = repo.save(entity);

                    store.setContent(entity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                    store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                }

                @AfterEach
                void tearDown() {
                    context.close();
                }

                @Test
                void shouldBeAbleToStoreNewContent() {
                    // content
                    try (InputStream content = store.getContent(entity)) {
                        assertThat(IOUtils.contentEquals(new ByteArrayInputStream("Hello Spring Content World!".getBytes()), content)).isTrue();
                    } catch (IOException ignored) {
                    }

                    //rendition
                    try (InputStream content = store.getContent(entity, PropertyPath.from("rendition"))) {
                        assertThat(IOUtils.contentEquals(new ByteArrayInputStream("Hello Spring Content World!".getBytes()), content)).isTrue();
                    } catch (IOException ignored) {
                    }
                }

                @Test
                void shouldHaveContentMetadata() {
                    // content
                    assertThat(entity.getContentId()).isNotNull();
                    assertThat(entity.getContentId().trim().length()).isGreaterThan(0);
                    assertThat(entity.getContentLen()).isEqualTo(27L);

                    //rendition
                    assertThat(entity.getRenditionId()).isNotNull();
                    assertThat(entity.getRenditionId().trim().length()).isGreaterThan(0);
                    assertThat(entity.getRenditionLen()).isEqualTo(27L);
                }
            }

            @Nested
            class WhenContentIsUpdated {
                @BeforeEach
                void setUp() {
                    context = new AnnotationConfigApplicationContext();
                    context.register(TestConfig.class);
                    context.refresh();

                    gridFsTemplate = context.getBean(GridFsTemplate.class);

                    repo = context.getBean(DualContentMongoRepository.class);
                    store = context.getBean(DualContentMongoStore.class);

                    RandomString random = new RandomString(5);
                    resourceLocation = random.nextString();

                    entity = new DualContentMongoDocument();
                    entity = repo.save(entity);

                    store.setContent(entity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                    store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("Hello Spring Content World!".getBytes()));

                    store.setContent(entity, new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()));
                    store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()));
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
                        boolean matches = IOUtils.contentEquals(new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()), content);
                        assertThat(matches).isTrue();
                    }

                    //rendition
                    try (InputStream content = store.getContent(entity, PropertyPath.from("rendition"))) {
                        boolean matches = IOUtils.contentEquals(new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()), content);
                        assertThat(matches).isTrue();
                    }
                }
            }

            @Nested
            class WhenContentIsUpdatedWithShorterContent {
                @BeforeEach
                void setUp() {
                    context = new AnnotationConfigApplicationContext();
                    context.register(TestConfig.class);
                    context.refresh();

                    gridFsTemplate = context.getBean(GridFsTemplate.class);

                    repo = context.getBean(DualContentMongoRepository.class);
                    store = context.getBean(DualContentMongoStore.class);

                    RandomString random = new RandomString(5);
                    resourceLocation = random.nextString();

                    entity = new DualContentMongoDocument();
                    entity = repo.save(entity);

                    store.setContent(entity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                    store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("Hello Spring Content World!".getBytes()));

                    store.setContent(entity, new ByteArrayInputStream("Hello Spring World!".getBytes()));
                    store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("Hello Spring World!".getBytes()));
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
                        boolean matches = IOUtils.contentEquals(new ByteArrayInputStream("Hello Spring World!".getBytes()), content);
                        assertThat(matches).isTrue();
                    }

                    //rendition
                    try (InputStream content = store.getContent(entity, PropertyPath.from("rendition"))) {
                        boolean matches = IOUtils.contentEquals(new ByteArrayInputStream("Hello Spring World!".getBytes()), content);
                        assertThat(matches).isTrue();
                    }
                }
            }

            @Nested
            class WhenContentIsUpdatedAndNotOverwritten {
                @BeforeEach
                void setUp() {
                    context = new AnnotationConfigApplicationContext();
                    context.register(TestConfig.class);
                    context.refresh();

                    gridFsTemplate = context.getBean(GridFsTemplate.class);

                    repo = context.getBean(DualContentMongoRepository.class);
                    store = context.getBean(DualContentMongoStore.class);

                    RandomString random = new RandomString(5);
                    resourceLocation = random.nextString();

                    entity = new DualContentMongoDocument();
                    entity = repo.save(entity);

                    store.setContent(entity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                    store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                }

                @AfterEach
                void tearDown() {
                    context.close();
                }

                @Test
                void shouldHaveTheUpdatedContent() throws IOException {
                    String contentId = entity.getContentId();
                    assertThat(gridFsTemplate.getResource(contentId).exists()).isTrue();

                    store.setContent(entity, PropertyPath.from("content"), new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()), new SetContentParams(-1, true, SetContentParams.ContentDisposition.CreateNew));
                    entity = repo.save(entity);

                    try (InputStream content = store.getContent(entity)) {
                        boolean matches = IOUtils.contentEquals(new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()), content);
                        assertThat(matches).isTrue();
                    }

                    assertThat(gridFsTemplate.getResource(contentId).exists()).isTrue();

                    assertThat(entity.getContentId()).isNotEqualTo(contentId);

                    assertThat(gridFsTemplate.getResource(entity.getContentId()).exists()).isTrue();
                }
            }

            @Nested
            class WhenContentIsUnset {
                @BeforeEach
                void setUp() {
                    context = new AnnotationConfigApplicationContext();
                    context.register(TestConfig.class);
                    context.refresh();

                    gridFsTemplate = context.getBean(GridFsTemplate.class);

                    repo = context.getBean(DualContentMongoRepository.class);
                    store = context.getBean(DualContentMongoStore.class);

                    RandomString random = new RandomString(5);
                    resourceLocation = random.nextString();

                    entity = new DualContentMongoDocument();
                    entity = repo.save(entity);

                    store.setContent(entity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                    store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("Hello Spring Content World!".getBytes()));

                    resourceLocation = entity.getContentId();
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
                        assertThat(content).isNull();
                    }

                    assertThat(entity.getContentId()).isNull();
                    assertThat(entity.getContentLen()).isNull();

                    assertThat(gridFsTemplate.getResource(resourceLocation).exists()).isFalse();

                    //rendition
                    try (InputStream content = store.getContent(entity, PropertyPath.from("rendition"))) {
                        assertThat(content).isNull();
                    }

                    assertThat(entity.getRenditionId()).isNull();
                    assertThat(entity.getRenditionLen()).isNull();
                }
            }

            @Nested
            class WhenContentIsUnsetButKept {
                @BeforeEach
                void setUp() {
                    context = new AnnotationConfigApplicationContext();
                    context.register(TestConfig.class);
                    context.refresh();

                    gridFsTemplate = context.getBean(GridFsTemplate.class);

                    repo = context.getBean(DualContentMongoRepository.class);
                    store = context.getBean(DualContentMongoStore.class);

                    RandomString random = new RandomString(5);
                    resourceLocation = random.nextString();

                    entity = new DualContentMongoDocument();
                    entity = repo.save(entity);

                    store.setContent(entity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                    store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("Hello Spring Content World!".getBytes()));

                    resourceLocation = entity.getContentId();
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
                        assertThat(content).isNull();
                    }

                    assertThat(entity.getContentId()).isNull();
                    assertThat(entity.getContentLen()).isNull();

                    assertThat(gridFsTemplate.getResource(resourceLocation).exists()).isTrue();
                }
            }

            @Nested
            class WhenAnInvalidPropertyPathIsUsedToSetContent {
                @BeforeEach
                void setUp() {
                    context = new AnnotationConfigApplicationContext();
                    context.register(TestConfig.class);
                    context.refresh();

                    gridFsTemplate = context.getBean(GridFsTemplate.class);

                    repo = context.getBean(DualContentMongoRepository.class);
                    store = context.getBean(DualContentMongoStore.class);

                    RandomString random = new RandomString(5);
                    resourceLocation = random.nextString();

                    entity = new DualContentMongoDocument();
                    entity = repo.save(entity);

                    store.setContent(entity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                    store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
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
            class WhenAnInvalidPropertyPathIsUsedToGetContent {
                @BeforeEach
                void setUp() {
                    context = new AnnotationConfigApplicationContext();
                    context.register(TestConfig.class);
                    context.refresh();

                    gridFsTemplate = context.getBean(GridFsTemplate.class);

                    repo = context.getBean(DualContentMongoRepository.class);
                    store = context.getBean(DualContentMongoStore.class);

                    RandomString random = new RandomString(5);
                    resourceLocation = random.nextString();

                    entity = new DualContentMongoDocument();
                    entity = repo.save(entity);

                    store.setContent(entity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                    store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
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
            class WhenAnInvalidPropertyPathIsUsedToUnsetContent {
                @BeforeEach
                void setUp() {
                    context = new AnnotationConfigApplicationContext();
                    context.register(TestConfig.class);
                    context.refresh();

                    gridFsTemplate = context.getBean(GridFsTemplate.class);

                    repo = context.getBean(DualContentMongoRepository.class);
                    store = context.getBean(DualContentMongoStore.class);

                    RandomString random = new RandomString(5);
                    resourceLocation = random.nextString();

                    entity = new DualContentMongoDocument();
                    entity = repo.save(entity);

                    store.setContent(entity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                    store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
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
            class WhenContentIsDeletedAndTheIdFieldIsSharedWithJakartaId {
                @BeforeEach
                void setUp() {
                    context = new AnnotationConfigApplicationContext();
                    context.register(TestConfig.class);
                    context.refresh();

                    gridFsTemplate = context.getBean(GridFsTemplate.class);

                    repo = context.getBean(DualContentMongoRepository.class);
                    store = context.getBean(DualContentMongoStore.class);

                    RandomString random = new RandomString(5);
                    resourceLocation = random.nextString();

                    entity = new DualContentMongoDocument();
                    entity = repo.save(entity);

                    store.setContent(entity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                    store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
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

                    sharedIdContentIdEntity = sharedIdStore.setContent(sharedIdContentIdEntity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                    sharedIdContentIdEntity = sharedIdRepository.save(sharedIdContentIdEntity);
                    String id = sharedIdContentIdEntity.getContentId();
                    sharedIdContentIdEntity = sharedIdStore.unsetContent(sharedIdContentIdEntity);
                    assertThat(sharedIdContentIdEntity.getContentId()).isEqualTo(id);
                    assertThat(sharedIdContentIdEntity.getContentLen()).isNull();
                }
            }

            @Nested
            class WhenContentIsDeletedAndTheIdFieldIsSharedWithSpringId {
                @BeforeEach
                void setUp() {
                    context = new AnnotationConfigApplicationContext();
                    context.register(TestConfig.class);
                    context.refresh();

                    gridFsTemplate = context.getBean(GridFsTemplate.class);

                    repo = context.getBean(DualContentMongoRepository.class);
                    store = context.getBean(DualContentMongoStore.class);

                    RandomString random = new RandomString(5);
                    resourceLocation = random.nextString();

                    entity = new DualContentMongoDocument();
                    entity = repo.save(entity);

                    store.setContent(entity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                    store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
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

                    SharedIdSpringMongoDocument = SharedIdSpringMongoStore.setContent(SharedIdSpringMongoDocument, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                    SharedIdSpringMongoDocument = SharedIdSpringMongoRepository.save(SharedIdSpringMongoDocument);
                    String id = SharedIdSpringMongoDocument.getContentId();
                    SharedIdSpringMongoDocument = SharedIdSpringMongoStore.unsetContent(SharedIdSpringMongoDocument);
                    assertThat(SharedIdSpringMongoDocument.getContentId()).isEqualTo(id);
                    assertThat(SharedIdSpringMongoDocument.getContentLen()).isNull();
                }
            }
        }
    }
}
