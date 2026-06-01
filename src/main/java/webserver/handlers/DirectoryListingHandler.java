package webserver.handlers;

import webserver.filesystem.DirectoryScanner;
import webserver.http.HttpHeaders;
import webserver.http.HttpResponse;

import java.io.File;

public class DirectoryListingHandler {

    private final DirectoryScanner scanner = new DirectoryScanner();

    public HttpResponse handle(File directory, String requestPath) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n")
            .append("<html>\n<head>\n")
            .append("<title>Index of ").append(requestPath).append("</title>\n")
            .append("</head>\n<body>\n")
            .append("<h1>Index of ").append(requestPath).append("</h1>\n")
            .append("<hr>\n<pre>\n");

        if (!"/".equals(requestPath)) {
            String parent = requestPath.endsWith("/")
                    ? requestPath.substring(0, requestPath.length() - 1)
                    : requestPath;
            int slash = parent.lastIndexOf('/');
            String parentPath = slash > 0 ? parent.substring(0, slash) : "/";
            html.append("<a href=\"").append(parentPath).append("\">../</a>\n");
        }

        File[] entries = directory.listFiles();
        if (entries != null) {
            for (File entry : entries) {
                String name = entry.getName();
                if (entry.isDirectory()) {
                    name += "/";
                }
                String href = requestPath.endsWith("/")
                        ? requestPath + entry.getName()
                        : requestPath + "/" + entry.getName();
                if (entry.isDirectory()) {
                    href += "/";
                }
                html.append("<a href=\"").append(href).append("\">").append(name).append("</a>\n");
            }
        }

        html.append("</pre>\n<hr>\n</body>\n</html>\n");

        return HttpResponse.builder()
                .header(HttpHeaders.CONTENT_TYPE, "text/html; charset=utf-8")
                .body(html.toString())
                .build();
    }
}
