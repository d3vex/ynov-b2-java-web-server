package webserver.session;

import webserver.http.Cookie;
import webserver.http.HttpRequest;

public class CookieService {

    static final String DEFAULT_SESSION_COOKIE = "SESSION_ID";

    private final String sessionCookieName;

    public CookieService() {
        this(DEFAULT_SESSION_COOKIE);
    }

    public CookieService(String sessionCookieName) {
        this.sessionCookieName = sessionCookieName;
    }

    public String getSessionCookieName() {
        return sessionCookieName;
    }

    public Cookie createSessionCookie(String sessionId) {
        Cookie cookie = new Cookie(sessionCookieName, sessionId);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        return cookie;
    }

    public String getSessionId(HttpRequest request) {
        Cookie cookie = request.getCookie(sessionCookieName);
        return cookie != null ? cookie.getValue() : null;
    }
}
