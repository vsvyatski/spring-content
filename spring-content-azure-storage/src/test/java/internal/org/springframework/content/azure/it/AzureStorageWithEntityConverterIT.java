package internal.org.springframework.content.azure.it;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.TestFactory;
import static org.assertj.core.api.Assertions.assertThat;


import java.io.ByteArrayInputStream;
import java.io.Serializable;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import javax.sql.DataSource;

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
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClientBuilder;

public class AzureStorageWithEntityConverterIT {

    private static final BlobServiceClientBuilder builder = Azurite.getBlobServiceClientBuilder();
    private static final BlobContainerClient containerClient = builder.buildClient().getBlobContainerClient("test");

    private static final String BUCKET = "aws-test-bucket";
    private static final String OTHER_BUCKET = "other-bucket";
    private static final String OTHER_OTHER_BUCKET = "other-other-bucket";

    private static TestData[] testDataSets = null;

    static {
        if (!containerClient.exists()) {
            containerClient.create();
        }

        System.setProperty("spring.content.azure.bucket", BUCKET);

        testDataSets = new TestData[] {
                new TestData("Default Converter", new Class[] {TestConfig.class}, OTHER_BUCKET),
                new TestData("Custom Converter", new Class[] {CustomConverterConfig.class, TestConfig.class}, OTHER_OTHER_BUCKET),

        };
    }

    private static class TestData {
        private String name;
        private Class[] config;
        private String bucket;

        public TestData(String name, Class[] config, String bucket) {
            this.name = name;
            this.config = config;
            this.bucket = bucket;
        }

        public String getName() {
            return name;
        }

        public Class[] getConfig() {
            return config;
        }

        public String getBucket() {
            return bucket;
        }
    }

    private TestEntity entity;

    private Exception e;

    private AnnotationConfigApplicationContext context;

    private TestEntityRepository repo;
    private TestEntityStore store;

    private String resourceLocation;

    
    @TestFactory
    java.util.stream.Stream<org.junit.jupiter.api.DynamicNode> generatedCases() {
        java.util.List<org.junit.jupiter.api.DynamicNode> tests = new java.util.ArrayList<>();
        for (TestData testDataSet : testDataSets) {
            {
                java.util.List<org.junit.jupiter.api.DynamicNode> nodes3 = new java.util.ArrayList<>();
                {
                    java.util.List<org.junit.jupiter.api.DynamicNode> nodes2 = new java.util.ArrayList<>();
                    nodes2.add(org.junit.jupiter.api.DynamicTest.dynamicTest("should store new content in bucket '", () -> {
                        try {
                            context = new AnnotationConfigApplicationContext();
                                                context.register(testDataSet.getConfig());
                                                context.refresh();

                                                repo = context.getBean(TestEntityRepository.class);
                                                store = context.getBean(TestEntityStore.class);

                            entity = new TestEntity();
                                                    entity.setContentType("text/plain");
                                                    entity = repo.save(entity);

                                                    store.setContent(entity, PropertyPath.from("content"), new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                            BlobClient blobClient = builder.buildClient().getBlobContainerClient(testDataSet.getBucket()).getBlobClient(entity.getContentId().toString());
                                                    assertThat(blobClient.exists()).isTrue();
                        } finally {
                            context.close();
                        }
                    }));
                    nodes2.add(org.junit.jupiter.api.DynamicTest.dynamicTest("should have content metadata", () -> {
                        try {
                            context = new AnnotationConfigApplicationContext();
                                                context.register(testDataSet.getConfig());
                                                context.refresh();

                                                repo = context.getBean(TestEntityRepository.class);
                                                store = context.getBean(TestEntityStore.class);

                            entity = new TestEntity();
                                                    entity.setContentType("text/plain");
                                                    entity = repo.save(entity);

                                                    store.setContent(entity, PropertyPath.from("content"), new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                            // content
                                                    assertThat(entity.getContentId()).isNotNull();
                                                    assertThat(entity.getContentId().toString().trim().length()).isGreaterThan(0);
                                                    assertThat(entity.getContentLen()).isEqualTo(27L);
                        } finally {
                            context.close();
                        }
                    }));
                    {
                        java.util.List<org.junit.jupiter.api.DynamicNode> nodes1 = new java.util.ArrayList<>();
                        nodes1.add(org.junit.jupiter.api.DynamicTest.dynamicTest("should delete content from bucket '", () -> {
                            try {
                                context = new AnnotationConfigApplicationContext();
                                                    context.register(testDataSet.getConfig());
                                                    context.refresh();

                                                    repo = context.getBean(TestEntityRepository.class);
                                                    store = context.getBean(TestEntityStore.class);

                                entity = new TestEntity();
                                                        entity.setContentType("text/plain");
                                                        entity = repo.save(entity);

                                                        store.setContent(entity, PropertyPath.from("content"), new ByteArrayInputStream("Hello Spring Content World!".getBytes()));

                                resourceLocation = entity.getContentId().toString();
                                                            entity = store.unsetContent(entity, PropertyPath.from("content"));
                                                            entity = repo.save(entity);
                                BlobClient blobClient = builder.buildClient().getBlobContainerClient(testDataSet.getBucket()).getBlobClient(resourceLocation);
                                                            assertThat(blobClient.exists()).isFalse();
                            } finally {
                                context.close();
                            }
                        }));
                        nodes1.add(org.junit.jupiter.api.DynamicContainer.dynamicContainer("when content is deleted", nodes1.stream()));
                    }
                    nodes2.add(org.junit.jupiter.api.DynamicContainer.dynamicContainer("given an entity with content", nodes2.stream()));
                }
                tests.add(org.junit.jupiter.api.DynamicContainer.dynamicContainer(testDataSet.getName(), nodes3.stream()));
            }
        }
        return tests.stream();
    }


    @Test
    public void test() {
        // noop
    }

    @Configuration
    @EnableJpaRepositories(basePackages="internal.org.springframework.content.azure.it", considerNestedRepositories = true)
    @EnableAzureStorage(basePackages="internal.org.springframework.content.azure.it")
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
          return new AzureStorageConfigurer() {

              @Override
              public void configureAzureStorageConverters(ConverterRegistry registry) {

                  registry.addConverter(new Converter<ContentPropertyInfo<TestEntity, Serializable>, BlobId>() {
                      @Override
                      public BlobId convert(ContentPropertyInfo<TestEntity, Serializable> info) {
                          return new BlobId(OTHER_OTHER_BUCKET, info.contentId().toString());
                      }
                  });


                  registry.addConverter(new Converter<ContentPropertyInfo<FakeEntity, Serializable>, BlobId>() {
                      @Override
                      public BlobId convert(ContentPropertyInfo<FakeEntity, Serializable> info) {
                          throw new IllegalStateException("wrong converter called");
                      }
                  });
              }
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
    @Table(name="test_entity")
    public static class TestEntity {

        @Id
        @GeneratedValue(strategy=GenerationType.AUTO)
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

        public TestEntity(String contentId) {
            this.contentId = contentId;
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

    public interface TestEntityRepository extends JpaRepository<TestEntity, Long> {}
    public interface TestEntityStore extends ContentStore<TestEntity, String> {}
}
