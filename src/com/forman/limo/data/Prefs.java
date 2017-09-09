package com.forman.limo.data;

import com.sun.javafx.geom.Rectangle;
import javafx.beans.property.*;

import java.util.Scanner;
import java.util.prefs.Preferences;

import static com.forman.limo.data.Defaults.*;

public class Prefs {

    public final StringProperty mainWindowBounds;
    public final StringProperty metadataWindowBounds;
    public final BooleanProperty metadataWindowVisible;
    public final StringProperty lastProjectFile;
    public final BooleanProperty openLastProject;
    public final IntegerProperty minImageDisplaySize;
    public final IntegerProperty maxImageDisplaySize;

    public Prefs() {
        mainWindowBounds = new SimpleStringProperty();
        metadataWindowBounds = new SimpleStringProperty();
        metadataWindowVisible = new SimpleBooleanProperty();
        lastProjectFile = new SimpleStringProperty();
        openLastProject = new SimpleBooleanProperty();
        minImageDisplaySize = new SimpleIntegerProperty();
        maxImageDisplaySize = new SimpleIntegerProperty();
        init();
    }

    public void init() {
        setDefaultSettings();
    }

    void setDefaultSettings() {
        mainWindowBounds.set(null);
        metadataWindowBounds.set(null);
        metadataWindowVisible.set(false);
        lastProjectFile.set(null);
        openLastProject.set(OPEN_LAST_PROJECT);
        minImageDisplaySize.set(MIN_IMAGE_DISPLAY_SIZE);
        maxImageDisplaySize.set(MAX_IMAGE_DISPLAY_SIZE);
    }

    public void setSettings(Preferences preferences) {
        mainWindowBounds.set(preferences.get("mainWindowBounds", mainWindowBounds.get()));
        metadataWindowBounds.set(preferences.get("metadataWindowBounds", metadataWindowBounds.get()));
        metadataWindowVisible.set(preferences.getBoolean("metadataWindowVisible", metadataWindowVisible.get()));
        lastProjectFile.set(preferences.get("lastProjectFile", lastProjectFile.get()));
        openLastProject.set(preferences.getBoolean("openLastProject", openLastProject.get()));
        minImageDisplaySize.set(preferences.getInt("minImageDisplaySize", minImageDisplaySize.get()));
        maxImageDisplaySize.set(preferences.getInt("maxImageDisplaySize", maxImageDisplaySize.get()));
    }

    public void getSettings(Preferences preferences) {
        preferences.put("mainWindowBounds", mainWindowBounds.get() != null ? mainWindowBounds.get() : "");
        preferences.put("metadataWindowBounds", metadataWindowBounds.get() != null ? metadataWindowBounds.get() : "");
        preferences.putBoolean("metadataWindowVisible", metadataWindowVisible.get());
        preferences.put("lastProjectFile", lastProjectFile.get() != null ? lastProjectFile.get() : "");
        preferences.putBoolean("openLastProject", openLastProject.get());
        preferences.putInt("minImageDisplaySize", minImageDisplaySize.get());
        preferences.putInt("maxImageDisplaySize", maxImageDisplaySize.get());
    }

    public Rectangle getMainWindowBounds() {
        return getWindowBounds(mainWindowBounds);
    }

    public void setMainWindowBounds(Rectangle rectangle) {
        setWindowBounds(mainWindowBounds, rectangle);
    }

    public Rectangle getMetadataWindowBounds() {
        return getWindowBounds(metadataWindowBounds);
    }

    public void setMetadataWindowBounds(Rectangle rectangle) {
        setWindowBounds(metadataWindowBounds, rectangle);
    }

    public static Rectangle getWindowBounds(StringProperty property) {
        String s = property.get();
        if (s != null && !s.isEmpty()) {
            Scanner scanner = new Scanner(s);
            int windowX = scanner.nextInt();
            int windowY = scanner.nextInt();
            int windowWidth = scanner.nextInt();
            int windowHeight = scanner.nextInt();
            return new Rectangle(windowX, windowY, windowWidth, windowHeight);
        }
        return null;
    }

    public static void setWindowBounds(StringProperty property, Rectangle rectangle) {
        if (rectangle != null) {
            property.set(String.format("%d %d %d %d", rectangle.x, rectangle.y, rectangle.width, rectangle.height));
        } else {
            property.set("");
        }
    }
}
