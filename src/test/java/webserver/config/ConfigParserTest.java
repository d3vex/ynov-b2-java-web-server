package webserver.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static webserver.config.ConfigParser.*;

class ConfigParserTest {

    private final ConfigParser parser = new ConfigParser();

    @Nested
    @DisplayName("Happy path: basic structures")
    class BasicStructures {

        @Test
        @DisplayName("Single server with one route")
        void singleServerOneRoute() {
            String config = """
                    server {
                        host 0.0.0.0
                        port 8080
                        route /api {
                            root www
                        }
                    }
                    """;
            List<ParsedServer> servers = parser.parse(config);
            assertEquals(1, servers.size());
            ParsedServer s = servers.get(0);
            assertEquals(1, s.routes.size());
            assertEquals("/api", s.routes.get(0).path);
        }

        @Test
        @DisplayName("Multiple servers")
        void multipleServers() {
            String config = """
                    server {
                        host 0.0.0.0
                        port 8080
                    }
                    server {
                        host 127.0.0.1
                        port 9090
                    }
                    """;
            List<ParsedServer> servers = parser.parse(config);
            assertEquals(2, servers.size());
            assertEquals("0.0.0.0", servers.get(0).directives.get(0).values.get(0));
            assertEquals("127.0.0.1", servers.get(1).directives.get(0).values.get(0));
        }

        @Test
        @DisplayName("Multiple routes per server")
        void multipleRoutes() {
            String config = """
                    server {
                        port 8080
                        route /a { root r1 }
                        route /b { root r2 }
                        route /c { root r3 }
                    }
                    """;
            List<ParsedServer> servers = parser.parse(config);
            assertEquals(1, servers.size());
            assertEquals(3, servers.get(0).routes.size());
            assertEquals("/a", servers.get(0).routes.get(0).path);
            assertEquals("/b", servers.get(0).routes.get(1).path);
            assertEquals("/c", servers.get(0).routes.get(2).path);
        }
    }

    @Nested
    @DisplayName("Happy path: all directive types")
    class AllDirectives {

        @Test
        @DisplayName("All server-level directives")
        void allServerDirectives() {
            String config = """
                    server {
                        host 127.0.0.1
                        port 8888
                        port 9999
                        default_server_root /var/www
                        timeout 60000
                        client_body_limit 2097152
                        directory_listing true
                        error_page 404 /errors/404.html
                        error_page 500 /errors/500.html
                        allowed_methods GET POST
                        cgi_extensions .py .sh
                    }
                    """;
            List<ParsedServer> servers = parser.parse(config);
            assertEquals(1, servers.size());
            ParsedServer s = servers.get(0);
            assertEquals(11, s.directives.size());

            assertDirective(s.directives, "host", "127.0.0.1");
            assertDirective(s.directives, "port", "8888");
            assertDirective(s.directives, "port", "9999");
            assertDirective(s.directives, "default_server_root", "/var/www");
            assertDirective(s.directives, "timeout", "60000");
            assertDirective(s.directives, "client_body_limit", "2097152");
            assertDirective(s.directives, "directory_listing", "true");
            assertDirective(s.directives, "error_page", "404", "/errors/404.html");
            assertDirective(s.directives, "error_page", "500", "/errors/500.html");
            assertDirective(s.directives, "allowed_methods", "GET", "POST");
            assertDirective(s.directives, "cgi_extensions", ".py", ".sh");
        }

        @Test
        @DisplayName("All route-level directives")
        void allRouteDirectives() {
            String config = """
                    server {
                        port 8080
                        route /app {
                            root /app/www
                            default_file app.html
                            redirect /new-location
                            timeout 30000
                            client_body_limit 1048576
                            directory_listing false
                            error_page 403 /errors/403.html
                            allowed_methods GET HEAD
                            cgi_extensions .pl
                        }
                    }
                    """;
            List<ParsedServer> servers = parser.parse(config);
            assertEquals(1, servers.size());
            assertEquals(1, servers.get(0).routes.size());
            ParsedRoute r = servers.get(0).routes.get(0);
            assertEquals(9, r.directives.size());

            assertDirective(r.directives, "root", "/app/www");
            assertDirective(r.directives, "default_file", "app.html");
            assertDirective(r.directives, "redirect", "/new-location");
            assertDirective(r.directives, "timeout", "30000");
            assertDirective(r.directives, "client_body_limit", "1048576");
            assertDirective(r.directives, "directory_listing", "false");
            assertDirective(r.directives, "error_page", "403", "/errors/403.html");
            assertDirective(r.directives, "allowed_methods", "GET", "HEAD");
            assertDirective(r.directives, "cgi_extensions", ".pl");
        }
    }

    @Nested
    @DisplayName("Brace placement")
    class BracePlacement {

        @Test
        @DisplayName("Braces on same line as server/route")
        void inlineBraces() {
            String config = """
                    server {
                        port 8080
                        route /x { root r }
                    }
                    """;
            List<ParsedServer> servers = parser.parse(config);
            assertEquals(1, servers.size());
            assertEquals(1, servers.get(0).routes.size());
            assertEquals("/x", servers.get(0).routes.get(0).path);
        }

        @Test
        @DisplayName("Braces on separate lines")
        void separateLineBraces() {
            String config = """
                    server
                    {
                        port 8080
                        route /x
                        {
                            root r
                        }
                    }
                    """;
            List<ParsedServer> servers = parser.parse(config);
            assertEquals(1, servers.size());
            assertEquals(1, servers.get(0).routes.size());
            assertEquals("/x", servers.get(0).routes.get(0).path);
        }
    }

    @Nested
    @DisplayName("Comments and whitespace")
    class CommentsAndWhitespace {

        @Test
        @DisplayName("Full-line comments")
        void fullLineComments() {
            String config = """
                    # This is a comment
                    server {
                        # Server-level comment
                        port 8080
                        # Route comment
                        route /x { root r }
                    }
                    # End comment
                    """;
            List<ParsedServer> servers = parser.parse(config);
            assertEquals(1, servers.size());
            assertEquals(1, servers.get(0).routes.size());
        }

        @Test
        @DisplayName("Blank lines are ignored")
        void blankLines() {
            String config = """

                    server {

                        port 8080

                        route /x { root r }

                    }

                    """;
            List<ParsedServer> servers = parser.parse(config);
            assertEquals(1, servers.size());
            assertEquals(1, servers.get(0).routes.size());
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("Empty content returns empty list")
        void emptyContent() {
            assertTrue(parser.parse("").isEmpty());
        }

        @Test
        @DisplayName("Only whitespace returns empty list")
        void onlyWhitespace() {
            assertTrue(parser.parse("   \n  \n   ").isEmpty());
        }

        @Test
        @DisplayName("Only comments returns empty list")
        void onlyComments() {
            assertTrue(parser.parse("# comment 1\n# comment 2").isEmpty());
        }

        @Test
        @DisplayName("No server blocks returns empty list")
        void noServerBlocks() {
            assertTrue(parser.parse("some random text").isEmpty());
        }

        @Test
        @DisplayName("Unknown directives are captured")
        void unknownDirectives() {
            String config = """
                    server {
                        port 8080
                        custom_setting some_value
                        another_option 1 2 3
                    }
                    """;
            List<ParsedServer> servers = parser.parse(config);
            assertEquals(1, servers.size());
            List<ParsedDirective> dirs = servers.get(0).directives;
            assertTrue(dirs.stream().anyMatch(d -> d.key.equals("custom_setting")));
            assertTrue(dirs.stream().anyMatch(d -> d.key.equals("another_option")));
        }

        @Test
        @DisplayName("Directive with no values has empty list")
        void directiveWithoutValue() {
            String config = """
                    server {
                        port 8080
                        bare_key
                    }
                    """;
            List<ParsedServer> servers = parser.parse(config);
            assertEquals(1, servers.size());
            ParsedServer s = servers.get(0);
            ParsedDirective bare = s.directives.stream()
                    .filter(d -> d.key.equals("bare_key"))
                    .findFirst().orElseThrow();
            assertTrue(bare.values.isEmpty());
        }

        @Test
        @DisplayName("route /path { on same line is parsed correctly")
        void routeInline() {
            String config = """
                    server {
                        port 8080
                        route /test { root /t }
                    }
                    """;
            List<ParsedServer> servers = parser.parse(config);
            assertEquals(1, servers.get(0).routes.size());
            assertEquals("/test", servers.get(0).routes.get(0).path);
            assertEquals("root", servers.get(0).routes.get(0).directives.get(0).key);
            assertEquals("/t", servers.get(0).routes.get(0).directives.get(0).values.get(0));
        }
    }

    @Nested
    @DisplayName("Stateful behavior / memory leak scenarios")
    class MemoryLeakScenarios {

        @Test
        @DisplayName("Parser produces no cumulative state across 1000 calls")
        void noCumulativeState() {
            String configA = "server {\n  port 8080\n  route /a { root r }\n}";
            String configB = "server {\n  port 9090\n}";

            for (int i = 0; i < 1000; i++) {
                List<ParsedServer> resultA = parser.parse(configA);
                assertEquals(1, resultA.size());
                assertEquals(1, resultA.get(0).routes.size());

                List<ParsedServer> resultB = parser.parse(configB);
                assertEquals(1, resultB.size());
                assertTrue(resultB.get(0).routes.isEmpty());
            }
        }

        @Test
        @DisplayName("Parse large config with 1000 routes without OOM")
        void largeConfig() {
            StringBuilder sb = new StringBuilder();
            sb.append("server {\n");
            sb.append("  port 8080\n");
            for (int i = 0; i < 1000; i++) {
                sb.append("  route /route").append(i).append(" { root r").append(i).append(" }\n");
            }
            sb.append("}\n");

            List<ParsedServer> servers = parser.parse(sb.toString());
            assertEquals(1, servers.size());
            assertEquals(1000, servers.get(0).routes.size());
            assertEquals("/route999", servers.get(0).routes.get(999).path);
        }

        @Test
        @DisplayName("Parse large config with 10000 directives")
        void manyDirectives() {
            StringBuilder sb = new StringBuilder();
            sb.append("server {\n");
            sb.append("  port 8080\n");
            for (int i = 0; i < 10000; i++) {
                sb.append("  key_").append(i).append(" value_").append(i).append("\n");
            }
            sb.append("}\n");

            List<ParsedServer> servers = parser.parse(sb.toString());
            assertEquals(1, servers.size());
            assertEquals(10001, servers.get(0).directives.size());
        }
    }

    private static void assertDirective(List<ParsedDirective> directives, String key, String... expectedValues) {
        ParsedDirective found = directives.stream()
                .filter(d -> d.key.equals(key))
                .filter(d -> d.values.equals(List.of(expectedValues)))
                .findFirst().orElse(null);
        assertNotNull(found, "Directive '" + key + "' with values " + List.of(expectedValues) + " not found");
    }
}
