package com.forman.limo;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import javafx.concurrent.Task;
import javafx.scene.image.Image;

import java.io.IOException;
import java.nio.file.Path;

import static com.forman.limo.AppInfo.DEBUG;

public class ImageLoaderTask extends Task<Void> {

    public interface Listener {
        void onImageLoaded(Path file, Image image, Metadata metadata);

        void onImageLoadFailed(Path file, Exception e);
    }

    private final Path[] imageFiles;
    private final double requestedSize;
    private final Listener listener;

    public ImageLoaderTask(Path[] imageFiles, double requestedSize, Listener listener) {
        this.imageFiles = imageFiles;
        this.requestedSize = requestedSize;
        this.listener = listener;
    }

    @Override
    protected Void call() throws Exception {
        for (Path imageFile : imageFiles) {
            loadImage(imageFile);
        }
        return null;
    }

    private void loadImage(Path imageFile) {
        try {
            if (DEBUG) {
                System.out.println("loading " + imageFile);
            }
            Metadata metadata = null;
            try {
                metadata = ImageMetadataReader.readMetadata(imageFile.toFile());
            } catch (Throwable e) {
                e.printStackTrace();
            }
            Image image = new Image(imageFile.toUri().toURL().toString(), requestedSize, requestedSize, true, true);
            listener.onImageLoaded(imageFile, image, metadata);
        } catch (IOException e) {
            e.printStackTrace();
            listener.onImageLoadFailed(imageFile, e);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

}
