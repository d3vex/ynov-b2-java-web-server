package webserver.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import webserver.http.HttpMethod;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConfigLoaderTest {

    private final ConfigLoader loader = new ConfigLoader();

    @Nested
    @DisplayName("Load from file")
    class LoadFromFile {

        @Test
        @DisplayName("Load valid config file produces ServerConfigs")
        void loadValidConfig(@TempDir Path tempDir) throws IOException {
            Path config = tempDir.resolve("server.conf");
            Files.writeString(config, """
                    server {
                        host 0.0.0.0
                        port 8080
                        default_server_root www
                        timeout 30000
                        client_body_limit 1048576
                        directory_listing false
                        error_page 404 /errors/404.html
                        allowed_methods GET HEAD POST
                        cgi_extensions .py

                        route /api {
                            root /api/www
                            default_file api.html
                            timeout 60000
                            client_body_limit 512
                            directory_listing true
                            error_page 403 /errors/403.html
                            allowed_methods GET
                            cgi_extensions .sh
                        }

                        route /docs {
                            redirect https://docs.example.com
                        }
                    }
                    """);

            List<ServerConfig> configs = loader.load(config);
            assertEquals(1, configs.size());
            ServerConfig sc = configs.get(0);

            assertEquals("0.0.0.0", sc.getHost());
            assertEquals(List.of(8080), sc.getPorts());
            assertEquals("www", sc.getDefaultServerRoot());
            assertEquals(30000, sc.getTimeoutMs());
            assertEquals(1048576, sc.getClientBodyLimit());
            assertFalse(sc.getDirectoryListing());
            assertEquals("/errors/404.html", sc.getErrorPages().get(404));
            assertEquals(List.of(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.POST), sc.getAllowedMethods());
            assertEquals(List.of(".py"), sc.getCgiExtensions());
        }

        @Test
        @DisplayName("Route configs are correctly populated")
        void routeConfigs(@TempDir Path tempDir) throws IOException {
            Path config = tempDir.resolve("server.conf");
            Files.writeString(config, """
                    server {
                        port 8080
                        route /api {
                            root /api/www
                            default_file api.html
                            timeout 60000
                            client_body_limit 512
                            directory_listing true
                            error_page 403 /errors/403.html
                            allowed_methods GET
                            cgi_extensions .sh
                        }
                        route /docs {
                            redirect https://docs.example.com
                        }
                    }
                    """);

            List<ServerConfig> configs = loader.load(config);
            assertEquals(1, configs.size());
            ServerConfig sc = configs.get(0);

            RouteConfig api = sc.getRoutes().get("/api");
            assertNotNull(api);
            assertEquals("/api", api.getPath());
            assertEquals("/api/www", api.getRoot());
            assertEquals("api.html", api.getDefaultFile());
            assertEquals(60000, api.getTimeoutMs().longValue());
            assertEquals(512, api.getClientBodyLimit().longValue());
            assertTrue(api.getDirectoryListing());
            assertEquals("/errors/403.html", api.getErrorPages().get(403));
            assertEquals(List.of(HttpMethod.GET), api.getAllowedMethods());
            assertEquals(List.of(".sh"), api.getCgiExtensions());

            RouteConfig docs = sc.getRoutes().get("/docs");
            assertNotNull(docs);
            assertEquals("/docs", docs.getPath());
            assertEquals("https://docs.example.com", docs.getRedirect());
        }

        @Test
        @DisplayName("Config with validation errors throws ConfigLoadException")
        void invalidConfigThrows(@TempDir Path tempDir) {
            Path config = tempDir.resolve("bad.conf");
            String content = """
                    server {
                        port abc
                    }
                    """;
            assertDoesNotThrow(() -> Files.writeString(config, content));
            assertThrows(ConfigLoader.ConfigLoadException.class, () -> loader.load(config));
        }

        @Test
        @DisplayName("Missing file throws IOException")
        void missingFile() {
            assertThrows(IOException.class, () -> loader.load("/nonexistent/path/server.conf"));
        }

        @Test
        @DisplayName("Load from String path overload")
        void loadFromStringPath(@TempDir Path tempDir) throws IOException {
            Path config = tempDir.resolve("server.conf");
            Files.writeString(config, "server {\n  port 8080\n}");
            List<ServerConfig> configs = loader.load(config.toString());
            assertEquals(1, configs.size());
            assertEquals(List.of(8080), configs.get(0).getPorts());
        }

        @Test
        @DisplayName("Error message from validation is included in exception")
        void errorMessageIncluded(@TempDir Path tempDir) throws IOException {
            Path config = tempDir.resolve("bad.conf");
            Files.writeString(config, """
                    server {
                        port abc
                        timeout xyz
                    }
                    """);
            ConfigLoader.ConfigLoadException ex = assertThrows(
                    ConfigLoader.ConfigLoadException.class, () -> loader.load(config));
            assertTrue(ex.getMessage().contains("port"));
            assertTrue(ex.getMessage().contains("timeout"));
        }
    }

    @Nested
    @DisplayName("Conversion to ServerConfig")
    class Conversion {

        @Test
        @DisplayName("Multiple ports are collected")
        void multiplePorts(@TempDir Path tempDir) throws IOException {
            Path config = tempDir.resolve("server.conf");
            Files.writeString(config, "server {\n  port 8080\n  port 9090\n  port 10000\n}");
            List<ServerConfig> configs = loader.load(config);
            assertEquals(List.of(8080, 9090, 10000), configs.get(0).getPorts());
        }

        @Test
        @DisplayName("Multiple error_pages are collected")
        void multipleErrorPages(@TempDir Path tempDir) throws IOException {
            Path config = tempDir.resolve("server.conf");
            Files.writeString(config, """
                    server {
                        port 8080
                        error_page 404 /404.html
                        error_page 500 /500.html
                    }
                    """);
            List<ServerConfig> configs = loader.load(config);
            assertEquals("/404.html", configs.get(0).getErrorPages().get(404));
            assertEquals("/500.html", configs.get(0).getErrorPages().get(500));
        }

        @Test
        @DisplayName("Multiple servers are returned")
        void multipleServers(@TempDir Path tempDir) throws IOException {
            Path config = tempDir.resolve("server.conf");
            Files.writeString(config, """
                    server { port 8080 }
                    server { port 9090 }
                    """);
            List<ServerConfig> configs = loader.load(config);
            assertEquals(2, configs.size());
            assertEquals(List.of(8080), configs.get(0).getPorts());
            assertEquals(List.of(9090), configs.get(1).getPorts());
        }

        @Test
        @DisplayName("Defaults are applied for omitted values")
        void defaultsApplied(@TempDir Path tempDir) throws IOException {
            Path config = tempDir.resolve("server.conf");
            Files.writeString(config, "server {\n  port 8080\n}");
            List<ServerConfig> configs = loader.load(config);
            ServerConfig sc = configs.get(0);
            assertEquals(ConfigDefaults.TIMEOUT_MS, sc.resolveTimeout("/", null));
            assertEquals(ConfigDefaults.CLIENT_BODY_LIMIT, sc.resolveClientBodyLimit("/"));
            assertEquals(ConfigDefaults.DEFAULT_SERVER_ROOT, sc.getDefaultServerRoot());
            assertFalse(sc.resolveDirectoryListing("/"));
            assertEquals(ConfigDefaults.DEFAULT_ALLOWED_METHODS, sc.resolveAllowedMethods("/"));
        }

        @Test
        @DisplayName("Route defaults are applied")
        void routeDefaultsApplied(@TempDir Path tempDir) throws IOException {
            Path config = tempDir.resolve("server.conf");
            Files.writeString(config, """
                    server {
                        port 8080
                        route /api { root www }
                    }
                    """);
            List<ServerConfig> configs = loader.load(config);
            RouteConfig route = configs.get(0).getRoutes().get("/api");
            assertEquals(ConfigDefaults.DEFAULT_FILE, route.getDefaultFile());
            assertNull(route.getAllowedMethods());
            assertNull(route.getRedirect());
            assertNull(route.getCgiExtensions());
            assertNull(route.getTimeoutMs());
            assertNull(route.getDirectoryListing());
            assertNull(route.getClientBodyLimit());
        }
    }

    @Nested
    @DisplayName("Memory leak / resource scenarios")
    class MemoryLeakScenarios {

        @Test
        @DisplayName("Load 1000 different configs without resource leak")
        void loadManyConfigs(@TempDir Path tempDir) throws IOException {
            for (int i = 0; i < 1000; i++) {
                Path config = tempDir.resolve("server_" + i + ".conf");
                String content = "server { port " + (8080 + i) + " }";
                Files.writeString(config, content);

                List<ServerConfig> configs = loader.load(config);
                assertEquals(1, configs.size());
                assertEquals(List.of(8080 + i), configs.get(0).getPorts());
            }
        }

        @Test
        @DisplayName("Load config with 1000 routes")
        void manyRoutes(@TempDir Path tempDir) throws IOException {
            StringBuilder sb = new StringBuilder();
            sb.append("server {\n");
            sb.append("  port 8080\n");
            for (int i = 0; i < 1000; i++) {
                sb.append("  route /r").append(i).append(" { root www").append(i).append(" }\n");
            }
            sb.append("}\n");

            Path config = tempDir.resolve("large.conf");
            Files.writeString(config, sb.toString());

            List<ServerConfig> configs = loader.load(config);
            assertEquals(1, configs.size());
            assertEquals(1000, configs.get(0).getRoutes().size());
        }

        @Test
        @DisplayName("Loader instances are independent — no shared state")
        void loaderInstancesIndependent(@TempDir Path tempDir) throws IOException {
            Path configA = tempDir.resolve("a.conf");
            Files.writeString(configA, "server { port 8080 }");
            Path configB = tempDir.resolve("b.conf");
            Files.writeString(configB, "server { port 9090 }");

            ConfigLoader loaderA = new ConfigLoader();
            ConfigLoader loaderB = new ConfigLoader();

            List<ServerConfig> resultA = loaderA.load(configA);
            List<ServerConfig> resultB = loaderB.load(configB);

            assertEquals(List.of(8080), resultA.get(0).getPorts());
            assertEquals(List.of(9090), resultB.get(0).getPorts());
        }

        @Test
        @DisplayName("Custom parser and validator are used when injected")
        void customParserAndValidator(@TempDir Path tempDir) throws IOException {
            Path config = tempDir.resolve("custom.conf");
            Files.writeString(config, "server {\n  port 8080\n}");

            ConfigParser customParser = new ConfigParser();
            ConfigValidator customValidator = new ConfigValidator();
            ConfigLoader customLoader = new ConfigLoader(customParser, customValidator);

            List<ServerConfig> configs = customLoader.load(config);
            assertEquals(1, configs.size());
            assertEquals(List.of(8080), configs.get(0).getPorts());
        }

        @Test
        @DisplayName("Load with large directive values")
        void largeValues(@TempDir Path tempDir) throws IOException {
            String longPath = "/" + "a".repeat(10000);
            String content = "server {\n"
                    + "  port 8080\n"
                    + "  default_server_root " + longPath + "\n"
                    + "  route /api {\n"
                    + "    root " + longPath + "\n"
                    + "  }\n"
                    + "}\n";
            Path config = tempDir.resolve("large_values.conf");
            Files.writeString(config, content);

            List<ServerConfig> configs = loader.load(config);
            assertEquals(1, configs.size());
            assertEquals(longPath, configs.get(0).getDefaultServerRoot());
            assertEquals(longPath, configs.get(0).getRoutes().get("/api").getRoot());
        }
    }
}
