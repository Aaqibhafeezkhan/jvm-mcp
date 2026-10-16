# Security Policy

## Supported Versions

Only the latest release and active development branch receive security updates:

| Version | Supported |
| ------- | --------- |
| 1.1.x   | Yes       |
| 1.0.x   | Yes       |
| < 1.0   | No        |

---

## Threat Model & Security Boundaries

`jvm-mcp` is designed as a local developer inspection tool with strong defense-in-depth boundaries:

1. **OS User & Process Isolation (Attach API):**
   By JVM kernel design, the JDK Attach API enforces matching OS user credentials (UID/GID). A standard user cannot attach to or inspect JVM processes owned by other users or root without explicit operating system privilege escalation. In addition, `JvmAttachService` validates that the target PID is a positive numeric identifier, confirms the process is alive in the operating system process table, and verifies process user ownership via `ProcessHandle.info().user()` before initiating an attach. Host-to-container attachment across unshared PID/IPC namespaces is rejected by default.

2. **Sensitive Data Redaction in MCP Responses:**
   All diagnostic tool outputs, property dumps, and execution error messages returned through the MCP protocol are automatically sanitized by `SensitiveDataRedactor`:
   - Nested maps, lists, and array structures are recursively traversed and sanitized via `redactDeep`.
   - Authorization headers (`Bearer <token>`, `Basic <credentials>`) are masked.
   - Sensitive key-value pairs (passwords, tokens, API keys, secrets) in JSON responses and configuration dumps are redacted.
   - Embedded JDBC userinfo credentials (`jdbc:...://user:password@host`) are stripped.
   - JSON Web Tokens (`eyJ...`) are replaced with `***`.

3. **Spring Boot Actuator HTTP Protection & SSRF Mitigation:**
   When querying Actuator endpoints:
   - Target URLs are strictly restricted to `http` and `https` schemes. Arbitrary URI schemes (`file:`, `ftp:`, `gopher:`) are rejected.
   - Userinfo, query parameters, and fragments are rejected in base URLs to prevent URI confusion.
   - HTTP redirect following is strictly disabled (`HttpClient.Redirect.NEVER`) to prevent redirect bypass attacks to internal networks or cloud metadata services (`169.254.169.254`).
   - Endpoint paths reject directory traversal attempts (`..` or `\`).
   - Response bodies are capped at 30 MB to prevent memory exhaustion from Billion Laughs or HTTP payload tarpits.
   - Plaintext HTTP credential warnings are emitted when Basic or Bearer authentication is provided over non-TLS endpoints.

4. **PostgreSQL JDBC Security & Schema Sanitization:**
   When executing database introspection tools (`inspect_pg_schema`, `find_missing_indexes`, `find_slow_queries`), connection strings and identifiers are strictly validated:
   - Must strictly begin with `jdbc:postgresql://`.
   - Host must resolve to a valid loopback address (`localhost`, `127.0.0.1`, `::1`). Multi-host syntax and embedded userinfo are rejected.
   - Disallowed driver connection parameters (e.g. `socketFactory`, `socketFactoryArg`, `sslrootcert`, `sslkey`, `sslpassword`, `options=`) are rejected to prevent arbitrary class loading or socket hijacking.
   - Target schema names are strictly validated against standard SQL identifier rules (`^[a-zA-Z_][a-zA-Z0-9_]*$`, max 63 characters) to eliminate SQL injection vectors.
   - SQL queries in slow query reports are sanitized to redact sensitive literals.
   - All statement executions enforce a strict 10-second JDBC query timeout (`setQueryTimeout(10)`).

5. **Resource Bounding & Execution Timeouts:**
   - Transport level: `BoundedLineReader` streams incoming stdio requests in fixed 8KB chunks, strictly counting UTF-8 bytes up to a hard ceiling of 4MB. Oversized lines are discarded and drained without accumulating in memory, completely preventing OOM attacks before line completion.
   - Diagnostic tools execute on virtual threads with an enforced 15-second timeout, preventing hung sockets or blocking calls from freezing the MCP transport. `ToolRegistry` implements `AutoCloseable` for clean executor termination.
   - Heap histogram inspection (`HeapHistogramReader`) enforces safe defaults (`topN = 50`), a maximum ceiling (`topN = 1000`), and caps parser processing at 100,000 lines.
   - CLI and tool arguments are bounded against integer overflow and negative values via `ToolArgs`.

6. **Algorithmic Complexity & ReDoS Defense:**
   - Wildcard matching in `GlobMatcher` collapses consecutive wildcards and bounds pattern length to 256 characters to avoid quadratic regex backtracking (ReDoS).
   - The embedded `SimpleJson` parser limits serialization recursion depth (`MAX_SERIALIZATION_DEPTH = 100`) to prevent thread stack overflow (`StackOverflowError`).

---

## Reporting a Vulnerability

We take security seriously. If you discover a security vulnerability in `jvm-mcp`, please follow responsible disclosure:

1. **Do not open a public GitHub issue.**
2. Report the vulnerability privately via **GitHub Private Vulnerability Reporting** on this repository:
   - Navigate to **Security** -> **Advisories** -> **Report a vulnerability**.
   - Alternatively, email the maintainer directly at `oscarbolanos09@gmail.com` with the subject `[SECURITY] JVM-MCP Vulnerability Report`.
3. Include:
   - Steps to reproduce the issue.
   - Proof-of-concept code or minimal reproduction environment.
   - Affected operating system and JVM version.

### Response Timeline
- **Initial Response:** Within 48 hours.
- **Triage & Status Update:** Within 5 business days.
- **Fix & Public Advisory:** Coordinated release within 14 days of confirmed reproduction.
