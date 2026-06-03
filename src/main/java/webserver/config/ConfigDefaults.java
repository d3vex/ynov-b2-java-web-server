package webserver.config;

import webserver.http.HttpMethod;
import java.util.List;

public class ConfigDefaults {

    public static final long TIMEOUT_MS = 30000;
    public static final long CLIENT_BODY_LIMIT = 1024 * 1024;
    public static final String DEFAULT_SERVER_ROOT = "www";
    public static final String DEFAULT_FILE = "index.html";
    public static final boolean DIRECTORY_LISTING = false;
    public static final List<HttpMethod> DEFAULT_ALLOWED_METHODS = List.of(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.POST);

    private ConfigDefaults() {
    }
}
