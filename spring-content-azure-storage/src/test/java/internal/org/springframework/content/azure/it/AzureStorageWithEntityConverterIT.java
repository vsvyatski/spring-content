package internal.org.springframework.content.azure.it;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import jakarta.persistence.*;
import lombok.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.content.azure.Bucket;
import org.springframework.content.azure.config.AzureStorageConfigurer;
import org.springframework.content.azure.config.BlobId;
import org.springframework.content.azure.config.EnableAzureStorage;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.config.ContentPropertyInfo;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.ContentStore;
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

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;

@RunWith(Ginkgo4jRunner.class)
@Ginkgo4jConfiguration(threads = 1)
public class AzureStorageWithEntityConverterIT {

    private static final BlobServiceClientBuilder builder = Azurite.getBlobServiceClientBuilder();
    private static final BlobContainerClient containerClient = builder.buildClient().getBlobContainerClient("test");

    private static final String BUCKET = "aws-test-bucket";
    private static final String OTHER_BUCKET = "other-bucket";
    private static final String OTHER_OTHER_BUCKET = "other-other-bucket";

    private static final TestData[] testDataSets;

    static {
        if (!containerClient.exists()) {
            containerClient.create();
        }

        System.setProperty("spring.content.azure.bucket", BUCKET);

        testDataSets = new TestData[]{
                new TestData("Default Converter", new Class[]{TestConfig.class}, OTHER_BUCKET),
                new TestData("Custom Converter", new Class[]{CustomConverterConfig.class, TestConfig.class}, OTHER_OTHER_BUCKET),

        };
    }

    @Data
    @AllArgsConstructor
    private static class TestData {
        private String name;
        private Class[] config;
        private String bucket;
    }

    private TestEntity entity;

    private Exception e;

    private AnnotationConfigApplicationContext context;

    private TestEntityRepository repo;
    private TestEntityStore store;

    private String resourceLocation;

    {
        for (TestData testDataSet : testDataSets) {

            Describe(testDataSet.getName(), () -> {

                BeforeEach(() -> {
                    context = new AnnotationConfigApplicationContext();
                    context.register(testDataSet.getConfig());
                    context.refresh();

                    repo = context.getBean(TestEntityRepository.class);
                    store = context.getBean(TestEntityStore.class);
                });

                AfterEach(() -> context.close());

                Describe("given an entity with content", () -> {

                    BeforeEach(() -> {
                        entity = new TestEntity();
                        entity.setContentType("text/plain");
                        entity = repo.save(entity);

                        store.setContent(entity, PropertyPath.from("content"), new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                    });

                    It("should store new content in bucket '" + testDataSet.getBucket() + "'", () -> {
                        BlobClient blobClient = builder.buildClient().getBlobContainerClient(testDataSet.getBucket()).getBlobClient(entity.getContentId().toString());
                        assertThat(blobClient.exists(), is(true));
                    });

                    It("should have content metadata", () -> {
                        // content
                        assertThat(entity.getContentId(), is(notNullValue()));
                        assertThat(entity.getContentId().toString().trim().length(), greaterThan(0));
                        assertThat(entity.getContentLen(), is(27L));
                    });

                    Context("when content is deleted", () -> {
                        BeforeEach(() -> {
                            resourceLocation = entity.getContentId().toString();
                            entity = store.unsetContent(entity, PropertyPath.from("content"));
                            entity = repo.save(entity);
                        });

                        It("should delete content from bucket '" + testDataSet.getBucket() + "'", () -> {
                            BlobClient blobClient = builder.buildClient().getBlobContainerClient(testDataSet.getBucket()).getBlobClient(resourceLocation);
                            assertThat(blobClient.exists(), is(false));
                        });
                    });
                });
            });
        }
    }

    @Test
    public void test() {
        // noop
    }

    @Configuration
    @EnableJpaRepositories(basePackages = "internal.org.springframework.content.azure.it", considerNestedRepositories = true)
    @EnableAzureStorage(basePackages = "internal.org.springframework.content.azure.it")
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

                registry.addConverter(
                        (Converter<ContentPropertyInfo<TestEntity, Serializable>, BlobId>) info -> new BlobId(OTHER_OTHER_BUCKET, info.contentId().toString())
                );

                registry.addConverter(
                        (Converter<ContentPropertyInfo<FakeEntity, Serializable>, BlobId>) info -> {
                            throw new IllegalStateException("wrong converter called");
                        }
                );
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
            factory.setPackagesToScan("internal.org.springframework.content.azure.it");
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
    @Setter
    @Getter
    @NoArgsConstructor
    @Table(name = "test_entity")
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

        public TestEntity(String contentId) {
            this.contentId = contentId;
        }
    }

    public interface TestEntityRepository extends JpaRepository<TestEntity, Long> {
    }

    public interface TestEntityStore extends ContentStore<TestEntity, String> {
    }
}
