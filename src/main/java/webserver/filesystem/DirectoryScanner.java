package webserver.filesystem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DirectoryScanner {

    public List<File> listFiles(File directory) {
        List<File> files = new ArrayList<>();
        File[] entries = directory.listFiles();
        if (entries != null) {
            for (File entry : entries) {
                files.add(entry);
            }
        }
        return files;
    }

    public List<File> listFiles(File directory, String prefix) {
        List<File> files = new ArrayList<>();
        File[] entries = directory.listFiles();
        if (entries != null) {
            for (File entry : entries) {
                if (entry.getName().startsWith(prefix)) {
                    files.add(entry);
                }
            }
        }
        return files;
    }
}
