package webserver.session;

import webserver.http.HttpRequest;
import webserver.http.HttpResponseBuilder;

import java.util.UUID;

public class SessionManager {

    private final CookieService cookieService;
    private final SessionStore sessionStore;
    private final long sessionTimeoutMs;

    public SessionManager(long sessionTimeoutMs) {
        this(new CookieService(), sessionTimeoutMs);
    }

    public SessionManager(CookieService cookieService, long sessionTimeoutMs) {
        this.cookieService = cookieService;
        this.sessionStore = new SessionStore();
        this.sessionTimeoutMs = sessionTimeoutMs;
    }

    public SessionManager() {
        this(30 * 60 * 1000);
    }

    public Session getOrCreateSession(HttpRequest request, HttpResponseBuilder response) {
        String sessionId = cookieService.getSessionId(request);
        Session session = sessionId != null ? sessionStore.getSession(sessionId) : null;

        if (session == null) {
            String id = UUID.randomUUID().toString();
            session = sessionStore.createSession(id);
            response.cookie(cookieService.createSessionCookie(id));
        }

        return session;
    }

    public Session getSession(HttpRequest request) {
        String sessionId = cookieService.getSessionId(request);
        return sessionId != null ? sessionStore.getSession(sessionId) : null;
    }

    public void expireStaleSessions() {
        sessionStore.expireStaleSessions(sessionTimeoutMs);
    }

    public SessionStore getSessionStore() {
        return sessionStore;
    }

    public CookieService getCookieService() {
        return cookieService;
    }
}
