package internal.org.springframework.content.jpa.io;

import com.microsoft.sqlserver.jdbc.SQLServerStatement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.NonNull;
import org.springframework.content.jpa.io.AbstractBlobResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

import static java.lang.String.format;

public class SQLServerBlobResource extends AbstractBlobResource {

    private static final Log logger = LogFactory.getLog(SQLServerBlobResource.class);

    public SQLServerBlobResource(Object id, JdbcTemplate template, PlatformTransactionManager txnMgr) {
        super(id, template, txnMgr);
    }

    @Override
    public @NonNull InputStream getInputStream() throws IOException {
        final Object blobId = getId();
        String sql = getSelectBlobSQL(blobId);

        DataSource ds = Objects.requireNonNull(getTemplate().getDataSource(), "JdbcTemplate DataSource");
        Connection conn = DataSourceUtils.getConnection(ds);
        try {
            conn.setAutoCommit(false);
        } catch (SQLException e) {
            DataSourceUtils.releaseConnection(conn, ds);
            logger.error(format("setting autocommit to false whilst getting content %s", blobId), e);
            throw new IOException(format("setting autocommit to false whilst getting content %s", blobId), e);
        }

        try {
            Statement stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);

            if (stmt.isWrapperFor(SQLServerStatement.class)) {
                SQLServerStatement sqlStmt = stmt.unwrap(SQLServerStatement.class);
                sqlStmt.setResponseBuffering("adaptive");
            }
            ResultSet rs = stmt.executeQuery(sql);

            if (!rs.next()) {
                try {
                    rs.close();
                    stmt.close();
                } catch (SQLException sqle) {
                    logger.debug(format("closing resources for missing blob resource %s", blobId), sqle);
                }
                DataSourceUtils.releaseConnection(conn, ds);
                throw new FileNotFoundException(format("Blob resource %s not found", blobId));
            }
            InputStream is = rs.getBinaryStream("content");
            return new ClosingInputStream(blobId, is, rs, stmt, null, getTransactionManager(), conn, ds);
        } catch (SQLException e) {
            DataSourceUtils.releaseConnection(conn, ds);
            logger.error(format("getting content %s", blobId), e);
            throw new IOException(format("getting content %s", blobId), e);
        }
    }
}
