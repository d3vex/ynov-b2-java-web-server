package webserver.handlers;

import webserver.http.HttpHeaders;
import webserver.http.HttpRequest;
import webserver.http.HttpResponse;

public class DeleteHandler {

    public HttpResponse handle(HttpRequest request) {
        String raw = reconstructRawRequest(request);

        return HttpResponse.builder()
                .header(HttpHeaders.CONTENT_TYPE, "text/plain; charset=utf-8")
                .body(raw)
                .build();
    }

    private String reconstructRawRequest(HttpRequest req) {
        StringBuilder sb = new StringBuilder();

        sb.append(req.getMethod()).append(' ')
                .append(req.getPath());

        if (!req.getQueryParameters().isEmpty()) {
            sb.append('?');
            var params = req.getQueryParameters();
            boolean first = true;
            for (var entry : params.entrySet()) {
                if (!first) sb.append('&');
                first = false;
                sb.append(entry.getKey()).append('=').append(entry.getValue());
            }
        }

        sb.append(' ').append(req.getHttpVersion()).append("\r\n");

        for (String line : req.getHeaders().toHeaderLines()) {
            sb.append(line).append("\r\n");
        }
        sb.append("\r\n");

        String body = req.getBodyAsString();
        if (!body.isEmpty()) {
            sb.append(body);
        }

        return sb.toString();
    }
}
