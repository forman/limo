package com.forman.limo.data;

import com.forman.limo.AppInfo;
import com.sun.javafx.collections.ObservableListWrapper;
import com.sun.javafx.collections.ObservableMapWrapper;
import javafx.beans.InvalidationListener;
import javafx.beans.property.*;
import javafx.collections.FXCollections;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static com.forman.limo.data.Defaults.*;

public class Project {

    private static final int VERSION = 1;
    private static final String SETTINGS_PROPERTIES = "settings.properties";
    private static final String FILELIST_TXT = "filelist.txt";

    public final StringProperty projectFile;

    public final StringProperty targetDirName;
    public final StringProperty targetFileNamePattern;
    public final IntegerProperty targetFileStartIndex;
    public final BooleanProperty relativizePaths;
    public final StringProperty imageFilenameExt;
    public final BooleanProperty scanRecursive;
    public final DoubleProperty imageDisplaySizeRatio;
    public final ListProperty<Path> selectedImageFiles;
    public final ListProperty<Path> imageFiles;
    public final MapProperty<Path, ImageItem> imageItems;
    private final List<Path> fileListImpl = new ArrayList<>();
    private final Map<Path, ImageItem> imageItemsImpl = new HashMap<>();
    public final BooleanProperty modified;

    private final ArrayList<InvalidationListener> invalidationListeners = new ArrayList<>();

    public Project() {
        projectFile = new SimpleStringProperty();
        targetDirName = new SimpleStringProperty();
        targetFileNamePattern = new SimpleStringProperty();
        targetFileStartIndex = new SimpleIntegerProperty();
        relativizePaths = new SimpleBooleanProperty();
        imageFilenameExt = new SimpleStringProperty();
        scanRecursive = new SimpleBooleanProperty();
        imageDisplaySizeRatio = new SimpleDoubleProperty();
        selectedImageFiles = new SimpleListProperty<>(FXCollections.observableArrayList());
        imageFiles = new SimpleListProperty<>(new ObservableListWrapper<>(fileListImpl));
        imageItems = new SimpleMapProperty<>(new ObservableMapWrapper<>(imageItemsImpl));
        modified = new SimpleBooleanProperty();

        InvalidationListener invalidationListener = observable -> modified.set(true);
        projectFile.addListener(invalidationListener);
        targetDirName.addListener(invalidationListener);
        targetFileNamePattern.addListener(invalidationListener);
        targetFileStartIndex.addListener(invalidationListener);
        relativizePaths.addListener(invalidationListener);
        imageFilenameExt.addListener(invalidationListener);
        scanRecursive.addListener(invalidationListener);
        imageDisplaySizeRatio.addListener(invalidationListener);
        //selectedImageFiles.addListener(invalidationListener);
        imageFiles.addListener(invalidationListener);
        //imageItems.addListener(invalidationListener);
        init();
    }

    public void init() {
        projectFile.set(null);
        setDefaultSettings();
        imageItemsImpl.clear();
        imageFiles.clear();
        modified.set(false);
    }

    public void setDefaultSettings() {
        targetDirName.set(TARGET_DIR_NAME);
        targetFileNamePattern.set(TARGET_FILE_NAME_PATTERN);
        targetFileStartIndex.set(TARGET_FILE_START_INDEX);
        relativizePaths.set(RELATIVIZE_PATHS);
        imageFilenameExt.set(IMAGE_FILE_NAME_EXT);
        scanRecursive.set(SCAN_RECURSIVE);
        imageDisplaySizeRatio.set(IMAGE_DISPLAY_SIZE_RATIO);
        selectedImageFiles.clear();
    }

    public void setSettings(Preferences preferences) {
        targetDirName.set(preferences.get("targetDirName", targetDirName.get()));
        targetFileNamePattern.set(preferences.get("targetFileNamePattern", targetFileNamePattern.get()));
        targetFileStartIndex.set(preferences.getInt("targetFileStartIndex", targetFileStartIndex.get()));
        relativizePaths.set(preferences.getBoolean("relativizePaths", relativizePaths.get()));
        imageFilenameExt.set(preferences.get("imageFilenameExt", imageFilenameExt.get()));
        scanRecursive.set(preferences.getBoolean("scanRecursive", scanRecursive.get()));
        imageDisplaySizeRatio.set(preferences.getDouble("imageDisplaySizeRatio", imageDisplaySizeRatio.get()));
    }

    public void getSettings(Preferences preferences) {
        preferences.put("targetDirName", targetDirName.get());
        preferences.put("targetFileNamePattern", targetFileNamePattern.get());
        preferences.putInt("targetFileStartIndex", targetFileStartIndex.get());
        preferences.putBoolean("relativizePaths", relativizePaths.get());
        preferences.put("imageFilenameExt", imageFilenameExt.get());
        preferences.putBoolean("scanRecursive", scanRecursive.get());
        preferences.putDouble("imageDisplaySizeRatio", imageDisplaySizeRatio.get());
    }

    public void open(String projectFile) throws IOException {
        Path normalizedFile = Paths.get(projectFile).toAbsolutePath().normalize();
        Path projectDir = normalizedFile.getParent();
        File file = normalizedFile.toFile();
        try (ZipFile zipFile = new ZipFile(file)) {
            loadSettings(zipFile);
            loadFileList(zipFile, projectDir);
        }
        this.projectFile.set(normalizedFile.toString());
        modified.set(false);
    }

    public void save() throws IOException {
        saveAs(projectFile.get());
    }

    public void saveAs(String projectFile) throws IOException {
        Path normalizedFile = Paths.get(projectFile).toAbsolutePath().normalize();
        Path projectDir = normalizedFile.getParent();
        try (ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(normalizedFile.toFile()))) {
            storeSettings(zout);
            storeFileList(zout, projectDir);
        }
        this.projectFile.set(projectFile);
        modified.set(false);
    }

    private void loadSettings(ZipFile zipFile) throws IOException {
        ZipEntry entry = zipFile.getEntry(SETTINGS_PROPERTIES);
        Properties properties = new Properties();
        try (InputStream inputStream = zipFile.getInputStream(entry)) {
            properties.load(inputStream);
        }
        setSettings(new Settings(properties));
    }

    private void storeSettings(ZipOutputStream zout) throws IOException {
        ZipEntry entry = new ZipEntry(SETTINGS_PROPERTIES);
        zout.putNextEntry(entry);
        Properties properties = new Properties();
        properties.put("version", VERSION + "");
        getSettings(new Settings(properties));
        String comments = AppInfo.NAME + " " + AppInfo.VERSION + " album file";
        properties.store(zout, comments);
        zout.closeEntry();
    }

    private void loadFileList(ZipFile zipFile, Path projectDir) throws IOException {
        ZipEntry entry = zipFile.getEntry(FILELIST_TXT);
        ArrayList<Path> imageFiles = new ArrayList<>();
        HashMap<Path, ImageItem> imageItems = new HashMap<>();
        try (LineNumberReader reader = new LineNumberReader(new InputStreamReader(zipFile.getInputStream(entry)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    Path imageFile = projectDir.resolve(Paths.get(line));
                    // System.out.println("loadedImageFile = " + loadedImageFile);
                    imageFiles.add(imageFile);
                    imageItems.put(imageFile, ImageItem.newEmpty(imageFile));
                }
            }
        }
        this.imageItemsImpl.clear();
        this.imageItemsImpl.putAll(imageItems);
        this.imageFiles.clear();
        this.imageFiles.addAll(imageFiles);
    }

    private void storeFileList(ZipOutputStream zout, Path projectDir) throws IOException {
        ZipEntry entry = new ZipEntry(FILELIST_TXT);
        zout.putNextEntry(entry);
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(zout));
        for (Path imageFile : imageFiles) {
            Path storedImageFile = relativizePaths.get() ? projectDir.relativize(imageFile) : imageFile;
            // System.out.println("storedImageFile = " + storedImageFile);
            writer.println(storedImageFile);
        }
        writer.flush();
        zout.closeEntry();
    }

    public void addFile(String file) {
        addFile(Paths.get(file));
    }

    public void addFile(Path file) {
        Path path = file.toAbsolutePath().normalize();
        if (!imageItemsImpl.containsKey(path)) {
            imageItemsImpl.put(path, ImageItem.newEmpty(path));
            imageFiles.add(path); // notifies UI
        }
    }

    public void addFiles(List<Path> files) {
        for (Path file : files) {
            addFile(file);
        }
    }

    public List<Path> filterFiles(List<File> files) {
        Set<String> extSet = getFilenameExtensions(imageFilenameExt.get());
        boolean scanRecursive = this.scanRecursive.get();
        ArrayList<Path> filteredFiles = new ArrayList<>();
        collectFilteredFiles(files, extSet, scanRecursive, filteredFiles);
        return filteredFiles;
    }

    public static Set<String> getFilenameExtensions(String s) {
        if (s.trim().isEmpty()) {
            return new HashSet<>();
        }
        String[] extensions = s.split(",");
        return new HashSet<>(Arrays.stream(extensions).map(String::trim).map(String::toLowerCase).collect(Collectors.toList()));
    }

    private void collectFilteredFiles(List<File> files, Set<String> extSet, boolean scanRecursive, List<Path> filteredFiles) {
        for (File file : files) {
            if (file.isFile()) {
                String name = file.getName();
                int pos = name.lastIndexOf('.');
                if (pos > 0) {
                    String ext = name.substring(pos + 1).toLowerCase();
                    if (extSet.contains(ext) && file.isFile()) {
                        filteredFiles.add(file.toPath());
                    }
                }
            } else if (file.isDirectory() && scanRecursive) {
                File[] dirFiles = file.listFiles();
                if (dirFiles != null) {
                    collectFilteredFiles(Arrays.asList(dirFiles), extSet, true, filteredFiles);
                }
            }
        }
    }

    public int getSelectedImageIndex() {
        if (selectedImageFiles.size() > 0) {
            return fileListImpl.indexOf(selectedImageFiles.get(0));
        }
        return -1;
    }

    public ImageItem getImageItem(Path file) {
        return imageItemsImpl.get(file);
    }

    public Map<Path, Integer> getImageFileIndexes() {
        Map<Path, Integer> indexes = new HashMap<>();
        for (int i = 0; i < imageFiles.size(); i++) {
            Path file = imageFiles.get(i);
            indexes.put(file, i);
        }
        return indexes;
    }

    public List<Integer> getSelectedImageFileIndexes() {
        return getImageFileIndexes(selectedImageFiles);
    }

    public List<Integer> getImageFileIndexes(List<Path> files) {
        if (files.isEmpty()) {
            return Collections.emptyList();
        }
        if (files.size() == 1) {
            return Collections.singletonList(imageFiles.indexOf(files.get(0)));
        }
        Map<Path, Integer> pathIndexes = getImageFileIndexes();
        ArrayList<Integer> indexes = new ArrayList<>();
        for (Path file : files) {
            Integer index = pathIndexes.get(file);
            if (index != null) {
                indexes.add(index);
            }
        }
        Collections.sort(indexes);
        return indexes;
    }

    class Settings extends AbstractPreferences {
        final Properties properties;

        Settings(Properties properties) {
            super(null, "");
            this.properties = properties;
        }

        @Override
        protected void putSpi(String key, String value) {
            properties.setProperty(key, value);
        }

        @Override
        protected String getSpi(String key) {
            return properties.getProperty(key);
        }

        @Override
        protected void removeSpi(String key) {
            properties.remove(key);
        }

        @Override
        protected void removeNodeSpi() throws BackingStoreException {
        }

        @Override
        protected String[] keysSpi() throws BackingStoreException {
            return (String[]) properties.keySet().toArray();
        }

        @Override
        protected String[] childrenNamesSpi() throws BackingStoreException {
            return new String[0];
        }

        @Override
        protected AbstractPreferences childSpi(String name) {
            return null;
        }

        @Override
        protected void syncSpi() {
        }

        @Override
        protected void flushSpi() {
        }
    }
}
