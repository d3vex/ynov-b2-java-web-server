package webserver.handlers;

import java.util.List;

import webserver.config.RouteConfig;
import webserver.config.ServerConfig;
import webserver.http.*;

public class RequestDispatcher {

    private final ErrorHandler errorHandler = new ErrorHandler();
    private final StaticFileHandler staticFileHandler = new StaticFileHandler();
    private final CgiHandler cgiHandler = new CgiHandler();

    public HttpResponse dispatch(HttpRequest request, ServerConfig config) {
        try {
            String path = request.getPath();
            RouteConfig route = config.findRoute(path);

            List<HttpMethod> allowedMethods = config.resolveAllowedMethods(path);
            if (!allowedMethods.contains(request.getMethod())) {
                if (request.getMethod() != HttpMethod.HEAD || !allowedMethods.contains(HttpMethod.GET)) {
                    return errorHandler.handleError(HttpStatus.METHOD_NOT_ALLOWED, config, path, request.getMethod());
                }
            }

            if (route != null && route.getRedirect() != null) {
                return HttpResponse.builder()
                        .status(HttpStatus.REDIRECT_FOUND)
                        .header(HttpHeaders.LOCATION, route.getRedirect())
                        .build();
            }

            if (route != null && route.getRoot() != null) {
                HttpResponse response = staticFileHandler.handle(request, route, config);
                return wrapHead(request, response);
            }

            return wrapHead(request, staticFileHandler.handle(request, null, config));

        } catch (Exception e) {
            System.err.println("Dispatch error: " + e.getMessage());
            return errorHandler.handleError(HttpStatus.INTERNAL_SERVER_ERROR, config, request.getPath(), request.getMethod());
        }
    }

    private HttpResponse wrapHead(HttpRequest request, HttpResponse response) {
        if (request.getMethod() == HttpMethod.HEAD) {
            return new HttpResponse(
                    response.getHttpVersion(),
                    response.getStatusCode(),
                    response.getHeaders(),
                    null
            );
        }
        return response;
    }
}
