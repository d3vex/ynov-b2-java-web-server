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

        return buildHardcodedConfig();
    }

    private static List<ServerConfig> buildHardcodedConfig() {
        RouteConfig apiRoute = new RouteConfig.Builder()
                .path("/api")
                .root("www")
                .defaultFile("index.html")
                .errorPage(404, "www/errors/custom_404.html")
                .errorPage(403, "www/errors/401.html")
                .build();

        RouteConfig filesRoute = new RouteConfig.Builder()
                .path("/files")
                .root("www")
                .directoryListing(true)
                .build();

        RouteConfig docsRoute = new RouteConfig.Builder()
                .path("/docs")
                .redirect("https://aaa.com/docs")
                .build();

        RouteConfig cgiRoute = new RouteConfig.Builder()
                .path("/cgi")
                .root("www/cgi")
                .defaultFile("index.py")
                .cgiExtensions(List.of(".py", ".sh"))
                .build();

        ServerConfig apiConfig = new ServerConfig.Builder()
                .host("0.0.0.0")
                .port(8888)
                .defaultServerRoot("www")
                .route("/api", apiRoute)
                .route("/files", filesRoute)
                .route("/docs", docsRoute)
                .route("/cgi", cgiRoute)
                .errorPage(404, "www/errors/404.html")
                .timeoutMs(60000)
                .build();

        ServerConfig adminConfig = new ServerConfig.Builder()
                .host("0.0.0.0")
                .port(8889)
                .defaultServerRoot("www/admin")
                .errorPage(403, "www/errors/401.html")
                .errorPage(404, "www/errors/404.html")
                .build();

        return List.of(apiConfig, adminConfig);
    }
}
