package webserver.session;

import webserver.http.Cookie;
import webserver.http.HttpRequest;
import webserver.http.HttpResponse;
import webserver.http.HttpResponseBuilder;

import java.util.UUID;

public class SessionManager {

    private static final String SESSION_COOKIE = "SESSION_ID";

    private final SessionStore sessionStore;
    private final long sessionTimeoutMs;

    public SessionManager(long sessionTimeoutMs) {
        this.sessionStore = new SessionStore();
        this.sessionTimeoutMs = sessionTimeoutMs;
    }

    public SessionManager() {
        this(30 * 60 * 1000);
    }

    public Session getOrCreateSession(HttpRequest request, HttpResponseBuilder response) {
        Cookie sessionCookie = request.getCookie(SESSION_COOKIE);
        Session session = null;

        if (sessionCookie != null) {
            session = sessionStore.getSession(sessionCookie.getValue());
        }

        if (session == null) {
            String id = UUID.randomUUID().toString();
            session = sessionStore.createSession(id);
            Cookie cookie = new Cookie(SESSION_COOKIE, id);
            cookie.setPath("/");
            cookie.setHttpOnly(true);
            response.cookie(cookie);
        }

        return session;
    }

    public Session getSession(HttpRequest request) {
        Cookie sessionCookie = request.getCookie(SESSION_COOKIE);
        if (sessionCookie != null) {
            return sessionStore.getSession(sessionCookie.getValue());
        }
        return null;
    }

    public void expireStaleSessions() {
        sessionStore.expireStaleSessions(sessionTimeoutMs);
    }

    public SessionStore getSessionStore() {
        return sessionStore;
    }
}
