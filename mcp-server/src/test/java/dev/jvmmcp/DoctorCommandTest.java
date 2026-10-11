package dev.jvmmcp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.assertThat;

class DoctorCommandTest {

    @Test
    @DisplayName("doctor command with --help should display options and return 0")
    void shouldDisplayHelp() {
        CommandLine cmd = new CommandLine(new JvmMcp());
        int exitCode = cmd.execute("doctor", "--help");

        assertThat(exitCode).isZero();
    }

    @Test
    @DisplayName("doctor command should run diagnostics and report system readiness")
    void shouldRunDoctorDiagnostics() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        try {
            System.setOut(new PrintStream(out));
            CommandLine cmd = new CommandLine(new JvmMcp());
            int exitCode = cmd.execute("doctor");

            assertThat(exitCode).isZero();
            String output = out.toString();
            assertThat(output).contains("JVM-MCP Doctor");
            assertThat(output).contains("Java Version:");
            assertThat(output).contains("Attach API");
        } finally {
            System.setOut(originalOut);
        }
    }
}
