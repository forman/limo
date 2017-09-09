/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017 by Norman Fomferra (https://github.com/forman) and contributors.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */


package com.forman.limo.dialogs;

import com.forman.limo.AppInfo;
import com.forman.limo.CopyAndRenameImagesService;
import com.forman.limo.data.Project;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CopyAndRenameDialog {


    public static void show(Stage window, Project project) {

        assert project.projectFile.get() != null;
        Path projectFile = Paths.get(project.projectFile.get());
        Path projectDir = projectFile.getParent();

        TextField targetDirectoryTextField = new TextField();
        Button imageTargetDirectoryButton = new Button("...");
        imageTargetDirectoryButton.setOnAction(event -> showDirChooser(window, projectDir, targetDirectoryTextField));
        TextField targetFileNamePatternTextField = new TextField();
        targetDirectoryTextField.setPrefColumnCount(48);
        targetDirectoryTextField.setText(project.targetDirName.get());
        targetFileNamePatternTextField.setText(project.targetFileNamePattern.get());
        TextField targetStartIndexTextField = new TextField();
        targetStartIndexTextField.setPrefColumnCount(8);
        targetStartIndexTextField.setText(project.targetFileStartIndex.get() + "");

        BorderPane imageTargetDirectoryPane = new BorderPane();
        imageTargetDirectoryPane.setCenter(targetDirectoryTextField);
        imageTargetDirectoryPane.setRight(imageTargetDirectoryButton);
        BorderPane.setMargin(targetDirectoryTextField, new Insets(0, 2, 0, 0));

        GridPane settingsPane = new GridPane();
        settingsPane.setHgap(3);
        settingsPane.setVgap(3);

        Label imageTargetDirectoryLabel = new Label(AppInfo.RES.getString("target.directory"));
        Label patternLabel = new Label(AppInfo.RES.getString("image.filename.pattern"));
        Label indexLabel = new Label(AppInfo.RES.getString("start.at.index"));
        settingsPane.add(imageTargetDirectoryLabel, 0, 0);
        settingsPane.add(imageTargetDirectoryPane, 0, 1);
        settingsPane.add(patternLabel, 0, 2);
        settingsPane.add(targetFileNamePatternTextField, 1, 2);
        settingsPane.add(indexLabel, 0, 3);
        settingsPane.add(targetStartIndexTextField, 1, 3);

        GridPane.setColumnSpan(imageTargetDirectoryLabel, 2);
        GridPane.setColumnSpan(imageTargetDirectoryPane, 2);

        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setDisable(true);
        progressBar.setPrefWidth(400);

        BorderPane progressPane = new BorderPane();
        progressPane.setPadding(new Insets(10, 0, 0, 0));
        progressPane.setCenter(progressBar);

        BorderPane content = new BorderPane();
        content.setPadding(new Insets(10));
        content.setCenter(settingsPane);
        content.setBottom(progressPane);


        ButtonType renameButtonType = new ButtonType(AppInfo.RES.getString("start"), ButtonBar.ButtonData.APPLY);
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(window);
        dialog.setTitle(AppInfo.getWindowTitle(AppInfo.RES.getString("copy.and.rename")));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(renameButtonType);
        dialog.setResizable(true);

        final Button applyButton = (Button) dialog.getDialogPane().lookupButton(renameButtonType);
        applyButton.addEventFilter(ActionEvent.ACTION, event -> {

                    // TODO: validate dialog inputs
                    project.targetDirName.set(targetDirectoryTextField.getText());
                    project.targetFileNamePattern.set(targetFileNamePatternTextField.getText());
                    project.targetFileStartIndex.set(Integer.parseInt(targetStartIndexTextField.getText()));

                    content.setDisable(true);
                    applyButton.setDisable(true);
                    progressBar.setDisable(false);

                    event.consume();

                    CopyAndRenameImagesService copyAndRenameImagesService = new CopyAndRenameImagesService();
                    copyAndRenameImagesService.setFiles(project.imageFiles);
                    copyAndRenameImagesService.setDirectory(projectDir.resolve(project.targetDirName.get()));
                    copyAndRenameImagesService.setPattern(project.targetFileNamePattern.get());
                    copyAndRenameImagesService.setStartIndex(project.targetFileStartIndex.get());
                    copyAndRenameImagesService.setOnSucceeded(event1 -> dialog.close());
                    copyAndRenameImagesService.setOnCancelled(event1 -> dialog.close());
                    copyAndRenameImagesService.setOnFailed(event1 -> dialog.close());
                    progressBar.progressProperty().bind(copyAndRenameImagesService.progressProperty());
                    copyAndRenameImagesService.start();
                }
        );

        dialog.show();
    }

    private static void showDirChooser(Stage window, Path projectDir, TextField textField) {
        String initialPath = textField.getText();
        Path initialDir = projectDir.resolve(initialPath != null ? initialPath : "");
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle(AppInfo.getWindowTitle(AppInfo.RES.getString("select.target.directory")));
        File initialDir1 = new File(initialDir.toString());
        while (initialDir1 != null && !initialDir1.isDirectory()) {
            initialDir1 = initialDir1.getParentFile();
        }
        if (initialDir1 != null) {
            dirChooser.setInitialDirectory(initialDir1);
        }
        File file = dirChooser.showDialog(window);
        Path targetPath = file != null ? file.toPath() : null;
        if (targetPath != null) {
            textField.setText(projectDir.relativize(targetPath).toString());
        }
    }
}


