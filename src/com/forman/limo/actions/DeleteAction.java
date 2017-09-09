package com.forman.limo.actions;

import com.forman.limo.AppInfo;
import com.forman.limo.data.Project;

import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public class DeleteAction extends AbstractUndoableAction {
    private final Project project;
    private final List<Path> selectedFiles;
    private List<Path> oldFiles;

    public DeleteAction(Project project, List<Path> selectedFiles) {
        this.project = project;
        this.selectedFiles = new ArrayList<>(selectedFiles);
    }

    @Override
    public String getName() {
        return MessageFormat.format(AppInfo.RES.getString("remove.0.file.s"), selectedFiles.size());
    }

    @Override
    public boolean callImpl() {
        if (!selectedFiles.isEmpty()) {
            List<Integer> imageFileIndexes = project.getImageFileIndexes(selectedFiles);
            Integer index = imageFileIndexes.get(0);

            oldFiles = new ArrayList<>(project.imageFiles);
            project.selectedImageFiles.clear();
            project.imageFiles.removeAll(selectedFiles);

            if (index >= 0 && index < project.imageFiles.size()) {
                project.selectedImageFiles.setAll(project.imageFiles.get(index));
            } else if (index - 1 >= 0 && index - 1 < project.imageFiles.size()) {
                project.selectedImageFiles.setAll(project.imageFiles.get(index - 1));
            }
            return true;
        }
        return false;
    }

    @Override
    public void undoImpl() {
        project.selectedImageFiles.clear();
        project.imageFiles.setAll(oldFiles);
        project.selectedImageFiles.setAll(selectedFiles);
    }

    @Override
    public void redoImpl() {
        call();
    }
}
