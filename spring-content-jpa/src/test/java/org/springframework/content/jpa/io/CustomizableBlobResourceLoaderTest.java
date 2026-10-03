package org.springframework.content.jpa.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import static org.assertj.core.api.Assertions.assertThat;

import internal.org.springframework.content.jpa.io.GenericBlobResource;
import org.springframework.content.jpa.io.CustomizableBlobResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CustomizableBlobResourceLoaderTest {

	private CustomizableBlobResourceLoader loader;

	private JdbcTemplate template;
	private PlatformTransactionManager txnMgr;

	private DataSource ds;
	private Connection conn;
	private Statement stmt;

	private Resource customDBResource;

	private Object result;

	
    @Nested
    class CustomizableBlobResourceLoaderCases {
        @Nested
        class GetDatabaseName {
            @BeforeEach
            void setUp() throws Throwable {
                loader = new CustomizableBlobResourceLoader(template, txnMgr);

                result = loader.getDatabaseName();

            }

            @Test
            void shouldReturnGENERIC() throws Throwable {
                assertThat(result.toString()).isEqualTo("GENERIC");

            }

        }

        @Nested
        class GetResource {
            @BeforeEach
            void setUp() throws Throwable {
                ds = mock(DataSource.class);
                template = new JdbcTemplate(ds);
                txnMgr = new DataSourceTransactionManager(ds);

                conn = mock(Connection.class);
                when(ds.getConnection()).thenReturn(conn);
                stmt = mock(Statement.class);
                when(conn.createStatement()).thenReturn(stmt);

                loader = new CustomizableBlobResourceLoader(template, txnMgr);

                result = loader.getResource("some-id");

            }

            @Test
            void shouldReturnAGenericBlobResource() throws Throwable {
                assertThat(result).isInstanceOf(GenericBlobResource.class);

            }

        }

        @Nested
        class GetClassLoader {
            @BeforeEach
            void setUp() throws Throwable {
                loader = new CustomizableBlobResourceLoader(template, txnMgr);

                result = loader.getClassLoader();

            }

            @Test
            void shouldReturnAClassLoader() throws Throwable {
                assertThat(result).isInstanceOf(ClassLoader.class);

            }

        }

        @Nested
        class GivenAResourceProvider {
            @Nested
            class GetResource {
                @BeforeEach
                void setUp() throws Throwable {
                    customDBResource = mock(Resource.class);

                    ds = mock(DataSource.class);
                    template = new JdbcTemplate(ds);
                    txnMgr = new DataSourceTransactionManager(ds);

                    loader = new CustomizableBlobResourceLoader(template, txnMgr);

                    loader = new CustomizableBlobResourceLoader(template, txnMgr, "CUSTOM_DB", (l, t, txn) -> { return customDBResource; });

                    result = loader.getResource("some-id");

                }

                @Test
                void shouldReturnTheResourceProvidersCustomResource() throws Throwable {
                    assertThat(result).isEqualTo(customDBResource);

                }

            }

        }

    }

}
