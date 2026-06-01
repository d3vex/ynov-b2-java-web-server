package webserver.http;

public enum RequestParserState {
    REQUEST_LINE,
    HEADERS,
    BODY,
    COMPLETE,
    ERROR
}
