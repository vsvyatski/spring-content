package internal.org.springframework.content.jpa.io;

import com.github.f4b6a3.uuid.UuidCreator;
import internal.org.springframework.content.jpa.it.support.PostgresStoreItJdbcConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.test.support.TestSupportData;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

@ContextConfiguration(classes = PostgresBlobResourceIT.PostgresConfig.class)
@ExtendWith(SpringExtension.class)
public class PostgresBlobResourceIT {

    @Autowired
    private DataSource ds;

    @Autowired
    private PlatformTransactionManager txn;

    private JdbcTemplate template;

    private long lobId;

    @Configuration
    public static class PostgresConfig extends PostgresStoreItJdbcConfig {

        @Override
        protected String[] entityPackagesToScan() {
            return new String[]{getClass().getPackage().getName()};
        }
    }

    @Nested
    @DisplayName("Given there is content")
    class Content {
        @Nested
        @DisplayName("When the content is deleted")
        class ContentIsDeleted {
            @BeforeEach
            void setUp() throws IOException, SQLException {
                String entityId = UuidCreator.getTimeOrdered().toString();
                template = new JdbcTemplate(ds);

                PostgresBlobResource r = new PostgresBlobResource(entityId, template, txn);

                DataSource ds = PostgresBlobResourceIT.this.template.getDataSource();
                assert ds != null;
                Connection conn = DataSourceUtils.getConnection(ds);

                TransactionStatus status = txn.getTransaction(new DefaultTransactionDefinition());

                try (OutputStream os = r.getOutputStream()) {
                    os.write(TestSupportData.sampleTextBytes());
                }

                txn.commit(status);

                // assert associated lob resource exist
                {
                    String sql = "SELECT id, content FROM BLOBS WHERE id='" + entityId + "'";

                    Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(sql);
                    assertThat(rs.next()).isTrue();
                    lobId = rs.getLong("content");
                    rs.close();
                    stmt.close();

                    sql = "SELECT * from pg_largeobject where loid = " + lobId;
                    stmt = conn.createStatement();
                    rs = stmt.executeQuery(sql);
                    assertThat(rs.next()).isTrue();
                    rs.close();
                    stmt.close();

                    sql = "SELECT * from pg_largeobject_metadata where oid = " + lobId;
                    stmt = conn.createStatement();
                    rs = stmt.executeQuery(sql);
                    assertThat(rs.next()).isTrue();
                    rs.close();
                    stmt.close();
                }

                r.delete();
            }

            @Test
            void shouldDeleteAssociatedLobResources() throws SQLException {
                DataSource ds = PostgresBlobResourceIT.this.template.getDataSource();
                assert ds != null;
                Connection conn = DataSourceUtils.getConnection(ds);

                String sql = "SELECT * from pg_largeobject where loid = " + lobId;
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql);
                assertThat(rs.next()).isFalse();
                rs.close();
                stmt.close();

                sql = "SELECT * from pg_largeobject_metadata where oid = " + lobId;
                stmt = conn.createStatement();
                rs = stmt.executeQuery(sql);
                assertThat(rs.next()).isFalse();
                rs.close();
                stmt.close();
            }
        }
    }
}
