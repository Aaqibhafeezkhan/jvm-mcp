package dev.jvmmcp.core.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SimpleJsonTest {

    @Test
    @DisplayName("toJson should throw IllegalArgumentException when exceeding max depth")
    void shouldRejectExcessiveSerializationDepth() {
        Map<String, Object> root = new java.util.HashMap<>();
        Map<String, Object> current = root;
        for (int i = 0; i < 120; i++) {
            Map<String, Object> next = new java.util.HashMap<>();
            current.put("child", next);
            current = next;
        }
        assertThatThrownBy(() -> SimpleJson.toJson(root))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("depth");
    }

    @Test
    @DisplayName("parseObject should parse complex nested JSON with strings, arrays, and numbers")
    void shouldParseNestedJsonObject() {
        String json = """
            {
                "serviceName": "order-service",
                "port": 8080,
                "negativeVal": -42,
                "floatingVal": 3.1415,
                "expVal": 1.2e3,
                "active": true,
                "disabled": false,
                "emptyValue": null,
                "tags": ["spring", "mcp", "jvm", true, false, null, 100],
                "config": {
                    "timeoutMs": 1500,
                    "ratio": 0.85
                },
                "standardString": "value",
                "emptyObj": {},
                "emptyArr": []
            }
            """;

        Map<String, Object> map = SimpleJson.parseObject(json);

        assertThat(map).isNotNull();
        assertThat(map.get("serviceName")).isEqualTo("order-service");
        assertThat(map.get("port")).isEqualTo(8080);
        assertThat(map.get("negativeVal")).isEqualTo(-42);
        assertThat(map.get("floatingVal")).isEqualTo(3.1415);
        assertThat(map.get("expVal")).isEqualTo(1200.0);
        assertThat(map.get("active")).isEqualTo(true);
        assertThat(map.get("disabled")).isEqualTo(false);
        assertThat(map.get("emptyValue")).isNull();
        assertThat(map.get("standardString")).isEqualTo("value");

        @SuppressWarnings("unchecked")
        List<Object> tags = (List<Object>) map.get("tags");
        assertThat(tags).hasSize(7);
        assertThat(tags.get(0)).isEqualTo("spring");
        assertThat(tags.get(3)).isEqualTo(true);
        assertThat(tags.get(4)).isEqualTo(false);
        assertThat(tags.get(5)).isNull();
        assertThat(tags.get(6)).isEqualTo(100);

        @SuppressWarnings("unchecked")
        Map<String, Object> emptyObj = (Map<String, Object>) map.get("emptyObj");
        assertThat(emptyObj).isEmpty();

        @SuppressWarnings("unchecked")
        List<Object> emptyArr = (List<Object>) map.get("emptyArr");
        assertThat(emptyArr).isEmpty();
    }

    @Test
    @DisplayName("parseObject should handle all escape sequences and unicode characters")
    void shouldHandleEscapeSequences() {
        String json = "{\"msg\": \"\\\" \\' \\\\ \\/ \\b \\f \\n \\r \\t \\u0041 \\z\"}";
        Map<String, Object> map = SimpleJson.parseObject(json);

        assertThat(map.get("msg")).isEqualTo("\" ' \\ / \b \f \n \r \t A z");
    }

    @Test
    @DisplayName("parse handles null and blank inputs gracefully")
    void shouldHandleNullAndBlankStrings() {
        assertThat(SimpleJson.parse(null)).isNull();
        assertThat(SimpleJson.parse("   ")).isNull();
    }

    @Test
    @DisplayName("parse numbers handles large long numbers and numbers exceeding long capacity")
    void shouldParseVariousNumberFormats() {
        assertThat(SimpleJson.parse("5000000000")).isEqualTo(5000000000L);
        assertThat(SimpleJson.parse("-0.5e-2")).isEqualTo(-0.005);
        assertThat(SimpleJson.parse("10000000000000000000000000000000000000")).isInstanceOf(Double.class);
    }

    @Test
    @DisplayName("parseObject throws IllegalArgumentException for non-object root")
    void shouldThrowWhenNotAnObject() {
        assertThatThrownBy(() -> SimpleJson.parseObject("[1, 2, 3]"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Expected JSON Object");

        assertThatThrownBy(() -> SimpleJson.parseObject("\"just a string\""))
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> SimpleJson.parseObject(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("parse throws IllegalArgumentException for syntax errors and unterminated tokens")
    void shouldThrowForMalformedJson() {
        // Unexpected starting character
        assertThatThrownBy(() -> SimpleJson.parse("@invalid"))
            .isInstanceOf(IllegalArgumentException.class);

        // Unterminated string
        assertThatThrownBy(() -> SimpleJson.parse("{\"key\": \"unterminated"))
            .isInstanceOf(IllegalArgumentException.class);

        // Missing colon in object
        assertThatThrownBy(() -> SimpleJson.parse("{\"key\" \"val\"}"))
            .isInstanceOf(IllegalArgumentException.class);

        // Missing comma in object
        assertThatThrownBy(() -> SimpleJson.parse("{\"a\": 1 \"b\": 2}"))
            .isInstanceOf(IllegalArgumentException.class);

        // Missing comma in array
        assertThatThrownBy(() -> SimpleJson.parse("[1 2 3]"))
            .isInstanceOf(IllegalArgumentException.class);

        // Invalid boolean and null tokens
        assertThatThrownBy(() -> SimpleJson.parse("truth"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SimpleJson.parse("nil"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("parse and toJson should round-trip successfully for strings with escape sequences and unicode")
    void shouldRoundTripStrings() {
        String original = "Here is a string with \n newline, \t tab, \u001b escape, \" quotes \", and unicode \u2603 \uD83D\uDCA9.";
        String json = SimpleJson.toJson(original);
        Object parsed = SimpleJson.parse(json);
        assertThat(parsed).isEqualTo(original);
    }

    @Test
    @DisplayName("parse should strictly reject non-standard single-quoted strings")
    void shouldRejectSingleQuotedStrings() {
        assertThatThrownBy(() -> SimpleJson.parse("'single'"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unexpected character");

        assertThatThrownBy(() -> SimpleJson.parse("{\"key\": 'value'}"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("parse should strictly reject trailing content after a valid JSON value")
    void shouldRejectTrailingContentAfterJsonValue() {
        assertThatThrownBy(() -> SimpleJson.parse("{\"jsonrpc\":\"2.0\"} basura"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Trailing content");

        assertThatThrownBy(() -> SimpleJson.parse("true anything"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Trailing content");

        assertThatThrownBy(() -> SimpleJson.parse("[1, 2, 3] trailing"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Trailing content");

        assertThatThrownBy(() -> SimpleJson.parse("\"valid string\" extra"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Trailing content");
    }

    public record SampleRecord(String title, int count) {}

    @Test
    @DisplayName("toJson should serialize public Java records cleanly")
    void shouldSerializeRecordWithoutRequiringAccessibleOverride() {
        SampleRecord record = new SampleRecord("metrics", 42);
        String json = SimpleJson.toJson(record);
        assertThat(json).contains("\"title\":\"metrics\"");
        assertThat(json).contains("\"count\":42");
    }
}

