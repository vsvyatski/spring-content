package org.springframework.content.solr;

import org.apache.solr.client.solrj.SolrClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.content.jpa.config.EnableJpaStores;
import org.springframework.content.test.support.EmbeddedJpa;
import org.springframework.content.test.support.TestSupportPackages;
import org.springframework.content.test.support.fulltext.FulltextJpaUuidDocumentEntity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableFullTextSolrIndexing
@EnableJpaRepositories(basePackages = TestSupportPackages.FULLTEXT)
@EnableJpaStores(basePackages = TestSupportPackages.FULLTEXT)
public class SolrITConfig {

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

    @Bean
    public FilterQueryProvider filterQueryProvider() {
        return entityClass -> new String[]{"author:\"author@email.com\""};
    }
}
