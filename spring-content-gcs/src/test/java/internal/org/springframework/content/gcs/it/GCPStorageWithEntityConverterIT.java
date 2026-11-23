package internal.org.springframework.content.gcs.it;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.content.commons.config.ContentPropertyInfo;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.gcs.config.EnableGCPStorage;
import org.springframework.content.gcs.config.GCPStorageConfigurer;
import org.springframework.content.test.support.EmbeddedJpa;
import org.springframework.content.test.support.FakeEntity;
import org.springframework.content.test.support.TestSupportData;
import org.springframework.content.test.support.TestSupportPackages;
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

public class GCPStorageWithEntityConverterIT {

    private static final String BUCKET = "test-bucket";
    private static final String OTHER_BUCKET = "other-bucket";
    private static final String OTHER_OTHER_BUCKET = "other-other-bucket";

    private static final TestData[] testDataSets;

    static {
        System.setProperty("spring.content.gcp.storage.bucket", BUCKET);

        testDataSets = new TestData[]{
            new TestData("Default Converter", new Class<?>[]{TestConfig.class}, OTHER_BUCKET),
            new TestData("Custom Converter", new Class<?>[]{CustomConverterConfig.class, TestConfig.class}, OTHER_OTHER_BUCKET)
        };
    }

    private GcsBucketMimeEntity entity;
    private AnnotationConfigApplicationContext context;
    private GcsBucketMimeRepository repo;
    private GcsBucketMimeStore store;
    private Storage storage;

    static Stream<TestData> converterConfigs() {
        return Stream.of(testDataSets);
    }

    private void boot(TestData testDataSet) {
        context = new AnnotationConfigApplicationContext();
        context.register(testDataSet.config());
        context.refresh();

        repo = context.getBean(GcsBucketMimeRepository.class);
        store = context.getBean(GcsBucketMimeStore.class);
        storage = context.getBean(Storage.class);
    }

    private void saveEntityWithContent() {
        entity = new GcsBucketMimeEntity();
        entity.setContentType("text/plain");
        entity = repo.save(entity);

        store.setContent(entity, PropertyPath.from("content"), TestSupportData.sampleTextStream());
    }

    private record TestData(String name, Class<?>[] config, String bucket) {
        @Override
        public @NonNull String toString() {
            return name;
        }
    }

    @Configuration
    @EnableJpaRepositories(basePackages = {TestSupportPackages.JPA, "internal.org.springframework.content.gcs.it"},
        considerNestedRepositories = true)
    @EnableGCPStorage(basePackages = {TestSupportPackages.JPA, "internal.org.springframework.content.gcs.it"})
    @Import(InfrastructureConfig.class)
    public static class TestConfig {

        @Bean
        public static Storage storage() {
            return GCSStorageContainer.getStorage();
        }
    }

    @Configuration
    public static class CustomConverterConfig {

        @Bean
        public GCPStorageConfigurer configurer() {
            return registry -> {
                registry.addConverter((Converter<ContentPropertyInfo<GcsBucketMimeEntity, Serializable>, BlobId>) info -> BlobId.of(OTHER_OTHER_BUCKET, info.contentId().toString()));
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
                dataSource(), Database.H2, "internal.org.springframework.content.gcs.it", TestSupportPackages.JPA);
        }

        @Bean
        public PlatformTransactionManager transactionManager() {
            return EmbeddedJpa.transactionManager(entityManagerFactory().getObject());
        }
    }

    @Nested
    class GivenAnEntityWithContent {
        @ParameterizedTest(name = "{0}")
        @MethodSource("internal.org.springframework.content.gcs.it.GCPStorageWithEntityConverterIT#converterConfigs")
        void shouldStoreNewContentInBucket(TestData testDataSet) {
            try {
                boot(testDataSet);
                saveEntityWithContent();
                Blob blob = storage.get(BlobId.of(testDataSet.bucket(), Objects.requireNonNull(entity.getContentId())));
                assertThat(blob.exists()).isTrue();
            } finally {
                context.close();
            }
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("internal.org.springframework.content.gcs.it.GCPStorageWithEntityConverterIT#converterConfigs")
        void shouldHaveContentMetadata(TestData testDataSet) {
            try {
                boot(testDataSet);
                saveEntityWithContent();
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
            @MethodSource("internal.org.springframework.content.gcs.it.GCPStorageWithEntityConverterIT#converterConfigs")
            void shouldDeleteContentFromBucket(TestData testDataSet) {
                try {
                    boot(testDataSet);
                    saveEntityWithContent();

                    String resourceLocation = Objects.requireNonNull(entity.getContentId());
                    entity = store.unsetContent(entity, PropertyPath.from("content"));
                    entity = repo.save(entity);
                    Blob blob = storage.get(BlobId.of(testDataSet.bucket(), resourceLocation));
                    assertThat(blob).isNull();
                } finally {
                    context.close();
                }
            }
        }
    }
}
