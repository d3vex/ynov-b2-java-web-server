package webserver.config;

import java.util.ArrayList;
import java.util.List;

public class ConfigParser {

    public List<ParsedServer> parse(String content) {
        List<ParsedServer> servers = new ArrayList<>();
        content = content.replace("{", "\n{\n").replace("}", "\n}\n");
        String[] rawLines = content.split("\n");

        ParsedServer currentServer = null;
        ParsedRoute currentRoute = null;

        for (String rawLine : rawLines) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            if (line.equals("server")) {
                currentServer = new ParsedServer();
                currentRoute = null;
            } else if (line.equals("{")) {
                // block opener, context already set by server/route keyword
            } else if (line.equals("}")) {
                if (currentRoute != null) {
                    currentServer.routes.add(currentRoute);
                    currentRoute = null;
                } else if (currentServer != null) {
                    servers.add(currentServer);
                    currentServer = null;
                }
            } else if (line.startsWith("route ")) {
                String rest = line.substring("route ".length()).trim();
                if (rest.endsWith("{")) {
                    rest = rest.substring(0, rest.length() - 1).trim();
                }
                currentRoute = new ParsedRoute(rest);
            } else {
                String[] parts = line.split("\\s+");
                String key = parts[0];
                List<String> values = new ArrayList<>();
                for (int j = 1; j < parts.length; j++) {
                    values.add(parts[j]);
                }
                ParsedDirective directive = new ParsedDirective(key, values);
                if (currentRoute != null) {
                    currentRoute.directives.add(directive);
                } else if (currentServer != null) {
                    currentServer.directives.add(directive);
                }
            }
        }

        return servers;
    }

    public static class ParsedDirective {
        public final String key;
        public final List<String> values;

        public ParsedDirective(String key, List<String> values) {
            this.key = key;
            this.values = values;
        }
    }

    public static class ParsedRoute {
        public final String path;
        public final List<ParsedDirective> directives;

        public ParsedRoute(String path) {
            this.path = path;
            this.directives = new ArrayList<>();
        }
    }

    public static class ParsedServer {
        public final List<ParsedDirective> directives;
        public final List<ParsedRoute> routes;

        public ParsedServer() {
            this.directives = new ArrayList<>();
            this.routes = new ArrayList<>();
        }
    }
}
