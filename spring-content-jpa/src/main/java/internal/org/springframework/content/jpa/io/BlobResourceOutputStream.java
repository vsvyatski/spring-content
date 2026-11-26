package internal.org.springframework.content.jpa.io;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.content.jpa.io.BlobResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.*;
import java.util.Objects;

import static java.lang.String.format;

public class BlobResourceOutputStream extends OutputStream {

    private static final Log logger = LogFactory.getLog(BlobResourceOutputStream.class);

    private final BlobResource blobResource;
    private final JdbcTemplate template;

    protected Connection con;
    protected PreparedStatement ps;
    protected ResultSet rs;
    protected OutputStream os;
    protected Blob blob;
    protected boolean insert;

    public BlobResourceOutputStream(BlobResource blobResource, JdbcTemplate template) {
        this.blobResource = blobResource;
        this.template = template;
    }

    protected BlobResource getResource() {
        return blobResource;
    }

    protected JdbcTemplate getTemplate() {
        return template;
    }

    @Override
    public void write(int b) throws IOException {
        if (os == null) {
            os = initializeStream();
        }

        os.write(b);
    }

    protected OutputStream initializeStream() throws IOException {
        final Object rid = blobResource.getId();
        con = DataSourceUtils.getConnection(dataSource());

        String sql = "SELECT id FROM BLOBS WHERE id=?";
        try {
            ps = con.prepareStatement(sql);
            ps.setString(1, rid.toString());
            rs = ps.executeQuery();
            insert = !rs.next();

            blob = ps.getConnection().createBlob();
            return blob.setBinaryStream(1L);
        } catch (SQLException e) {
            logger.error(format("initializing stream for blob resource %s", rid), e);
            throw new IOException(format("initializing stream for blob resource %s", rid), e);
        }
    }

    @Override
    public void close() throws IOException {
        IOUtils.closeQuietly(os);

        if (blob == null || con == null) {
            return;
        }

        final Object rid = blobResource.getId();
        try {
            if (rs != null) {
                rs.close();
            }
            if (ps != null) {
                ps.close();
            }

            if (insert) {
                try (PreparedStatement insertStmt =
                         con.prepareStatement("INSERT INTO BLOBS (id, content) VALUES (?, ?)")) {
                    insertStmt.setString(1, rid.toString());
                    insertStmt.setBlob(2, blob);
                    insertStmt.executeUpdate();
                }
            } else {
                try (PreparedStatement updateStmt =
                         con.prepareStatement("UPDATE BLOBS SET content = ? WHERE id = ?")) {
                    updateStmt.setBlob(1, blob);
                    updateStmt.setString(2, rid.toString());
                    int updated = updateStmt.executeUpdate();
                    if (updated != 1) {
                        throw new IOException(
                            format("Expected 1 row updated for blob resource %s, got %d", rid, updated));
                    }
                }
            }
        } catch (SQLException e) {
            throw new IOException(format("closing stream for blob resource %s", rid), e);
        } finally {
            DataSourceUtils.releaseConnection(con, dataSource());
        }

        super.close();
    }

    private DataSource dataSource() {
        return Objects.requireNonNull(template.getDataSource(), "JdbcTemplate DataSource");
    }
}
