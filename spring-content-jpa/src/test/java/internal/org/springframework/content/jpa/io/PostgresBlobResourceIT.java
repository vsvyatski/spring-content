package internal.org.springframework.content.jpa.io;

import org.junit.jupiter.api.extension.ExtendWith;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.github.f4b6a3.uuid.UuidCreator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.context.ContextConfiguration;
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

@ContextConfiguration(classes = PostgresBlobResourceIT.PostgresConfig.class)
@ExtendWith(SpringExtension.class)
public class PostgresBlobResourceIT {

    @Autowired
    private DataSource ds;

    @Autowired
    private PlatformTransactionManager txn;

    private JdbcTemplate template;

    private String entityId = null;
    private long lobId;

    private PostgresBlobResource r = null;

    
    @Nested
    class PostgresBlobResourceCases {
        @Nested
        class GivenThereIsContent {
            @Nested
            class WhenTheContentIsDeleted {
                @BeforeEach
                void setUp() throws IOException, SQLException {
                    entityId = UuidCreator.getTimeOrdered().toString();
                    template = new JdbcTemplate(ds);

                    r = new PostgresBlobResource(entityId, template, txn);

                    DataSource ds = PostgresBlobResourceIT.this.template.getDataSource();
                    assert ds != null;
                    Connection conn = DataSourceUtils.getConnection(ds);

                    TransactionStatus status = txn.getTransaction(new DefaultTransactionDefinition());

                    try (OutputStream os = r.getOutputStream()) {
                        os.write("Hello Spring Content World!".getBytes());
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

    @Configuration
    @EnableTransactionManagement
    public static class PostgresConfig {

        @Value("/org/springframework/content/jpa/schema-drop-postgresql.sql")
        private Resource dropStoreTables;

        @Value("/org/springframework/content/jpa/schema-postgresql.sql")
        private Resource dataStoreSchema;

        @Bean
        DataSourceInitializer datasourceInitializer(DataSource dataSource) {
            ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();

            databasePopulator.addScript(dropStoreTables);
            databasePopulator.addScript(dataStoreSchema);
            databasePopulator.setIgnoreFailedDrops(true);

            DataSourceInitializer initializer = new DataSourceInitializer();
            initializer.setDataSource(dataSource);
            initializer.setDatabasePopulator(databasePopulator);

            return initializer;
        }

        @Bean
        public DataSource dataSource() {
            DriverManagerDataSource ds = new DriverManagerDataSource();
            ds.setUrl("jdbc:tc:postgresql:12:///databasename?TC_TMPFS=/testtmpfs:rw&TC_DAEMON=true");
            ds.setUsername("test");
            ds.setPassword("test");
            return ds;
        }

        @Bean
        public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
            HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
            vendorAdapter.setDatabase(Database.POSTGRESQL);
            vendorAdapter.setGenerateDdl(true);

            LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
            factory.setJpaVendorAdapter(vendorAdapter);
            factory.setPackagesToScan(getClass().getPackage().getName());
            factory.setDataSource(dataSource());

            return factory;
        }

        @Bean
        public PlatformTransactionManager transactionManager() {
            JpaTransactionManager txManager = new JpaTransactionManager();
            txManager.setEntityManagerFactory(entityManagerFactory().getObject());
            return txManager;
        }
    }

}
