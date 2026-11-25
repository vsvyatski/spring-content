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

    protected OutputStream initializeStream() {
        final Object rid = blobResource.getId();
        con = DataSourceUtils.getConnection(dataSource());

        String sql = "SELECT id, content FROM BLOBS WHERE id=?";
        try {
            ps = con.prepareStatement(sql, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ps.setString(1, rid.toString());
            rs = ps.executeQuery();

            if (!rs.next()) {
                insert = true;
                rs.moveToInsertRow();
            }

            blob = ps.getConnection().createBlob();
            return blob.setBinaryStream(1L);

        } catch (SQLException e) {
            logger.error(format("initializing stream for blob resource %s", rid), e);
        }

        return null;
    }

    @Override
    public void close() throws IOException {
        IOUtils.closeQuietly(os);

        try {
            rs.updateString(1, blobResource.getId().toString());
            rs.updateBlob(2, blob);

            if (!insert) {
                rs.updateRow();
            } else {
                rs.insertRow();
            }
        } catch (SQLException e) {
            logger.error(format("closing stream for blob resource %s", blobResource.getId()), e);
        } finally {
            try {
                rs.close();
                ps.close();
                DataSourceUtils.releaseConnection(con, dataSource());
            } catch (SQLException e) {
                logger.error(format("closing resources for blob resource %s", blobResource.getId()), e);
            }
        }

        super.close();
    }

    private DataSource dataSource() {
        return Objects.requireNonNull(template.getDataSource(), "JdbcTemplate DataSource");
    }
}
