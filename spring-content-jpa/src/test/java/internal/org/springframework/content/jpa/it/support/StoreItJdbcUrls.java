package internal.org.springframework.content.jpa.it.support;

/**
 * Testcontainers JDBC URLs shared by JPA and REST integration tests.
 *
 * @author Vladimir Svyatski
 */
public final class StoreItJdbcUrls {

    private static final String TESTCONTAINERS_OPTIONS = "?TC_TMPFS=/testtmpfs:rw&TC_DAEMON=true";

    public static final String MARIADB = "jdbc:tc:mariadb:10.11:///databasename" + TESTCONTAINERS_OPTIONS;

    public static final String POSTGRESQL = "jdbc:tc:postgresql:12:///databasename" + TESTCONTAINERS_OPTIONS;

    public static final String SQL_SERVER = "jdbc:tc:sqlserver:///databasename" + TESTCONTAINERS_OPTIONS;

    public static final String SQL_SERVER_USERNAME = "SA";

    public static final String SQL_SERVER_PASSWORD = "A_Str0ng_Required_Password";

    public static final String TESTCONTAINERS_USERNAME = "test";

    public static final String TESTCONTAINERS_PASSWORD = "test";

    private StoreItJdbcUrls() {
    }
}
