package internal.org.springframework.content.azure.it;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.content.azure.Bucket;
import org.springframework.content.azure.config.AzureStorageConfigurer;
import org.springframework.content.azure.config.BlobId;
import org.springframework.content.azure.config.EnableAzureStorage;
import org.springframework.content.commons.config.ContentPropertyInfo;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.test.support.EmbeddedJpa;
import org.springframework.content.test.support.FakeEntity;
import org.springframework.content.test.support.TestSupportData;
import org.springframework.content.test.support.TestSupportPackages;
import org.springframework.content.test.support.jpa.DualContentJpaLongIdMimeEntity;
import org.springframework.content.test.support.jpa.DualContentJpaLongIdMimeRepository;
import org.springframework.content.test.support.jpa.DualContentJpaLongIdMimeStore;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.io.Serializable;
import java.util.Objects;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class AzureStorageWithEntityConverterIT {

    private static final BlobServiceClientBuilder builder = Azurite.getBlobServiceClientBuilder();
    private static final BlobContainerClient containerClient = builder.buildClient().getBlobContainerClient("test");

    private static final String BUCKET = "spring-eg-content-azure";
    private static final String OTHER_BUCKET = "other-bucket-4f91c2";
    private static final String OTHER_OTHER_BUCKET = "other-other-bucket-8c3e17";

    private static final TestData[] TEST_DATA_SETS = {
        new TestData("Default Converter", new Class<?>[]{TestConfig.class}, OTHER_BUCKET),
        new TestData("Custom Converter", new Class<?>[]{CustomConverterConfig.class, TestConfig.class}, OTHER_OTHER_BUCKET)
    };

    static {
        if (!containerClient.exists()) {
            containerClient.create();
        }

        System.setProperty("spring.content.azure.bucket", BUCKET);
    }

    private DualContentJpaLongIdMimeEntity entity;
    private AnnotationConfigApplicationContext context;
    private DualContentJpaLongIdMimeRepository repo;
    private DualContentJpaLongIdMimeStore store;

    static Stream<TestData> converterConfigs() {
        return Stream.of(TEST_DATA_SETS);
    }

    private void bootContext(TestData testDataSet) {
        context = new AnnotationConfigApplicationContext();
        context.register(testDataSet.config());
        context.refresh();

        repo = context.getBean(DualContentJpaLongIdMimeRepository.class);
        store = context.getBean(DualContentJpaLongIdMimeStore.class);
    }

    private record TestData(String name, Class<?>[] config, String bucket) {
        @Override
        public @NonNull String toString() {
            return name;
        }
    }

    @Configuration
    @EnableJpaRepositories(basePackages = {TestSupportPackages.JPA, "internal.org.springframework.content.azure.it"},
        considerNestedRepositories = true)
    @EnableAzureStorage(basePackages = {TestSupportPackages.JPA, "internal.org.springframework.content.azure.it"})
    @Import(InfrastructureConfig.class)
    public static class TestConfig {
        @Bean
        public BlobServiceClientBuilder blobServiceClientBuilder() {
            return builder;
        }
    }

    @Configuration
    public static class CustomConverterConfig {

        @Bean
        public AzureStorageConfigurer configurer() {
            return registry -> {
                registry.addConverter((Converter<ContentPropertyInfo<TestEntity, Serializable>, BlobId>) info -> new BlobId(OTHER_OTHER_BUCKET, info.contentId().toString()));

                registry.addConverter((Converter<ContentPropertyInfo<FakeEntity, Serializable>, BlobId>) info -> {
                    throw new IllegalStateException("wrong converter called");
                });
            };
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
            return EmbeddedJpa.entityManagerFactory(
                dataSource(), Database.H2, "internal.org.springframework.content.azure.it", TestSupportPackages.JPA);
        }

        @Bean
        public PlatformTransactionManager transactionManager() {
            return EmbeddedJpa.transactionManager(entityManagerFactory().getObject());
        }
    }

    /**
     * Azure {@link Bucket} lives on a subtype; commons test-jar cannot depend on this module.
     */
    @Entity
    @Table(name = "test_entity")
    @Access(AccessType.FIELD)
    public static class TestEntity extends DualContentJpaLongIdMimeEntity {

        @Bucket
        @SuppressWarnings("unused") // read by Azure entity-level @Bucket mapping
        private final String bucket = OTHER_BUCKET;
    }

    @Nested
    class GivenAnEntityWithContent {
        @ParameterizedTest(name = "{0}")
        @MethodSource("internal.org.springframework.content.azure.it.AzureStorageWithEntityConverterIT#converterConfigs")
        void shouldStoreNewContentInBucket(TestData testDataSet) {
            try {
                bootContext(testDataSet);

                entity = new TestEntity();
                entity.setContentType("text/plain");
                entity = repo.save(entity);

                store.setContent(entity, PropertyPath.from("content"), TestSupportData.sampleTextStream());
                BlobClient blobClient = builder.buildClient().getBlobContainerClient(testDataSet.bucket())
                    .getBlobClient(Objects.requireNonNull(entity.getContentId()));
                assertThat(blobClient.exists()).isTrue();
            } finally {
                context.close();
            }
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("internal.org.springframework.content.azure.it.AzureStorageWithEntityConverterIT#converterConfigs")
        void shouldHaveContentMetadata(TestData testDataSet) {
            try {
                bootContext(testDataSet);

                entity = new TestEntity();
                entity.setContentType("text/plain");
                entity = repo.save(entity);

                store.setContent(entity, PropertyPath.from("content"), TestSupportData.sampleTextStream());
                // content
                assertThat(entity.getContentId()).isNotNull();
                assertThat(entity.getContentId().trim().length()).isGreaterThan(0);
                assertThat(entity.getContentLen()).isEqualTo(27L);
            } finally {
                context.close();
            }
        }

        @Nested
        class WhenContentIsDeleted {
            @ParameterizedTest(name = "{0}")
            @MethodSource("internal.org.springframework.content.azure.it.AzureStorageWithEntityConverterIT#converterConfigs")
            void shouldDeleteContentFromBucket(TestData testDataSet) {
                try {
                    bootContext(testDataSet);

                    entity = new TestEntity();
                    entity.setContentType("text/plain");
                    entity = repo.save(entity);

                    store.setContent(entity, PropertyPath.from("content"), TestSupportData.sampleTextStream());

                    String resourceLocation = Objects.requireNonNull(entity.getContentId());
                    entity = store.unsetContent(entity, PropertyPath.from("content"));
                    entity = repo.save(entity);
                    BlobClient blobClient = builder.buildClient().getBlobContainerClient(testDataSet.bucket()).getBlobClient(resourceLocation);
                    assertThat(blobClient.exists()).isFalse();
                } finally {
                    context.close();
                }
            }
        }
    }
}
