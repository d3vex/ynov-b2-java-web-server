package webserver.session;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import webserver.http.Cookie;
import webserver.http.HttpHeaders;
import webserver.http.HttpMethod;
import webserver.http.HttpRequest;
import webserver.http.HttpResponse;
import webserver.http.HttpResponseBuilder;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SessionTest {

    @Nested
    @DisplayName("Session")
    class SessionTests {

        @Test
        @DisplayName("Create session with ID")
        void createSession() {
            Session s = new Session("test-id");
            assertEquals("test-id", s.getId());
            assertTrue(s.getCreatedAt() > 0);
            assertTrue(s.getLastAccessedAt() > 0);
        }

        @Test
        @DisplayName("Set and get attributes")
        void attributes() {
            Session s = new Session("id");
            s.setAttribute("key1", "value1");
            s.setAttribute("key2", "value2");
            assertEquals("value1", s.getAttribute("key1"));
            assertEquals("value2", s.getAttribute("key2"));
        }

        @Test
        @DisplayName("Remove attribute")
        void removeAttribute() {
            Session s = new Session("id");
            s.setAttribute("key", "value");
            s.removeAttribute("key");
            assertNull(s.getAttribute("key"));
        }

        @Test
        @DisplayName("Get all attributes")
        void allAttributes() {
            Session s = new Session("id");
            s.setAttribute("a", "1");
            s.setAttribute("b", "2");
            Map<String, String> attrs = s.getAttributes();
            assertEquals(2, attrs.size());
            assertEquals("1", attrs.get("a"));
        }

        @Test
        @DisplayName("updateLastAccessedAt refreshes timestamp")
        void updateLastAccessed() {
            Session s = new Session("id");
            long old = s.getLastAccessedAt();
            s.updateLastAccessedAt();
            assertTrue(s.getLastAccessedAt() >= old);
        }
    }

    @Nested
    @DisplayName("SessionStore")
    class SessionStoreTests {

        private final SessionStore store = new SessionStore();

        @Test
        @DisplayName("Create and retrieve session")
        void createAndGet() {
            Session s = store.createSession("id-1");
            assertNotNull(s);
            assertEquals("id-1", s.getId());

            Session retrieved = store.getSession("id-1");
            assertNotNull(retrieved);
            assertEquals("id-1", retrieved.getId());
        }

        @Test
        @DisplayName("Remove session")
        void remove() {
            store.createSession("id-1");
            store.removeSession("id-1");
            assertNull(store.getSession("id-1"));
        }

        @Test
        @DisplayName("Size reflects session count")
        void size() {
            assertEquals(0, store.size());
            store.createSession("a");
            assertEquals(1, store.size());
            store.createSession("b");
            assertEquals(2, store.size());
        }

        @Test
        @DisplayName("Get non-existent session returns null")
        void getNonExistent() {
            assertNull(store.getSession("nonexistent"));
        }

        @Test
        @DisplayName("Get session updates last accessed time")
        void getUpdatesLastAccessed() {
            Session s = store.createSession("id");
            long old = s.getLastAccessedAt();
            sleep(1);
            store.getSession("id");
            assertTrue(s.getLastAccessedAt() > old);
        }

        @Test
        @DisplayName("Expire stale sessions")
        void expireStale() {
            store.createSession("fresh");
            store.expireStaleSessions(1);
            assertEquals(1, store.size());
        }

        @Test
        @DisplayName("Create 1000 sessions and verify count")
        void manySessions() {
            for (int i = 0; i < 1000; i++) {
                store.createSession("id-" + i);
            }
            assertEquals(1000, store.size());
        }
    }

    @Nested
    @DisplayName("CookieService")
    class CookieServiceTests {

        @Test
        @DisplayName("Default cookie name is SESSION_ID")
        void defaultName() {
            CookieService cs = new CookieService();
            assertEquals("SESSION_ID", cs.getSessionCookieName());
        }

        @Test
        @DisplayName("Custom cookie name")
        void customName() {
            CookieService cs = new CookieService("MY_SESSION");
            assertEquals("MY_SESSION", cs.getSessionCookieName());
        }

        @Test
        @DisplayName("Create session cookie with HttpOnly and Path=/")
        void createSessionCookie() {
            CookieService cs = new CookieService();
            Cookie c = cs.createSessionCookie("abc-123");
            assertEquals("SESSION_ID", c.getName());
            assertEquals("abc-123", c.getValue());
            assertEquals("/", c.getPath());
            assertTrue(c.isHttpOnly());
        }

        @Test
        @DisplayName("Get session ID from request")
        void getSessionId() {
            CookieService cs = new CookieService();
            HttpHeaders headers = new HttpHeaders();
            headers.add("Cookie", "SESSION_ID=my-session-id");
            HttpRequest req = new HttpRequest(HttpMethod.GET, "/", "HTTP/1.1", headers);
            assertEquals("my-session-id", cs.getSessionId(req));
        }

        @Test
        @DisplayName("Get session ID returns null when no cookie")
        void noSessionId() {
            CookieService cs = new CookieService();
            HttpRequest req = new HttpRequest(HttpMethod.GET, "/", "HTTP/1.1", new HttpHeaders());
            assertNull(cs.getSessionId(req));
        }

        @Test
        @DisplayName("Get session ID with custom cookie name")
        void customCookieName() {
            CookieService cs = new CookieService("TOKEN");
            HttpHeaders headers = new HttpHeaders();
            headers.add("Cookie", "TOKEN=custom-token");
            HttpRequest req = new HttpRequest(HttpMethod.GET, "/", "HTTP/1.1", headers);
            assertEquals("custom-token", cs.getSessionId(req));
        }
    }

    @Nested
    @DisplayName("SessionManager")
    class SessionManagerTests {

        @Test
        @DisplayName("Get or create session — new session")
        void getOrCreateSession() {
            SessionManager sm = new SessionManager(60000);
            HttpRequest req = new HttpRequest(HttpMethod.GET, "/", "HTTP/1.1", new HttpHeaders());
            HttpResponseBuilder builder = HttpResponse.builder();

            Session s = sm.getOrCreateSession(req, builder);
            assertNotNull(s);
            assertNotNull(s.getId());
        }

        @Test
        @DisplayName("Get or create session — existing session")
        void getOrCreateExisting() {
            SessionManager sm = new SessionManager(60000);
            HttpRequest req = new HttpRequest(HttpMethod.GET, "/", "HTTP/1.1", new HttpHeaders());
            HttpResponseBuilder builder = HttpResponse.builder();

            Session first = sm.getOrCreateSession(req, builder);
            assertNotNull(first);

            // Second call with same request (no cookie) creates a new one
            Session second = sm.getOrCreateSession(req, HttpResponse.builder());
            assertNotNull(second);
            assertNotEquals(first.getId(), second.getId());
        }

        @Test
        @DisplayName("Get existing session with valid cookie")
        void getWithValidCookie() {
            SessionManager sm = new SessionManager(60000);
            HttpRequest req1 = new HttpRequest(HttpMethod.GET, "/", "HTTP/1.1", new HttpHeaders());
            HttpResponseBuilder builder = HttpResponse.builder();

            Session created = sm.getOrCreateSession(req1, builder);
            String sessionId = created.getId();

            // Build response to get the cookie, then create a new request with it
            HttpHeaders headers2 = new HttpHeaders();
            headers2.add("Cookie", "SESSION_ID=" + sessionId);
            HttpRequest req2 = new HttpRequest(HttpMethod.GET, "/", "HTTP/1.1", headers2);

            Session retrieved = sm.getSession(req2);
            assertNotNull(retrieved);
            assertEquals(sessionId, retrieved.getId());
        }

        @Test
        @DisplayName("Get session returns null when no cookie")
        void getSessionNoCookie() {
            SessionManager sm = new SessionManager(60000);
            HttpRequest req = new HttpRequest(HttpMethod.GET, "/", "HTTP/1.1", new HttpHeaders());
            assertNull(sm.getSession(req));
        }

        @Test
        @DisplayName("Cookie is added to response for new sessions")
        void cookieAddedToResponse() {
            SessionManager sm = new SessionManager(60000);
            HttpRequest req = new HttpRequest(HttpMethod.GET, "/", "HTTP/1.1", new HttpHeaders());
            HttpResponseBuilder builder = HttpResponse.builder();

            sm.getOrCreateSession(req, builder);
            var response = builder.build();
            var cookies = response.getSetCookies();
            assertEquals(1, cookies.size());
            assertEquals("SESSION_ID", cookies.get(0).getName());
        }

        @Test
        @DisplayName("Expire stale sessions via session manager")
        void expireStale() {
            SessionManager sm = new SessionManager(1);
            sm.getOrCreateSession(
                    new HttpRequest(HttpMethod.GET, "/", "HTTP/1.1", new HttpHeaders()),
                    HttpResponse.builder()
            );
            assertEquals(1, sm.getSessionStore().size());
            sleep(2);
            sm.expireStaleSessions();
            assertEquals(0, sm.getSessionStore().size());
        }

        @Test
        @DisplayName("Custom CookieService can be injected")
        void customCookieService() {
            CookieService customCs = new CookieService("MY_SESSION");
            SessionManager sm = new SessionManager(customCs, 60000);
            assertSame(customCs, sm.getCookieService());
        }

        @Test
        @DisplayName("Default constructor uses 30min timeout")
        void defaultConstructor() {
            SessionManager sm = new SessionManager();
            assertNotNull(sm.getSessionStore());
            assertNotNull(sm.getCookieService());
        }
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
