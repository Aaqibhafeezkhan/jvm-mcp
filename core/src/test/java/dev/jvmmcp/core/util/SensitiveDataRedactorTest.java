package dev.jvmmcp.core.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveDataRedactorTest {

    @Test
    @DisplayName("Returns null or empty unchanged")
    void testNullOrEmpty() {
        assertThat(SensitiveDataRedactor.redact(null)).isNull();
        assertThat(SensitiveDataRedactor.redact("")).isEmpty();
        assertThat(SensitiveDataRedactor.redact("   ")).isEqualTo("   ");
    }

    @Test
    @DisplayName("Redacts Bearer and Basic authentication headers")
    void testAuthHeaders() {
        String input = "Authorization: Bearer my-super-secret-token-12345";
        assertThat(SensitiveDataRedactor.redact(input)).isEqualTo("Authorization: Bearer ***");

        String basic = "Authorization: Basic dXNlcjpwYXNzd29yZA==";
        assertThat(SensitiveDataRedactor.redact(basic)).isEqualTo("Authorization: Basic ***");
    }

    @Test
    @DisplayName("Redacts JSON key-value secrets")
    void testJsonSecrets() {
        String json = "{\"username\": \"admin\", \"password\": \"SuperSecret123!\", \"apiKey\": \"ak_live_abcdef123456\"}";
        String redacted = SensitiveDataRedactor.redact(json);
        assertThat(redacted)
            .contains("\"password\": \"***\"")
            .contains("\"apiKey\": \"***\"")
            .contains("\"username\": \"admin\"");
    }

    @Test
    @DisplayName("Redacts assignment-based secrets")
    void testAssignmentSecrets() {
        String text = "Connecting with password=mySecretPass and token=xyz789";
        String redacted = SensitiveDataRedactor.redact(text);
        assertThat(redacted)
            .contains("password=***")
            .contains("token=***")
            .doesNotContain("mySecretPass")
            .doesNotContain("xyz789");
    }

    @Test
    @DisplayName("Redacts JDBC connection password in URLs")
    void testJdbcUrlPassword() {
        String url = "Connecting to jdbc:postgresql://postgres:secretDbPass123@localhost:5432/mydb";
        assertThat(SensitiveDataRedactor.redact(url))
            .contains("jdbc:postgresql://postgres:***@localhost:5432/mydb")
            .doesNotContain("secretDbPass123");
    }

    @Test
    @DisplayName("Redacts standalone JWT tokens")
    void testJwtRedaction() {
        String part1 = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9";
        String part2 = "eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4ifQ";
        String part3 = "SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
        String jwt = String.join(".", part1, part2, part3);
        String log = "Active session token: " + jwt + " for user";
        assertThat(SensitiveDataRedactor.redact(log))
            .contains("Active session token: *** for user")
            .doesNotContain(part3);
    }
}
