package com.forman.limo.actions;

public abstract class AbstractUndoableAction implements UndoableAction {
    private Boolean done;

    @Override
    public void run() {
        call();
    }

    @Override
    public final Boolean call() {
        done = callImpl();
        return done;
    }

    public final boolean canUndo() {
        return done != null && done;
    }

    public final boolean canRedo() {
        return done != null && !done;
    }

    @Override
    public final void undo() {
        if (!canUndo()) {
            throw new IllegalStateException("cannot undo: " + getName());
        }
        undoImpl();
        done = false;
    }

    @Override
    public final void redo() {
        if (!canRedo()) {
            throw new IllegalStateException("cannot redo: " + getName());
        }
        redoImpl();
        done = true;
    }

    protected abstract boolean callImpl();

    protected abstract void undoImpl();

    protected abstract void redoImpl();
}
