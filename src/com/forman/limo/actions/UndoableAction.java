package com.forman.limo.actions;

import java.util.concurrent.Callable;

public interface UndoableAction extends Callable<Boolean>, Action {
    String getName();
    boolean canUndo();
    boolean canRedo();
    void undo() throws Exception;
    void redo() throws Exception;
}
