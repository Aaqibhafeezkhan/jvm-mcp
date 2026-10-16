package dev.jvmmcp.server.tool;

import dev.jvmmcp.core.pg.PostgresModels;
import dev.jvmmcp.core.pg.PostgresSchemaReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostgresToolHandlerTest {

    @Mock
    private PostgresSchemaReader mockReader;

    @Test
    @DisplayName("toolNames and toolDefinitions return all 3 PostgreSQL tools")
    void testToolNamesAndDefinitions() {
        PostgresToolHandler handler = new PostgresToolHandler((url, user, pass) -> mockReader);

        assertThat(handler.toolNames()).containsExactlyInAnyOrder(
            "inspect_pg_schema",
            "find_missing_indexes",
            "find_slow_queries"
        );
        assertThat(handler.toolDefinitions()).hasSize(3);
    }

    @Test
    @DisplayName("execute throws on missing url")
    void testMissingUrl() {
        PostgresToolHandler handler = new PostgresToolHandler((url, user, pass) -> mockReader);

        assertThatThrownBy(() -> handler.execute("inspect_pg_schema", Map.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Missing required argument: url");
    }

    @Test
    @DisplayName("execute throws on invalid JDBC url")
    void testInvalidJdbcUrl() {
        PostgresToolHandler handler = new PostgresToolHandler((url, user, pass) -> mockReader);

        assertThatThrownBy(() -> handler.execute("inspect_pg_schema", Map.of("url", "http://not-jdbc.com")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must start with jdbc:postgresql://");
    }

    @Test
    @DisplayName("inspect_pg_schema executes with default and explicit schema")
    void testInspectPgSchema() throws Exception {
        when(mockReader.inspectSchema("public")).thenReturn(new PostgresModels.SchemaInfo("public", List.of(), List.of(), List.of()));
        when(mockReader.inspectSchema("custom")).thenReturn(new PostgresModels.SchemaInfo("custom", List.of(), List.of(), List.of()));

        Map<String, String> env = Map.of("PG_USER", "postgres", "PG_PASSWORD", "secret");
        PostgresToolHandler handler = new PostgresToolHandler((url, user, pass) -> {
            assertThat(user).isEqualTo("postgres");
            assertThat(pass).isEqualTo("secret");
            return mockReader;
        }, env::get);

        // default public
        Map<String, Object> resultDefault = handler.execute("inspect_pg_schema", Map.of("url", "jdbc:postgresql://localhost:5432/mydb"));
        assertThat(resultDefault.get("isError")).isNull();
        verify(mockReader).inspectSchema("public");

        // explicit schema
        Map<String, Object> resultCustom = handler.execute("inspect_pg_schema", Map.of("url", "jdbc:postgresql://localhost:5432/mydb", "schema", "custom"));
        assertThat(resultCustom.get("isError")).isNull();
        verify(mockReader).inspectSchema("custom");
    }

    @Test
    @DisplayName("find_missing_indexes executes with default and explicit schema")
    void testFindMissingIndexes() throws Exception {
        when(mockReader.findMissingIndexes("public")).thenReturn(new PostgresModels.MissingIndexAnalysis(List.of(
            new PostgresModels.MissingIndexCandidate("users", 1000L, 5000L, 10L, 50L)
        )));

        PostgresToolHandler handler = new PostgresToolHandler((url, user, pass) -> mockReader);
        Map<String, Object> result = handler.execute("find_missing_indexes", Map.of("url", "jdbc:postgresql://localhost:5432/mydb"));

        assertThat(result.get("isError")).isNull();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) result.get("content");
        assertThat(content.get(0).get("text").toString()).contains("users");
        verify(mockReader).findMissingIndexes("public");
    }

    @Test
    @DisplayName("find_slow_queries executes properly")
    void testFindSlowQueries() throws Exception {
        when(mockReader.findSlowQueries()).thenReturn(new PostgresModels.SlowQueryAnalysis(true, List.of(
            new PostgresModels.SlowQueryInfo("SELECT * FROM big_table", 10L, 500.0, 50.0, 1000L)
        )));

        PostgresToolHandler handler = new PostgresToolHandler((url, user, pass) -> mockReader);
        Map<String, Object> result = handler.execute("find_slow_queries", Map.of("url", "jdbc:postgresql://localhost:5432/mydb"));

        assertThat(result.get("isError")).isNull();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) result.get("content");
        assertThat(content.get(0).get("text").toString()).contains("SELECT * FROM big_table");
        verify(mockReader).findSlowQueries();
    }

    @Test
    @DisplayName("execute throws on invalid or malicious schema name")
    void testInvalidSchemaNameThrows() {
        PostgresToolHandler handler = new PostgresToolHandler((url, user, pass) -> mockReader);

        assertThatThrownBy(() -> handler.execute("inspect_pg_schema", Map.of(
            "url", "jdbc:postgresql://localhost:5432/mydb",
            "schema", "public; DROP TABLE users;--"
        )))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid schema name");
    }

    @Test
    @DisplayName("execute throws on unknown postgres tool")
    void testUnknownTool() {
        PostgresToolHandler handler = new PostgresToolHandler((url, user, pass) -> mockReader);

        assertThatThrownBy(() -> handler.execute("unknown_pg_tool", Map.of("url", "jdbc:postgresql://localhost:5432/mydb")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unknown postgres tool");
    }
}
