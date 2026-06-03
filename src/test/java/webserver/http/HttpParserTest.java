package webserver.http;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class HttpParserTest {

    private HttpParser newParser() {
        return new HttpParser();
    }

    @Nested
    @DisplayName("Request line parsing")
    class RequestLine {

        @Test
        @DisplayName("Parse simple GET request")
        void simpleGet() {
            HttpParser parser = newParser();
            parser.consume("GET /index.html HTTP/1.1\r\n\r\n");
            assertEquals(RequestParserState.COMPLETE, parser.getState());
            HttpRequest req = parser.build();
            assertEquals(HttpMethod.GET, req.getMethod());
            assertEquals("/index.html", req.getPath());
            assertEquals("HTTP/1.1", req.getHttpVersion());
        }

        @Test
        @DisplayName("Parse POST request")
        void postRequest() {
            HttpParser parser = newParser();
            String body = "data=hello";
            String raw = "POST /submit HTTP/1.1\r\nContent-Length: " + body.length() + "\r\n\r\n" + body;
            parser.consume(raw);
            assertEquals(RequestParserState.COMPLETE, parser.getState());
            HttpRequest req = parser.build();
            assertEquals(HttpMethod.POST, req.getMethod());
            assertEquals("/submit", req.getPath());
            assertArrayEquals(body.getBytes(StandardCharsets.UTF_8), req.getBody());
        }

        @Test
        @DisplayName("Parse with query parameters")
        void queryParams() {
            HttpParser parser = newParser();
            parser.consume("GET /search?q=hello&page=1 HTTP/1.1\r\n\r\n");
            HttpRequest req = parser.build();
            assertEquals("/search", req.getPath());
            assertEquals("q=hello&page=1", req.getRawQueryString());
            assertEquals("hello", req.getQueryParameter("q"));
            assertEquals("1", req.getQueryParameter("page"));
        }

        @Test
        @DisplayName("Parse HEAD request")
        void headRequest() {
            HttpParser parser = newParser();
            parser.consume("HEAD / HTTP/1.1\r\n\r\n");
            assertEquals(RequestParserState.COMPLETE, parser.getState());
            assertEquals(HttpMethod.HEAD, parser.build().getMethod());
        }

        @Test
        @DisplayName("Parse DELETE request")
        void deleteRequest() {
            HttpParser parser = newParser();
            parser.consume("DELETE /item/42 HTTP/1.1\r\n\r\n");
            assertEquals(HttpMethod.DELETE, parser.build().getMethod());
        }
    }

    @Nested
    @DisplayName("Header parsing")
    class HeaderParsing {

        @Test
        @DisplayName("Parse multiple headers")
        void multipleHeaders() {
            HttpParser parser = newParser();
            parser.consume("GET / HTTP/1.1\r\nHost: example.com\r\nUser-Agent: test\r\nAccept: */*\r\n\r\n");
            HttpRequest req = parser.build();
            assertEquals("example.com", req.getHeaders().get("Host"));
            assertEquals("test", req.getHeaders().get("User-Agent"));
            assertEquals("*/*", req.getHeaders().get("Accept"));
        }

        @Test
        @DisplayName("Parse cookies from headers")
        void cookiesInHeaders() {
            HttpParser parser = newParser();
            parser.consume("GET / HTTP/1.1\r\nCookie: a=1; b=2\r\n\r\n");
            HttpRequest req = parser.build();
            assertEquals(2, req.getCookies().size());
            assertEquals("a", req.getCookies().get(0).getName());
            assertEquals("b", req.getCookies().get(1).getName());
        }

        @Test
        @DisplayName("Get cookie by name")
        void getCookieByName() {
            HttpParser parser = newParser();
            parser.consume("GET / HTTP/1.1\r\nCookie: session=abc123\r\n\r\n");
            HttpRequest req = parser.build();
            Cookie cookie = req.getCookie("session");
            assertNotNull(cookie);
            assertEquals("abc123", cookie.getValue());
            assertNull(req.getCookie("nonexistent"));
        }
    }

    @Nested
    @DisplayName("Body parsing")
    class BodyParsing {

        @Test
        @DisplayName("Content-Length body")
        void contentLengthBody() {
            HttpParser parser = newParser();
            String body = "{\"key\":\"value\"}";
            String raw = "POST /api HTTP/1.1\r\nContent-Length: " + body.length() + "\r\n\r\n" + body;
            parser.consume(raw);
            assertEquals(RequestParserState.COMPLETE, parser.getState());
            assertArrayEquals(body.getBytes(StandardCharsets.UTF_8), parser.build().getBody());
        }

        @Test
        @DisplayName("Zero-length body")
        void zeroBody() {
            HttpParser parser = newParser();
            parser.consume("POST /api HTTP/1.1\r\nContent-Length: 0\r\n\r\n");
            assertEquals(RequestParserState.COMPLETE, parser.getState());
            assertEquals(0, parser.build().getBody().length);
        }

        @Test
        @DisplayName("Body as string convenience")
        void bodyAsString() {
            HttpParser parser = newParser();
            String body = "hello world";
            parser.consume("POST / HTTP/1.1\r\nContent-Length: " + body.length() + "\r\n\r\n" + body);
            assertEquals("hello world", parser.build().getBodyAsString());
        }

        @Test
        @DisplayName("Body is mutable via setBody")
        void setBody() {
            HttpParser parser = newParser();
            parser.consume("POST / HTTP/1.1\r\nContent-Length: 3\r\n\r\nold");
            HttpRequest req = parser.build();
            assertEquals("old", req.getBodyAsString());
            req.setBody("new".getBytes(StandardCharsets.UTF_8));
            assertArrayEquals("new".getBytes(StandardCharsets.UTF_8), req.getBody());
        }
    }

    @Nested
    @DisplayName("Chunked transfer encoding")
    class ChunkedEncoding {

        @Test
        @DisplayName("Decode simple chunked body")
        void simpleChunked() {
            HttpParser parser = newParser();
            String raw = "POST / HTTP/1.1\r\nTransfer-Encoding: chunked\r\n\r\n5\r\nHello\r\n0\r\n\r\n";
            parser.consume(raw);
            assertEquals(RequestParserState.COMPLETE, parser.getState());
            assertArrayEquals("Hello".getBytes(StandardCharsets.UTF_8), parser.build().getBody());
        }

        @Test
        @DisplayName("Decode multi-chunk body")
        void multiChunk() {
            HttpParser parser = newParser();
            String raw = "POST / HTTP/1.1\r\nTransfer-Encoding: chunked\r\n\r\n"
                    + "5\r\nHello\r\n"
                    + "6\r\n World\r\n"
                    + "0\r\n\r\n";
            parser.consume(raw);
            assertEquals(RequestParserState.COMPLETE, parser.getState());
            assertEquals("Hello World", parser.build().getBodyAsString());
        }

        @Test
        @DisplayName("Decode chunked body without trailing CRLF")
        void chunkedNoTrailer() {
            HttpParser parser = newParser();
            String raw = "POST / HTTP/1.1\r\nTransfer-Encoding: chunked\r\n\r\n4\r\ntest\r\n0\r\n\r\n";
            parser.consume(raw);
            assertEquals(RequestParserState.COMPLETE, parser.getState());
            assertEquals("test", parser.build().getBodyAsString());
        }

        @Test
        @DisplayName("ChunkedDecoder error state")
        void chunkedError() {
            ChunkedDecoder decoder = new ChunkedDecoder();
            StringBuilder buf = new StringBuilder("invalid\r\n");
            assertEquals(ChunkedDecoder.Result.ERROR, decoder.decode(buf));
            assertNotNull(decoder.getErrorReason());
            assertEquals("Invalid chunk size: invalid", decoder.getErrorReason());
            decoder.reset();
            assertNull(decoder.getErrorReason());
            StringBuilder buf2 = new StringBuilder("5\r\nHello\r\n0\r\n\r\n");
            assertEquals(ChunkedDecoder.Result.COMPLETE, decoder.decode(buf2));
            assertArrayEquals("Hello".getBytes(java.nio.charset.StandardCharsets.UTF_8), decoder.getBody());
        }
    }

    @Nested
    @DisplayName("Parser state machine")
    class StateMachine {

        @Test
        @DisplayName("Initial state is REQUEST_LINE")
        void initialState() {
            assertEquals(RequestParserState.REQUEST_LINE, newParser().getState());
        }

        @Test
        @DisplayName("Reset clears parser state")
        void reset() {
            HttpParser parser = newParser();
            parser.consume("GET / HTTP/1.1\r\n\r\n");
            assertEquals(RequestParserState.COMPLETE, parser.getState());
            parser.reset();
            assertEquals(RequestParserState.REQUEST_LINE, parser.getState());
            assertNull(parser.build());
        }

        @Test
        @DisplayName("Consume with ByteBuffer")
        void byteBufferConsume() {
            HttpParser parser = newParser();
            String raw = "GET /test HTTP/1.1\r\n\r\n";
            ByteBuffer buf = ByteBuffer.wrap(raw.getBytes(StandardCharsets.UTF_8));
            parser.consume(buf);
            assertEquals(RequestParserState.COMPLETE, parser.getState());
            assertEquals("/test", parser.build().getPath());
        }

        @Test
        @DisplayName("Error state on bad request line")
        void badRequestLine() {
            HttpParser parser = newParser();
            parser.consume("INVALID\r\n\r\n");
            assertEquals(RequestParserState.ERROR, parser.getState());
            assertNotNull(parser.getErrorReason());
        }

        @Test
        @DisplayName("Error state on no method")
        void noMethod() {
            HttpParser parser = newParser();
            parser.consume(" / HTTP/1.1\r\n\r\n");
            assertTrue(parser.getState() == RequestParserState.ERROR);
        }
    }

    @Nested
    @DisplayName("HttpMethod enum")
    class HttpMethodTests {

        @Test
        @DisplayName("All methods exist")
        void allMethods() {
            assertNotNull(HttpMethod.GET);
            assertNotNull(HttpMethod.POST);
            assertNotNull(HttpMethod.PATCH);
            assertNotNull(HttpMethod.PUT);
            assertNotNull(HttpMethod.DELETE);
            assertNotNull(HttpMethod.HEAD);
            assertNotNull(HttpMethod.OPTIONS);
        }

        @Test
        @DisplayName("Seven methods total")
        void methodCount() {
            assertEquals(7, HttpMethod.values().length);
        }
    }

    @Nested
    @DisplayName("Memory leak scenarios")
    class MemoryLeakScenarios {

        @Test
        @DisplayName("Parser reset between requests prevents state accumulation")
        void resetBetweenRequests() {
            HttpParser parser = newParser();
            for (int i = 0; i < 1000; i++) {
                parser.consume("GET /page" + i + " HTTP/1.1\r\nHost: test\r\n\r\n");
                assertEquals(RequestParserState.COMPLETE, parser.getState());
                assertEquals("/page" + i, parser.build().getPath());
                parser.reset();
                assertEquals(RequestParserState.REQUEST_LINE, parser.getState());
            }
        }

        @Test
        @DisplayName("ChunkedDecoder reset prevents state accumulation")
        void chunkedReset() {
            ChunkedDecoder decoder = new ChunkedDecoder();
            for (int i = 0; i < 1000; i++) {
                StringBuilder buf = new StringBuilder("4\r\ntest\r\n0\r\n\r\n");
                assertEquals(ChunkedDecoder.Result.COMPLETE, decoder.decode(buf));
                assertArrayEquals("test".getBytes(StandardCharsets.UTF_8), decoder.getBody());
                decoder.reset();
            }
        }

        @Test
        @DisplayName("Parse 1000 different requests without reset (should not leak)")
        void manyParses() {
            for (int i = 0; i < 1000; i++) {
                HttpParser parser = newParser();
                parser.consume("GET /" + i + " HTTP/1.1\r\n\r\n");
                assertEquals("/" + i, parser.build().getPath());
            }
        }

        @Test
        @DisplayName("Large chunked body")
        void largeChunkedBody() {
            ChunkedDecoder decoder = new ChunkedDecoder();
            StringBuilder chunkData = new StringBuilder();
            for (int i = 0; i < 10_000; i++) chunkData.append('x');
            String chunkLine = Integer.toHexString(10_000) + "\r\n" + chunkData + "\r\n";
            StringBuilder raw = new StringBuilder();
            raw.append(chunkLine).append("0\r\n\r\n");
            assertEquals(ChunkedDecoder.Result.COMPLETE, decoder.decode(raw));
            assertEquals(10_000, decoder.getBody().length);
        }
    }
}
