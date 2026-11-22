package it.store;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.content.commons.io.DeletableResource;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.StoreAccessException;
import org.springframework.content.fs.config.EnableFileSystemStores;
import org.springframework.content.fs.io.FileSystemResourceLoader;
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
            factory.setPackagesToScan(TestSupportPackages.JPA);
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
                class GivenContentIsAddedToThatResource {
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
                    class GivenThatResourceIsThenUpdated {
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
                    class GivenThatResourceIsThenDeleted {
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
            class GivenANewEntity {
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
                class GivenAResource {
                    @Nested
                    class WhenTheResourceIsAssociated {
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
                        class WhenTheResourceIsUnassociated {
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
                        class WhenAInvalidPropertyPathIsUsedToAssociateAResource {
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
                        class WhenAInvalidPropertyPathIsUsedToLoadAResource {
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
                        class WhenAInvalidPropertyPathIsUsedToUnassociateAResource {
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
            class WhenContentIsUpdated {
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
            class WhenContentIsUpdatedWithShorterContent {
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
            class WhenContentIsDeleted {
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
            class WhenAnInvalidPropertyPathIsUsedToSetContent {
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
            class WhenAnInvalidPropertyPathIsUsedToGetContent {
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
            class WhenAnInvalidPropertyPathIsUsedToUnsetContent {
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
