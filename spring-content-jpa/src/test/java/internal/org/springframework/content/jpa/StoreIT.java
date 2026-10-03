package internal.org.springframework.content.jpa;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestFactory;
import static org.assertj.core.api.Assertions.assertThat;

import internal.org.springframework.content.jpa.testsupport.stores.DocumentStore;
import net.bytebuddy.utility.RandomString;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.content.commons.io.DeletableResource;
import org.springframework.content.jpa.config.EnableJpaStores;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
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
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import javax.sql.DataSource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.TimeZone;

import static org.mockito.Mockito.mock;

public class StoreIT {

    private static final Class<?>[] CONFIG_CLASSES = new Class[]{
            H2Config.class,
            HSQLConfig.class,
            MySqlConfig.class,
            PostgresConfig.class
//			SqlServerConfig.class,
//			OracleConfig.class
    };

    private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

    // for postgres (large object api operations must be in a transaction)
    private PlatformTransactionManager ptm;

    private DocumentStore store;

    private Resource r;
    private Exception e;

    private TransactionStatus status;

    @Nested
    class Store {
        @TestFactory
        java.util.stream.Stream<org.junit.jupiter.api.DynamicNode> generatedCases() {
            java.util.List<org.junit.jupiter.api.DynamicNode> tests = new java.util.ArrayList<>();
            for (Class<?> configClass : CONFIG_CLASSES) {
                tests.add(org.junit.jupiter.api.DynamicTest.dynamicTest("\"should not exist\"", () -> {
                    context = new AnnotationConfigApplicationContext();
                    context.register(TestConfig.class);
                    context.register(configClass);
                    context.refresh();

                    ptm = context.getBean(PlatformTransactionManager.class);
                    store = context.getBean(DocumentStore.class);

                    if (ptm == null) {
                        ptm = mock(PlatformTransactionManager.class);
                    }

                    if (ptm == null) {
                        ptm = mock(PlatformTransactionManager.class);
                    }

                    status = ptm.getTransaction(new DefaultTransactionDefinition());

                    try {
                        assertThat(r.exists()).isFalse();
                    } finally {
                        ptm.commit(status);
                    }
                }));
                tests.add(org.junit.jupiter.api.DynamicTest.dynamicTest("\"should store that content\"", () -> {
                    context = new AnnotationConfigApplicationContext();
                    context.register(TestConfig.class);
                    context.register(configClass);
                    context.refresh();

                    ptm = context.getBean(PlatformTransactionManager.class);
                    store = context.getBean(DocumentStore.class);

                    if (ptm == null) {
                        ptm = mock(PlatformTransactionManager.class);
                    }

                    if (ptm == null) {
                        ptm = mock(PlatformTransactionManager.class);
                    }

                    status = ptm.getTransaction(new DefaultTransactionDefinition());

                    r = store.getResource(getId());
                    try {
                        try {
                            assertThat(r.exists()).isTrue();
                        } catch (Throwable t) {
                            t.printStackTrace(System.err);

                            throw t;
                        }

                        boolean matches = false;
                        InputStream expected = new ByteArrayInputStream("Hello Spring Content World!".getBytes());
                        InputStream actual = null;
                        try {
                            actual = r.getInputStream();
                            matches = IOUtils.contentEquals(expected, actual);
                        } catch (IOException ignored) {
                        } finally {
                            IOUtils.closeQuietly(expected);
                            IOUtils.closeQuietly(actual);
                        }
                        assertThat(matches).isTrue();

                    } finally {
                        ptm.commit(status);
                    }
                }));
                tests.add(org.junit.jupiter.api.DynamicTest.dynamicTest("\"should store that updated content\"", () -> {
                    context = new AnnotationConfigApplicationContext();
                    context.register(TestConfig.class);
                    context.register(configClass);
                    context.refresh();

                    ptm = context.getBean(PlatformTransactionManager.class);
                    store = context.getBean(DocumentStore.class);

                    if (ptm == null) {
                        ptm = mock(PlatformTransactionManager.class);
                    }

                    if (ptm == null) {
                        ptm = mock(PlatformTransactionManager.class);
                    }

                    status = ptm.getTransaction(new DefaultTransactionDefinition());

                    r = store.getResource(getId());
                    InputStream is = new ByteArrayInputStream("Hello Spring Content World!".getBytes());
                    try (OutputStream os = ((WritableResource) r).getOutputStream()) {
                        IOUtils.copy(is, os);
                    }

                    try {
                        assertThat(r.exists()).isTrue();

                        boolean matches = false;
                        InputStream expected = new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes());
                        InputStream actual = null;
                        try {
                            actual = r.getInputStream();
                            matches = IOUtils.contentEquals(expected, actual);
                        } catch (IOException ignored) {
                        } finally {
                            IOUtils.closeQuietly(expected);
                            IOUtils.closeQuietly(actual);
                        }
                        assertThat(matches).isTrue();

                    } finally {
                        try {
                            ((DeletableResource) r).delete();
                        } catch (Exception ignored) {
                        }

                        ptm.commit(status);
                    }
                }));
                tests.add(org.junit.jupiter.api.DynamicTest.dynamicTest("\"should not exist\"", () -> {
                    context = new AnnotationConfigApplicationContext();
                    context.register(TestConfig.class);
                    context.register(configClass);
                    context.refresh();

                    ptm = context.getBean(PlatformTransactionManager.class);
                    store = context.getBean(DocumentStore.class);

                    if (ptm == null) {
                        ptm = mock(PlatformTransactionManager.class);
                    }

                    if (ptm == null) {
                        ptm = mock(PlatformTransactionManager.class);
                    }

                    status = ptm.getTransaction(new DefaultTransactionDefinition());

                    r = store.getResource(getId());
                    InputStream is = new ByteArrayInputStream("Hello Spring Content World!".getBytes());
                    try (OutputStream os = ((WritableResource) r).getOutputStream()) {
                        IOUtils.copy(is, os);
                    }

                    try {
                        assertThat(e).isNull();
                    } finally {
                        try {
                            ((DeletableResource) r).delete();
                        } catch (Exception ignored) {
                        }

                        ptm.commit(status);
                    }
                }));
            }
            return tests.stream();
        }

    }

    public static String getContextName(Class<?> configClass) {
        return configClass.getSimpleName().replaceAll("Config", "");
    }

    protected String getId() {
        RandomString random = new RandomString(5);
        return "/store-tests/" + random.nextString();
    }

    @Configuration
    @EnableJpaRepositories(considerNestedRepositories = true)
    @EnableJpaStores
    public static class TestConfig {
    }

    @Configuration
    @EnableTransactionManagement
    public static class H2Config {

        @Value("/org/springframework/content/jpa/schema-drop-h2.sql")
        private Resource dropRepositoryTables;

        @Value("/org/springframework/content/jpa/schema-h2.sql")
        private Resource dataRepositorySchema;

        @Bean
        DataSourceInitializer datasourceInitializer(DataSource dataSource) {
            ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();

            databasePopulator.addScript(dropRepositoryTables);
            databasePopulator.addScript(dataRepositorySchema);
            databasePopulator.setIgnoreFailedDrops(true);

            DataSourceInitializer initializer = new DataSourceInitializer();
            initializer.setDataSource(dataSource);
            initializer.setDatabasePopulator(databasePopulator);

            return initializer;
        }

        @Bean
        public DataSource dataSource() {
            EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
            return builder.setType(EmbeddedDatabaseType.H2).build();
        }

        @Bean
        public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
            HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
            vendorAdapter.setDatabase(Database.H2);
            vendorAdapter.setGenerateDdl(true);

            LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
            factory.setJpaVendorAdapter(vendorAdapter);
            factory.setPackagesToScan(getClass().getPackage().getName());
            factory.setDataSource(dataSource());

            return factory;
        }

        @Bean
        public PlatformTransactionManager transactionManager() {
            JpaTransactionManager txManager = new JpaTransactionManager();
            txManager.setEntityManagerFactory(entityManagerFactory().getObject());
            return txManager;
        }
    }

    @Configuration
    @EnableTransactionManagement
    public static class HSQLConfig {

        @Value("/org/springframework/content/jpa/schema-drop-hsqldb.sql")
        private Resource dropRepositoryTables;

        @Value("/org/springframework/content/jpa/schema-hsqldb.sql")
        private Resource dataRepositorySchema;

        @Bean
        DataSourceInitializer datasourceInitializer(DataSource dataSource) {
            ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();

            databasePopulator.addScript(dropRepositoryTables);
            databasePopulator.addScript(dataRepositorySchema);
            databasePopulator.setIgnoreFailedDrops(true);

            DataSourceInitializer initializer = new DataSourceInitializer();
            initializer.setDataSource(dataSource);
            initializer.setDatabasePopulator(databasePopulator);

            return initializer;
        }

        @Bean
        public DataSource dataSource() {
            EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
            return builder.setType(EmbeddedDatabaseType.HSQL).build();
        }

        @Bean
        public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
            HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
            vendorAdapter.setDatabase(Database.HSQL);
            vendorAdapter.setGenerateDdl(true);

            LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
            factory.setJpaVendorAdapter(vendorAdapter);
            factory.setPackagesToScan(getClass().getPackage().getName());
            factory.setDataSource(dataSource());

            return factory;
        }

        @Bean
        public PlatformTransactionManager transactionManager() {
            JpaTransactionManager txManager = new JpaTransactionManager();
            txManager.setEntityManagerFactory(entityManagerFactory().getObject());
            return txManager;
        }
    }

    @Configuration
    @EnableTransactionManagement
    public static class MySqlConfig {

        @Value("/org/springframework/content/jpa/schema-drop-mysql.sql")
        private Resource dropRepositoryTables;

        @Value("/org/springframework/content/jpa/schema-mysql.sql")
        private Resource dataRepositorySchema;

        @Bean
        DataSourceInitializer datasourceInitializer(DataSource dataSource) {
            ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();

            databasePopulator.addScript(dropRepositoryTables);
            databasePopulator.addScript(dataRepositorySchema);
            databasePopulator.setIgnoreFailedDrops(true);

            DataSourceInitializer initializer = new DataSourceInitializer();
            initializer.setDataSource(dataSource);
            initializer.setDatabasePopulator(databasePopulator);

            return initializer;
        }

        @Bean
        public DataSource dataSource() {
            DriverManagerDataSource ds = new DriverManagerDataSource();
            ds.setUrl("jdbc:tc:mysql:5.7.34:///databasename?TC_TMPFS=/testtmpfs:rw&TC_DAEMON=true&emulateLocators=true");
            ds.setUsername("test");
            ds.setPassword("test");
            return ds;
        }

        @Bean
        public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
            HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
            vendorAdapter.setDatabase(Database.MYSQL);
            vendorAdapter.setGenerateDdl(true);

            LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
            factory.setJpaVendorAdapter(vendorAdapter);
            factory.setPackagesToScan(getClass().getPackage().getName());
            factory.setDataSource(dataSource);

            factory.setJpaVendorAdapter(vendorAdapter);
            HashMap<String, Object> properties = new HashMap<>();
            factory.setJpaPropertyMap(properties);

            return factory;
        }

        @Bean
        public PlatformTransactionManager transactionManager(
                LocalContainerEntityManagerFactoryBean entityManagerFactory) {

            JpaTransactionManager txManager = new JpaTransactionManager();
            txManager.setEntityManagerFactory(entityManagerFactory.getObject());
            return txManager;
        }
    }

    @Configuration
    @EnableTransactionManagement
    public static class PostgresConfig {

        @Value("/org/springframework/content/jpa/schema-drop-postgresql.sql")
        private Resource dropRepositoryTables;

        @Value("/org/springframework/content/jpa/schema-postgresql.sql")
        private Resource dataRepositorySchema;

        @Bean
        DataSourceInitializer datasourceInitializer(DataSource dataSource) {
            ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();

            databasePopulator.addScript(dropRepositoryTables);
            databasePopulator.addScript(dataRepositorySchema);
            databasePopulator.setIgnoreFailedDrops(true);

            DataSourceInitializer initializer = new DataSourceInitializer();
            initializer.setDataSource(dataSource);
            initializer.setDatabasePopulator(databasePopulator);

            return initializer;
        }

        @Bean
        public DataSource dataSource() {
            DriverManagerDataSource ds = new DriverManagerDataSource();
            ds.setUrl("jdbc:tc:postgresql:12:///databasename?TC_TMPFS=/testtmpfs:rw&TC_DAEMON=true");
            ds.setUsername("test");
            ds.setPassword("test");
            return ds;
        }

        @Bean
        public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
            HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
            vendorAdapter.setDatabase(Database.POSTGRESQL);
            vendorAdapter.setGenerateDdl(true);

            LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
            factory.setJpaVendorAdapter(vendorAdapter);
            factory.setPackagesToScan(getClass().getPackage().getName());
            factory.setDataSource(dataSource());

            return factory;
        }

        @Bean
        public PlatformTransactionManager transactionManager() {
            JpaTransactionManager txManager = new JpaTransactionManager();
            txManager.setEntityManagerFactory(entityManagerFactory().getObject());
            return txManager;
        }
    }

    @Configuration
    @EnableTransactionManagement
    public static class SqlServerConfig {

        @Value("/org/springframework/content/jpa/schema-drop-sqlserver.sql")
        private Resource dropRepositoryTables;

        @Value("/org/springframework/content/jpa/schema-sqlserver.sql")
        private Resource dataRepositorySchema;

        @Bean
        DataSourceInitializer datasourceInitializer(DataSource dataSource) {
            ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();

            databasePopulator.addScript(dropRepositoryTables);
            databasePopulator.addScript(dataRepositorySchema);
            databasePopulator.setIgnoreFailedDrops(true);

            DataSourceInitializer initializer = new DataSourceInitializer();
            initializer.setDataSource(dataSource);
            initializer.setDatabasePopulator(databasePopulator);

            return initializer;
        }

        @Bean
        public DataSource dataSource() {
            DriverManagerDataSource ds = new DriverManagerDataSource();
            ds.setUrl("jdbc:tc:sqlserver:///databasename?TC_TMPFS=/testtmpfs:rw&TC_DAEMON=true");
            ds.setUsername("SA");
            ds.setPassword("A_Str0ng_Required_Password");
            return ds;
        }

        @Bean
        public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
            HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
            vendorAdapter.setDatabase(Database.SQL_SERVER);
            vendorAdapter.setGenerateDdl(true);

            LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
            factory.setJpaVendorAdapter(vendorAdapter);
            factory.setPackagesToScan(getClass().getPackage().getName());
            factory.setDataSource(dataSource());

            return factory;
        }

        @Bean
        public PlatformTransactionManager transactionManager() {
            JpaTransactionManager txManager = new JpaTransactionManager();
            txManager.setEntityManagerFactory(entityManagerFactory().getObject());
            return txManager;
        }
    }

    @Configuration
    @EnableTransactionManagement
    public static class OracleConfig {

        @Value("/org/springframework/content/jpa/schema-drop-oracle.sql")
        private Resource dropRepositoryTables;

        @Value("/org/springframework/content/jpa/schema-oracle.sql")
        private Resource dataRepositorySchema;

        @Bean
        DataSourceInitializer datasourceInitializer(DataSource dataSource) {
            ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();

            databasePopulator.addScript(dropRepositoryTables);
            databasePopulator.addScript(dataRepositorySchema);
            databasePopulator.setIgnoreFailedDrops(true);

            DataSourceInitializer initializer = new DataSourceInitializer();
            initializer.setDataSource(dataSource);
            initializer.setDatabasePopulator(databasePopulator);

            return initializer;
        }

        @Bean
        public DataSource dataSource() {
            // Timezone is not set in GitHub containers, need this for connections to work
            TimeZone.setDefault(TimeZone.getTimeZone("GMT"));

            DriverManagerDataSource ds = new DriverManagerDataSource();
            ds.setUrl("jdbc:tc:oracle:///databasename?TC_TMPFS=/testtmpfs:rw?TC_DAEMON=true");
            ds.setUsername("system");
            ds.setPassword("oracle");
            return ds;
        }

        @Bean
        public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
            HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
            vendorAdapter.setDatabase(Database.ORACLE);
            vendorAdapter.setGenerateDdl(true);

            LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
            factory.setJpaVendorAdapter(vendorAdapter);
            factory.setPackagesToScan(getClass().getPackage().getName());
            factory.setDataSource(dataSource);

            return factory;
        }

        @Bean
        public PlatformTransactionManager transactionManager(
                LocalContainerEntityManagerFactoryBean entityManagerFactory) {

            JpaTransactionManager txManager = new JpaTransactionManager();
            txManager.setEntityManagerFactory(entityManagerFactory.getObject());
            return txManager;
        }
    }
}
