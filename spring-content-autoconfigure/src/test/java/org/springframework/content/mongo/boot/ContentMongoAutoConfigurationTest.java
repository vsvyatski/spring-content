package org.springframework.content.mongo.boot;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Disabled;
import static org.assertj.core.api.Assertions.assertThat;

import com.mongodb.client.MongoClient;
import internal.org.springframework.content.mongo.boot.autoconfigure.MongoContentAutoConfiguration;
import internal.org.springframework.content.s3.boot.autoconfigure.S3ContentAutoConfiguration;
import internal.org.springframework.content.solr.boot.autoconfigure.SolrAutoConfiguration;
import internal.org.springframework.content.solr.boot.autoconfigure.SolrExtensionAutoConfiguration;
import org.assertj.core.api.Assertions;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.mongo.store.MongoContentStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.data.mongodb.repository.MongoRepository;

public class ContentMongoAutoConfigurationTest {

    private ApplicationContextRunner contextRunner;

    
    @Nested
    class ContentMongoAutoConfiguration {
        @BeforeEach
        void setUp() throws Throwable {
            contextRunner = new ApplicationContextRunner()
                                .withConfiguration(AutoConfigurations.of(MongoContentAutoConfiguration.class));
        }

        @Test
        void shouldLoadTheContext() throws Throwable {
            contextRunner.withUserConfiguration(TestConfig.class)
                                .run((context) ->
                                        Assertions.assertThat(context).hasSingleBean(TestEntityContentRepository.class));
        }

    }

    @Disabled("This is not a test")
    @Configuration
    public static class InfrastructureConfig extends AbstractMongoClientConfiguration {
        @Override
        protected @NonNull String getDatabaseName() {
            return MongoTestContainer.getTestDbName();
        }

        @Override
        @Bean
        public @NonNull MongoClient mongoClient() {
            return MongoTestContainer.getMongoClient();
        }

        @Bean
        public GridFsTemplate gridFsTemplate(MappingMongoConverter mongoConverter) {
            return new GridFsTemplate(mongoDbFactory(), mongoConverter);
        }

        @Override
        @Bean
        public @NonNull MongoDatabaseFactory mongoDbFactory() {
            return new SimpleMongoClientDatabaseFactory(mongoClient(), getDatabaseName());
        }
    }

    @Disabled("This is not a test")
    @SpringBootApplication(exclude = {SolrAutoConfiguration.class, SolrExtensionAutoConfiguration.class, S3ContentAutoConfiguration.class})
    @Import(InfrastructureConfig.class)
    public static class TestConfig {
    }

    @Disabled("This is not a test")
    @Document
    public static class TestEntity {
        @Id
        private String id;
        @ContentId
        private String contentId;
    }

    public interface TestEntityRepository extends MongoRepository<TestEntity, String> {
    }

    public interface TestEntityContentRepository extends MongoContentStore<TestEntity, String> {
    }
}
