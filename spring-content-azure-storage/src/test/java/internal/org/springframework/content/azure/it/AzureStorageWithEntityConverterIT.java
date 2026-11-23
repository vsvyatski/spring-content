package internal.org.springframework.content.azure.it;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.springframework.content.azure.Bucket;
import org.springframework.content.azure.config.AzureStorageConfigurer;
import org.springframework.content.azure.config.BlobId;
import org.springframework.content.azure.config.EnableAzureStorage;
import org.springframework.content.commons.config.ContentPropertyInfo;
import org.springframework.content.test.support.AbstractEntityConverterBucketIT;
import org.springframework.content.test.support.EmbeddedJpa;
import org.springframework.content.test.support.FakeEntity;
import org.springframework.content.test.support.TestSupportPackages;
import org.springframework.content.test.support.jpa.DualContentJpaLongIdMimeEntity;
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

public class AzureStorageWithEntityConverterIT extends AbstractEntityConverterBucketIT {

    private static final BlobServiceClientBuilder builder = Azurite.getBlobServiceClientBuilder();
    private static final BlobContainerClient containerClient = builder.buildClient().getBlobContainerClient("test");

    static {
        if (!containerClient.exists()) {
            containerClient.create();
        }

        System.setProperty("spring.content.azure.bucket", BUCKET);
    }

    static Stream<Case> converterConfigs() {
        return Stream.of(cases(TestConfig.class, CustomConverterConfig.class));
    }

    @Override
    protected DualContentJpaLongIdMimeEntity newEntity() {
        return new TestEntity();
    }

    @Override
    protected void assertStored(Case testData) {
        BlobClient blobClient = builder.buildClient().getBlobContainerClient(testData.bucket())
            .getBlobClient(Objects.requireNonNull(entity.getContentId()));
        assertThat(blobClient.exists()).isTrue();
    }

    @Override
    protected void assertRemoved(Case testData, String contentId) {
        BlobClient blobClient = builder.buildClient().getBlobContainerClient(testData.bucket()).getBlobClient(contentId);
        assertThat(blobClient.exists()).isFalse();
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
}
