package webserver.config;

import webserver.http.HttpMethod;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static webserver.config.ConfigParser.ParsedDirective;
import static webserver.config.ConfigParser.ParsedRoute;
import static webserver.config.ConfigParser.ParsedServer;

public class ConfigValidator {

    public List<String> validate(List<ParsedServer> servers) {
        List<String> errors = new ArrayList<>();
        if (servers.isEmpty()) {
            errors.add("No server blocks defined");
            return errors;
        }
        for (int i = 0; i < servers.size(); i++) {
            validateServer(servers.get(i), i, errors);
        }
        return errors;
    }

    private void validateServer(ParsedServer server, int index, List<String> errors) {
        boolean hasPort = false;

        for (ParsedDirective dir : server.directives) {
            switch (dir.key) {
                case "host":
                    if (dir.values.isEmpty() || dir.values.get(0).isEmpty()) {
                        errors.add("Server " + index + ": host must not be empty");
                    }
                    break;
                case "port":
                    hasPort = true;
                    if (dir.values.isEmpty()) {
                        errors.add("Server " + index + ": port value missing");
                    } else if (!isValidInteger(dir.values.get(0))) {
                        errors.add("Server " + index + ": invalid port '" + dir.values.get(0) + "'");
                    }
                    break;
                case "timeout":
                    if (!isValidLong(dir.values.get(0))) {
                        errors.add("Server " + index + ": invalid timeout '" + dir.values.get(0) + "'");
                    }
                    break;
                case "client_body_limit":
                    if (!isValidLong(dir.values.get(0))) {
                        errors.add("Server " + index + ": invalid client_body_limit '" + dir.values.get(0) + "'");
                    }
                    break;
                case "directory_listing":
                    if (!isValidBoolean(dir.values.get(0))) {
                        errors.add("Server " + index + ": invalid directory_listing '" + dir.values.get(0) + "'");
                    }
                    break;
                case "error_page":
                    if (dir.values.size() < 2) {
                        errors.add("Server " + index + ": error_page requires status code and path");
                    } else if (!isValidInteger(dir.values.get(0))) {
                        errors.add("Server " + index + ": invalid error_page status code '" + dir.values.get(0) + "'");
                    }
                    break;
                case "allowed_methods":
                    for (String v : dir.values) {
                        if (!isValidHttpMethod(v)) {
                            errors.add("Server " + index + ": invalid HTTP method '" + v + "'");
                        }
                    }
                    break;
            }
        }

        if (!hasPort) {
            errors.add("Server " + index + ": at least one port is required");
        }

        for (int r = 0; r < server.routes.size(); r++) {
            validateRoute(server.routes.get(r), index, r, errors);
        }
    }

    private void validateRoute(ParsedRoute route, int serverIndex, int routeIndex, List<String> errors) {
        if (route.path == null || !route.path.startsWith("/")) {
            errors.add("Server " + serverIndex + ", route " + routeIndex + ": path must start with '/'");
        }

        for (ParsedDirective dir : route.directives) {
            switch (dir.key) {
                case "root":
                    if (dir.values.isEmpty()) {
                        errors.add("Server " + serverIndex + ", route " + routeIndex + ": root value missing");
                    }
                    break;
                case "default_file":
                    if (dir.values.isEmpty()) {
                        errors.add("Server " + serverIndex + ", route " + routeIndex + ": default_file value missing");
                    }
                    break;
                case "timeout":
                    if (!isValidLong(dir.values.get(0))) {
                        errors.add("Server " + serverIndex + ", route " + routeIndex + ": invalid timeout '" + dir.values.get(0) + "'");
                    }
                    break;
                case "client_body_limit":
                    if (!isValidLong(dir.values.get(0))) {
                        errors.add("Server " + serverIndex + ", route " + routeIndex + ": invalid client_body_limit '" + dir.values.get(0) + "'");
                    }
                    break;
                case "directory_listing":
                    if (!isValidBoolean(dir.values.get(0))) {
                        errors.add("Server " + serverIndex + ", route " + routeIndex + ": invalid directory_listing '" + dir.values.get(0) + "'");
                    }
                    break;
                case "error_page":
                    if (dir.values.size() < 2) {
                        errors.add("Server " + serverIndex + ", route " + routeIndex + ": error_page requires status code and path");
                    } else if (!isValidInteger(dir.values.get(0))) {
                        errors.add("Server " + serverIndex + ", route " + routeIndex + ": invalid error_page status code '" + dir.values.get(0) + "'");
                    }
                    break;
                case "redirect":
                    if (dir.values.isEmpty()) {
                        errors.add("Server " + serverIndex + ", route " + routeIndex + ": redirect value missing");
                    }
                    break;
                case "allowed_methods":
                    for (String v : dir.values) {
                        if (!isValidHttpMethod(v)) {
                            errors.add("Server " + serverIndex + ", route " + routeIndex + ": invalid HTTP method '" + v + "'");
                        }
                    }
                    break;
            }
        }
    }

    private boolean isValidInteger(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isValidLong(String s) {
        try {
            Long.parseLong(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isValidBoolean(String s) {
        return "true".equalsIgnoreCase(s) || "false".equalsIgnoreCase(s);
    }

    private boolean isValidHttpMethod(String s) {
        try {
            HttpMethod.valueOf(s.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
