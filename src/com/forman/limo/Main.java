package com.forman.limo;

import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.forman.limo.actions.*;
import com.forman.limo.data.ImageItem;
import com.forman.limo.data.Prefs;
import com.forman.limo.data.Project;
import com.forman.limo.dialogs.*;
import com.sun.javafx.geom.Rectangle;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ListProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.IntStream;

public class Main extends Application {
    private static final int DEFAULT_INSET_SIZE = 4;

    final Project project = new Project();
    final Prefs prefs = new Prefs();

    private Stage mainWindow;

    private FlowPane imageFlowPane;
    private Label infoLabel;

    private HashMap<String, ImageTile> imageTilesMap = new HashMap<>();
    private ScrollPane imageScrollPanel;

    private BorderPane emptyPanel;
    private BorderPane imagePanel;
    private BorderPane rootPanel;

    ExecutorService executorService = Executors.newCachedThreadPool();

    private UndoList undoList = new UndoList();
    private MetadataWindow metadataWindow;
    private ImageTileListener imageTileListener;

    Preferences getPreferences() {
        return Preferences.userNodeForPackage(Main.class).node("v" + AppInfo.VERSION);
    }

    @Override
    public void init() {
        Preferences preferences = getPreferences();
        if (System.getProperty("limo.clearPrefs", "false").equalsIgnoreCase("true")) {
            try {
                preferences.clear();
                preferences.sync();
            } catch (BackingStoreException e) {
                // ok
            }
        }
        prefs.setSettings(preferences);
        project.setSettings(preferences);
    }

    @Override
    public void stop() {
        Preferences preferences = getPreferences();
        prefs.getSettings(preferences);
        project.getSettings(preferences);
        try {
            preferences.flush();
        } catch (BackingStoreException e) {
            // ok
        }
    }

    @Override
    public void start(Stage mainWindow) {
        this.mainWindow = mainWindow;

        this.mainWindow.setOnCloseRequest(event -> {
            if (!exit()) {
                event.consume();
            }
        });

        infoLabel = new Label();
        imageFlowPane = new FlowPane();

        Slider imageDisplaySizeSlider = new Slider();
        imageDisplaySizeSlider.setMin(0.0);
        imageDisplaySizeSlider.setMax(1.0);
        imageDisplaySizeSlider.valueProperty().bindBidirectional(project.imageDisplaySizeRatio);

        imageFlowPane.setHgap(1);
        imageFlowPane.setVgap(1);
        imageScrollPanel = new ScrollPane(imageFlowPane);
        imageScrollPanel.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        imageScrollPanel.setPannable(true);
        imageScrollPanel.setFitToWidth(true);
        imageScrollPanel.setFitToHeight(false);
        imageScrollPanel.setStyle("-fx-background-color:transparent;");

        BorderPane imageActionPanel = new BorderPane();
        imageActionPanel.setCenter(infoLabel);
        imageActionPanel.setRight(imageDisplaySizeSlider);
        BorderPane.setAlignment(infoLabel, Pos.CENTER_LEFT);

        imagePanel = new BorderPane();
        imagePanel.setPadding(new Insets(DEFAULT_INSET_SIZE));
        imagePanel.setCenter(imageScrollPanel);
        imagePanel.setBottom(imageActionPanel);
        BorderPane.setMargin(imageScrollPanel, new Insets(DEFAULT_INSET_SIZE, 0, DEFAULT_INSET_SIZE, 0));

        Text emptyPanelText = new Text(AppInfo.RES.getString("drop.image.files.here.n") + "\n\n\u20DD");
        emptyPanelText.setFont(new Font(28));
        emptyPanelText.setFill(Color.GRAY);
        emptyPanelText.setTextAlignment(TextAlignment.CENTER);
        emptyPanel = new BorderPane(emptyPanelText);
        emptyPanel.setPadding(new Insets(DEFAULT_INSET_SIZE));

        BooleanBinding hasNoProject = Bindings.createBooleanBinding(() -> project.projectFile.get() == null, project.projectFile);
        BooleanBinding hasNoImageFiles = Bindings.createBooleanBinding(project.imageFiles::isEmpty, project.imageFiles);
        BooleanBinding hasNoSelectedImageFiles = Bindings.createBooleanBinding(project.selectedImageFiles::isEmpty, project.selectedImageFiles);
        BooleanBinding isNotModified = Bindings.createBooleanBinding(() -> !project.modified.get(), project.modified);

        Menu fileMenu = getFileMenu(hasNoProject, hasNoImageFiles, isNotModified);
        Menu editMenu = getEditMenu(hasNoImageFiles, hasNoSelectedImageFiles);
        Menu toolsMenu = getToolsMenu(hasNoImageFiles);
        Menu helpMenu = getHelpMenu();

        MenuBar menuBar = new MenuBar();
        menuBar.getMenus().addAll(fileMenu, editMenu, toolsMenu, helpMenu);

        rootPanel = new BorderPane();
        rootPanel.setTop(menuBar);
        //rootPanel.setCenter(imagePanel);
        rootPanel.setCenter(emptyPanel);

        Scene scene = new Scene(this.rootPanel, 480, 520);
        //scene.addEventHandler(KeyEvent.KEY_PRESSED, this::handleKeyPressed);
        imagePanel.addEventHandler(KeyEvent.KEY_PRESSED, this::handleKeyPressed);
        scene.setOnDragOver(event -> {
            if (event.getGestureSource() != scene && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });
        scene.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                List<Path> files = project.filterFiles(db.getFiles());
                runAction(new AddAction(project, files));
                loadImageFiles(files);
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });

        updateTitle();
        updateEnabledState();
        updateInfoLabel();

        // Listener bindings
        project.imageFiles.addListener((ListChangeListener<Path>) change -> Platform.runLater(() -> handleImageFilesChange(change)));
        project.imageItems.addListener((MapChangeListener<Path, ImageItem>) change -> Platform.runLater(() -> handleImageItemsChange(change)));
        project.selectedImageFiles.addListener((ListChangeListener<Path>) change -> Platform.runLater(() -> handleSelectedImageFilesChange(change)));
        ChangeListener<Number> imageDisplaySizeListener = (ObservableValue<? extends Number> ov, Number oldVal, Number newVal) -> handleImageDisplaySizeChange();
        project.imageDisplaySizeRatio.addListener(imageDisplaySizeListener);
        prefs.minImageDisplaySize.addListener(imageDisplaySizeListener);
        prefs.maxImageDisplaySize.addListener(imageDisplaySizeListener);
        project.projectFile.addListener((observable, oldValue, newValue) -> handleProjectFileChange());

        project.selectedImageFiles.addListener((InvalidationListener) observable -> {
            if (metadataWindow != null) {
                if (!project.selectedImageFiles.isEmpty()) {
                    Path file = project.selectedImageFiles.get(0);
                    ImageItem imageItem = project.imageItems.get(file);
                    if (imageItem.metadata != null) {
                        metadataWindow.setItems(imageItem.metadata);
                        return;
                    }
                }
                metadataWindow.clearItems();
            }
        });

        project.modified.addListener(observable -> updateTitle());


        mainWindow.getIcons().addAll(IntStream
                .of(16, 24, 32, 48, 64, 128, 210, 256)
                .mapToObj(value -> new Image(String.format("com/forman/limo/resources/limo-%d.png", value)))
                .toArray(Image[]::new));
        mainWindow.setScene(scene);
        restoreWindowBounds(this.mainWindow, prefs.getMainWindowBounds());
        mainWindow.show();

        if (prefs.metadataWindowVisible.get()) {
            showExifWindow();
        }

        project.init();

        String lastProjectFile = prefs.lastProjectFile.get();
        //System.out.println("lastProjectFile = " + lastProjectFile);
        if (lastProjectFile != null && prefs.openLastProject.get() && new File(lastProjectFile).isFile()) {
            openProject(new File(lastProjectFile));
        }
    }

    private Menu getFileMenu(BooleanBinding hasNoProject, BooleanBinding hasNoImageFiles, BooleanBinding isNotModified) {
        MenuItem openItem = new MenuItem(AppInfo.RES.getString("open"));
        openItem.setAccelerator(KeyCombination.keyCombination("Ctrl+O"));
        openItem.setOnAction(event -> openProject());

        MenuItem closeItem = new MenuItem(AppInfo.RES.getString("close"));
        closeItem.disableProperty().bind(hasNoProject);
        closeItem.setOnAction(event -> closeProject());

        MenuItem saveItem = new MenuItem(AppInfo.RES.getString("save"));
        saveItem.disableProperty().bind(hasNoImageFiles.or(isNotModified).or(hasNoProject));
        saveItem.setAccelerator(KeyCombination.keyCombination("Ctrl+S"));
        saveItem.setOnAction(event -> saveProject());

        MenuItem saveAsItem = new MenuItem(AppInfo.RES.getString("save.as"));
        //saveAsItem.disableProperty().bind(hasNoImageFiles);
        saveAsItem.setOnAction(event -> saveProjectAs());

        MenuItem settingsItem = new MenuItem(AppInfo.RES.getString("settings"));
        settingsItem.setOnAction(event -> SettingsDialog.show(mainWindow, project, prefs));

        MenuItem exitItem = new MenuItem(AppInfo.RES.getString("exit"));
        exitItem.setOnAction(event -> exit());

        Menu fileMenu = new Menu(AppInfo.RES.getString("file"));
        fileMenu.getItems().addAll(
                openItem,
                closeItem,
                new SeparatorMenuItem(),
                saveItem,
                saveAsItem,
                new SeparatorMenuItem(),
                settingsItem,
                new SeparatorMenuItem(),
                exitItem
        );
        return fileMenu;
    }

    private Menu getEditMenu(BooleanBinding hasNoImageFiles, BooleanBinding hasNoSelectedImageFiles) {
        BooleanBinding cannotUndo = Bindings.createBooleanBinding(() -> !undoList.canUndo(), undoList);
        BooleanBinding cannotRedo = Bindings.createBooleanBinding(() -> !undoList.canRedo(), undoList);

        MenuItem undoItem = new MenuItem(AppInfo.RES.getString("undo"));
        undoItem.disableProperty().bind(cannotUndo);
        undoItem.setAccelerator(KeyCombination.keyCombination("Ctrl+Z"));
        undoItem.setOnAction(event -> {
            try {
                undoList.undo();
            } catch (Exception e) {
                ExceptionDialog.show(mainWindow, AppInfo.RES.getString("failed.to.undo.last.actions"), e);
            }
        });

        MenuItem redoItem = new MenuItem(AppInfo.RES.getString("redo"));
        redoItem.disableProperty().bind(cannotRedo);
        redoItem.setAccelerator(KeyCombination.keyCombination("Shift+Ctrl+Z"));
        redoItem.setOnAction(event -> {
            try {
                undoList.redo();
            } catch (Exception e) {
                ExceptionDialog.show(mainWindow, AppInfo.RES.getString("failed.to.redo.last.actions"), e);
            }
        });

        MenuItem deleteItem = new MenuItem(AppInfo.RES.getString("delete"));
        deleteItem.disableProperty().bind(hasNoSelectedImageFiles);
        deleteItem.setAccelerator(new KeyCodeCombination(KeyCode.DELETE));
        deleteItem.setOnAction(event -> runAction(new DeleteAction(project, project.selectedImageFiles)));

        MenuItem selectAllItem = new MenuItem(AppInfo.RES.getString("select.all"));
        selectAllItem.disableProperty().bind(hasNoImageFiles);
        selectAllItem.setAccelerator(KeyCombination.keyCombination("Ctrl+A"));
        selectAllItem.setOnAction(event -> project.selectedImageFiles.setAll(project.imageFiles));

        Menu editMenu = new Menu(AppInfo.RES.getString("edit"));
        editMenu.getItems().addAll(
                undoItem,
                redoItem,
                new SeparatorMenuItem(),
                deleteItem,
                new SeparatorMenuItem(),
                selectAllItem
        );
        return editMenu;
    }

    private Menu getToolsMenu(BooleanBinding hasNoImageFiles) {
        MenuItem copyAndRenameItem = new MenuItem(AppInfo.RES.getString("copy.and.rename") + "...");
        copyAndRenameItem.setAccelerator(KeyCombination.keyCombination("Ctrl+R"));
        copyAndRenameItem.setOnAction(event -> CopyAndRenameDialog.show(mainWindow, project));
        copyAndRenameItem.disableProperty().bind(hasNoImageFiles);

        MenuItem sortByFilenameItem = new MenuItem(AppInfo.RES.getString("sort.by.filename"));
        sortByFilenameItem.disableProperty().bind(hasNoImageFiles);
        sortByFilenameItem.setOnAction(event -> runAction(new SortyByAction(project, SortyByAction.FILENAME)));

        MenuItem sortByDateTimeItem = new MenuItem(AppInfo.RES.getString("sort.by.date.time"));
        sortByDateTimeItem.disableProperty().bind(hasNoImageFiles);
        sortByDateTimeItem.setOnAction(event -> runAction(new SortyByAction(project, SortyByAction.DATE_TIME_ORIGINAL)));

        MenuItem showExifItem = new MenuItem(AppInfo.RES.getString("show.exif.metadata"));
        // showExifItem.disableProperty().bind(hasNoImageFiles);
        showExifItem.setOnAction(event -> showExifWindow());

        Menu toolsMenu = new Menu(AppInfo.RES.getString("tools"));
        toolsMenu.getItems().addAll(
                copyAndRenameItem,
                new SeparatorMenuItem(),
                sortByFilenameItem,
                sortByDateTimeItem,
                new SeparatorMenuItem(),
                showExifItem
        );
        return toolsMenu;
    }

    private Menu getHelpMenu() {
        MenuItem keymapItem = new MenuItem(AppInfo.RES.getString("keymap.reference") + "...");
        keymapItem.setOnAction(event -> KeymapReferenceDialog.show(mainWindow));

        MenuItem aboutItem = new MenuItem(AppInfo.RES.getString("about") + "...");
        aboutItem.setOnAction(event -> AboutDialog.show(mainWindow));

        Menu helpMenu = new Menu(AppInfo.RES.getString("help"));
        helpMenu.getItems().addAll(
                keymapItem,
                new SeparatorMenuItem(),
                aboutItem
        );
        return helpMenu;
    }

    private void showExifWindow() {
        if (metadataWindow == null) {
            metadataWindow = new MetadataWindow(mainWindow);
            restoreWindowBounds(metadataWindow, prefs.getMetadataWindowBounds());
        }
        metadataWindow.show();
    }

    private void restoreWindowBounds(Stage window, Rectangle exifWindowRectangle) {
        if (exifWindowRectangle != null) {
            window.setX(exifWindowRectangle.x);
            window.setY(exifWindowRectangle.y);
            window.setWidth(exifWindowRectangle.width);
            window.setHeight(exifWindowRectangle.height);
        } else {
            window.centerOnScreen();
        }
    }

    private Rectangle getWindowBounds(Stage window) {
        if (window == null) {
            return null;
        }
        return new Rectangle(
                (int) window.getX(),
                (int) window.getY(),
                (int) window.getWidth(),
                (int) window.getHeight());
    }

    private boolean exit() {
        if (!checkModified(AppInfo.RES.getString("exit"))) {
            return false;
        }

        prefs.setMainWindowBounds(getWindowBounds(mainWindow));
        prefs.setMetadataWindowBounds(getWindowBounds(metadataWindow));
        prefs.metadataWindowVisible.set(metadataWindow != null && metadataWindow.isShowing());
        prefs.lastProjectFile.set(project.projectFile.get());

        Platform.exit();
        return true;
    }

    private void runAction(UndoableAction action) {
        try {
            if (!action.call()) {
                return;
            }
        } catch (Exception e) {
            ExceptionDialog.show(mainWindow, MessageFormat.format(AppInfo.RES.getString("unable.to.perform.action.0"), action.getName()), e);
            return;
        }
        undoList.add(action);
    }

    private void loadImageFiles(List<Path> files) {
        executorService.submit(new ImageLoaderTask(
                files.toArray(new Path[0]),
                prefs.maxImageDisplaySize.doubleValue(),
                new ImageLoaderTask.Listener() {
                    @Override
                    public void onImageLoaded(Path file, Image image, Metadata metadata) {
                        project.imageItems.replace(file, new ImageItem(file, image, convertMetadata(metadata)));
                    }

                    @Override
                    public void onImageLoadFailed(Path file, Exception e) {
                        // Note, could be used to display error message in image tile
                    }
                }));
    }

    private static Map<String, Map<String, String>> convertMetadata(Metadata metadata) {
        HashMap<String, Map<String, String>> metadataDict = new HashMap<>();
        Iterable<Directory> directories = metadata.getDirectories();
        for (Directory directory : directories) {
            Map<String, String> catData = new HashMap<>();
            metadataDict.put(directory.getName(), catData);
            for (Tag tag : directory.getTags()) {
                String tagName = tag.getTagName();
                Object tagValue = directory.getObject(tag.getTagType());
                //System.out.printf("  %s = %s (%s)%n", tagName, tagValue, tagValue.getClass());
                String tagTextValue = "";
                if (tagValue != null && tagValue.getClass().isArray()) {
                    if (tagValue instanceof byte[]) {
                        tagTextValue = Arrays.toString((byte[]) tagValue);
                    } else if (tagValue instanceof char[]) {
                        tagTextValue = Arrays.toString((char[]) tagValue);
                    } else if (tagValue instanceof boolean[]) {
                        tagTextValue = Arrays.toString((boolean[]) tagValue);
                    } else if (tagValue instanceof short[]) {
                        tagTextValue = Arrays.toString((short[]) tagValue);
                    } else if (tagValue instanceof int[]) {
                        tagTextValue = Arrays.toString((int[]) tagValue);
                    } else if (tagValue instanceof long[]) {
                        tagTextValue = Arrays.toString((long[]) tagValue);
                    } else if (tagValue instanceof float[]) {
                        tagTextValue = Arrays.toString((float[]) tagValue);
                    } else if (tagValue instanceof double[]) {
                        tagTextValue = Arrays.toString((double[]) tagValue);
                    } else if (tagValue instanceof Object[]) {
                        tagTextValue = Arrays.deepToString((Object[]) tagValue);
                    }
                } else if (tagValue != null) {
                    tagTextValue = tagValue.toString();
                }
                catData.put(tagName, tagTextValue);
            }
        }
        return metadataDict;
    }

    private void handleImageDisplaySizeChange() {
        double fitWidth = computeImageFitWidth();
        for (ImageTile imageTile : this.imageTilesMap.values()) {
            imageTile.imageView.setFitWidth(fitWidth);
        }
    }

    private void handleSelectedImageFilesChange(ListChangeListener.Change<? extends Path> change) {
        while (change.next()) {
            if (change.wasRemoved()) {
                setImageFilesSelected(change.getRemoved(), false);
            }
            if (change.wasAdded()) {
                setImageFilesSelected(change.getAddedSubList(), true);
            }
        }

        updateInfoLabel();
    }

    private void setImageFilesSelected(List<? extends Path> imageFiles, boolean selected) {
        if (imageFiles != null) {
            for (Path imageFile : imageFiles) {
                ImageTile imageTile = imageTilesMap.get(imageFile.toString());
                if (imageTile != null) {
                    imageTile.setSelected(selected);
                }
            }
        }
    }

    private void handleProjectFileChange() {
        updateTitle();
        updateEnabledState();
    }

    private void openProject() {
        FileChooser fileChooser = createProjectFileChooser(AppInfo.RES.getString("open.project"));
        File projectFile = fileChooser.showOpenDialog(mainWindow);
        if (projectFile != null) {
            openProject(projectFile.getAbsoluteFile());
        }
    }

    private FileChooser createProjectFileChooser(String title) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(AppInfo.FILE_EXTENSION_FILTER);
        fileChooser.setSelectedExtensionFilter(AppInfo.FILE_EXTENSION_FILTER);
        File initialDir = null;
        if (project.projectFile.get() != null) {
            initialDir = new File(project.projectFile.get()).getParentFile();
        }
        if (initialDir == null && prefs.lastProjectFile.get() != null) {
            initialDir = new File(prefs.lastProjectFile.get()).getParentFile();
        }
        fileChooser.setInitialDirectory(initialDir);
        fileChooser.setTitle(AppInfo.getWindowTitle(title));
        return fileChooser;
    }

    private void openProject(File projectFile) {
        if (!checkModified(AppInfo.RES.getString("open.project"))) {
            return;
        }
        try {
            undoList.clear();
            project.init();
            project.open(projectFile.getPath());
            loadImageFiles(project.imageFiles);
        } catch (IOException e) {
            project.init();
            ErrorDialog.show(mainWindow, AppInfo.RES.getString("opening.project.failed"),
                    MessageFormat.format(AppInfo.RES.getString("an.error.occurred.while.opening.project.from.file.n.0.n.1"),
                            project.projectFile.get(),
                            e.getMessage()));
        }
    }

    private boolean checkModified(String title) {
        if (project.modified.get()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(AppInfo.getWindowTitle(title));
            alert.setHeaderText(AppInfo.RES.getString("project.has.been.modified"));
            alert.setContentText(AppInfo.RES.getString("save.changes.first"));
            ButtonType buttonTypeYes = new ButtonType(AppInfo.RES.getString("yes"), ButtonBar.ButtonData.YES);
            ButtonType buttonTypeNo = new ButtonType(AppInfo.RES.getString("no"), ButtonBar.ButtonData.NO);
            ButtonType buttonTypeCancel = new ButtonType(AppInfo.RES.getString("cancel"), ButtonBar.ButtonData.NO);
            alert.getButtonTypes().setAll(buttonTypeYes, buttonTypeNo, buttonTypeCancel);
            Optional<ButtonType> result = alert.showAndWait();
            if (!result.isPresent() || result.get() == buttonTypeCancel) {
                return false;
            }
            if (result.get() == buttonTypeYes && !saveProject()) {
                return false;
            }
        }
        return true;
    }

    private boolean saveProject() {
        if (project.projectFile.get() == null) {
            return saveProjectAs();
        }
        assert project.projectFile.get() != null;
        try {
            undoList.clear();
            project.save();
            return true;
        } catch (IOException e) {
            ErrorDialog.show(mainWindow, AppInfo.RES.getString("saving.project.failed"),
                    MessageFormat.format(AppInfo.RES.getString("an.error.occurred.while.saving.project.to.file.n.0.n.1"),
                            project.projectFile.get(),
                            e.getMessage()));
            return false;
        }
    }

    private boolean saveProjectAs() {
        FileChooser fileChooser = createProjectFileChooser("Save Project As");
        File projectFile = fileChooser.showSaveDialog(mainWindow);
        if (projectFile == null) {
            return false;
        }
        try {
            undoList.clear();
            project.saveAs(projectFile.getPath());
            return true;
        } catch (IOException e) {
            ErrorDialog.show(mainWindow, AppInfo.RES.getString("saving.project.failed"),
                    MessageFormat.format(AppInfo.RES.getString("an.error.occurred.while.saving.project.to.file.n.0.n.1"),
                            project.projectFile.get(),
                            e.getMessage()));
            return false;
        }
    }

    private void closeProject() {
        if (!checkModified("Close Project")) {
            return;
        }
        undoList.clear();
        project.init();
    }

    private void updateTitle() {
        String title = MessageFormat.format(AppInfo.RES.getString("title.base"), AppInfo.NAME, AppInfo.VERSION);
        String path = project.projectFile.get();
        if (path != null) {
            File file = new File(path);
            String modifiedText = project.modified.get() ? "*" : "";
            title = MessageFormat.format(AppInfo.RES.getString("title"), file.getName(), modifiedText, file.getParent(), title);
        }
        mainWindow.setTitle(title);
    }

    private void handleImageFilesChange(ListChangeListener.Change<? extends Path> change) {
        if (AppInfo.DEBUG)
            System.out.println("handleImageFilesChange: " + change);

        ObservableList<Node> imageTilePanes = imageFlowPane.getChildren();

        if (project.imageFiles.isEmpty()) {
            if (rootPanel.getCenter() != emptyPanel) {
                rootPanel.setCenter(emptyPanel);
            }

            imageTilesMap.clear();
            imageTilePanes.clear();

        } else {
            if (rootPanel.getCenter() != imagePanel) {
                rootPanel.setCenter(imagePanel);
            }

            // For all existing images
            for (int i = 0; i < project.imageFiles.size(); i++) {
                Path imageFile = project.imageFiles.get(i);
                if (i < imageTilePanes.size()) {
                    Node imageTilePane = imageTilePanes.get(i);
                    if (!imageFile.toString().equals(imageTilePane.getId())) {
                        imageTilePanes.remove(imageTilePane);
                        ImageTile imageTile = imageTilesMap.get(imageFile.toString());
                        if (imageTile != null) {
                            imageTilePanes.remove(imageTile.imageTilePane);
                            imageTilePanes.add(i, imageTile.imageTilePane);
                        } else {
                            ImageItem imageItem = project.getImageItem(imageFile);
                            addImageTile(imageItem, i);
                        }
                    }
                } else {
                    ImageItem imageItem = project.getImageItem(imageFile);
                    addImageTile(imageItem, i);
                }
            }
            // Remove remaining imageTilePanes
            ArrayList<Node> removedNodes = new ArrayList<>();
            for (int i = project.imageFiles.size(); i < imageTilePanes.size(); i++) {
                removedNodes.add(imageTilePanes.get(i));
            }
            for (Node removedNode : removedNodes) {
                imageTilesMap.remove(removedNode.getId());
                imageTilePanes.remove(removedNode);
            }
        }

        updateInfoLabel();
        ensureImageIsVisible(project.getSelectedImageIndex());
    }

    private void handleImageItemsChange(MapChangeListener.Change<? extends Path, ? extends ImageItem> change) {
        if (AppInfo.DEBUG)
            System.out.println("handleImageItemsChange: change = " + change);
        if (change.wasAdded() && change.wasRemoved()) {
            ImageItem imageItem = change.getValueAdded();
            assert imageItem != null;
            replaceImageTile(imageItem);
        } else if (change.wasAdded()) {
            ImageItem imageItem = change.getValueAdded();
            assert imageItem != null;
            addImageTile(imageItem, -1);
        } else if (change.wasRemoved()) {
            ImageItem imageItem = change.getValueRemoved();
            assert imageItem != null;
            ImageTile imageTile = imageTilesMap.remove(imageItem.file.toString());
            if (imageTile != null) {
                imageFlowPane.getChildren().remove(imageTile.imageTilePane);
            }
        }
    }

    private void addImageTile(ImageItem imageItem, int index) {
        assert imageItem != null;
        ImageTile oldImageTile = imageTilesMap.get(imageItem.file.toString());
        ImageTile newImageTile = createImageTile(imageItem, oldImageTile != null && oldImageTile.isSelected());
        imageTilesMap.put(newImageTile.imageTilePane.getId(), newImageTile);
        if (index == -1 && oldImageTile != null) {
            index = imageFlowPane.getChildren().indexOf(oldImageTile.imageTilePane);
        }
        if (index >= 0) {
            imageFlowPane.getChildren().add(index, newImageTile.imageTilePane);
        } else {
            imageFlowPane.getChildren().add(newImageTile.imageTilePane);
        }
    }

    private void replaceImageTile(ImageItem imageItem) {
        assert imageItem != null;
        ImageTile oldImageTile = imageTilesMap.get(imageItem.file.toString());
        assert oldImageTile != null;
        ImageTile newImageTile = createImageTile(imageItem, oldImageTile.isSelected());
        imageTilesMap.put(newImageTile.imageTilePane.getId(), newImageTile);
        int index = imageFlowPane.getChildren().indexOf(oldImageTile.imageTilePane);
        imageFlowPane.getChildren().set(index, newImageTile.imageTilePane);
    }

    private ImageTile createImageTile(ImageItem imageItem, boolean selected) {
        if (imageTileListener == null) {
            imageTileListener = new ImageTileListener();
        }
        return new ImageTile(imageItem,
                computeImageFitWidth(),
                selected,
                imageTileListener);
    }

    private void openExternal(Path file) {
        try {
            java.awt.Desktop.getDesktop().open(file.toFile());
        } catch (IOException e) {
            ErrorDialog.show(mainWindow, AppInfo.RES.getString("failed.to.open.file.externally"), e.getMessage());
        }
    }

    private void editExternal(Path file) {
        try {
            java.awt.Desktop.getDesktop().edit(file.toFile());
        } catch (IOException e) {
            ErrorDialog.show(mainWindow, AppInfo.RES.getString("failed.to.edit.file.externally"), e.getMessage());
        }
    }

    private void ensureImageIsVisible(int index) {
        if (index >= 0) {
            Path file = project.imageFiles.get(index);
            ImageTile imageTile = imageTilesMap.get(file.toString());
            if (imageTile != null) {
                ensureNodeIsVisible(imageScrollPanel, imageTile.imageTilePane);
            }
        }
    }

    private static void ensureNodeIsVisible(ScrollPane pane, Node node) {

        Bounds paneBounds = pane.localToScene(pane.getBoundsInLocal());
        //System.out.println("paneBounds = " + paneBounds);
        Bounds nodeBounds = node.localToScene(node.getBoundsInLocal());
        //System.out.println("nodeBounds = " + nodeBounds);

        if (paneBounds.intersects(nodeBounds)) {
            return;
        }

        Bounds parentBounds = pane.getContent().getBoundsInLocal();
        double width = parentBounds.getWidth();
        double height = parentBounds.getHeight();

        Bounds nodeBoundsInParent = node.getBoundsInParent();
        double x = nodeBoundsInParent.getMaxX();
        double y = nodeBoundsInParent.getMaxY();

        // scrolling values range from 0 to 1
        pane.setHvalue(x / width);
        pane.setVvalue(y / height);
    }

    private void handleKeyPressed(KeyEvent key) {
        if (key.getCode() == KeyCode.PLUS || key.getCode() == KeyCode.UP) {
            List<Integer> indexes = project.getSelectedImageFileIndexes();
            if (indexes.isEmpty() || indexes.get(0) == 0) {
                return;
            }
            runAction(new MoveUpAction(project, project.selectedImageFiles, key.isControlDown()));
            key.consume();
        } else if (key.getCode() == KeyCode.MINUS || key.getCode() == KeyCode.DOWN) {
            List<Integer> indexes = project.getSelectedImageFileIndexes();
            if (indexes.isEmpty() || indexes.get(indexes.size() - 1) == project.imageFiles.size() - 1) {
                return;
            }
            runAction(new MoveDownAction(project, project.selectedImageFiles, key.isControlDown()));
            key.consume();
        }
    }

    private void updateEnabledState() {
    }

    private void updateInfoLabel() {
        int imageCount = project.imageFiles.size();
        int selectedImageCount = project.selectedImageFiles.size();
        if (imageCount == 0) {
            infoLabel.setText(AppInfo.RES.getString("status.no.images"));
        } else if (imageCount == 1) {
            infoLabel.setText(MessageFormat.format(AppInfo.RES.getString("status.one.image"), selectedImageCount));
        } else {
            infoLabel.setText(MessageFormat.format(AppInfo.RES.getString("status.n.images"), imageCount, selectedImageCount));
        }
    }


    private double computeImageFitWidth() {
        int min = prefs.minImageDisplaySize.get();
        int max = prefs.maxImageDisplaySize.get();
        double ratio = project.imageDisplaySizeRatio.get();
        return min + ratio * (max - min);
    }


    public static void main(String[] args) {
        launch(args);
    }

    class ImageTileListener implements ImageTile.Listener {
        @Override
        public void onImageTileMousePressed(MouseEvent event, ImageTile imageTile) {
            Path clickedFile = imageTile.imageItem.file;

            if (event.getClickCount() == 2) {
                openExternal(clickedFile);
                return;
            }

            ListProperty<Path> selectedFiles = project.selectedImageFiles;
            if (selectedFiles.isEmpty()) {
                selectedFiles.add(clickedFile);
            } else if (event.isControlDown()) {
                if (selectedFiles.contains(clickedFile)) {
                    selectedFiles.remove(clickedFile);
                } else {
                    selectedFiles.add(clickedFile);
                }
            } else if (event.isShiftDown()) {
                Map<Path, Integer> indexes = project.getImageFileIndexes();

                int iMin = Integer.MAX_VALUE;
                int iMax = Integer.MIN_VALUE;
                for (Path file : selectedFiles) {
                    Integer i = indexes.get(file);
                    iMin = Math.min(iMin, i);
                    iMax = Math.max(iMax, i);
                }
                int i0 = indexes.get(clickedFile);
                ArrayList<Path> newSelectedFiles = new ArrayList<>();
                int i1, i2;
                if (i0 <= iMin) {
                    i1 = i0;
                    i2 = iMin;
                } else if (i0 >= iMax) {
                    i1 = iMax;
                    i2 = i0;
                } else {
                    i1 = iMin;
                    i2 = i0;
                }
                for (int i = i1; i <= i2; i++) {
                    newSelectedFiles.add(project.imageFiles.get(i));
                }
                selectedFiles.setAll(newSelectedFiles);
            } else {
                selectedFiles.setAll(clickedFile);
            }
        }

        @Override
        public void onImageTileContextMenuRequested(ContextMenuEvent event, ImageTile imageTile) {
            final ContextMenu contextMenu = new ContextMenu();
            MenuItem item1 = new MenuItem(AppInfo.RES.getString("open.directory"));
            item1.setOnAction(actionEvent -> {
                contextMenu.hide();
                openExternal(imageTile.imageItem.file.getParent());
            });
            MenuItem item2 = new MenuItem(AppInfo.RES.getString("open.image"));
            item2.setOnAction(actionEvent -> {
                contextMenu.hide();
                openExternal(imageTile.imageItem.file);
            });
            MenuItem item3 = new MenuItem(AppInfo.RES.getString("edit.image"));
            item3.setOnAction(actionEvent -> {
                contextMenu.hide();
                editExternal(imageTile.imageItem.file);
            });
            contextMenu.getItems().addAll(item1, item2, item3);

            contextMenu.show(imageTile.imageTilePane, event.getScreenX(), event.getScreenY());

            event.consume();
        }
    }

}
