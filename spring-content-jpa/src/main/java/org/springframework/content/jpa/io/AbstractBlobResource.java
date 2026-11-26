package org.springframework.content.jpa.io;

import internal.org.springframework.content.jpa.io.BlobResourceOutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.core.io.AbstractResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import javax.sql.DataSource;
import java.io.*;
import java.sql.*;
import java.util.Objects;

import static java.lang.String.format;

public abstract class AbstractBlobResource extends AbstractResource implements BlobResource {

    private static final Log logger = LogFactory.getLog(AbstractBlobResource.class);

    private final Object id;
    private final JdbcTemplate template;
    private final PlatformTransactionManager txnMgr;

    public AbstractBlobResource(Object id, JdbcTemplate template, PlatformTransactionManager txnMgr) {
        this.id = Objects.requireNonNull(id, "id");
        this.template = Objects.requireNonNull(template, "template");
        this.txnMgr = Objects.requireNonNull(txnMgr, "txnMgr");
    }

    @Override
    public Object getId() {
        return id;
    }

    protected JdbcTemplate getTemplate() {
        return template;
    }

    protected PlatformTransactionManager getTransactionManager() {
        return txnMgr;
    }

    @Override
    public @NonNull String getDescription() {
        return "JPA blob resource [" + id + "]";
    }

    @Override
    public @NonNull OutputStream getOutputStream() throws IOException {
        return new BufferedOutputStream(new BlobResourceOutputStream(this, template), 10);
    }

    @Override
    public boolean exists() {
        final Object blobId = this.id;
        String sql = "SELECT COUNT(id) FROM BLOBS WHERE id='" + blobId + "'";

        return Boolean.TRUE.equals(this.template.query(sql, rs -> {
            try {
                if (rs.next()) {
                    return rs.getInt(1) == 1;
                }
            } catch (SQLException sqle) {
                logger.warn(format("checking existence of blob resource %s", blobId), sqle);
            }
            return false;
        }));
    }

    @Override
    public String getFilename() {
        return id.toString();
    }

    @Override
    public @NonNull InputStream getInputStream() throws IOException {
        final Object blobId = this.id;
        String sql = getSelectBlobSQL(this.id);

        DataSource ds = Objects.requireNonNull(this.template.getDataSource(), "JdbcTemplate DataSource");
        Connection conn = DataSourceUtils.getConnection(ds);

        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            if (!rs.next()) {
                releaseAfterMissingRow(rs, stmt, conn, ds, blobId);
                throw new FileNotFoundException(format("Blob resource %s not found", blobId));
            }

            Blob b = rs.getBlob("content");
            InputStream is = b.getBinaryStream();
            return new ClosingInputStream(blobId, is, rs, stmt, null, txnMgr, conn, ds);
        } catch (SQLException e) {
            DataSourceUtils.releaseConnection(conn, ds);
            logger.error(format("getting input stream for blob resource %s", blobId), e);
            throw new IOException(format("getting input stream for blob resource %s", blobId), e);
        }
    }

    @Override
    public void delete() throws IOException {
        String sql = "DELETE FROM BLOBS WHERE id='" + this.id + "'";
        this.template.update(sql);
    }

    protected String getSelectBlobSQL(Object blobId) {
        return "SELECT id, content FROM BLOBS WHERE id='" + blobId + "'";
    }

    private void releaseAfterMissingRow(ResultSet rs, Statement stmt, Connection conn, DataSource ds, Object blobId) {
        try {
            rs.close();
            stmt.close();
            DataSourceUtils.releaseConnection(conn, ds);
        } catch (SQLException sqle) {
            logger.debug(
                format("failed to release database connection getting input stream for blob resource %s", blobId),
                sqle);
        }
    }

    public static class ClosingInputStream extends InputStream {

        private final Object id;
        private final InputStream actual;
        private final ResultSet rs;
        private final Statement stmt;
        @Nullable
        private final TransactionStatus txnStatus;
        private final PlatformTransactionManager txnMgr;
        private final Connection conn;
        private final DataSource ds;

        @Nullable
        private final AutoCloseable additionalResource;

        public ClosingInputStream(Object id, InputStream actual, ResultSet rs, Statement stmt,
                @Nullable TransactionStatus txnStatus, PlatformTransactionManager txnMgr, Connection conn,
                DataSource ds) {
            this(id, actual, rs, stmt, txnStatus, txnMgr, conn, ds, null);
        }

        public ClosingInputStream(Object id, InputStream actual, ResultSet rs, Statement stmt,
                @Nullable TransactionStatus txnStatus, PlatformTransactionManager txnMgr, Connection conn,
                DataSource ds, @Nullable AutoCloseable additionalResource) {
            this.id = id;
            this.actual = actual;
            this.rs = rs;
            this.stmt = stmt;
            this.txnStatus = txnStatus;
            this.txnMgr = txnMgr;
            this.conn = conn;
            this.ds = ds;
            this.additionalResource = additionalResource;
        }

        @Override
        public int read() throws IOException {
            try {
                return actual.read();
            } catch (IOException ioe) {
                if (txnStatus != null && !txnStatus.isCompleted()) {
                    txnMgr.rollback(txnStatus);
                }
                throw ioe;
            }
        }

        @Override
        public void close() {
            try {
                actual.close();
            } catch (IOException e) {
                logger.debug(format("closing stream for blob resource %s", id), e);
            }
            if (additionalResource != null) {
                try {
                    additionalResource.close();
                } catch (Exception e) {
                    logger.debug(format("closing additional resource for blob resource %s", id), e);
                }
            }
            try {
                rs.close();
            } catch (SQLException e) {
                logger.debug(format("closing resultset for blob resource %s", id), e);
            }
            try {
                stmt.close();
            } catch (SQLException e) {
                logger.debug(format("closing statement for blob resource %s", id), e);
            }
            try {
                if (txnStatus != null && !txnStatus.isCompleted()) {
                    txnMgr.commit(txnStatus);
                }
            } finally {
                DataSourceUtils.releaseConnection(conn, ds);
            }
        }
    }
}
