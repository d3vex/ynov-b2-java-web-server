package webserver.network;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import webserver.handlers.HandlerHttpRequest;

public class ConnectionHandler {

    public static void handleRead(SelectionKey key) {
        ClientConnection connection = (ClientConnection) key.attachment();
        SocketChannel channel = connection.getSocketChannel();
        try {
            int bytesRead = channel.read(connection.getReadBuffer());
            if (bytesRead == -1) {
                System.out.println("Client disconnected: " + connection.getRemoteAddress());
                connection.close();
                key.cancel();
                return;
            } else if (bytesRead == 0) {
                return;
            }
            System.out.println("Read " + bytesRead + " bytes from " + connection.getRemoteAddress());
            connection.getReadBuffer().flip();
            if (connection.parse()) {
                connection.getReadBuffer().compact();
                HandlerHttpRequest.process(connection);
            } else {
                connection.getReadBuffer().compact();
            }

        } catch (Exception e) {
            System.err.println("Error reading from client: " + e.getMessage());
            connection.close();
            key.cancel();
        }
    }
}
