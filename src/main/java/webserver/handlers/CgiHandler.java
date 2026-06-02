package webserver.handlers;

import webserver.cgi.*;
import webserver.config.RouteConfig;
import webserver.config.ServerConfig;
import webserver.http.HttpRequest;
import webserver.http.HttpResponse;

import java.io.File;
import java.util.Map;

public class CgiHandler {

    private final CgiPathResolver pathResolver = new CgiPathResolver();
    private final CgiEnvironmentBuilder envBuilder = new CgiEnvironmentBuilder();
    private final CgiExecutor executor = new CgiExecutor();
    private final ProcessOutputParser outputParser = new ProcessOutputParser();

    public HttpResponse handle(HttpRequest request, RouteConfig route, ServerConfig config) {
        CgiPathResolver.CgiResolvedPath resolved = pathResolver.resolve(request.getPath(), route);
        if (resolved == null) {
            return null;
        }

        return executeCgi(request, route, config, resolved.scriptFile(),
                resolved.scriptName(), resolved.pathInfo());
    }

    public HttpResponse handleFile(HttpRequest request, RouteConfig route, ServerConfig config,
                                   File file, String scriptName) {
        return executeCgi(request, route, config, file, scriptName, "");
    }

    private HttpResponse executeCgi(HttpRequest request, RouteConfig route, ServerConfig config,
                                    File scriptFile, String scriptName, String pathInfo) {
        int port = config.getPorts().isEmpty() ? 80 : config.getPorts().get(0);

        Map<String, String> env = envBuilder.build(request, route, config,
                scriptName, pathInfo, port);

        long timeout = config.resolveTimeout(request.getPath(), request.getMethod());

        CgiExecutor.CgiResult result = executor.execute(scriptFile, env,
                request.getBody(), timeout);

        ProcessOutputParser.ParsedCgiOutput parsed = outputParser.parse(result.rawOutput());

        return HttpResponse.builder()
                .httpVersion(request.getHttpVersion())
                .status(parsed.status())
                .headers(parsed.headers())
                .body(parsed.body())
                .build();
    }
}
