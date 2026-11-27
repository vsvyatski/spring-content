package org.springframework.content.solr.it;

import jakarta.persistence.EntityManagerFactory;
import org.apache.solr.client.solrj.SolrClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.io.Resource;
import org.springframework.content.jpa.config.EnableJpaStores;
import org.springframework.content.solr.AttributeProvider;
import org.springframework.content.solr.EnableFullTextSolrIndexing;
import org.springframework.content.solr.FilterQueryProvider;
import org.springframework.content.solr.SolrTestContainer;
import org.springframework.content.test.support.EmbeddedJpa;
import org.springframework.content.test.support.TestSupportPackages;
import org.springframework.content.test.support.fulltext.FulltextJpaUuidDocumentEntity;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * JPA + Solr container wiring for {@link org.springframework.content.solr.SolrIT}.
 * Lives outside {@code org.springframework.content.solr} so it is never picked up by production
 * configuration scanning.
 *
 * @author Vladimir Svyatski
 */
@Configuration
@PropertySource("classpath:spring-content-it-jpa-default.properties")
@EnableTransactionManagement
@EnableFullTextSolrIndexing
@EnableJpaRepositories(basePackages = TestSupportPackages.FULLTEXT)
@EnableJpaStores(basePackages = TestSupportPackages.FULLTEXT)
public class SolrItApplicationConfig {

    @Value("/org/springframework/content/jpa/schema-drop-hsqldb.sql")
    private Resource dropRepositoryTables;

    @Value("/org/springframework/content/jpa/schema-hsqldb.sql")
    private Resource dataRepositorySchema;

    @Bean
    public ConversionService conversionService() {
        return new DefaultFormattingConversionService();
    }

    @Bean
    public DataSource dataSource() {
        return EmbeddedJpa.embeddedDataSource(EmbeddedDatabaseType.H2);
    }

    @Bean
    static DataSourceInitializer datasourceInitializer(
        DataSource dataSource,
        @Value("/org/springframework/content/jpa/schema-drop-hsqldb.sql") Resource dropRepositoryTables,
        @Value("/org/springframework/content/jpa/schema-hsqldb.sql") Resource dataRepositorySchema) {
        return EmbeddedJpa.schemaInitializer(dataSource, dropRepositoryTables, dataRepositorySchema);
    }

    @Bean
    @DependsOn("datasourceInitializer")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        return EmbeddedJpa.entityManagerFactory(dataSource, Database.H2, TestSupportPackages.FULLTEXT);
    }

    @Bean
    @DependsOn("entityManagerFactory")
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        return EmbeddedJpa.transactionManager(entityManagerFactory);
    }

    @Bean
    public SolrClient solrClient() {
        return SolrTestContainer.getSolrClient();
    }

    @Bean
    public AttributeProvider<FulltextJpaUuidDocumentEntity> attributeProvider() {
        return entity -> {
            Map<String, String> attrs = new HashMap<>();
            attrs.put("author", entity.getAuthor());
            return attrs;
        };
    }

    private static final ThreadLocal<String[]> REQUEST_FILTERS = new ThreadLocal<>();

    public static void requestFilters(String... filters) {
        REQUEST_FILTERS.set(filters);
    }

    public static void clearRequestFilters() {
        REQUEST_FILTERS.remove();
    }

    @Bean
    public FilterQueryProvider filterQueryProvider() {
        return entityClass -> {
            String[] filters = REQUEST_FILTERS.get();
            return filters != null ? filters : new String[0];
        };
    }
}
