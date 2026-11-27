package org.springframework.content.s3.boot.autoconfigure;

import internal.org.springframework.content.fs.boot.autoconfigure.FileSystemContentAutoConfiguration;
import internal.org.springframework.content.mongo.boot.autoconfigure.MongoContentAutoConfiguration;
import internal.org.springframework.content.s3.boot.autoconfigure.S3ContentAutoConfiguration;
import internal.org.springframework.content.solr.boot.autoconfigure.SolrAutoConfiguration;
import internal.org.springframework.content.solr.boot.autoconfigure.SolrExtensionAutoConfiguration;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.content.s3.config.EnableS3Stores;
import org.springframework.content.s3.store.S3ContentStore;
import org.springframework.content.test.support.jpa.SingleContentJpaLongIdEntity;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.services.s3.S3Client;

import static org.mockito.Mockito.mock;

public class ContentS3AutoConfigurationTest {

    private static final S3Client client;

    static {
        client = mock(S3Client.class);
    }

    private ApplicationContextRunner contextRunner;

    public interface TestEntityContentStore extends S3ContentStore<SingleContentJpaLongIdEntity, String> {
    }

    @SpringBootApplication(exclude = {FileSystemContentAutoConfiguration.class, MongoContentAutoConfiguration.class, SolrAutoConfiguration.class, SolrExtensionAutoConfiguration.class})
    public static class TestConfig {

        @Bean
        public S3Client s3Client() {
            return client;
        }
    }

    @SpringBootApplication
    public static class TestConfigWithoutBeans {
        // will be supplied by autoconfiguration
    }

    @SpringBootApplication
    @EnableS3Stores
    public static class TestConfigWithExplicitEnableS3Stores {
        // will be supplied by autoconfiguration
    }

    @SpringBootApplication
    public static class TestConfigWithProperties {
        // will be supplied by autoconfiguration
    }

    @Nested
    @DisplayName("Given a configuration with beans")
    class ConfigurationWithBeans {
        @BeforeEach
        void setUp() {
            contextRunner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(S3ContentAutoConfiguration.class));
        }

        @Test
        void shouldLoadContext() {
            contextRunner.withUserConfiguration(TestConfig.class).run((context) -> {
                Assertions.assertThat(context).hasSingleBean(TestEntityContentStore.class);
                Assertions.assertThat(context).hasSingleBean(S3Client.class);
            });
        }
    }

    @Nested
    @DisplayName("Given a configuration without any beans")
    class ConfigurationWithoutAnyBeans {
        @BeforeEach
        void setUp() {
            contextRunner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(S3ContentAutoConfiguration.class));
        }

        @Test
        void shouldLoadContext() {
            contextRunner.withUserConfiguration(TestConfigWithoutBeans.class).run((context) -> {
                Assertions.assertThat(context).hasSingleBean(TestEntityContentStore.class);
                Assertions.assertThat(context).hasSingleBean(S3Client.class);
            });
        }
    }

    @Nested
    @DisplayName("Given a configuration with explicit @EnableS3Stores")
    class ConfigurationWithExplicitEnableS3StoresAnnotation {
        @BeforeEach
        void setUp() {
            contextRunner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(S3ContentAutoConfiguration.class));
        }

        @Test
        void shouldLoadContext() {
            contextRunner.withUserConfiguration(TestConfigWithExplicitEnableS3Stores.class).run((context) -> {
                Assertions.assertThat(context).hasSingleBean(TestEntityContentStore.class);
                Assertions.assertThat(context).hasSingleBean(S3Client.class);
            });
        }
    }

    @Nested
    @DisplayName("Given an environment specifying S3 properties")
    class EnvironmentSpecifyingS3Properties {
        @BeforeEach
        void setUp() {
            contextRunner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(S3ContentAutoConfiguration.class));

            System.setProperty("spring.content.s3.endpoint", "http://some-endpoint");
            System.setProperty("spring.content.s3.accessKey", "foo");
            System.setProperty("spring.content.s3.secretKey", "bar");
            System.setProperty("spring.content.s3.pathStyleAccess", "true");
        }

        @AfterEach
        void tearDown() {
            System.clearProperty("spring.content.s3.endpoint");
            System.clearProperty("spring.content.s3.accessKey");
            System.clearProperty("spring.content.s3.secretKey");
            System.clearProperty("spring.content.s3.pathStyleAccess");
        }

        @Test
        void shouldHaveS3PropertiesBeanWithCorrectValuesAndS3Client() {
            contextRunner.withUserConfiguration(TestConfigWithProperties.class).run((context) -> {
                Assertions.assertThat(context).hasSingleBean(S3Client.class);

                S3ContentAutoConfiguration.S3Properties properties =
                    context.getBean(S3ContentAutoConfiguration.S3Properties.class);
                Assertions.assertThat(properties.getEndpoint()).isEqualTo("http://some-endpoint");
                Assertions.assertThat(properties.getAccessKey()).isEqualTo("foo");
                Assertions.assertThat(properties.getSecretKey()).isEqualTo("bar");
                Assertions.assertThat(properties.isPathStyleAccess()).isTrue();
            });
        }
    }

    @Nested
    @DisplayName("S3Properties")
    class S3PropertiesAccessor {
        @Test
        void settersPopulateGetters() {
            S3ContentAutoConfiguration.S3Properties properties = new S3ContentAutoConfiguration.S3Properties();

            properties.setEndpoint("http://some-endpoint");
            properties.setAccessKey("foo");
            properties.setSecretKey("bar");
            properties.setPathStyleAccess(true);

            Assertions.assertThat(properties.getEndpoint()).isEqualTo("http://some-endpoint");
            Assertions.assertThat(properties.getAccessKey()).isEqualTo("foo");
            Assertions.assertThat(properties.getSecretKey()).isEqualTo("bar");
            Assertions.assertThat(properties.isPathStyleAccess()).isTrue();
        }
    }
}
