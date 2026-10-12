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
   By JVM kernel design, the JDK Attach API enforces matching OS user credentials (UID/GID). A standard user cannot attach to or inspect JVM processes owned by other users or root without explicit operating system privilege escalation. Host-to-container attachment across unshared PID/IPC namespaces is rejected by default.

2. **PostgreSQL JDBC Security & SSRF Mitigation:**
   When executing database introspection tools (`inspect_pg_schema`, `find_missing_indexes`, `find_slow_queries`), connection strings are strictly validated:
   - Must strictly begin with `jdbc:postgresql://`.
   - Host must resolve to a valid loopback address (`localhost`, `127.0.0.1`, `::1`). Multi-host syntax and embedded userinfo are rejected.
   - Disallowed driver connection parameters (e.g. `socketFactory`, `socketFactoryArg`, `sslrootcert`, `sslkey`, `sslpassword`, `options=`) are rejected to prevent arbitrary class loading or socket hijacking.
   - SQL queries in slow query reports are sanitized to redact sensitive credentials.

3. **Spring Boot Actuator HTTP Protection:**
   When querying remote Actuator endpoints:
   - Target URLs are validated against host injection.
   - Response bodies are capped at 30 MB to prevent memory exhaustion from Billion Laughs or HTTP payload tarpits.
   - Plaintext HTTP credential warnings are emitted when Basic or Bearer authentication is provided over non-TLS endpoints.

4. **Algorithmic Complexity & ReDoS Defense:**
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
