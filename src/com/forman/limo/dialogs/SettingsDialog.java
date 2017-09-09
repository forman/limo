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
import com.forman.limo.data.Prefs;
import com.forman.limo.data.Project;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

public class SettingsDialog {

    public static void show(Stage window, Project project, Prefs preferences) {

        // Project

        TextField imageFileNameExtTextField = new TextField();
        imageFileNameExtTextField.setText(project.imageFileNameExt.getValue());

        CheckBox scanRecursiveCheckBox = new CheckBox(AppInfo.RES.getString("also.scan.subdirectories"));
        scanRecursiveCheckBox.setSelected(project.scanRecursive.get());

        CheckBox relativizePathsCheckBox = new CheckBox(AppInfo.RES.getString("store.image.file.paths.relative.to.project"));
        relativizePathsCheckBox.setSelected(project.relativizePaths.get());

        GridPane projectPanel = new GridPane();
        projectPanel.setPadding(new Insets(10));
        projectPanel.setHgap(3);
        projectPanel.setVgap(3);

        projectPanel.add(new Label(AppInfo.RES.getString("image.file.extensions")), 0, 0);
        projectPanel.add(imageFileNameExtTextField, 1, 0);
        projectPanel.add(scanRecursiveCheckBox, 0, 1);
        projectPanel.add(relativizePathsCheckBox, 0, 2);

        GridPane.setColumnSpan(scanRecursiveCheckBox, 2);
        GridPane.setColumnSpan(relativizePathsCheckBox, 2);

        // Prefs

        CheckBox openLastProjectCheckBox = new CheckBox(AppInfo.RES.getString("reopen.last.project.on.startup"));
        openLastProjectCheckBox.setSelected(preferences.openLastProject.get());

        TextField minImageDisplaySizeTextField = new TextField(preferences.minImageDisplaySize.get() + "");
        minImageDisplaySizeTextField.setPrefColumnCount(6);
        TextField maxImageDisplaySizeTextField = new TextField(preferences.maxImageDisplaySize.get() + "");
        maxImageDisplaySizeTextField.setPrefColumnCount(6);

        GridPane preferencesPanel = new GridPane();
        preferencesPanel.setPadding(new Insets(10));
        preferencesPanel.setHgap(3);
        preferencesPanel.setVgap(3);

        preferencesPanel.add(openLastProjectCheckBox, 0, 0);
        preferencesPanel.add(new Label(AppInfo.RES.getString("minimum.image.size")), 0, 1);
        preferencesPanel.add(minImageDisplaySizeTextField, 1, 1);
        preferencesPanel.add(new Label(AppInfo.RES.getString("maximum.image.size")), 0, 2);
        preferencesPanel.add(maxImageDisplaySizeTextField, 1, 2);

        GridPane.setColumnSpan(openLastProjectCheckBox, 2);
        GridPane.setHalignment(minImageDisplaySizeTextField, HPos.RIGHT);
        GridPane.setHalignment(maxImageDisplaySizeTextField, HPos.RIGHT);

        Tab projectTab = new Tab();
        projectTab.setText(AppInfo.RES.getString("project"));
        projectTab.setContent(projectPanel);
        projectTab.setClosable(false);

        Tab preferencesTab = new Tab();
        preferencesTab.setText(AppInfo.RES.getString("preferences"));
        preferencesTab.setContent(preferencesPanel);
        preferencesTab.setClosable(false);

        TabPane tabPane = new TabPane();
        tabPane.getTabs().add(projectTab);
        tabPane.getTabs().add(preferencesTab);

        ButtonType okButtonType = new ButtonType(AppInfo.RES.getString("ok"), ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType(AppInfo.RES.getString("cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(window);
        dialog.setTitle(AppInfo.getWindowTitle(AppInfo.RES.getString("settings1")));
        dialog.getDialogPane().setContent(tabPane);
        dialog.getDialogPane().getButtonTypes().add(okButtonType);
        dialog.getDialogPane().getButtonTypes().add(cancelButtonType);
        dialog.showAndWait().ifPresent(response -> {
            if (response == okButtonType) {
                // TODO: validate dialog inputs

                // Project
                project.imageFileNameExt.set(imageFileNameExtTextField.getText());
                project.scanRecursive.set(scanRecursiveCheckBox.isSelected());
                project.relativizePaths.set(relativizePathsCheckBox.isSelected());

                // Prefs
                preferences.openLastProject.set(openLastProjectCheckBox.isSelected());
                preferences.minImageDisplaySize.set(Integer.parseInt(minImageDisplaySizeTextField.getText()));
                preferences.maxImageDisplaySize.set(Integer.parseInt(maxImageDisplaySizeTextField.getText()));
            }
        });

    }

}


