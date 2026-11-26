package internal.org.springframework.content.s3.it;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.jspecify.annotations.NonNull;
import org.mockito.ArgumentCaptor;
import org.springframework.content.commons.config.ContentPropertyInfo;
import org.springframework.content.s3.Bucket;
import org.springframework.content.s3.S3ObjectId;
import org.springframework.content.s3.config.EnableS3Stores;
import org.springframework.content.s3.config.S3StoreConfigurer;
import org.springframework.content.test.support.AbstractEntityConverterBucketIT;
import org.springframework.content.test.support.EmbeddedJpa;
import org.springframework.content.test.support.FakeEntity;
import org.springframework.content.test.support.TestSupportPackages;
import org.springframework.content.test.support.s3.LocalStack;
import org.springframework.content.test.support.jpa.DualContentJpaLongIdMimeEntity;
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
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class S3StoreWithEntityConverterIT extends AbstractEntityConverterBucketIT {

    private static S3Client s3ClientSpy;

    static {
        System.setProperty("spring.content.s3.bucket", INIT_BUCKET);
    }

    private S3Client client;

    static Stream<Case> converterConfigs() {
        return Stream.of(cases(TestConfig.class, CustomConverterConfig.class));
    }

    @Override
    protected DualContentJpaLongIdMimeEntity newEntity() {
        return new TestEntity();
    }

    @Override
    protected void afterRefresh(Case testData) {
        client = context.getBean(S3Client.class);
        try {
            client.headBucket(HeadBucketRequest.builder().bucket(testData.bucket()).build());
        } catch (NoSuchBucketException e) {
            client.createBucket(CreateBucketRequest.builder().bucket(testData.bucket()).build());
        }
    }

    @Override
    protected void assertStored(Case testData) {
        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3ClientSpy).putObject(captor.capture(), any(RequestBody.class));
        assertThat(captor.getValue().bucket()).isEqualTo(testData.bucket());

        client.headObject(HeadObjectRequest.builder()
            .bucket(testData.bucket())
            .key(entity.getContentId())
            .build());
    }

    @Override
    protected void assertRemoved(Case testData, String contentId) {
        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3ClientSpy).deleteObject(captor.capture());
        assertThat(captor.getValue().bucket()).isEqualTo(testData.bucket());

        HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
            .bucket(testData.bucket())
            .key(contentId)
            .build();

        assertThatThrownBy(() -> client.headObject(headObjectRequest))
            .isInstanceOf(NoSuchKeyException.class);
    }

    @Configuration
    @EnableJpaRepositories(basePackages = {TestSupportPackages.JPA, "internal.org.springframework.content.s3.it"},
        considerNestedRepositories = true)
    @EnableS3Stores(basePackages = {TestSupportPackages.JPA, "internal.org.springframework.content.s3.it"})
    @Import(InfrastructureConfig.class)
    public static class TestConfig {

        @Bean
        public S3Client client() {
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
                        new S3ObjectId(CUSTOM_BUCKET, String.valueOf(info.contentId())));
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
        private final String bucket = DEFAULT_BUCKET;
    }
}
