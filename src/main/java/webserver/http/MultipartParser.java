package webserver.http;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MultipartParser {

    public record Part(String name, String filename, String contentType, byte[] data) {}

    public List<Part> parse(byte[] body, String contentTypeHeader) {
        List<Part> parts = new ArrayList<>();
        if (body == null || body.length == 0 || contentTypeHeader == null) return parts;

        String boundary = extractBoundary(contentTypeHeader);
        if (boundary == null) return parts;

        String raw = new String(body, StandardCharsets.ISO_8859_1);
        String delim = "--" + boundary;
        int start = raw.indexOf(delim);
        if (start < 0) return parts;

        start += delim.length();
        while (true) {
            if (start + 2 > raw.length()) break;
            if (raw.startsWith("--", start)) break;
            if (raw.charAt(start) == '\r' && start + 1 < raw.length()) start++;
            if (raw.charAt(start) == '\n') start++;

            int headerEnd = raw.indexOf("\r\n\r\n", start);
            if (headerEnd < 0) break;

            String headerBlock = raw.substring(start, headerEnd);
            String name = null;
            String filename = null;
            String contentType = null;

            for (String line : headerBlock.split("\r\n")) {
                if (line.toLowerCase().startsWith("content-disposition:")) {
                    name = extractParam(line, "name");
                    filename = extractParam(line, "filename");
                } else if (line.toLowerCase().startsWith("content-type:")) {
                    contentType = line.substring("content-type:".length()).trim();
                }
            }

            int dataStart = headerEnd + 4;

            String endDelim = "\r\n" + delim;
            int dataEnd = raw.indexOf(endDelim, dataStart);
            if (dataEnd < 0) {
                dataEnd = raw.indexOf(delim, dataStart);
                if (dataEnd < 0) break;
            }

            byte[] data = body != null ? java.util.Arrays.copyOfRange(body, dataStart, dataEnd) : new byte[0];

            if (name != null) {
                parts.add(new Part(name, filename, contentType, data));
            }

            start = dataEnd + endDelim.length();
            if (start > raw.length()) break;
        }

        return parts;
    }

    private String extractBoundary(String contentType) {
        for (String part : contentType.split(";")) {
            String trimmed = part.trim();
            if (trimmed.toLowerCase().startsWith("boundary=")) {
                return trimmed.substring("boundary=".length()).trim();
            }
        }
        return null;
    }

    private String extractParam(String header, String paramName) {
        String lower = header.toLowerCase();
        int idx = lower.indexOf(paramName + "=");
        if (idx < 0) return null;
        int start = idx + paramName.length() + 1;
        if (start >= header.length()) return null;
        String value;
        if (header.charAt(start) == '"') {
            int end = header.indexOf('"', start + 1);
            if (end < 0) return null;
            value = header.substring(start + 1, end);
        } else {
            int end = header.indexOf(';', start);
            if (end < 0) end = header.length();
            value = header.substring(start, end).trim();
        }
        return value.isEmpty() ? null : value;
    }
}
