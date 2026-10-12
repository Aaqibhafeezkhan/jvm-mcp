# Contributing to JVM-MCP

Thank you for contributing to JVM-MCP! We welcome issues, architectural feedback, and pull requests from developers passionate about JVM runtime diagnostics and AI agent tooling.

---

## 1. Core Architectural Guidelines

Before writing code, please review these foundational design decisions:

1. **Pure Java Core (`core` module):**
   - The `core` module must remain **zero-dependency** on heavy frameworks (no Spring, no ORMs, no heavy JSON parsers).
   - Use Java 21 features idiomatically: `record` for immutable DTOs, `switch` pattern matching, and standard JDK APIs (`com.sun.tools.attach`, `javax.management`, `java.net.http`).

2. **Concurrency & Protocol Engine:**
   - The MCP server operates strictly over `stdio` using an in-house JSON-RPC 2.0 engine on Java 21 Virtual Threads (`Executors.newVirtualThreadPerTaskExecutor()`). Cold start must remain `< 150ms`.
   - Keep the CLI lean and zero-dependency: do not introduce external application frameworks (no Spring Boot, Quarkus, or heavy JSON dependencies).
   - Write operations to `stdout` must be thread-safe (synchronized) to avoid interleaving concurrent JSON-RPC output.

3. **Jlink & Jpackage Custom Runtime Architecture:**
   - The Attach API requires HotSpot native library bindings (`attach.dll` / `libattach.so`), so standalone distribution is packaged via `jlink` custom-stripped JREs and `jpackage` installers.
   - Avoid unchecked dynamic reflection or bloated dependencies in `core` and `mcp-server` to keep binary size minimal and startup fast.

4. **Resource Safety & Graceful Failures:**
   - All JMX connections and Attach handles must implement deterministic cleanup (`try-with-resources` or explicit `detach()`).
   - Never throw raw runtime exceptions to the MCP client; wrap errors in structured diagnostic responses (`AttachResult`, `DiagnosticReport`).

---

## 2. Local Development & Setup

### Prerequisites
* **JDK 21+** (Temurin or OpenJDK).
* Git.

### Building the Monorepo
```bash
# Build all modules and run test suite
./mvnw clean verify

# Run CLI locally
java -jar mcp-server/target/jvm-mcp-server-1.1.0-SNAPSHOT.jar list
```

---

## 3. How to Implement a New MCP Tool

1. **Define Domain Models in `core/`:**
   Create an immutable `record` in `dev.jvmmcp.core.model` representing the structured diagnostic data.

2. **Implement the Client/Reader:**
   Place JMX readers in `dev.jvmmcp.core.jmx` or HTTP clients in `dev.jvmmcp.core.spring`. Ensure it takes an attached `VirtualMachine` or connection handle and returns domain records.

3. **Write Unit & Integration Tests:**
   Add tests under `core/src/test/java/` verifying behavior, including mock processes or error scenarios (e.g., target MBean missing).

4. **Register the MCP Tool in `mcp-server/`:**
   - Add the tool specification and input schema to `ServeCommand#getToolsList()`.
   - Add the execution handler and argument extraction logic to `ServeCommand#executeTool()`.
   - If exposing as a standalone CLI command, implement a `@Command` class and register it under `@CommandLine.Command(subcommands = { ... })` in `JvmMcp.java`.

---

## 4. Commit & Pull Request Guidelines

### Commit Format (Conventional Commits)
Use atomic commits with descriptive, human-written messages explaining the **why**:
* `feat(heap): add live heap histogram reader via attach api`
* `fix(jmx): resolve memory leak when detaching from target vm`
* `docs(readme): clarify docker permission constraints`
* `test(threads): add deadlock reproduction integration test`

### Pull Request Rules
* Keep PRs focused on a single concern.
* Ensure `./mvnw clean verify` passes locally before submitting.
* Write a concise summary explaining **what** changed and **why** (avoid AI-generated filler paragraphs).
* Link any relevant issues (`Fixes #12`).
