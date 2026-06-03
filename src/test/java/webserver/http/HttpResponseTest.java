package webserver.http;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class HttpResponseTest {

    @Nested
    @DisplayName("HttpStatus constants")
    class HttpStatusConstants {

        @Test
        @DisplayName("All status constants have correct codes")
        void allStatusCodes() {
            assertEquals(200, HttpStatus.OK.getCode());
            assertEquals(301, HttpStatus.REDIRECT_PERMANENT.getCode());
            assertEquals(302, HttpStatus.REDIRECT_FOUND.getCode());
            assertEquals(400, HttpStatus.BAD_REQUEST.getCode());
            assertEquals(401, HttpStatus.UNAUTHORIZED.getCode());
            assertEquals(402, HttpStatus.PAYMENT_REQUIRED.getCode());
            assertEquals(403, HttpStatus.FORBIDDEN.getCode());
            assertEquals(404, HttpStatus.NOT_FOUND.getCode());
            assertEquals(405, HttpStatus.METHOD_NOT_ALLOWED.getCode());
            assertEquals(406, HttpStatus.NOT_ACCEPTABLE.getCode());
            assertEquals(408, HttpStatus.REQUEST_TIMEOUT.getCode());
            assertEquals(413, HttpStatus.PAYLOAD_TOO_LARGE.getCode());
            assertEquals(415, HttpStatus.UNSUPPORTED_MEDIA_TYPE.getCode());
            assertEquals(426, HttpStatus.UPGRADE_REQUIRED.getCode());
            assertEquals(429, HttpStatus.TOO_MANY_REQUESTS.getCode());
            assertEquals(500, HttpStatus.INTERNAL_SERVER_ERROR.getCode());
            assertEquals(501, HttpStatus.NOT_IMPLEMENTED.getCode());
            assertEquals(502, HttpStatus.BAD_GATEWAY.getCode());
            assertEquals(503, HttpStatus.SERVICE_UNAVAILABLE.getCode());
            assertEquals(504, HttpStatus.GATEWAY_TIMEOUT.getCode());
            assertEquals(505, HttpStatus.HTTP_VERSION_NOT_SUPPORTED.getCode());
        }

        @Test
        @DisplayName("Reason phrases are non-empty")
        void reasonPhrases() {
            assertNotNull(HttpStatus.OK.getReasonPhrase());
            assertFalse(HttpStatus.NOT_FOUND.getReasonPhrase().isEmpty());
            assertFalse(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase().isEmpty());
        }

        @Test
        @DisplayName("fromCode creates custom status")
        void fromCode() {
            HttpStatus custom = HttpStatus.fromCode(299, "Custom");
            assertEquals(299, custom.getCode());
            assertEquals("Custom", custom.getReasonPhrase());
            HttpStatus custom2 = HttpStatus.fromCode(299, "Custom");
            assertEquals(custom.getCode(), custom2.getCode());
            assertEquals(custom.getReasonPhrase(), custom2.getReasonPhrase());
        }
    }

    @Nested
    @DisplayName("HttpResponseBuilder")
    class ResponseBuilder {

        @Test
        @DisplayName("Build minimal response with defaults")
        void minimalResponse() {
            HttpResponse res = HttpResponse.builder()
                    .status(HttpStatus.OK)
                    .build();
            assertEquals("HTTP/1.1", res.getHttpVersion());
            assertEquals(HttpStatus.OK, res.getStatusCode());
            assertNotNull(res.getHeaders());
            assertEquals(0, res.getBody().length);
        }

        @Test
        @DisplayName("Build response with body auto-sets Content-Length")
        void bodySetsContentLength() {
            byte[] body = "Hello, World!".getBytes(StandardCharsets.UTF_8);
            HttpResponse res = HttpResponse.builder()
                    .status(HttpStatus.OK)
                    .body(body)
                    .build();
            assertArrayEquals(body, res.getBody());
            assertEquals(String.valueOf(body.length), res.getHeaders().get("Content-Length"));
        }

        @Test
        @DisplayName("String body is encoded and Content-Length set")
        void stringBody() {
            HttpResponse res = HttpResponse.builder()
                    .status(HttpStatus.OK)
                    .body("test")
                    .build();
            assertArrayEquals("test".getBytes(StandardCharsets.UTF_8), res.getBody());
            assertEquals("4", res.getHeaders().get("Content-Length"));
        }

        @Test
        @DisplayName("Build with cookies adds Set-Cookie header")
        void withCookies() {
            Cookie cookie = new Cookie("session", "abc123");
            cookie.setPath("/");
            cookie.setHttpOnly(true);

            HttpResponse res = HttpResponse.builder()
                    .status(HttpStatus.OK)
                    .cookie(cookie)
                    .build();
            assertEquals(1, res.getSetCookies().size());
            assertEquals("session", res.getSetCookies().get(0).getName());
        }

        @Test
        @DisplayName("Build with all fields")
        void fullResponse() {
            HttpResponse res = HttpResponse.builder()
                    .httpVersion("HTTP/1.0")
                    .status(HttpStatus.NOT_FOUND)
                    .header("X-Custom", "value")
                    .addHeader("X-Multi", "v1")
                    .addHeader("X-Multi", "v2")
                    .body("not found")
                    .build();
            assertEquals("HTTP/1.0", res.getHttpVersion());
            assertEquals(HttpStatus.NOT_FOUND, res.getStatusCode());
            assertEquals("value", res.getHeaders().get("X-Custom"));
            assertEquals(2, res.getHeaders().getAll("X-Multi").size());
        }

        @Test
        @DisplayName("Redirect sets Content-Length to 0")
        void redirectHasContentLength() {
            HttpResponse res = HttpResponse.builder()
                    .status(HttpStatus.REDIRECT_FOUND)
                    .header("Location", "/new")
                    .build();
            assertEquals("0", res.getHeaders().get("Content-Length"));
            assertEquals(0, res.getBody().length);
        }

        @Test
        @DisplayName("status(int, String) convenience method")
        void statusIntReason() {
            HttpResponse res = HttpResponse.builder()
                    .status(418, "I'm a Teapot")
                    .build();
            assertEquals(418, res.getStatusCode().getCode());
            assertEquals("I'm a Teapot", res.getStatusCode().getReasonPhrase());
        }
    }

    @Nested
    @DisplayName("HttpResponse serialization")
    class Serialization {

        @Test
        @DisplayName("toBytes produces valid HTTP response")
        void toBytes() {
            HttpResponse res = HttpResponse.builder()
                    .status(HttpStatus.OK)
                    .header("Content-Type", "text/plain")
                    .body("OK")
                    .build();
            String wire = new String(res.toBytes(), StandardCharsets.UTF_8);
            assertTrue(wire.startsWith("HTTP/1.1 200 OK\r\n"));
            assertTrue(wire.contains("Content-Type: text/plain\r\n"));
            assertTrue(wire.contains("Content-Length: 2\r\n"));
            assertTrue(wire.endsWith("\r\n\r\nOK"));
        }

        @Test
        @DisplayName("toBytes with cookies includes Set-Cookie header")
        void toBytesWithCookies() {
            Cookie cookie = new Cookie("id", "42");
            cookie.setPath("/");
            HttpResponse res = HttpResponse.builder()
                    .status(HttpStatus.OK)
                    .cookie(cookie)
                    .body("x")
                    .build();
            String wire = new String(res.toBytes(), StandardCharsets.UTF_8);
            assertTrue(wire.contains("Set-Cookie: id=42; Path=/"));
        }

        @Test
        @DisplayName("getBodyAsString convenience")
        void getBodyAsString() {
            HttpResponse res = HttpResponse.builder()
                    .status(HttpStatus.OK)
                    .body("hello")
                    .build();
            assertEquals("hello", res.getBodyAsString());
        }

        @Test
        @DisplayName("getReasonPhrase convenience")
        void getReasonPhrase() {
            HttpResponse res = HttpResponse.builder()
                    .status(HttpStatus.NOT_FOUND)
                    .build();
            assertEquals("Not Found", res.getReasonPhrase());
        }
    }

    @Nested
    @DisplayName("Memory leak scenarios")
    class MemoryLeakScenarios {

        @Test
        @DisplayName("1000 response builds without issues")
        void manyResponses() {
            for (int i = 0; i < 1000; i++) {
                HttpResponse res = HttpResponse.builder()
                        .status(HttpStatus.OK)
                        .header("X-Request", String.valueOf(i))
                        .body("response " + i)
                        .build();
                assertEquals(200, res.getStatusCode().getCode());
                assertEquals("response " + i, res.getBodyAsString());
            }
        }

        @Test
        @DisplayName("Large body serialization")
        void largeBody() {
            byte[] largeBody = new byte[100_000];
            for (int i = 0; i < largeBody.length; i++) largeBody[i] = (byte) (i % 256);
            HttpResponse res = HttpResponse.builder()
                    .status(HttpStatus.OK)
                    .body(largeBody)
                    .build();
            byte[] wire = res.toBytes();
            assertTrue(wire.length > 100_000);
        }
    }
}
