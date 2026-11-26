package internal.org.springframework.content.jpa.io;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.NonNull;
import org.postgresql.PGConnection;
import org.postgresql.largeobject.LargeObject;
import org.postgresql.largeobject.LargeObjectManager;
import org.springframework.content.jpa.io.AbstractBlobResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import javax.sql.DataSource;
import java.io.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

import static java.lang.String.format;

public class PostgresBlobResource extends AbstractBlobResource {

    private static final Log logger = LogFactory.getLog(PostgresBlobResource.class);

    public PostgresBlobResource(Object id, JdbcTemplate template, PlatformTransactionManager txnMgr) {
        super(id, template, txnMgr);
    }

    @Override
    public @NonNull InputStream getInputStream() throws IOException {
        final Object blobId = this.getId();
        String sql = getSelectBlobSQL(blobId);

        TransactionStatus status = getTransactionManager().getTransaction(new DefaultTransactionDefinition());

        DataSource ds = Objects.requireNonNull(getTemplate().getDataSource(), "JdbcTemplate DataSource");
        Connection conn = DataSourceUtils.getConnection(ds);

        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            if (!rs.next()) {
                releaseQueryResources(rs, stmt, conn, ds, blobId);
                rollbackOpenTransaction(status);
                throw new FileNotFoundException(format("Blob resource %s not found", blobId));
            }

            LargeObjectManager manager = conn.unwrap(PGConnection.class).getLargeObjectAPI();
            long oid = rs.getLong("content");
            LargeObject largeObject = manager.open(oid, LargeObjectManager.READ);
            InputStream is = largeObject.getInputStream(-1);
            return new ClosingInputStream(blobId, is, rs, stmt, status, getTransactionManager(), conn, ds, largeObject);
        } catch (SQLException e) {
            DataSourceUtils.releaseConnection(conn, ds);
            rollbackOpenTransaction(status);
            logger.error(format("getting content %s", blobId), e);
            throw new IOException(format("getting content %s", blobId), e);
        }
    }

    @Override
    public @NonNull OutputStream getOutputStream() throws IOException {
        return new BufferedOutputStream(new PostgresBlobResourceOutputStream(this, getTemplate()), 10);
    }

    @Override
    public void delete() throws IOException {
        final Object blobId = this.getId();
        String sql = getSelectBlobSQL(blobId);

        DataSource ds = Objects.requireNonNull(getTemplate().getDataSource(), "JdbcTemplate DataSource");
        Connection conn = DataSourceUtils.getConnection(ds);

        try {
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
                if (!rs.next()) {
                    return;
                }

                LargeObjectManager manager = conn.unwrap(PGConnection.class).getLargeObjectAPI();
                manager.delete(rs.getLong("content"));
            }
            super.delete();
        } catch (SQLException e) {
            logger.error(format("deleting content %s", blobId), e);
            throw new IOException(format("deleting content %s", blobId), e);
        } finally {
            DataSourceUtils.releaseConnection(conn, ds);
        }
    }

    private static void releaseQueryResources(ResultSet rs, Statement stmt, Connection conn, DataSource ds,
                                              Object blobId) {
        try {
            rs.close();
            stmt.close();
            DataSourceUtils.releaseConnection(conn, ds);
        } catch (SQLException sqle) {
            logger.debug(format("failed to release database connection while fetching content %s", blobId), sqle);
        }
    }

    private void rollbackOpenTransaction(TransactionStatus status) {
        if (status != null && !status.isCompleted()) {
            getTransactionManager().rollback(status);
        }
    }
}
