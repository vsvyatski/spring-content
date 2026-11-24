package internal.org.springframework.content.jpa;

import internal.org.springframework.content.jpa.support.stores.DocumentStore;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.content.commons.io.DeletableResource;
import org.springframework.content.jpa.config.EnableJpaStores;
import org.springframework.content.test.support.EmbeddedJpa;
import org.springframework.content.test.support.TestSupportData;
import org.springframework.content.test.support.TestSupportPackages;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class StoreIT {

    /**
     * Shared by JPA storage ITs; SqlServer needs Docker and stays commented until re-enabled (#2227).
     */
    public static final Class<?>[] DATABASE_CONFIG_CLASSES = {
        H2Config.class,
        HSQLConfig.class,
        MariaDbConfig.class,
        PostgresConfig.class,
        //SqlServerConfig.class,
    };
    private static final String IT_PACKAGE = "internal.org.springframework.content.jpa";
    // for postgres (large object api operations must be in a transaction)
    private PlatformTransactionManager ptm;

    private DocumentStore store;

    private Resource r;

    private TransactionStatus status;

    public static Stream<Class<?>> databaseConfigs() {
        return Stream.of(DATABASE_CONFIG_CLASSES);
    }

    protected String getId() {
        return "/store-tests/" + TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;
    }

    private void bootStoreContext(Class<?> configClass) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(TestConfig.class);
        context.register(configClass);
        context.refresh();

        ptm = context.getBean(PlatformTransactionManager.class);
        store = context.getBean(DocumentStore.class);

        if (ptm == null) {
            ptm = mock(PlatformTransactionManager.class);
        }
    }

    @Configuration
    @EnableJpaRepositories(basePackages = {IT_PACKAGE, TestSupportPackages.JPA}, considerNestedRepositories = true)
    @EnableJpaStores(basePackages = {IT_PACKAGE, TestSupportPackages.JPA})
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
            return EmbeddedJpa.schemaInitializer(dataSource, dropRepositoryTables, dataRepositorySchema);
        }

        @Bean
        public DataSource dataSource() {
            return EmbeddedJpa.embeddedDataSource(EmbeddedDatabaseType.H2);
        }

        @Bean
        public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
            return EmbeddedJpa.entityManagerFactory(dataSource(), Database.H2, IT_PACKAGE, TestSupportPackages.JPA);
        }

        @Bean
        public PlatformTransactionManager transactionManager() {
            return EmbeddedJpa.transactionManager(entityManagerFactory().getObject());
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
            return EmbeddedJpa.schemaInitializer(dataSource, dropRepositoryTables, dataRepositorySchema);
        }

        @Bean
        public DataSource dataSource() {
            return EmbeddedJpa.embeddedDataSource(EmbeddedDatabaseType.HSQL);
        }

        @Bean
        public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
            return EmbeddedJpa.entityManagerFactory(dataSource(), Database.HSQL, IT_PACKAGE, TestSupportPackages.JPA);
        }

        @Bean
        public PlatformTransactionManager transactionManager() {
            return EmbeddedJpa.transactionManager(entityManagerFactory().getObject());
        }
    }

    @Configuration
    @EnableTransactionManagement
    public static class MariaDbConfig {

        @Value("/org/springframework/content/jpa/schema-drop-mariadb.sql")
        private Resource dropRepositoryTables;

        @Value("/org/springframework/content/jpa/schema-mariadb.sql")
        private Resource dataRepositorySchema;

        @Bean
        DataSourceInitializer datasourceInitializer(DataSource dataSource) {
            return EmbeddedJpa.schemaInitializer(dataSource, dropRepositoryTables, dataRepositorySchema);
        }

        @Bean
        public DataSource dataSource() {
            return EmbeddedJpa.driverManagerDataSource(
                "jdbc:tc:mariadb:10.11:///databasename?TC_TMPFS=/testtmpfs:rw&TC_DAEMON=true",
                "test",
                "test");
        }

        @Bean
        public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
            return EmbeddedJpa.entityManagerFactory(dataSource, Database.MYSQL, IT_PACKAGE, TestSupportPackages.JPA);
        }

        @Bean
        public PlatformTransactionManager transactionManager(
            LocalContainerEntityManagerFactoryBean entityManagerFactory) {
            return EmbeddedJpa.transactionManager(entityManagerFactory.getObject());
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
            return EmbeddedJpa.schemaInitializer(dataSource, dropRepositoryTables, dataRepositorySchema);
        }

        @Bean
        public DataSource dataSource() {
            return EmbeddedJpa.driverManagerDataSource(
                "jdbc:tc:postgresql:12:///databasename?TC_TMPFS=/testtmpfs:rw&TC_DAEMON=true",
                "test",
                "test");
        }

        @Bean
        public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
            return EmbeddedJpa.entityManagerFactory(
                dataSource(), Database.POSTGRESQL, IT_PACKAGE, TestSupportPackages.JPA);
        }

        @Bean
        public PlatformTransactionManager transactionManager() {
            return EmbeddedJpa.transactionManager(entityManagerFactory().getObject());
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
            return EmbeddedJpa.schemaInitializer(dataSource, dropRepositoryTables, dataRepositorySchema);
        }

        @Bean
        public DataSource dataSource() {
            return EmbeddedJpa.driverManagerDataSource(
                "jdbc:tc:sqlserver:///databasename?TC_TMPFS=/testtmpfs:rw&TC_DAEMON=true",
                "SA",
                "A_Str0ng_Required_Password");
        }

        @Bean
        public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
            return EmbeddedJpa.entityManagerFactory(
                dataSource(), Database.SQL_SERVER, IT_PACKAGE, TestSupportPackages.JPA);
        }

        @Bean
        public PlatformTransactionManager transactionManager() {
            return EmbeddedJpa.transactionManager(entityManagerFactory().getObject());
        }
    }

    @Nested
    class Store {
        @ParameterizedTest(name = "{0}")
        @MethodSource("internal.org.springframework.content.jpa.StoreIT#databaseConfigs")
        void shouldNotExistWhenResourceIsMissing(Class<?> configClass) {
            bootStoreContext(configClass);

            status = ptm.getTransaction(new DefaultTransactionDefinition());

            try {
                r = store.getResource(getId());
                assertThat(r.exists()).isFalse();
            } finally {
                ptm.commit(status);
            }
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("internal.org.springframework.content.jpa.StoreIT#databaseConfigs")
        void shouldStoreThatContent(Class<?> configClass) throws IOException {
            bootStoreContext(configClass);

            status = ptm.getTransaction(new DefaultTransactionDefinition());

            r = store.getResource(getId());
            try (InputStream is = TestSupportData.sampleTextStream();
                 OutputStream os = ((WritableResource) r).getOutputStream()) {
                IOUtils.copy(is, os);
            }
            try {
                assertThat(r.exists()).isTrue();

                boolean matches = false;
                InputStream expected = TestSupportData.sampleTextStream();
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
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("internal.org.springframework.content.jpa.StoreIT#databaseConfigs")
        void shouldStoreThatUpdatedContent(Class<?> configClass) throws IOException {
            bootStoreContext(configClass);

            status = ptm.getTransaction(new DefaultTransactionDefinition());

            r = store.getResource(getId());
            try (InputStream is = TestSupportData.sampleTextStream();
                 OutputStream os = ((WritableResource) r).getOutputStream()) {
                IOUtils.copy(is, os);
            }
            try (InputStream is = TestSupportData.sampleUpdatedTextStream();
                 OutputStream os = ((WritableResource) r).getOutputStream()) {
                IOUtils.copy(is, os);
            }

            try {
                assertThat(r.exists()).isTrue();

                boolean matches = false;
                InputStream expected = TestSupportData.sampleUpdatedTextStream();
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
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("internal.org.springframework.content.jpa.StoreIT#databaseConfigs")
        void shouldExistAfterWritingContent(Class<?> configClass) throws IOException {
            bootStoreContext(configClass);

            status = ptm.getTransaction(new DefaultTransactionDefinition());

            r = store.getResource(getId());
            InputStream is = TestSupportData.sampleTextStream();
            try (OutputStream os = ((WritableResource) r).getOutputStream()) {
                IOUtils.copy(is, os);
            }

            try {
                assertThat(r.exists()).isTrue();
            } finally {
                try {
                    ((DeletableResource) r).delete();
                } catch (Exception ignored) {
                }

                ptm.commit(status);
            }
        }
    }
}
