package com.forman.limo;

import javafx.concurrent.Service;
import javafx.concurrent.Task;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class CopyAndRenameImagesService extends Service<Void> {

    private List<Path> files;
    private Path directory;
    private String pattern;
    private int startIndex;

    public void setFiles(List<Path> files) {
        this.files = files;
    }

    public void setDirectory(Path directory) {
        this.directory = directory;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    @Override
    protected Task<Void> createTask() {
        return new CopyAndRenameImagesTask(files.toArray(new Path[files.size()]), directory, pattern, startIndex);
    }

    public void setStartIndex(int startIndex) {
        this.startIndex = startIndex;
    }
}

class CopyAndRenameImagesTask extends Task<Void> {
    private final Path[] files;
    private final Path directory;
    private final String pattern;
    private final int startIndex;

    public CopyAndRenameImagesTask(Path[] files, Path directory, String pattern, int startIndex) {
        this.files = files;
        this.directory = directory;
        this.pattern = pattern;
        this.startIndex = startIndex;
    }

    @Override
    protected Void call() throws Exception {
        // TODO - check if files exist
        updateProgress(0, files.length);
        Files.createDirectories(directory);
        Map<String, String> replacements  = new HashMap<>();
        for (int i = 0; i < files.length; i++) {
            if (isCancelled() ) {
                break;
            }
            Path sourceFile = files[i];
            String sourceFileName = sourceFile.getFileName().toString();
            String sourceFileNameExt = "";
            int extIndex = sourceFileName.lastIndexOf('.');
            if (extIndex > 0) {
                sourceFileNameExt = sourceFileName.substring(extIndex);
                replacements.put("{NAME}", sourceFileName.substring(0, extIndex));
                replacements.put("{FORMAT}", sourceFileNameExt.substring(1).toUpperCase());
            } else {
                replacements.put("{NAME}", sourceFileName);
                replacements.put("{FORMAT}", "");
            }

            String targetFileName = StringReplacer.replace(pattern, startIndex + i, replacements) + sourceFileNameExt;
            Path targetFile = directory.resolve(targetFileName);
            if (Files.exists(targetFile)) {
                try {
                    Files.delete(targetFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                Files.copy(sourceFile, targetFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
            updateProgress(i + 1, files.length);
        }
        updateMessage(MessageFormat.format("{0} file(s) copied", files.length));
        return null;
    }
}
