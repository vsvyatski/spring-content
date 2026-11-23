package org.springframework.versions.jpa.boot;

import internal.org.springframework.versions.jpa.boot.autoconfigure.JpaVersionsDatabaseInitializer;
import internal.org.springframework.versions.jpa.boot.autoconfigure.JpaVersionsProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.sql.init.DatabaseInitializationMode;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

public class JpaVersionsDatabaseInitializerTest {

    private JpaVersionsDatabaseInitializer initializer;

    private DataSource ds;
    private JpaVersionsProperties props;

    private boolean locksTableExists() throws SQLException {
        try (Connection conn = ds.getConnection();
             ResultSet tables = conn.getMetaData().getTables(null, null, null, new String[]{"TABLE"})) {
            while (tables.next()) {
                if ("locks".equalsIgnoreCase(tables.getString("TABLE_NAME"))) {
                    return true;
                }
            }
            return false;
        }
    }

    @Nested
    @DisplayName("When initialization is enabled")
    class InitializationIsEnabled {
        @BeforeEach
        void setUp() {
            ds = new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.HSQL).build();
            props = new JpaVersionsProperties();
            initializer = new JpaVersionsDatabaseInitializer(ds, props);
            initializer.initializeDatabase();
        }

        @Test
        void shouldExecuteCreateTableStatementsOnDatabase() throws SQLException {
            assertThat(locksTableExists()).isTrue();
        }
    }

    @Nested
    @DisplayName("When initialization is disabled")
    class InitializationIsDisabled {
        @BeforeEach
        void setUp() {
            ds = new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.HSQL).build();
            props = new JpaVersionsProperties();
            props.getInitializer().setInitializeSchema(DatabaseInitializationMode.NEVER);
            initializer = new JpaVersionsDatabaseInitializer(ds, props);
            initializer.initializeDatabase();
        }

        @Test
        void shouldNotExecuteCreateTableStatementsOnDatabase() throws SQLException {
            assertThat(locksTableExists()).isFalse();
        }
    }
}