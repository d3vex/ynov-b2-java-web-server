package webserver.handlers;

import webserver.cgi.CgiAsyncExecutor;
import webserver.config.RouteConfig;
import webserver.config.ServerConfig;
import webserver.http.HttpMethod;
import webserver.http.HttpRequest;
import webserver.http.HttpResponse;
import webserver.http.HttpStatus;
import webserver.network.ClientConnection;
import webserver.network.SelectorManager;

public class HandlerHttpRequest {

    public static void process(ClientConnection connection) {

        HttpRequest request = connection.getCurrentRequest();
        ServerConfig config = connection.getServerConfig();

        if (request == null || config == null) {
            sendError(connection, HttpStatus.BAD_REQUEST, null);
            return;
        }

        RouteConfig route = config.findRoute(request.getPath());
        if (route != null && route.getCgiExtensions() != null && !route.getCgiExtensions().isEmpty()) {
            CgiAsyncExecutor.getInstance().execute(connection, request, config);
            return;
        }

        try {
            RequestDispatcher dispatcher = new RequestDispatcher();
            HttpResponse response = dispatcher.dispatch(request, config);
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
