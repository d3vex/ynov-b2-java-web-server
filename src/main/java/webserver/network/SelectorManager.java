package webserver.network;

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


    public void register(SocketChannel channel) throws Exception {
        channel.configureBlocking(false);

        ClientConnection clientConnection = new ClientConnection(channel);

        SelectionKey key = channel.register(selector, channel.validOps() | SelectionKey.OP_READ);
        key.attach(clientConnection);
        clientChannels.put(channel, key);
    }

    public void registerServer(ServerSocketChannel channel) throws Exception {
        channel.configureBlocking(false);
        SelectionKey key = channel.register(selector, SelectionKey.OP_ACCEPT);
        serverChannels.put(channel, key);
    }


    public void enableWrite(SocketChannel channel) throws Exception {
        SelectionKey key = clientChannels.get(channel);
        if (key != null) {
            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
        }
    }
    public void disableWrite(SocketChannel channel) throws Exception {
        SelectionKey key = clientChannels.get(channel);
        if (key != null) {
            key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
        }
    }
    
}
