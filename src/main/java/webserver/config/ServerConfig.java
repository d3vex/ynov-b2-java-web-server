package webserver.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import webserver.http.HttpMethod;

public class ServerConfig {

    private final String host;
    private final List<Integer> ports;
    private final Map<String, RouteConfig> routes;
    private final Map<Integer, String> errorPages;
    private final long clientBodyLimit;
    private final String defaultServerRoot;
    private final Long timeoutMs;

    public ServerConfig(String host, List<Integer> ports, Map<String, RouteConfig> routes,
                        Map<Integer, String> errorPages, long clientBodyLimit,
                        String defaultServerRoot, Long timeoutMs) {
        this.host = host;
        this.ports = ports;
        this.routes = routes;
        this.errorPages = errorPages;
        this.clientBodyLimit = clientBodyLimit;
        this.defaultServerRoot = defaultServerRoot;
        this.timeoutMs = timeoutMs;
    }

    public String getHost() {
        return host;
    }

    public List<Integer> getPorts() {
        return ports;
    }

    public Map<String, RouteConfig> getRoutes() {
        return routes;
    }

    public Map<Integer, String> getErrorPages() {
        return errorPages;
    }

    public long getClientBodyLimit() {
        return clientBodyLimit;
    }

    public String getDefaultServerRoot() {
        return defaultServerRoot;
    }

    public Long getTimeoutMs() {
        return timeoutMs;
    }

    public long resolveTimeout(String requestPath, HttpMethod method) {
        RouteConfig route = findRoute(requestPath);
        if (route != null && route.getTimeoutMs() != null) {
            return route.getTimeoutMs();
        }
        if (timeoutMs != null) {
            return timeoutMs;
        }
        return ConfigDefaults.TIMEOUT_MS;
    }

    public String retrieveErrorPagePath(int statusCode, String requestPath, HttpMethod method) {
        if (requestPath != null) {
            RouteConfig route = findRoute(requestPath);
            if (route != null) {
                String routeErrorPage = route.getErrorPages().get(statusCode);
                if (routeErrorPage != null) {
                    return routeErrorPage;
                }
            }
        }
        return errorPages.get(statusCode);
    }

    public RouteConfig findRoute(String requestPath) {
        RouteConfig bestMatch = null;
        int bestLength = -1;
        for (var entry : routes.entrySet()) {
            String routePath = entry.getValue().getPath();
            if (requestPath.equals(routePath) || requestPath.startsWith(routePath)) {
                if (routePath.length() > bestLength) {
                    bestLength = routePath.length();
                    bestMatch = entry.getValue();
                }
            }
        }
        return bestMatch;
    }

    public static class Builder {
        private String host = "0.0.0.0";
        private final List<Integer> ports = new ArrayList<>();
        private final Map<String, RouteConfig> routes = new HashMap<>();
        private final Map<Integer, String> errorPages = new HashMap<>();
        private long clientBodyLimit = ConfigDefaults.CLIENT_BODY_LIMIT;
        private String defaultServerRoot = ConfigDefaults.DEFAULT_SERVER_ROOT;
        private Long timeoutMs;

        public Builder host(String host) {
            this.host = host;
            return this;
        }

        public Builder port(int port) {
            this.ports.add(port);
            return this;
        }

        public Builder ports(List<Integer> ports) {
            this.ports.addAll(ports);
            return this;
        }

        public Builder route(String path, RouteConfig route) {
            this.routes.put(path, route);
            return this;
        }

        public Builder errorPage(int statusCode, String path) {
            this.errorPages.put(statusCode, path);
            return this;
        }

        public Builder clientBodyLimit(long limit) {
            this.clientBodyLimit = limit;
            return this;
        }

        public Builder defaultServerRoot(String root) {
            this.defaultServerRoot = root;
            return this;
        }

        public Builder timeoutMs(long timeoutMs) {
            this.timeoutMs = timeoutMs;
            return this;
        }

        public ServerConfig build() {
            if (ports.isEmpty()) {
                ports.add(8080);
            }
            return new ServerConfig(host, ports, routes, errorPages, clientBodyLimit,
                    defaultServerRoot, timeoutMs);
        }
    }
}
