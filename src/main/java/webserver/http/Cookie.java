package webserver.http;

import java.util.Objects;

public class Cookie {

    private final String name;
    private String value;
    private String path;
    private String domain;
    private Integer maxAge;
    private boolean secure;
    private boolean httpOnly;
    private String sameSite;

    public Cookie(String name, String value) {
        this.name = Objects.requireNonNull(name, "Cookie name must not be null");
        this.value = Objects.requireNonNull(value, "Cookie value must not be null");
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = Objects.requireNonNull(value, "Cookie value must not be null");
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public Integer getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(Integer maxAge) {
        this.maxAge = maxAge;
    }

    public boolean isSecure() {
        return secure;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    public boolean isHttpOnly() {
        return httpOnly;
    }

    public void setHttpOnly(boolean httpOnly) {
        this.httpOnly = httpOnly;
    }

    public String getSameSite() {
        return sameSite;
    }

    public void setSameSite(String sameSite) {
        this.sameSite = sameSite;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Cookie cookie)) return false;
        return name.equals(cookie.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
