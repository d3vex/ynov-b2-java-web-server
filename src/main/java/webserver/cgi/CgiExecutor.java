package webserver.cgi;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class CgiExecutor {

    private static final long DEFAULT_TIMEOUT_MS = 10000;

    public CgiResult execute(File scriptFile, Map<String, String> environment,
                             byte[] requestBody, long timeoutMs) {
        try {
            ProcessBuilder pb = new ProcessBuilder(determineCommand(scriptFile));
            pb.environment().putAll(environment);

            long effectiveTimeout = timeoutMs > 0 ? timeoutMs : DEFAULT_TIMEOUT_MS;

            Process process = pb.start();

            if (requestBody != null && requestBody.length > 0) {
                try (OutputStream stdin = process.getOutputStream()) {
                    stdin.write(requestBody);
                    stdin.flush();
                }
            }

            ByteArrayOutputStream stdout = new ByteArrayOutputStream();
            ByteArrayOutputStream stderr = new ByteArrayOutputStream();
            Thread stdoutThread = drainStream(process.getInputStream(), stdout);
            Thread stderrThread = drainStream(process.getErrorStream(), stderr);
            stdoutThread.start();
            stderrThread.start();

            boolean finished = process.waitFor(effectiveTimeout, TimeUnit.MILLISECONDS);
            stdoutThread.join(2000);
            stderrThread.join(2000);

            if (!finished) {
                process.destroyForcibly();
                return new CgiResult(504, "Gateway Timeout", "CGI script timed out".getBytes(StandardCharsets.UTF_8));
            }

            int exitCode = process.exitValue();

            if (exitCode != 0) {
                String errMsg = stderr.size() > 0
                        ? stderr.toString(StandardCharsets.UTF_8)
                        : "CGI script exited with code " + exitCode;
                System.err.println("CGI error (" + scriptFile.getName() + "): " + errMsg);
            }

            return new CgiResult(200, "OK", stdout.toByteArray());

        } catch (Exception e) {
            System.err.println("CGI execution error: " + e.getMessage());
            return new CgiResult(500, "Internal Server Error",
                    ("CGI error: " + e.getMessage()).getBytes(StandardCharsets.UTF_8));
        }
    }

    private Thread drainStream(InputStream input, ByteArrayOutputStream output) {
        return new Thread(() -> {
            byte[] buffer = new byte[8192];
            try (InputStream is = input) {
                int read;
                while ((read = is.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                }
            } catch (Exception ignored) {
            }
        }, "cgi-stream-drain");
    }

    private List<String> determineCommand(File scriptFile) {
        String name = scriptFile.getName().toLowerCase();
        if (name.endsWith(".py")) {
            return List.of("python3", scriptFile.getAbsolutePath());
        }
        if (name.endsWith(".sh")) {
            return List.of("bash", scriptFile.getAbsolutePath());
        }
        if (name.endsWith(".pl")) {
            return List.of("perl", scriptFile.getAbsolutePath());
        }
        return List.of(scriptFile.getAbsolutePath());
    }

    public record CgiResult(int statusCode, String statusMessage, byte[] rawOutput) {
    }
}
