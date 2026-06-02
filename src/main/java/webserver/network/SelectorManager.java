package webserver.network;

import webserver.config.ConfigDefaults;
import webserver.config.ServerConfig;
import webserver.http.HttpMethod;
import webserver.http.HttpResponse;

import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SelectorManager {

    private static SelectorManager instance;
    private Selector selector;
    private final Map<SocketChannel, SelectionKey> clientChannels = new ConcurrentHashMap<>();
    private final Map<ServerSocketChannel, SelectionKey> serverChannels = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<ClientConnection> pendingCgiResponses = new ConcurrentLinkedQueue<>();


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
        clientConnection.setTimeoutMs(config.resolveTimeout("/", HttpMethod.GET));
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


    public void addPendingCgi(ClientConnection connection) {
        pendingCgiResponses.add(connection);
    }

    public void flushPendingCgi() {
        ClientConnection conn;
        while ((conn = pendingCgiResponses.poll()) != null) {
            conn.setCgiResponsePending(false);
            HttpResponse response = conn.getPendingResponse();
            if (response != null) {
                byte[] responseBytes = response.toBytes();
                conn.CreateWriteBuffer(responseBytes.length);
                conn.getWriteBuffer().put(responseBytes);
            }
            SocketChannel channel = conn.getSocketChannel();
            SelectionKey key = clientChannels.get(channel);
            if (key != null && key.isValid()) {
                int ops = key.interestOps();
                if ((ops & SelectionKey.OP_WRITE) == 0) {
                    key.interestOps(ops | SelectionKey.OP_WRITE);
                }
            }
        }
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

    public void removeClientChannel(SocketChannel channel) {
        clientChannels.remove(channel);
    }

    public void disableWrite(SocketChannel channel) throws Exception {
        SelectionKey key = clientChannels.get(channel);
        if (key != null) {
            key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
        }
    }

    public void wakeup() {
        selector.wakeup();
    }

    public void shutdown() {
        selector.wakeup();
        closeChannels();
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
