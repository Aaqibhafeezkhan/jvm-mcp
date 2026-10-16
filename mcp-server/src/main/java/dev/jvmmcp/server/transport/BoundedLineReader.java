package dev.jvmmcp.server.transport;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * A streaming bounded line reader that reads UTF-8 lines from an {@link InputStream}.
 * Reads into a fixed-size internal buffer, counts accumulated line bytes against {@code maxBytes},
 * and if an oversized line is encountered, drains the remaining line from the stream up to the
 * next newline or EOF and throws {@link LineTooLargeException}.
 * This avoids memory exhaustion (OOM) before a full line can be read into memory.
 */
public class BoundedLineReader implements Closeable {

    public static final int DEFAULT_BUFFER_SIZE = 8192;

    private final InputStream in;
    private final int maxBytes;
    private final byte[] buffer;
    private int bufferPos = 0;
    private int bufferCount = 0;
    private boolean closed = false;

    public BoundedLineReader(InputStream in, int maxBytes) {
        this(in, maxBytes, DEFAULT_BUFFER_SIZE);
    }

    public BoundedLineReader(InputStream in, int maxBytes, int bufferSize) {
        if (in == null) {
            throw new IllegalArgumentException("InputStream cannot be null");
        }
        if (maxBytes <= 0) {
            throw new IllegalArgumentException("maxBytes must be positive");
        }
        if (bufferSize <= 0) {
            throw new IllegalArgumentException("bufferSize must be positive");
        }
        this.in = in;
        this.maxBytes = maxBytes;
        this.buffer = new byte[bufferSize];
    }

    /**
     * Reads a line of text terminated by '\n' or '\r\n', or reaching EOF.
     *
     * @return the line as a UTF-8 String without line-termination characters,
     *         or {@code null} if the end of the stream has been reached without reading any bytes.
     * @throws LineTooLargeException if the line exceeds {@code maxBytes}. Remaining bytes of the
     *                               oversized line are drained from the stream so subsequent lines
     *                               can be read cleanly.
     * @throws IOException           if an I/O error occurs.
     */
    public String readLine() throws IOException {
        if (closed) {
            throw new IOException("Stream closed");
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream(Math.min(1024, maxBytes));
        int accumulatedBytes = 0;

        while (true) {
            if (bufferPos >= bufferCount) {
                bufferCount = in.read(buffer);
                bufferPos = 0;
                if (bufferCount == -1) {
                    if (accumulatedBytes == 0) {
                        return null;
                    }
                    return finishLine(out);
                }
            }

            while (bufferPos < bufferCount) {
                byte b = buffer[bufferPos++];
                if (b == '\n') {
                    return finishLine(out);
                }

                accumulatedBytes++;
                if (accumulatedBytes > maxBytes) {
                    drainOversizedLine();
                    throw new LineTooLargeException(
                        "Request line exceeds maximum limit of " + maxBytes + " bytes",
                        maxBytes
                    );
                }

                out.write(b);
            }
        }
    }

    private String finishLine(ByteArrayOutputStream out) {
        byte[] bytes = out.toByteArray();
        int len = bytes.length;
        if (len > 0 && bytes[len - 1] == '\r') {
            len--;
        }
        return new String(bytes, 0, len, StandardCharsets.UTF_8);
    }

    /**
     * Discards all bytes until the next '\n' or EOF so that the stream stays aligned
     * for subsequent requests.
     */
    private void drainOversizedLine() throws IOException {
        while (true) {
            if (bufferPos >= bufferCount) {
                bufferCount = in.read(buffer);
                bufferPos = 0;
                if (bufferCount == -1) {
                    return;
                }
            }
            while (bufferPos < bufferCount) {
                if (buffer[bufferPos++] == '\n') {
                    return;
                }
            }
        }
    }

    @Override
    public void close() throws IOException {
        closed = true;
        in.close();
    }
}
