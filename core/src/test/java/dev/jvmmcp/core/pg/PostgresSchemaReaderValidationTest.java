package dev.jvmmcp.core.pg;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PostgresSchemaReaderValidationTest {

    @ParameterizedTest
    @ValueSource(strings = {
        "jdbc:postgresql://localhost:5432/mydb",
        "jdbc:postgresql://127.0.0.1:5432/testdb",
        "jdbc:postgresql://[::1]:5432/ipv6db"
    })
    @DisplayName("Valid loopback JDBC URLs should pass validation")
    void shouldAcceptValidLoopbackUrls(String url) {
        assertThatCode(() -> PostgresSchemaReader.validateJdbcUrl(url))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("SSRF Bypass: Embedded userinfo in authority must be rejected")
    void shouldRejectUserinfoBypass() {
        String bypassUrl = "jdbc:postgresql://localhost:secret@attacker-db.com:5432/evil_db";
        assertThatThrownBy(() -> PostgresSchemaReader.validateJdbcUrl(bypassUrl))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Embedded userinfo");
    }

    @Test
    @DisplayName("SSRF Bypass: Multi-host connection syntax must be rejected")
    void shouldRejectMultiHostBypass() {
        String multiHostUrl = "jdbc:postgresql://localhost:5432,internal-database.corp:5432/db?targetServerType=secondary";
        assertThatThrownBy(() -> PostgresSchemaReader.validateJdbcUrl(multiHostUrl))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Multi-host");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "jdbc:postgresql://169.254.169.254/latest/meta-data",
        "jdbc:postgresql://google.com:5432/db",
        "jdbc:postgresql://10.0.0.1:5432/internal",
        "jdbc:postgresql://192.168.1.50:5432/lan"
    })
    @DisplayName("Non-loopback hosts must be rejected by SSRF protection")
    void shouldRejectNonLoopbackHosts(String url) {
        assertThatThrownBy(() -> PostgresSchemaReader.validateJdbcUrl(url))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("SSRF Protection");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "jdbc:postgresql://localhost:5432/db?socketFactory=org.newsclub.net.unix.AFUNIXSocketFactory$FactoryArg",
        "jdbc:postgresql://localhost:5432/db?sslfactory=org.example.EvilFactory",
        "jdbc:postgresql://localhost:5432/db?sslrootcert=/etc/passwd",
        "jdbc:postgresql://localhost:5432/db?options=-c%20statement_timeout=1",
        "jdbc:postgresql://localhost:5432/db?sslmode=disable"
    })
    @DisplayName("Dangerous connection property parameters must be rejected")
    void shouldRejectDangerousParameters(String url) {
        assertThatThrownBy(() -> PostgresSchemaReader.validateJdbcUrl(url))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Dangerous JDBC connection parameter");
    }
}
