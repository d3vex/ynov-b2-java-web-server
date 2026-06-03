package webserver.errors;

import webserver.config.ServerConfig;
import webserver.http.HttpMethod;

public class ErrorPageResolver {

    public String resolve(ServerConfig config, String requestPath, HttpMethod method, int statusCode) {
        if (config == null) return null;
        return config.retrieveErrorPagePath(statusCode, requestPath, method);
    }
}
