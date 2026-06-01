package webserver.network;

import webserver.config.ServerConfig;

import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class ConnectionAcceptor {

    private final SelectorManager selectorManager;

    public ConnectionAcceptor(SelectorManager selectorManager) {
        this.selectorManager = selectorManager;
    }

    public void accept(SelectionKey key) {
        try {
            ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
            SocketChannel clientChannel = serverChannel.accept();
            if (clientChannel != null) {
                System.out.println("Accepted connection from " + clientChannel.getRemoteAddress());

                ServerConfig config = (ServerConfig) key.attachment();
                selectorManager.register(clientChannel, config);
            }
        } catch (Exception e) {
            System.err.println("Error accepting connection: " + e.getMessage());
        }
    }
}
