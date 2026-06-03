package webserver.cgi;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import webserver.config.RouteConfig;
import webserver.config.ServerConfig;
import webserver.http.HttpHeaders;
import webserver.http.HttpMethod;
import webserver.http.HttpRequest;
import webserver.http.HttpStatus;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CgiTest {

    @Nested
    @DisplayName("CgiEnvironmentBuilder")
    class EnvironmentBuilderTests {

        private final CgiEnvironmentBuilder builder = new CgiEnvironmentBuilder();

        @Test
        @DisplayName("Builds standard CGI environment")
        void standardEnvironment() {
            HttpRequest req = new HttpRequest(HttpMethod.GET, "/test.py", "HTTP/1.1", new HttpHeaders());
            RouteConfig route = new RouteConfig.Builder().path("/cgi").root("/var/www/cgi").build();
            ServerConfig config = new ServerConfig.Builder().port(8080).host("localhost").build();

            Map<String, String> env = builder.build(req, route, config, "/cgi/test.py", null, 8080);

            assertEquals("CGI/1.1", env.get("GATEWAY_INTERFACE"));
            assertEquals("HTTP/1.1", env.get("SERVER_PROTOCOL"));
            assertEquals("localhost", env.get("SERVER_NAME"));
            assertEquals("8080", env.get("SERVER_PORT"));
            assertEquals("GET", env.get("REQUEST_METHOD"));
            assertEquals("/cgi/test.py", env.get("SCRIPT_NAME"));
            assertEquals("", env.get("QUERY_STRING"));
        }

        @Test
        @DisplayName("Includes PATH_INFO and PATH_TRANSLATED when pathInfo provided")
        void pathInfo() {
            HttpRequest req = new HttpRequest(HttpMethod.GET, "/cgi/test.py/extra", "HTTP/1.1", new HttpHeaders());
            RouteConfig route = new RouteConfig.Builder().path("/cgi").root("/var/www").build();
            ServerConfig config = new ServerConfig.Builder().port(8080).defaultServerRoot("/var/www").build();

            Map<String, String> env = builder.build(req, route, config, "/cgi/test.py", "/extra", 8080);

            assertEquals("/extra", env.get("PATH_INFO"));
            assertEquals("/var/www/extra", env.get("PATH_TRANSLATED"));
        }

        @Test
        @DisplayName("Includes CONTENT_TYPE and CONTENT_LENGTH for POST")
        void contentHeaders() {
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Type", "application/x-www-form-urlencoded");
            HttpRequest req = new HttpRequest(HttpMethod.POST, "/submit", "HTTP/1.1", headers, "data=hello".getBytes());
            RouteConfig route = new RouteConfig.Builder().path("/api").root("/var/www").build();
            ServerConfig config = new ServerConfig.Builder().port(8080).build();

            Map<String, String> env = builder.build(req, route, config, "/api/submit", null, 8080);

            assertEquals("application/x-www-form-urlencoded", env.get("CONTENT_TYPE"));
            assertEquals("10", env.get("CONTENT_LENGTH"));
        }

        @Test
        @DisplayName("Includes HTTP_ prefix headers")
        void httpHeaders() {
            HttpHeaders headers = new HttpHeaders();
            headers.add("User-Agent", "test-agent");
            headers.add("Accept", "text/html");
            HttpRequest req = new HttpRequest(HttpMethod.GET, "/", "HTTP/1.1", headers);
            ServerConfig config = new ServerConfig.Builder().port(8080).build();

            Map<String, String> env = builder.build(req, null, config, "/script", null, 8080);

            assertEquals("test-agent", env.get("HTTP_USER_AGENT"));
            assertEquals("text/html", env.get("HTTP_ACCEPT"));
        }

        @Test
        @DisplayName("Content-Type and Content-Length are not duplicated as HTTP_")
        void noDuplicateContentHeaders() {
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Type", "text/plain");
            HttpRequest req = new HttpRequest(HttpMethod.POST, "/", "HTTP/1.1", headers, "body".getBytes());
            ServerConfig config = new ServerConfig.Builder().port(8080).build();

            Map<String, String> env = builder.build(req, null, config, "/script", null, 8080);

            assertNull(env.get("HTTP_CONTENT_TYPE"));
            assertNull(env.get("HTTP_CONTENT_LENGTH"));
            assertEquals("text/plain", env.get("CONTENT_TYPE"));
        }

        @Test
        @DisplayName("Query string is included")
        void queryString() {
            HttpRequest req = new HttpRequest(HttpMethod.GET, "/search?q=test", "HTTP/1.1", new HttpHeaders());
            ServerConfig config = new ServerConfig.Builder().port(8080).build();

            Map<String, String> env = builder.build(req, null, config, "/search", null, 8080);

            assertEquals("q=test", env.get("QUERY_STRING"));
        }
    }

    @Nested
    @DisplayName("CgiPathResolver")
    class PathResolverTests {

        private final CgiPathResolver resolver = new CgiPathResolver();

        @Test
        @DisplayName("Resolves CGI script path")
        void resolveCgiScript(@TempDir Path tempDir) throws Exception {
            Path root = tempDir.resolve("www/cgi");
            Files.createDirectories(root);
            Path script = root.resolve("test.py");
            Files.writeString(script, "#!/usr/bin/env python3\nprint('hello')\n");
            script.toFile().setExecutable(true);

            RouteConfig route = new RouteConfig.Builder().path("/cgi").root(root.toString())
                    .cgiExtensions(List.of(".py")).build();
            ServerConfig config = new ServerConfig.Builder().port(8080).defaultServerRoot("www").build();

            CgiPathResolver.CgiResolvedPath resolved = resolver.resolve("/cgi/test.py", route, config);
            assertNotNull(resolved);
            assertEquals(script.toFile().getCanonicalPath(), resolved.scriptFile().getCanonicalPath());
            assertEquals("/cgi/test.py", resolved.scriptName());
            assertEquals("", resolved.pathInfo());
        }

        @Test
        @DisplayName("Returns null when no CGI extensions configured")
        void noCgiExtensions() {
            RouteConfig route = new RouteConfig.Builder().path("/cgi").root("/tmp").build();
            ServerConfig config = new ServerConfig.Builder().port(8080).build();

            assertNull(resolver.resolve("/cgi/test.py", route, config));
        }

        @Test
        @DisplayName("Returns null when file not found")
        void fileNotFound(@TempDir Path tempDir) {
            RouteConfig route = new RouteConfig.Builder().path("/cgi").root(tempDir.toString())
                    .cgiExtensions(List.of(".py")).build();
            ServerConfig config = new ServerConfig.Builder().port(8080).build();

            assertNull(resolver.resolve("/cgi/nonexistent.py", route, config));
        }

        @Test
        @DisplayName("Includes PATH_INFO for path after script")
        void pathInfo(@TempDir Path tempDir) throws Exception {
            Path root = tempDir.resolve("www/cgi");
            Files.createDirectories(root);
            Path script = root.resolve("app.py");
            Files.writeString(script, "#!/usr/bin/env python3\nprint('hello')\n");
            script.toFile().setExecutable(true);

            RouteConfig route = new RouteConfig.Builder().path("/cgi").root(root.toString())
                    .cgiExtensions(List.of(".py")).build();
            ServerConfig config = new ServerConfig.Builder().port(8080).build();

            CgiPathResolver.CgiResolvedPath resolved = resolver.resolve("/cgi/app.py/extra/path", route, config);
            assertNotNull(resolved);
            assertEquals("/cgi/app.py", resolved.scriptName());
            assertEquals("/extra/path", resolved.pathInfo());
        }
    }

    @Nested
    @DisplayName("ProcessOutputParser")
    class ProcessOutputParserTests {

        private final ProcessOutputParser parser = new ProcessOutputParser();

        @Test
        @DisplayName("Parse simple CGI output with headers")
        void simpleOutput() {
            String raw = "Content-Type: text/html\r\n\r\n<html><body>Hello</body></html>";
            ProcessOutputParser.ParsedCgiOutput result = parser.parse(raw.getBytes(StandardCharsets.UTF_8));
            assertEquals(200, result.status().getCode());
            assertEquals("text/html", result.headers().get("Content-Type"));
            assertEquals("<html><body>Hello</body></html>", new String(result.body(), StandardCharsets.UTF_8));
        }

        @Test
        @DisplayName("Parse CGI output with Status header")
        void withStatus() {
            String raw = "Status: 404 Not Found\r\nContent-Type: text/html\r\n\r\nNot Found";
            var result = parser.parse(raw.getBytes(StandardCharsets.UTF_8));
            assertEquals(404, result.status().getCode());
            assertEquals("Not Found", result.status().getReasonPhrase());
            assertEquals("Not Found", new String(result.body(), StandardCharsets.UTF_8));
        }

        @Test
        @DisplayName("Default Content-Type when missing")
        void defaultContentType() {
            String raw = "\r\nHello";
            var result = parser.parse(raw.getBytes(StandardCharsets.UTF_8));
            assertEquals("text/html", result.headers().get("Content-Type"));
        }

        @Test
        @DisplayName("Empty output returns OK with empty body")
        void emptyOutput() {
            var result = parser.parse(new byte[0]);
            assertEquals(200, result.status().getCode());
            assertEquals("text/plain", result.headers().get("Content-Type"));
            assertEquals(0, result.body().length);
        }

        @Test
        @DisplayName("Null output returns OK with empty body")
        void nullOutput() {
            var result = parser.parse(null);
            assertEquals(200, result.status().getCode());
        }

        @Test
        @DisplayName("Parse with LF-only line endings")
        void lfOnly() {
            String raw = "Content-Type: application/json\n\n{\"key\":\"value\"}";
            var result = parser.parse(raw.getBytes(StandardCharsets.UTF_8));
            assertEquals("application/json", result.headers().get("Content-Type"));
            assertEquals("{\"key\":\"value\"}", new String(result.body(), StandardCharsets.UTF_8));
        }

        @Test
        @DisplayName("Status header is removed from output headers")
        void statusRemoved() {
            String raw = "Status: 200 OK\r\nContent-Type: text/plain\r\n\r\nbody";
            var result = parser.parse(raw.getBytes(StandardCharsets.UTF_8));
            assertNull(result.headers().get("Status"));
        }
    }

    @Nested
    @DisplayName("CgiExecutor (with real script)")
    class CgiExecutorTests {

        @Test
        @DisplayName("Execute simple echo CGI script")
        void executeEchoScript(@TempDir Path tempDir) throws Exception {
            Path script = tempDir.resolve("echo.sh");
            Files.writeString(script, "#!/bin/bash\necho \"Content-Type: text/plain\"\necho \"\"\necho \"Hello from CGI\"");
            script.toFile().setExecutable(true);

            CgiExecutor executor = new CgiExecutor();
            CgiExecutor.CgiResult result = executor.execute(script.toFile(), Map.of(
                    "GATEWAY_INTERFACE", "CGI/1.1",
                    "REQUEST_METHOD", "GET"
            ), new byte[0], 5000);

            assertEquals(200, result.statusCode());
            String output = new String(result.rawOutput(), StandardCharsets.UTF_8);
            assertTrue(output.contains("Hello from CGI"));
        }

        @Test
        @DisplayName("Execute script with request body")
        void executeWithBody(@TempDir Path tempDir) throws Exception {
            Path script = tempDir.resolve("read.sh");
            Files.writeString(script, "#!/bin/bash\nread input\necho \"Content-Type: text/plain\"\necho \"\"\necho \"Received: $input\"");
            script.toFile().setExecutable(true);

            CgiExecutor executor = new CgiExecutor();
            byte[] body = "hello".getBytes(StandardCharsets.UTF_8);
            CgiExecutor.CgiResult result = executor.execute(script.toFile(), Map.of(
                    "GATEWAY_INTERFACE", "CGI/1.1",
                    "REQUEST_METHOD", "POST",
                    "CONTENT_LENGTH", "5"
            ), body, 5000);

            assertEquals(200, result.statusCode());
            String output = new String(result.rawOutput(), StandardCharsets.UTF_8);
            assertTrue(output.contains("Received: hello"));
        }

        @Test
        @DisplayName("Time out long-running script")
        void timeout(@TempDir Path tempDir) throws Exception {
            Path script = tempDir.resolve("slow.sh");
            Files.writeString(script, "#!/bin/bash\nsleep 10\necho \"done\"");
            script.toFile().setExecutable(true);

            CgiExecutor executor = new CgiExecutor();
            CgiExecutor.CgiResult result = executor.execute(script.toFile(), Map.of(), new byte[0], 100);

            assertEquals(504, result.statusCode());
        }

        @Test
        @DisplayName("Non-existent script returns error")
        void nonExistentScript() {
            CgiExecutor executor = new CgiExecutor();
            CgiExecutor.CgiResult result = executor.execute(new File("/nonexistent/script.sh"), Map.of(), new byte[0], 1000);
            assertEquals(500, result.statusCode());
        }
    }

    @Nested
    @DisplayName("Memory leak scenarios")
    class MemoryLeakScenarios {

        @Test
        @DisplayName("Build 1000 environments without issues")
        void manyEnvironments() {
            CgiEnvironmentBuilder builder = new CgiEnvironmentBuilder();
            for (int i = 0; i < 1000; i++) {
                HttpRequest req = new HttpRequest(HttpMethod.GET, "/path", "HTTP/1.1", new HttpHeaders());
                ServerConfig config = new ServerConfig.Builder().port(8080).build();
                Map<String, String> env = builder.build(req, null, config, "/script", null, 8080);
                assertEquals("CGI/1.1", env.get("GATEWAY_INTERFACE"));
            }
        }

        @Test
        @DisplayName("Parse 1000 CGI outputs")
        void manyParses() {
            ProcessOutputParser parser = new ProcessOutputParser();
            for (int i = 0; i < 1000; i++) {
                String raw = "Content-Type: text/plain\r\n\r\n" + i;
                var result = parser.parse(raw.getBytes(StandardCharsets.UTF_8));
                assertEquals(200, result.status().getCode());
            }
        }

        @Test
        @DisplayName("Execute 10 CGI scripts sequentially")
        void multipleExecutions(@TempDir Path tempDir) throws Exception {
            Path script = tempDir.resolve("counter.sh");
            Files.writeString(script, "#!/bin/bash\necho \"Content-Type: text/plain\"\necho \"\"\necho \"ok\"");
            script.toFile().setExecutable(true);

            CgiExecutor executor = new CgiExecutor();
            for (int i = 0; i < 10; i++) {
                CgiExecutor.CgiResult result = executor.execute(script.toFile(), Map.of("REQUEST_METHOD", "GET"), new byte[0], 5000);
                assertEquals(200, result.statusCode());
            }
        }
    }
}
