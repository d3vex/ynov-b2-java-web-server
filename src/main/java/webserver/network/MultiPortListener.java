package webserver.network;

import webserver.config.ServerConfig;

import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.util.ArrayList;
import java.util.List;

public class MultiPortListener {

    private final SelectorManager selectorManager;

    public MultiPortListener() {
        this.selectorManager = SelectorManager.getInstance();
    }

    public void bind(String host, List<Integer> ports, ServerConfig config) throws Exception {
        for (int port : ports) {
            ServerSocketChannel serverChannel = ServerSocketChannel.open();
            serverChannel.socket().bind(new InetSocketAddress(host, port));
            selectorManager.registerServer(serverChannel, config);
            System.out.println("Listening on " + host + ":" + port);
        }
    }
}
