package webserver.filesystem;

import java.io.File;
import java.io.IOException;

public class SecurityPathValidator {

    public boolean isValid(String resolvedPath, String rootDir) {
        if (resolvedPath == null || resolvedPath.isEmpty()) {
            return false;
        }

        if (resolvedPath.contains("..")) {
            return false;
        }

        try {
            String canonical = new File(resolvedPath).getCanonicalPath();
            String canonicalRoot = new File(rootDir).getCanonicalPath();
            return canonical.startsWith(canonicalRoot + File.separator)
                    || canonical.equals(canonicalRoot);
        } catch (IOException e) {
            return false;
        }
    }
}
