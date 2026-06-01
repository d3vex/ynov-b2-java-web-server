package webserver.network;

import webserver.config.ConfigDefaults;
import webserver.config.ServerConfig;

import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SelectorManager {

    private static SelectorManager instance;
    private Selector selector;
    private Map<SocketChannel, SelectionKey> clientChannels = new HashMap<>();
    private Map<ServerSocketChannel, SelectionKey> serverChannels = new HashMap<>();


    private SelectorManager() {
        try {
            this.selector = Selector.open();

        } catch (Exception e) {
            throw new RuntimeException("Failed to open selector", e);
        }
    }

    public static synchronized SelectorManager getInstance() {
        if (instance == null) {
            instance = new SelectorManager();
        }
        return instance;
    }

    public int select() throws Exception {
        return selector.select();
    }

    public Set<SelectionKey> selectedKeys() {
        return selector.selectedKeys();
    }


    public void register(SocketChannel channel, ServerConfig config) throws Exception {
        channel.configureBlocking(false);

        ClientConnection clientConnection = new ClientConnection(channel);
        clientConnection.setTimeoutMs(config.resolveTimeout("/"));
        clientConnection.setServerConfig(config);

        SelectionKey key = channel.register(selector, SelectionKey.OP_READ);
        key.attach(clientConnection);
        clientChannels.put(channel, key);
    }

    public void registerServer(ServerSocketChannel channel, ServerConfig config) throws Exception {
        channel.configureBlocking(false);
        SelectionKey key = channel.register(selector, SelectionKey.OP_ACCEPT);
        key.attach(config);
        serverChannels.put(channel, key);
    }


    public void enableWrite(SocketChannel channel) throws Exception {
        SelectionKey key = clientChannels.get(channel);
        if (key != null) {
            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
        }
    }
    public Map<SocketChannel, SelectionKey> getClientChannels() {
        return clientChannels;
    }

    public void disableWrite(SocketChannel channel) throws Exception {
        SelectionKey key = clientChannels.get(channel);
        if (key != null) {
            key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
        }
    }

    public void closeChannels() {
        for (SocketChannel channel : clientChannels.keySet()) {
            try {
                channel.close();
            } catch (Exception e) {
                System.err.println("Error closing client channel: " + e.getMessage());
            }
        }
        clientChannels.clear();

        for (ServerSocketChannel channel : serverChannels.keySet()) {
            try {
                channel.close();
            } catch (Exception e) {
                System.err.println("Error closing server channel: " + e.getMessage());
            }
        }
        serverChannels.clear();
    }

}
