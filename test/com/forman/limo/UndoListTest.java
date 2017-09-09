package com.forman.limo;

import com.forman.limo.actions.AbstractUndoableAction;
import com.forman.limo.actions.UndoList;
import junit.framework.TestCase;

public class UndoListTest extends TestCase {
    public void testZero() throws Exception {
        UndoList actionList = new UndoList();
        assertEquals(false, actionList.canUndo());
        assertEquals(false, actionList.canRedo());
    }

    public void testOne() throws Exception {
        UndoList actionList = new UndoList();

        TestAction actionA = new TestAction("A");
        actionA.run();
        actionList.add(actionA);

        assertEquals(true, actionList.canUndo());
        assertEquals(false, actionList.canRedo());
        assertEquals("call;", actionA.trace);

        actionList.undo();
        assertEquals(false, actionList.canUndo());
        assertEquals(true, actionList.canRedo());
        assertEquals("call;undo;", actionA.trace);

        actionList.redo();
        assertEquals(true, actionList.canUndo());
        assertEquals(false, actionList.canRedo());
        assertEquals("call;undo;redo;", actionA.trace);

        actionList.undo();
        assertEquals(false, actionList.canUndo());
        assertEquals(true, actionList.canRedo());
        assertEquals("call;undo;redo;undo;", actionA.trace);
    }

    public void testMore() throws Exception {
        UndoList actionList = new UndoList();

        TestAction actionA = new TestAction("A");
        TestAction actionB = new TestAction("B");
        TestAction actionC = new TestAction("C");

        actionA.call();
        actionList.add(actionA);
        actionB.call();
        actionList.add(actionB);
        actionC.call();
        actionList.add(actionC);

        assertEquals(true, actionList.canUndo());
        assertEquals(false, actionList.canRedo());

        actionList.undo();
        assertEquals(true, actionList.canUndo());
        assertEquals(true, actionList.canRedo());

        actionList.undo();
        assertEquals(true, actionList.canUndo());
        assertEquals(true, actionList.canRedo());

        actionList.undo();
        assertEquals(false, actionList.canUndo());
        assertEquals(true, actionList.canRedo());

        actionList.redo();
        assertEquals(true, actionList.canUndo());
        assertEquals(true, actionList.canRedo());

        actionList.redo();
        assertEquals(true, actionList.canUndo());
        assertEquals(true, actionList.canRedo());

        actionList.redo();
        assertEquals(true, actionList.canUndo());
        assertEquals(false, actionList.canRedo());

        assertEquals("call;undo;redo;", actionA.trace);
        assertEquals("call;undo;redo;", actionB.trace);
        assertEquals("call;undo;redo;", actionC.trace);
    }

    private static class TestAction extends AbstractUndoableAction {
        private final String name;
        String trace = "";

        public TestAction(String name) {
            this.name = name;
        }

        @Override
        protected boolean callImpl() {
            trace += "call;";
            return true;
        }

        @Override
        protected void undoImpl() {
            trace += "undo;";
        }

        @Override
        protected void redoImpl() {
            trace += "redo;";
        }

        @Override
        public String getName() {
            return this.name;
        }
    }
}
