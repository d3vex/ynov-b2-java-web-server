package webserver.filesystem;

import java.util.Map;

public class MimeTypeResolver {

    private static final Map<String, String> MIME_MAP = Map.ofEntries(
        Map.entry("html", "text/html; charset=utf-8"),
        Map.entry("htm", "text/html; charset=utf-8"),
        Map.entry("css", "text/css"),
        Map.entry("js", "application/javascript"),
        Map.entry("json", "application/json"),
        Map.entry("xml", "application/xml"),
        Map.entry("txt", "text/plain; charset=utf-8"),
        Map.entry("csv", "text/csv"),
        Map.entry("png", "image/png"),
        Map.entry("jpg", "image/jpeg"),
        Map.entry("jpeg", "image/jpeg"),
        Map.entry("gif", "image/gif"),
        Map.entry("svg", "image/svg+xml"),
        Map.entry("ico", "image/x-icon"),
        Map.entry("webp", "image/webp"),
        Map.entry("bmp", "image/bmp"),
        Map.entry("pdf", "application/pdf"),
        Map.entry("zip", "application/zip"),
        Map.entry("gz", "application/gzip"),
        Map.entry("tar", "application/x-tar"),
        Map.entry("mp3", "audio/mpeg"),
        Map.entry("mp4", "video/mp4"),
        Map.entry("webm", "video/webm"),
        Map.entry("woff", "font/woff"),
        Map.entry("woff2", "font/woff2"),
        Map.entry("ttf", "font/ttf"),
        Map.entry("otf", "font/otf"),
        Map.entry("eot", "application/vnd.ms-fontobject"),
        Map.entry("wasm", "application/wasm")
    );

    public String resolve(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "application/octet-stream";
        }
        String ext = filename.substring(dot + 1).toLowerCase();
        return MIME_MAP.getOrDefault(ext, "application/octet-stream");
    }
}
