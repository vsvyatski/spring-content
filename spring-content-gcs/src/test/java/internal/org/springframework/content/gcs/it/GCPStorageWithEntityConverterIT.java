package internal.org.springframework.content.gcs.it;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.springframework.content.commons.config.ContentPropertyInfo;
import org.springframework.content.gcs.Bucket;
import org.springframework.content.gcs.config.EnableGCPStorage;
import org.springframework.content.gcs.config.GCPStorageConfigurer;
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

public class GCPStorageWithEntityConverterIT extends AbstractEntityConverterBucketIT {

    static {
        System.setProperty("spring.content.gcp.storage.bucket", BUCKET);
    }

    private Storage storage;

    static Stream<Case> converterConfigs() {
        return Stream.of(cases(TestConfig.class, CustomConverterConfig.class));
    }

    @Override
    protected DualContentJpaLongIdMimeEntity newEntity() {
        return new TestEntity();
    }

    @Override
    protected void afterRefresh(Case testData) {
        storage = context.getBean(Storage.class);
    }

    @Override
    protected void assertStored(Case testData) {
        Blob blob = storage.get(BlobId.of(testData.bucket(), Objects.requireNonNull(entity.getContentId())));
        assertThat(blob.exists()).isTrue();
    }

    @Override
    protected void assertRemoved(Case testData, String contentId) {
        Blob blob = storage.get(BlobId.of(testData.bucket(), contentId));
        assertThat(blob).isNull();
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
                registry.addConverter((Converter<ContentPropertyInfo<TestEntity, Serializable>, BlobId>) info -> BlobId.of(OTHER_OTHER_BUCKET, info.contentId().toString()));
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

    /**
     * GCS {@link Bucket} lives on a subtype; commons test-jar cannot depend on this module.
     */
    @Entity
    @Table(name = "test_entity")
    @Access(AccessType.FIELD)
    public static class TestEntity extends DualContentJpaLongIdMimeEntity {

        @Bucket
        @SuppressWarnings("unused") // read by GCS entity-level @Bucket mapping
        private final String bucket = OTHER_BUCKET;
    }
}
