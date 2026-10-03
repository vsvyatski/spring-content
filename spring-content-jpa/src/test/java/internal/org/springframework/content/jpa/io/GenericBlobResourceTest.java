package internal.org.springframework.content.jpa.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

public class GenericBlobResourceTest {

    private GenericBlobResource resource;

    private String id;
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
            class GivenTheResultsetThrowsSQLException {
                @BeforeEach
                void setUp() throws Throwable {
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
                    resource = new GenericBlobResource(id, template, txnMgr);
                    result = resource.exists();

                }

                @Test
                void shouldReturnFalse() throws Throwable {
                    assertThat(result).isEqualTo(false);
                }

            }

        }

        @Nested
        class GetInputStream {
            @Nested
            class GivenASQLExceptionIsThrown {
                @BeforeEach
                void setUp() throws Throwable {
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
                    resource = new GenericBlobResource(id, template, txnMgr);
                    result = resource.getInputStream();

                }

                @Test
                void shouldReturnNull() throws Throwable {
                    assertThat(result).isNull();
                }

            }

        }

    }

}
