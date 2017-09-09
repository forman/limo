package com.forman.limo;

import com.forman.limo.actions.MoveUpAction;
import com.forman.limo.actions.MoveDownAction;
import com.forman.limo.data.Project;
import junit.framework.TestCase;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class MoveTest extends TestCase {
    public void testMoveUp() throws Exception {
        Project project = new Project();
        project.imageFiles.addAll(
                Paths.get("im1.jpg"),
                Paths.get("im2.jpg"),
                Paths.get("im3.jpg"),
                Paths.get("im4.jpg"));
        new MoveUpAction(project, Arrays.asList(Paths.get("im3.jpg")), false).call();
        assertListEquals(Arrays.asList(
                Paths.get("im1.jpg"),
                Paths.get("im3.jpg"),
                Paths.get("im2.jpg"),
                Paths.get("im4.jpg")), project.imageFiles);
        new MoveUpAction(project, Arrays.asList(Paths.get("im3.jpg")), false).call();
        assertListEquals(Arrays.asList(
                Paths.get("im3.jpg"),
                Paths.get("im1.jpg"),
                Paths.get("im2.jpg"),
                Paths.get("im4.jpg")), project.imageFiles);
        new MoveUpAction(project, Arrays.asList(Paths.get("im4.jpg")), true).call();
        assertListEquals(Arrays.asList(
                Paths.get("im4.jpg"),
                Paths.get("im3.jpg"),
                Paths.get("im1.jpg"),
                Paths.get("im2.jpg")
        ), project.imageFiles);
        new MoveUpAction(project, Arrays.asList(Paths.get("im3.jpg"), Paths.get("im2.jpg")), true).call();
        assertListEquals(Arrays.asList(
                Paths.get("im3.jpg"),
                Paths.get("im4.jpg"),
                Paths.get("im2.jpg"),
                Paths.get("im1.jpg")
        ), project.imageFiles);
    }

    public void testMoveDown() throws Exception {
        Project project = new Project();
        project.imageFiles.addAll(
                Paths.get("im1.jpg"),
                Paths.get("im2.jpg"),
                Paths.get("im3.jpg"),
                Paths.get("im4.jpg"));
        new MoveDownAction(project, Arrays.asList(Paths.get("im2.jpg")), false).call();
        assertListEquals(Arrays.asList(
                Paths.get("im1.jpg"),
                Paths.get("im3.jpg"),
                Paths.get("im2.jpg"),
                Paths.get("im4.jpg")), project.imageFiles);
        new MoveDownAction(project, Arrays.asList(Paths.get("im2.jpg")), false).call();
        assertListEquals(Arrays.asList(
                Paths.get("im1.jpg"),
                Paths.get("im3.jpg"),
                Paths.get("im4.jpg"),
                Paths.get("im2.jpg")), project.imageFiles);
        new MoveDownAction(project, Arrays.asList(Paths.get("im1.jpg")), true).call();
        assertListEquals(Arrays.asList(
                Paths.get("im3.jpg"),
                Paths.get("im4.jpg"),
                Paths.get("im2.jpg"),
                Paths.get("im1.jpg")
        ), project.imageFiles);
        new MoveDownAction(project, Arrays.asList(Paths.get("im3.jpg"), Paths.get("im2.jpg")), true).call();
        assertListEquals(Arrays.asList(
                Paths.get("im4.jpg"),
                Paths.get("im3.jpg"),
                Paths.get("im1.jpg"),
                Paths.get("im2.jpg")
        ), project.imageFiles);
    }

    private void assertListEquals(List<Path> expected, List<Path> actual) {
        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < actual.size(); i++) {
            assertEquals("at index " + i, expected.get(i), actual.get(i));
        }
    }
}
