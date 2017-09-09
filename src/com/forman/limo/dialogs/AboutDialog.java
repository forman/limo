package com.forman.limo.dialogs;

import com.forman.limo.AppInfo;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.text.MessageFormat;

public class AboutDialog {
    public static void show(Stage window) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initOwner(window);
        alert.setTitle(AppInfo.getWindowTitle(AppInfo.RES.getString("about")));
        alert.setHeaderText(MessageFormat.format("{0}, version {1}\nLowly Image Organizer", AppInfo.NAME, AppInfo.VERSION));
        alert.setContentText(AppInfo.RES.getString("copyright"));
        alert.showAndWait();
    }
}
