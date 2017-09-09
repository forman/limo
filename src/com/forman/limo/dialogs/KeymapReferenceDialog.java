package com.forman.limo.dialogs;

import com.forman.limo.AppInfo;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import static com.forman.limo.AppInfo.*;

public class KeymapReferenceDialog {
    static String[][] KEYMAP = {
            {"+", RES.getString("move.selected.images.up")},
            {"-", RES.getString("move.selected.images.down")},
            {"Ctrl +", RES.getString("move.selected.images.to.top")},
            {"Ctrl -", RES.getString("move.selected.images.to.bottom")},
            {"Delete", RES.getString("delete.selected.images.removes.from.list.never.deletes.files")},
            {"Ctrl Z", RES.getString("undo.last.action")},
            {"Ctrl Shift Z", RES.getString("redo.last.undone.action")},
    };

    public static void show(Stage window) {

        GridPane keyMapPane = new GridPane();
        keyMapPane.setHgap(8);
        keyMapPane.setVgap(4);

        for (int i = 0; i < KEYMAP.length; i++) {
            String[] info = KEYMAP[i];
            Text key = new Text(info[0]);
            key.setFont(Font.font(key.getFont().getFamily(), FontWeight.BOLD, key.getFont().getSize()));
            keyMapPane.add(key, 0, i);
            keyMapPane.add(new Text(info[1]), 1, i);
        }

        ButtonType buttonTypeOk = new ButtonType(AppInfo.RES.getString("ok"), ButtonBar.ButtonData.OK_DONE);
        Dialog<Void> dialog = new Dialog<>();
        dialog.initOwner(window);
        dialog.setTitle(AppInfo.RES.getString("keymap.reference"));
        dialog.getDialogPane().setContent(keyMapPane);
        dialog.getDialogPane().getButtonTypes().add(buttonTypeOk);
        dialog.show();
    }
}
