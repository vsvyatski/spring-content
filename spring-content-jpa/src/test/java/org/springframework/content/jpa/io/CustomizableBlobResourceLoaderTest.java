package org.springframework.content.jpa.io;

import internal.org.springframework.content.jpa.io.GenericBlobResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CustomizableBlobResourceLoaderTest {

    private CustomizableBlobResourceLoader loader;

    private Object result;

    @Nested
    class CustomizableBlobResourceLoaderCases {
        @Nested
        class GetDatabaseName {
            @BeforeEach
            void setUp() {
                loader = new CustomizableBlobResourceLoader(mock(JdbcTemplate.class), mock(PlatformTransactionManager.class));

                result = loader.getDatabaseName();
            }

            @Test
            void shouldReturnGENERIC() {
                assertThat(result.toString()).isEqualTo("GENERIC");
            }
        }

        @Nested
        class GetResource {
            @BeforeEach
            void setUp() throws SQLException {
                DataSource ds = mock(DataSource.class);
                JdbcTemplate template = new JdbcTemplate(ds);
                PlatformTransactionManager txnMgr = new DataSourceTransactionManager(ds);

                Connection conn = mock(Connection.class);
                when(ds.getConnection()).thenReturn(conn);
                Statement stmt = mock(Statement.class);
                when(conn.createStatement()).thenReturn(stmt);

                loader = new CustomizableBlobResourceLoader(template, txnMgr);

                result = loader.getResource("some-id");
            }

            @Test
            void shouldReturnGenericBlobResource() {
                assertThat(result).isInstanceOf(GenericBlobResource.class);
            }
        }

        @Nested
        class GetClassLoader {
            @BeforeEach
            void setUp() {
                loader = new CustomizableBlobResourceLoader(mock(JdbcTemplate.class), mock(PlatformTransactionManager.class));

                result = loader.getClassLoader();
            }

            @Test
            void shouldReturnClassLoader() {
                assertThat(result).isInstanceOf(ClassLoader.class);
            }
        }

        @Nested
        @DisplayName("Given a resource provider")
        class ResourceProvider {
            @Nested
            class GetResource {
                private Resource expectedResource;

                @BeforeEach
                void setUp() {
                    expectedResource = mock(Resource.class);

                    DataSource ds = mock(DataSource.class);
                    JdbcTemplate template = new JdbcTemplate(ds);
                    PlatformTransactionManager txnMgr = new DataSourceTransactionManager(ds);

                    loader = new CustomizableBlobResourceLoader(template, txnMgr, "CUSTOM_DB",
                        (l, t, txn) -> expectedResource);

                    result = loader.getResource("some-id");
                }

                @Test
                void shouldReturnResourceProvidersCustomResource() {
                    assertThat(result).isSameAs(expectedResource);
                }
            }
        }
    }
}
