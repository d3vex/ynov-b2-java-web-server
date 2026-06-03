package webserver.errors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import webserver.config.RouteConfig;
import webserver.config.ServerConfig;
import webserver.handlers.ErrorHandler;
import webserver.http.HttpMethod;
import webserver.http.HttpResponse;
import webserver.http.HttpStatus;

import java.io.FileNotFoundException;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class ErrorHandlingTest {

    @Nested
    @DisplayName("DefaultErrorPages")
    class DefaultErrorPagesTests {

        private final DefaultErrorPages pages = new DefaultErrorPages();

        @Test
        @DisplayName("Render 404 page")
        void render404() {
            String html = pages.render(HttpStatus.NOT_FOUND);
            assertTrue(html.contains("404"));
            assertTrue(html.contains("Not Found"));
            assertTrue(html.contains("<html>"));
        }

        @Test
        @DisplayName("Render 500 page")
        void render500() {
            String html = pages.render(HttpStatus.INTERNAL_SERVER_ERROR);
            assertTrue(html.contains("500"));
            assertTrue(html.contains("Internal Server Error"));
        }

        @Test
        @DisplayName("Render with custom message")
        void renderCustomMessage() {
            String html = pages.render(HttpStatus.NOT_FOUND, "Custom error message");
            assertTrue(html.contains("404"));
            assertTrue(html.contains("Custom error message"));
        }

        @Test
        @DisplayName("Render without message uses reason phrase")
        void renderWithoutMessage() {
            String html = pages.render(HttpStatus.FORBIDDEN);
            assertTrue(html.contains("403"));
            assertTrue(html.contains("Forbidden"));
        }
    }

    @Nested
    @DisplayName("ServerExceptionMapper")
    class ServerExceptionMapperTests {

        private final ServerExceptionMapper mapper = new ServerExceptionMapper();

        @Test
        @DisplayName("FileNotFoundException maps to 404")
        void fileNotFound() {
            assertEquals(HttpStatus.NOT_FOUND, mapper.toStatus(new FileNotFoundException()));
        }

        @Test
        @DisplayName("SecurityException maps to 403")
        void securityException() {
            assertEquals(HttpStatus.FORBIDDEN, mapper.toStatus(new SecurityException()));
        }

        @Test
        @DisplayName("IllegalArgumentException maps to 400")
        void illegalArgument() {
            assertEquals(HttpStatus.BAD_REQUEST, mapper.toStatus(new IllegalArgumentException()));
        }

        @Test
        @DisplayName("ClosedChannelException maps to 503")
        void closedChannel() {
            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, mapper.toStatus(new ClosedChannelException()));
        }

        @Test
        @DisplayName("Unknown exception maps to 500")
        void unknownException() {
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, mapper.toStatus(new RuntimeException()));
        }

        @Test
        @DisplayName("toStatus with HttpStatus passes through")
        void statusPassthrough() {
            assertEquals(HttpStatus.NOT_FOUND, mapper.toStatus(HttpStatus.NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("ErrorPageResolver")
    class ErrorPageResolverTests {

        private final ErrorPageResolver resolver = new ErrorPageResolver();

        @Test
        @DisplayName("Resolve error page path from config")
        void resolveFromConfig() {
            ServerConfig config = new ServerConfig.Builder().port(8080)
                    .errorPage(404, "/errors/404.html")
                    .build();
            String path = resolver.resolve(config, "/", HttpMethod.GET, 404);
            assertEquals("/errors/404.html", path);
        }

        @Test
        @DisplayName("Resolve error page path from route")
        void resolveFromRoute() {
            RouteConfig route = new RouteConfig.Builder().path("/api")
                    .errorPage(403, "/api/errors/403.html")
                    .build();
            ServerConfig config = new ServerConfig.Builder().port(8080)
                    .route("/api", route)
                    .build();
            String path = resolver.resolve(config, "/api/endpoint", HttpMethod.GET, 403);
            assertEquals("/api/errors/403.html", path);
        }

        @Test
        @DisplayName("Returns null when config is null")
        void nullConfig() {
            assertNull(resolver.resolve(null, "/", HttpMethod.GET, 404));
        }

        @Test
        @DisplayName("Returns null when no error page configured")
        void noErrorPage() {
            ServerConfig config = new ServerConfig.Builder().port(8080).build();
            assertNull(resolver.resolve(config, "/", HttpMethod.GET, 404));
        }
    }

    @Nested
    @DisplayName("ErrorHandler (integration)")
    class ErrorHandlerTests {

        private final ErrorHandler handler = new ErrorHandler();

        @Test
        @DisplayName("Handle error without config returns inline HTML")
        void handleErrorWithoutConfig() {
            HttpResponse res = handler.handleError(HttpStatus.NOT_FOUND);
            assertEquals(404, res.getStatusCode().getCode());
            assertTrue(res.getBodyAsString().contains("Not Found"));
            assertEquals("text/html; charset=utf-8", res.getHeaders().get("Content-Type"));
        }

        @Test
        @DisplayName("Handle error with config and custom page")
        void handleErrorWithCustomPage() throws Exception {
            java.nio.file.Path tempDir = java.nio.file.Files.createTempDirectory("errors");
            java.nio.file.Path errorPage = tempDir.resolve("custom_404.html");
            java.nio.file.Files.writeString(errorPage, "<html>Custom 404</html>");

            ServerConfig config = new ServerConfig.Builder().port(8080)
                    .errorPage(404, errorPage.toAbsolutePath().toString())
                    .build();
            HttpResponse res = handler.handleError(HttpStatus.NOT_FOUND, config, "/", HttpMethod.GET);
            assertEquals(404, res.getStatusCode().getCode());
            assertTrue(res.getBodyAsString().contains("Custom 404"));

            java.nio.file.Files.deleteIfExists(errorPage);
            java.nio.file.Files.deleteIfExists(tempDir);
        }

        @Test
        @DisplayName("Handle error via exception")
        void handleException() {
            HttpResponse res = handler.handleError(
                    new FileNotFoundException("test"),
                    null, "/", HttpMethod.GET);
            assertEquals(404, res.getStatusCode().getCode());
        }

        @Test
        @DisplayName("Handle 403 via SecurityException mapping")
        void handleSecurityException() {
            HttpResponse res = handler.handleError(
                    new SecurityException("forbidden"),
                    null, "/admin", HttpMethod.GET);
            assertEquals(403, res.getStatusCode().getCode());
        }

        @Test
        @DisplayName("Connection header is set to close")
        void connectionClose() {
            HttpResponse res = handler.handleError(HttpStatus.INTERNAL_SERVER_ERROR);
            assertEquals("close", res.getHeaders().get("Connection"));
        }

        @Test
        @DisplayName("Handle 1000 errors without issues")
        void manyErrors() {
            for (int i = 0; i < 1000; i++) {
                HttpResponse res = handler.handleError(HttpStatus.NOT_FOUND);
                assertEquals(404, res.getStatusCode().getCode());
            }
        }
    }
}
