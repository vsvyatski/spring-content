package org.springframework.content.solr.boot;

import internal.org.springframework.content.elasticsearch.boot.autoconfigure.ElasticsearchAutoConfiguration;
import internal.org.springframework.content.solr.boot.autoconfigure.SolrAutoConfiguration;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
public class SolrAutoConfigurationTest {

    private ApplicationContextRunner contextRunner;

    @SpringBootApplication(exclude = ElasticsearchAutoConfiguration.class)
    public static class TestConfig {
    }

    @Nested
    class Solr {
        @Nested
        class GivenAnApplicationContextWithASolrClientBeanAndSolrAutoConfiguration {
            @BeforeEach
            void setUp() {
                contextRunner = new ApplicationContextRunner()
                        .withConfiguration(AutoConfigurations.of(SolrAutoConfiguration.class));
            }

            @Test
            void shouldIncludeTheAutoconfiguredAnnotatedEventHandlerBean() {
                contextRunner.withUserConfiguration(TestConfig.class)
                        .run((context) ->
                                Assertions.assertThat(context).getBean("solrFulltextEventListener")
                                        .isNotNull());
            }
        }
    }
}
