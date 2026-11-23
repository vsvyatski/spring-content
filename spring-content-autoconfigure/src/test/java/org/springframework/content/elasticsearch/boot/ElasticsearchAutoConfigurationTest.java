package org.springframework.content.elasticsearch.boot;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import internal.org.springframework.content.elasticsearch.ElasticsearchIndexServiceImpl;
import internal.org.springframework.content.elasticsearch.ElasticsearchIndexer;
import internal.org.springframework.content.elasticsearch.IndexManager;
import internal.org.springframework.content.elasticsearch.boot.autoconfigure.ElasticsearchAutoConfiguration;
import org.junit.jupiter.api.*;
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

    @Nested
    @DisplayName("Given a context without a elasticsearch client configured")
    class ContextWithoutElasticsearchClientConfigured {
        @Test
        void shouldCreateClient() {
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
    @DisplayName("Given a context with a elasticsearch client configured")
    class ContextWithElasticsearchClientConfigured {
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
    @DisplayName("Given a context with auto indexing disabled")
    class ContextWithAutoIndexingDisabled {
        @BeforeEach
        void setUp() {
            System.setProperty("spring.content.elasticsearch.autoindex", "false");
        }

        @AfterEach
        void tearDown() {
            System.clearProperty("spring.content.elasticsearch.autoindex");
        }

        @Test
        void shouldNotConfigureIndexingEventHandler() {
            final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ElasticsearchAutoConfiguration.class));

            contextRunner.withUserConfiguration(ContextWithClientBean.class)
                .run((context) ->
                    assertThat(context).doesNotHaveBean(ElasticsearchIndexer.class));
        }
    }

    @Nested
    @DisplayName("Given a context with auto indexing configured")
    class ContextWithAutoIndexingConfigured {
        @BeforeEach
        void setUp() {
            System.setProperty("spring.content.elasticsearch.autoindex", "true");
        }

        @AfterEach
        void tearDown() {
            System.clearProperty("spring.content.elasticsearch.autoindex");
        }

        @Test
        void shouldLoadContext() {
            final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ElasticsearchAutoConfiguration.class));

            contextRunner.withUserConfiguration(ContextWithClientBean.class)
                .run((context) ->
                    assertThat(context).hasSingleBean(ElasticsearchIndexer.class));
        }
    }

    @Nested
    @DisplayName("Given a context that already has @EnableElasticsearchFulltextIndexing")
    class ContextThatAlreadyEnablesElasticSearchFulltextIndexing {
        @Test
        void shouldLoadContextAndNotThrowBeanDefinitionOverrideException() {
            final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ElasticsearchAutoConfiguration.class));

            contextRunner.withUserConfiguration(ContextWithEnablement.class)
                .run((context) ->
                    assertThat(context).hasSingleBean(ElasticsearchClient.class));
        }
    }
}
