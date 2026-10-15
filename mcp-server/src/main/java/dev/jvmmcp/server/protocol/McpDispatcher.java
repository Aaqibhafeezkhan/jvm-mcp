package dev.jvmmcp.server.protocol;

import dev.jvmmcp.JvmMcp;
import dev.jvmmcp.core.util.SimpleJson;
import dev.jvmmcp.server.tool.ToolArgs;
import dev.jvmmcp.server.tool.ToolRegistry;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Handles JSON-RPC 2.0 protocol dispatching for MCP server.
 */
public class McpDispatcher {

    public static final String PROTOCOL_VERSION = "2024-11-05";

    private final ToolRegistry toolRegistry;
    private final String serverName;
    private final String serverVersion;

    public McpDispatcher(ToolRegistry toolRegistry) {
        this(toolRegistry, "jvm-mcp", JvmMcp.getVersion());
    }

    public McpDispatcher(ToolRegistry toolRegistry, String serverName, String serverVersion) {
        this.toolRegistry = toolRegistry;
        this.serverName = serverName;
        this.serverVersion = serverVersion;
    }

    /**
     * Parses a raw line of input and dispatches it as a JSON-RPC request.
     *
     * @param requestLine raw JSON string
     * @return response map if a response is expected, or empty for notifications
     */
    public Optional<Map<String, Object>> dispatch(String requestLine) {
        try {
            Map<String, Object> req = SimpleJson.parseObject(requestLine);
            return handleRequest(req);
        } catch (Throwable e) {
            System.err.println("[jvm-mcp] Error processing request: " + e.getMessage());
            return Optional.of(formatParseError(e.getMessage()));
        }
    }

    /**
     * Dispatches an already-parsed JSON-RPC request map.
     *
     * @param req request map
     * @return response map if a response is expected, or empty for notifications
     */
    public Optional<Map<String, Object>> handleRequest(Map<String, Object> req) {
        if (req == null) {
            return Optional.empty();
        }

        Object id = req.get("id");
        if (id == null) {
            // JSON-RPC 2.0: do not respond to notifications
            return Optional.empty();
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", id);

        try {
            String method = (String) req.get("method");

            if ("initialize".equals(method)) {
                response.put("result", Map.of(
                    "protocolVersion", PROTOCOL_VERSION,
                    "capabilities", Map.of("tools", Map.of()),
                    "serverInfo", Map.of("name", serverName, "version", serverVersion)
                ));
                return Optional.of(response);
            }

            if ("ping".equals(method)) {
                response.put("result", Map.of());
                return Optional.of(response);
            }

            if ("tools/list".equals(method)) {
                response.put("result", Map.of("tools", toolRegistry.getToolsList()));
                return Optional.of(response);
            }

            if ("tools/call".equals(method)) {
                Map<String, Object> params = ToolArgs.asMap(req.get("params"));
                if (params != null) {
                    String toolName = (String) params.get("name");
                    Map<String, Object> args = ToolArgs.asMap(params.get("arguments"));
                    response.put("result", toolRegistry.execute(toolName, args));
                    return Optional.of(response);
                } else {
                    response.put("error", Map.of("code", -32602, "message", "Invalid params"));
                    return Optional.of(response);
                }
            }

            response.put("error", Map.of("code", -32601, "message", "Method not found"));
            return Optional.of(response);
        } catch (Throwable e) {
            System.err.println("[jvm-mcp] Internal error handling request: " + e.getMessage());
            response.put("error", Map.of("code", -32603, "message", "Internal error: " + e.getMessage()));
            return Optional.of(response);
        }
    }

    public Map<String, Object> formatParseError(String message) {
        Map<String, Object> errResp = new LinkedHashMap<>();
        errResp.put("jsonrpc", "2.0");
        errResp.put("id", null);
        errResp.put("error", Map.of("code", -32700, "message", "Parse error: " + message));
        return errResp;
    }

    public Map<String, Object> formatMethodNotFound(Object id) {
        Map<String, Object> errResp = new LinkedHashMap<>();
        errResp.put("jsonrpc", "2.0");
        errResp.put("id", id);
        errResp.put("error", Map.of("code", -32601, "message", "Method not found"));
        return errResp;
    }

    public Map<String, Object> formatInvalidParams(Object id, String message) {
        Map<String, Object> errResp = new LinkedHashMap<>();
        errResp.put("jsonrpc", "2.0");
        errResp.put("id", id);
        errResp.put("error", Map.of("code", -32602, "message", message != null ? message : "Invalid params"));
        return errResp;
    }

    public Map<String, Object> formatInternalError(Object id, String message) {
        Map<String, Object> errResp = new LinkedHashMap<>();
        errResp.put("jsonrpc", "2.0");
        errResp.put("id", id);
        errResp.put("error", Map.of("code", -32603, "message", "Internal error: " + message));
        return errResp;
    }
}
