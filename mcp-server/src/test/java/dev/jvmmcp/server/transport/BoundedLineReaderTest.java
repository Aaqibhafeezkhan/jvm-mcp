package dev.jvmmcp.server.transport;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BoundedLineReaderTest {

    @Test
    @DisplayName("reads simple lines terminated by newline")
    void testReadsSimpleLines() throws Exception {
        String input = "line1\nline2\nline3\n";
        try (BoundedLineReader reader = new BoundedLineReader(
            new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)), 1024)) {
            assertThat(reader.readLine()).isEqualTo("line1");
            assertThat(reader.readLine()).isEqualTo("line2");
            assertThat(reader.readLine()).isEqualTo("line3");
            assertThat(reader.readLine()).isNull();
        }
    }

    @Test
    @DisplayName("strips trailing carriage return on CRLF")
    void testStripsCarriageReturn() throws Exception {
        String input = "hello world\r\nnext line\r\n";
        try (BoundedLineReader reader = new BoundedLineReader(
            new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)), 1024)) {
            assertThat(reader.readLine()).isEqualTo("hello world");
            assertThat(reader.readLine()).isEqualTo("next line");
            assertThat(reader.readLine()).isNull();
        }
    }

    @Test
    @DisplayName("handles empty lines and lines reaching EOF without newline")
    void testEmptyLinesAndEofWithoutNewline() throws Exception {
        String input = "\n\nlast line";
        try (BoundedLineReader reader = new BoundedLineReader(
            new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)), 1024)) {
            assertThat(reader.readLine()).isEmpty();
            assertThat(reader.readLine()).isEmpty();
            assertThat(reader.readLine()).isEqualTo("last line");
            assertThat(reader.readLine()).isNull();
        }
    }

    @Test
    @DisplayName("counts UTF-8 multi-byte characters accurately by bytes")
    void testCountsUtf8BytesAccurately() throws Exception {
        // "ñ" is 2 bytes in UTF-8
        String input = "ñññ\n"; // 6 bytes + 1 newline
        // If maxBytes is 5, it should exceed the limit even though char count is 3
        try (BoundedLineReader reader = new BoundedLineReader(
            new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)), 5)) {
            assertThatThrownBy(reader::readLine)
                .isInstanceOf(LineTooLargeException.class);
        }

        // If maxBytes is 6, it should succeed
        try (BoundedLineReader reader = new BoundedLineReader(
            new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)), 6)) {
            assertThat(reader.readLine()).isEqualTo("ñññ");
        }
    }

    @Test
    @DisplayName("drains oversized line and cleanly reads subsequent line")
    void testDrainsOversizedLineAndRecovers() throws Exception {
        // Line 1 is 20 'a's (limit 10)
        // Line 2 is "valid line"
        String input = "a".repeat(20) + "\nvalid line\n";
        try (BoundedLineReader reader = new BoundedLineReader(
            new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)), 10, 4)) {
            
            assertThatThrownBy(reader::readLine)
                .isInstanceOf(LineTooLargeException.class)
                .hasMessageContaining("10 bytes");

            // Subsequent read MUST cleanly return line 2
            assertThat(reader.readLine()).isEqualTo("valid line");
            assertThat(reader.readLine()).isNull();
        }
    }

    @Test
    @DisplayName("validates constructor parameters")
    void testConstructorValidation() {
        assertThatThrownBy(() -> new BoundedLineReader(null, 100))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new BoundedLineReader(new ByteArrayInputStream(new byte[0]), 0))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new BoundedLineReader(new ByteArrayInputStream(new byte[0]), -1))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new BoundedLineReader(new ByteArrayInputStream(new byte[0]), 10, 0))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("close closes stream and rejects subsequent reads")
    void testClose() throws Exception {
        BoundedLineReader reader = new BoundedLineReader(new ByteArrayInputStream("data\n".getBytes(StandardCharsets.UTF_8)), 100);
        reader.close();
        assertThatThrownBy(reader::readLine)
            .isInstanceOf(IOException.class)
            .hasMessageContaining("Stream closed");
    }
}
