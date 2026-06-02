package webserver.http;

public class HttpStatus {
    public static final HttpStatus OK = new HttpStatus(200, "OK");

    public static final HttpStatus REDIRECT_PERMANENT = new HttpStatus(301, "Moved Permanently");
    public static final HttpStatus REDIRECT_FOUND = new HttpStatus(302, "Found");

    public static final HttpStatus BAD_REQUEST = new HttpStatus(400, "Bad Request");
    public static final HttpStatus UNAUTHORIZED = new HttpStatus(401, "Unauthorized");
    public static final HttpStatus PAYMENT_REQUIRED = new HttpStatus(402, "Payment Required");
    public static final HttpStatus FORBIDDEN = new HttpStatus(403, "Forbidden");
    public static final HttpStatus NOT_FOUND = new HttpStatus(404, "Not Found");
    public static final HttpStatus METHOD_NOT_ALLOWED = new HttpStatus(405, "Method Not Allowed");
    public static final HttpStatus NOT_ACCEPTABLE = new HttpStatus(406, "Not Acceptable");
    public static final HttpStatus REQUEST_TIMEOUT = new HttpStatus(408, "Request Timeout");
    public static final HttpStatus PAYLOAD_TOO_LARGE = new HttpStatus(413, "Payload Too Large");
    public static final HttpStatus UNSUPPORTED_MEDIA_TYPE = new HttpStatus(415, "Unsupported Media Type");
    public static final HttpStatus UPGRADE_REQUIRED = new HttpStatus(426, "Upgrade Required");
    public static final HttpStatus TOO_MANY_REQUESTS = new HttpStatus(429, "Too Many Requests");
    public static final HttpStatus INTERNAL_SERVER_ERROR = new HttpStatus(500, "Internal Server Error");
    public static final HttpStatus NOT_IMPLEMENTED = new HttpStatus(501, "Not Implemented");
    public static final HttpStatus BAD_GATEWAY = new HttpStatus(502, "Bad Gateway");
    public static final HttpStatus SERVICE_UNAVAILABLE = new HttpStatus(503, "Service Unavailable");
    public static final HttpStatus GATEWAY_TIMEOUT = new HttpStatus(504, "Gateway Timeout");
    public static final HttpStatus HTTP_VERSION_NOT_SUPPORTED = new HttpStatus(505, "HTTP Version Not Supported");

    private final int code;
    private final String reasonPhrase;

    public HttpStatus(int code, String reasonPhrase) {
        this.code = code;
        this.reasonPhrase = reasonPhrase;
    }

    public static HttpStatus fromCode(int code, String reasonPhrase) {
        return new HttpStatus(code, reasonPhrase);
    }

    public int getCode() {
        return code;
    }

    public String getReasonPhrase() {
        return reasonPhrase;
    }
}
