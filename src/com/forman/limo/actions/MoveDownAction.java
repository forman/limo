package com.forman.limo.actions;

import com.forman.limo.AppInfo;
import com.forman.limo.data.Project;

import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public class MoveDownAction extends AbstractUndoableAction {
    private final Project project;
    private final List<Path> selectedFiles;
    private final boolean moveToBottom;
    private List<Path> oldFiles;

    public MoveDownAction(Project project, List<Path> selectedFiles, boolean moveToBottom) {
        this.project = project;
        this.selectedFiles = new ArrayList<>(selectedFiles);
        this.moveToBottom = moveToBottom;
    }

    @Override
    public String getName() {
        return MessageFormat.format(AppInfo.RES.getString("move.down.0.file.s"), selectedFiles.size());
    }

    @Override
    public boolean callImpl() {
        List<Integer> selectedIndexes = project.getImageFileIndexes(this.selectedFiles);
        if (!selectedIndexes.isEmpty()) {
            int indexN = selectedIndexes.get(selectedIndexes.size() - 1);
            int insertionStartIndex = moveToBottom ? project.imageFiles.size() - 1 : indexN + 1;
            if (insertionStartIndex >= 0 && insertionStartIndex < project.imageFiles.size()) {
                oldFiles = new ArrayList<>(project.imageFiles);
                for (int i = selectedIndexes.size() - 1; i >= 0; i--) {
                    int index = selectedIndexes.get(i);
                    Path movedPath = project.imageFiles.remove(index);
                    project.imageFiles.add(insertionStartIndex - (indexN - index), movedPath);
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
