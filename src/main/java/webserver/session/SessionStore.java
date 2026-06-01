package webserver.session;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SessionStore {

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    public Session createSession(String id) {
        Session session = new Session(id);
        sessions.put(id, session);
        return session;
    }

    public Session getSession(String id) {
        Session session = sessions.get(id);
        if (session != null) {
            session.updateLastAccessedAt();
        }
        return session;
    }

    public void removeSession(String id) {
        sessions.remove(id);
    }

    public int size() {
        return sessions.size();
    }

    public void expireStaleSessions(long maxAgeMs) {
        long now = System.currentTimeMillis();
        sessions.values().removeIf(s -> (now - s.getLastAccessedAt()) > maxAgeMs);
    }
}
