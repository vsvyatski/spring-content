package org.springframework.content.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.content.commons.renditions.RenditionProvider;
import org.springframework.content.jpa.config.EnableJpaStores;
import org.springframework.content.test.support.EmbeddedJpa;
import org.springframework.content.test.support.TestSupportPackages;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.io.Resource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Configuration
@PropertySource("classpath:spring-content-it-jpa-default.properties")
@EnableElasticsearchFulltextIndexing
@EnableJpaRepositories(basePackages = TestSupportPackages.FULLTEXT)
@EnableJpaStores(basePackages = TestSupportPackages.FULLTEXT)
public class ElasticsearchConfig {

    @Value("/org/springframework/content/jpa/schema-drop-hsqldb.sql")
    private Resource dropRepositoryTables;
    @Value("/org/springframework/content/jpa/schema-hsqldb.sql")
    private Resource dataRepositorySchema;

    @Bean
    public ConversionService conversionService() {
        return new DefaultFormattingConversionService();
    }

    @Bean
    public ElasticsearchClient client() {
        return ElasticsearchTestContainer.client();
    }

    @Bean
    public RenditionProvider resourceConverter() {
        return new RenditionProvider() {

            @Override
            public String consumes() {
                return "image/png";
            }

            @Override
            public String[] produces() {
                return new String[]{"text/plain"};
            }

            @Override
            public InputStream convert(InputStream fromInputSource, String toMimeType) {
                return new ByteArrayInputStream("It was the best of times, the worst of times, it was the age of wisdom, it was the age of foolishness".getBytes());
            }
        };
    }

    @Bean
    public DataSource dataSource() {
        return EmbeddedJpa.embeddedDataSource(EmbeddedDatabaseType.H2);
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        return EmbeddedJpa.entityManagerFactory(dataSource(), Database.H2, TestSupportPackages.FULLTEXT);
    }

    @Bean
    public PlatformTransactionManager transactionManager() {
        return EmbeddedJpa.transactionManager(entityManagerFactory().getObject());
    }

    @Bean
    DataSourceInitializer datasourceInitializer(DataSource dataSource) {
        return EmbeddedJpa.schemaInitializer(dataSource, dropRepositoryTables, dataRepositorySchema);
    }
}
