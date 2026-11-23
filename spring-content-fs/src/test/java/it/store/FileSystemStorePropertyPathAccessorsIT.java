package it.store;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.content.commons.io.DeletableResource;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.StoreAccessException;
import org.springframework.content.fs.config.EnableFileSystemStores;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.content.test.support.EmbeddedJpa;
import org.springframework.content.test.support.TestSupportData;
import org.springframework.content.test.support.TestSupportPackages;
import org.springframework.content.test.support.jpa.PropertyPathJpaUuidEntity;
import org.springframework.content.test.support.jpa.PropertyPathJpaUuidRepository;
import org.springframework.content.test.support.jpa.PropertyPathJpaUuidStore;
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
import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;

public class FileSystemStorePropertyPathAccessorsIT {

    private PropertyPathJpaUuidEntity entity;
    private Resource genericResource;

    private Exception e;

    private AnnotationConfigApplicationContext context;

    private PropertyPathJpaUuidRepository repo;
    private PropertyPathJpaUuidStore store;

    private String resourceLocation;

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

                    repo = context.getBean(PropertyPathJpaUuidRepository.class);
                    store = context.getBean(PropertyPathJpaUuidStore.class);

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

                @Nested

                @DisplayName("Given content is added to that resource")

                class ContentIsAddedToThatResource {
                    @BeforeEach
                    void setUp() throws IOException {
                        context = new AnnotationConfigApplicationContext();
                        context.register(FileSystemStorePropertyPathAccessorsIT.TestConfig.class);
                        context.refresh();

                        repo = context.getBean(PropertyPathJpaUuidRepository.class);
                        store = context.getBean(PropertyPathJpaUuidStore.class);

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

                    @Nested

                    @DisplayName("Given that resource is then updated")

                    class ResourceIsThenUpdated {
                        @BeforeEach
                        void setUp() throws IOException {
                            context = new AnnotationConfigApplicationContext();
                            context.register(FileSystemStorePropertyPathAccessorsIT.TestConfig.class);
                            context.refresh();

                            repo = context.getBean(PropertyPathJpaUuidRepository.class);
                            store = context.getBean(PropertyPathJpaUuidStore.class);

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
                            context.register(FileSystemStorePropertyPathAccessorsIT.TestConfig.class);
                            context.refresh();

                            repo = context.getBean(PropertyPathJpaUuidRepository.class);
                            store = context.getBean(PropertyPathJpaUuidStore.class);

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
            @DisplayName("Given a new entity")
            class NewEntity {
                @BeforeEach
                void setUp() {
                    context = new AnnotationConfigApplicationContext();
                    context.register(FileSystemStorePropertyPathAccessorsIT.TestConfig.class);
                    context.refresh();

                    repo = context.getBean(PropertyPathJpaUuidRepository.class);
                    store = context.getBean(PropertyPathJpaUuidStore.class);

                    resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                    entity = new PropertyPathJpaUuidEntity();
                    entity = repo.save(entity);
                }

                @AfterEach
                void tearDown() {
                    context.close();
                }

                @Test
                void shouldNotHaveAnAssociatedResource() {
                    assertThat(entity.getContent().getContentId()).isNull();
                    assertThat(store.getResource(entity, PropertyPath.from("content"))).isNull();
                }

                @Nested

                @DisplayName("Given a resource")

                class AssociatedResource {
                    @Nested
                    @DisplayName("When the resource is associated")
                    class ResourceIsAssociated {
                        @BeforeEach
                        void setUp() {
                            context = new AnnotationConfigApplicationContext();
                            context.register(FileSystemStorePropertyPathAccessorsIT.TestConfig.class);
                            context.refresh();

                            repo = context.getBean(PropertyPathJpaUuidRepository.class);
                            store = context.getBean(PropertyPathJpaUuidStore.class);

                            resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                            entity = new PropertyPathJpaUuidEntity();
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
                            assertThat(entity.getContent().getContentId()).isEqualTo(resourceLocation);
                        }

                        @Nested

                        @DisplayName("When the resource is unassociated")

                        class ResourceIsUnassociated {
                            @BeforeEach
                            void setUp() {
                                context = new AnnotationConfigApplicationContext();
                                context.register(FileSystemStorePropertyPathAccessorsIT.TestConfig.class);
                                context.refresh();

                                repo = context.getBean(PropertyPathJpaUuidRepository.class);
                                store = context.getBean(PropertyPathJpaUuidStore.class);

                                resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                                entity = new PropertyPathJpaUuidEntity();
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
                                assertThat(entity.getContent().getContentId()).isNull();
                            }
                        }

                        @Nested

                        @DisplayName("When a invalid property path is used to associate a resource")

                        class InvalidPropertyPathIsUsedToAssociateResource {
                            @BeforeEach
                            void setUp() {
                                context = new AnnotationConfigApplicationContext();
                                context.register(FileSystemStorePropertyPathAccessorsIT.TestConfig.class);
                                context.refresh();

                                repo = context.getBean(PropertyPathJpaUuidRepository.class);
                                store = context.getBean(PropertyPathJpaUuidStore.class);

                                resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                                entity = new PropertyPathJpaUuidEntity();
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

                        @DisplayName("When a invalid property path is used to load a resource")

                        class InvalidPropertyPathIsUsedToLoadResource {
                            @BeforeEach
                            void setUp() {
                                context = new AnnotationConfigApplicationContext();
                                context.register(FileSystemStorePropertyPathAccessorsIT.TestConfig.class);
                                context.refresh();

                                repo = context.getBean(PropertyPathJpaUuidRepository.class);
                                store = context.getBean(PropertyPathJpaUuidStore.class);

                                resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                                entity = new PropertyPathJpaUuidEntity();
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

                        @DisplayName("When a invalid property path is used to unassociate a resource")

                        class InvalidPropertyPathIsUsedToUnassociateResource {
                            @BeforeEach
                            void setUp() {
                                context = new AnnotationConfigApplicationContext();
                                context.register(FileSystemStorePropertyPathAccessorsIT.TestConfig.class);
                                context.refresh();

                                repo = context.getBean(PropertyPathJpaUuidRepository.class);
                                store = context.getBean(PropertyPathJpaUuidStore.class);

                                resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                                entity = new PropertyPathJpaUuidEntity();
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

                repo = context.getBean(PropertyPathJpaUuidRepository.class);
                store = context.getBean(PropertyPathJpaUuidStore.class);

                resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                entity = new PropertyPathJpaUuidEntity();
                entity = repo.save(entity);

                store.setContent(entity, PropertyPath.from("content"), TestSupportData.sampleTextStream());
            }

            @AfterEach
            void tearDown() {
                context.close();
            }

            @Test
            void shouldBeAbleToStoreNewContent() {
                // content
                try (InputStream content = store.getContent(entity, PropertyPath.from("content"))) {
                    assertThat(IOUtils.contentEquals(TestSupportData.sampleTextStream(), content)).isTrue();
                } catch (IOException ignored) {
                }
            }

            @Test
            void shouldHaveContentMetadata() {
                // content
                assertThat(entity.getContent().getContentId()).isNotNull();
                assertThat(entity.getContent().getContentId().trim().length()).isGreaterThan(0);
                assertThat(entity.getContent().getContentLen()).isEqualTo(27L);
            }

            @Nested

            @DisplayName("When content is updated")

            class ContentIsUpdated {
                @BeforeEach
                void setUp() {
                    context = new AnnotationConfigApplicationContext();
                    context.register(FileSystemStorePropertyPathAccessorsIT.TestConfig.class);
                    context.refresh();

                    repo = context.getBean(PropertyPathJpaUuidRepository.class);
                    store = context.getBean(PropertyPathJpaUuidStore.class);

                    resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                    entity = new PropertyPathJpaUuidEntity();
                    entity = repo.save(entity);

                    store.setContent(entity, PropertyPath.from("content"),
                        TestSupportData.sampleTextStream());

                    entity = store.setContent(entity, PropertyPath.from("content"),
                        TestSupportData.sampleUpdatedTextStream());
                }

                @AfterEach
                void tearDown() {
                    context.close();
                }

                @Test
                void shouldHaveTheUpdatedContent() throws IOException {
                    //content
                    try (InputStream content = store.getContent(entity, PropertyPath.from("content"))) {
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
                    context.register(FileSystemStorePropertyPathAccessorsIT.TestConfig.class);
                    context.refresh();

                    repo = context.getBean(PropertyPathJpaUuidRepository.class);
                    store = context.getBean(PropertyPathJpaUuidStore.class);

                    resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                    entity = new PropertyPathJpaUuidEntity();
                    entity = repo.save(entity);

                    store.setContent(entity, PropertyPath.from("content"),
                        TestSupportData.sampleTextStream());

                    entity = store.setContent(entity, PropertyPath.from("content"),
                        TestSupportData.samplePartialUpdateStream());
                }

                @AfterEach
                void tearDown() {
                    context.close();
                }

                @Test
                void shouldStoreOnlyTheNewContent() throws IOException {
                    //content
                    try (InputStream content = store.getContent(entity, PropertyPath.from("content"))) {
                        boolean matches = IOUtils.contentEquals(TestSupportData.samplePartialUpdateStream(), content);
                        assertThat(matches).isTrue();
                    }
                }
            }

            @Nested

            @DisplayName("When content is deleted")

            class ContentIsDeleted {
                @BeforeEach
                void setUp() {
                    context = new AnnotationConfigApplicationContext();
                    context.register(FileSystemStorePropertyPathAccessorsIT.TestConfig.class);
                    context.refresh();

                    repo = context.getBean(PropertyPathJpaUuidRepository.class);
                    store = context.getBean(PropertyPathJpaUuidStore.class);

                    resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                    entity = new PropertyPathJpaUuidEntity();
                    entity = repo.save(entity);

                    store.setContent(entity, PropertyPath.from("content"), TestSupportData.sampleTextStream());

                    resourceLocation = entity.getContent().getContentId();
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

                    assertThat(entity.getContent().getContentId()).isNull();
                    assertThat(entity.getContent().getContentLen()).isNull();
                }
            }

            @Nested

            @DisplayName("When an invalid property path is used to set content")

            class InvalidPropertyPathIsUsedToSetContent {
                @BeforeEach
                void setUp() {
                    context = new AnnotationConfigApplicationContext();
                    context.register(FileSystemStorePropertyPathAccessorsIT.TestConfig.class);
                    context.refresh();

                    repo = context.getBean(PropertyPathJpaUuidRepository.class);
                    store = context.getBean(PropertyPathJpaUuidStore.class);

                    resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                    entity = new PropertyPathJpaUuidEntity();
                    entity = repo.save(entity);

                    store.setContent(entity, PropertyPath.from("content"), TestSupportData.sampleTextStream());
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

            @DisplayName("When an invalid property path is used to GET content")

            class InvalidPropertyPathIsUsedToGetContent {
                @BeforeEach
                void setUp() {
                    context = new AnnotationConfigApplicationContext();
                    context.register(FileSystemStorePropertyPathAccessorsIT.TestConfig.class);
                    context.refresh();

                    repo = context.getBean(PropertyPathJpaUuidRepository.class);
                    store = context.getBean(PropertyPathJpaUuidStore.class);

                    resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                    entity = new PropertyPathJpaUuidEntity();
                    entity = repo.save(entity);

                    store.setContent(entity, PropertyPath.from("content"), TestSupportData.sampleTextStream());
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

            @DisplayName("When an invalid property path is used to unset content")

            class InvalidPropertyPathIsUsedToUnsetContent {
                @BeforeEach
                void setUp() {
                    context = new AnnotationConfigApplicationContext();
                    context.register(FileSystemStorePropertyPathAccessorsIT.TestConfig.class);
                    context.refresh();

                    repo = context.getBean(PropertyPathJpaUuidRepository.class);
                    store = context.getBean(PropertyPathJpaUuidStore.class);

                    resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                    entity = new PropertyPathJpaUuidEntity();
                    entity = repo.save(entity);

                    store.setContent(entity, PropertyPath.from("content"), TestSupportData.sampleTextStream());
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
