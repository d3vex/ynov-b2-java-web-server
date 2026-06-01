package webserver.bootstrap;

import webserver.config.ServerConfig;
import webserver.network.MultiPortListener;

public class ServerBootstrap {

    private final ServerConfig config;
    private final MultiPortListener multiPortListener;

    public ServerBootstrap(ServerConfig config) {
        this.config = config;
        this.multiPortListener = new MultiPortListener();
    }

    public ServerConfig getConfig() {
        return config;
    }

    public void bind() throws Exception {
        System.out.println("Binding server: " + config.getHost() + " ports: " + config.getPorts());
        multiPortListener.bind(config.getHost(), config.getPorts(), config);
    }
}
