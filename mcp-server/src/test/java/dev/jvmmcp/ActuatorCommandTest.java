package dev.jvmmcp;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ActuatorCommandTest {

    private static HttpServer server;
    private static String serverBaseUrl;

    @BeforeAll
    static void setUpServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/actuator/health", exchange -> {
            byte[] resp = "{\"status\":\"UP\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/vnd.spring-boot.actuator.v3+json");
            exchange.sendResponseHeaders(200, resp.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp);
            }
        });
        server.createContext("/actuator/metrics", exchange -> {
            byte[] resp = "{\"names\":[\"jvm.memory.used\",\"jvm.threads.live\"]}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/vnd.spring-boot.actuator.v3+json");
            exchange.sendResponseHeaders(200, resp.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp);
            }
        });
        server.createContext("/actuator/metrics/jvm.memory.used", exchange -> {
            byte[] resp = "{\"name\":\"jvm.memory.used\",\"measurements\":[{\"value\":100}]}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/vnd.spring-boot.actuator.v3+json");
            exchange.sendResponseHeaders(200, resp.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp);
            }
        });
        server.createContext("/actuator/metrics/jvm.threads.live", exchange -> {
            byte[] resp = "{\"name\":\"jvm.threads.live\",\"measurements\":[{\"value\":10}]}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/vnd.spring-boot.actuator.v3+json");
            exchange.sendResponseHeaders(200, resp.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp);
            }
        });
        server.createContext("/actuator/startup", exchange -> {
            byte[] resp = "{\"springBootVersion\":\"3.3.0\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/vnd.spring-boot.actuator.v3+json");
            exchange.sendResponseHeaders(200, resp.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp);
            }
        });
        server.createContext("/actuator/info", exchange -> {
            byte[] resp = "{\"app\":{\"version\":\"1.0\"}}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/vnd.spring-boot.actuator.v3+json");
            exchange.sendResponseHeaders(200, resp.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp);
            }
        });
        server.start();
        serverBaseUrl = "http://localhost:" + server.getAddress().getPort();
    }

    @AfterAll
    static void tearDownServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("actuator command with --help should display help options and return 0")
    void shouldDisplayHelp() {
        CommandLine cmd = new CommandLine(new JvmMcp());
        int exitCode = cmd.execute("actuator", "--help");

        assertThat(exitCode).isZero();
    }

    @Test
    @DisplayName("actuator command with missing required options should return usage error code 2")
    void shouldFailOnMissingRequiredOptions() {
        CommandLine cmd = new CommandLine(new JvmMcp());
        int exitCode = cmd.execute("actuator");

        assertThat(exitCode).isEqualTo(2);
    }

    @Test
    @DisplayName("actuator command should query health successfully")
    void shouldQueryHealth() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        try {
            System.setOut(new PrintStream(out));
            CommandLine cmd = new CommandLine(new JvmMcp());
            int exitCode = cmd.execute("actuator", "--url", serverBaseUrl, "health");

            assertThat(exitCode).isZero();
            assertThat(out.toString()).contains("\"status\":\"UP\"");
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    @DisplayName("actuator command should default to health when endpoint is omitted")
    void shouldDefaultToHealthWhenEndpointOmitted() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        try {
            System.setOut(new PrintStream(out));
            CommandLine cmd = new CommandLine(new JvmMcp());
            int exitCode = cmd.execute("actuator", "--url", serverBaseUrl);

            assertThat(exitCode).isZero();
            assertThat(out.toString()).contains("\\"status\\":\\"UP\\"");
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    @DisplayName("actuator command should query metrics successfully")
    void shouldQueryMetrics() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        try {
            System.setOut(new PrintStream(out));
            CommandLine cmd = new CommandLine(new JvmMcp());
            int exitCode = cmd.execute("actuator", "--url", serverBaseUrl, "metrics");

            assertThat(exitCode).isZero();
            assertThat(out.toString()).contains("jvm.memory.used");
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    @DisplayName("actuator command should query single metric successfully")
    void shouldQuerySingleMetric() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        try {
            System.setOut(new PrintStream(out));
            CommandLine cmd = new CommandLine(new JvmMcp());
            int exitCode = cmd.execute("actuator", "--url", serverBaseUrl, "metrics/jvm.memory.used");

            assertThat(exitCode).isZero();
            assertThat(out.toString()).contains("\"name\":\"jvm.memory.used\"");
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    @DisplayName("actuator command should query batch metrics successfully")
    void shouldQueryBatchMetrics() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        try {
            System.setOut(new PrintStream(out));
            CommandLine cmd = new CommandLine(new JvmMcp());
            int exitCode = cmd.execute("actuator", "--url", serverBaseUrl, "metrics/jvm.memory.used,jvm.threads.live");

            assertThat(exitCode).isZero();
            assertThat(out.toString()).contains("jvm.memory.used");
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    @DisplayName("actuator command should query custom endpoint successfully")
    void shouldQueryCustomEndpoint() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        try {
            System.setOut(new PrintStream(out));
            CommandLine cmd = new CommandLine(new JvmMcp());
            int exitCode = cmd.execute("actuator", "--url", serverBaseUrl, "actuator/info");

            assertThat(exitCode).isZero();
            assertThat(out.toString()).contains("\"version\":\"1.0\"");
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    @DisplayName("actuator command warns on credentials over unencrypted HTTP")
    void shouldWarnOnCredentialsOverHttp() {
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        PrintStream originalErr = System.err;
        try {
            System.setErr(new PrintStream(err));
            CommandLine cmd = new CommandLine(new JvmMcp());
            String ipUrl = "http://127.0.0.1:" + server.getAddress().getPort();
            cmd.execute("actuator", "--url", ipUrl, "--token", "secret", "health");

            assertThat(err.toString()).contains("WARNING: Sending Actuator credentials over unencrypted HTTP.");
        } finally {
            System.setErr(originalErr);
        }
    }

    @Test
    @DisplayName("actuator command fails on invalid auth combination")
    void shouldFailOnInvalidAuth() {
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        PrintStream originalErr = System.err;
        try {
            System.setErr(new PrintStream(err));
            CommandLine cmd = new CommandLine(new JvmMcp());
            int exitCode = cmd.execute("actuator", "--url", serverBaseUrl, "--user", "admin", "health");

            assertThat(exitCode).isEqualTo(1);
            assertThat(err.toString()).contains("--actuator-user and --actuator-password must be provided together");
        } finally {
            System.setErr(originalErr);
        }
    }

    @Test
    @DisplayName("actuator command should query startup successfully")
    void shouldQueryStartup() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        try {
            System.setOut(new PrintStream(out));
            CommandLine cmd = new CommandLine(new JvmMcp());
            int exitCode = cmd.execute("actuator", "--url", serverBaseUrl, "startup");

            assertThat(exitCode).isZero();
            assertThat(out.toString()).contains("springBootVersion");
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    @DisplayName("actuator command against unreachable endpoint should return error code 1")
    void shouldFailOnUnreachableEndpoint() {
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        PrintStream originalErr = System.err;
        try {
            System.setErr(new PrintStream(err));
            CommandLine cmd = new CommandLine(new JvmMcp());
            int exitCode = cmd.execute("actuator", "--url", "http://localhost:59999", "health");

            assertThat(exitCode).isEqualTo(1);
            assertThat(err.toString()).contains("Error inspecting Actuator");
        } finally {
            System.setErr(originalErr);
        }
    }
}
