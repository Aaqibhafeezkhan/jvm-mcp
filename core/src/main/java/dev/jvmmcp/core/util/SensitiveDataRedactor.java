package dev.jvmmcp.core.util;

import java.util.regex.Pattern;

/**
 * Defensive utility that scrubs operational secrets (passwords, auth tokens, JDBC credentials,
 * API keys, and JWTs) from diagnostic reports, thread dumps, and actuator output before
 * transmission over MCP.
 */
public final class SensitiveDataRedactor {

    private static final Pattern BEARER_PATTERN = Pattern.compile(
        "(?i)\\b(bearer\\s+)[A-Za-z0-9\\-._~+/]+=*",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern BASIC_PATTERN = Pattern.compile(
        "(?i)\\b(basic\\s+)[A-Za-z0-9+/]+=*",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern JSON_SECRET_PATTERN = Pattern.compile(
        "(?i)(\"(?:password|secret|token|apikey|api_key|access_token|client_secret|private_key|auth_token)\"\\s*:\\s*)\"(?:[^\"\\\\]|\\\\.)*\"",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern ASSIGNMENT_PATTERN = Pattern.compile(
        "(?i)\\b(password|secret|token|apikey|api_key|access_token|client_secret|private_key|auth_token)\\s*=\\s*['\"]?[^'\"\\s,;&]+['\"]?",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern JDBC_URL_PATTERN = Pattern.compile(
        "(?i)(jdbc:[a-z:]+://[^:]+:)([^@]+)@",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern JWT_PATTERN = Pattern.compile(
        "\\beyJ[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]*\\b"
    );

    private SensitiveDataRedactor() {
    }

    /**
     * Redacts known sensitive patterns from the provided text.
     *
     * @param text input diagnostic text or JSON
     * @return sanitized text with sensitive substrings replaced by '***'
     */
    public static String redact(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }

        String result = text;
        result = BEARER_PATTERN.matcher(result).replaceAll("$1***");
        result = BASIC_PATTERN.matcher(result).replaceAll("$1***");
        result = JSON_SECRET_PATTERN.matcher(result).replaceAll("$1\"***\"");
        result = ASSIGNMENT_PATTERN.matcher(result).replaceAll("$1=***");
        result = JDBC_URL_PATTERN.matcher(result).replaceAll("$1***@");
        result = JWT_PATTERN.matcher(result).replaceAll("***");

        return result;
    }
}
