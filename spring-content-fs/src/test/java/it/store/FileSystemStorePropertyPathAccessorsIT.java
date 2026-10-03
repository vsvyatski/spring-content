package it.store;

import internal.org.springframework.content.fs.store.DefaultFileSystemStoreImpl;
import jakarta.persistence.*;
import net.bytebuddy.utility.RandomString;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.jpa.EntityManagerFactoryBuilder;
import org.springframework.boot.jpa.autoconfigure.JpaProperties;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.annotations.OriginalFileName;
import org.springframework.content.commons.io.DeletableResource;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.commons.store.StoreAccessException;
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
import java.nio.file.Files;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class FileSystemStorePropertyPathAccessorsIT {

    private FileSystemStorePropertyPathAccessorsIT.TEntity entity;
    private Resource genericResource;

    private Exception e;

    private AnnotationConfigApplicationContext context;

    private TestEntityRepository repo;
    private TestEntityStore store;

    private String resourceLocation;

    @Test
    public void test() {
        // noop
    }

    public interface TestEntityRepository extends JpaRepository<TEntity, UUID> {
    }

    public interface TestEntityStore extends ContentStore<TEntity, String> {
    }

    @Configuration
    @EnableJpaRepositories(considerNestedRepositories = true)
    @EntityScan(basePackageClasses = TEntity.class)
    @EnableFileSystemStores
    @Import(InfrastructureConfig.class)
    public static class TestConfig {
        //
    }

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
        public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {

            HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
            vendorAdapter.setDatabase(Database.HSQL);
            vendorAdapter.setGenerateDdl(true);

            EntityManagerFactoryBuilder builder = createEntityManagerFactoryBuilder(new JpaProperties());
            return builder.dataSource(dataSource).packages(TEntity.class).persistenceUnit("firstDs").build();
        }

        private EntityManagerFactoryBuilder createEntityManagerFactoryBuilder(JpaProperties jpaProperties) {

            HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
            vendorAdapter.setDatabase(Database.HSQL);
            vendorAdapter.setGenerateDdl(true);

            return new EntityManagerFactoryBuilder(vendorAdapter, dataSource -> jpaProperties.getProperties(), null);
        }

        @Bean
        public PlatformTransactionManager transactionManager(DataSource dataSource) {

            JpaTransactionManager txManager = new JpaTransactionManager();
            txManager.setEntityManagerFactory(entityManagerFactory(dataSource).getObject());
            return txManager;
        }
    }

    @Entity
    @Table(name = "tentity_content")
    public static class TEntity {

        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        private UUID id;

        private String number;

        @Embedded
        @AttributeOverride(name = "id", column = @Column(name = "content__id"))
        private EmbeddedContent content = new EmbeddedContent();

        public TEntity() {
        }

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }

        public String getNumber() {
            return number;
        }

        public void setNumber(String number) {
            this.number = number;
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
        private String id;

        @ContentLength
        private long length;

        @MimeType
        private String mimetype;

        @OriginalFileName
        private String filename;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public long getLength() {
            return length;
        }

        public void setLength(long length) {
            this.length = length;
        }

        public String getMimetype() {
            return mimetype;
        }

        public void setMimetype(String mimetype) {
            this.mimetype = mimetype;
        }

        public String getFilename() {
            return filename;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }
    }

    @Nested
    class DefaultFileSystemStoreImplPropertyPathAccessors {
        @Nested
        class Store {
            @Nested
            class GetResource {
                @BeforeEach
                void setUp() {
                    context = new AnnotationConfigApplicationContext();
                    context.register(FileSystemStorePropertyPathAccessorsIT.TestConfig.class);
                    context.refresh();

                    repo = context.getBean(TestEntityRepository.class);
                    store = context.getBean(TestEntityStore.class);

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

                @Nested
                class GivenContentIsAddedToThatResource {
                    @BeforeEach
                    void setUp() throws IOException {
                        context = new AnnotationConfigApplicationContext();
                        context.register(FileSystemStorePropertyPathAccessorsIT.TestConfig.class);
                        context.refresh();

                        repo = context.getBean(TestEntityRepository.class);
                        store = context.getBean(TestEntityStore.class);

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

                    @Nested
                    class GivenThatResourceIsThenUpdated {
                        @BeforeEach
                        void setUp() throws IOException {
                            context = new AnnotationConfigApplicationContext();
                            context.register(FileSystemStorePropertyPathAccessorsIT.TestConfig.class);
                            context.refresh();

                            repo = context.getBean(TestEntityRepository.class);
                            store = context.getBean(TestEntityStore.class);

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
                            context.register(FileSystemStorePropertyPathAccessorsIT.TestConfig.class);
                            context.refresh();

                            repo = context.getBean(TestEntityRepository.class);
                            store = context.getBean(TestEntityStore.class);

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
                                FileSystemStorePropertyPathAccessorsIT.this.e = e;
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
        class AssociativeStore {
            @Nested
            class GivenANewEntity {
                @BeforeEach
                void setUp() {
                    context = new AnnotationConfigApplicationContext();
                    context.register(FileSystemStorePropertyPathAccessorsIT.TestConfig.class);
                    context.refresh();

                    repo = context.getBean(TestEntityRepository.class);
                    store = context.getBean(TestEntityStore.class);

                    RandomString random = new RandomString(5);
                    resourceLocation = random.nextString();

                    entity = new FileSystemStorePropertyPathAccessorsIT.TEntity();
                    entity = repo.save(entity);
                }

                @AfterEach
                void tearDown() {
                    context.close();
                }

                @Test
                void shouldNotHaveAnAssociatedResource() {
                    assertThat(entity.getContent().getId()).isNull();
                    assertThat(store.getResource(entity, PropertyPath.from("content"))).isNull();
                }

                @Nested
                class GivenAResource {
                    @Nested
                    class WhenTheResourceIsAssociated {
                        @BeforeEach
                        void setUp() {
                            context = new AnnotationConfigApplicationContext();
                            context.register(FileSystemStorePropertyPathAccessorsIT.TestConfig.class);
                            context.refresh();

                            repo = context.getBean(TestEntityRepository.class);
                            store = context.getBean(TestEntityStore.class);

                            RandomString random = new RandomString(5);
                            resourceLocation = random.nextString();

                            entity = new FileSystemStorePropertyPathAccessorsIT.TEntity();
                            entity = repo.save(entity);

                            genericResource = store.getResource(resourceLocation);

                            store.associate(entity, PropertyPath.from("content"), resourceLocation);
                        }

                        @AfterEach
                        void tearDown() {
                            context.close();
                        }

                        @Test
                        void shouldBeRecordedAsSuchOnTheEntitySContentId() {
                            assertThat(entity.getContent().getId()).isEqualTo(resourceLocation);
                        }

                        @Nested
                        class WhenTheResourceIsUnassociated {
                            @BeforeEach
                            void setUp() {
                                context = new AnnotationConfigApplicationContext();
                                context.register(FileSystemStorePropertyPathAccessorsIT.TestConfig.class);
                                context.refresh();

                                repo = context.getBean(TestEntityRepository.class);
                                store = context.getBean(TestEntityStore.class);

                                RandomString random = new RandomString(5);
                                resourceLocation = random.nextString();

                                entity = new FileSystemStorePropertyPathAccessorsIT.TEntity();
                                entity = repo.save(entity);

                                genericResource = store.getResource(resourceLocation);

                                store.associate(entity, PropertyPath.from("content"), resourceLocation);

                                store.unassociate(entity, PropertyPath.from("content"));
                            }

                            @AfterEach
                            void tearDown() {
                                context.close();
                            }

                            @Test
                            void shouldResetTheEntitySContentId() {
                                assertThat(entity.getContent().getId()).isNull();
                            }
                        }

                        @Nested
                        class WhenAInvalidPropertyPathIsUsedToAssociateAResource {
                            @BeforeEach
                            void setUp() {
                                context = new AnnotationConfigApplicationContext();
                                context.register(FileSystemStorePropertyPathAccessorsIT.TestConfig.class);
                                context.refresh();

                                repo = context.getBean(TestEntityRepository.class);
                                store = context.getBean(TestEntityStore.class);

                                RandomString random = new RandomString(5);
                                resourceLocation = random.nextString();

                                entity = new FileSystemStorePropertyPathAccessorsIT.TEntity();
                                entity = repo.save(entity);

                                genericResource = store.getResource(resourceLocation);

                                store.associate(entity, PropertyPath.from("content"), resourceLocation);
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
                                    FileSystemStorePropertyPathAccessorsIT.this.e = sae;
                                }
                                assertThat(e).isInstanceOf(StoreAccessException.class);
                            }
                        }

                        @Nested
                        class WhenAInvalidPropertyPathIsUsedToLoadAResource {
                            @BeforeEach
                            void setUp() {
                                context = new AnnotationConfigApplicationContext();
                                context.register(FileSystemStorePropertyPathAccessorsIT.TestConfig.class);
                                context.refresh();

                                repo = context.getBean(TestEntityRepository.class);
                                store = context.getBean(TestEntityStore.class);

                                RandomString random = new RandomString(5);
                                resourceLocation = random.nextString();

                                entity = new FileSystemStorePropertyPathAccessorsIT.TEntity();
                                entity = repo.save(entity);

                                genericResource = store.getResource(resourceLocation);

                                store.associate(entity, PropertyPath.from("content"), resourceLocation);
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
                                    FileSystemStorePropertyPathAccessorsIT.this.e = sae;
                                }
                                assertThat(e).isInstanceOf(StoreAccessException.class);
                            }
                        }

                        @Nested
                        class WhenAInvalidPropertyPathIsUsedToUnassociateAResource {
                            @BeforeEach
                            void setUp() {
                                context = new AnnotationConfigApplicationContext();
                                context.register(FileSystemStorePropertyPathAccessorsIT.TestConfig.class);
                                context.refresh();

                                repo = context.getBean(TestEntityRepository.class);
                                store = context.getBean(TestEntityStore.class);

                                RandomString random = new RandomString(5);
                                resourceLocation = random.nextString();

                                entity = new FileSystemStorePropertyPathAccessorsIT.TEntity();
                                entity = repo.save(entity);

                                genericResource = store.getResource(resourceLocation);

                                store.associate(entity, PropertyPath.from("content"), resourceLocation);
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
                                    FileSystemStorePropertyPathAccessorsIT.this.e = sae;
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
            @BeforeEach
            void setUp() {
                context = new AnnotationConfigApplicationContext();
                context.register(FileSystemStorePropertyPathAccessorsIT.TestConfig.class);
                context.refresh();

                repo = context.getBean(TestEntityRepository.class);
                store = context.getBean(TestEntityStore.class);

                RandomString random = new RandomString(5);
                resourceLocation = random.nextString();

                entity = new FileSystemStorePropertyPathAccessorsIT.TEntity();
                entity = repo.save(entity);

                store.setContent(entity, PropertyPath.from("content"), new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
            }

            @AfterEach
            void tearDown() {
                context.close();
            }

            @Test
            void shouldBeAbleToStoreNewContent() {
                // content
                try (InputStream content = store.getContent(entity, PropertyPath.from("content"))) {
                    assertThat(IOUtils.contentEquals(new ByteArrayInputStream("Hello Spring Content World!".getBytes()), content)).isTrue();
                } catch (IOException ignored) {
                }
            }

            @Test
            void shouldHaveContentMetadata() {
                // content
                assertThat(entity.getContent().getId()).isNotNull();
                assertThat(entity.getContent().getId().trim().length()).isGreaterThan(0);
                assertThat(entity.getContent().getLength()).isEqualTo(27L);
            }

            @Nested
            class WhenContentIsUpdated {
                @BeforeEach
                void setUp() {
                    context = new AnnotationConfigApplicationContext();
                    context.register(FileSystemStorePropertyPathAccessorsIT.TestConfig.class);
                    context.refresh();

                    repo = context.getBean(TestEntityRepository.class);
                    store = context.getBean(TestEntityStore.class);

                    RandomString random = new RandomString(5);
                    resourceLocation = random.nextString();

                    entity = new FileSystemStorePropertyPathAccessorsIT.TEntity();
                    entity = repo.save(entity);

                    store.setContent(entity, PropertyPath.from("content"), new ByteArrayInputStream("Hello Spring Content World!".getBytes()));

                    store.setContent(entity, PropertyPath.from("content"), new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()));
                    entity = repo.save(entity);

                }

                @AfterEach
                void tearDown() {
                    context.close();
                }

                @Test
                void shouldHaveTheUpdatedContent() throws IOException {
                    //content
                    try (InputStream content = store.getContent(entity, PropertyPath.from("content"))) {
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
                    context.register(FileSystemStorePropertyPathAccessorsIT.TestConfig.class);
                    context.refresh();

                    repo = context.getBean(TestEntityRepository.class);
                    store = context.getBean(TestEntityStore.class);

                    RandomString random = new RandomString(5);
                    resourceLocation = random.nextString();

                    entity = new FileSystemStorePropertyPathAccessorsIT.TEntity();
                    entity = repo.save(entity);

                    store.setContent(entity, PropertyPath.from("content"), new ByteArrayInputStream("Hello Spring Content World!".getBytes()));

                    store.setContent(entity, PropertyPath.from("content"), new ByteArrayInputStream("Hello Spring World!".getBytes()));
                    entity = repo.save(entity);
                }

                @AfterEach
                void tearDown() {
                    context.close();
                }

                @Test
                void shouldStoreOnlyTheNewContent() throws IOException {
                    //content
                    try (InputStream content = store.getContent(entity, PropertyPath.from("content"))) {
                        boolean matches = IOUtils.contentEquals(new ByteArrayInputStream("Hello Spring World!".getBytes()), content);
                        assertThat(matches).isTrue();
                    }
                }
            }

            @Nested
            class WhenContentIsDeleted {
                @BeforeEach
                void setUp() {
                    context = new AnnotationConfigApplicationContext();
                    context.register(FileSystemStorePropertyPathAccessorsIT.TestConfig.class);
                    context.refresh();

                    repo = context.getBean(TestEntityRepository.class);
                    store = context.getBean(TestEntityStore.class);

                    RandomString random = new RandomString(5);
                    resourceLocation = random.nextString();

                    entity = new FileSystemStorePropertyPathAccessorsIT.TEntity();
                    entity = repo.save(entity);

                    store.setContent(entity, PropertyPath.from("content"), new ByteArrayInputStream("Hello Spring Content World!".getBytes()));

                    resourceLocation = entity.getContent().getId();
                    entity = store.unsetContent(entity, PropertyPath.from("content"));
                    entity = repo.save(entity);
                }

                @AfterEach
                void tearDown() {
                    context.close();
                }

                @Test
                void shouldHaveNoContent() throws IOException {
                    //content
                    try (InputStream content = store.getContent(entity, PropertyPath.from("content"))) {
                        assertThat(content).isNull();
                    }

                    assertThat(entity.getContent().getId()).isNull();
                    assertThat(entity.getContent().getLength()).isEqualTo(0);
                }
            }

            @Nested
            class WhenAnInvalidPropertyPathIsUsedToSetContent {
                @BeforeEach
                void setUp() {
                    context = new AnnotationConfigApplicationContext();
                    context.register(FileSystemStorePropertyPathAccessorsIT.TestConfig.class);
                    context.refresh();

                    repo = context.getBean(TestEntityRepository.class);
                    store = context.getBean(TestEntityStore.class);

                    RandomString random = new RandomString(5);
                    resourceLocation = random.nextString();

                    entity = new FileSystemStorePropertyPathAccessorsIT.TEntity();
                    entity = repo.save(entity);

                    store.setContent(entity, PropertyPath.from("content"), new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
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
                        FileSystemStorePropertyPathAccessorsIT.this.e = sae;
                    }
                    assertThat(e).isInstanceOf(StoreAccessException.class);

                }
            }

            @Nested
            class WhenAnInvalidPropertyPathIsUsedToGetContent {
                @BeforeEach
                void setUp() {
                    context = new AnnotationConfigApplicationContext();
                    context.register(FileSystemStorePropertyPathAccessorsIT.TestConfig.class);
                    context.refresh();

                    repo = context.getBean(TestEntityRepository.class);
                    store = context.getBean(TestEntityStore.class);

                    RandomString random = new RandomString(5);
                    resourceLocation = random.nextString();

                    entity = new FileSystemStorePropertyPathAccessorsIT.TEntity();
                    entity = repo.save(entity);

                    store.setContent(entity, PropertyPath.from("content"), new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
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
                        FileSystemStorePropertyPathAccessorsIT.this.e = sae;
                    }
                    assertThat(e).isInstanceOf(StoreAccessException.class);
                }
            }

            @Nested
            class WhenAnInvalidPropertyPathIsUsedToUnsetContent {
                @BeforeEach
                void setUp() {
                    context = new AnnotationConfigApplicationContext();
                    context.register(FileSystemStorePropertyPathAccessorsIT.TestConfig.class);
                    context.refresh();

                    repo = context.getBean(TestEntityRepository.class);
                    store = context.getBean(TestEntityStore.class);

                    RandomString random = new RandomString(5);
                    resourceLocation = random.nextString();

                    entity = new FileSystemStorePropertyPathAccessorsIT.TEntity();
                    entity = repo.save(entity);

                    store.setContent(entity, PropertyPath.from("content"), new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
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
                        FileSystemStorePropertyPathAccessorsIT.this.e = sae;
                    }
                    assertThat(e).isInstanceOf(StoreAccessException.class);
                }
            }
        }
    }
}
