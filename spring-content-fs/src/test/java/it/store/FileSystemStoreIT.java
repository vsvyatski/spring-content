package it.store;

import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import net.bytebuddy.utility.RandomString;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext();
        context.register(TestConfig.class);
        context.refresh();

        repo = context.getBean(TestEntityRepository.class);
        store = context.getBean(TestEntityStore.class);
        embeddedRepo = context.getBean(EmbeddedRepository.class);
        embeddedStore = context.getBean(EmbeddedStore.class);
        resourceLocation = new RandomString(5).nextString();
    }

    @AfterEach
    void tearDown() {
        if (context != null) {
            context.close();
        }
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

    @Configuration
    @EnableJpaRepositories(considerNestedRepositories = true)
    @EnableFileSystemStores
    @Import(InfrastructureConfig.class)
    public static class TestConfig {
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

    @Nested
    class StoreCases {

        @BeforeEach
        void setUp() {
            genericResource = store.getResource(resourceLocation);
        }

        @AfterEach
        void deleteResource() throws IOException {
            if (genericResource != null) {
                ((DeletableResource) genericResource).delete();
            }
        }

        @Test
        void shouldGetResource() {
            assertThat(genericResource).isInstanceOf(Resource.class);
        }

        @Test
        void shouldNotExist() {
            assertThat(genericResource.exists()).isFalse();
        }

        @Test
        void shouldStoreThatContent() throws IOException {
            write(genericResource, "Hello Spring Content World!");

            assertThat(genericResource.exists()).isTrue();
            try (InputStream expected = new ByteArrayInputStream("Hello Spring Content World!".getBytes())) {
                try (InputStream actual = genericResource.getInputStream()) {
                    assertThat(IOUtils.contentEquals(expected, actual)).isTrue();
                }
            }
        }

        @Test
        void shouldStoreThatUpdatedContent() throws IOException {
            write(genericResource, "Hello Spring Content World!");
            write(genericResource, "Hello Updated Spring Content World!");

            assertThat(genericResource.exists()).isTrue();
            try (InputStream expected = new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes())) {
                try (InputStream actual = genericResource.getInputStream()) {
                    assertThat(IOUtils.contentEquals(expected, actual)).isTrue();
                }
            }
        }

        @Test
        void shouldNotExistOnceDeleted() {
            try {
                write(genericResource, "Hello Spring Content World!");
                ((DeletableResource) genericResource).delete();
            } catch (Exception ex) {
                e = ex;
            }
            assertThat(e).isNull();
        }

        private void write(Resource resource, String content) throws IOException {
            try (InputStream is = new ByteArrayInputStream(content.getBytes())) {
                try (OutputStream os = ((WritableResource) resource).getOutputStream()) {
                    IOUtils.copy(is, os);
                }
            }
        }
    }

    @Nested
    class AssociativeStoreCases {

        @BeforeEach
        void setUp() {
            entity = repo.save(new TEntity());
        }

        @Test
        void shouldNotHaveAnAssociatedResource() {
            assertThat(entity.getContentId()).isNull();
            assertThat(store.getResource(entity)).isNull();
        }

        @Test
        void shouldBeRecordedAsSuchOnTheEntitySContentId() {
            associateBoth();
            assertThat(entity.getContentId()).isEqualTo(resourceLocation);
            assertThat(entity.getRenditionId()).isEqualTo(resourceLocation);
        }

        @Test
        void shouldNotHonorByteRanges() throws IOException {
            associateBoth();
            try (OutputStream os = ((WritableResource) genericResource).getOutputStream()) {
                os.write("Hello Client-side World!".getBytes());
            }

            Resource r = store.getResource(entity, PropertyPath.from("content"), new GetResourceParams("5-10"));
            try (InputStream is = r.getInputStream()) {
                assertThat(IOUtils.toString(is, Charset.defaultCharset())).isEqualTo("Hello Client-side World!");
            }
        }

        @Test
        void shouldResetTheEntitySContentId() {
            associateBoth();
            store.unassociate(entity);
            store.unassociate(entity, PropertyPath.from("rendition"));
            assertThat(entity.getContentId()).isNull();
            assertThat(entity.getRenditionId()).isNull();
        }

        @Test
        void shouldThrowWhenAssociatingAnInvalidPropertyPath() {
            associateBoth();
            try {
                store.associate(entity, PropertyPath.from("does.not.exist"), resourceLocation);
            } catch (Exception sae) {
                e = sae;
            }
            assertThat(e).isInstanceOf(StoreAccessException.class);
        }

        @Test
        void shouldThrowWhenLoadingAnInvalidPropertyPath() {
            associateBoth();
            try {
                store.getResource(entity, PropertyPath.from("does.not.exist"));
            } catch (Exception sae) {
                e = sae;
            }
            assertThat(e).isInstanceOf(StoreAccessException.class);
        }

        @Test
        void shouldThrowWhenUnassociatingAnInvalidPropertyPath() {
            associateBoth();
            try {
                store.unassociate(entity, PropertyPath.from("does.not.exist"));
            } catch (Exception sae) {
                e = sae;
            }
            assertThat(e).isInstanceOf(StoreAccessException.class);
        }

        private void associateBoth() {
            genericResource = store.getResource(resourceLocation);
            store.associate(entity, resourceLocation);
            store.associate(entity, PropertyPath.from("rendition"), resourceLocation);
        }
    }

    @Nested
    class ContentStoreCases {

        @BeforeEach
        void setUp() {
            entity = repo.save(new TEntity());
            store.setContent(entity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
            store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("<html>Hello Spring Content World!</html>".getBytes()));
        }

        @Test
        void shouldBeAbleToStoreNewContent() {
            try (InputStream content = store.getContent(entity)) {
                assertThat(IOUtils.contentEquals(new ByteArrayInputStream("Hello Spring Content World!".getBytes()), content)).isTrue();
            } catch (IOException ignored) {
            }

            try (InputStream content = store.getContent(entity, PropertyPath.from("rendition"))) {
                assertThat(IOUtils.contentEquals(new ByteArrayInputStream("<html>Hello Spring Content World!</html>".getBytes()), content)).isTrue();
            } catch (IOException ignored) {
            }
        }

        @Test
        void shouldHaveContentMetadata() {
            assertThat(entity.getContentId()).isNotNull();
            assertThat(entity.getContentId().trim().length()).isGreaterThan(0);
            assertThat(entity.getContentLen()).isEqualTo(Long.valueOf(27L));

            assertThat(entity.getRenditionId()).isNotNull();
            assertThat(entity.getRenditionId().trim().length()).isGreaterThan(0);
            assertThat(40L).isEqualTo(entity.getRenditionLen());
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

            try (InputStream content = store.getContent(entity)) {
                boolean matches = IOUtils.contentEquals(new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()), content);
                assertThat(matches).isTrue();
            }

            try (InputStream content = store.getContent(entity, PropertyPath.from("rendition"))) {
                boolean matches = IOUtils.contentEquals(new ByteArrayInputStream("<html>Hello Updated Spring Content World!</html>".getBytes()), content);
                assertThat(matches).isTrue();
            }

            assertThat(entity.getContentId()).isEqualTo(contentId);
            assertThat(entity.getRenditionId()).isEqualTo(renditionId);

            assertThat(new File(loader.getRootResource().getPath(), entity.getContentId()).exists()).isTrue();
            assertThat(new File(loader.getRootResource().getPath(), entity.getRenditionId()).exists()).isTrue();
        }

        @Test
        void shouldStoreOnlyTheNewContent() throws IOException {
            store.setContent(entity, new ByteArrayInputStream("Hello Spring World!".getBytes()));
            store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("<html>Hello Spring World!</html>".getBytes()));
            entity = repo.save(entity);

            try (InputStream content = store.getContent(entity)) {
                boolean matches = IOUtils.contentEquals(new ByteArrayInputStream("Hello Spring World!".getBytes()), content);
                assertThat(matches).isTrue();
            }

            try (InputStream content = store.getContent(entity, PropertyPath.from("rendition"))) {
                boolean matches = IOUtils.contentEquals(new ByteArrayInputStream("<html>Hello Spring World!</html>".getBytes()), content);
                assertThat(matches).isTrue();
            }
        }

        @Test
        void shouldHaveTheUpdatedContentWhenNotOverwritten() throws IOException {
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

        @Test
        void shouldHaveNoContent() throws IOException {
            resourceLocation = entity.getContentId();
            entity = store.unsetContent(entity);
            entity = store.unsetContent(entity, PropertyPath.from("rendition"));
            entity = repo.save(entity);

            try (InputStream content = store.getContent(entity)) {
                assertThat(content).isNull();
            }

            assertThat(entity.getContentId()).isNull();
            assertThat(entity.getContentLen()).isNull();

            try (InputStream content = store.getContent(entity, PropertyPath.from("rendition"))) {
                assertThat(content).isNull();
            }

            assertThat(entity.getRenditionId()).isNull();
            assertThat(entity.getRenditionLen()).isEqualTo(0L);

            FileSystemResourceLoader loader = context.getBean(FileSystemResourceLoader.class);
            assertThat(new File(loader.getRootResource().getPath(), resourceLocation).exists()).isFalse();
        }

        @Test
        void shouldHaveNoContentWhenResourceIsKept() throws IOException {
            resourceLocation = entity.getContentId();
            entity = store.unsetContent(entity, PropertyPath.from("content"), new UnsetContentParams(UnsetContentParams.Disposition.Keep));
            entity = repo.save(entity);

            try (InputStream content = store.getContent(entity)) {
                assertThat(content).isNull();
            }

            assertThat(entity.getContentId()).isNull();
            assertThat(entity.getContentLen()).isNull();

            FileSystemResourceLoader loader = context.getBean(FileSystemResourceLoader.class);
            assertThat(new File(loader.getRootResource().getPath(), resourceLocation).exists()).isTrue();
        }

        @Test
        void shouldThrowWhenSettingContentOnAnInvalidPropertyPath() {
            try {
                store.setContent(entity, PropertyPath.from("does.not.exist"), new ByteArrayInputStream("foo".getBytes()));
            } catch (Exception sae) {
                e = sae;
            }
            assertThat(e).isInstanceOf(StoreAccessException.class);
        }

        @Test
        void shouldThrowWhenGettingContentOnAnInvalidPropertyPath() {
            try {
                store.getContent(entity, PropertyPath.from("does.not.exist"));
            } catch (Exception sae) {
                e = sae;
            }
            assertThat(e).isInstanceOf(StoreAccessException.class);
        }

        @Test
        void shouldThrowWhenUnsettingContentOnAnInvalidPropertyPath() {
            try {
                store.unsetContent(entity, PropertyPath.from("does.not.exist"));
            } catch (Exception sae) {
                e = sae;
            }
            assertThat(e).isInstanceOf(StoreAccessException.class);
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

        @Test
        void shouldReturnNullWhenContentIsFetched() throws IOException {
            EntityWithEmbeddedContent embedded = embeddedRepo.save(new EntityWithEmbeddedContent());
            assertThat(embeddedStore.getContent(embedded, PropertyPath.from("content"))).isNull();
        }

        @Test
        void shouldBeSuccessfulWhenContentIsSet() throws IOException {
            EntityWithEmbeddedContent embedded = embeddedRepo.save(new EntityWithEmbeddedContent());
            embeddedStore.setContent(embedded, PropertyPath.from("content"), new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
            try (InputStream is = embeddedStore.getContent(embedded, PropertyPath.from("content"))) {
                assertThat(IOUtils.contentEquals(is, new ByteArrayInputStream("Hello Spring Content World!".getBytes()))).isTrue();
            }
        }

        @Test
        void shouldReturnNullWhenContentIsUnset() {
            EntityWithEmbeddedContent embedded = embeddedRepo.save(new EntityWithEmbeddedContent());
            assertThat(embeddedStore.unsetContent(embedded, PropertyPath.from("content"))).isSameAs(embedded);
        }
    }
}
