package webserver.http;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HttpHeadersTest {

    @Nested
    @DisplayName("HttpHeaders basic operations")
    class BasicOperations {

        @Test
        @DisplayName("New headers is empty")
        void empty() {
            HttpHeaders h = new HttpHeaders();
            assertTrue(h.isEmpty());
            assertEquals(0, h.size());
        }

        @Test
        @DisplayName("Add and get single value")
        void addAndGet() {
            HttpHeaders h = new HttpHeaders();
            h.add("Content-Type", "text/html");
            assertEquals("text/html", h.get("Content-Type"));
        }

        @Test
        @DisplayName("Case-insensitive header names")
        void caseInsensitive() {
            HttpHeaders h = new HttpHeaders();
            h.add("content-type", "text/html");
            assertEquals("text/html", h.get("Content-Type"));
            assertEquals("text/html", h.get("CONTENT-TYPE"));
            assertEquals("text/html", h.get("content-type"));
        }

        @Test
        @DisplayName("Set replaces existing values")
        void setReplaces() {
            HttpHeaders h = new HttpHeaders();
            h.add("X-Custom", "v1");
            h.add("X-Custom", "v2");
            assertEquals(2, h.getAll("X-Custom").size());
            h.set("X-Custom", "v3");
            assertEquals(1, h.getAll("X-Custom").size());
            assertEquals("v3", h.get("X-Custom"));
        }

        @Test
        @DisplayName("Set with list replaces all")
        void setList() {
            HttpHeaders h = new HttpHeaders();
            h.add("X-Custom", "original");
            h.set("X-Custom", List.of("a", "b", "c"));
            assertEquals(List.of("a", "b", "c"), h.getAll("X-Custom"));
        }

        @Test
        @DisplayName("Contains checks header existence")
        void contains() {
            HttpHeaders h = new HttpHeaders();
            h.add("Content-Type", "text/plain");
            assertTrue(h.contains("Content-Type"));
            assertTrue(h.contains("content-type"));
            assertFalse(h.contains("X-Nonexistent"));
        }

        @Test
        @DisplayName("Remove returns removed values")
        void remove() {
            HttpHeaders h = new HttpHeaders();
            h.add("X-Custom", "value");
            List<String> removed = h.remove("X-Custom");
            assertEquals(List.of("value"), removed);
            assertFalse(h.contains("X-Custom"));
        }

        @Test
        @DisplayName("Names returns all header names")
        void names() {
            HttpHeaders h = new HttpHeaders();
            h.add("Content-Type", "text/html");
            h.add("Content-Length", "42");
            assertEquals(2, h.names().size());
        }

        @Test
        @DisplayName("Clear removes all headers")
        void clear() {
            HttpHeaders h = new HttpHeaders();
            h.add("Content-Type", "text/html");
            h.clear();
            assertTrue(h.isEmpty());
        }

        @Test
        @DisplayName("Constructor from map")
        void fromMap() {
            var map = java.util.Map.of(
                    "Content-Type", List.of("application/json"),
                    "X-Custom", List.of("v1", "v2")
            );
            HttpHeaders h = new HttpHeaders(map);
            assertEquals("application/json", h.get("Content-Type"));
            assertEquals(List.of("v1", "v2"), h.getAll("X-Custom"));
        }

        @Test
        @DisplayName("toMap returns defensive copy")
        void toMap() {
            HttpHeaders h = new HttpHeaders();
            h.add("Content-Type", "text/html");
            var map = h.toMap();
            assertEquals(List.of("text/html"), map.get("Content-Type"));
        }

        @Test
        @DisplayName("toHeaderLines formats as raw header strings")
        void toHeaderLines() {
            HttpHeaders h = new HttpHeaders();
            h.add("Content-Type", "text/html");
            h.add("Content-Length", "42");
            List<String> lines = h.toHeaderLines();
            assertTrue(lines.contains("Content-Type: text/html"));
            assertTrue(lines.contains("Content-Length: 42"));
        }

        @Test
        @DisplayName("Parse from raw header lines")
        void parse() {
            String raw = "Content-Type: text/html\r\nContent-Length: 42\r\n";
            HttpHeaders h = HttpHeaders.parse(List.of(raw.split("\r\n")));
            assertEquals("text/html", h.get("Content-Type"));
            assertEquals("42", h.get("Content-Length"));
        }

        @Test
        @DisplayName("Parse handles multi-value headers")
        void parseMultiValue() {
            String raw = "Set-Cookie: a=1\r\nSet-Cookie: b=2\r\n";
            HttpHeaders h = HttpHeaders.parse(List.of(raw.split("\r\n")));
            assertEquals(2, h.getAll("Set-Cookie").size());
        }
    }

    @Nested
    @DisplayName("Cookie integration in HttpHeaders")
    class CookieIntegration {

        @Test
        @DisplayName("Cookie header parsed from request")
        void requestCookies() {
            HttpHeaders h = new HttpHeaders();
            h.add("Cookie", "a=1; b=2; c=3");
            List<Cookie> cookies = h.getCookies();
            assertEquals(3, cookies.size());
            assertEquals("a", cookies.get(0).getName());
            assertEquals("1", cookies.get(0).getValue());
        }

        @Test
        @DisplayName("Set-Cookie header can be added and retrieved")
        void setCookies() {
            HttpHeaders h = new HttpHeaders();
            Cookie c = new Cookie("session", "xyz");
            c.setHttpOnly(true);
            c.setPath("/");
            h.addSetCookie(c);
            List<Cookie> setCookies = h.getSetCookies();
            assertEquals(1, setCookies.size());
            assertEquals("session", setCookies.get(0).getName());
            assertTrue(setCookies.get(0).isHttpOnly());
        }
    }

    @Nested
    @DisplayName("Cookie class")
    class CookieTests {

        @Test
        @DisplayName("Cookie name and value")
        void basic() {
            Cookie c = new Cookie("name", "value");
            assertEquals("name", c.getName());
            assertEquals("value", c.getValue());
        }

        @Test
        @DisplayName("Cookie attributes")
        void attributes() {
            Cookie c = new Cookie("n", "v");
            c.setPath("/app");
            c.setDomain("example.com");
            c.setMaxAge(3600);
            c.setSecure(true);
            c.setHttpOnly(true);
            c.setSameSite("Lax");

            assertEquals("/app", c.getPath());
            assertEquals("example.com", c.getDomain());
            assertEquals(3600, c.getMaxAge());
            assertTrue(c.isSecure());
            assertTrue(c.isHttpOnly());
            assertEquals("Lax", c.getSameSite());
        }

        @Test
        @DisplayName("Cookie value is mutable")
        void mutableValue() {
            Cookie c = new Cookie("n", "v1");
            c.setValue("v2");
            assertEquals("v2", c.getValue());
        }

        @Test
        @DisplayName("Cookie equality by name only")
        void equality() {
            Cookie c1 = new Cookie("session", "abc");
            Cookie c2 = new Cookie("session", "xyz");
            assertEquals(c1, c2);
            assertEquals(c1.hashCode(), c2.hashCode());
        }

        @Test
        @DisplayName("Different cookie names not equal")
        void notEqual() {
            Cookie c1 = new Cookie("a", "1");
            Cookie c2 = new Cookie("b", "1");
            assertNotEquals(c1, c2);
        }
    }

    @Nested
    @DisplayName("CookieParser")
    class CookieParserTests {

        private final CookieParser parser = new CookieParser();

        @Test
        @DisplayName("Parse request Cookie header")
        void parseCookie() {
            List<Cookie> cookies = parser.parseCookies("a=1; b=2; c=3");
            assertEquals(3, cookies.size());
            assertEquals("a", cookies.get(0).getName());
            assertEquals("1", cookies.get(0).getValue());
            assertEquals("b", cookies.get(1).getName());
            assertEquals("2", cookies.get(1).getValue());
        }

        @Test
        @DisplayName("Parse cookie with spaces")
        void parseWithSpaces() {
            List<Cookie> cookies = parser.parseCookies("a = 1; b = 2");
            assertEquals(2, cookies.size());
            assertEquals("1", cookies.get(0).getValue());
        }

        @Test
        @DisplayName("Parse empty cookie header")
        void parseEmpty() {
            List<Cookie> cookies = parser.parseCookies("");
            assertTrue(cookies.isEmpty());
        }

        @Test
        @DisplayName("Parse Set-Cookie header")
        void parseSetCookie() {
            Cookie c = parser.parseSetCookie("session=abc123; Path=/; HttpOnly; Max-Age=3600");
            assertEquals("session", c.getName());
            assertEquals("abc123", c.getValue());
            assertEquals("/", c.getPath());
            assertTrue(c.isHttpOnly());
            assertEquals(3600, c.getMaxAge());
        }

        @Test
        @DisplayName("Format cookies as request header")
        void formatCookies() {
            List<Cookie> cookies = List.of(new Cookie("a", "1"), new Cookie("b", "2"));
            String result = parser.formatCookie(cookies);
            assertEquals("a=1; b=2", result);
        }

        @Test
        @DisplayName("Format Set-Cookie with all attributes")
        void formatSetCookie() {
            Cookie c = new Cookie("session", "xyz");
            c.setPath("/");
            c.setMaxAge(3600);
            c.setHttpOnly(true);
            c.setSecure(true);
            String result = parser.formatSetCookie(c);
            assertTrue(result.contains("session=xyz"));
            assertTrue(result.contains("Path=/"));
            assertTrue(result.contains("HttpOnly"));
            assertTrue(result.contains("Secure"));
            assertTrue(result.contains("Max-Age=3600"));
        }

        @Test
        @DisplayName("Parse Set-Cookie with SameSite")
        void parseSameSite() {
            Cookie c = parser.parseSetCookie("n=v; SameSite=Lax");
            assertEquals("Lax", c.getSameSite());
        }
    }

    @Nested
    @DisplayName("Memory leak scenarios")
    class MemoryLeakScenarios {

        @Test
        @DisplayName("1000 header operations without issues")
        void manyOperations() {
            for (int i = 0; i < 1000; i++) {
                HttpHeaders h = new HttpHeaders();
                h.add("X-Request", String.valueOf(i));
                h.add("Content-Type", "text/plain");
                assertEquals(String.valueOf(i), h.get("X-Request"));
                assertEquals("text/plain", h.get("Content-Type"));
                assertTrue(h.contains("x-request"));
            }
        }

        @Test
        @DisplayName("Parse 1000 cookie headers")
        void manyCookieParses() {
            CookieParser parser = new CookieParser();
            for (int i = 0; i < 1000; i++) {
                List<Cookie> cookies = parser.parseCookies("a=" + i + "; b=" + (i + 1));
                assertEquals(2, cookies.size());
                assertEquals(String.valueOf(i), cookies.get(0).getValue());
            }
        }
    }
}
