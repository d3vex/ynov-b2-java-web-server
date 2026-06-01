package webserver.http;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class HttpResponse {

    private final String httpVersion;
    private final HttpStatus statusCode;
    private final HttpHeaders headers;
    private final byte[] body;

    public HttpResponse(String httpVersion, HttpStatus statusCode,
                        HttpHeaders headers, byte[] body) {
        this.httpVersion = httpVersion;
        this.statusCode = statusCode;
        this.headers = headers;
        this.body = body;
    }

    public String getHttpVersion() {
        return httpVersion;
    }

    public HttpStatus getStatusCode() {
        return statusCode;
    }

    public String getReasonPhrase() {
        return statusCode.getReasonPhrase();
    }

    public HttpHeaders getHeaders() {
        return headers;
    }

    public byte[] getBody() {
        return body;
    }

    public String getBodyAsString() {
        if (body == null) return "";
        return new String(body, StandardCharsets.UTF_8);
    }

    public List<Cookie> getSetCookies() {
        return headers.getSetCookies();
    }

    public byte[] toBytes() {
        StringBuilder sb = new StringBuilder();
        sb.append(httpVersion).append(' ').append(statusCode.getCode()).append(' ')
                .append(statusCode.getReasonPhrase()).append("\r\n");

        for (String line : headers.toHeaderLines()) {
            sb.append(line).append("\r\n");
        }
        sb.append("\r\n");

        byte[] headBytes = sb.toString().getBytes(StandardCharsets.US_ASCII);
        if (body == null || body.length == 0) {
            return headBytes;
        }
        byte[] result = new byte[headBytes.length + body.length];
        System.arraycopy(headBytes, 0, result, 0, headBytes.length);
        System.arraycopy(body, 0, result, headBytes.length, body.length);
        return result;
    }

    public static HttpResponseBuilder builder() {
        return new HttpResponseBuilder();
    }
}
