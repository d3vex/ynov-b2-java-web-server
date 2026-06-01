package webserver.http;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HttpRequest {

    private final HttpMethod method;
    private final String path;
    private final String httpVersion;
    private final HttpHeaders headers;
    private final Map<String, String> queryParameters;
    private final List<Cookie> cookies;
    private byte[] body;

    public HttpRequest(HttpMethod method, String path, String httpVersion, HttpHeaders headers) {
        this.method = method;
        this.httpVersion = httpVersion;
        this.headers = headers;
        this.cookies = new ArrayList<>(headers.getCookies());

        int qm = path.indexOf('?');
        if (qm >= 0) {
            this.path = path.substring(0, qm);
            this.queryParameters = parseQueryString(path.substring(qm + 1));
        } else {
            this.path = path;
            this.queryParameters = Map.of();
        }
    }

    public HttpRequest(HttpMethod method, String path, String httpVersion, HttpHeaders headers, byte[] body) {
        this(method, path, httpVersion, headers);
        this.body = body;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public String getHttpVersion() {
        return httpVersion;
    }

    public HttpHeaders getHeaders() {
        return headers;
    }

    public Map<String, String> getQueryParameters() {
        return queryParameters;
    }

    public String getQueryParameter(String name) {
        return queryParameters.get(name);
    }

    public List<Cookie> getCookies() {
        return cookies;
    }

    public Cookie getCookie(String name) {
        for (Cookie c : cookies) {
            if (c.getName().equals(name)) return c;
        }
        return null;
    }

    public byte[] getBody() {
        return body;
    }

    public String getBodyAsString() {
        if (body == null) return "";
        return new String(body, StandardCharsets.UTF_8);
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    private static Map<String, String> parseQueryString(String query) {
        Map<String, String> params = new LinkedHashMap<>();
        if (query == null || query.isBlank()) return params;
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int eq = pair.indexOf('=');
            String key = eq > 0 ? decode(pair.substring(0, eq)) : decode(pair);
            String value = eq > 0 ? decode(pair.substring(eq + 1)) : "";
            params.put(key, value);
        }
        return params;
    }

    private static String decode(String s) {
        return URLDecoder.decode(s, StandardCharsets.UTF_8);
    }
}
