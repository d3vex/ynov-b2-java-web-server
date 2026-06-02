package webserver.cgi;

import webserver.http.HttpHeaders;
import webserver.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ProcessOutputParser {

    public ParsedCgiOutput parse(byte[] rawOutput) {
        if (rawOutput == null || rawOutput.length == 0) {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "text/plain");
            return new ParsedCgiOutput(HttpStatus.OK, headers, new byte[0]);
        }

        String raw = new String(rawOutput, StandardCharsets.ISO_8859_1);

        int separatorIndex = raw.indexOf("\r\n\r\n");
        int separatorLen = 4;
        if (separatorIndex < 0) {
            separatorIndex = raw.indexOf("\n\n");
            separatorLen = 2;
        }

        List<String> headerLines;
        byte[] body;

        if (separatorIndex >= 0) {
            String headerSection = raw.substring(0, separatorIndex);
            headerLines = parseHeaderLines(headerSection);

            int bodyStart = separatorIndex + separatorLen;
            body = new byte[rawOutput.length - bodyStart];
            System.arraycopy(rawOutput, bodyStart, body, 0, body.length);
        } else {
            headerLines = new ArrayList<>();
            body = rawOutput;
        }

        HttpHeaders headers = HttpHeaders.parse(headerLines);

        HttpStatus status = HttpStatus.OK;
        String statusHeader = headers.get("Status");
        if (statusHeader != null) {
            int space = statusHeader.indexOf(' ');
            if (space > 0) {
                try {
                    int code = Integer.parseInt(statusHeader.substring(0, space).trim());
                    String reason = statusHeader.substring(space + 1).trim();
                    status = new HttpStatus(code, reason);
                } catch (NumberFormatException ignored) {
                }
            }
            headers.remove("Status");
        }

        if (headers.get("Content-Type") == null) {
            headers.set("Content-Type", "text/html");
        }

        return new ParsedCgiOutput(status, headers, body);
    }

    private List<String> parseHeaderLines(String section) {
        List<String> lines = new ArrayList<>();
        String[] parts = section.split("\r\n|\n");
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                lines.add(trimmed);
            }
        }
        return lines;
    }

    public record ParsedCgiOutput(HttpStatus status, HttpHeaders headers, byte[] body) {
    }
}
