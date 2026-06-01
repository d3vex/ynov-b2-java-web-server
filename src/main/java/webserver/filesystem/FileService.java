package webserver.filesystem;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class FileService {

    public byte[] read(File file) throws IOException {
        return Files.readAllBytes(file.toPath());
    }

    public boolean exists(File file) {
        return file.exists() && file.isFile();
    }

    public boolean isDirectory(File file) {
        return file.exists() && file.isDirectory();
    }
}
