package webserver.handlers;

import webserver.filesystem.UploadStorage;
import webserver.http.HttpHeaders;
import webserver.http.HttpRequest;
import webserver.http.MultipartParser;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class UploadHandler {

    private final MultipartParser parser = new MultipartParser();
    private final UploadStorage storage = new UploadStorage();

    public record FileInfo(String fieldName, String originalName, String tempPath, String contentType) {}

    public record UploadResult(Map<String, String> fields, List<FileInfo> files, byte[] stdinBody) {}

    public UploadResult handle(HttpRequest request) throws Exception {
        String contentType = request.getHeaders().get(HttpHeaders.CONTENT_TYPE);
        if (contentType == null || !contentType.toLowerCase().startsWith("multipart/form-data")) {
            byte[] body = request.getBody();
            return new UploadResult(Map.of(), List.of(), body != null ? body : new byte[0]);
        }

        byte[] rawBody = request.getBody();
        List<MultipartParser.Part> parts = parser.parse(rawBody, contentType);

        Map<String, String> fields = new LinkedHashMap<>();
        List<FileInfo> files = new ArrayList<>();

        for (MultipartParser.Part part : parts) {
            if (part.filename() != null) {
                UploadStorage.StoredFile stored = storage.store(part.filename(), part.data(), part.contentType());
                files.add(new FileInfo(part.name(), stored.originalName(), stored.tempPath(), stored.contentType()));
            } else {
                fields.put(part.name(), new String(part.data(), StandardCharsets.UTF_8));
            }
        }

        byte[] stdinBody = buildStdinBody(fields);
        return new UploadResult(fields, files, stdinBody);
    }

    public void cleanup() {
        storage.cleanup();
    }

    private byte[] buildStdinBody(Map<String, String> fields) {
        if (fields.isEmpty()) return new byte[0];
        StringBuilder sb = new StringBuilder();
        for (var entry : fields.entrySet()) {
            if (!sb.isEmpty()) sb.append('&');
            sb.append(urlEncode(entry.getKey())).append('=').append(urlEncode(entry.getValue()));
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String urlEncode(String s) {
        try {
            return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s;
        }
    }
}
