package webserver.filesystem;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import webserver.config.RouteConfig;
import webserver.config.ServerConfig;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FileSystemTest {

    @Nested
    @DisplayName("MimeTypeResolver")
    class MimeTypeTests {

        private final MimeTypeResolver resolver = new MimeTypeResolver();

        @Test
        @DisplayName("Resolves common extensions")
        void commonExtensions() {
            assertEquals("text/html; charset=utf-8", resolver.resolve("index.html"));
            assertEquals("text/css", resolver.resolve("style.css"));
            assertEquals("application/javascript", resolver.resolve("app.js"));
            assertEquals("application/json", resolver.resolve("data.json"));
            assertEquals("image/png", resolver.resolve("image.png"));
            assertEquals("image/jpeg", resolver.resolve("photo.jpg"));
            assertEquals("image/jpeg", resolver.resolve("photo.jpeg"));
            assertEquals("application/pdf", resolver.resolve("doc.pdf"));
        }

        @Test
        @DisplayName("Unknown extension returns octet-stream")
        void unknownExtension() {
            assertEquals("application/octet-stream", resolver.resolve("file.xyz"));
        }

        @Test
        @DisplayName("No extension returns octet-stream")
        void noExtension() {
            assertEquals("application/octet-stream", resolver.resolve("Makefile"));
        }

        @Test
        @DisplayName("Dot at end returns octet-stream")
        void dotAtEnd() {
            assertEquals("application/octet-stream", resolver.resolve("file."));
        }

        @Test
        @DisplayName("Case insensitive")
        void caseInsensitive() {
            assertEquals("text/html; charset=utf-8", resolver.resolve("index.HTML"));
            assertEquals("image/png", resolver.resolve("image.PNG"));
        }

        @Test
        @DisplayName("All known extensions resolve to non-default types")
        void allKnownExtensions() {
            String[] known = {"html", "htm", "css", "js", "json", "xml", "txt", "csv",
                    "png", "jpg", "jpeg", "gif", "svg", "ico", "webp", "bmp",
                    "pdf", "zip", "gz", "tar", "mp3", "mp4", "webm",
                    "woff", "woff2", "ttf", "otf", "eot", "wasm"};
            for (String ext : known) {
                assertNotEquals("application/octet-stream", resolver.resolve("file." + ext),
                        "Extension ." + ext + " should have a known MIME type");
            }
        }
    }

    @Nested
    @DisplayName("FileService")
    class FileServiceTests {

        private final FileService fileService = new FileService();

        @Test
        @DisplayName("Read existing file")
        void readExistingFile(@TempDir Path tempDir) throws Exception {
            Path file = tempDir.resolve("test.txt");
            Files.writeString(file, "hello world");
            byte[] content = fileService.read(file.toFile());
            assertArrayEquals("hello world".getBytes(), content);
        }

        @Test
        @DisplayName("Exists returns true for existing files")
        void existsTrue(@TempDir Path tempDir) throws Exception {
            Path file = tempDir.resolve("exists.txt");
            Files.writeString(file, "content");
            assertTrue(fileService.exists(file.toFile()));
        }

        @Test
        @DisplayName("Exists returns false for non-existing files")
        void existsFalse() {
            assertFalse(fileService.exists(new File("/nonexistent/file.txt")));
        }

        @Test
        @DisplayName("isDirectory for directories")
        void isDirectory(@TempDir Path tempDir) {
            assertTrue(fileService.isDirectory(tempDir.toFile()));
        }

        @Test
        @DisplayName("isDirectory false for files")
        void isDirectoryFalse(@TempDir Path tempDir) throws Exception {
            Path file = tempDir.resolve("file.txt");
            Files.writeString(file, "content");
            assertFalse(fileService.isDirectory(file.toFile()));
        }
    }

    @Nested
    @DisplayName("DirectoryScanner")
    class DirectoryScannerTests {

        private final DirectoryScanner scanner = new DirectoryScanner();

        @Test
        @DisplayName("List files in directory")
        void listFiles(@TempDir Path tempDir) throws Exception {
            Files.writeString(tempDir.resolve("a.txt"), "a");
            Files.writeString(tempDir.resolve("b.txt"), "b");
            assertEquals(2, scanner.listFiles(tempDir.toFile()).size());
        }

        @Test
        @DisplayName("List files with prefix")
        void listFilesWithPrefix(@TempDir Path tempDir) throws Exception {
            Files.writeString(tempDir.resolve("foo_a.txt"), "a");
            Files.writeString(tempDir.resolve("foo_b.txt"), "b");
            Files.writeString(tempDir.resolve("bar.txt"), "c");
            List<File> prefixed = scanner.listFiles(tempDir.toFile(), "foo");
            assertEquals(2, prefixed.size());
        }

        @Test
        @DisplayName("Empty directory returns empty list")
        void emptyDirectory(@TempDir Path tempDir) {
            assertTrue(scanner.listFiles(tempDir.toFile()).isEmpty());
        }

        @Test
        @DisplayName("Non-existent directory returns empty list")
        void nonExistentDirectory() {
            assertTrue(scanner.listFiles(new File("/nonexistent")).isEmpty());
        }
    }

    @Nested
    @DisplayName("PathResolver")
    class PathResolverTests {

        private final PathResolver resolver = new PathResolver();

        @Test
        @DisplayName("Resolve path with route root")
        void resolveWithRouteRoot(@TempDir Path tempDir) throws Exception {
            String root = tempDir.toAbsolutePath().toString();
            RouteConfig route = new RouteConfig.Builder().path("/api").root(root).build();
            ServerConfig config = new ServerConfig.Builder().port(8080).build();

            Path file = tempDir.resolve("test.txt");
            Files.writeString(file, "content");

            var resolved = resolver.resolve("/api/test.txt", route, config);
            assertTrue(resolved.secure());
            assertEquals(file.toFile().getCanonicalPath(), resolved.file().getCanonicalPath());
        }

        @Test
        @DisplayName("Resolve path with server default root")
        void resolveWithServerRoot(@TempDir Path tempDir) throws Exception {
            String root = tempDir.toAbsolutePath().toString();
            ServerConfig config = new ServerConfig.Builder().port(8080).defaultServerRoot(root).build();

            Path file = tempDir.resolve("test.txt");
            Files.writeString(file, "content");

            var resolved = resolver.resolve("/test.txt", null, config);
            assertTrue(resolved.secure());
            assertEquals(file.toFile().getCanonicalPath(), resolved.file().getCanonicalPath());
        }

        @Test
        @DisplayName("Path traversal is detected as insecure")
        void pathTraversal(@TempDir Path tempDir) throws Exception {
            String root = tempDir.toAbsolutePath().toString();
            ServerConfig config = new ServerConfig.Builder().port(8080).defaultServerRoot(root).build();

            var resolved = resolver.resolve("/../etc/passwd", null, config);
            assertFalse(resolved.secure());
        }

        @Test
        @DisplayName("Path with route prefix stripped")
        void routePrefixStripped(@TempDir Path tempDir) throws Exception {
            String root = tempDir.toAbsolutePath().toString();
            RouteConfig route = new RouteConfig.Builder().path("/app").root(root).build();
            ServerConfig config = new ServerConfig.Builder().port(8080).build();

            Path file = tempDir.resolve("index.html");
            Files.writeString(file, "content");

            var resolved = resolver.resolve("/app/index.html", route, config);
            assertTrue(resolved.secure());
            assertEquals(file.toFile().getCanonicalPath(), resolved.file().getCanonicalPath());
        }
    }

    @Nested
    @DisplayName("UploadStorage")
    class UploadStorageTests {

        @Test
        @DisplayName("Store file creates temp file")
        void storeFile() throws Exception {
            UploadStorage storage = new UploadStorage();
            byte[] data = "file content".getBytes();
            UploadStorage.StoredFile stored = storage.store("test.txt", data, "text/plain");

            assertEquals("test.txt", stored.originalName());
            assertEquals("text/plain", stored.contentType());
            assertNotNull(stored.tempPath());
            assertTrue(Files.exists(Path.of(stored.tempPath())));
            assertArrayEquals(data, Files.readAllBytes(Path.of(stored.tempPath())));

            storage.cleanup();
        }

        @Test
        @DisplayName("Sanitize filename removes special chars")
        void sanitizeFilename() throws Exception {
            UploadStorage storage = new UploadStorage();
            UploadStorage.StoredFile stored = storage.store("../malicious!file.txt", new byte[]{1}, "text/plain");
            assertTrue(stored.tempPath().contains("malicious_file.txt"));
            storage.cleanup();
        }

        @Test
        @DisplayName("Cleanup removes all temp files")
        void cleanup() throws Exception {
            UploadStorage storage = new UploadStorage();
            UploadStorage.StoredFile stored = storage.store("keep.txt", new byte[]{1}, "text/plain");
            Path tempPath = Path.of(stored.tempPath());
            assertTrue(Files.exists(tempPath));
            storage.cleanup();
            assertFalse(Files.exists(tempPath));
        }

        @Test
        @DisplayName("Cleanup is idempotent")
        void cleanupIdempotent() throws Exception {
            UploadStorage storage = new UploadStorage();
            storage.store("f.txt", new byte[]{1}, "text/plain");
            storage.cleanup();
            assertDoesNotThrow(storage::cleanup);
        }

        @Test
        @DisplayName("Store 100 files and cleanup all")
        void manyFiles() throws Exception {
            UploadStorage storage = new UploadStorage();
            for (int i = 0; i < 100; i++) {
                storage.store("file" + i + ".txt", ("content" + i).getBytes(), "text/plain");
            }
            storage.cleanup();
        }
    }
}
