package it.store;

import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import net.bytebuddy.utility.RandomString;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.*;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.io.DeletableResource;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.*;
import org.springframework.content.fs.config.EnableFileSystemStores;
import org.springframework.content.fs.io.FileSystemResourceLoader;
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
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;

public class FileSystemStoreIT {

    private FileSystemStoreIT.TEntity entity;
    private Resource genericResource;
    private Exception e;

    private AnnotationConfigApplicationContext context;

    private TestEntityRepository repo;
    private TestEntityStore store;

    private EmbeddedRepository embeddedRepo;
    private EmbeddedStore embeddedStore;

    private String resourceLocation;

    @Test
    public void test() {
        // noop
    }

    public interface ContentProperty {
        String getContentId();

        void setContentId(String contentId);

        Long getContentLen();

        void setContentLen(Long contentLen);
    }

    public interface TestEntityRepository extends JpaRepository<TEntity, String> {
    }

    public interface TestEntityStore extends ContentStore<TEntity, String> {
    }

    public interface SharedIdRepository extends JpaRepository<SharedIdContentIdEntity, String> {
    }

    public interface SharedIdStore extends ContentStore<SharedIdContentIdEntity, String> {
    }

    public interface EmbeddedRepository extends JpaRepository<EntityWithEmbeddedContent, String> {
    }

    public interface EmbeddedStore extends ContentStore<EntityWithEmbeddedContent, String> {
    }

    @Disabled("It's not a test and must not be considered as one.")
    @Configuration
    @EnableJpaRepositories(considerNestedRepositories = true)
    @EnableFileSystemStores
    @Import(InfrastructureConfig.class)
    public static class TestConfig {
    }

    @Disabled("It's not a test and must not be considered as one.")
    @Configuration
    public static class InfrastructureConfig {

        @Bean
        File filesystemRoot() {
            try {
                return Files.createTempDirectory("").toFile();
            } catch (IOException ignored) {
            }
            return null;
        }

        @Bean
        FileSystemResourceLoader fileSystemResourceLoader() {
            return new FileSystemResourceLoader(filesystemRoot().getAbsolutePath());
        }

        @Bean
        public DataSource dataSource() {
            EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
            return builder.setType(EmbeddedDatabaseType.HSQL).build();
        }

        @Bean
        public LocalContainerEntityManagerFactoryBean entityManagerFactory() {

            HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
            vendorAdapter.setDatabase(Database.HSQL);
            vendorAdapter.setGenerateDdl(true);

            LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
            factory.setJpaVendorAdapter(vendorAdapter);
            factory.setPackagesToScan("it.store");
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

    @Disabled("It's not a test and must not be considered as one.")
    @Entity
    @Table(name = "tentity")
    public static class TEntity implements ContentProperty {

        @Id
        private String id = UuidCreator.getTimeOrdered().toString();

        @ContentId
        private String contentId;

        @ContentLength
        private Long contentLen;

        @ContentId
        private String renditionId;

        @ContentLength
        private long renditionLen;

        public TEntity() {
        }

        @Override
        public String getContentId() {
            return this.contentId;
        }

        @Override
        public void setContentId(String contentId) {
            this.contentId = contentId;
        }

        @Override
        public Long getContentLen() {
            return contentLen;
        }

        @Override
        public void setContentLen(Long contentLen) {
            this.contentLen = contentLen;
        }

        public String getRenditionId() {
            return this.renditionId;
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

    @Disabled("It's not a test and must not be considered as one.")
    @Entity
    @Table(name = "shared_id_entity")
    public static class SharedIdContentIdEntity implements ContentProperty {

        @Id
        @ContentId
        private String contentId = UuidCreator.getTimeOrdered().toString();

        @ContentLength
        private Long contentLen;

        @Override
        public String getContentId() {
            return this.contentId;
        }

        @Override
        public void setContentId(String contentId) {
            this.contentId = contentId;
        }

        @Override
        public Long getContentLen() {
            return contentLen;
        }

        @Override
        public void setContentLen(Long contentLen) {
            this.contentLen = contentLen;
        }
    }

    @Disabled("It's not a test and must not be considered as one.")
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

    @Disabled("It's not a test and must not be considered as one.")
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

    @Nested
    class DefaultFileSystemStoreImplCases {
        @Nested
        class StoreCases {
            @Nested
            class GetResource {
                @Nested
                class Tests {
                    @BeforeEach
                    void setUp() {
                        context = new AnnotationConfigApplicationContext();
                        context.register(FileSystemStoreIT.TestConfig.class);
                        context.refresh();

                        repo = context.getBean(TestEntityRepository.class);
                        store = context.getBean(TestEntityStore.class);

                        RandomString random = new RandomString(5);
                        resourceLocation = random.nextString();

                        embeddedRepo = context.getBean(EmbeddedRepository.class);
                        embeddedStore = context.getBean(EmbeddedStore.class);

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
                            context.register(FileSystemStoreIT.TestConfig.class);
                            context.refresh();

                            repo = context.getBean(TestEntityRepository.class);
                            store = context.getBean(TestEntityStore.class);

                            RandomString random = new RandomString(5);
                            resourceLocation = random.nextString();

                            embeddedRepo = context.getBean(EmbeddedRepository.class);
                            embeddedStore = context.getBean(EmbeddedStore.class);

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
                                    assertThat(IOUtils.contentEquals(expected, actual)).isTrue();
                                }
                            }
                        }
                    }

                    @Nested
                    class GivenThatResourceIsThenUpdated {
                        @BeforeEach
                        void setUp() throws IOException {
                            context = new AnnotationConfigApplicationContext();
                            context.register(FileSystemStoreIT.TestConfig.class);
                            context.refresh();

                            repo = context.getBean(TestEntityRepository.class);
                            store = context.getBean(TestEntityStore.class);

                            RandomString random = new RandomString(5);
                            resourceLocation = random.nextString();

                            embeddedRepo = context.getBean(EmbeddedRepository.class);
                            embeddedStore = context.getBean(EmbeddedStore.class);

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
                            context.register(FileSystemStoreIT.TestConfig.class);
                            context.refresh();

                            repo = context.getBean(TestEntityRepository.class);
                            store = context.getBean(TestEntityStore.class);

                            RandomString random = new RandomString(5);
                            resourceLocation = random.nextString();

                            embeddedRepo = context.getBean(EmbeddedRepository.class);
                            embeddedStore = context.getBean(EmbeddedStore.class);

                            genericResource = store.getResource(resourceLocation);

                            try (InputStream is = new ByteArrayInputStream("Hello Spring Content World!".getBytes())) {
                                try (OutputStream os = ((WritableResource) genericResource).getOutputStream()) {
                                    IOUtils.copy(is, os);
                                }
                            }

                            try {
                                ((DeletableResource) genericResource).delete();
                            } catch (Exception e) {
                                FileSystemStoreIT.this.e = e;
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
                        context.register(FileSystemStoreIT.TestConfig.class);
                        context.refresh();

                        repo = context.getBean(TestEntityRepository.class);
                        store = context.getBean(TestEntityStore.class);

                        RandomString random = new RandomString(5);
                        resourceLocation = random.nextString();

                        embeddedRepo = context.getBean(EmbeddedRepository.class);
                        embeddedStore = context.getBean(EmbeddedStore.class);

                        entity = new TEntity();
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
                                context.register(FileSystemStoreIT.TestConfig.class);
                                context.refresh();

                                repo = context.getBean(TestEntityRepository.class);
                                store = context.getBean(TestEntityStore.class);

                                RandomString random = new RandomString(5);
                                resourceLocation = random.nextString();

                                embeddedRepo = context.getBean(EmbeddedRepository.class);
                                embeddedStore = context.getBean(EmbeddedStore.class);

                                entity = new TEntity();
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
                                context.register(FileSystemStoreIT.TestConfig.class);
                                context.refresh();

                                repo = context.getBean(TestEntityRepository.class);
                                store = context.getBean(TestEntityStore.class);

                                RandomString random = new RandomString(5);
                                resourceLocation = random.nextString();

                                embeddedRepo = context.getBean(EmbeddedRepository.class);
                                embeddedStore = context.getBean(EmbeddedStore.class);

                                entity = new TEntity();
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
                        class WhenTheResourceIsUnassociated {
                            @BeforeEach
                            void setUp() {
                                context = new AnnotationConfigApplicationContext();
                                context.register(FileSystemStoreIT.TestConfig.class);
                                context.refresh();

                                repo = context.getBean(TestEntityRepository.class);
                                store = context.getBean(TestEntityStore.class);

                                RandomString random = new RandomString(5);
                                resourceLocation = random.nextString();

                                embeddedRepo = context.getBean(EmbeddedRepository.class);
                                embeddedStore = context.getBean(EmbeddedStore.class);

                                entity = new TEntity();
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
                                context.register(FileSystemStoreIT.TestConfig.class);
                                context.refresh();

                                repo = context.getBean(TestEntityRepository.class);
                                store = context.getBean(TestEntityStore.class);

                                RandomString random = new RandomString(5);
                                resourceLocation = random.nextString();

                                embeddedRepo = context.getBean(EmbeddedRepository.class);
                                embeddedStore = context.getBean(EmbeddedStore.class);

                                entity = new TEntity();
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
                                    FileSystemStoreIT.this.e = sae;
                                }
                                assertThat(e).isInstanceOf(StoreAccessException.class);
                            }
                        }

                        @Nested
                        class WhenAInvalidPropertyPathIsUsedToLoadAResource {
                            @BeforeEach
                            void setUp() {
                                context = new AnnotationConfigApplicationContext();
                                context.register(FileSystemStoreIT.TestConfig.class);
                                context.refresh();

                                repo = context.getBean(TestEntityRepository.class);
                                store = context.getBean(TestEntityStore.class);

                                RandomString random = new RandomString(5);
                                resourceLocation = random.nextString();

                                embeddedRepo = context.getBean(EmbeddedRepository.class);
                                embeddedStore = context.getBean(EmbeddedStore.class);

                                entity = new TEntity();
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
                                    FileSystemStoreIT.this.e = sae;
                                }
                                assertThat(e).isInstanceOf(StoreAccessException.class);
                            }
                        }

                        @Nested
                        class WhenAInvalidPropertyPathIsUsedToUnassociateAResource {
                            @BeforeEach
                            void setUp() {
                                context = new AnnotationConfigApplicationContext();
                                context.register(FileSystemStoreIT.TestConfig.class);
                                context.refresh();

                                repo = context.getBean(TestEntityRepository.class);
                                store = context.getBean(TestEntityStore.class);

                                RandomString random = new RandomString(5);
                                resourceLocation = random.nextString();

                                embeddedRepo = context.getBean(EmbeddedRepository.class);
                                embeddedStore = context.getBean(EmbeddedStore.class);

                                entity = new TEntity();
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
                                    FileSystemStoreIT.this.e = sae;
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
                    context.register(FileSystemStoreIT.TestConfig.class);
                    context.refresh();

                    repo = context.getBean(TestEntityRepository.class);
                    store = context.getBean(TestEntityStore.class);

                    RandomString random = new RandomString(5);
                    resourceLocation = random.nextString();

                    embeddedRepo = context.getBean(EmbeddedRepository.class);
                    embeddedStore = context.getBean(EmbeddedStore.class);

                    entity = new FileSystemStoreIT.TEntity();
                    entity = repo.save(entity);

                    store.setContent(entity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                    store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("<html>Hello Spring Content World!</html>".getBytes()));
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
                        assertThat(IOUtils.contentEquals(new ByteArrayInputStream("<html>Hello Spring Content World!</html>".getBytes()), content)).isTrue();
                    } catch (IOException ignored) {
                    }
                }

                @Test
                void shouldHaveContentMetadata() {
                    // content
                    assertThat(entity.getContentId()).isNotNull();
                    assertThat(entity.getContentId().trim().length()).isGreaterThan(0);
                    assertThat(entity.getContentLen()).isEqualTo(Long.valueOf(27L));

                    //rendition
                    assertThat(entity.getRenditionId()).isNotNull();
                    assertThat(entity.getRenditionId().trim().length()).isGreaterThan(0);
                    assertThat(40L).isEqualTo(entity.getRenditionLen());
                }
            }

            @Nested
            class WhenContentIsUpdated {
                @BeforeEach
                void setUp() {
                    context = new AnnotationConfigApplicationContext();
                    context.register(FileSystemStoreIT.TestConfig.class);
                    context.refresh();

                    repo = context.getBean(TestEntityRepository.class);
                    store = context.getBean(TestEntityStore.class);

                    RandomString random = new RandomString(5);
                    resourceLocation = random.nextString();

                    embeddedRepo = context.getBean(EmbeddedRepository.class);
                    embeddedStore = context.getBean(EmbeddedStore.class);

                    entity = new FileSystemStoreIT.TEntity();
                    entity = repo.save(entity);

                    store.setContent(entity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                    store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("<html>Hello Spring Content World!</html>".getBytes()));
                }

                @AfterEach
                void tearDown() {
                    context.close();
                }

                @Test
                void shouldHaveTheUpdatedContent() throws IOException {
                    FileSystemResourceLoader loader = context.getBean(FileSystemResourceLoader.class);
                    String contentId = entity.getContentId();
                    assertThat(new File(loader.getRootResource().getPath(), contentId).exists()).isTrue();
                    String renditionId = entity.getRenditionId();
                    assertThat(new File(loader.getRootResource().getPath(), renditionId).exists()).isTrue();
                    store.setContent(entity, new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()));
                    store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("<html>Hello Updated Spring Content World!</html>".getBytes()));
                    entity = repo.save(entity);

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

                    assertThat(entity.getContentId()).isEqualTo(contentId);
                    assertThat(entity.getRenditionId()).isEqualTo(renditionId);

                    assertThat(new File(loader.getRootResource().getPath(), entity.getContentId()).exists()).isTrue();
                    assertThat(new File(loader.getRootResource().getPath(), entity.getRenditionId()).exists()).isTrue();
                }
            }

            @Nested
            class WhenContentIsUpdatedWithShorterContent {
                @BeforeEach
                void setUp() {
                    context = new AnnotationConfigApplicationContext();
                    context.register(FileSystemStoreIT.TestConfig.class);
                    context.refresh();

                    repo = context.getBean(TestEntityRepository.class);
                    store = context.getBean(TestEntityStore.class);

                    RandomString random = new RandomString(5);
                    resourceLocation = random.nextString();

                    embeddedRepo = context.getBean(EmbeddedRepository.class);
                    embeddedStore = context.getBean(EmbeddedStore.class);

                    entity = new FileSystemStoreIT.TEntity();
                    entity = repo.save(entity);

                    store.setContent(entity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                    store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("<html>Hello Spring Content World!</html>".getBytes()));

                    store.setContent(entity, new ByteArrayInputStream("Hello Spring World!".getBytes()));
                    store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("<html>Hello Spring World!</html>".getBytes()));
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
                        boolean matches = IOUtils.contentEquals(new ByteArrayInputStream("<html>Hello Spring World!</html>".getBytes()), content);
                        assertThat(matches).isTrue();
                    }
                }
            }

            @Nested
            class WhenContentIsUpdatedAndNotOverwritten {
                @BeforeEach
                void setUp() {
                    context = new AnnotationConfigApplicationContext();
                    context.register(FileSystemStoreIT.TestConfig.class);
                    context.refresh();

                    repo = context.getBean(TestEntityRepository.class);
                    store = context.getBean(TestEntityStore.class);

                    RandomString random = new RandomString(5);
                    resourceLocation = random.nextString();

                    embeddedRepo = context.getBean(EmbeddedRepository.class);
                    embeddedStore = context.getBean(EmbeddedStore.class);

                    entity = new FileSystemStoreIT.TEntity();
                    entity = repo.save(entity);

                    store.setContent(entity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                    store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("<html>Hello Spring Content World!</html>".getBytes()));
                }

                @AfterEach
                void tearDown() {
                    context.close();
                }

                @Test
                void shouldHaveTheUpdatedContent() throws IOException {
                    FileSystemResourceLoader loader = context.getBean(FileSystemResourceLoader.class);
                    String contentId = entity.getContentId();
                    assertThat(new File(loader.getRootResource().getPath(), contentId).exists()).isTrue();

                    store.setContent(entity, PropertyPath.from("content"), new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()), new SetContentParams(-1, true, SetContentParams.ContentDisposition.CreateNew));
                    entity = repo.save(entity);

                    try (InputStream content = store.getContent(entity)) {
                        boolean matches = IOUtils.contentEquals(new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()), content);
                        assertThat(matches).isTrue();
                    }

                    assertThat(new File(loader.getRootResource().getPath(), contentId).exists()).isTrue();

                    assertThat(entity.getContentId()).isNotEqualTo(contentId);

                    assertThat(new File(loader.getRootResource().getPath(), entity.getContentId()).exists()).isTrue();
                }
            }

            @Nested
            class WhenContentIsUnset {
                @BeforeEach
                void setUp() {
                    context = new AnnotationConfigApplicationContext();
                    context.register(FileSystemStoreIT.TestConfig.class);
                    context.refresh();

                    repo = context.getBean(TestEntityRepository.class);
                    store = context.getBean(TestEntityStore.class);

                    RandomString random = new RandomString(5);
                    resourceLocation = random.nextString();

                    embeddedRepo = context.getBean(EmbeddedRepository.class);
                    embeddedStore = context.getBean(EmbeddedStore.class);

                    entity = new FileSystemStoreIT.TEntity();
                    entity = repo.save(entity);

                    store.setContent(entity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                    store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("<html>Hello Spring Content World!</html>".getBytes()));

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

                    //rendition
                    try (InputStream content = store.getContent(entity, PropertyPath.from("rendition"))) {
                        assertThat(content).isNull();
                    }

                    assertThat(entity.getRenditionId()).isNull();
                    assertThat(entity.getRenditionLen()).isEqualTo(0L);

                    FileSystemResourceLoader loader = context.getBean(FileSystemResourceLoader.class);
                    assertThat(new File(loader.getRootResource().getPath(), resourceLocation).exists()).isFalse();
                }
            }

            @Nested
            class WhenContentIsUnsetButKept {
                @BeforeEach
                void setUp() {
                    context = new AnnotationConfigApplicationContext();
                    context.register(FileSystemStoreIT.TestConfig.class);
                    context.refresh();

                    repo = context.getBean(TestEntityRepository.class);
                    store = context.getBean(TestEntityStore.class);

                    RandomString random = new RandomString(5);
                    resourceLocation = random.nextString();

                    embeddedRepo = context.getBean(EmbeddedRepository.class);
                    embeddedStore = context.getBean(EmbeddedStore.class);

                    entity = new FileSystemStoreIT.TEntity();
                    entity = repo.save(entity);

                    store.setContent(entity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                    store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("<html>Hello Spring Content World!</html>".getBytes()));

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

                    FileSystemResourceLoader loader = context.getBean(FileSystemResourceLoader.class);
                    assertThat(new File(loader.getRootResource().getPath(), resourceLocation).exists()).isTrue();
                }
            }

            @Nested
            class WhenAnInvalidPropertyPathIsUsedToSetContent {
                @BeforeEach
                void setUp() {
                    context = new AnnotationConfigApplicationContext();
                    context.register(FileSystemStoreIT.TestConfig.class);
                    context.refresh();

                    repo = context.getBean(TestEntityRepository.class);
                    store = context.getBean(TestEntityStore.class);

                    RandomString random = new RandomString(5);
                    resourceLocation = random.nextString();

                    embeddedRepo = context.getBean(EmbeddedRepository.class);
                    embeddedStore = context.getBean(EmbeddedStore.class);

                    entity = new FileSystemStoreIT.TEntity();
                    entity = repo.save(entity);

                    store.setContent(entity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                    store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("<html>Hello Spring Content World!</html>".getBytes()));
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
                        FileSystemStoreIT.this.e = sae;
                    }
                    assertThat(e).isInstanceOf(StoreAccessException.class);
                }
            }

            @Nested
            class WhenAnInvalidPropertyPathIsUsedToGetContent {
                @BeforeEach
                void setUp() {
                    context = new AnnotationConfigApplicationContext();
                    context.register(FileSystemStoreIT.TestConfig.class);
                    context.refresh();

                    repo = context.getBean(TestEntityRepository.class);
                    store = context.getBean(TestEntityStore.class);

                    RandomString random = new RandomString(5);
                    resourceLocation = random.nextString();

                    embeddedRepo = context.getBean(EmbeddedRepository.class);
                    embeddedStore = context.getBean(EmbeddedStore.class);

                    entity = new FileSystemStoreIT.TEntity();
                    entity = repo.save(entity);

                    store.setContent(entity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                    store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("<html>Hello Spring Content World!</html>".getBytes()));
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
                        FileSystemStoreIT.this.e = sae;
                    }
                    assertThat(e).isInstanceOf(StoreAccessException.class);
                }
            }

            @Nested
            class WhenAnInvalidPropertyPathIsUsedToUnsetContent {
                @BeforeEach
                void setUp() {
                    context = new AnnotationConfigApplicationContext();
                    context.register(FileSystemStoreIT.TestConfig.class);
                    context.refresh();

                    repo = context.getBean(TestEntityRepository.class);
                    store = context.getBean(TestEntityStore.class);

                    RandomString random = new RandomString(5);
                    resourceLocation = random.nextString();

                    embeddedRepo = context.getBean(EmbeddedRepository.class);
                    embeddedStore = context.getBean(EmbeddedStore.class);

                    entity = new FileSystemStoreIT.TEntity();
                    entity = repo.save(entity);

                    store.setContent(entity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                    store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("<html>Hello Spring Content World!</html>".getBytes()));
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
                        FileSystemStoreIT.this.e = sae;
                    }
                    assertThat(e).isInstanceOf(StoreAccessException.class);
                }
            }

            @Nested
            class WhenContentIsDeletedAndTheIdFieldIsSharedWithJakartaId {
                @BeforeEach
                void setUp() {
                    context = new AnnotationConfigApplicationContext();
                    context.register(FileSystemStoreIT.TestConfig.class);
                    context.refresh();

                    repo = context.getBean(TestEntityRepository.class);
                    store = context.getBean(TestEntityStore.class);

                    RandomString random = new RandomString(5);
                    resourceLocation = random.nextString();

                    embeddedRepo = context.getBean(EmbeddedRepository.class);
                    embeddedStore = context.getBean(EmbeddedStore.class);

                    entity = new FileSystemStoreIT.TEntity();
                    entity = repo.save(entity);

                    store.setContent(entity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                    store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("<html>Hello Spring Content World!</html>".getBytes()));
                }

                @AfterEach
                void tearDown() {
                    context.close();
                }

                @Test
                void shouldNotResetTheIdField() {
                    SharedIdRepository sharedIdRepository = context.getBean(SharedIdRepository.class);
                    SharedIdStore sharedIdStore = context.getBean(SharedIdStore.class);

                    SharedIdContentIdEntity sharedIdContentIdEntity = sharedIdRepository.save(new SharedIdContentIdEntity());

                    sharedIdContentIdEntity = sharedIdStore.setContent(sharedIdContentIdEntity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
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
                class GivenAEntityWithANullEmbeddedContentObject {
                    @BeforeEach
                    void setUp() {
                        context = new AnnotationConfigApplicationContext();
                        context.register(FileSystemStoreIT.TestConfig.class);
                        context.refresh();

                        repo = context.getBean(TestEntityRepository.class);
                        store = context.getBean(TestEntityStore.class);

                        RandomString random = new RandomString(5);
                        resourceLocation = random.nextString();

                        embeddedRepo = context.getBean(EmbeddedRepository.class);
                        embeddedStore = context.getBean(EmbeddedStore.class);

                        entity = new FileSystemStoreIT.TEntity();
                        entity = repo.save(entity);

                        store.setContent(entity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                        store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("<html>Hello Spring Content World!</html>".getBytes()));
                    }

                    @AfterEach
                    void tearDown() {
                        context.close();
                    }

                    @Test
                    void shouldReturnNullWhenContentIsFetched() throws IOException {
                        EntityWithEmbeddedContent entity = embeddedRepo.save(new EntityWithEmbeddedContent());
                        assertThat(embeddedStore.getContent(entity, PropertyPath.from("content"))).isNull();
                    }

                    @Test
                    void shouldBeSuccessfulWhenContentIsSet() throws IOException {
                        EntityWithEmbeddedContent entity = embeddedRepo.save(new EntityWithEmbeddedContent());
                        embeddedStore.setContent(entity, PropertyPath.from("content"), new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                        try (InputStream is = embeddedStore.getContent(entity, PropertyPath.from("content"))) {
                            assertThat(IOUtils.contentEquals(is, new ByteArrayInputStream("Hello Spring Content World!".getBytes()))).isTrue();
                        }
                    }

                    @Test
                    void shouldReturnNullWhenContentIsUnset() {
                        EntityWithEmbeddedContent entity = embeddedRepo.save(new EntityWithEmbeddedContent());
                        assertThat(embeddedStore.unsetContent(entity, PropertyPath.from("content"))).isSameAs(entity);
                    }
                }
            }
        }
    }
}
