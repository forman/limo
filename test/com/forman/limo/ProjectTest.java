package com.forman.limo;

import com.forman.limo.data.Project;
import junit.framework.TestCase;

public class ProjectTest extends TestCase {
    public void testIO() throws Exception {
        Project project1 = new Project();

        project1.targetDirName.set("test/out");
        project1.targetFileNamePattern.set("PIC-%06d");
        project1.relativizePaths.set(true);

        project1.addFile("test/img-01.png");
        project1.addFile("test/img-02.png");
        project1.addFile("test/img-03.png");

        project1.saveAs("test/test.limo");

        Project project2 = new Project();
        project2.open("test/test.limo");

        assertEquals(project1.targetDirName.get(), project2.targetDirName.get());
        assertEquals(project1.targetFileNamePattern.get(), project2.targetFileNamePattern.get());
        assertEquals(project1.imageFiles.size(), project2.imageFiles.size());
        assertEquals(project1.imageFiles.get(0), project2.imageFiles.get(0));
    }
}
