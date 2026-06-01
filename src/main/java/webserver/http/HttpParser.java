package webserver.http;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class HttpParser {

    private RequestParserState state = RequestParserState.REQUEST_LINE;
    private final StringBuilder buffer = new StringBuilder();

    private HttpMethod method;
    private String path;
    private String httpVersion;
    private final List<String> headerLines = new ArrayList<>();
    private HttpHeaders headers;
    private byte[] body;
    private int contentLength = -1;

    private String errorReason;

    public RequestParserState getState() {
        return state;
    }

    public String getErrorReason() {
        return errorReason;
    }

    public void consume(String data) {
        if (state == RequestParserState.COMPLETE || state == RequestParserState.ERROR) {
            return;
        }
        buffer.append(data);
        run();
    }

    public void consume(ByteBuffer data) {
        byte[] bytes = new byte[data.remaining()];
        data.get(bytes);
        consume(new String(bytes, StandardCharsets.ISO_8859_1));
    }

    public HttpRequest build() {
        if (state != RequestParserState.COMPLETE) {
            return null;
        }
        return new HttpRequest(method, path, httpVersion, headers, body);
    }

    public void reset() {
        state = RequestParserState.REQUEST_LINE;
        buffer.setLength(0);
        method = null;
        path = null;
        httpVersion = null;
        headerLines.clear();
        headers = null;
        body = null;
        contentLength = -1;
        errorReason = null;
    }

    private void run() {
        switch (state) {
            case REQUEST_LINE -> parseRequestLine();
            case HEADERS -> parseHeaders();
            case BODY -> parseBody();
        }
    }

    private void parseRequestLine() {
        int idx = buffer.indexOf("\r\n");
        if (idx < 0) return;

        String line = buffer.substring(0, idx);
        buffer.delete(0, idx + 2);

        String[] parts = line.split(" ", 3);
        if (parts.length != 3) {
            error("Invalid request line: " + line);
            return;
        }

        try {
            method = HttpMethod.valueOf(parts[0].toUpperCase());
        } catch (IllegalArgumentException e) {
            error("Unknown method: " + parts[0]);
            return;
        }

        path = parts[1];

        if (!parts[2].startsWith("HTTP/")) {
            error("Invalid HTTP version: " + parts[2]);
            return;
        }
        httpVersion = parts[2];

        state = RequestParserState.HEADERS;
        run();
    }

    private void parseHeaders() {
        int idx = buffer.indexOf("\r\n\r\n");
        if (idx < 0) {
            int lineEnd = buffer.indexOf("\r\n");
            if (lineEnd >= 0) {
                headerLines.add(buffer.substring(0, lineEnd));
                buffer.delete(0, lineEnd + 2);
            }
            return;
        }

        String remainingHeaders = buffer.substring(0, idx);
        if (!remainingHeaders.isEmpty()) {
            String[] lines = remainingHeaders.split("\r\n");
            for (String line : lines) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    headerLines.add(trimmed);
                }
            }
        }
        buffer.delete(0, idx + 4);

        headers = HttpHeaders.parse(headerLines);

        String cl = headers.get(HttpHeaders.CONTENT_LENGTH);
        if (cl != null) {
            try {
                contentLength = Integer.parseInt(cl);
            } catch (NumberFormatException e) {
                error("Invalid Content-Length: " + cl);
                return;
            }
        }

        String te = headers.get(HttpHeaders.TRANSFER_ENCODING);
        boolean chunked = te != null && te.toLowerCase().contains("chunked");

        if (chunked) {
            state = RequestParserState.BODY;
            run();
        } else if (contentLength > 0) {
            state = RequestParserState.BODY;
            run();
        } else if (contentLength == 0) {
            body = new byte[0];
            state = RequestParserState.COMPLETE;
        } else {
            boolean hasBody = method == HttpMethod.POST || method == HttpMethod.PUT || method == HttpMethod.PATCH;
            body = hasBody ? new byte[0] : null;
            state = RequestParserState.COMPLETE;
        }
    }

    private void parseBody() {
        int available = buffer.length();
        if (contentLength > 0) {
            if (available < contentLength) return;
            body = buffer.substring(0, contentLength).getBytes(StandardCharsets.ISO_8859_1);
            buffer.delete(0, contentLength);
            state = RequestParserState.COMPLETE;
        } else {
            String raw = buffer.toString();
            if (raw.endsWith("\r\n\r\n") || raw.endsWith("\n\n")) {
                body = raw.getBytes(StandardCharsets.ISO_8859_1);
                buffer.setLength(0);
                state = RequestParserState.COMPLETE;
            }
        }
    }

    private void error(String reason) {
        state = RequestParserState.ERROR;
        errorReason = reason;
    }
}
