package webserver.errors;

import webserver.http.HttpStatus;

public class DefaultErrorPages {

    public String render(HttpStatus status) {
        return render(status, status.getReasonPhrase());
    }

    public String render(HttpStatus status, String message) {
        int code = status.getCode();
        return """
                <!DOCTYPE html>
                <html>
                <head><title>%d %s</title></head>
                <body>
                <h1>%d %s</h1>
                <hr>
                </body>
                </html>
                """.formatted(code, message, code, message);
    }
}
