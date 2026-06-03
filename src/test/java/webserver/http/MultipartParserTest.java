package webserver.http;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MultipartParserTest {

    private final MultipartParser parser = new MultipartParser();

    private String boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW";

    private String multipartBody(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            sb.append("--").append(boundary).append("\r\n");
            sb.append(part);
        }
        sb.append("--").append(boundary).append("--\r\n");
        return sb.toString();
    }

    private String textPart(String name, String value) {
        return "Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n" + value + "\r\n";
    }

    private String filePart(String name, String filename, String contentType, String data) {
        return "Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + filename + "\"\r\n"
                + "Content-Type: " + contentType + "\r\n\r\n"
                + data + "\r\n";
    }

    @Nested
    @DisplayName("Basic multipart parsing")
    class BasicParsing {

        @Test
        @DisplayName("Parse text fields")
        void textFields() {
            String body = multipartBody(
                    textPart("username", "john"),
                    textPart("email", "john@example.com")
            );
            String contentType = "multipart/form-data; boundary=" + boundary;
            List<MultipartParser.Part> parts = parser.parse(body.getBytes(StandardCharsets.UTF_8), contentType);
            assertEquals(2, parts.size());
            assertEquals("username", parts.get(0).name());
            assertEquals("john", new String(parts.get(0).data(), StandardCharsets.UTF_8));
            assertEquals("email", parts.get(1).name());
            assertEquals("john@example.com", new String(parts.get(1).data(), StandardCharsets.UTF_8));
        }

        @Test
        @DisplayName("Parse file upload")
        void fileUpload() {
            String body = multipartBody(
                    filePart("file", "test.txt", "text/plain", "Hello, World!")
            );
            String contentType = "multipart/form-data; boundary=" + boundary;
            List<MultipartParser.Part> parts = parser.parse(body.getBytes(StandardCharsets.UTF_8), contentType);
            assertEquals(1, parts.size());
            MultipartParser.Part part = parts.get(0);
            assertEquals("file", part.name());
            assertEquals("test.txt", part.filename());
            assertEquals("text/plain", part.contentType());
            assertEquals("Hello, World!", new String(part.data(), StandardCharsets.UTF_8));
        }

        @Test
        @DisplayName("Parse mixed fields and files")
        void mixed() {
            String body = multipartBody(
                    textPart("username", "john"),
                    filePart("avatar", "pic.png", "image/png", "binary-data")
            );
            String contentType = "multipart/form-data; boundary=" + boundary;
            List<MultipartParser.Part> parts = parser.parse(body.getBytes(StandardCharsets.UTF_8), contentType);
            assertEquals(2, parts.size());
        }

        @Test
        @DisplayName("Empty multipart body")
        void empty() {
            String body = "--" + boundary + "--\r\n";
            String contentType = "multipart/form-data; boundary=" + boundary;
            List<MultipartParser.Part> parts = parser.parse(body.getBytes(StandardCharsets.UTF_8), contentType);
            assertTrue(parts.isEmpty());
        }
    }

    @Nested
    @DisplayName("Content-Type header parsing")
    class ContentTypeParsing {

        @Test
        @DisplayName("Parse boundary from Content-Type")
        void boundaryParsing() {
            String body = multipartBody(textPart("f", "v"));
            String contentType = "multipart/form-data; boundary=" + boundary;
            List<MultipartParser.Part> parts = parser.parse(body.getBytes(StandardCharsets.UTF_8), contentType);
            assertEquals(1, parts.size());
        }

        @Test
        @DisplayName("Content-Type with extra parameters")
        void contentTypeExtra() {
            String body = multipartBody(textPart("f", "v"));
            String contentType = "multipart/form-data; boundary=" + boundary + "; charset=UTF-8";
            List<MultipartParser.Part> parts = parser.parse(body.getBytes(StandardCharsets.UTF_8), contentType);
            assertEquals(1, parts.size());
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("Binary data in file part")
        void binaryData() throws Exception {
            byte[] binaryData = new byte[]{0, 1, 2, (byte) 0xFF, (byte) 0xFE};
            String partHeader = "Content-Disposition: form-data; name=\"bin\"; filename=\"data.bin\"\r\n"
                    + "Content-Type: application/octet-stream\r\n\r\n";
            java.io.ByteArrayOutputStream body = new java.io.ByteArrayOutputStream();
            body.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
            body.write(partHeader.getBytes(StandardCharsets.UTF_8));
            body.write(binaryData);
            body.write("\r\n".getBytes(StandardCharsets.UTF_8));
            body.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            String contentType = "multipart/form-data; boundary=" + boundary;
            List<MultipartParser.Part> parts = parser.parse(body.toByteArray(), contentType);
            assertEquals(1, parts.size());
            assertArrayEquals(binaryData, parts.get(0).data());
        }

        @Test
        @DisplayName("Part without filename")
        void partWithoutFilename() {
            String body = multipartBody(
                    "Content-Disposition: form-data; name=\"field\"\r\n\r\nvalue\r\n"
            );
            String contentType = "multipart/form-data; boundary=" + boundary;
            List<MultipartParser.Part> parts = parser.parse(body.getBytes(StandardCharsets.UTF_8), contentType);
            assertEquals(1, parts.size());
            assertNull(parts.get(0).filename());
        }
    }

    @Nested
    @DisplayName("Memory leak scenarios")
    class MemoryLeakScenarios {

        @Test
        @DisplayName("Parse 1000 multipart bodies without issues")
        void manyParses() {
            for (int i = 0; i < 1000; i++) {
                String body = multipartBody(
                        textPart("field", "value" + i),
                        filePart("file", "f" + i + ".txt", "text/plain", "data" + i)
                );
                String contentType = "multipart/form-data; boundary=" + boundary;
                List<MultipartParser.Part> parts = parser.parse(body.getBytes(StandardCharsets.UTF_8), contentType);
                assertEquals(2, parts.size());
                assertEquals("value" + i, new String(parts.get(0).data(), StandardCharsets.UTF_8));
            }
        }

        @Test
        @DisplayName("Parse large multipart body")
        void largeBody() {
            StringBuilder largeData = new StringBuilder();
            for (int i = 0; i < 50_000; i++) largeData.append('x');
            String body = multipartBody(filePart("big", "big.txt", "text/plain", largeData.toString()));
            String contentType = "multipart/form-data; boundary=" + boundary;
            List<MultipartParser.Part> parts = parser.parse(body.getBytes(StandardCharsets.UTF_8), contentType);
            assertEquals(1, parts.size());
            assertEquals(50_000, parts.get(0).data().length);
        }

        @Test
        @DisplayName("Parse 100-part multipart body")
        void manyParts() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 100; i++) {
                sb.append("--").append(boundary).append("\r\n");
                sb.append(textPart("field" + i, "value" + i));
            }
            sb.append("--").append(boundary).append("--\r\n");
            String contentType = "multipart/form-data; boundary=" + boundary;
            List<MultipartParser.Part> parts = parser.parse(sb.toString().getBytes(StandardCharsets.UTF_8), contentType);
            assertEquals(100, parts.size());
        }
    }
}
