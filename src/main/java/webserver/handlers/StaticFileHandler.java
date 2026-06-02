package webserver.handlers;

import webserver.config.RouteConfig;
import webserver.config.ServerConfig;
import webserver.filesystem.*;
import webserver.http.*;

import java.io.File;

public class StaticFileHandler {

    private final PathResolver pathResolver = new PathResolver();
    private final FileService fileService = new FileService();
    private final MimeTypeResolver mimeResolver = new MimeTypeResolver();
    private final DirectoryListingHandler directoryListing = new DirectoryListingHandler();
    private final ErrorHandler errorHandler = new ErrorHandler();
    private final CgiHandler cgiHandler = new CgiHandler();

    public HttpResponse handle(HttpRequest request, RouteConfig route, ServerConfig config) {
        if (route != null && route.getCgiExtensions() != null && !route.getCgiExtensions().isEmpty()) {
            HttpResponse cgiResponse = cgiHandler.handle(request, route, config);
            if (cgiResponse != null) return cgiResponse;
        }

        PathResolver.ResolvedPath resolved = pathResolver.resolve(request.getPath(), route, config);

        if (!resolved.secure()) {
            return errorHandler.handleError(HttpStatus.FORBIDDEN, config, request.getPath(), request.getMethod());
        }

        File file = resolved.file();

        if (fileService.isDirectory(file)) {
            boolean listing = route != null && route.isDirectoryListing();
            if (listing) {
                return directoryListing.handle(file, request.getPath());
            }

            String defaultFile = route != null && route.getDefaultFile() != null
                    ? route.getDefaultFile()
                    : "index.html";
            File defaultFileHandle = new File(file, defaultFile);

            if (fileService.exists(defaultFileHandle)) {
                if (route != null && route.hasCgiExtension(defaultFileHandle.getName())) {
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
