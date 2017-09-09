package com.forman.limo.data;

import javafx.scene.image.Image;

import java.nio.file.Path;
import java.util.Map;

public class ImageItem {

    public final Path file;
    public final Image image;
    public final Map<String, Map<String, String>> metadata;

    public static ImageItem newEmpty(Path file) {
        return new ImageItem(file, null, null);
    }

    public ImageItem(Path file, Image image, Map<String, Map<String, String>> metadata) {
        this.file = file;
        this.image = image;
        this.metadata = metadata;
    }
}
