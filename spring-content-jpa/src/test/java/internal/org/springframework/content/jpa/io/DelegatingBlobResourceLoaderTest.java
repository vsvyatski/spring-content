package internal.org.springframework.content.jpa.io;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.content.jpa.io.BlobResourceLoader;
import org.springframework.content.jpa.io.CustomizableBlobResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class DelegatingBlobResourceLoaderTest {

    private DelegatingBlobResourceLoader service;

    private DataSource ds;
    private List<BlobResourceLoader> loaders;

    private BlobResourceLoader customLoader;

    private Resource resource;

    @Nested
   class DelegatingBlobResourceLoaderCases {
        @Nested
        class GetResource {
            @Nested
            @DisplayName("Given a custom blob resource loader")
            class CustomBlobResourceLoader {
                @BeforeEach
                void setUp() throws SQLException {
                    ds = mock(DataSource.class);
                    Connection conn = mock(Connection.class);
                    DatabaseMetaData metadata = mock(DatabaseMetaData.class);
                    when(ds.getConnection()).thenReturn(conn);
                    when(conn.getMetaData()).thenReturn(metadata);
                    when(metadata.getDatabaseProductName()).thenReturn("my-custom-db");

                    customLoader = mock(BlobResourceLoader.class);
                    when(customLoader.getDatabaseName()).thenReturn("my-custom-db");

                    loaders = new ArrayList<>();
                    loaders.add(customLoader);

                    service = new DelegatingBlobResourceLoader(ds, loaders);
                    resource = service.getResource("some-id");
                }

                @Test
                void shouldReturnAPostgresBlobResource() {
                    verify(customLoader).getResource(any());
                }
            }

            @Nested

            @DisplayName("Given a datasource that doesnt have a matching blob resource loader")

            class DatasourceThatDoesntHaveAMatchingBlobResourceLoader {
                @BeforeEach
                void setUp() throws SQLException {
                    ds = mock(DataSource.class);
                    Connection conn = mock(Connection.class);
                    DatabaseMetaData metadata = mock(DatabaseMetaData.class);
                    when(ds.getConnection()).thenReturn(conn);
                    when(conn.getMetaData()).thenReturn(metadata);
                    when(metadata.getDatabaseProductName()).thenReturn("SomeOtherDatabase");

                    loaders = new ArrayList<>();
                    loaders.add(new CustomizableBlobResourceLoader(
                        mock(JdbcTemplate.class),
                        mock(PlatformTransactionManager.class)));

                    service = new DelegatingBlobResourceLoader(ds, loaders);
                    resource = service.getResource("some-id");
                }

                @Test
                void shouldReturnAGenericBlobResource() {
                    assertThat(resource).isInstanceOf(GenericBlobResource.class);
                }
            }
        }
    }
}
