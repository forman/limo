package com.forman.limo.actions;

import com.forman.limo.AppInfo;
import com.forman.limo.data.Project;

import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public class MoveUpAction extends AbstractUndoableAction {
    private final Project project;
    private final List<Path> selectedFiles;
    private final boolean moveToTop;
    private List<Path> oldFiles;

    public MoveUpAction(Project project, List<Path> selectedFiles, boolean moveToTop) {
        this.project = project;
        this.selectedFiles = new ArrayList<>(selectedFiles);
        this.moveToTop = moveToTop;
    }

    @Override
    public String getName() {
        return MessageFormat.format(AppInfo.RES.getString("move.up.0.file.s"), selectedFiles.size());
    }

    @Override
    public boolean callImpl() {
        List<Integer> selectedIndexes = project.getImageFileIndexes(this.selectedFiles);
        if (!selectedIndexes.isEmpty()) {
            int index0 = selectedIndexes.get(0);
            int insertionStartIndex = moveToTop ? 0 : index0 - 1;
            if (insertionStartIndex >= 0 && insertionStartIndex < project.imageFiles.size() - 1) {
                oldFiles = new ArrayList<>(project.imageFiles);
                for (int index : selectedIndexes) {
                    Path movedPath = project.imageFiles.remove(index);
                    project.imageFiles.add(insertionStartIndex + (index - index0), movedPath);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public void undoImpl() {
        project.imageFiles.setAll(oldFiles);
    }

    @Override
    public void redoImpl() {
        call();
    }
}
