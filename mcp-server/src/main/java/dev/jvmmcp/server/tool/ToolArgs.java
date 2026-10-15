package dev.jvmmcp.server.tool;

import java.util.Map;

/**
 * Type-safe argument extraction utilities for MCP tool invocations.
 */
public final class ToolArgs {

    private ToolArgs() {
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> asMap(Object obj) {
        return obj instanceof Map<?, ?> map ? (Map<String, Object>) map : null;
    }

    public static String getStringArg(Map<String, Object> args, String key, boolean required) {
        if (args == null || !args.containsKey(key)) {
            if (required) {
                throw new IllegalArgumentException("Missing required argument: " + key);
            }
            return null;
        }
        Object val = args.get(key);
        if (val != null && !(val instanceof String)) {
            throw new IllegalArgumentException("Argument '" + key + "' must be a string");
        }
        return (String) val;
    }

    public static long getLongArg(Map<String, Object> args, String key, boolean required) {
        if (args == null || !args.containsKey(key)) {
            if (required) {
                throw new IllegalArgumentException("Missing required argument: " + key);
            }
            return 0L;
        }
        Object val = args.get(key);
        if (!(val instanceof Number)) {
            throw new IllegalArgumentException("Argument '" + key + "' must be a number");
        }
        return ((Number) val).longValue();
    }
}
