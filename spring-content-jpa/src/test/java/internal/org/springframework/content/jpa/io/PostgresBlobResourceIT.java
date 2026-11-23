package internal.org.springframework.content.jpa.io;

import com.github.f4b6a3.uuid.UuidCreator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.content.test.support.EmbeddedJpa;
import org.springframework.content.test.support.TestSupportData;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.EnableTransactionManagement;
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
    @EnableTransactionManagement
    public static class PostgresConfig {

        @Value("/org/springframework/content/jpa/schema-drop-postgresql.sql")
        private Resource dropStoreTables;

        @Value("/org/springframework/content/jpa/schema-postgresql.sql")
        private Resource dataStoreSchema;

        @Bean
        DataSourceInitializer datasourceInitializer(DataSource dataSource) {
            return EmbeddedJpa.schemaInitializer(dataSource, dropStoreTables, dataStoreSchema);
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
                dataSource(), Database.POSTGRESQL, getClass().getPackage().getName());
        }

        @Bean
        public PlatformTransactionManager transactionManager() {
            return EmbeddedJpa.transactionManager(entityManagerFactory().getObject());
        }
    }

    @Nested
    class PostgresBlobResourceCases {
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
                        lobId = rs.getLong(2);
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
                void shouldDeleteTheAssociatedLobResources() throws SQLException {
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
}
