# JVM-MCP

> **Zero-dependency Java 21 Model Context Protocol (MCP) server for live Spring Boot and JVM runtime inspection.**  
> Provide Claude, Cursor, and AI coding assistants with real-time context on memory, threads, Spring beans, and database connection pools without modifying target application code.

[![CI](https://github.com/oscarbol09/jvm-mcp/actions/workflows/ci.yml/badge.svg)](https://github.com/oscarbol09/jvm-mcp/actions/workflows/ci.yml)
[![Java Version](https://img.shields.io/badge/Java-21+-007396?logo=openjdk&logoColor=white)](https://jdk.java.net/21/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## Table of Contents

- [Why This Exists](#why-this-exists)
- [How It Works](#how-it-works)
- [Quickstart: MCP Client Setup](#quickstart-mcp-client-setup)
- [CLI Usage](#cli-usage)
- [Exposed MCP Tools](#exposed-mcp-tools)
- [Architectural Principles](#architectural-principles)
- [Security & Defense Boundaries](#security--defense-boundaries)
- [Known Limitations and Trade-offs](#known-limitations-and-trade-offs)
- [Contributing & Governance](#contributing--governance)
- [License](#license)

---

## Why This Exists

When debugging backend Java services with LLMs, static source code only tells half the story:
1. Deadlocks, thread starvation, and runaway thread allocations cannot be deduced purely from code syntax.
2. Memory leaks in Old Gen or sudden allocation spikes in Eden require live heap inspection.
3. Spring Boot bean initialization orders and active HikariCP pool saturation require dynamic runtime context.

Existing tools require heavy external APM agents, modifying `pom.xml` files to expose open Actuator/JMX endpoints, or manually running `jstack` / `jcmd` in a terminal and copy-pasting megabytes of unformatted text into prompts.

**JVM-MCP** attaches locally to target JVM processes using the standard JDK Attach API (`com.sun.tools.attach`), dynamically exposes diagnostic data over the standardized Model Context Protocol (JSON-RPC 2.0 stdio), and requires **zero modifications** to target applications.

---

## How It Works

Distributed as a standalone multi-platform bundle with a stripped custom runtime via `jlink`/`jpackage`. No pre-installed JDK is required on the client machine.

```text
+-----------------------------------------+
|      Claude Desktop / Cursor / IDE      |
+-----------------------------------------+
                     | JSON-RPC 2.0 (stdio)
                     v
+-----------------------------------------+
|               jvm-mcp                   | (Virtual Threads, < 150ms cold start)
+-----------------------------------------+
                     | Attach API / Dynamic JMX
                     v
+-----------------------------------------+
|       Target Java App (PID 45231)       | (Zero modifications or dependencies)
+-----------------------------------------+
```

---

## Quickstart: MCP Client Setup

### Claude Desktop

Add `jvm-mcp` to your `claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "jvm-mcp": {
      "command": "jvm-mcp",
      "args": ["serve"]
    }
  }
}
```

*(If running from the repository fat JAR directly: `"command": "java", "args": ["-jar", "/path/to/jvm-mcp-server-1.1.0-SNAPSHOT.jar", "serve"]`)*

### Cursor / Windsurf

1. Open **Settings > Features > MCP**.
2. Click **+ Add New MCP Server**.
3. Name: `jvm-mcp`
4. Type: `command`
5. Command: `jvm-mcp serve`

Once configured, ask your AI: *"What Java processes are running?"* or *"Analyze the memory and detect deadlocks in PID 12345"*.

---

## CLI Usage

`jvm-mcp` doubles as a standalone diagnostic terminal CLI for local inspection:

```bash
# Verify local environment readiness and Attach API permissions
jvm-mcp doctor

# Discover running Java processes and frameworks
jvm-mcp list

# Inspect memory pools, GC pauses, and heap histogram
jvm-mcp memory <pid>

# Diagnose thread states and detect deadlock cycles
jvm-mcp threads <pid>

# Inspect Spring Boot ApplicationContext beans
jvm-mcp beans <pid> --filter "*Service*"

# Introspect PostgreSQL schema and find missing indexes via JDBC
jvm-mcp pg --url "jdbc:postgresql://localhost:5432/mydb" --missing-indexes

# Query remote Spring Boot Actuator endpoints
jvm-mcp actuator --url "http://localhost:8080" --metrics
```

---

## Exposed MCP Tools

The server exposes 14 diagnostic tools to AI assistants:

| Tool Name | Description | Arguments |
| :--- | :--- | :--- |
| `list_jvms` | Lists running JVMs with PID, display name, and detected framework (Spring Boot, Quarkus, Micronaut). | *None* |
| `get_memory_summary` | Live heap/non-heap regions, GC pauses, and memory pressure. | `pid` (number) |
| `get_heap_histogram` | Top class instance counts and memory consumption via `jcmd`. | `pid` (number), `limit` (number, optional) |
| `get_thread_diagnostics` | Thread states breakdown and automated cycle deadlock detection. | `pid` (number) |
| `get_thread_dump` | Full formatted thread dump with lock stack traces. | `pid` (number) |
| `list_spring_beans` | Instantiated beans in Spring ApplicationContext via JMX/Actuator. | `pid` (number), `filter` (string, optional) |
| `get_bean_detail` | Detailed metadata, autowired dependencies, and scope of a bean. | `pid` (number), `beanName` (string) |
| `list_hikari_pools` | HikariCP pool metrics, active/idle connections, and leak warnings. | `pid` (number) |
| `inspect_pg_schema` | PostgreSQL table schema, columns, row estimates, and foreign keys. | `url` (string), `schema` (string, optional) |
| `find_missing_indexes` | High sequential scan tables with low index usage via `pg_stat_user_tables`. | `url` (string), `schema` (string, optional) |
| `find_slow_queries` | Slow query execution profiles from `pg_stat_statements`. | `url` (string) |
| `get_actuator_health` | Recursively parsed Spring Boot `/actuator/health` component status. | `url` (string) |
| `get_actuator_metrics` | Batch or individual diagnostic metric values from `/actuator/metrics`. | `url` (string), `metrics` (string, optional) |
| `get_actuator_startup` | Startup timeline and slowest beans from `/actuator/startup`. | `url` (string) |

---

## Architectural Principles

- **Virtual Threads & Thread-Safe Stdio:** Each incoming JSON-RPC request executes on a dedicated Java 21 Virtual Thread (`Executors.newVirtualThreadPerTaskExecutor()`). Outbound `stdout` writes are synchronized on a dedicated monitor lock to prevent frame interleaving.
- **Dynamic JDK Management Agent:** JMX access is initialized dynamically via `VirtualMachine.startLocalManagementAgent()`, falling back to standard `$JAVA_HOME/lib/management-agent.jar`. No custom agent bytecode is injected into target processes.
- **Zero External Runtime Dependencies:** Built with zero runtime framework overhead: custom JSON-RPC parser engine (`SimpleJson`), direct JDK `HttpClient`, and Picocli for CLI execution.

---

## Security & Defense Boundaries

1. **OS Process Boundary:** Enforces OS user (UID/GID) matching. A non-root user cannot attach to other users' processes without operating system elevation.
2. **PostgreSQL JDBC Hardening:** Enforces `jdbc:postgresql://` protocol, validates loopback resolution, and strips unsafe driver properties (`socketFactory`, `sslrootcert`, `options`) to mitigate SSRF and arbitrary class loading.
3. **Actuator HTTP Protection:** Enforces host validation and caps response bodies at 30 MB to prevent memory exhaustion from payload tarpits.
4. **ReDoS & Recursion Guards:** Sanitizes glob wildcards in `GlobMatcher` to prevent quadratic backtracking and bounds `SimpleJson` recursion depth to 100.

---

## Known Limitations and Trade-offs

- **Container Boundaries:** Direct host attachment across isolated Docker container namespaces fails due to separate PID and IPC namespaces. Attach from within the container namespace, or use Actuator HTTP mode (`jvm-mcp actuator --url ...`).
- **Jlink / Jpackage Bundling:** GraalVM Native Image was evaluated and rejected because `jdk.attach` dynamically loads HotSpot C++ native libraries (`libattach.so` / `attach.dll`), violating Closed-World AOT assumptions. We distribute minimal bundles containing a stripped modular JRE via `jlink` (Linux/macOS) and `jpackage` app images (Windows), maintaining cross-platform parity with startup times under 150ms.

---

## Contributing & Governance

- [ROADMAP.md](ROADMAP.md): Milestones and active development phases.
- [CONTRIBUTING.md](CONTRIBUTING.md): Local environment setup, module architecture, and guide for adding MCP tools.
- [SECURITY.md](SECURITY.md): Vulnerability reporting policy and defense boundaries.
- [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md): Community standards (Contributor Covenant).

---

## License

This project is licensed under the terms of the **MIT License**. See the [`LICENSE`](LICENSE) file for details.

Copyright (c) 2026 oscarbol09 / JVM-MCP Contributors.
