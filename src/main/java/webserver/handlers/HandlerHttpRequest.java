package webserver.handlers;

import webserver.cgi.CgiAsyncExecutor;
import webserver.config.RouteConfig;
import webserver.config.ServerConfig;
import webserver.http.HttpMethod;
import webserver.http.HttpRequest;
import webserver.http.HttpResponse;
import webserver.http.HttpResponseBuilder;
import webserver.http.HttpStatus;
import webserver.network.ClientConnection;
import webserver.network.SelectorManager;
import webserver.session.CookieService;
import webserver.session.Session;
import webserver.session.SessionManager;

public class HandlerHttpRequest {

    private static final SessionManager sessionManager = new SessionManager();

    public static void process(ClientConnection connection) {

        HttpRequest request = connection.getCurrentRequest();
        ServerConfig config = connection.getServerConfig();

        if (request == null || config == null) {
            sendError(connection, HttpStatus.BAD_REQUEST, null);
            return;
        }

        long bodyLimit = config.resolveClientBodyLimit(request.getPath());
        byte[] body = request.getBody();
        if (body != null && body.length > bodyLimit) {
            sendError(connection, HttpStatus.PAYLOAD_TOO_LARGE, request);
            return;
        }

        HttpResponseBuilder sessionBuilder = HttpResponse.builder();
        Session session = sessionManager.getOrCreateSession(request, sessionBuilder);
        connection.setSession(session);

        if (!config.resolveCgiExtensions(request.getPath()).isEmpty()) {
            CgiAsyncExecutor.getInstance().execute(connection, request, config);
            return;
        }

        try {
            RequestDispatcher dispatcher = new RequestDispatcher();
            HttpResponse response = dispatcher.dispatch(request, config);
            attachSessionCookie(connection, response);
            connection.setPendingResponse(response);

            byte[] responseBytes = response.toBytes();
            connection.CreateWriteBuffer(responseBytes.length);
            connection.getWriteBuffer().put(responseBytes);

            SelectorManager.getInstance().enableWrite(connection.getSocketChannel());

        } catch (Exception e) {
            System.err.println("Error processing request: " + e.getMessage());
            sendError(connection, HttpStatus.INTERNAL_SERVER_ERROR, request);
        }
    }

    public static void attachSessionCookie(ClientConnection connection, HttpResponse response) {
        Session session = connection.getSession();
        if (session != null) {
            CookieService cookieService = new CookieService();
            response.getHeaders().addSetCookie(cookieService.createSessionCookie(session.getId()));
        }
    }

    private static void sendError(ClientConnection connection, HttpStatus status, HttpRequest request) {
        ErrorHandler errorHandler = new ErrorHandler();
        ServerConfig config = connection.getServerConfig();
        String requestPath = request != null ? request.getPath() : null;
        HttpMethod method = request != null ? request.getMethod() : null;
        HttpResponse response = errorHandler.handleError(status, config, requestPath, method);

        byte[] responseBytes = response.toBytes();
        connection.CreateWriteBuffer(responseBytes.length);
        connection.getWriteBuffer().put(responseBytes);

        connection.setPendingResponse(response);
        try {
            SelectorManager.getInstance().enableWrite(connection.getSocketChannel());
        } catch (Exception e) {
            System.err.println("Error enabling write: " + e.getMessage());
        }
    }
}
