package org.springframework.content.test.support;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * Shared embedded-JPA beans for tests. Dialect, packages, and schema scripts stay with the caller.
 *
 * @author Vladimir Svyatski
 */
public final class EmbeddedJpa {

    private EmbeddedJpa() {
    }

    public static DataSource embeddedDataSource(EmbeddedDatabaseType type) {
        return new EmbeddedDatabaseBuilder().setType(type).build();
    }

    public static DataSource driverManagerDataSource(String url, String username, String password) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        return dataSource;
    }

    public static LocalContainerEntityManagerFactoryBean entityManagerFactory(
        DataSource dataSource, Database database, String... packagesToScan) {
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        vendorAdapter.setDatabase(database);
        vendorAdapter.setGenerateDdl(true);

        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        factory.setJpaVendorAdapter(vendorAdapter);
        factory.setPackagesToScan(packagesToScan);
        factory.setDataSource(dataSource);
        return factory;
    }

    public static PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        JpaTransactionManager txManager = new JpaTransactionManager();
        txManager.setEntityManagerFactory(entityManagerFactory);
        return txManager;
    }

    public static DataSourceInitializer schemaInitializer(DataSource dataSource, Resource... scripts) {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScripts(scripts);
        populator.setIgnoreFailedDrops(true);
        DataSourceInitializer initializer = new DataSourceInitializer();
        initializer.setDataSource(dataSource);
        initializer.setDatabasePopulator(populator);
        return initializer;
    }
}
