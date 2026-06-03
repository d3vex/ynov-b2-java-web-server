package webserver.handlers;

import webserver.config.RouteConfig;
import webserver.config.ServerConfig;
import webserver.filesystem.*;
import webserver.http.*;

import java.io.File;
import java.util.List;

public class StaticFileHandler {

    private final PathResolver pathResolver = new PathResolver();
    private final FileService fileService = new FileService();
    private final MimeTypeResolver mimeResolver = new MimeTypeResolver();
    private final DirectoryListingHandler directoryListing = new DirectoryListingHandler();
    private final ErrorHandler errorHandler = new ErrorHandler();
    private final CgiHandler cgiHandler = new CgiHandler();

    public HttpResponse handle(HttpRequest request, RouteConfig route, ServerConfig config) {
        List<String> cgiExtensions = config.resolveCgiExtensions(request.getPath());
        if (!cgiExtensions.isEmpty()) {
            HttpResponse cgiResponse = cgiHandler.handle(request, route, config);
            if (cgiResponse != null) return cgiResponse;
        }

        PathResolver.ResolvedPath resolved = pathResolver.resolve(request.getPath(), route, config);

        if (!resolved.secure()) {
            return errorHandler.handleError(HttpStatus.FORBIDDEN, config, request.getPath(), request.getMethod());
        }

        File file = resolved.file();

        if (fileService.isDirectory(file)) {
            if (config.resolveDirectoryListing(request.getPath())) {
                return directoryListing.handle(file, request.getPath());
            }

            String defaultFile = route != null && route.getDefaultFile() != null
                    ? route.getDefaultFile()
                    : "index.html";
            File defaultFileHandle = new File(file, defaultFile);

            if (fileService.exists(defaultFileHandle)) {
                if (hasCgiExtension(defaultFileHandle.getName(), cgiExtensions)) {
                    String scriptName = request.getPath().endsWith("/")
                            ? request.getPath() + defaultFileHandle.getName()
                            : request.getPath() + "/" + defaultFileHandle.getName();
                    return cgiHandler.handleFile(request, route, config, defaultFileHandle, scriptName);
                }
                return serveFile(defaultFileHandle);
            }

            return errorHandler.handleError(HttpStatus.FORBIDDEN, config, request.getPath(), request.getMethod());
        }

        if (!fileService.exists(file)) {
            return errorHandler.handleError(HttpStatus.NOT_FOUND, config, request.getPath(), request.getMethod());
        }

        return serveFile(file);
    }

    private boolean hasCgiExtension(String filename, List<String> extensions) {
        if (extensions == null || extensions.isEmpty()) return false;
        int dot = filename.lastIndexOf('.');
        if (dot < 0) return false;
        String ext = filename.substring(dot);
        for (String configured : extensions) {
            if (configured.equalsIgnoreCase(ext)) return true;
        }
        return false;
    }

    private HttpResponse serveFile(File file) {
        try {
            byte[] content = fileService.read(file);
            String mime = mimeResolver.resolve(file.getName());

            return HttpResponse.builder()
                    .status(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_TYPE, mime)
                    .body(content)
                    .build();
        } catch (Exception e) {
            return errorHandler.handleError(HttpStatus.INTERNAL_SERVER_ERROR, null, null, null);
        }
    }
}
