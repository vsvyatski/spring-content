package internal.org.springframework.content.jpa.io;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.postgresql.PGConnection;
import org.postgresql.largeobject.LargeObject;
import org.postgresql.largeobject.LargeObjectManager;
import org.springframework.content.jpa.io.BlobResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

import static java.lang.String.format;

public class PostgresBlobResourceOutputStream extends BlobResourceOutputStream {

    private static final Log logger = LogFactory.getLog(PostgresBlobResourceOutputStream.class);

    private LargeObject lo;

    public PostgresBlobResourceOutputStream(BlobResource blobResource, JdbcTemplate template) {
        super(blobResource, template);
    }

    @Override
    protected OutputStream initializeStream() {
        final Object rid = getResource().getId();
        DataSource ds = Objects.requireNonNull(getTemplate().getDataSource(), "JdbcTemplate DataSource");
        con = DataSourceUtils.getConnection(ds);

        String sql = "SELECT id, content FROM BLOBS WHERE id=?";
        try {
            ps = con.prepareStatement(sql, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ps.setString(1, rid.toString());
            rs = ps.executeQuery();

            if (!rs.next()) {
                insert = true;
                rs.moveToInsertRow();
            }

            LargeObjectManager manager = con.unwrap(PGConnection.class).getLargeObjectAPI();
            long oid = manager.createLO(LargeObjectManager.READ | LargeObjectManager.WRITE);
            lo = manager.open(oid);
            return lo.getOutputStream();
        } catch (SQLException e) {
            logger.error(format("initializing postgres blob output stream for resource %s", rid), e);
        }

        return null;
    }

    @Override
    public void close() throws IOException {
        Object rid = getResource().getId();
        DataSource ds = Objects.requireNonNull(getTemplate().getDataSource(), "JdbcTemplate DataSource");

        IOUtils.closeQuietly(os);

        try {
            lo.close();
        } catch (SQLException e) {
            logger.error(format("closing large object for resource %s", rid), e);
        }

        Long oidToBeRemoved = null;
        try {
            rs.updateString(1, rid.toString());

            if (!insert) {
                oidToBeRemoved = rs.getLong("content");
            }

            rs.updateLong(2, lo.getLongOID());

            if (!insert) {
                rs.updateRow();
            } else {
                rs.insertRow();
            }
        } catch (SQLException e) {
            logger.error(format("updating large object for resource %s", rid), e);
        } finally {
            try {
                rs.close();
                ps.close();

                if (oidToBeRemoved != null) {
                    deleteReplacedLargeObject(con, oidToBeRemoved);
                }

                DataSourceUtils.releaseConnection(con, ds);
            } catch (SQLException e) {
                logger.error(format("finalizing large object for resource %s", rid), e);
            }
        }
    }

    private void deleteReplacedLargeObject(Connection connection, Long oidToBeRemoved) {
        try {
            LargeObjectManager manager = connection.unwrap(PGConnection.class).getLargeObjectAPI();
            manager.delete(oidToBeRemoved);
        } catch (SQLException e) {
            logger.error(format("deleting replaced large object %s", oidToBeRemoved), e);
        }
    }
}
