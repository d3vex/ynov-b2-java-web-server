package webserver.cgi;

import webserver.config.RouteConfig;

import java.io.File;

public class CgiPathResolver {

    public CgiResolvedPath resolve(String requestPath, RouteConfig route) {
        String rootDir = route != null && route.getRoot() != null
                ? route.getRoot()
                : "www";

        String routePath = route != null ? route.getPath() : "/";
        String relativePath = requestPath;
        if (!"/".equals(routePath) && relativePath.startsWith(routePath)) {
            relativePath = relativePath.substring(routePath.length());
        }
        if (relativePath.isEmpty()) {
            relativePath = "/";
        }

        String[] segments = relativePath.split("/", -1);
        StringBuilder currentPath = new StringBuilder();

        for (int i = 1; i < segments.length; i++) {
            if (currentPath.length() > 0) {
                currentPath.append("/");
            }
            currentPath.append(segments[i]);

            String candidateFilePath = rootDir + "/" + currentPath;
            File candidate = new File(candidateFilePath);

            if (candidate.isFile() && hasCgiExtension(candidate.getName(), route)) {
                String scriptName = routePath + "/" + currentPath;

                StringBuilder pathInfo = new StringBuilder();
                for (int j = i + 1; j < segments.length; j++) {
                    pathInfo.append("/").append(segments[j]);
                }

                return new CgiResolvedPath(candidate, scriptName, pathInfo.toString());
            }
        }

        return null;
    }

    private boolean hasCgiExtension(String filename, RouteConfig route) {
        if (route == null || route.getCgiExtensions() == null) {
            return false;
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0) return false;
        String ext = filename.substring(dot);
        return route.getCgiExtensions().contains(ext);
    }

    public record CgiResolvedPath(File scriptFile, String scriptName, String pathInfo) {
    }
}
