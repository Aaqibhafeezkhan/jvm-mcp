package dev.jvmmcp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.assertThat;

class PgCommandTest {

    @Test
    @DisplayName("pg command with --help should display help options and return 0")
    void shouldDisplayHelp() {
        CommandLine cmd = new CommandLine(new JvmMcp());
        int exitCode = cmd.execute("pg", "--help");

        assertThat(exitCode).isZero();
    }

    @Test
    @DisplayName("pg command with missing required options should return usage error code")
    void shouldFailOnMissingRequiredOptions() {
        CommandLine cmd = new CommandLine(new JvmMcp());
        int exitCode = cmd.execute("pg");

        assertThat(exitCode).isEqualTo(2); // Picocli parameter exception code
    }

    @Test
    @DisplayName("pg command with invalid/unreachable URL should return error code 1")
    void shouldFailOnInvalidUrl() {
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        PrintStream originalErr = System.err;
        try {
            System.setErr(new PrintStream(err));
            CommandLine cmd = new CommandLine(new JvmMcp());
            int exitCode = cmd.execute("pg", "--url", "jdbc:postgresql://localhost:9999/nonexistent", "--action", "schema");

            assertThat(exitCode).isEqualTo(1);
            assertThat(err.toString()).contains("Error inspecting PostgreSQL database");
        } finally {
            System.setErr(originalErr);
        }
    }

    @Test
    @DisplayName("pg command with dangerous connection parameter should be rejected with exit code 1")
    void shouldRejectDangerousParameters() {
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        PrintStream originalErr = System.err;
        try {
            System.setErr(new PrintStream(err));
            CommandLine cmd = new CommandLine(new JvmMcp());
            int exitCode = cmd.execute("pg", "--url", "jdbc:postgresql://localhost:5432/db?socketFactory=evil", "--action", "schema");

            assertThat(exitCode).isEqualTo(1);
            assertThat(err.toString()).contains("Dangerous JDBC connection parameter");
        } finally {
            System.setErr(originalErr);
        }
    }

    @Test
    @DisplayName("pg command with unknown action should return exit code 1")
    void shouldFailOnUnknownAction() {
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        PrintStream originalErr = System.err;
        try {
            System.setErr(new PrintStream(err));
            CommandLine cmd = new CommandLine(new JvmMcp());
            int exitCode = cmd.execute("pg", "--url", "jdbc:postgresql://localhost:5432/db", "--action", "unknown-action");

            assertThat(exitCode).isEqualTo(1);
            assertThat(err.toString()).contains("Unknown action");
        } finally {
            System.setErr(originalErr);
        }
    }
}
