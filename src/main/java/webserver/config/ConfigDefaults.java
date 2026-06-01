package webserver.config;

public class ConfigDefaults {

    public static final long TIMEOUT_MS = 30000;
    public static final long CLIENT_BODY_LIMIT = 1024 * 1024;
    public static final String DEFAULT_SERVER_ROOT = "www";
    public static final String DEFAULT_FILE = "index.html";
    public static final boolean DIRECTORY_LISTING = false;

    private ConfigDefaults() {
    }
}
