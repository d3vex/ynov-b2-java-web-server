package webserver.http;

import java.nio.charset.StandardCharsets;

public class HttpResponseBuilder {

    private String httpVersion = "HTTP/1.1";
    private HttpStatus statusCode = HttpStatus.OK;
    private HttpHeaders headers = new HttpHeaders();
    private byte[] body;

    public HttpResponseBuilder httpVersion(String httpVersion) {
        this.httpVersion = httpVersion;
        return this;
    }

    public HttpResponseBuilder status(HttpStatus status) {
        this.statusCode = status;
        return this;
    }

    public HttpResponseBuilder status(int statusCode, String reasonPhrase) {
        this.statusCode = new HttpStatus(statusCode, reasonPhrase);
        return this;
    }

    public HttpResponseBuilder header(String name, String value) {
        this.headers.set(name, value);
        return this;
    }

    public HttpResponseBuilder addHeader(String name, String value) {
        this.headers.add(name, value);
        return this;
    }

    public HttpResponseBuilder headers(HttpHeaders headers) {
        this.headers = headers;
        return this;
    }

    public HttpResponseBuilder body(byte[] body) {
        this.body = body;
        if (body != null) {
            this.headers.set(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));
        }
        return this;
    }

    public HttpResponseBuilder body(String body) {
        return body(body != null ? body.getBytes(StandardCharsets.UTF_8) : null);
    }

    public HttpResponseBuilder cookie(Cookie cookie) {
        this.headers.addSetCookie(cookie);
        return this;
    }

    public HttpResponse build() {
        if (!headers.contains(HttpHeaders.CONTENT_LENGTH)) {
            headers.set(HttpHeaders.CONTENT_LENGTH, body != null ? String.valueOf(body.length) : "0");
        }
        return new HttpResponse(httpVersion, statusCode, headers, body);
    }
}
