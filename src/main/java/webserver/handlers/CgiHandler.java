package webserver.handlers;

import webserver.cgi.*;
import webserver.config.RouteConfig;
import webserver.config.ServerConfig;
import webserver.http.HttpHeaders;
import webserver.http.HttpRequest;
import webserver.http.HttpResponse;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class CgiHandler {

    private final CgiPathResolver pathResolver = new CgiPathResolver();
    private final CgiEnvironmentBuilder envBuilder = new CgiEnvironmentBuilder();
    private final CgiExecutor executor = new CgiExecutor();
    private final ProcessOutputParser outputParser = new ProcessOutputParser();

    public HttpResponse handle(HttpRequest request, RouteConfig route, ServerConfig config) {
        CgiPathResolver.CgiResolvedPath resolved = pathResolver.resolve(request.getPath(), route, config);
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
        String contentType = request.getHeaders().get(HttpHeaders.CONTENT_TYPE);
        boolean isMultipart = contentType != null && contentType.toLowerCase().startsWith("multipart/form-data");

        UploadHandler uploadHandler = null;
        UploadHandler.UploadResult uploadResult = null;
        byte[] stdinBody = request.getBody();

        if (isMultipart && stdinBody != null && stdinBody.length > 0) {
            uploadHandler = new UploadHandler();
            try {
                uploadResult = uploadHandler.handle(request);
                stdinBody = uploadResult.stdinBody();
            } catch (Exception e) {
                System.err.println("Upload handling error: " + e.getMessage());
            }
        }

        int port = config.getPorts().isEmpty() ? 80 : config.getPorts().get(0);

        Map<String, String> env = envBuilder.build(request, route, config,
                scriptName, pathInfo, port);

        if (uploadResult != null) {
            env.put("CONTENT_TYPE", "application/x-www-form-urlencoded");
            env.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(stdinBody.length));

            int fileIdx = 0;
            for (UploadHandler.FileInfo file : uploadResult.files()) {
                env.put("UPLOAD_FILE_" + fileIdx + "_FIELD", file.fieldName());
                env.put("UPLOAD_FILE_" + fileIdx + "_NAME", file.originalName());
                env.put("UPLOAD_FILE_" + fileIdx + "_PATH", file.tempPath());
                if (file.contentType() != null) {
                    env.put("UPLOAD_FILE_" + fileIdx + "_TYPE", file.contentType());
                }
                fileIdx++;
            }
            env.put("UPLOAD_FILE_COUNT", String.valueOf(fileIdx));
        }

        long timeout = config.resolveTimeout(request.getPath(), request.getMethod());

        CgiExecutor.CgiResult result = executor.execute(scriptFile, env,
                stdinBody, timeout);

        if (uploadHandler != null) {
            uploadHandler.cleanup();
        }

        ProcessOutputParser.ParsedCgiOutput parsed = outputParser.parse(result.rawOutput());

        return HttpResponse.builder()
                .httpVersion(request.getHttpVersion())
                .status(parsed.status())
                .headers(parsed.headers())
                .body(parsed.body())
                .build();
    }
}
