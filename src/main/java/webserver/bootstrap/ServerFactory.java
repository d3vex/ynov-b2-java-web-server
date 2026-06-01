package webserver.bootstrap;

import webserver.config.ServerConfig;
import webserver.network.EventLoop;

import java.util.ArrayList;
import java.util.List;

public class ServerFactory {

    private final List<ServerBootstrap> servers = new ArrayList<>();

    public ServerFactory addServer(ServerConfig config) {
        servers.add(new ServerBootstrap(config));
        return this;
    }

    public void startAll() throws Exception {
        if (servers.isEmpty()) {
            throw new IllegalStateException("No servers configured");
        }

        for (ServerBootstrap server : servers) {
            server.bind();
        }

        System.out.println("All servers bound. Starting event loop.");
        EventLoop eventLoop = new EventLoop();

        ShutdownManager shutdownManager = ShutdownManager.getInstance();
        shutdownManager.register(eventLoop);

        eventLoop.start();
    }
}
