package internal.org.springframework.content.gcs.it;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.contrib.nio.testing.LocalStorageHelper;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.config.ContentPropertyInfo;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.gcs.Bucket;
import org.springframework.content.gcs.config.EnableGCPStorage;
import org.springframework.content.gcs.config.GCPStorageConfigurer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.converter.Converter;
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
import java.io.ByteArrayInputStream;
import java.io.Serializable;
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
                new TestData("Default Converter", new Class[]{TestConfig.class}, OTHER_BUCKET),
                new TestData("Custom Converter", new Class[]{CustomConverterConfig.class, TestConfig.class}, OTHER_OTHER_BUCKET),

        };
    }

    private TestEntity entity;
    private AnnotationConfigApplicationContext context;
    private TestEntityRepository repo;
    private TestEntityStore store;
    private Storage storage;

    static Stream<TestData> converterConfigs() {
        return Stream.of(testDataSets);
    }

    private void boot(TestData testDataSet) {
        context = new AnnotationConfigApplicationContext();
        context.register(testDataSet.config());
        context.refresh();

        repo = context.getBean(TestEntityRepository.class);
        store = context.getBean(TestEntityStore.class);
        storage = context.getBean(Storage.class);
    }

    private void saveEntityWithContent() {
        entity = new TestEntity();
        entity.setContentType("text/plain");
        entity = repo.save(entity);

        store.setContent(entity, PropertyPath.from("content"), new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
    }

    @Nested
    class GivenAnEntityWithContent {
        @ParameterizedTest(name = "{0}")
        @MethodSource("internal.org.springframework.content.gcs.it.GCPStorageWithEntityConverterIT#converterConfigs")
        void shouldStoreNewContentInBucket(TestData testDataSet) {
            try {
                boot(testDataSet);
                saveEntityWithContent();
                Blob blob = storage.get(BlobId.of(testDataSet.bucket(), entity.getContentId()));
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

                    String resourceLocation = entity.getContentId();
                    entity = store.unsetContent(entity, PropertyPath.from("content"));
                    entity = repo.save(entity);
                    Blob blob = storage.get(BlobId.of(testDataSet.bucket(), resourceLocation));
                    System.out.println(resourceLocation);
                    assertThat(blob).isNull();
                } finally {
                    context.close();
                }
            }
        }
    }

    public interface TestEntityRepository extends JpaRepository<TestEntity, Long> {
    }

    public interface TestEntityStore extends ContentStore<TestEntity, String> {
    }

    private record TestData(String name, Class[] config, String bucket) {

        @Override
            public @NonNull String toString() {
                return name;
            }
        }

    @Configuration
    @EnableJpaRepositories(basePackages = "internal.org.springframework.content.gcs.it", considerNestedRepositories = true)
    @EnableGCPStorage(basePackages = "internal.org.springframework.content.gcs.it")
    @Import(InfrastructureConfig.class)
    public static class TestConfig {

        @Bean
        public static Storage storage() {
            return LocalStorageHelper.getOptions().getService();
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
            EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
            return builder.setType(EmbeddedDatabaseType.H2).build();
        }

        @Bean
        public LocalContainerEntityManagerFactoryBean entityManagerFactory() {

            HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
            vendorAdapter.setDatabase(Database.H2);
            vendorAdapter.setGenerateDdl(true);

            LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
            factory.setJpaVendorAdapter(vendorAdapter);
            factory.setPackagesToScan("internal.org.springframework.content.gcs.it");
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

    public static class FakeEntity {
    }

    @Entity
    public static class TestEntity {

        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        private Long id;

        @Bucket
        private String bucket = "other-bucket";

        @ContentId
        private String contentId;

        @ContentLength
        private long contentLen;

        @MimeType
        private String contentType;

        @ContentId
        private String renditionId;

        @ContentLength
        private long renditionLen;

        @MimeType
        private String renditionContentType;

        public TestEntity() {
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }

        public String getContentId() {
            return contentId;
        }

        public void setContentId(String contentId) {
            this.contentId = contentId;
        }

        public long getContentLen() {
            return contentLen;
        }

        public void setContentLen(long contentLen) {
            this.contentLen = contentLen;
        }

        public String getContentType() {
            return contentType;
        }

        public void setContentType(String contentType) {
            this.contentType = contentType;
        }

        public String getRenditionId() {
            return renditionId;
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

        public String getRenditionContentType() {
            return renditionContentType;
        }

        public void setRenditionContentType(String renditionContentType) {
            this.renditionContentType = renditionContentType;
        }
    }
}
