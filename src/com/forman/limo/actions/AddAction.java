package com.forman.limo.actions;

import com.forman.limo.AppInfo;
import com.forman.limo.data.Project;

import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public class AddAction extends AbstractUndoableAction {
    private final Project project;
    private final List<Path> newFiles;

    public AddAction(Project project, List<Path> newFiles) {
        this.project = project;
        this.newFiles = new ArrayList<>(newFiles);
    }

    @Override
    public String getName() {
        return MessageFormat.format(AppInfo.RES.getString("add.0.file.s"), newFiles.size());
    }

    @Override
    public boolean callImpl() {
        if (!newFiles.isEmpty()) {
            project.addFiles(newFiles);
            return true;
        }
        return false;
    }

    @Override
    public void undoImpl() {
        project.selectedImageFiles.removeAll(newFiles);
        project.imageFiles.removeAll(newFiles);
    }

    @Override
    public void redoImpl() {
        call();
    }
}
