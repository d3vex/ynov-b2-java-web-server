package webserver;

import webserver.bootstrap.ServerFactory;
import webserver.config.ConfigLoader;
import webserver.config.ConfigLoader.ConfigLoadException;
import webserver.config.RouteConfig;
import webserver.config.ServerConfig;
import webserver.http.HttpMethod;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        try {
            List<ServerConfig> configs = loadConfig();
            ServerFactory factory = new ServerFactory();
            for (ServerConfig config : configs) {
                factory.addServer(config);
            }
            factory.startAll();

        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static List<ServerConfig> loadConfig() throws IOException {
        ConfigLoader loader = new ConfigLoader();
        Path configPath = Path.of("config/server.conf");

        if (Files.exists(configPath)) {
            try {
                return loader.load(configPath);
            } catch (ConfigLoadException e) {
                System.err.println("Config file has errors, falling back to hardcoded config: " + e.getMessage());
            }
        }

        return List.of();
    }
}
