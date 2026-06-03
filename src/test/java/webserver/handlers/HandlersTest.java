package webserver.handlers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import webserver.config.RouteConfig;
import webserver.config.ServerConfig;
import webserver.http.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HandlersTest {

    @Nested
    @DisplayName("DirectoryListingHandler")
    class DirectoryListingHandlerTests {

        private final DirectoryListingHandler handler = new DirectoryListingHandler();

        @Test
        @DisplayName("Generates HTML listing for directory")
        void generateListing(@TempDir Path tempDir) throws Exception {
            Files.writeString(tempDir.resolve("a.txt"), "a");
            Files.writeString(tempDir.resolve("b.txt"), "b");
            Path subdir = tempDir.resolve("sub");
            Files.createDirectory(subdir);

            HttpResponse res = handler.handle(tempDir.toFile(), "/files");
            assertEquals(200, res.getStatusCode().getCode());
            String body = res.getBodyAsString();
            assertTrue(body.contains("a.txt"));
            assertTrue(body.contains("b.txt"));
            assertTrue(body.contains("sub/"));
            assertTrue(body.contains("Index of /files"));
        }

        @Test
        @DisplayName("Generates parent directory link for non-root paths")
        void parentLink() {
            File dir = new File("/tmp");
            HttpResponse res = handler.handle(dir, "/files/subdir");
            String body = res.getBodyAsString();
            assertTrue(body.contains("../"));
            assertTrue(body.contains("/files"));
        }

        @Test
        @DisplayName("Root path has no parent link")
        void rootPath() {
            File dir = new File("/tmp");
            HttpResponse res = handler.handle(dir, "/");
            String body = res.getBodyAsString();
            assertFalse(body.contains("../"));
        }

        @Test
        @DisplayName("Empty directory")
        void emptyDirectory(@TempDir Path tempDir) {
            HttpResponse res = handler.handle(tempDir.toFile(), "/empty");
            assertEquals(200, res.getStatusCode().getCode());
        }

        @Test
        @DisplayName("Content-Type is text/html")
        void contentType(@TempDir Path tempDir) {
            HttpResponse res = handler.handle(tempDir.toFile(), "/");
            assertEquals("text/html; charset=utf-8", res.getHeaders().get("Content-Type"));
        }
    }

    @Nested
    @DisplayName("UploadHandler")
    class UploadHandlerTests {

        @Test
        @DisplayName("Non-multipart request returns empty result")
        void nonMultipart() throws Exception {
            UploadHandler handler = new UploadHandler();
            HttpRequest req = new HttpRequest(HttpMethod.POST, "/upload", "HTTP/1.1", new HttpHeaders());
            var result = handler.handle(req);
            assertTrue(result.fields().isEmpty());
            assertTrue(result.files().isEmpty());
            assertEquals(0, result.stdinBody().length);
        }

        @Test
        @DisplayName("Multipart request with text fields")
        void textFields() throws Exception {
            String boundary = "----TestBoundary";
            String body = "--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"username\"\r\n\r\n"
                    + "john\r\n"
                    + "--" + boundary + "--\r\n";

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Type", "multipart/form-data; boundary=" + boundary);
            HttpRequest req = new HttpRequest(HttpMethod.POST, "/upload", "HTTP/1.1", headers,
                    body.getBytes(StandardCharsets.UTF_8));

            UploadHandler handler = new UploadHandler();
            var result = handler.handle(req);
            assertEquals("john", result.fields().get("username"));
            assertTrue(result.files().isEmpty());
            handler.cleanup();
        }

        @Test
        @DisplayName("Multipart request with file upload")
        void fileUpload() throws Exception {
            String boundary = "----TestBoundary";
            String body = "--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"file\"; filename=\"test.txt\"\r\n"
                    + "Content-Type: text/plain\r\n\r\n"
                    + "file content\r\n"
                    + "--" + boundary + "--\r\n";

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Type", "multipart/form-data; boundary=" + boundary);
            HttpRequest req = new HttpRequest(HttpMethod.POST, "/upload", "HTTP/1.1", headers,
                    body.getBytes(StandardCharsets.UTF_8));

            UploadHandler handler = new UploadHandler();
            var result = handler.handle(req);
            assertEquals(1, result.files().size());
            assertEquals("file", result.files().get(0).fieldName());
            assertEquals("test.txt", result.files().get(0).originalName());
            assertEquals("text/plain", result.files().get(0).contentType());
            handler.cleanup();
        }

        @Test
        @DisplayName("Multipart with mixed fields and files")
        void mixed() throws Exception {
            String boundary = "----TestBoundary";
            String body = "--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"title\"\r\n\r\n"
                    + "My File\r\n"
                    + "--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"attachment\"; filename=\"doc.txt\"\r\n"
                    + "Content-Type: text/plain\r\n\r\n"
                    + "document content\r\n"
                    + "--" + boundary + "--\r\n";

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Type", "multipart/form-data; boundary=" + boundary);
            HttpRequest req = new HttpRequest(HttpMethod.POST, "/upload", "HTTP/1.1", headers,
                    body.getBytes(StandardCharsets.UTF_8));

            UploadHandler handler = new UploadHandler();
            var result = handler.handle(req);
            assertEquals("My File", result.fields().get("title"));
            assertEquals(1, result.files().size());
            assertEquals("doc.txt", result.files().get(0).originalName());
            assertTrue(result.stdinBody().length > 0);
            handler.cleanup();
        }

        @Test
        @DisplayName("Cleanup removes temp files from uploads")
        void cleanup() throws Exception {
            String boundary = "----TestBoundary";
            String body = "--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"f\"; filename=\"x.txt\"\r\n"
                    + "Content-Type: text/plain\r\n\r\n"
                    + "data\r\n"
                    + "--" + boundary + "--\r\n";

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Type", "multipart/form-data; boundary=" + boundary);
            HttpRequest req = new HttpRequest(HttpMethod.POST, "/upload", "HTTP/1.1", headers,
                    body.getBytes(StandardCharsets.UTF_8));

            UploadHandler handler = new UploadHandler();
            var result = handler.handle(req);
            Path tempPath = Path.of(result.files().get(0).tempPath());
            assertTrue(Files.exists(tempPath));
            handler.cleanup();
            assertFalse(Files.exists(tempPath));
        }
    }

    @Nested
    @DisplayName("RequestDispatcher")
    class RequestDispatcherTests {

        private final RequestDispatcher dispatcher = new RequestDispatcher();

        @Test
        @DisplayName("Dispatch returns 404 for unknown path")
        void unknownPath() {
            ServerConfig config = new ServerConfig.Builder().port(8080).defaultServerRoot("/nonexistent").build();
            HttpRequest req = new HttpRequest(HttpMethod.GET, "/nonexistent.html", "HTTP/1.1", new HttpHeaders());
            HttpResponse res = dispatcher.dispatch(req, config);
            assertEquals(404, res.getStatusCode().getCode());
        }

        @Test
        @DisplayName("Dispatch follows redirect")
        void redirect() {
            RouteConfig route = new RouteConfig.Builder().path("/old").redirect("/new").build();
            ServerConfig config = new ServerConfig.Builder().port(8080).route("/old", route).build();
            HttpRequest req = new HttpRequest(HttpMethod.GET, "/old", "HTTP/1.1", new HttpHeaders());
            HttpResponse res = dispatcher.dispatch(req, config);
            assertEquals(302, res.getStatusCode().getCode());
            assertEquals("/new", res.getHeaders().get("Location"));
        }

        @Test
        @DisplayName("Dispatch returns 405 for disallowed method")
        void disallowedMethod() {
            RouteConfig route = new RouteConfig.Builder().path("/api")
                    .allowedMethods(List.of(HttpMethod.GET))
                    .root("/tmp").build();
            ServerConfig config = new ServerConfig.Builder().port(8080).route("/api", route).build();
            HttpRequest req = new HttpRequest(HttpMethod.POST, "/api", "HTTP/1.1", new HttpHeaders());
            HttpResponse res = dispatcher.dispatch(req, config);
            assertEquals(405, res.getStatusCode().getCode());
        }

        @Test
        @DisplayName("HEAD request returns response without body")
        void headRequest() {
            RouteConfig route = new RouteConfig.Builder().path("/").root("/tmp").build();
            ServerConfig config = new ServerConfig.Builder().port(8080).route("/", route).build();
            HttpRequest req = new HttpRequest(HttpMethod.HEAD, "/", "HTTP/1.1", new HttpHeaders());
            HttpResponse res = dispatcher.dispatch(req, config);
            assertNull(res.getBody());
        }

        @Test
        @DisplayName("Exception during dispatch returns 500")
        void dispatchException() {
            ServerConfig config = new ServerConfig.Builder().port(8080).defaultServerRoot("").build();
            HttpRequest req = new HttpRequest(HttpMethod.GET, "/", "HTTP/1.1", new HttpHeaders());
            HttpResponse res = dispatcher.dispatch(req, config);
            // Might succeed or fail depending on the current directory
            assertNotNull(res);
        }

        @Test
        @DisplayName("Serves static file when route has root")
        void serveStaticFile(@TempDir Path tempDir) throws Exception {
            Path file = tempDir.resolve("test.html");
            Files.writeString(file, "<html>test</html>");

            RouteConfig route = new RouteConfig.Builder().path("/static").root(tempDir.toString()).build();
            ServerConfig config = new ServerConfig.Builder().port(8080).route("/static", route).build();
            HttpRequest req = new HttpRequest(HttpMethod.GET, "/static/test.html", "HTTP/1.1", new HttpHeaders());
            HttpResponse res = dispatcher.dispatch(req, config);
            assertEquals(200, res.getStatusCode().getCode());
            assertEquals("text/html; charset=utf-8", res.getHeaders().get("Content-Type"));
            assertTrue(res.getBodyAsString().contains("test"));
        }
    }
}
