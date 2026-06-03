package webserver.filesystem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class UploadStorage {

    private final List<Path> tempFiles = new ArrayList<>();

    public record StoredFile(String originalName, String tempPath, String contentType) {}

    public StoredFile store(String originalName, byte[] data, String contentType) throws IOException {
        Path tempFile = Files.createTempFile("upload_", "_" + sanitize(originalName));
        Files.write(tempFile, data);
        tempFiles.add(tempFile);
        return new StoredFile(originalName, tempFile.toAbsolutePath().toString(), contentType);
    }

    public void cleanup() {
        for (Path path : tempFiles) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException ignored) {
            }
        }
        tempFiles.clear();
    }

    private String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
