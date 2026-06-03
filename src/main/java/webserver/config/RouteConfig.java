package webserver.config;

import webserver.http.HttpMethod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RouteConfig {

    private final String path;
    private final List<HttpMethod> allowedMethods;
    private final String redirect;
    private final String root;
    private final String defaultFile;
    private final List<String> cgiExtensions;
    private final Boolean directoryListing;
    private final Long timeoutMs;
    private final Long clientBodyLimit;
    private final Map<Integer, String> errorPages;

    public RouteConfig(String path, List<HttpMethod> allowedMethods, String redirect,
                       String root, String defaultFile, List<String> cgiExtensions,
                       Boolean directoryListing, Long timeoutMs, Long clientBodyLimit,
                       Map<Integer, String> errorPages) {
        this.path = path;
        this.allowedMethods = allowedMethods;
        this.redirect = redirect;
        this.root = root;
        this.defaultFile = defaultFile;
        this.cgiExtensions = cgiExtensions;
        this.directoryListing = directoryListing;
        this.timeoutMs = timeoutMs;
        this.clientBodyLimit = clientBodyLimit;
        this.errorPages = errorPages;
    }

    public String getPath() {
        return path;
    }

    public List<HttpMethod> getAllowedMethods() {
        return allowedMethods;
    }

    public String getRedirect() {
        return redirect;
    }

    public String getRoot() {
        return root;
    }

    public String getDefaultFile() {
        return defaultFile;
    }

    public List<String> getCgiExtensions() {
        return cgiExtensions;
    }

    public boolean hasCgiExtension(String filename) {
        if (cgiExtensions == null) return false;
        int dot = filename.lastIndexOf('.');
        if (dot < 0) return false;
        String ext = filename.substring(dot);
        for (String configured : cgiExtensions) {
            if (configured.equalsIgnoreCase(ext)) return true;
        }
        return false;
    }

    public Boolean getDirectoryListing() {
        return directoryListing;
    }

    public Long getTimeoutMs() {
        return timeoutMs;
    }

    public Long getClientBodyLimit() {
        return clientBodyLimit;
    }

    public Map<Integer, String> getErrorPages() {
        return errorPages;
    }

    public static class Builder {
        private String path;
        private List<HttpMethod> allowedMethods;
        private String redirect;
        private String root;
        private String defaultFile = ConfigDefaults.DEFAULT_FILE;
        private List<String> cgiExtensions;
        private Boolean directoryListing;
        private Long timeoutMs;
        private Long clientBodyLimit;
        private final Map<Integer, String> errorPages = new HashMap<>();

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder allowedMethods(List<HttpMethod> allowedMethods) {
            this.allowedMethods = allowedMethods;
            return this;
        }

        public Builder redirect(String redirect) {
            this.redirect = redirect;
            return this;
        }

        public Builder root(String root) {
            this.root = root;
            return this;
        }

        public Builder defaultFile(String defaultFile) {
            this.defaultFile = defaultFile;
            return this;
        }

        public Builder cgiExtensions(List<String> cgiExtensions) {
            this.cgiExtensions = cgiExtensions;
            return this;
        }

        public Builder directoryListing(boolean directoryListing) {
            this.directoryListing = directoryListing;
            return this;
        }

        public Builder timeoutMs(long timeoutMs) {
            this.timeoutMs = timeoutMs;
            return this;
        }

        public Builder clientBodyLimit(long clientBodyLimit) {
            this.clientBodyLimit = clientBodyLimit;
            return this;
        }

        public Builder errorPage(int statusCode, String path) {
            this.errorPages.put(statusCode, path);
            return this;
        }

        public RouteConfig build() {
            return new RouteConfig(path, allowedMethods, redirect, root, defaultFile,
                    cgiExtensions, directoryListing, timeoutMs, clientBodyLimit, errorPages);
        }
    }
}
