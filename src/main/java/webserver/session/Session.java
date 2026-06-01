package webserver.session;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Session {

    private final String id;
    private final Map<String, String> attributes;
    private final long createdAt;
    private long lastAccessedAt;

    public Session(String id) {
        this.id = id;
        this.attributes = new ConcurrentHashMap<>();
        this.createdAt = System.currentTimeMillis();
        this.lastAccessedAt = this.createdAt;
    }

    public String getId() {
        return id;
    }

    public String getAttribute(String key) {
        return attributes.get(key);
    }

    public void setAttribute(String key, String value) {
        attributes.put(key, value);
    }

    public void removeAttribute(String key) {
        attributes.remove(key);
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getLastAccessedAt() {
        return lastAccessedAt;
    }

    public void updateLastAccessedAt() {
        this.lastAccessedAt = System.currentTimeMillis();
    }
}
