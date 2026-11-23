package it.store;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.content.commons.io.DeletableResource;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.GetResourceParams;
import org.springframework.content.commons.store.SetContentParams;
import org.springframework.content.commons.store.StoreAccessException;
import org.springframework.content.commons.store.UnsetContentParams;
import org.springframework.content.fs.config.EnableFileSystemStores;
import org.springframework.content.fs.io.FileSystemResourceLoader;
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
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

public class FileSystemStoreIT {

    private DualContentJpaStringIdEntity entity;
    private Resource genericResource;
    private Exception e;

    private AnnotationConfigApplicationContext context;

    private DualContentJpaStringIdRepository repo;
    private DualContentJpaStringIdStore store;

    private EmbeddedContentJpaStringIdRepository embeddedRepo;
    private EmbeddedContentJpaStringIdStore embeddedStore;

    private String resourceLocation;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext();
        context.register(TestConfig.class);
        context.refresh();

        repo = context.getBean(DualContentJpaStringIdRepository.class);
        store = context.getBean(DualContentJpaStringIdStore.class);
        embeddedRepo = context.getBean(EmbeddedContentJpaStringIdRepository.class);
        embeddedStore = context.getBean(EmbeddedContentJpaStringIdStore.class);
        resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;
    }

    @AfterEach
    void tearDown() {
        if (context != null) {
            context.close();
        }
    }

    @Configuration
    @EnableJpaRepositories(basePackages = TestSupportPackages.JPA, considerNestedRepositories = true)
    @EnableFileSystemStores(basePackages = TestSupportPackages.JPA)
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
            return EmbeddedJpa.embeddedDataSource(EmbeddedDatabaseType.HSQL);
        }

        @Bean
        public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
            return EmbeddedJpa.entityManagerFactory(dataSource(), Database.HSQL, TestSupportPackages.JPA);
        }

        @Bean
        public PlatformTransactionManager transactionManager() {
            return EmbeddedJpa.transactionManager(entityManagerFactory().getObject());
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
            write(genericResource, TestSupportData.SAMPLE_TEXT_PAYLOAD);

            assertThat(genericResource.exists()).isTrue();
            try (InputStream expected = TestSupportData.sampleTextStream()) {
                try (InputStream actual = genericResource.getInputStream()) {
                    assertThat(IOUtils.contentEquals(expected, actual)).isTrue();
                }
            }
        }

        @Test
        void shouldStoreThatUpdatedContent() throws IOException {
            write(genericResource, TestSupportData.SAMPLE_TEXT_PAYLOAD);
            write(genericResource, TestSupportData.SAMPLE_UPDATED_TEXT_PAYLOAD);

            assertThat(genericResource.exists()).isTrue();
            try (InputStream expected = TestSupportData.sampleUpdatedTextStream()) {
                try (InputStream actual = genericResource.getInputStream()) {
                    assertThat(IOUtils.contentEquals(expected, actual)).isTrue();
                }
            }
        }

        @Test
        void shouldNotExistOnceDeleted() {
            try {
                write(genericResource, TestSupportData.SAMPLE_TEXT_PAYLOAD);
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
            entity = repo.save(new DualContentJpaStringIdEntity());
        }

        @Test
        void shouldNotHaveAssociatedResource() {
            assertThat(entity.getContentId()).isNull();
            assertThat(store.getResource(entity)).isNull();
        }

        @Test
        void shouldBeRecordedAsSuchOnEntitySContentId() {
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
        void shouldResetEntitySContentId() {
            associateBoth();
            store.unassociate(entity);
            store.unassociate(entity, PropertyPath.from("rendition"));
            assertThat(entity.getContentId()).isNull();
            assertThat(entity.getRenditionId()).isNull();
        }

        @Test
        void shouldThrowWhenAssociatingInvalidPropertyPath() {
            associateBoth();
            try {
                store.associate(entity, PropertyPath.from("does.not.exist"), resourceLocation);
            } catch (Exception sae) {
                e = sae;
            }
            assertThat(e).isInstanceOf(StoreAccessException.class);
        }

        @Test
        void shouldThrowWhenLoadingInvalidPropertyPath() {
            associateBoth();
            try {
                store.getResource(entity, PropertyPath.from("does.not.exist"));
            } catch (Exception sae) {
                e = sae;
            }
            assertThat(e).isInstanceOf(StoreAccessException.class);
        }

        @Test
        void shouldThrowWhenUnassociatingInvalidPropertyPath() {
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
            entity = repo.save(new DualContentJpaStringIdEntity());
            store.setContent(entity, TestSupportData.sampleTextStream());
            store.setContent(entity, PropertyPath.from("rendition"), TestSupportData.sampleHtmlRenditionStream());
        }

        @Test
        void shouldBeAbleToStoreNewContent() {
            try (InputStream content = store.getContent(entity)) {
                assertThat(IOUtils.contentEquals(TestSupportData.sampleTextStream(), content)).isTrue();
            } catch (IOException ignored) {
            }

            try (InputStream content = store.getContent(entity, PropertyPath.from("rendition"))) {
                assertThat(IOUtils.contentEquals(TestSupportData.sampleHtmlRenditionStream(), content)).isTrue();
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
        void shouldHaveUpdatedContent() throws IOException {
            FileSystemResourceLoader loader = context.getBean(FileSystemResourceLoader.class);
            String contentId = Objects.requireNonNull(entity.getContentId());
            assertThat(new File(loader.getRootResource().getPath(), contentId).exists()).isTrue();
            String renditionId = Objects.requireNonNull(entity.getRenditionId());
            assertThat(new File(loader.getRootResource().getPath(), renditionId).exists()).isTrue();
            entity = store.setContent(entity,
                TestSupportData.sampleUpdatedTextStream());
            entity = store.setContent(entity, PropertyPath.from("rendition"),
                TestSupportData.sampleUpdatedHtmlRenditionStream());

            try (InputStream content = store.getContent(entity)) {
                boolean matches = IOUtils.contentEquals(TestSupportData.sampleUpdatedTextStream(), content);
                assertThat(matches).isTrue();
            }

            try (InputStream content = store.getContent(entity, PropertyPath.from("rendition"))) {
                boolean matches = IOUtils.contentEquals(TestSupportData.sampleUpdatedHtmlRenditionStream(), content);
                assertThat(matches).isTrue();
            }

            assertThat(entity.getContentId()).isEqualTo(contentId);
            assertThat(entity.getRenditionId()).isEqualTo(renditionId);

            assertThat(new File(loader.getRootResource().getPath(), Objects.requireNonNull(entity.getContentId())).exists()).isTrue();
            assertThat(new File(loader.getRootResource().getPath(), Objects.requireNonNull(entity.getRenditionId())).exists()).isTrue();
        }

        @Test
        void shouldStoreOnlyNewContent() throws IOException {
            entity = store.setContent(entity,
                TestSupportData.samplePartialUpdateStream());
            entity = store.setContent(entity, PropertyPath.from("rendition"),
                TestSupportData.samplePartialUpdateHtmlRenditionStream());

            try (InputStream content = store.getContent(entity)) {
                boolean matches = IOUtils.contentEquals(TestSupportData.samplePartialUpdateStream(), content);
                assertThat(matches).isTrue();
            }

            try (InputStream content = store.getContent(entity, PropertyPath.from("rendition"))) {
                boolean matches = IOUtils.contentEquals(TestSupportData.samplePartialUpdateHtmlRenditionStream(), content);
                assertThat(matches).isTrue();
            }
        }

        @Test
        void shouldHaveUpdatedContentWhenNotOverwritten() throws IOException {
            FileSystemResourceLoader loader = context.getBean(FileSystemResourceLoader.class);
            String contentId = Objects.requireNonNull(entity.getContentId());
            assertThat(new File(loader.getRootResource().getPath(), contentId).exists()).isTrue();

            entity = store.setContent(entity, PropertyPath.from("content"),
                TestSupportData.sampleUpdatedTextStream(),
                new SetContentParams(-1, true, SetContentParams.ContentDisposition.CreateNew));

            try (InputStream content = store.getContent(entity)) {
                boolean matches = IOUtils.contentEquals(TestSupportData.sampleUpdatedTextStream(), content);
                assertThat(matches).isTrue();
            }

            assertThat(new File(loader.getRootResource().getPath(), contentId).exists()).isTrue();
            assertThat(entity.getContentId()).isNotEqualTo(contentId);
            assertThat(new File(loader.getRootResource().getPath(), Objects.requireNonNull(entity.getContentId())).exists()).isTrue();
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
            assertThat(entity.getRenditionLen()).isNull();

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
        void shouldThrowWhenSettingContentOnInvalidPropertyPath() {
            try {
                store.setContent(entity, PropertyPath.from("does.not.exist"), new ByteArrayInputStream("foo".getBytes()));
            } catch (Exception sae) {
                e = sae;
            }
            assertThat(e).isInstanceOf(StoreAccessException.class);
        }

        @Test
        void shouldThrowWhenGettingContentOnInvalidPropertyPath() {
            try {
                store.getContent(entity, PropertyPath.from("does.not.exist"));
            } catch (Exception sae) {
                e = sae;
            }
            assertThat(e).isInstanceOf(StoreAccessException.class);
        }

        @Test
        void shouldThrowWhenUnsettingContentOnInvalidPropertyPath() {
            try {
                store.unsetContent(entity, PropertyPath.from("does.not.exist"));
            } catch (Exception sae) {
                e = sae;
            }
            assertThat(e).isInstanceOf(StoreAccessException.class);
        }

        @Test
        void shouldNotResetIdField() {
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

        @Test
        void shouldReturnNullWhenContentIsFetched() throws IOException {
            EmbeddedContentJpaStringIdEntity embedded = embeddedRepo.save(new EmbeddedContentJpaStringIdEntity());
            assertThat(embeddedStore.getContent(embedded, PropertyPath.from("content"))).isNull();
        }

        @Test
        void shouldBeSuccessfulWhenContentIsSet() throws IOException {
            EmbeddedContentJpaStringIdEntity embedded = embeddedRepo.save(new EmbeddedContentJpaStringIdEntity());
            embeddedStore.setContent(embedded, PropertyPath.from("content"), TestSupportData.sampleTextStream());
            try (InputStream is = embeddedStore.getContent(embedded, PropertyPath.from("content"))) {
                assertThat(IOUtils.contentEquals(is, TestSupportData.sampleTextStream())).isTrue();
            }
        }

        @Test
        void shouldReturnNullWhenContentIsUnset() {
            EmbeddedContentJpaStringIdEntity embedded = embeddedRepo.save(new EmbeddedContentJpaStringIdEntity());
            assertThat(embeddedStore.unsetContent(embedded, PropertyPath.from("content"))).isSameAs(embedded);
        }
    }
}
