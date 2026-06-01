package webserver;

import webserver.bootstrap.ServerFactory;
import webserver.config.RouteConfig;
import webserver.config.ServerConfig;
import webserver.http.HttpMethod;

public class Main {

    public static void main(String[] args) {
        try {
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

            ServerConfig apiConfig = new ServerConfig.Builder()
                    .host("0.0.0.0")
                    .port(8888)
                    .defaultServerRoot("www")
                    .route("/api", apiRoute)
                    .route("/files", filesRoute)
                    .route("/docs", docsRoute)
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


            new ServerFactory()
                    .addServer(apiConfig)
                    .addServer(adminConfig)
                    .startAll();

        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
