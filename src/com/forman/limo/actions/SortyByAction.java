package com.forman.limo.actions;

import com.forman.limo.AppInfo;
import com.forman.limo.data.ImageItem;
import com.forman.limo.data.Project;

import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.*;

public class SortyByAction extends AbstractUndoableAction {
    private final Project project;
    private final ImageFileComparatorFactory comparatorFactory;
    private List<Path> oldFiles;

    public SortyByAction(Project project, ImageFileComparatorFactory comparatorFactory) {
        this.project = project;
        this.comparatorFactory = comparatorFactory;
    }

    @Override
    public String getName() {
        return MessageFormat.format(AppInfo.RES.getString("sort.by.0"), comparatorFactory.getTagName());
    }

    @Override
    public boolean callImpl() {
        oldFiles = new ArrayList<>(project.imageFiles);
        ArrayList<Path> imageFiles = new ArrayList<>(project.imageFiles);
        imageFiles.sort(comparatorFactory.create(project));
        boolean change = false;
        for (int i = 0; i < imageFiles.size(); i++) {
            if (!imageFiles.get(i).equals(project.imageFiles.get(i))) {
                change = true;
                break;
            }
        }
        if (change) {
            project.imageFiles.setAll(imageFiles);
        }
        return change;
    }

    @Override
    public void undoImpl() {
        project.imageFiles.setAll(oldFiles);
    }

    @Override
    public void redoImpl() {
        call();
    }

    public final static ImageFileComparatorFactory FILENAME = new ImageFileComparatorFactory("Filename");
    public final static ImageFileComparatorFactory DATE_TIME_ORIGINAL = new ImageFileComparatorFactory("Date/Time Original");
    public final static ImageFileComparatorFactory DATE_TIME_DIGITIZED = new ImageFileComparatorFactory("Date/Time Digitized");

    public static class ImageFileComparatorFactory {
        final String tagName;

        public ImageFileComparatorFactory(String tagName) {
            this.tagName = tagName;
        }

        public String getTagName() {
            return tagName;
        }

        public Comparator<Path> create(Project project) {
            return (file1, file2) -> {
                //Exif SubIFD
                if (!tagName.equalsIgnoreCase("Filename")) {
                    ImageItem imageItem1 = project.imageItems.get(file1);
                    ImageItem imageItem2 = project.imageItems.get(file2);
                    Map<String, Map<String, String>> metadata1 = imageItem1 != null ? imageItem1.metadata : null;
                    Map<String, Map<String, String>> metadata2 = imageItem2 != null ? imageItem2.metadata : null;
                    if (metadata1 != null && metadata2 != null) {
                        Map<String, String> exifData1 = metadata1.get("Exif SubIFD");
                        Map<String, String> exifData2 = metadata2.get("Exif SubIFD");
                        if (exifData1 != null && exifData2 != null) {
                            String value1 = exifData1.get(tagName);
                            String value2 = exifData2.get(tagName);
                            if (value1 != null && value2 != null) {
                                try {
                                    return value1.compareTo(value2);
                                } catch (ClassCastException ignored) {
                                }
                            }
                        }
                    }
                }
                return file1.getFileName().compareTo(file2.getFileName());
            };
        }
    }

}
