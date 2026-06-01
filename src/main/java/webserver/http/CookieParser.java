package webserver.http;

import java.util.ArrayList;
import java.util.List;

public class CookieParser {

    public List<Cookie> parseCookies(String headerValue) {
        List<Cookie> cookies = new ArrayList<>();
        if (headerValue == null || headerValue.isBlank()) {
            return cookies;
        }
        String[] parts = headerValue.split(";");
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) continue;
            int eq = trimmed.indexOf('=');
            if (eq > 0) {
                String name = trimmed.substring(0, eq).trim();
                String value = trimmed.substring(eq + 1).trim();
                cookies.add(new Cookie(name, value));
            }
        }
        return cookies;
    }

    public Cookie parseSetCookie(String headerValue) {
        if (headerValue == null || headerValue.isBlank()) {
            return null;
        }
        String[] parts = headerValue.split(";");
        String first = parts[0].trim();
        int eq = first.indexOf('=');
        if (eq <= 0) return null;

        Cookie cookie = new Cookie(first.substring(0, eq).trim(), first.substring(eq + 1).trim());

        for (int i = 1; i < parts.length; i++) {
            String attr = parts[i].trim();
            int ae = attr.indexOf('=');
            String attrName = ae > 0 ? attr.substring(0, ae).trim() : attr;
            String attrValue = ae > 0 ? attr.substring(ae + 1).trim() : null;

            switch (attrName.toLowerCase()) {
                case "path" -> cookie.setPath(attrValue);
                case "domain" -> cookie.setDomain(attrValue);
                case "max-age" -> {
                    if (attrValue != null) {
                        try {
                            cookie.setMaxAge(Integer.parseInt(attrValue));
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
                case "secure" -> cookie.setSecure(true);
                case "httponly" -> cookie.setHttpOnly(true);
                case "samesite" -> cookie.setSameSite(attrValue);
            }
        }
        return cookie;
    }

    public String formatCookie(List<Cookie> cookies) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cookies.size(); i++) {
            if (i > 0) sb.append("; ");
            Cookie c = cookies.get(i);
            sb.append(c.getName()).append('=').append(c.getValue());
        }
        return sb.toString();
    }

    public String formatSetCookie(Cookie cookie) {
        StringBuilder sb = new StringBuilder();
        sb.append(cookie.getName()).append('=').append(cookie.getValue());

        if (cookie.getPath() != null) {
            sb.append("; Path=").append(cookie.getPath());
        }
        if (cookie.getDomain() != null) {
            sb.append("; Domain=").append(cookie.getDomain());
        }
        if (cookie.getMaxAge() != null) {
            sb.append("; Max-Age=").append(cookie.getMaxAge());
        }
        if (cookie.isSecure()) {
            sb.append("; Secure");
        }
        if (cookie.isHttpOnly()) {
            sb.append("; HttpOnly");
        }
        if (cookie.getSameSite() != null) {
            sb.append("; SameSite=").append(cookie.getSameSite());
        }
        return sb.toString();
    }
}
