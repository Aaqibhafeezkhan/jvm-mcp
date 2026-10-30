# Project Roadmap & Contribution Scope

> Real-time JVM diagnostic tools for AI coding assistants via Model Context Protocol (MCP).

This roadmap outlines completed development milestones, architectural priorities, and high-impact areas open for community contributions.

---

## Current Status: Phase 8 (Completed)

- [x] **Phase 0: Architecture Scaffolding**
  - Maven multi-module architecture (`core`, `mcp-server`).
  - Zero-dependency JSON-RPC 2.0 stdio server on Java 21 Virtual Threads (`Executors.newVirtualThreadPerTaskExecutor()`).
  - GitHub Actions CI matrix on Temurin JDK 21 with JaCoCo and Pitest mutation testing.

- [x] **Phase 1: JVM Discovery & Attach Lifecycle**
  - JDK Attach API wrapper (`JvmAttachService`) for live process discovery.
  - Framework heuristics (`FrameworkDetector` for Spring Boot, Quarkus, Micronaut, Plain Java).
  - Dynamic JDK management agent engagement (`VirtualMachine.startLocalManagementAgent()`).
  - Tabular CLI commands (`jvm-mcp list`, `ps`, `ls`).

- [x] **Phase 2: Memory, Heap & Concurrency Diagnostics**
  - Live heap region breakdown (Eden, Survivor, Old Gen) & allocation pressure evaluation (`MemoryMXBeanClient`).
  - GC pause tracker and diagnostic recommendations (`get_memory_summary`).
  - Heap class histogram via `jcmd GC.class_histogram` Attach API stream parsing (`HeapHistogramReader`).
  - Structured thread dumps and deadlock cycle reconstruction (`ThreadMXBeanClient`).
  - Per-thread cumulative CPU time diagnostics and top CPU consumer ranking (`getTopCpuThreads`, `jvm-mcp threads --top-cpu`).
  - CLI diagnostic commands (`jvm-mcp memory <pid>`, `jvm-mcp threads <pid>`).

- [x] **Phase 3: Spring Beans Inspector**
  - ApplicationContext bean hierarchy inspection (`SpringBeansClient`).
  - Dual-strategy discovery: Actuator HTTP discovery (`/actuator/beans`) with automatic JMX fallback.
  - Case-insensitive glob filtering (`*Service*`, `*Repository*`, `*Controller*`) with ReDoS protection.
  - Direct dependency graph and scope inspection (`jvm-mcp beans <pid> [--filter] [--detail]`).
  - Zero-dependency JSON parser engine (`SimpleJson`).

- [x] **Phase 4: Database Connection Pool Diagnostics (HikariCP)**
  - Query `com.zaxxer.hikari:type=Pool (*)` MBeans across single and multi-datasource architectures.
  - Live metric extraction: active, idle, pending connections, max pool size, acquire latency.
  - Saturation ratio calculation and automated leak/exhaustion diagnostics (`list_hikari_pools`).
  - Hermetic testing via in-memory H2 database.

- [x] **Phase 5: PostgreSQL Schema & Stat Inspector**
  - Pure JDBC metadata reader (`PostgresSchemaReader`) for tables, indexes, and foreign keys.
  - Query `pg_stat_user_tables` to identify sequential scan bottlenecks and missing indexes.
  - Query `pg_stat_statements` for slow query execution profiling.
  - Strict JDBC URL validation against SSRF and unsafe connection properties.
  - Dedicated CLI (`jvm-mcp pg`) and MCP endpoints (`inspect_pg_schema`, `find_missing_indexes`, `find_slow_queries`).

- [x] **Phase 6: Remote Spring Boot Actuator Client**
  - Lightweight `java.net.http.HttpClient` client (`ActuatorClient`) for `/actuator/health`, `/actuator/metrics`, and `/actuator/startup`.
  - Basic Auth and Bearer token header propagation (`ActuatorAuth`) with 30MB payload tarpit bounds.
  - Dedicated CLI (`jvm-mcp actuator`) and MCP endpoints (`get_actuator_health`, `get_actuator_metrics`, `get_actuator_startup`).

- [x] **Phase 7: Multi-Platform Bundling Pipeline**
  - Abandoned GraalVM Native Image due to `jdk.attach` C++ native library loading constraints under Closed-World AOT.
  - Implemented `jlink` custom JRE stripping for Linux/macOS (~40MB footprint) with recursive symlink-safe bash launcher.
  - Configured `jpackage` bundling for zero-dependency Windows `.exe` installers.
  - Implemented two-stage collector release pipeline in GitHub Actions (`build-matrix` + `publish-release`) with SHA-256 checksum generation (`checksums.txt`).

- [x] **Phase 8: Ecosystem, Multi-Platform & Packaging Automation**
  - Universal `install.sh` (`curl | bash`) with OS/architecture detection and pre-extraction SHA-256 cryptographic verification.
  - Environment readiness diagnostics command (`jvm-mcp doctor`) with ptrace scope and container boundary checks.
  - Homebrew Formula specification (`homebrew/jvm-mcp.rb`) with multi-architecture UNIX blocks (Apple Silicon, Intel Mac, Linux x86_64, Linux ARM64) and automated SHA-256 injection during GitHub Releases.
  - Dynamic runtime version resolution hierarchy with Picocli `IVersionProvider`.
  - Harmonized quality gates across modules (PIT Mutation >= 70%, Branch Coverage >= 65%).

---

## Upcoming Milestones & Community Roadmap

- [ ] **Phase 9: SSE Transport & Remote Agent Dashboard**
  - Streamable HTTP / Server-Sent Events (SSE) remote transport (`--transport sse`).
  - Spring AI / Web dashboard integration (`--spring`) as an optional decoupled module.
  - Migration to official Java MCP SDK (`io.modelcontextprotocol.sdk`) once modularized without heavy transitive dependencies.

- [ ] **Phase 10: Target Virtual Threads (Project Loom) Diagnostics (Issue #10)**
  - [x] Enumerate and inspect virtual threads inside target JVM processes via bounded HotSpot thread dump parsing (`Thread.dump_to_file`).
  - [x] Expose `--virtual` (`-v`) flag in `jvm-mcp threads` and include `includeVirtualThreads` in `get_thread_diagnostics` MCP tool.
  - [ ] Track mounted vs unmounted virtual thread counts and continuous carrier thread mapping via JVMTI.

---

## How to Claim an Area

1. Check existing issues or open a new one with the title `[Proposal] Milestone Name`.
2. Follow the architectural constraints in [`CONTRIBUTING.md`](CONTRIBUTING.md).
3. Open a draft PR early for architectural alignment.
