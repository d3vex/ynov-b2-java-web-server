package webserver.filesystem;

import webserver.config.RouteConfig;
import webserver.config.ServerConfig;

import java.io.File;

public class PathResolver {

    private final SecurityPathValidator securityValidator = new SecurityPathValidator();

    public ResolvedPath resolve(String requestPath, RouteConfig route, ServerConfig config) {
        String rootDir = route != null && route.getRoot() != null
                ? route.getRoot()
                : config.getDefaultServerRoot();

        String relativePath = requestPath;
        if (route != null) {
            String routePath = route.getPath();
            if (!"/".equals(routePath) && relativePath.startsWith(routePath)) {
                relativePath = relativePath.substring(routePath.length());
            }
        }
        if (relativePath.isEmpty()) {
            relativePath = "/";
        }

        String filePath = rootDir + relativePath.replace('/', File.separatorChar);
        File file = new File(filePath);

        try {
            String canonical = file.getCanonicalPath();
            String canonicalRoot = new File(rootDir).getCanonicalPath();
            boolean secure = canonical.startsWith(canonicalRoot + File.separatorChar)
                    || canonical.equals(canonicalRoot);

            return new ResolvedPath(file, secure);
        } catch (Exception e) {
            return new ResolvedPath(file, false);
        }
    }

    public record ResolvedPath(File file, boolean secure) {
    }
}
