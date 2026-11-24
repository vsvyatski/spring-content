package internal.org.springframework.content.jpa;

import internal.org.springframework.content.jpa.it.support.H2StoreItJdbcConfig;
import internal.org.springframework.content.jpa.it.support.HsqlStoreItJdbcConfig;
import internal.org.springframework.content.jpa.it.support.MariaDbStoreItJdbcConfig;
import internal.org.springframework.content.jpa.it.support.PostgresStoreItJdbcConfig;
import internal.org.springframework.content.jpa.support.stores.DocumentStore;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.content.commons.io.DeletableResource;
import org.springframework.content.jpa.config.EnableJpaStores;
import org.springframework.content.test.support.TestSupportData;
import org.springframework.content.test.support.TestSupportPackages;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

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
        H2StoreItJdbcConfig.class,
        HsqlStoreItJdbcConfig.class,
        MariaDbStoreItJdbcConfig.class,
        PostgresStoreItJdbcConfig.class,
        //SqlServerStoreItJdbcConfig.class,
    };

    private static final String IT_PACKAGE = "internal.org.springframework.content.jpa";

    // for Postgres (large object api operations must be in a transaction)
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
