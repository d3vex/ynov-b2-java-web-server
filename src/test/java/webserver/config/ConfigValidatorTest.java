package webserver.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static webserver.config.ConfigParser.*;

class ConfigValidatorTest {

    private final ConfigValidator validator = new ConfigValidator();

    @Nested
    @DisplayName("Valid configs pass")
    class ValidConfigs {

        @Test
        @DisplayName("Minimal valid server")
        void minimalServer() {
            ParsedServer s = new ParsedServer();
            s.directives.add(new ParsedDirective("port", List.of("8080")));
            List<String> errors = validator.validate(List.of(s));
            assertTrue(errors.isEmpty());
        }

        @Test
        @DisplayName("Full valid config")
        void fullConfig() {
            ParsedServer s = new ParsedServer();
            s.directives.add(new ParsedDirective("host", List.of("0.0.0.0")));
            s.directives.add(new ParsedDirective("port", List.of("8888")));
            s.directives.add(new ParsedDirective("port", List.of("9999")));
            s.directives.add(new ParsedDirective("timeout", List.of("60000")));
            s.directives.add(new ParsedDirective("client_body_limit", List.of("1048576")));
            s.directives.add(new ParsedDirective("directory_listing", List.of("true")));
            s.directives.add(new ParsedDirective("error_page", List.of("404", "/errors/404.html")));
            s.directives.add(new ParsedDirective("allowed_methods", List.of("GET", "POST", "HEAD")));
            s.directives.add(new ParsedDirective("cgi_extensions", List.of(".py", ".sh")));

            ParsedRoute r = new ParsedRoute("/api");
            r.directives.add(new ParsedDirective("root", List.of("www")));
            r.directives.add(new ParsedDirective("default_file", List.of("index.html")));
            r.directives.add(new ParsedDirective("redirect", List.of("/new-location")));
            r.directives.add(new ParsedDirective("timeout", List.of("30000")));
            r.directives.add(new ParsedDirective("client_body_limit", List.of("512")));
            r.directives.add(new ParsedDirective("directory_listing", List.of("false")));
            r.directives.add(new ParsedDirective("error_page", List.of("403", "/errors/403.html")));
            r.directives.add(new ParsedDirective("allowed_methods", List.of("GET")));
            r.directives.add(new ParsedDirective("cgi_extensions", List.of(".cgi")));
            s.routes.add(r);

            List<String> errors = validator.validate(List.of(s));
            assertTrue(errors.isEmpty(), "Expected no errors but got: " + errors);
        }

        @Test
        @DisplayName("Multiple servers all valid")
        void multipleServers() {
            ParsedServer s1 = new ParsedServer();
            s1.directives.add(new ParsedDirective("port", List.of("8080")));
            ParsedServer s2 = new ParsedServer();
            s2.directives.add(new ParsedDirective("port", List.of("9090")));
            List<String> errors = validator.validate(List.of(s1, s2));
            assertTrue(errors.isEmpty());
        }

        @Test
        @DisplayName("Host default is acceptable (0.0.0.0 is a valid host)")
        void defaultHost() {
            ParsedServer s = new ParsedServer();
            s.directives.add(new ParsedDirective("host", List.of("0.0.0.0")));
            s.directives.add(new ParsedDirective("port", List.of("8080")));
            assertTrue(validator.validate(List.of(s)).isEmpty());
        }
    }

    @Nested
    @DisplayName("Server-level validation errors")
    class ServerErrors {

        @Test
        @DisplayName("Empty servers list fails")
        void emptyServers() {
            List<String> errors = validator.validate(List.of());
            assertFalse(errors.isEmpty());
            assertTrue(errors.get(0).toLowerCase().contains("no server"));
        }

        @Test
        @DisplayName("Missing port fails")
        void missingPort() {
            ParsedServer s = new ParsedServer();
            s.directives.add(new ParsedDirective("host", List.of("localhost")));
            List<String> errors = validator.validate(List.of(s));
            assertFalse(errors.isEmpty());
            assertTrue(errors.stream().anyMatch(e -> e.contains("port") && e.contains("required")));
        }

        @Test
        @DisplayName("Empty host fails")
        void emptyHost() {
            ParsedServer s = new ParsedServer();
            s.directives.add(new ParsedDirective("host", List.of("")));
            s.directives.add(new ParsedDirective("port", List.of("8080")));
            List<String> errors = validator.validate(List.of(s));
            assertFalse(errors.isEmpty());
            assertTrue(errors.stream().anyMatch(e -> e.contains("host")));
        }

        @Test
        @DisplayName("Invalid port fails")
        void invalidPort() {
            ParsedServer s = new ParsedServer();
            s.directives.add(new ParsedDirective("port", List.of("not-a-number")));
            List<String> errors = validator.validate(List.of(s));
            assertFalse(errors.isEmpty());
            assertTrue(errors.stream().anyMatch(e -> e.contains("port")));
        }

        @Test
        @DisplayName("Invalid timeout fails")
        void invalidTimeout() {
            ParsedServer s = new ParsedServer();
            s.directives.add(new ParsedDirective("port", List.of("8080")));
            s.directives.add(new ParsedDirective("timeout", List.of("not-a-number")));
            List<String> errors = validator.validate(List.of(s));
            assertFalse(errors.isEmpty());
            assertTrue(errors.stream().anyMatch(e -> e.contains("timeout")));
        }

        @Test
        @DisplayName("Missing timeout value reported")
        void missingTimeoutValue() {
            ParsedServer s = new ParsedServer();
            s.directives.add(new ParsedDirective("port", List.of("8080")));
            s.directives.add(new ParsedDirective("timeout", List.of()));
            List<String> errors = validator.validate(List.of(s));
            assertFalse(errors.isEmpty());
            assertTrue(errors.stream().anyMatch(e -> e.contains("timeout") && e.contains("missing")));
        }

        @Test
        @DisplayName("Invalid client_body_limit fails")
        void invalidClientBodyLimit() {
            ParsedServer s = new ParsedServer();
            s.directives.add(new ParsedDirective("port", List.of("8080")));
            s.directives.add(new ParsedDirective("client_body_limit", List.of("big")));
            List<String> errors = validator.validate(List.of(s));
            assertFalse(errors.isEmpty());
            assertTrue(errors.stream().anyMatch(e -> e.contains("client_body_limit")));
        }

        @Test
        @DisplayName("Invalid directory_listing fails")
        void invalidDirectoryListing() {
            ParsedServer s = new ParsedServer();
            s.directives.add(new ParsedDirective("port", List.of("8080")));
            s.directives.add(new ParsedDirective("directory_listing", List.of("maybe")));
            List<String> errors = validator.validate(List.of(s));
            assertFalse(errors.isEmpty());
            assertTrue(errors.stream().anyMatch(e -> e.contains("directory_listing")));
        }

        @Test
        @DisplayName("Missing directory_listing value reported")
        void missingDirectoryListingValue() {
            ParsedServer s = new ParsedServer();
            s.directives.add(new ParsedDirective("port", List.of("8080")));
            s.directives.add(new ParsedDirective("directory_listing", List.of()));
            List<String> errors = validator.validate(List.of(s));
            assertFalse(errors.isEmpty());
            assertTrue(errors.stream().anyMatch(e -> e.contains("directory_listing") && e.contains("missing")));
        }

        @Test
        @DisplayName("error_page without path fails")
        void errorPageWithoutPath() {
            ParsedServer s = new ParsedServer();
            s.directives.add(new ParsedDirective("port", List.of("8080")));
            s.directives.add(new ParsedDirective("error_page", List.of("404")));
            List<String> errors = validator.validate(List.of(s));
            assertFalse(errors.isEmpty());
            assertTrue(errors.stream().anyMatch(e -> e.contains("error_page")));
        }

        @Test
        @DisplayName("error_page invalid status code fails")
        void errorPageInvalidCode() {
            ParsedServer s = new ParsedServer();
            s.directives.add(new ParsedDirective("port", List.of("8080")));
            s.directives.add(new ParsedDirective("error_page", List.of("ABC", "/path")));
            List<String> errors = validator.validate(List.of(s));
            assertFalse(errors.isEmpty());
        }

        @Test
        @DisplayName("Invalid HTTP method fails")
        void invalidHttpMethod() {
            ParsedServer s = new ParsedServer();
            s.directives.add(new ParsedDirective("port", List.of("8080")));
            s.directives.add(new ParsedDirective("allowed_methods", List.of("GET", "DELETE", "SLEEP")));
            List<String> errors = validator.validate(List.of(s));
            assertFalse(errors.isEmpty());
            assertTrue(errors.stream().anyMatch(e -> e.contains("SLEEP")));
        }
    }

    @Nested
    @DisplayName("Route-level validation errors")
    class RouteErrors {

        @Test
        @DisplayName("Route path without leading slash fails")
        void routePathNoSlash() {
            ParsedServer s = new ParsedServer();
            s.directives.add(new ParsedDirective("port", List.of("8080")));
            ParsedRoute r = new ParsedRoute("api");
            r.directives.add(new ParsedDirective("root", List.of("www")));
            s.routes.add(r);
            List<String> errors = validator.validate(List.of(s));
            assertFalse(errors.isEmpty());
            assertTrue(errors.stream().anyMatch(e -> e.contains("path") && e.contains("/")));
        }

        @Test
        @DisplayName("Missing root value reported")
        void missingRootValue() {
            ParsedServer s = new ParsedServer();
            s.directives.add(new ParsedDirective("port", List.of("8080")));
            ParsedRoute r = new ParsedRoute("/api");
            r.directives.add(new ParsedDirective("root", List.of()));
            s.routes.add(r);
            List<String> errors = validator.validate(List.of(s));
            assertFalse(errors.isEmpty());
        }

        @Test
        @DisplayName("Missing redirect value reported")
        void missingRedirectValue() {
            ParsedServer s = new ParsedServer();
            s.directives.add(new ParsedDirective("port", List.of("8080")));
            ParsedRoute r = new ParsedRoute("/docs");
            r.directives.add(new ParsedDirective("redirect", List.of()));
            s.routes.add(r);
            List<String> errors = validator.validate(List.of(s));
            assertFalse(errors.isEmpty());
        }

        @Test
        @DisplayName("Invalid route timeout fails")
        void invalidRouteTimeout() {
            ParsedServer s = new ParsedServer();
            s.directives.add(new ParsedDirective("port", List.of("8080")));
            ParsedRoute r = new ParsedRoute("/api");
            r.directives.add(new ParsedDirective("root", List.of("www")));
            r.directives.add(new ParsedDirective("timeout", List.of("slow")));
            s.routes.add(r);
            List<String> errors = validator.validate(List.of(s));
            assertFalse(errors.isEmpty());
        }

        @Test
        @DisplayName("Invalid route client_body_limit fails")
        void invalidRouteBodyLimit() {
            ParsedServer s = new ParsedServer();
            s.directives.add(new ParsedDirective("port", List.of("8080")));
            ParsedRoute r = new ParsedRoute("/api");
            r.directives.add(new ParsedDirective("root", List.of("www")));
            r.directives.add(new ParsedDirective("client_body_limit", List.of("lots")));
            s.routes.add(r);
            List<String> errors = validator.validate(List.of(s));
            assertFalse(errors.isEmpty());
        }

        @Test
        @DisplayName("Invalid route directory_listing fails")
        void invalidRouteDirectoryListing() {
            ParsedServer s = new ParsedServer();
            s.directives.add(new ParsedDirective("port", List.of("8080")));
            ParsedRoute r = new ParsedRoute("/api");
            r.directives.add(new ParsedDirective("root", List.of("www")));
            r.directives.add(new ParsedDirective("directory_listing", List.of("on")));
            s.routes.add(r);
            List<String> errors = validator.validate(List.of(s));
            assertFalse(errors.isEmpty());
        }

        @Test
        @DisplayName("Route error_page without path fails")
        void routeErrorPageWithoutPath() {
            ParsedServer s = new ParsedServer();
            s.directives.add(new ParsedDirective("port", List.of("8080")));
            ParsedRoute r = new ParsedRoute("/api");
            r.directives.add(new ParsedDirective("root", List.of("www")));
            r.directives.add(new ParsedDirective("error_page", List.of("404")));
            s.routes.add(r);
            List<String> errors = validator.validate(List.of(s));
            assertFalse(errors.isEmpty());
        }

        @Test
        @DisplayName("Multiple errors reported together")
        void multipleErrors() {
            ParsedServer s = new ParsedServer();
            s.directives.add(new ParsedDirective("host", List.of("")));
            s.directives.add(new ParsedDirective("port", List.of("abc")));
            s.directives.add(new ParsedDirective("timeout", List.of("xyz")));

            ParsedRoute r = new ParsedRoute("no-slash");
            r.directives.add(new ParsedDirective("root", List.of("")));
            r.directives.add(new ParsedDirective("error_page", List.of("ABC", "/path")));
            s.routes.add(r);

            List<String> errors = validator.validate(List.of(s));
            assertTrue(errors.size() >= 5);
        }
    }

    @Nested
    @DisplayName("Memory leak / state leak scenarios")
    class MemoryLeakScenarios {

        @Test
        @DisplayName("Validator has no static state across 1000 calls")
        void noStaticState() {
            ParsedServer valid = new ParsedServer();
            valid.directives.add(new ParsedDirective("port", List.of("8080")));

            for (int i = 0; i < 1000; i++) {
                List<String> errors = validator.validate(List.of(valid));
                assertTrue(errors.isEmpty());
            }
        }

        @Test
        @DisplayName("Validator is idempotent — same input always produces same output")
        void idempotent() {
            ParsedServer s = new ParsedServer();
            s.directives.add(new ParsedDirective("port", List.of("8080")));
            s.directives.add(new ParsedDirective("timeout", List.of("abc")));

            List<String> first = validator.validate(List.of(s));
            List<String> second = validator.validate(List.of(s));
            assertEquals(first, second);
        }

        @Test
        @DisplayName("Validate large server with 1000 routes")
        void largeServer() {
            ParsedServer s = new ParsedServer();
            s.directives.add(new ParsedDirective("port", List.of("8080")));
            for (int i = 0; i < 1000; i++) {
                ParsedRoute r = new ParsedRoute("/route" + i);
                r.directives.add(new ParsedDirective("root", List.of("www")));
                s.routes.add(r);
            }
            List<String> errors = validator.validate(List.of(s));
            assertTrue(errors.isEmpty());
        }
    }
}
