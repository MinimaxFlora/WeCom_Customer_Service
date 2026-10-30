package xlike.top.kn_ai_chat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;

import static org.assertj.core.api.Assertions.assertThat;

class MigrationTest {

    @Test
    void freshDatabaseAppliesAllMigrations() throws Exception {
        String url = "jdbc:h2:mem:migration_test;DB_CLOSE_DELAY=-1";
        Flyway.configure()
                .dataSource(url, "sa", "")
                .locations("classpath:db/migration")
                .load()
                .migrate();

        try (Connection connection = DriverManager.getConnection(url, "sa", "")) {
            assertThat(tableExists(connection, "KNOWLEDGE_BASE")).isTrue();
            assertThat(tableExists(connection, "KNOWLEDGE_CHUNK")).isTrue();
            assertThat(tableExists(connection, "ENTERPRISE_PROFILE")).isTrue();
            assertThat(columnExists(connection, "KNOWLEDGE_BASE", "SCOPE")).isTrue();
            assertThat(columnExists(connection, "AI_CONFIG", "TEMPERATURE")).isTrue();
        }
    }

    private boolean tableExists(Connection connection, String table) throws Exception {
        try (ResultSet rs = connection.getMetaData().getTables(null, null, table, null)) {
            return rs.next();
        }
    }

    private boolean columnExists(Connection connection, String table, String column) throws Exception {
        try (ResultSet rs = connection.getMetaData().getColumns(null, null, table, column)) {
            return rs.next();
        }
    }
}
