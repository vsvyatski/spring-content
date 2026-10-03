package internal.org.springframework.content.gcs.it;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.f4b6a3.uuid.UuidCreator;
import com.google.api.gax.paging.Page;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.contrib.nio.testing.LocalStorageHelper;
import jakarta.persistence.*;
import net.bytebuddy.utility.RandomString;
import org.apache.commons.io.IOUtils;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.io.DeletableResource;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.*;
import org.springframework.content.gcs.config.EnableGCPStorage;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;


public class GCPStorageIT {

    private TestEntity entity;
    private Resource genericResource;

    private Exception e;

    private AnnotationConfigApplicationContext context;

    private TestEntityRepository repo;
    private TestEntityStore store;
    private Storage storage;

    private EmbeddedRepository embeddedRepo;
    private EmbeddedStore embeddedStore;

    private String resourceLocation;

    static {
        System.setProperty("spring.content.gcp.storage.bucket", "test-bucket");
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
                    void setUp() throws Throwable {
                        context = new AnnotationConfigApplicationContext();
                                        context.register(TestConfig.class);
                                        context.refresh();

                                        repo = context.getBean(TestEntityRepository.class);
                                        store = context.getBean(TestEntityStore.class);
                                        storage = context.getBean(Storage.class);

                                        embeddedRepo = context.getBean(EmbeddedRepository.class);
                                        embeddedStore = context.getBean(EmbeddedStore.class);

                                        RandomString random = new RandomString(5);
                                        resourceLocation = random.nextString();

                        genericResource = store.getResource(resourceLocation);
                    }
                    @AfterEach
                    void tearDown() throws Throwable {
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
                    void shouldGetResource() throws Throwable {
                        assertThat(genericResource).isInstanceOf(Resource.class);
                    }
                    @Test
                    void shouldNotExist() throws Throwable {
                        assertThat(genericResource.exists()).isFalse();
                    }
                }
                @Nested
                class GivenContentIsAddedToThatResource {
                    @Nested
                    class Tests {
                        @BeforeEach
                        void setUp() throws Throwable {
                            context = new AnnotationConfigApplicationContext();
                                            context.register(TestConfig.class);
                                            context.refresh();

                                            repo = context.getBean(TestEntityRepository.class);
                                            store = context.getBean(TestEntityStore.class);
                                            storage = context.getBean(Storage.class);

                                            embeddedRepo = context.getBean(EmbeddedRepository.class);
                                            embeddedStore = context.getBean(EmbeddedStore.class);

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
                        void tearDown() throws Throwable {
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
                        void shouldStoreThatContent() throws Throwable {
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
                        void setUp() throws Throwable {
                            context = new AnnotationConfigApplicationContext();
                                            context.register(TestConfig.class);
                                            context.refresh();

                                            repo = context.getBean(TestEntityRepository.class);
                                            store = context.getBean(TestEntityStore.class);
                                            storage = context.getBean(Storage.class);

                                            embeddedRepo = context.getBean(EmbeddedRepository.class);
                                            embeddedStore = context.getBean(EmbeddedStore.class);

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
                        void tearDown() throws Throwable {
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
                        void shouldStoreThatUpdatedContent() throws Throwable {
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
                        void setUp() throws Throwable {
                            context = new AnnotationConfigApplicationContext();
                                            context.register(TestConfig.class);
                                            context.refresh();

                                            repo = context.getBean(TestEntityRepository.class);
                                            store = context.getBean(TestEntityStore.class);
                                            storage = context.getBean(Storage.class);

                                            embeddedRepo = context.getBean(EmbeddedRepository.class);
                                            embeddedStore = context.getBean(EmbeddedStore.class);

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
                                                            GCPStorageIT.this.e = e;
                                                        }
                        }
                        @AfterEach
                        void tearDown() throws Throwable {
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
                        void shouldNotExist() throws Throwable {
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
                    void setUp() throws Throwable {
                        context = new AnnotationConfigApplicationContext();
                                        context.register(TestConfig.class);
                                        context.refresh();

                                        repo = context.getBean(TestEntityRepository.class);
                                        store = context.getBean(TestEntityStore.class);
                                        storage = context.getBean(Storage.class);

                                        embeddedRepo = context.getBean(EmbeddedRepository.class);
                                        embeddedStore = context.getBean(EmbeddedStore.class);

                                        RandomString random = new RandomString(5);
                                        resourceLocation = random.nextString();

                        entity = new TestEntity();
                                            entity = repo.save(entity);
                    }
                    @AfterEach
                    void tearDown() throws Throwable {
                        context.close();
                    }
                    @Test
                    void shouldNotHaveAnAssociatedResource() throws Throwable {
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
                            void setUp() throws Throwable {
                                context = new AnnotationConfigApplicationContext();
                                                context.register(TestConfig.class);
                                                context.refresh();

                                                repo = context.getBean(TestEntityRepository.class);
                                                store = context.getBean(TestEntityStore.class);
                                                storage = context.getBean(Storage.class);

                                                embeddedRepo = context.getBean(EmbeddedRepository.class);
                                                embeddedStore = context.getBean(EmbeddedStore.class);

                                                RandomString random = new RandomString(5);
                                                resourceLocation = random.nextString();

                                entity = new TestEntity();
                                                    entity = repo.save(entity);

                                genericResource = store.getResource(resourceLocation);

                                store.associate(entity, resourceLocation);
                                                            store.associate(entity, PropertyPath.from("rendition"), resourceLocation);
                            }
                            @AfterEach
                            void tearDown() throws Throwable {
                                context.close();
                            }
                            @Test
                            void shouldBeRecordedAsSuchOnTheEntitySContentId() throws Throwable {
                                assertThat(entity.getContentId()).isEqualTo(resourceLocation);
                                                            assertThat(entity.getRenditionId()).isEqualTo(resourceLocation);
                            }
                        }
                        @Nested
                        class WhenTheResourceHasContent {
                            @BeforeEach
                            void setUp() throws Throwable {
                                context = new AnnotationConfigApplicationContext();
                                                context.register(TestConfig.class);
                                                context.refresh();

                                                repo = context.getBean(TestEntityRepository.class);
                                                store = context.getBean(TestEntityStore.class);
                                                storage = context.getBean(Storage.class);

                                                embeddedRepo = context.getBean(EmbeddedRepository.class);
                                                embeddedStore = context.getBean(EmbeddedStore.class);

                                                RandomString random = new RandomString(5);
                                                resourceLocation = random.nextString();

                                entity = new TestEntity();
                                                    entity = repo.save(entity);

                                genericResource = store.getResource(resourceLocation);

                                store.associate(entity, resourceLocation);
                                                            store.associate(entity, PropertyPath.from("rendition"), resourceLocation);

                                try (OutputStream os = ((WritableResource) genericResource).getOutputStream()) {
                                                                    os.write("Hello Client-side World!".getBytes());
                                                                }
                            }
                            @AfterEach
                            void tearDown() throws Throwable {
                                context.close();
                            }
                            @Test
                            void shouldNotHonorByteRanges() throws Throwable {
                                // relies on REST-layer to serve byte range
                                                                Resource r = store.getResource(entity, PropertyPath.from("content"), new GetResourceParams("5-10"));
                                                                try (InputStream is = r.getInputStream()) {
                                                                    assertThat(IOUtils.toString(is, Charset.defaultCharset())).isEqualTo("Hello Client-side World!");
                                                                }
                            }
                        }
                        @Nested
                        class WhenTheResourceIsUnassociated {
                            @BeforeEach
                            void setUp() throws Throwable {
                                context = new AnnotationConfigApplicationContext();
                                                context.register(TestConfig.class);
                                                context.refresh();

                                                repo = context.getBean(TestEntityRepository.class);
                                                store = context.getBean(TestEntityStore.class);
                                                storage = context.getBean(Storage.class);

                                                embeddedRepo = context.getBean(EmbeddedRepository.class);
                                                embeddedStore = context.getBean(EmbeddedStore.class);

                                                RandomString random = new RandomString(5);
                                                resourceLocation = random.nextString();

                                entity = new TestEntity();
                                                    entity = repo.save(entity);

                                genericResource = store.getResource(resourceLocation);

                                store.associate(entity, resourceLocation);
                                                            store.associate(entity, PropertyPath.from("rendition"), resourceLocation);

                                store.unassociate(entity);
                                                                store.unassociate(entity, PropertyPath.from("rendition"));
                            }
                            @AfterEach
                            void tearDown() throws Throwable {
                                context.close();
                            }
                            @Test
                            void shouldResetTheEntitySContentId() throws Throwable {
                                assertThat(entity.getContentId()).isNull();
                                                                assertThat(entity.getRenditionId()).isNull();
                            }
                        }
                        @Nested
                        class WhenAInvalidPropertyPathIsUsedToAssociateAResource {
                            @BeforeEach
                            void setUp() throws Throwable {
                                context = new AnnotationConfigApplicationContext();
                                                context.register(TestConfig.class);
                                                context.refresh();

                                                repo = context.getBean(TestEntityRepository.class);
                                                store = context.getBean(TestEntityStore.class);
                                                storage = context.getBean(Storage.class);

                                                embeddedRepo = context.getBean(EmbeddedRepository.class);
                                                embeddedStore = context.getBean(EmbeddedStore.class);

                                                RandomString random = new RandomString(5);
                                                resourceLocation = random.nextString();

                                entity = new TestEntity();
                                                    entity = repo.save(entity);

                                genericResource = store.getResource(resourceLocation);

                                store.associate(entity, resourceLocation);
                                                            store.associate(entity, PropertyPath.from("rendition"), resourceLocation);
                            }
                            @AfterEach
                            void tearDown() throws Throwable {
                                context.close();
                            }
                            @Test
                            void shouldThrowAnError() throws Throwable {
                                try {
                                                                        store.associate(entity, PropertyPath.from("does.not.exist"), resourceLocation);
                                                                    } catch (Exception sae) {
                                                                        GCPStorageIT.this.e = sae;
                                                                    }
                                                                    assertThat(e).isInstanceOf(StoreAccessException.class);
                            }
                        }
                        @Nested
                        class WhenAInvalidPropertyPathIsUsedToLoadAResource {
                            @BeforeEach
                            void setUp() throws Throwable {
                                context = new AnnotationConfigApplicationContext();
                                                context.register(TestConfig.class);
                                                context.refresh();

                                                repo = context.getBean(TestEntityRepository.class);
                                                store = context.getBean(TestEntityStore.class);
                                                storage = context.getBean(Storage.class);

                                                embeddedRepo = context.getBean(EmbeddedRepository.class);
                                                embeddedStore = context.getBean(EmbeddedStore.class);

                                                RandomString random = new RandomString(5);
                                                resourceLocation = random.nextString();

                                entity = new TestEntity();
                                                    entity = repo.save(entity);

                                genericResource = store.getResource(resourceLocation);

                                store.associate(entity, resourceLocation);
                                                            store.associate(entity, PropertyPath.from("rendition"), resourceLocation);
                            }
                            @AfterEach
                            void tearDown() throws Throwable {
                                context.close();
                            }
                            @Test
                            void shouldThrowAnError() throws Throwable {
                                try {
                                                                        store.getResource(entity, PropertyPath.from("does.not.exist"));
                                                                    } catch (Exception sae) {
                                                                        GCPStorageIT.this.e = sae;
                                                                    }
                                                                    assertThat(e).isInstanceOf(StoreAccessException.class);
                            }
                        }
                        @Nested
                        class WhenAInvalidPropertyPathIsUsedToUnassociateAResource {
                            @BeforeEach
                            void setUp() throws Throwable {
                                context = new AnnotationConfigApplicationContext();
                                                context.register(TestConfig.class);
                                                context.refresh();

                                                repo = context.getBean(TestEntityRepository.class);
                                                store = context.getBean(TestEntityStore.class);
                                                storage = context.getBean(Storage.class);

                                                embeddedRepo = context.getBean(EmbeddedRepository.class);
                                                embeddedStore = context.getBean(EmbeddedStore.class);

                                                RandomString random = new RandomString(5);
                                                resourceLocation = random.nextString();

                                entity = new TestEntity();
                                                    entity = repo.save(entity);

                                genericResource = store.getResource(resourceLocation);

                                store.associate(entity, resourceLocation);
                                                            store.associate(entity, PropertyPath.from("rendition"), resourceLocation);
                            }
                            @AfterEach
                            void tearDown() throws Throwable {
                                context.close();
                            }
                            @Test
                            void shouldThrowAnError() throws Throwable {
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
                void setUp() throws Throwable {
                    context = new AnnotationConfigApplicationContext();
                                    context.register(TestConfig.class);
                                    context.refresh();

                                    repo = context.getBean(TestEntityRepository.class);
                                    store = context.getBean(TestEntityStore.class);
                                    storage = context.getBean(Storage.class);

                                    embeddedRepo = context.getBean(EmbeddedRepository.class);
                                    embeddedStore = context.getBean(EmbeddedStore.class);

                                    RandomString random = new RandomString(5);
                                    resourceLocation = random.nextString();

                    entity = new TestEntity();
                                        entity = repo.save(entity);

                                        store.setContent(entity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                                        store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("<html>Hello Spring Content World!</html>".getBytes()));
                                        entity = repo.save(entity);
                }
                @AfterEach
                void tearDown() throws Throwable {
                    context.close();
                }
                @Test
                void shouldBeAbleToStoreNewContent() throws Throwable {
                    // content
                                        try (InputStream content = store.getContent(entity)) {
                                            assertThat(IOUtils.contentEquals(new ByteArrayInputStream("Hello Spring Content World!".getBytes()), content)).isTrue();
                                        } catch (IOException ignored) {
                                        }

                                        //rendition
                                        try (InputStream content = store.getContent(entity, PropertyPath.from("rendition"))) {
                                            assertThat(IOUtils.contentEquals(new ByteArrayInputStream("<html>Hello Spring Content World!</html>".getBytes()), content)).isTrue();
                                        } catch (IOException ignored) {
                                        }
                }
                @Test
                void shouldHaveContentMetadata() throws Throwable {
                    // content
                                        assertThat(entity.getContentId()).isNotNull();
                                        assertThat(entity.getContentId().trim().length()).isGreaterThan(0);
                                        assertThat(27L).isEqualTo((long) entity.getContentLen());

                                        //rendition
                                        assertThat(entity.getRenditionId()).isNotNull();
                                        assertThat(entity.getRenditionId().trim().length()).isGreaterThan(0);
                                        assertThat(40L).isEqualTo((long) entity.getRenditionLen());
                }
            }
            @Nested
            class WhenContentIsUpdated {
                @BeforeEach
                void setUp() throws Throwable {
                    context = new AnnotationConfigApplicationContext();
                                    context.register(TestConfig.class);
                                    context.refresh();

                                    repo = context.getBean(TestEntityRepository.class);
                                    store = context.getBean(TestEntityStore.class);
                                    storage = context.getBean(Storage.class);

                                    embeddedRepo = context.getBean(EmbeddedRepository.class);
                                    embeddedStore = context.getBean(EmbeddedStore.class);

                                    RandomString random = new RandomString(5);
                                    resourceLocation = random.nextString();

                    entity = new TestEntity();
                                        entity = repo.save(entity);

                                        store.setContent(entity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                                        store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("<html>Hello Spring Content World!</html>".getBytes()));
                                        entity = repo.save(entity);

                    store.setContent(entity, new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()));
                                            store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("<html>Hello Updated Spring Content World!</html>".getBytes()));
                                            entity = repo.save(entity);
                }
                @AfterEach
                void tearDown() throws Throwable {
                    context.close();
                }
                @Test
                void shouldHaveTheUpdatedContent() throws Throwable {
                    //content
                                            try (InputStream content = store.getContent(entity)) {
                                                boolean matches = IOUtils.contentEquals(new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()), content);
                                                assertThat(matches).isTrue();
                                            }

                                            //rendition
                                            try (InputStream content = store.getContent(entity, PropertyPath.from("rendition"))) {
                                                boolean matches = IOUtils.contentEquals(new ByteArrayInputStream("<html>Hello Updated Spring Content World!</html>".getBytes()), content);
                                                assertThat(matches).isTrue();
                                            }
                }
            }
            @Nested
            class WhenContentIsUpdatedWithShorterContent {
                @BeforeEach
                void setUp() throws Throwable {
                    context = new AnnotationConfigApplicationContext();
                                    context.register(TestConfig.class);
                                    context.refresh();

                                    repo = context.getBean(TestEntityRepository.class);
                                    store = context.getBean(TestEntityStore.class);
                                    storage = context.getBean(Storage.class);

                                    embeddedRepo = context.getBean(EmbeddedRepository.class);
                                    embeddedStore = context.getBean(EmbeddedStore.class);

                                    RandomString random = new RandomString(5);
                                    resourceLocation = random.nextString();

                    entity = new TestEntity();
                                        entity = repo.save(entity);

                                        store.setContent(entity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                                        store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("<html>Hello Spring Content World!</html>".getBytes()));
                                        entity = repo.save(entity);

                    store.setContent(entity, new ByteArrayInputStream("Hello Spring World!".getBytes()));
                                            store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("<html>Hello Spring World!</html>".getBytes()));
                                            entity = repo.save(entity);
                }
                @AfterEach
                void tearDown() throws Throwable {
                    context.close();
                }
                @Test
                void shouldStoreOnlyTheNewContent() throws Throwable {
                    //content
                                            try (InputStream content = store.getContent(entity)) {
                                                boolean matches = IOUtils.contentEquals(new ByteArrayInputStream("Hello Spring World!".getBytes()), content);
                                                assertThat(matches).isTrue();
                                            }

                                            //rendition
                                            try (InputStream content = store.getContent(entity, PropertyPath.from("rendition"))) {
                                                boolean matches = IOUtils.contentEquals(new ByteArrayInputStream("<html>Hello Spring World!</html>".getBytes()), content);
                                                assertThat(matches).isTrue();
                                            }
                }
            }
            @Nested
            class WhenContentIsUpdatedAndNotOverwritten {
                @BeforeEach
                void setUp() throws Throwable {
                    context = new AnnotationConfigApplicationContext();
                                    context.register(TestConfig.class);
                                    context.refresh();

                                    repo = context.getBean(TestEntityRepository.class);
                                    store = context.getBean(TestEntityStore.class);
                                    storage = context.getBean(Storage.class);

                                    embeddedRepo = context.getBean(EmbeddedRepository.class);
                                    embeddedStore = context.getBean(EmbeddedStore.class);

                                    RandomString random = new RandomString(5);
                                    resourceLocation = random.nextString();

                    entity = new TestEntity();
                                        entity = repo.save(entity);

                                        store.setContent(entity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                                        store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("<html>Hello Spring Content World!</html>".getBytes()));
                                        entity = repo.save(entity);
                }
                @AfterEach
                void tearDown() throws Throwable {
                    context.close();
                }
                @Test
                void shouldHaveTheUpdatedContent() throws Throwable {
                    String contentId = entity.getContentId();
                                                assertThat(contentId).isNotNull();
                                                assertThat(storage.get(BlobId.of("test-bucket", contentId)).exists()).isTrue();

                                                store.setContent(entity, PropertyPath.from("content"), new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()), new SetContentParams(-1, true, SetContentParams.ContentDisposition.CreateNew));
                                                entity = repo.save(entity);

                                                try (InputStream content = store.getContent(entity)) {
                                                    boolean matches = IOUtils.contentEquals(new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()), content);
                                                    assertThat(matches).isTrue();
                                                }

                                                assertThat(entity.getContentId()).isNotEqualTo(contentId);

                                                assertThat(storage.get(BlobId.of("test-bucket", entity.getContentId())).exists()).isTrue();
                }
            }
            @Nested
            class WhenContentIsUnset {
                @BeforeEach
                void setUp() throws Throwable {
                    context = new AnnotationConfigApplicationContext();
                                    context.register(TestConfig.class);
                                    context.refresh();

                                    repo = context.getBean(TestEntityRepository.class);
                                    store = context.getBean(TestEntityStore.class);
                                    storage = context.getBean(Storage.class);

                                    embeddedRepo = context.getBean(EmbeddedRepository.class);
                                    embeddedStore = context.getBean(EmbeddedStore.class);

                                    RandomString random = new RandomString(5);
                                    resourceLocation = random.nextString();

                    entity = new TestEntity();
                                        entity = repo.save(entity);

                                        store.setContent(entity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                                        store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("<html>Hello Spring Content World!</html>".getBytes()));
                                        entity = repo.save(entity);

                    resourceLocation = entity.getContentId().toString();
                                            entity = store.unsetContent(entity);
                                            entity = store.unsetContent(entity, PropertyPath.from("rendition"));
                                            entity = repo.save(entity);
                }
                @AfterEach
                void tearDown() throws Throwable {
                    context.close();
                }
                @Test
                void shouldHaveNoContent() throws Throwable {
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
                                            assertThat(entity.getRenditionLen()).isEqualTo(0L);
                }
            }
            @Nested
            class WhenContentIsUnsetButKept {
                @BeforeEach
                void setUp() throws Throwable {
                    context = new AnnotationConfigApplicationContext();
                                    context.register(TestConfig.class);
                                    context.refresh();

                                    repo = context.getBean(TestEntityRepository.class);
                                    store = context.getBean(TestEntityStore.class);
                                    storage = context.getBean(Storage.class);

                                    embeddedRepo = context.getBean(EmbeddedRepository.class);
                                    embeddedStore = context.getBean(EmbeddedStore.class);

                                    RandomString random = new RandomString(5);
                                    resourceLocation = random.nextString();

                    entity = new TestEntity();
                                        entity = repo.save(entity);

                                        store.setContent(entity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                                        store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("<html>Hello Spring Content World!</html>".getBytes()));
                                        entity = repo.save(entity);

                    resourceLocation = entity.getContentId().toString();
                                            entity = store.unsetContent(entity, PropertyPath.from("content"), new UnsetContentParams(UnsetContentParams.Disposition.Keep));
                                            entity = repo.save(entity);
                }
                @AfterEach
                void tearDown() throws Throwable {
                    context.close();
                }
                @Test
                void shouldHaveNoContent() throws Throwable {
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
            class WhenAnInvalidPropertyPathIsUsedToSetContent {
                @BeforeEach
                void setUp() throws Throwable {
                    context = new AnnotationConfigApplicationContext();
                                    context.register(TestConfig.class);
                                    context.refresh();

                                    repo = context.getBean(TestEntityRepository.class);
                                    store = context.getBean(TestEntityStore.class);
                                    storage = context.getBean(Storage.class);

                                    embeddedRepo = context.getBean(EmbeddedRepository.class);
                                    embeddedStore = context.getBean(EmbeddedStore.class);

                                    RandomString random = new RandomString(5);
                                    resourceLocation = random.nextString();

                    entity = new TestEntity();
                                        entity = repo.save(entity);

                                        store.setContent(entity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                                        store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("<html>Hello Spring Content World!</html>".getBytes()));
                                        entity = repo.save(entity);
                }
                @AfterEach
                void tearDown() throws Throwable {
                    context.close();
                }
                @Test
                void shouldThrowAnError() throws Throwable {
                    try {
                                                    store.setContent(entity, PropertyPath.from("does.not.exist"), new ByteArrayInputStream("foo".getBytes()));
                                                } catch (Exception sae) {
                                                    GCPStorageIT.this.e = sae;
                                                }
                                                assertThat(e).isInstanceOf(StoreAccessException.class);
                }
            }
            @Nested
            class WhenAnInvalidPropertyPathIsUsedToGetContent {
                @BeforeEach
                void setUp() throws Throwable {
                    context = new AnnotationConfigApplicationContext();
                                    context.register(TestConfig.class);
                                    context.refresh();

                                    repo = context.getBean(TestEntityRepository.class);
                                    store = context.getBean(TestEntityStore.class);
                                    storage = context.getBean(Storage.class);

                                    embeddedRepo = context.getBean(EmbeddedRepository.class);
                                    embeddedStore = context.getBean(EmbeddedStore.class);

                                    RandomString random = new RandomString(5);
                                    resourceLocation = random.nextString();

                    entity = new TestEntity();
                                        entity = repo.save(entity);

                                        store.setContent(entity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                                        store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("<html>Hello Spring Content World!</html>".getBytes()));
                                        entity = repo.save(entity);
                }
                @AfterEach
                void tearDown() throws Throwable {
                    context.close();
                }
                @Test
                void shouldThrowAnError() throws Throwable {
                    try {
                                                    store.getContent(entity, PropertyPath.from("does.not.exist"));
                                                } catch (Exception sae) {
                                                    GCPStorageIT.this.e = sae;
                                                }
                                                assertThat(e).isInstanceOf(StoreAccessException.class);
                }
            }
            @Nested
            class WhenAnInvalidPropertyPathIsUsedToUnsetContent {
                @BeforeEach
                void setUp() throws Throwable {
                    context = new AnnotationConfigApplicationContext();
                                    context.register(TestConfig.class);
                                    context.refresh();

                                    repo = context.getBean(TestEntityRepository.class);
                                    store = context.getBean(TestEntityStore.class);
                                    storage = context.getBean(Storage.class);

                                    embeddedRepo = context.getBean(EmbeddedRepository.class);
                                    embeddedStore = context.getBean(EmbeddedStore.class);

                                    RandomString random = new RandomString(5);
                                    resourceLocation = random.nextString();

                    entity = new TestEntity();
                                        entity = repo.save(entity);

                                        store.setContent(entity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                                        store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("<html>Hello Spring Content World!</html>".getBytes()));
                                        entity = repo.save(entity);
                }
                @AfterEach
                void tearDown() throws Throwable {
                    context.close();
                }
                @Test
                void shouldThrowAnError() throws Throwable {
                    try {
                                                    store.unsetContent(entity, PropertyPath.from("does.not.exist"));
                                                } catch (Exception sae) {
                                                    GCPStorageIT.this.e = sae;
                                                }
                                                assertThat(e).isInstanceOf(StoreAccessException.class);
                }
            }
            @Nested
            class WhenContentIsDeletedAndTheContentIdFieldIsSharedWithEntityId {
                @BeforeEach
                void setUp() throws Throwable {
                    context = new AnnotationConfigApplicationContext();
                                    context.register(TestConfig.class);
                                    context.refresh();

                                    repo = context.getBean(TestEntityRepository.class);
                                    store = context.getBean(TestEntityStore.class);
                                    storage = context.getBean(Storage.class);

                                    embeddedRepo = context.getBean(EmbeddedRepository.class);
                                    embeddedStore = context.getBean(EmbeddedStore.class);

                                    RandomString random = new RandomString(5);
                                    resourceLocation = random.nextString();

                    entity = new TestEntity();
                                        entity = repo.save(entity);

                                        store.setContent(entity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                                        store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("<html>Hello Spring Content World!</html>".getBytes()));
                                        entity = repo.save(entity);
                }
                @AfterEach
                void tearDown() throws Throwable {
                    context.close();
                }
                @Test
                void shouldNotResetTheIdField() throws Throwable {
                    SharedIdRepository sharedIdRepository = context.getBean(SharedIdRepository.class);
                                                SharedIdStore sharedIdStore = context.getBean(SharedIdStore.class);

                                                SharedIdContentIdEntity sharedIdContentIdEntity = sharedIdRepository.save(new SharedIdContentIdEntity());

                                                sharedIdContentIdEntity = sharedIdStore.setContent(sharedIdContentIdEntity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                                                sharedIdContentIdEntity = sharedIdRepository.save(sharedIdContentIdEntity);
                                                String id = sharedIdContentIdEntity.getContentId();
                                                sharedIdContentIdEntity = sharedIdStore.unsetContent(sharedIdContentIdEntity);
                                                assertThat(sharedIdContentIdEntity.getContentId()).isEqualTo(id);
                                                assertThat(sharedIdContentIdEntity.getContentLen()).isEqualTo(0L);
                }
            }
            @Nested
            class EmbeddedContent {
                @Nested
                class GivenAEntityWithANullEmbeddedContentObject {
                    @BeforeEach
                    void setUp() throws Throwable {
                        context = new AnnotationConfigApplicationContext();
                                        context.register(TestConfig.class);
                                        context.refresh();

                                        repo = context.getBean(TestEntityRepository.class);
                                        store = context.getBean(TestEntityStore.class);
                                        storage = context.getBean(Storage.class);

                                        embeddedRepo = context.getBean(EmbeddedRepository.class);
                                        embeddedStore = context.getBean(EmbeddedStore.class);

                                        RandomString random = new RandomString(5);
                                        resourceLocation = random.nextString();

                        entity = new TestEntity();
                                            entity = repo.save(entity);

                                            store.setContent(entity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                                            store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("<html>Hello Spring Content World!</html>".getBytes()));
                                            entity = repo.save(entity);
                    }
                    @AfterEach
                    void tearDown() throws Throwable {
                        context.close();
                    }
                    @Test
                    void shouldReturnNullWhenContentIsFetched() throws Throwable {
                        EntityWithEmbeddedContent entity = embeddedRepo.save(new EntityWithEmbeddedContent());
                                                                assertThat(embeddedStore.getContent(entity, PropertyPath.from("content"))).isNull();
                    }
                    @Test
                    void shouldBeSuccessfulWhenContentIsSet() throws Throwable {
                        EntityWithEmbeddedContent entity = embeddedRepo.save(new EntityWithEmbeddedContent());
                                                                embeddedStore.setContent(entity, PropertyPath.from("content"), new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                                                                try (InputStream is = embeddedStore.getContent(entity, PropertyPath.from("content"))) {
                                                                    assertThat(IOUtils.contentEquals(is, new ByteArrayInputStream("Hello Spring Content World!".getBytes()))).isTrue();
                                                                }
                    }
                    @Test
                    void shouldReturnNullWhenContentIsUnset() throws Throwable {
                        EntityWithEmbeddedContent entity = embeddedRepo.save(new EntityWithEmbeddedContent());
                                                                assertThat(embeddedStore.unsetContent(entity, PropertyPath.from("content"))).isSameAs(entity);
                                                                int i = 0;
                    }
                }
            }
        }
    }


    @Test
    public void test() {
        // noop
    }

    @Configuration
    @EnableJpaRepositories(basePackages = "internal.org.springframework.content.gcs.it", considerNestedRepositories = true)
    @EnableGCPStorage(basePackages = "internal.org.springframework.content.gcs.it")
    @Import(InfrastructureConfig.class)
    public static class TestConfig {

        @Bean
        public static Storage storage() {
            return LocalStorageHelper.getOptions().getService();
        }
    }

    @Configuration
    public static class InfrastructureConfig {

        @Bean
        public DataSource dataSource() {
            EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
            return builder.setType(EmbeddedDatabaseType.H2).build();
        }

        @Bean
        public LocalContainerEntityManagerFactoryBean entityManagerFactory() {

            HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
            vendorAdapter.setDatabase(Database.H2);
            vendorAdapter.setGenerateDdl(true);

            LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
            factory.setJpaVendorAdapter(vendorAdapter);
            factory.setPackagesToScan("internal.org.springframework.content.gcs.it");
            factory.setDataSource(dataSource());

            return factory;
        }

        @Bean
        public PlatformTransactionManager transactionManager() {

            JpaTransactionManager txManager = new JpaTransactionManager();
            txManager.setEntityManagerFactory(entityManagerFactory().getObject());
            return txManager;
        }
    }

    @Entity
    public static class TestEntity {

        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        private Long id;

        @ContentId
        private String contentId;

        @ContentLength
        private Long contentLen;

        @ContentId
        private String renditionId;

        @ContentLength
        private long renditionLen;

        public TestEntity() {
        }

        public TestEntity(String contentId) {
            this.contentId = contentId;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getContentId() {
            return contentId;
        }

        public void setContentId(String contentId) {
            this.contentId = contentId;
        }

        public Long getContentLen() {
            return contentLen;
        }

        public void setContentLen(Long contentLen) {
            this.contentLen = contentLen;
        }

        public String getRenditionId() {
            return renditionId;
        }

        public void setRenditionId(String renditionId) {
            this.renditionId = renditionId;
        }

        public long getRenditionLen() {
            return renditionLen;
        }

        public void setRenditionLen(long renditionLen) {
            this.renditionLen = renditionLen;
        }
    }

    public interface TestEntityRepository extends JpaRepository<TestEntity, Long> {
    }

    public interface TestEntityStore extends ContentStore<TestEntity, String> {
    }

    @Entity
    public static class SharedIdContentIdEntity {

        @jakarta.persistence.Id
        @ContentId
        private String contentId = UuidCreator.getTimeOrdered().toString();

        @ContentLength
        private long contentLen;

        public SharedIdContentIdEntity() {
        }

        public String getContentId() {
            return contentId;
        }

        public void setContentId(String contentId) {
            this.contentId = contentId;
        }

        public long getContentLen() {
            return contentLen;
        }

        public void setContentLen(long contentLen) {
            this.contentLen = contentLen;
        }
    }

    public interface SharedIdRepository extends JpaRepository<SharedIdContentIdEntity, String> {
    }

    public interface SharedIdStore extends ContentStore<SharedIdContentIdEntity, String> {
    }

    @Entity
    @Table(name = "entity_with_embedded")
    public static class EntityWithEmbeddedContent {

        @Id
        private String id = UuidCreator.getTimeOrdered().toString();

        @Embedded
        private EmbeddedContent content;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public EmbeddedContent getContent() {
            return content;
        }

        public void setContent(EmbeddedContent content) {
            this.content = content;
        }
    }

    @Embeddable
    public static class EmbeddedContent {

        @ContentId
        private String contentId;

        @ContentLength
        private Long contentLen;

        public String getContentId() {
            return contentId;
        }

        public void setContentId(String contentId) {
            this.contentId = contentId;
        }

        public Long getContentLen() {
            return contentLen;
        }

        public void setContentLen(Long contentLen) {
            this.contentLen = contentLen;
        }
    }

    public interface EmbeddedRepository extends JpaRepository<EntityWithEmbeddedContent, String> {
    }

    public interface EmbeddedStore extends ContentStore<EntityWithEmbeddedContent, String> {
    }
}
