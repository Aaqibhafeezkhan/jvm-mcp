package dev.jvmmcp.server.tool;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Manages registration and lookup of ToolHandlers and delegates execution.
 */
public class ToolRegistry {

    private final List<ToolHandler> registeredHandlers = new ArrayList<>();
    private final Map<String, ToolHandler> handlerMap = new LinkedHashMap<>();

    public void register(ToolHandler handler) {
        if (handler == null) {
            return;
        }
        registeredHandlers.add(handler);
        for (String name : handler.toolNames()) {
            handlerMap.put(name, handler);
        }
    }

    public Optional<ToolHandler> getHandler(String toolName) {
        if (toolName == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(handlerMap.get(toolName));
    }

    public List<Map<String, Object>> getToolsList() {
        List<Map<String, Object>> tools = new ArrayList<>();
        for (ToolHandler handler : registeredHandlers) {
            tools.addAll(handler.toolDefinitions());
        }
        return tools;
    }

    public Map<String, Object> execute(String toolName, Map<String, Object> arguments) {
        ToolHandler handler = toolName != null ? handlerMap.get(toolName) : null;
        if (handler == null) {
            return Map.of(
                "content", List.of(Map.of("type", "text", "text", "Unknown tool: " + toolName)),
                "isError", true
            );
        }

        try {
            return handler.execute(toolName, arguments);
        } catch (Exception e) {
            return Map.of(
                "content", List.of(Map.of("type", "text", "text", "Error executing tool: " + e.getMessage())),
                "isError", true
            );
        }
    }
}
