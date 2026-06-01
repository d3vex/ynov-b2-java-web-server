package webserver.handlers;

import webserver.config.RouteConfig;
import webserver.config.ServerConfig;
import webserver.http.*;

public class RequestDispatcher {

    private final GetHandler getHandler = new GetHandler();
    private final PostHandler postHandler = new PostHandler();
    private final DeleteHandler deleteHandler = new DeleteHandler();
    private final ErrorHandler errorHandler = new ErrorHandler();
    private final StaticFileHandler staticFileHandler = new StaticFileHandler();

    public HttpResponse dispatch(HttpRequest request, ServerConfig config) {
        try {
            String path = request.getPath();
            RouteConfig route = config.findRoute(path);

            if (route != null) {
                if (route.getRedirect() != null) {
                    return HttpResponse.builder()
                            .status(HttpStatus.REDIRECT_FOUND)
                            .header(HttpHeaders.LOCATION, route.getRedirect())
                            .build();
                }

                if (route.getAllowedMethods() != null
                        && !route.getAllowedMethods().contains(request.getMethod())) {
                    return errorHandler.handleError(HttpStatus.METHOD_NOT_ALLOWED, config, path);
                }
            }

            return switch (request.getMethod()) {
                case GET, HEAD -> {
                    HttpResponse response = staticFileHandler.handle(request, route, config);
                    if (request.getMethod() == HttpMethod.HEAD) {
                        response = new HttpResponse(
                                response.getHttpVersion(),
                                response.getStatusCode(),
                                response.getHeaders(),
                                null
                        );
                    }
                    yield response;
                }
                case POST -> {
                    if (route != null && route.getRoot() != null) {
                        yield staticFileHandler.handle(request, route, config);
                    }
                    yield postHandler.handle(request);
                }
                case DELETE -> deleteHandler.handle(request);
                default -> errorHandler.handleError(HttpStatus.METHOD_NOT_ALLOWED, config, path);
            };

        } catch (Exception e) {
            System.err.println("Dispatch error: " + e.getMessage());
            return errorHandler.handleError(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
