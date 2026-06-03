package webserver.cgi;

import webserver.config.RouteConfig;
import webserver.config.ServerConfig;

import java.io.File;
import java.util.List;

public class CgiPathResolver {

    public CgiResolvedPath resolve(String requestPath, RouteConfig route, ServerConfig config) {
        String rootDir = route != null && route.getRoot() != null
                ? route.getRoot()
                : config.getDefaultServerRoot();

        String routePath = route != null ? route.getPath() : "/";
        String relativePath = requestPath;
        if (!"/".equals(routePath) && relativePath.startsWith(routePath)) {
            relativePath = relativePath.substring(routePath.length());
        }
        if (relativePath.isEmpty()) {
            relativePath = "/";
        }

        List<String> extensions = route != null && route.getCgiExtensions() != null
                ? route.getCgiExtensions()
                : config.resolveCgiExtensions(requestPath);
        if (extensions.isEmpty()) return null;

        String[] segments = relativePath.split("/", -1);
        StringBuilder currentPath = new StringBuilder();

        for (int i = 1; i < segments.length; i++) {
            if (currentPath.length() > 0) {
                currentPath.append("/");
            }
            currentPath.append(segments[i]);

            String candidateFilePath = rootDir + "/" + currentPath;
            File candidate = new File(candidateFilePath);

            if (candidate.isFile() && hasCgiExtension(candidate.getName(), extensions)) {
                String scriptName = (routePath.endsWith("/") ? routePath : routePath + "/") + currentPath;

                StringBuilder pathInfo = new StringBuilder();
                for (int j = i + 1; j < segments.length; j++) {
                    pathInfo.append("/").append(segments[j]);
                }

                return new CgiResolvedPath(candidate, scriptName, pathInfo.toString());
            }
        }

        return null;
    }

    private boolean hasCgiExtension(String filename, List<String> extensions) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0) return false;
        String ext = filename.substring(dot);
        for (String configured : extensions) {
            if (configured.equalsIgnoreCase(ext)) return true;
        }
        return false;
    }

    public record CgiResolvedPath(File scriptFile, String scriptName, String pathInfo) {
    }
}
