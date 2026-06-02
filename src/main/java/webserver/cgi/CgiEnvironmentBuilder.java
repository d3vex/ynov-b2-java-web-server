package webserver.cgi;

import webserver.config.RouteConfig;
import webserver.config.ServerConfig;
import webserver.http.HttpHeaders;
import webserver.http.HttpRequest;

import java.util.LinkedHashMap;
import java.util.Map;

public class CgiEnvironmentBuilder {

    public Map<String, String> build(HttpRequest request, RouteConfig route,
                                     ServerConfig config, String scriptName,
                                     String pathInfo, int port) {
        Map<String, String> env = new LinkedHashMap<>();

        String rootDir = route != null && route.getRoot() != null
                ? route.getRoot()
                : config.getDefaultServerRoot();

        env.put("GATEWAY_INTERFACE", "CGI/1.1");
        env.put("SERVER_PROTOCOL", request.getHttpVersion());
        env.put("SERVER_SOFTWARE", "ynov-java-server/1.0");
        env.put("SERVER_NAME", config.getHost());
        env.put("SERVER_PORT", String.valueOf(port));
        env.put("REQUEST_METHOD", request.getMethod().name());
        env.put("SCRIPT_NAME", scriptName);

        if (pathInfo != null && !pathInfo.isEmpty()) {
            env.put("PATH_INFO", pathInfo);
            env.put("PATH_TRANSLATED", rootDir + pathInfo);
        }

        String qs = request.getRawQueryString();
        env.put("QUERY_STRING", qs != null ? qs : "");

        String contentType = request.getHeaders().get(HttpHeaders.CONTENT_TYPE);
        if (contentType != null) {
            env.put("CONTENT_TYPE", contentType);
        }

        byte[] body = request.getBody();
        int contentLength = (body != null) ? body.length : 0;
        if (contentLength > 0) {
            env.put("CONTENT_LENGTH", String.valueOf(contentLength));
        } else {
            String cl = request.getHeaders().get(HttpHeaders.CONTENT_LENGTH);
            if (cl != null) {
                env.put("CONTENT_LENGTH", cl);
            }
        }

        for (String key : request.getHeaders().names()) {
            if ("Content-Type".equalsIgnoreCase(key) || "Content-Length".equalsIgnoreCase(key)) {
                continue;
            }
            String upper = key.toUpperCase().replace('-', '_');
            env.put("HTTP_" + upper, request.getHeaders().get(key));
        }

        return env;
    }
}
