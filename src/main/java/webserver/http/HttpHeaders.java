package webserver.http;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HttpHeaders {

    private static final CookieParser COOKIE_PARSER = new CookieParser();

    public static final String HOST = "Host";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String CONTENT_LENGTH = "Content-Length";
    public static final String TRANSFER_ENCODING = "Transfer-Encoding";
    public static final String CONNECTION = "Connection";
    public static final String ACCEPT = "Accept";
    public static final String AUTHORIZATION = "Authorization";
    public static final String COOKIE = "Cookie";
    public static final String SET_COOKIE = "Set-Cookie";
    public static final String LOCATION = "Location";
    public static final String DATE = "Date";
    public static final String SERVER = "Server";
    public static final String CONTENT_DISPOSITION = "Content-Disposition";
    public static final String CACHE_CONTROL = "Cache-Control";
    public static final String USER_AGENT = "User-Agent";
    public static final String REFERER = "Referer";
    public static final String ORIGIN = "Origin";
    public static final String UPGRADE = "Upgrade";
    public static final String X_FORWARDED_FOR = "X-Forwarded-For";
    public static final String X_REQUESTED_WITH = "X-Requested-With";

    private final Map<String, List<String>> headers;

    public HttpHeaders() {
        this.headers = new LinkedHashMap<>();
    }

    public HttpHeaders(Map<String, List<String>> headers) {
        this.headers = new LinkedHashMap<>();
        for (var entry : headers.entrySet()) {
            this.headers.put(normalize(entry.getKey()), new ArrayList<>(entry.getValue()));
        }
    }

    public void add(String name, String value) {
        String key = normalize(name);
        headers.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
    }

    public void set(String name, String value) {
        headers.put(normalize(name), new ArrayList<>(List.of(value)));
    }

    public void set(String name, List<String> values) {
        headers.put(normalize(name), new ArrayList<>(values));
    }

    public String get(String name) {
        List<String> values = headers.get(normalize(name));
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.get(0);
    }

    public List<String> getAll(String name) {
        List<String> values = headers.get(normalize(name));
        if (values == null) {
            return List.of();
        }
        return Collections.unmodifiableList(values);
    }

    public boolean contains(String name) {
        return headers.containsKey(normalize(name));
    }

    public List<String> remove(String name) {
        return headers.remove(normalize(name));
    }

    public Set<String> names() {
        return Collections.unmodifiableSet(headers.keySet());
    }

    public Map<String, List<String>> toMap() {
        Map<String, List<String>> copy = new LinkedHashMap<>();
        for (var entry : headers.entrySet()) {
            copy.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return copy;
    }

    public boolean isEmpty() {
        return headers.isEmpty();
    }

    public int size() {
        return headers.size();
    }

    public void clear() {
        headers.clear();
    }

    public List<Cookie> getCookies() {
        String cookieHeader = get(COOKIE);
        return cookieHeader != null ? COOKIE_PARSER.parseCookies(cookieHeader) : List.of();
    }

    public void setCookies(List<Cookie> cookies) {
        if (cookies.isEmpty()) {
            remove(COOKIE);
        } else {
            set(COOKIE, COOKIE_PARSER.formatCookie(cookies));
        }
    }

    public List<Cookie> getSetCookies() {
        List<String> values = getAll(SET_COOKIE);
        if (values.isEmpty()) return List.of();
        List<Cookie> cookies = new ArrayList<>();
        for (String value : values) {
            Cookie c = COOKIE_PARSER.parseSetCookie(value);
            if (c != null) cookies.add(c);
        }
        return cookies;
    }

    public void addSetCookie(Cookie cookie) {
        add(SET_COOKIE, COOKIE_PARSER.formatSetCookie(cookie));
    }

    public static HttpHeaders parse(List<String> headerLines) {
        HttpHeaders headers = new HttpHeaders();
        for (String line : headerLines) {
            int colonIndex = line.indexOf(':');
            if (colonIndex > 0) {
                String name = line.substring(0, colonIndex).trim();
                String value = line.substring(colonIndex + 1).trim();
                headers.add(name, value);
            }
        }
        return headers;
    }

    public List<String> toHeaderLines() {
        List<String> lines = new ArrayList<>();
        for (var entry : headers.entrySet()) {
            for (String value : entry.getValue()) {
                lines.add(entry.getKey() + ": " + value);
            }
        }
        return lines;
    }

    private static String normalize(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Header name must not be null or empty");
        }
        boolean capitalizeNext = true;
        StringBuilder sb = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == '-') {
                sb.append('-');
                capitalizeNext = true;
            } else if (capitalizeNext) {
                sb.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString();
    }
}
