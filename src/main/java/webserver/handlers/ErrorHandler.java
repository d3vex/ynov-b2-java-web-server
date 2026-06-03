package webserver.handlers;

import webserver.config.ServerConfig;
import webserver.errors.DefaultErrorPages;
import webserver.errors.ErrorPageResolver;
import webserver.errors.ServerExceptionMapper;
import webserver.filesystem.FileService;
import webserver.filesystem.MimeTypeResolver;
import webserver.http.HttpHeaders;
import webserver.http.HttpMethod;
import webserver.http.HttpResponse;
import webserver.http.HttpStatus;

import java.io.File;

public class ErrorHandler {

    private final DefaultErrorPages defaultPages = new DefaultErrorPages();
    private final ErrorPageResolver pageResolver = new ErrorPageResolver();
    private final ServerExceptionMapper exceptionMapper = new ServerExceptionMapper();
    private final FileService fileService = new FileService();
    private final MimeTypeResolver mimeResolver = new MimeTypeResolver();

    public HttpResponse handleError(HttpStatus status) {
        return serveErrorPage(status, null);
    }

    public HttpResponse handleError(HttpStatus status, ServerConfig config, String requestPath, HttpMethod method) {
        String errorPath = pageResolver.resolve(config, requestPath, method, status.getCode());
        return serveErrorPage(status, errorPath);
    }

    public HttpResponse handleError(Exception e, ServerConfig config, String requestPath, HttpMethod method) {
        HttpStatus status = exceptionMapper.toStatus(e);
        String errorPath = pageResolver.resolve(config, requestPath, method, status.getCode());
        return serveErrorPage(status, errorPath);
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
        String body = defaultPages.render(status);
        return HttpResponse.builder()
                .status(status)
                .header(HttpHeaders.CONTENT_TYPE, "text/html; charset=utf-8")
                .header(HttpHeaders.CONNECTION, "close")
                .body(body)
                .build();
    }
}
