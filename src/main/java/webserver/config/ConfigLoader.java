package webserver.config;

import webserver.http.HttpMethod;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static webserver.config.ConfigParser.ParsedDirective;
import static webserver.config.ConfigParser.ParsedRoute;
import static webserver.config.ConfigParser.ParsedServer;

public class ConfigLoader {

    private final ConfigParser parser;
    private final ConfigValidator validator;

    public ConfigLoader() {
        this.parser = new ConfigParser();
        this.validator = new ConfigValidator();
    }

    public ConfigLoader(ConfigParser parser, ConfigValidator validator) {
        this.parser = parser;
        this.validator = validator;
    }

    public List<ServerConfig> load(Path path) throws IOException {
        String content = Files.readString(path);
        List<ParsedServer> parsed = parser.parse(content);
        List<String> errors = validator.validate(parsed);
        if (!errors.isEmpty()) {
            throw new ConfigLoadException("Invalid configuration:\n" + String.join("\n", errors));
        }
        return toServerConfigs(parsed);
    }

    public List<ServerConfig> load(String path) throws IOException {
        return load(Path.of(path));
    }

    private List<ServerConfig> toServerConfigs(List<ParsedServer> parsedServers) {
        List<ServerConfig> configs = new ArrayList<>();
        for (ParsedServer ps : parsedServers) {
            configs.add(buildServerConfig(ps));
        }
        return configs;
    }

    private ServerConfig buildServerConfig(ParsedServer ps) {
        ServerConfig.Builder builder = new ServerConfig.Builder();

        for (ParsedDirective dir : ps.directives) {
            switch (dir.key) {
                case "host":
                    builder.host(dir.values.get(0));
                    break;
                case "port":
                    builder.port(Integer.parseInt(dir.values.get(0)));
                    break;
                case "default_server_root":
                    builder.defaultServerRoot(dir.values.get(0));
                    break;
                case "timeout":
                    builder.timeoutMs(Long.parseLong(dir.values.get(0)));
                    break;
                case "client_body_limit":
                    builder.clientBodyLimit(Long.parseLong(dir.values.get(0)));
                    break;
                case "error_page":
                    int code = Integer.parseInt(dir.values.get(0));
                    String path = String.join(" ", dir.values.subList(1, dir.values.size()));
                    builder.errorPage(code, path);
                    break;
                case "directory_listing":
                    builder.directoryListing(Boolean.parseBoolean(dir.values.get(0)));
                    break;
                case "allowed_methods":
                    builder.allowedMethods(dir.values.stream()
                            .map(v -> HttpMethod.valueOf(v.toUpperCase()))
                            .toList());
                    break;
                case "cgi_extensions":
                    builder.cgiExtensions(dir.values);
                    break;
            }
        }

        for (ParsedRoute pr : ps.routes) {
            RouteConfig route = buildRouteConfig(pr);
            builder.route(route.getPath(), route);
        }

        return builder.build();
    }

    private RouteConfig buildRouteConfig(ParsedRoute pr) {
        RouteConfig.Builder builder = new RouteConfig.Builder();
        builder.path(pr.path);

        for (ParsedDirective dir : pr.directives) {
            switch (dir.key) {
                case "root":
                    builder.root(dir.values.get(0));
                    break;
                case "default_file":
                    builder.defaultFile(dir.values.get(0));
                    break;
                case "redirect":
                    builder.redirect(dir.values.get(0));
                    break;
                case "timeout":
                    builder.timeoutMs(Long.parseLong(dir.values.get(0)));
                    break;
                case "client_body_limit":
                    builder.clientBodyLimit(Long.parseLong(dir.values.get(0)));
                    break;
                case "error_page":
                    int code = Integer.parseInt(dir.values.get(0));
                    String path = String.join(" ", dir.values.subList(1, dir.values.size()));
                    builder.errorPage(code, path);
                    break;
                case "directory_listing":
                    builder.directoryListing(Boolean.parseBoolean(dir.values.get(0)));
                    break;
                case "allowed_methods":
                    builder.allowedMethods(dir.values.stream()
                            .map(v -> HttpMethod.valueOf(v.toUpperCase()))
                            .toList());
                    break;
                case "cgi_extensions":
                    builder.cgiExtensions(dir.values);
                    break;
            }
        }

        return builder.build();
    }

    public static class ConfigLoadException extends RuntimeException {
        public ConfigLoadException(String message) {
            super(message);
        }
    }
}
