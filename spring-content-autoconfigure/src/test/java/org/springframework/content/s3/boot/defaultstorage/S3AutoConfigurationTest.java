package org.springframework.content.s3.boot.defaultstorage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Disabled;
import static org.assertj.core.api.Assertions.assertThat;

import internal.org.springframework.content.elasticsearch.boot.autoconfigure.ElasticsearchAutoConfiguration;
import internal.org.springframework.content.s3.boot.autoconfigure.S3ContentAutoConfiguration;
import org.assertj.core.api.Assertions;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.s3.config.EnableS3Stores;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.support.TestEntity;
import software.amazon.awssdk.services.s3.S3Client;

import static org.mockito.Mockito.mock;

public class S3AutoConfigurationTest {

	static {
		mock(S3Client.class);
    }

    private ApplicationContextRunner contextRunner;

    @Nested
    class S3AutoConfigurationWithDefaultStorage {
        @Nested
        class GivenADefaultStorageTypeOfS3 {
            @BeforeEach
            void setUp() {
                contextRunner = new ApplicationContextRunner()
                        .withConfiguration(AutoConfigurations.of(S3ContentAutoConfiguration.class));

                System.setProperty("spring.content.storage.type.default", "s3");
            }

            @AfterEach
            void tearDown() {
                System.clearProperty("spring.content.storage.type.default");
            }

            @Test
            void shouldCreateAnS3ClientBean() {
                contextRunner.withUserConfiguration(TestConfigWithoutBeans.class).run((context) -> {
                    Assertions.assertThat(context).hasSingleBean(S3Client.class);
                });
            }

        }

        @Nested
        class GivenADefaultStorageTypeOtherThanS3 {
            @BeforeEach
            void setUp() {
                contextRunner = new ApplicationContextRunner()
                        .withConfiguration(AutoConfigurations.of(S3ContentAutoConfiguration.class));

                System.setProperty("spring.content.storage.type.default", "fs");
            }

            @AfterEach
            void tearDown() {
                System.clearProperty("spring.content.storage.type.default");
            }

            @Test
            void shouldNotCreateAnS3ClientBean() {
                contextRunner.withUserConfiguration(TestConfigWithoutBeans.class).run((context) -> {
                    Assertions.assertThat(context).doesNotHaveBean(S3Client.class);
                });
            }

        }

        @Nested
        class GivenNoDefaultStorageType {
            @BeforeEach
            void setUp() {
                contextRunner = new ApplicationContextRunner()
                        .withConfiguration(AutoConfigurations.of(S3ContentAutoConfiguration.class));
            }

            @Test
            void shouldCreateAnS3ClientBean() {
                contextRunner.withUserConfiguration(TestConfigWithoutBeans.class).run((context) -> {
                    Assertions.assertThat(context).hasSingleBean(S3Client.class);
                });
            }

        }

    }

    @Disabled("This is not a test")
    @SpringBootApplication(exclude={ElasticsearchAutoConfiguration.class})
	@EnableS3Stores(basePackageClasses=S3AutoConfigurationTest.class)
	public static class TestConfigWithoutBeans {
		// will be supplied by autoconfiguration
	}

	public interface TestEntityRepository extends JpaRepository<TestEntity, Long> {
	}

	public interface TestEntityContentRepository extends ContentStore<TestEntity, String> {
	}
}
