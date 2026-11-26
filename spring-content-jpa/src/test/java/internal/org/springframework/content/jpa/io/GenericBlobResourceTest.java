package internal.org.springframework.content.jpa.io;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GenericBlobResourceTest {

    private GenericBlobResource resource;

    private JdbcTemplate template;
    private PlatformTransactionManager txnMgr;

    private DataSource ds;
    private Connection conn;
    private Statement statement;
    private ResultSet rs;

    private Object result;

    @Nested
    class GenericBlobResourceCases {
        @Nested
        class Exists {
            @Nested
            @DisplayName("Given the resultset throws sql exception")
            class ResultsetThrowsSQLException {
                @BeforeEach
                void setUp() throws SQLException {
                    ds = mock(DataSource.class);
                    template = new JdbcTemplate(ds);
                    txnMgr = new DataSourceTransactionManager(ds);

                    conn = mock(Connection.class);
                    statement = mock(Statement.class);
                    rs = mock(ResultSet.class);

                    when(ds.getConnection()).thenReturn(conn);
                    when(conn.createStatement()).thenReturn(statement);
                    when(statement.executeQuery(any())).thenReturn(rs);

                    when(rs.next()).thenThrow(new SQLException("badness"));
                    resource = new GenericBlobResource("some-id", template, txnMgr);
                    result = resource.exists();
                }

                @Test
                void shouldReturnFalse() {
                    assertThat(result).isEqualTo(false);
                }
            }
        }

        @Nested
        class GetInputStream {
            @Nested
            @DisplayName("Given asql exception is thrown")
            class SQLExceptionIsThrown {
                @BeforeEach
                void setUp() throws SQLException {
                    ds = mock(DataSource.class);
                    template = new JdbcTemplate(ds);
                    txnMgr = new DataSourceTransactionManager(ds);

                    conn = mock(Connection.class);
                    statement = mock(Statement.class);
                    rs = mock(ResultSet.class);

                    when(ds.getConnection()).thenReturn(conn);
                    when(conn.createStatement()).thenReturn(statement);
                    when(statement.executeQuery(any())).thenReturn(rs);

                    when(rs.next()).thenThrow(new SQLException("badness"));
                    resource = new GenericBlobResource("some-id", template, txnMgr);
                }

                @Test
                void shouldThrowIOException() {
                    assertThatThrownBy(() -> resource.getInputStream()).isInstanceOf(IOException.class);
                }
            }
        }
    }
}
