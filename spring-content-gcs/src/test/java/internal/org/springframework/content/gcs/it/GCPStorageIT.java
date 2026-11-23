package internal.org.springframework.content.gcs.it;

import com.google.api.gax.paging.Page;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import org.apache.commons.io.IOUtils;
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
import org.springframework.content.gcs.config.EnableGCPStorage;
import org.springframework.content.test.support.EmbeddedJpa;
import org.springframework.content.test.support.TestSupportData;
import org.springframework.content.test.support.TestSupportPackages;
import org.springframework.content.test.support.jpa.*;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

public class GCPStorageIT {

    static {
        System.setProperty("spring.content.gcp.storage.bucket", "test-bucket");
    }

    private DualContentJpaLongIdEntity entity;
    private Resource genericResource;
    private Exception e;
    private AnnotationConfigApplicationContext context;
    private DualContentJpaLongIdRepository repo;
    private DualContentJpaLongIdStore store;
    private Storage storage;
    private String resourceLocation;

    @Configuration
    @EnableJpaRepositories(basePackages = TestSupportPackages.JPA, considerNestedRepositories = true)
    @EnableGCPStorage(basePackages = TestSupportPackages.JPA)
    @Import(InfrastructureConfig.class)
    public static class TestConfig {

        @Bean
        public static Storage storage() {
            return GCSStorageContainer.getStorage();
        }
    }

    @Configuration
    public static class InfrastructureConfig {

        @Bean
        public DataSource dataSource() {
            return EmbeddedJpa.embeddedDataSource(EmbeddedDatabaseType.H2);
        }

        @Bean
        public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
            return EmbeddedJpa.entityManagerFactory(dataSource(), Database.H2, TestSupportPackages.JPA);
        }

        @Bean
        public PlatformTransactionManager transactionManager() {
            return EmbeddedJpa.transactionManager(entityManagerFactory().getObject());
        }
    }

    @Nested
   class DefaultGCPStorageImplCases {
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

                        repo = context.getBean(DualContentJpaLongIdRepository.class);
                        store = context.getBean(DualContentJpaLongIdStore.class);
                        storage = context.getBean(Storage.class);


                        resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                        genericResource = store.getResource(resourceLocation);
                    }

                    @AfterEach
                    void tearDown() throws IOException {
                        if (genericResource != null) {
                            ((DeletableResource) genericResource).delete();
                        }

                        Page<Blob> blobs = storage.list("delete-me-please-please", Storage.BlobListOption.currentDirectory());
                        for (Blob blob : blobs.iterateAll()) {
                            storage.delete(blob.getBlobId());
                        }

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

                            repo = context.getBean(DualContentJpaLongIdRepository.class);
                            store = context.getBean(DualContentJpaLongIdStore.class);
                            storage = context.getBean(Storage.class);


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
                            if (genericResource != null) {
                                ((DeletableResource) genericResource).delete();
                            }

                            Page<Blob> blobs = storage.list("delete-me-please-please", Storage.BlobListOption.currentDirectory());
                            for (Blob blob : blobs.iterateAll()) {
                                storage.delete(blob.getBlobId());
                            }

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

                            repo = context.getBean(DualContentJpaLongIdRepository.class);
                            store = context.getBean(DualContentJpaLongIdStore.class);
                            storage = context.getBean(Storage.class);


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
                            if (genericResource != null) {
                                ((DeletableResource) genericResource).delete();
                            }

                            Page<Blob> blobs = storage.list("delete-me-please-please", Storage.BlobListOption.currentDirectory());
                            for (Blob blob : blobs.iterateAll()) {
                                storage.delete(blob.getBlobId());
                            }

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

                            repo = context.getBean(DualContentJpaLongIdRepository.class);
                            store = context.getBean(DualContentJpaLongIdStore.class);
                            storage = context.getBean(Storage.class);


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
                                GCPStorageIT.this.e = e;
                            }
                        }

                        @AfterEach
                        void tearDown() throws IOException {
                            if (genericResource != null) {
                                ((DeletableResource) genericResource).delete();
                            }

                            Page<Blob> blobs = storage.list("delete-me-please-please", Storage.BlobListOption.currentDirectory());
                            for (Blob blob : blobs.iterateAll()) {
                                storage.delete(blob.getBlobId());
                            }

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

                        repo = context.getBean(DualContentJpaLongIdRepository.class);
                        store = context.getBean(DualContentJpaLongIdStore.class);
                        storage = context.getBean(Storage.class);


                        resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                        entity = new DualContentJpaLongIdEntity();
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

                                repo = context.getBean(DualContentJpaLongIdRepository.class);
                                store = context.getBean(DualContentJpaLongIdStore.class);
                                storage = context.getBean(Storage.class);


                                resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                                entity = new DualContentJpaLongIdEntity();
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

                                repo = context.getBean(DualContentJpaLongIdRepository.class);
                                store = context.getBean(DualContentJpaLongIdStore.class);
                                storage = context.getBean(Storage.class);


                                resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                                entity = new DualContentJpaLongIdEntity();
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
                                    assertThat(IOUtils.toString(is, Charset.defaultCharset())).isEqualTo("Hello Client-side World!");
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

                                repo = context.getBean(DualContentJpaLongIdRepository.class);
                                store = context.getBean(DualContentJpaLongIdStore.class);
                                storage = context.getBean(Storage.class);


                                resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                                entity = new DualContentJpaLongIdEntity();
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

                                repo = context.getBean(DualContentJpaLongIdRepository.class);
                                store = context.getBean(DualContentJpaLongIdStore.class);
                                storage = context.getBean(Storage.class);


                                resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                                entity = new DualContentJpaLongIdEntity();
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
                                    GCPStorageIT.this.e = sae;
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

                                repo = context.getBean(DualContentJpaLongIdRepository.class);
                                store = context.getBean(DualContentJpaLongIdStore.class);
                                storage = context.getBean(Storage.class);


                                resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                                entity = new DualContentJpaLongIdEntity();
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
                                    GCPStorageIT.this.e = sae;
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

                                repo = context.getBean(DualContentJpaLongIdRepository.class);
                                store = context.getBean(DualContentJpaLongIdStore.class);
                                storage = context.getBean(Storage.class);


                                resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                                entity = new DualContentJpaLongIdEntity();
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
                                    GCPStorageIT.this.e = sae;
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

                    repo = context.getBean(DualContentJpaLongIdRepository.class);
                    store = context.getBean(DualContentJpaLongIdStore.class);
                    storage = context.getBean(Storage.class);


                    resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                    entity = new DualContentJpaLongIdEntity();
                    entity = repo.save(entity);

                    store.setContent(entity, TestSupportData.sampleTextStream());
                    store.setContent(entity, PropertyPath.from("rendition"), TestSupportData.sampleHtmlRenditionStream());
                    entity = repo.save(entity);
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
                        assertThat(IOUtils.contentEquals(TestSupportData.sampleHtmlRenditionStream(), content)).isTrue();
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
                    assertThat(40L).isEqualTo(entity.getRenditionLen());
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

                    repo = context.getBean(DualContentJpaLongIdRepository.class);
                    store = context.getBean(DualContentJpaLongIdStore.class);
                    storage = context.getBean(Storage.class);


                    resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                    entity = new DualContentJpaLongIdEntity();
                    entity = repo.save(entity);

                    store.setContent(entity, TestSupportData.sampleTextStream());
                    store.setContent(entity, PropertyPath.from("rendition"), TestSupportData.sampleHtmlRenditionStream());
                    entity = repo.save(entity);

                    store.setContent(entity, TestSupportData.sampleUpdatedTextStream());
                    store.setContent(entity, PropertyPath.from("rendition"), TestSupportData.sampleUpdatedHtmlRenditionStream());
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
                        boolean matches = IOUtils.contentEquals(TestSupportData.sampleUpdatedHtmlRenditionStream(), content);
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

                    repo = context.getBean(DualContentJpaLongIdRepository.class);
                    store = context.getBean(DualContentJpaLongIdStore.class);
                    storage = context.getBean(Storage.class);


                    resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                    entity = new DualContentJpaLongIdEntity();
                    entity = repo.save(entity);

                    store.setContent(entity, TestSupportData.sampleTextStream());
                    store.setContent(entity, PropertyPath.from("rendition"), TestSupportData.sampleHtmlRenditionStream());
                    entity = repo.save(entity);

                    store.setContent(entity, TestSupportData.samplePartialUpdateStream());
                    store.setContent(entity, PropertyPath.from("rendition"), TestSupportData.samplePartialUpdateHtmlRenditionStream());
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
                        boolean matches = IOUtils.contentEquals(TestSupportData.samplePartialUpdateHtmlRenditionStream(), content);
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

                    repo = context.getBean(DualContentJpaLongIdRepository.class);
                    store = context.getBean(DualContentJpaLongIdStore.class);
                    storage = context.getBean(Storage.class);


                    resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                    entity = new DualContentJpaLongIdEntity();
                    entity = repo.save(entity);

                    store.setContent(entity, TestSupportData.sampleTextStream());
                    store.setContent(entity, PropertyPath.from("rendition"), TestSupportData.sampleHtmlRenditionStream());
                    entity = repo.save(entity);
                }

                @AfterEach
                void tearDown() {
                    context.close();
                }

                @Test
                void shouldHaveTheUpdatedContent() throws IOException {
                    String contentId = entity.getContentId();
                    assertThat(contentId).isNotNull();
                    assertThat(storage.get(BlobId.of("test-bucket", contentId)).exists()).isTrue();

                    store.setContent(entity, PropertyPath.from("content"), TestSupportData.sampleUpdatedTextStream(), new SetContentParams(-1, true, SetContentParams.ContentDisposition.CreateNew));
                    entity = repo.save(entity);

                    try (InputStream content = store.getContent(entity)) {
                        boolean matches = IOUtils.contentEquals(TestSupportData.sampleUpdatedTextStream(), content);
                        assertThat(matches).isTrue();
                    }

                    assertThat(entity.getContentId()).isNotEqualTo(contentId);

                    assertThat(storage.get(BlobId.of("test-bucket", Objects.requireNonNull(entity.getContentId()))).exists()).isTrue();
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

                    repo = context.getBean(DualContentJpaLongIdRepository.class);
                    store = context.getBean(DualContentJpaLongIdStore.class);
                    storage = context.getBean(Storage.class);


                    resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                    entity = new DualContentJpaLongIdEntity();
                    entity = repo.save(entity);

                    store.setContent(entity, TestSupportData.sampleTextStream());
                    store.setContent(entity, PropertyPath.from("rendition"), TestSupportData.sampleHtmlRenditionStream());
                    entity = repo.save(entity);

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
                    assertThat(storage.get(BlobId.of("test-bucket", resourceLocation))).isNull();

                    //rendition
                    try (InputStream content = store.getContent(entity, PropertyPath.from("rendition"))) {
                        assertThat(content).isNull();
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

                    repo = context.getBean(DualContentJpaLongIdRepository.class);
                    store = context.getBean(DualContentJpaLongIdStore.class);
                    storage = context.getBean(Storage.class);


                    resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                    entity = new DualContentJpaLongIdEntity();
                    entity = repo.save(entity);

                    store.setContent(entity, TestSupportData.sampleTextStream());
                    store.setContent(entity, PropertyPath.from("rendition"), TestSupportData.sampleHtmlRenditionStream());
                    entity = repo.save(entity);

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
                    assertThat(storage.get(BlobId.of("test-bucket", resourceLocation)).exists()).isTrue();
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

                    repo = context.getBean(DualContentJpaLongIdRepository.class);
                    store = context.getBean(DualContentJpaLongIdStore.class);
                    storage = context.getBean(Storage.class);


                    resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                    entity = new DualContentJpaLongIdEntity();
                    entity = repo.save(entity);

                    store.setContent(entity, TestSupportData.sampleTextStream());
                    store.setContent(entity, PropertyPath.from("rendition"), TestSupportData.sampleHtmlRenditionStream());
                    entity = repo.save(entity);
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
                        GCPStorageIT.this.e = sae;
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

                    repo = context.getBean(DualContentJpaLongIdRepository.class);
                    store = context.getBean(DualContentJpaLongIdStore.class);
                    storage = context.getBean(Storage.class);


                    resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                    entity = new DualContentJpaLongIdEntity();
                    entity = repo.save(entity);

                    store.setContent(entity, TestSupportData.sampleTextStream());
                    store.setContent(entity, PropertyPath.from("rendition"), TestSupportData.sampleHtmlRenditionStream());
                    entity = repo.save(entity);
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
                        GCPStorageIT.this.e = sae;
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

                    repo = context.getBean(DualContentJpaLongIdRepository.class);
                    store = context.getBean(DualContentJpaLongIdStore.class);
                    storage = context.getBean(Storage.class);


                    resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                    entity = new DualContentJpaLongIdEntity();
                    entity = repo.save(entity);

                    store.setContent(entity, TestSupportData.sampleTextStream());
                    store.setContent(entity, PropertyPath.from("rendition"), TestSupportData.sampleHtmlRenditionStream());
                    entity = repo.save(entity);
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
                        GCPStorageIT.this.e = sae;
                    }
                    assertThat(e).isInstanceOf(StoreAccessException.class);
                }
            }

            @Nested

            @DisplayName("When content is deleted and the content ID field is shared with entity id")

            class ContentIsDeletedAndTheContentIdFieldIsSharedWithEntityId {
                @BeforeEach
                void setUp() {
                    context = new AnnotationConfigApplicationContext();
                    context.register(TestConfig.class);
                    context.refresh();

                    repo = context.getBean(DualContentJpaLongIdRepository.class);
                    store = context.getBean(DualContentJpaLongIdStore.class);
                    storage = context.getBean(Storage.class);


                    resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                    entity = new DualContentJpaLongIdEntity();
                    entity = repo.save(entity);

                    store.setContent(entity, TestSupportData.sampleTextStream());
                    store.setContent(entity, PropertyPath.from("rendition"), TestSupportData.sampleHtmlRenditionStream());
                    entity = repo.save(entity);
                }

                @AfterEach
                void tearDown() {
                    context.close();
                }

                @Test
                void shouldNotResetTheIdField() {
                    SharedIdJakartaJpaRepository sharedIdRepository = context.getBean(SharedIdJakartaJpaRepository.class);
                    SharedIdJakartaJpaStore sharedIdStore = context.getBean(SharedIdJakartaJpaStore.class);

                    SharedIdJakartaJpaEntity sharedIdContentIdEntity = sharedIdRepository.save(new SharedIdJakartaJpaEntity());

                    sharedIdContentIdEntity = sharedIdStore.setContent(sharedIdContentIdEntity, TestSupportData.sampleTextStream());
                    sharedIdContentIdEntity = sharedIdRepository.save(sharedIdContentIdEntity);
                    String id = sharedIdContentIdEntity.getContentId();
                    sharedIdContentIdEntity = sharedIdStore.unsetContent(sharedIdContentIdEntity);
                    assertThat(sharedIdContentIdEntity.getContentId()).isEqualTo(id);
                    assertThat(sharedIdContentIdEntity.getContentLen()).isNull();
                }
            }

            @Nested
           class EmbeddedContent {
                @Nested
                @DisplayName("Given a entity with a null embedded content object")
                class EntityWithANullEmbeddedContentObject {
                    @BeforeEach
                    void setUp() {
                        context = new AnnotationConfigApplicationContext();
                        context.register(TestConfig.class);
                        context.refresh();
                    }

                    @AfterEach
                    void tearDown() {
                        context.close();
                    }

                    @Test
                    void shouldReturnNullWhenContentIsFetched() throws IOException {
                        EmbeddedContentJpaStringIdRepository embeddedRepo = context.getBean(EmbeddedContentJpaStringIdRepository.class);
                        EmbeddedContentJpaStringIdStore embeddedStore = context.getBean(EmbeddedContentJpaStringIdStore.class);
                        EmbeddedContentJpaStringIdEntity embedded = embeddedRepo.save(new EmbeddedContentJpaStringIdEntity());
                        assertThat(embeddedStore.getContent(embedded, PropertyPath.from("content"))).isNull();
                    }

                    @Test
                    void shouldBeSuccessfulWhenContentIsSet() throws IOException {
                        EmbeddedContentJpaStringIdRepository embeddedRepo = context.getBean(EmbeddedContentJpaStringIdRepository.class);
                        EmbeddedContentJpaStringIdStore embeddedStore = context.getBean(EmbeddedContentJpaStringIdStore.class);
                        EmbeddedContentJpaStringIdEntity embedded = embeddedRepo.save(new EmbeddedContentJpaStringIdEntity());
                        embeddedStore.setContent(embedded, PropertyPath.from("content"), TestSupportData.sampleTextStream());
                        try (InputStream is = embeddedStore.getContent(embedded, PropertyPath.from("content"))) {
                            assertThat(IOUtils.contentEquals(is, TestSupportData.sampleTextStream())).isTrue();
                        }
                    }

                    @Test
                    void shouldReturnNullWhenContentIsUnset() {
                        EmbeddedContentJpaStringIdRepository embeddedRepo = context.getBean(EmbeddedContentJpaStringIdRepository.class);
                        EmbeddedContentJpaStringIdStore embeddedStore = context.getBean(EmbeddedContentJpaStringIdStore.class);
                        EmbeddedContentJpaStringIdEntity embedded = embeddedRepo.save(new EmbeddedContentJpaStringIdEntity());
                        assertThat(embeddedStore.unsetContent(embedded, PropertyPath.from("content"))).isSameAs(embedded);
                    }
                }
            }
        }
    }
}
