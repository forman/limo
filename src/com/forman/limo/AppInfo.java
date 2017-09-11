package com.forman.limo;

import javafx.stage.FileChooser.ExtensionFilter;

import java.text.MessageFormat;
import java.util.ResourceBundle;

public class AppInfo {
    public final static ResourceBundle RES = ResourceBundle.getBundle("com.forman.limo.resources.limo");

    public static final String NAME = "Limo";
    public static final String VERSION = "0.5.1";

    public static final String FILE_EXTENSION = "limo";
    public static final String FILE_DESCRIPTION = MessageFormat.format(AppInfo.RES.getString("limo.project.files"), NAME);
    public static final ExtensionFilter FILE_EXTENSION_FILTER = new ExtensionFilter(FILE_DESCRIPTION, "*." + FILE_EXTENSION);

    public static String getWindowTitle(String title) {
        return MessageFormat.format("{0} - {1}", NAME, title);
    }

    public final static boolean DEBUG = Boolean.getBoolean("limo.debug");
}
