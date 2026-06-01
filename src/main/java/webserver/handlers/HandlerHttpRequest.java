package webserver.handlers;

import webserver.config.ServerConfig;
import webserver.http.HttpRequest;
import webserver.http.HttpResponse;
import webserver.http.HttpStatus;
import webserver.network.ClientConnection;
import webserver.network.SelectorManager;

public class HandlerHttpRequest {

    public static void process(ClientConnection connection) {
        try {
            HttpRequest request = connection.getCurrentRequest();
            ServerConfig config = connection.getServerConfig();

            if (request == null || config == null) {
                sendError(connection, HttpStatus.BAD_REQUEST);
                return;
            }

            RequestDispatcher dispatcher = new RequestDispatcher();
            HttpResponse response = dispatcher.dispatch(request, config);

            byte[] responseBytes = response.toBytes();
            connection.CreateWriteBuffer(responseBytes.length);
            connection.getWriteBuffer().put(responseBytes);

            connection.setPendingResponse(response);
            SelectorManager.getInstance().enableWrite(connection.getSocketChannel());

        } catch (Exception e) {
            System.err.println("Error processing request: " + e.getMessage());
            sendError(connection, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private static void sendError(ClientConnection connection, HttpStatus status) {
        ErrorHandler errorHandler = new ErrorHandler();
        ServerConfig config = connection.getServerConfig();
        HttpResponse response = errorHandler.handleError(status, config, null);

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
