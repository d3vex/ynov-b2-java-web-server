package webserver.handlers;

import webserver.config.ServerConfig;
import webserver.filesystem.FileService;
import webserver.filesystem.MimeTypeResolver;
import webserver.http.HttpHeaders;
import webserver.http.HttpMethod;
import webserver.http.HttpResponse;
import webserver.http.HttpStatus;

import java.io.File;
import java.util.Map;

public class ErrorHandler {

    private final FileService fileService = new FileService();
    private final MimeTypeResolver mimeResolver = new MimeTypeResolver();

    public HttpResponse handleError(HttpStatus status) {
        return handleError(status, (Map<Integer, String>) null);
    }

    public HttpResponse handleError(HttpStatus status, ServerConfig config, String requestPath, HttpMethod method) {
        String errorPath = config != null ? config.retrieveErrorPagePath(status.getCode(), requestPath, method) : null;
        return serveErrorPage(status, errorPath);
    }

    public HttpResponse handleError(HttpStatus status, Map<Integer, String> errorPages) {
        return serveErrorPage(status, errorPages != null ? errorPages.get(status.getCode()) : null);
    }

    private HttpResponse serveErrorPage(HttpStatus status, String errorPath) {
        if (errorPath != null) {
            File errorFile = new File(errorPath);
            if (fileService.exists(errorFile)) {
                try {
                    byte[] content = fileService.read(errorFile);
                    return HttpResponse.builder()
                            .status(status)
                            .header(HttpHeaders.CONTENT_TYPE, mimeResolver.resolve(errorFile.getName()))
                            .body(content)
                            .build();
                } catch (Exception e) {
                    System.err.println("Failed to read error page: " + e.getMessage());
                }
            }
        }
        String body = buildErrorPage(status);
        return HttpResponse.builder()
                .status(status)
                .header(HttpHeaders.CONTENT_TYPE, "text/html; charset=utf-8")
                .header(HttpHeaders.CONNECTION, "close")
                .body(body)
                .build();
    }

    private String buildErrorPage(HttpStatus status) {
        return buildErrorPage(status, status.getReasonPhrase());
    }

    private String buildErrorPage(HttpStatus status, String message) {
        int code = status.getCode();
        return """
                <!DOCTYPE html>
                <html>
                <head><title>%d %s</title></head>
                <body>
                <h1>%d %s</h1>
                <hr>
                </body>
                </html>
                """.formatted(code, message, code, message);
    }
}
