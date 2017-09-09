package com.forman.limo.actions;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;

import java.util.ArrayList;
import java.util.List;

public class UndoList implements Observable {
    private final List<UndoableAction> actionList = new ArrayList<>();
    private int actionIndex = -1;
    private final ArrayList<InvalidationListener> invalidationListeners = new ArrayList<>();

    public UndoList() {
    }

    public boolean canUndo() {
        return !actionList.isEmpty() && isValidActionIndex(actionIndex) && actionList.get(actionIndex).canUndo();
    }

    public boolean canRedo() {
        return !actionList.isEmpty() && isValidActionIndex(actionIndex + 1) && actionList.get(actionIndex + 1).canRedo();
    }

    public void clear() {
        actionList.clear();
        actionIndex = -1;
        notifyInvalidationListeners();
    }

    public void add(UndoableAction action) {
        assert action.canUndo();
        actionIndex++;
        actionList.removeAll(actionList.subList(actionIndex, actionList.size()));
        actionList.add(action);
        notifyInvalidationListeners();
    }

    public void undo() throws Exception {
        assert canUndo();
        UndoableAction action = actionList.get(actionIndex);
        try {
            action.undo();
        } finally {
            actionIndex--;
            notifyInvalidationListeners();
        }
    }

    public void redo() throws Exception {
        assert canRedo();
        UndoableAction action = actionList.get(actionIndex + 1);
        try {
            action.redo();
        } finally {
            actionIndex++;
            notifyInvalidationListeners();
        }
    }

    private void notifyInvalidationListeners() {
        InvalidationListener[] listeners = this.invalidationListeners.toArray(new InvalidationListener[0]);
        for (InvalidationListener listener : listeners) {
            listener.invalidated(this);
        }
    }

    @Override
    public void addListener(InvalidationListener listener) {
        invalidationListeners.add(listener);
    }

    @Override
    public void removeListener(InvalidationListener listener) {
        invalidationListeners.remove(listener);
    }

    private boolean isValidActionIndex(int i) {
        return i >= 0 && i < actionList.size();
    }
}
