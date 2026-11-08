package org.springframework.content.elasticsearch.boot;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import internal.org.springframework.content.elasticsearch.ElasticsearchIndexServiceImpl;
import internal.org.springframework.content.elasticsearch.ElasticsearchIndexer;
import internal.org.springframework.content.elasticsearch.IndexManager;
import internal.org.springframework.content.elasticsearch.boot.autoconfigure.ElasticsearchAutoConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.content.elasticsearch.EnableElasticsearchFulltextIndexing;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ElasticsearchAutoConfigurationTest {

    private static final ElasticsearchClient client;

    static {
        client = mock(ElasticsearchClient.class);
    }

    @Nested
    class GivenAContextWithoutAElasticsearchClientConfigured {
        @Test
        void shouldCreateAClient() {
            final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(ElasticsearchAutoConfiguration.class));

            contextRunner.withUserConfiguration(ContextWithoutClientBean.class).run((context) -> {
                assertThat(context).hasSingleBean(ElasticsearchClient.class);
                assertThat(context).getBean(ElasticsearchClient.class).isNotEqualTo(client);
                assertThat(context).hasSingleBean(ElasticsearchAutoConfiguration.ElasticsearchProperties.class);
                assertThat(context).hasSingleBean(ElasticsearchIndexer.class);

                assertThat(context).hasSingleBean(ElasticsearchIndexServiceImpl.class);
                assertThat(context).hasSingleBean(IndexManager.class);
            });
        }
    }

    @Nested
    class GivenAContextWithAElasticsearchClientConfigured {
        @Test
        void shouldUseThatClient() {
            final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(ElasticsearchAutoConfiguration.class));

            contextRunner.withUserConfiguration(ContextWithClientBean.class)
                    .run((context) ->
                            assertThat(context).getBean(ElasticsearchClient.class).isEqualTo(client));
        }
    }

    @Nested
    class GivenAContextWithAutoIndexingDisabled {
        @BeforeEach
        void setUp() {
            System.setProperty("spring.content.elasticsearch.autoindex", "false");
        }

        @AfterEach
        void tearDown() {
            System.clearProperty("spring.content.elasticsearch.autoindex");
        }

        @Test
        void shouldNotConfigureTheIndexingEventHandler() {
            final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(ElasticsearchAutoConfiguration.class));

            contextRunner.withUserConfiguration(ContextWithClientBean.class)
                    .run((context) ->
                            assertThat(context).doesNotHaveBean(ElasticsearchIndexer.class));
        }
    }

    @Nested
    class GivenAContextWithAutoIndexingConfigured {
        @BeforeEach
        void setUp() {
            System.setProperty("spring.content.elasticsearch.autoindex", "true");
        }

        @AfterEach
        void tearDown() {
            System.clearProperty("spring.content.elasticsearch.autoindex");
        }

        @Test
        void shouldLoadTheContext() {
            final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(ElasticsearchAutoConfiguration.class));

            contextRunner.withUserConfiguration(ContextWithClientBean.class)
                    .run((context) ->
                            assertThat(context).hasSingleBean(ElasticsearchIndexer.class));
        }
    }

    @Nested
    class GivenAContextThatAlreadyEnablesElasticSearchFulltextIndexing {
        @Test
        void shouldLoadTheContextAndNotThrowABeanDefinitionOverrideException() {
            final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(ElasticsearchAutoConfiguration.class));

            contextRunner.withUserConfiguration(ContextWithEnablement.class)
                    .run((context) ->
                            assertThat(context).hasSingleBean(ElasticsearchClient.class));
        }
    }

    @Configuration
    public static class ContextWithoutClientBean {
    }

    @Configuration
    public static class ContextWithClientBean {

        @Bean
        public ElasticsearchClient client() {
            return client;
        }
    }

    @Configuration
    @EnableElasticsearchFulltextIndexing
    public static class ContextWithEnablement {

        @Bean
        public ElasticsearchClient client() {
            return client;
        }
    }
}
