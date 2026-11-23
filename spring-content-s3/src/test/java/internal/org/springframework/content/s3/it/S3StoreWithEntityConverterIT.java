package internal.org.springframework.content.s3.it;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.content.commons.config.ContentPropertyInfo;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.s3.Bucket;
import org.springframework.content.s3.S3ObjectId;
import org.springframework.content.s3.config.EnableS3Stores;
import org.springframework.content.s3.config.S3StoreConfigurer;
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
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.transaction.PlatformTransactionManager;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import javax.sql.DataSource;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class S3StoreWithEntityConverterIT {

    private static final String BUCKET = "spring-eg-content-s3";
    private static final String OTHER_BUCKET = "other-bucket-4f91c2";
    private static final String OTHER_OTHER_BUCKET = "other-other-bucket-8c3e17";

    private static final TestData[] TEST_DATA_SETS = {
        new TestData("Default Converter", new Class<?>[]{TestConfig.class}, OTHER_BUCKET),
        new TestData("Custom Converter", new Class<?>[]{CustomConverterConfig.class, TestConfig.class}, OTHER_OTHER_BUCKET)
    };
    private static S3Client s3ClientSpy;

    static {
        System.setProperty("spring.content.s3.bucket", BUCKET);
    }

    private DualContentJpaLongIdMimeEntity entity;

    private AnnotationConfigApplicationContext context;

    private DualContentJpaLongIdMimeRepository repo;
    private DualContentJpaLongIdMimeStore store;
    private S3Client client;

    static Stream<TestData> converterConfigs() {
        return Stream.of(TEST_DATA_SETS);
    }

    private void bootContext(TestData testDataSet) {
        context = new AnnotationConfigApplicationContext();
        context.register(testDataSet.config());
        context.refresh();

        repo = context.getBean(DualContentJpaLongIdMimeRepository.class);
        store = context.getBean(DualContentJpaLongIdMimeStore.class);
        client = context.getBean(S3Client.class);

        try {
            HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                .bucket(testDataSet.bucket())
                .build();

            client.headBucket(headBucketRequest);
        } catch (NoSuchBucketException e) {

            CreateBucketRequest bucketRequest = CreateBucketRequest.builder()
                .bucket(testDataSet.bucket())
                .build();
            client.createBucket(bucketRequest);
        }
    }

    private void saveEntityWithContent() {
        entity = new TestEntity();
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
    @EnableJpaRepositories(basePackages = {TestSupportPackages.JPA, "internal.org.springframework.content.s3.it"},
        considerNestedRepositories = true)
    @EnableS3Stores(basePackages = {TestSupportPackages.JPA, "internal.org.springframework.content.s3.it"})
    @Import(InfrastructureConfig.class)
    public static class TestConfig {

        @Bean
        public S3Client client() throws URISyntaxException {
            s3ClientSpy = spy(LocalStack.getAmazonS3Client());
            return s3ClientSpy;
        }
    }

    @Configuration
    public static class CustomConverterConfig {

        @Bean
        public S3StoreConfigurer configurer() {
            return registry -> {
                registry.addConverter(
                    (@NonNull ContentPropertyInfo<TestEntity, Serializable> info) ->
                        new S3ObjectId(OTHER_OTHER_BUCKET, String.valueOf(info.contentId())));
                registry.addConverter(
                    (@NonNull ContentPropertyInfo<FakeEntity, Serializable> info) -> {
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
                dataSource(), Database.H2, "internal.org.springframework.content.s3.it", TestSupportPackages.JPA);
        }

        @Bean
        public PlatformTransactionManager transactionManager() {
            return EmbeddedJpa.transactionManager(entityManagerFactory().getObject());
        }
    }

    /**
     * S3 {@link Bucket} lives on a subtype; commons test-jar cannot depend on this module.
     */
    @Entity
    @Table(name = "test_entity")
    @Access(AccessType.FIELD)
    public static class TestEntity extends DualContentJpaLongIdMimeEntity {

        @Bucket
        @SuppressWarnings("unused") // read by S3 entity-level @Bucket mapping
        private final String bucket = OTHER_BUCKET;
    }

    @Nested
    class GivenAnEntityWithContent {
        @ParameterizedTest(name = "{0}")
        @MethodSource("internal.org.springframework.content.s3.it.S3StoreWithEntityConverterIT#converterConfigs")
        void shouldStoreNewContentInBucket(TestData testDataSet) {
            try {
                bootContext(testDataSet);
                saveEntityWithContent();
                ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
                verify(s3ClientSpy).putObject(captor.capture(), any(RequestBody.class));
                assertThat(captor.getValue().bucket()).isEqualTo(testDataSet.bucket());

                String contentKey = entity.getContentId();
                assertThat(contentKey).isNotNull();
                client.headObject(HeadObjectRequest.builder()
                    .bucket(testDataSet.bucket())
                    .key(contentKey)
                    .build());
            } finally {
                context.close();
            }
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("internal.org.springframework.content.s3.it.S3StoreWithEntityConverterIT#converterConfigs")
        void shouldHaveContentMetadata(TestData testDataSet) {
            try {
                bootContext(testDataSet);
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
            @MethodSource("internal.org.springframework.content.s3.it.S3StoreWithEntityConverterIT#converterConfigs")
            void shouldDeleteContentFromBucket(TestData testDataSet) {
                try {
                    bootContext(testDataSet);
                    saveEntityWithContent();

                    String resourceLocation = entity.getContentId();
                    entity = store.unsetContent(entity, PropertyPath.from("content"));
                    entity = repo.save(entity);
                    ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
                    verify(s3ClientSpy).deleteObject(captor.capture());
                    assertThat(captor.getValue().bucket()).isEqualTo(testDataSet.bucket());

                    HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                        .bucket(testDataSet.bucket())
                        .key(resourceLocation)
                        .build();

                    assertThatThrownBy(() -> client.headObject(headObjectRequest))
                        .isInstanceOf(NoSuchKeyException.class);
                } finally {
                    context.close();
                }
            }
        }
    }
}
