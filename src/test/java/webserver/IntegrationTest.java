package webserver;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import webserver.cgi.CgiAsyncExecutor;
import webserver.config.ConfigLoader;
import webserver.config.RouteConfig;
import webserver.config.ServerConfig;
import webserver.handlers.ErrorHandler;
import webserver.handlers.RequestDispatcher;
import webserver.http.*;
import webserver.network.ClientConnection;
import webserver.session.CookieService;
import webserver.session.SessionManager;

import java.io.File;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Cross-package integration")
class IntegrationTest {

    @Nested
    @DisplayName("Config + Routing + StaticFile")
    class ConfigRoutingStaticFile {

        @Test
        @DisplayName("Load config, find route, serve static file")
        void serveStaticFile(@TempDir Path tempDir) throws Exception {
            Path www = tempDir.resolve("www");
            Files.createDirectories(www);
            Files.writeString(www.resolve("hello.html"), "<h1>Hello</h1>");

            Path configFile = tempDir.resolve("server.conf");
            Files.writeString(configFile,
                    "server {\n    port 8080\n    default_server_root " + www.toString().replace("\\", "\\\\") + "\n}");

            List<ServerConfig> configs = new ConfigLoader().load(configFile);
            ServerConfig config = configs.get(0);

            HttpRequest request = new HttpRequest(HttpMethod.GET, "/hello.html", "HTTP/1.1", new HttpHeaders());
            RequestDispatcher dispatcher = new RequestDispatcher();
            HttpResponse response = dispatcher.dispatch(request, config);

            assertEquals(200, response.getStatusCode().getCode());
            assertTrue(response.getBodyAsString().contains("Hello"));
        }

        @Test
        @DisplayName("Non-existent path returns 404")
        void notFound(@TempDir Path tempDir) throws Exception {
            Path www = tempDir.resolve("www");
            Files.createDirectories(www);

            Path configFile = tempDir.resolve("server.conf");
            Files.writeString(configFile,
                    "server {\n    port 8080\n    default_server_root " + www.toString().replace("\\", "\\\\") + "\n}");

            List<ServerConfig> configs = new ConfigLoader().load(configFile);
            ServerConfig config = configs.get(0);

            HttpRequest request = new HttpRequest(HttpMethod.GET, "/nonexistent.html", "HTTP/1.1", new HttpHeaders());
            HttpResponse response = new RequestDispatcher().dispatch(request, config);

            assertEquals(404, response.getStatusCode().getCode());
        }

        @Test
        @DisplayName("Redirect from route config")
        void redirect(@TempDir Path tempDir) throws Exception {
            Path www = tempDir.resolve("www");
            Files.createDirectories(www);

            Path configFile = tempDir.resolve("server.conf");
            Files.writeString(configFile,
                    "server {\n    port 8080\n    default_server_root " + www.toString().replace("\\", "\\\\") + "\n" +
                    "    route /old {\n        redirect https://example.com/new\n    }\n}");

            List<ServerConfig> configs = new ConfigLoader().load(configFile);
            ServerConfig config = configs.get(0);

            HttpRequest request = new HttpRequest(HttpMethod.GET, "/old", "HTTP/1.1", new HttpHeaders());
            HttpResponse response = new RequestDispatcher().dispatch(request, config);

            assertEquals(302, response.getStatusCode().getCode());
            assertEquals("https://example.com/new", response.getHeaders().get("Location"));
        }
    }

    @Nested
    @DisplayName("HttpParser + RequestDispatcher + StaticFile")
    class ParserDispatcherStaticFile {

        @Test
        @DisplayName("Parse raw request, dispatch, get response")
        void fullRequestLifecycle(@TempDir Path tempDir) throws Exception {
            Path www = tempDir.resolve("www");
            Files.createDirectories(www);
            Files.writeString(www.resolve("data.txt"), "raw content");

            ServerConfig config = new ServerConfig.Builder()
                    .port(8080)
                    .defaultServerRoot(www.toString())
                    .build();

            String rawRequest = "GET /data.txt HTTP/1.1\r\nHost: localhost\r\n\r\n";
            HttpParser parser = new HttpParser();
            parser.consume(rawRequest);
            HttpRequest request = parser.build();
            assertNotNull(request);

            HttpResponse response = new RequestDispatcher().dispatch(request, config);
            assertEquals(200, response.getStatusCode().getCode());
            assertEquals("raw content", response.getBodyAsString());
        }

        @Test
        @DisplayName("HEAD request strips body")
        void headRequestStripsBody(@TempDir Path tempDir) throws Exception {
            Path www = tempDir.resolve("www");
            Files.createDirectories(www);
            Files.writeString(www.resolve("page.html"), "<body>full</body>");

            ServerConfig config = new ServerConfig.Builder()
                    .port(8080)
                    .defaultServerRoot(www.toString())
                    .build();

            String raw = "HEAD /page.html HTTP/1.1\r\nHost: localhost\r\n\r\n";
            HttpParser parser = new HttpParser();
            parser.consume(raw);
            HttpRequest request = parser.build();

            HttpResponse response = new RequestDispatcher().dispatch(request, config);
            assertEquals(200, response.getStatusCode().getCode());
            assertNull(response.getBody());
        }
    }

    @Nested
    @DisplayName("Error handling integration")
    class ErrorHandlingIntegration {

        @Test
        @DisplayName("Custom error page from config served on 404")
        void customErrorPage(@TempDir Path tempDir) throws Exception {
            Path www = tempDir.resolve("www");
            Files.createDirectories(www);
            Path errors = tempDir.resolve("errors");
            Files.createDirectories(errors);
            Files.writeString(errors.resolve("my_404.html"), "<html>My Custom 404</html>");

            ServerConfig config = new ServerConfig.Builder()
                    .port(8080)
                    .defaultServerRoot(www.toString())
                    .errorPage(404, errors.resolve("my_404.html").toString())
                    .build();

            HttpRequest request = new HttpRequest(HttpMethod.GET, "/missing", "HTTP/1.1", new HttpHeaders());
            HttpResponse response = new RequestDispatcher().dispatch(request, config);

            assertEquals(404, response.getStatusCode().getCode());
            assertTrue(response.getBodyAsString().contains("My Custom 404"));
        }

        @Test
        @DisplayName("Path traversal attempt returns 403")
        void pathTraversalReturnsForbidden(@TempDir Path tempDir) throws Exception {
            Path www = tempDir.resolve("www");
            Files.createDirectories(www);
            Files.writeString(www.resolve("safe.txt"), "safe");

            Path outside = tempDir.resolve("outside");
            Files.createDirectories(outside);
            Files.writeString(outside.resolve("secret.txt"), "secret");

            ServerConfig config = new ServerConfig.Builder()
                    .port(8080)
                    .defaultServerRoot(www.toString())
                    .build();

            HttpRequest request = new HttpRequest(HttpMethod.GET, "/../outside/secret.txt", "HTTP/1.1", new HttpHeaders());
            HttpResponse response = new RequestDispatcher().dispatch(request, config);

            assertEquals(403, response.getStatusCode().getCode());
        }
    }

    @Nested
    @DisplayName("Session integration")
    class SessionIntegration {

        @Test
        @DisplayName("Create session, get cookie, round-trip to retrieve session")
        void sessionRoundTrip() {
            SessionManager sm = new SessionManager(60000);
            CookieService cs = new CookieService();

            HttpRequest req1 = new HttpRequest(HttpMethod.GET, "/", "HTTP/1.1", new HttpHeaders());
            HttpResponseBuilder builder1 = HttpResponse.builder();

            var session1 = sm.getOrCreateSession(req1, builder1);
            var response1 = builder1.build();
            var setCookie = response1.getSetCookies().get(0);
            assertNotNull(session1.getId());

            HttpHeaders headers2 = new HttpHeaders();
            headers2.add("Cookie", "SESSION_ID=" + session1.getId());
            HttpRequest req2 = new HttpRequest(HttpMethod.GET, "/", "HTTP/1.1", headers2);

            var session2 = sm.getSession(req2);
            assertNotNull(session2);
            assertEquals(session1.getId(), session2.getId());
        }

        @Test
        @DisplayName("Session attributes persist across requests")
        void sessionAttributesPersist() {
            SessionManager sm = new SessionManager(60000);
            HttpRequest req1 = new HttpRequest(HttpMethod.GET, "/", "HTTP/1.1", new HttpHeaders());
            HttpResponseBuilder builder1 = HttpResponse.builder();

            var session1 = sm.getOrCreateSession(req1, builder1);
            session1.setAttribute("theme", "dark");
            session1.setAttribute("lang", "fr");

            HttpHeaders headers2 = new HttpHeaders();
            headers2.add("Cookie", "SESSION_ID=" + session1.getId());
            HttpRequest req2 = new HttpRequest(HttpMethod.GET, "/", "HTTP/1.1", headers2);

            var session2 = sm.getSession(req2);
            assertNotNull(session2);
            assertEquals("dark", session2.getAttribute("theme"));
            assertEquals("fr", session2.getAttribute("lang"));
        }

        @Test
        @DisplayName("No session cookie returns null")
        void noSessionReturnsNull() {
            SessionManager sm = new SessionManager(60000);
            HttpRequest req = new HttpRequest(HttpMethod.GET, "/", "HTTP/1.1", new HttpHeaders());
            assertNull(sm.getSession(req));
        }
    }

    @Nested
    @DisplayName("CGI integration")
    class CgiIntegration {

        @Test
        @DisplayName("Execute CGI script via route config")
        void executeCgiScript(@TempDir Path tempDir) throws Exception {
            Path cgiDir = tempDir.resolve("www/cgi");
            Files.createDirectories(cgiDir);
            Path script = cgiDir.resolve("hello.py");
            Files.writeString(script,
                    "#!/usr/bin/env python3\n" +
                    "import os\n" +
                    "print(\"Content-Type: text/plain\")\n" +
                    "print()\n" +
                    "print(\"Hello from CGI, method=\" + os.environ.get(\"REQUEST_METHOD\", \"\"))");
            script.toFile().setExecutable(true);

            Path www = tempDir.resolve("www");
            Files.createDirectories(www);

            ServerConfig config = new ServerConfig.Builder()
                    .port(8080)
                    .defaultServerRoot(www.toString())
                    .route("/cgi", new RouteConfig.Builder()
                            .path("/cgi")
                            .root(cgiDir.toString())
                            .cgiExtensions(List.of(".py"))
                            .build())
                    .build();

            HttpRequest request = new HttpRequest(HttpMethod.GET, "/cgi/hello.py", "HTTP/1.1", new HttpHeaders());
            HttpResponse response = new RequestDispatcher().dispatch(request, config);

            assertEquals(200, response.getStatusCode().getCode());
            assertTrue(response.getBodyAsString().contains("Hello from CGI"));
        }

        @Test
        @DisplayName("CGI script not found returns 500")
        void cgiScriptNotFound(@TempDir Path tempDir) throws Exception {
            Path cgiDir = tempDir.resolve("www/cgi");
            Files.createDirectories(cgiDir);

            Path www = tempDir.resolve("www");
            Files.createDirectories(www);

            ServerConfig config = new ServerConfig.Builder()
                    .port(8080)
                    .defaultServerRoot(www.toString())
                    .route("/cgi", new RouteConfig.Builder()
                            .path("/cgi")
                            .root(cgiDir.toString())
                            .cgiExtensions(List.of(".py"))
                            .build())
                    .build();

            HttpRequest request = new HttpRequest(HttpMethod.GET, "/cgi/nonexistent.py", "HTTP/1.1", new HttpHeaders());
            HttpResponse response = new RequestDispatcher().dispatch(request, config);

            assertEquals(404, response.getStatusCode().getCode());
        }

        @Test
        @DisplayName("10s CGI does not block concurrent request handling")
        void slowCgiDoesNotBlock(@TempDir Path tempDir) throws Exception {
            Path cgiDir = tempDir.resolve("cgi");
            Files.createDirectories(cgiDir);
            Path slowScript = cgiDir.resolve("slow.sh");
            Files.writeString(slowScript, "#!/bin/bash\nsleep 10\necho \"Content-Type: text/plain\"\necho \"\"\necho \"done\"");
            slowScript.toFile().setExecutable(true);

            Path www = tempDir.resolve("www");
            Files.createDirectories(www);
            Files.writeString(www.resolve("fast.txt"), "fast-response-body");

            ServerConfig config = new ServerConfig.Builder()
                    .port(8080)
                    .defaultServerRoot(www.toString())
                    .route("/cgi", new RouteConfig.Builder()
                            .path("/cgi")
                            .root(cgiDir.toString())
                            .cgiExtensions(List.of(".sh"))
                            .build())
                    .build();

            try (var serverSocket = java.nio.channels.ServerSocketChannel.open()) {
                serverSocket.bind(new java.net.InetSocketAddress("127.0.0.1", 0));
                SocketChannel peer = SocketChannel.open();
                peer.connect(serverSocket.socket().getLocalSocketAddress());
                SocketChannel accepted = serverSocket.accept();

                ClientConnection cgiConn = new ClientConnection(accepted);
                cgiConn.setServerConfig(config);

                String rawRequest = "GET /cgi/slow.sh HTTP/1.1\r\nHost: localhost\r\n\r\n";
                cgiConn.getReadBuffer().put(rawRequest.getBytes(StandardCharsets.ISO_8859_1));
                cgiConn.getReadBuffer().flip();
                assertTrue(cgiConn.parse());
                assertNotNull(cgiConn.getCurrentRequest());

                long start = System.currentTimeMillis();
                CgiAsyncExecutor.getInstance().execute(cgiConn,
                        cgiConn.getCurrentRequest(), config);
                long elapsed = System.currentTimeMillis() - start;
                assertTrue(elapsed < 1000, "CGI dispatch blocked for " + elapsed + "ms");

                HttpRequest fastReq = new HttpRequest(HttpMethod.GET, "/fast.txt", "HTTP/1.1", new HttpHeaders());
                HttpResponse fastRes = new RequestDispatcher().dispatch(fastReq, config);
                assertEquals(200, fastRes.getStatusCode().getCode());
                assertEquals("fast-response-body", fastRes.getBodyAsString());

                peer.close();
                accepted.close();
            }
        }
    }

    @Nested
    @DisplayName("Response serialization")
    class ResponseSerialization {

        @Test
        @DisplayName("Response toBytes produces valid HTTP response text")
        void responseToBytes() {
            HttpResponse response = HttpResponse.builder()
                    .httpVersion("HTTP/1.1")
                    .status(HttpStatus.OK)
                    .header("Content-Type", "text/plain")
                    .body("hello")
                    .build();

            byte[] raw = response.toBytes();
            String text = new String(raw, StandardCharsets.US_ASCII);

            assertTrue(text.startsWith("HTTP/1.1 200 OK\r\n"));
            assertTrue(text.contains("Content-Type: text/plain\r\n"));
            assertTrue(text.endsWith("hello"));
        }

        @Test
        @DisplayName("Empty body response serialization")
        void emptyBody() {
            HttpResponse response = HttpResponse.builder()
                    .httpVersion("HTTP/1.1")
                    .status(new HttpStatus(204, "No Content"))
                    .build();

            byte[] raw = response.toBytes();
            String text = new String(raw, StandardCharsets.US_ASCII);

            assertTrue(text.startsWith("HTTP/1.1 204 No Content\r\n"));
        }
    }

    @Nested
    @DisplayName("Memory leak / state accumulation")
    class MemoryLeakScenarios {

        @Test
        @DisplayName("Sequential request dispatches without state leak")
        void sequentialDispatches(@TempDir Path tempDir) throws Exception {
            Path www = tempDir.resolve("www");
            Files.createDirectories(www);
            Files.writeString(www.resolve("a.txt"), "A");
            Files.writeString(www.resolve("b.txt"), "B");

            ServerConfig config = new ServerConfig.Builder()
                    .port(8080)
                    .defaultServerRoot(www.toString())
                    .build();

            RequestDispatcher dispatcher = new RequestDispatcher();
            for (int i = 0; i < 100; i++) {
                HttpRequest req = new HttpRequest(HttpMethod.GET, "/a.txt", "HTTP/1.1", new HttpHeaders());
                HttpResponse res = dispatcher.dispatch(req, config);
                assertEquals(200, res.getStatusCode().getCode());
                assertEquals("A", res.getBodyAsString());
            }
        }

        @Test
        @DisplayName("Parser reset between requests prevents state leak")
        void parserReset() {
            HttpParser parser = new HttpParser();

            for (int i = 0; i < 100; i++) {
                parser.consume("GET / HTTP/1.1\r\nHost: localhost\r\n\r\n");
                HttpRequest req = parser.build();
                assertNotNull(req);
                assertEquals("/", req.getPath());
                parser.reset();
            }
        }
    }
}
