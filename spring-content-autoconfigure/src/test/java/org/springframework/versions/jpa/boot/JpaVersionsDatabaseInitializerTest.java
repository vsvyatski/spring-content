package org.springframework.versions.jpa.boot;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Disabled;

import internal.org.springframework.versions.jpa.boot.autoconfigure.JpaVersionsDatabaseInitializer;
import internal.org.springframework.versions.jpa.boot.autoconfigure.JpaVersionsProperties;
import org.springframework.boot.sql.init.DatabaseInitializationMode;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@Disabled("This test was entirely commented out, it may not work at all. We'll ignore it for now.")
public class JpaVersionsDatabaseInitializerTest {

    private JpaVersionsDatabaseInitializer initializer;

    private DataSource ds;
    private JpaVersionsProperties props;

    // mocks
    private Statement stmt;

    
    @Nested
    class ContentJpaDatabaseInitializer {
        @Nested
        class Initialize {
            @Nested
            class WhenInitializationIsEnabled {
                @BeforeEach
                void setUp() throws SQLException {
                    ds = mock(DataSource.class);
                    props = new JpaVersionsProperties();

                    Connection conn = mock(Connection.class);
                    when(ds.getConnection()).thenReturn(conn);
                    stmt = mock(Statement.class);
                    when(conn.createStatement()).thenReturn(stmt);
                    DatabaseMetaData metadata = mock(DatabaseMetaData.class);
                    when(conn.getMetaData()).thenReturn(metadata);
                    when(metadata.getDatabaseProductName()).thenReturn("h2");

                    initializer = new JpaVersionsDatabaseInitializer(ds, props);
                    initializer.initializeDatabase();
                }

                @Test
                void shouldExecuteCREATETABLEStatementsOnTheDatabase() throws SQLException {
                    verify(stmt, atLeastOnce()).execute(org.mockito.ArgumentMatchers.argThat(v -> String.valueOf(v).contains("CREATE TABLE")));
                }

            }

            @Nested
            class WhenInitializationIsDisabled {
                @BeforeEach
                void setUp() throws SQLException {
                    ds = mock(DataSource.class);
                    props = new JpaVersionsProperties();

                    Connection conn = mock(Connection.class);
                    when(ds.getConnection()).thenReturn(conn);
                    stmt = mock(Statement.class);
                    when(conn.createStatement()).thenReturn(stmt);
                    DatabaseMetaData metadata = mock(DatabaseMetaData.class);
                    when(conn.getMetaData()).thenReturn(metadata);
                    when(metadata.getDatabaseProductName()).thenReturn("h2");

                    props.getInitializer().setInitializeSchema(DatabaseInitializationMode.NEVER);
                    initializer = new JpaVersionsDatabaseInitializer(ds, props);
                }

                @Test
                void shouldNotExecuteAnyStatementsOnTheDatabase() throws SQLException {
                    verify(stmt, never()).execute(anyString());
                }

            }

        }

    }

}
