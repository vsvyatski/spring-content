package internal.org.springframework.content.jpa.io;

import org.springframework.content.jpa.io.AbstractBlobResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

public class MariaDbBlobResource extends AbstractBlobResource {

    public MariaDbBlobResource(Object id, JdbcTemplate template, PlatformTransactionManager txnMgr) {
        super(id, template, txnMgr);
    }
}
