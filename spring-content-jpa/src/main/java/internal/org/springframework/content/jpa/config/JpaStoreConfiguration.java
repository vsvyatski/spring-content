package internal.org.springframework.content.jpa.config;

import internal.org.springframework.content.jpa.io.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.content.jpa.io.BlobResourceLoader;
import org.springframework.content.jpa.io.CustomizableBlobResourceLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.List;

@Configuration
public class JpaStoreConfiguration {

    private static Log logger = LogFactory.getLog(JpaStoreConfiguration.class);

    @Bean
    public DelegatingBlobResourceLoader blobResourceLoader(DataSource ds, List<BlobResourceLoader> loaders) {
        return new DelegatingBlobResourceLoader(ds, loaders);
    }

    @Bean
    public BlobResourceLoader genericBlobResourceLoader(DataSource ds, PlatformTransactionManager txnMgr) {
        return new CustomizableBlobResourceLoader(new JdbcTemplate(ds), txnMgr, "GENERIC", (l, t, txn) -> {
            return new GenericBlobResource(l, t, txn);
        });
    }

    @Bean
    public BlobResourceLoader mysqlBlobResourceLoader(DataSource ds, PlatformTransactionManager txnMgr) {
        return new CustomizableBlobResourceLoader(new JdbcTemplate(ds), txnMgr, "MySQL", (l, t, txn) -> {
            return new MySQLBlobResource(l, t, txn);
        });
    }

    @Bean
    public BlobResourceLoader postgresBlobResourceLoader(DataSource ds, PlatformTransactionManager txnMgr) {
        return new CustomizableBlobResourceLoader(new JdbcTemplate(ds), txnMgr, "PostgreSQL", (l, t, txn) -> {
            return new PostgresBlobResource(l, t, txn);
        });
    }

    @Bean
    public BlobResourceLoader sqlServerBlobResourceLoader(DataSource ds, PlatformTransactionManager txnMgr) {
        return new CustomizableBlobResourceLoader(new JdbcTemplate(ds), txnMgr, "Microsoft SQL Server", (l, t, txn) -> {
            return new SQLServerBlobResource(l, t, txn);
        });
    }
}
