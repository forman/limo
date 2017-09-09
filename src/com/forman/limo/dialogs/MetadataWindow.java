package com.forman.limo.dialogs;

import com.forman.limo.AppInfo;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;

public class MetadataWindow extends Stage {

    private final ObservableList<Item> items;

    public static class Item {
        private final SimpleStringProperty catNameProperty;
        private final SimpleStringProperty tagNameProperty;
        private final SimpleStringProperty tagValueProperty;

        public Item(String catName, String tagName, String tagValue) {
            this.catNameProperty = new SimpleStringProperty(this, "catName", catName);
            this.tagNameProperty = new SimpleStringProperty(this, "tagName", tagName);
            this.tagValueProperty = new SimpleStringProperty(this, "tagValue", tagValue);
        }

        public String getCatName() {
            return catNameProperty.get();
        }

        public StringProperty catNameProperty() {
            return catNameProperty;
        }

        public String getTagName() {
            return tagNameProperty.get();
        }

        public StringProperty tagNameProperty() {
            return tagNameProperty;
        }

        public String getTagValue() {
            return tagValueProperty.get();
        }

        public StringProperty tagValueProperty() {
            return tagValueProperty;
        }
    }

    public MetadataWindow(Stage owner) {
        initStyle(StageStyle.UTILITY);
        initOwner(owner);
        initModality(Modality.NONE);

        setTitle(AppInfo.getWindowTitle(AppInfo.RES.getString("image.metadata")));
        getIcons().setAll(owner.getIcons());

        TableView<Item> table = new TableView<>();

        items = FXCollections.observableArrayList();
        table.setItems(items);

        int catNameColSize = 80;
        int tagNameColSize = 140;
        int tagValueColSize = 240;

        TableColumn<Item, String> catNameColumn = new TableColumn<>(AppInfo.RES.getString("category"));
        catNameColumn.setCellValueFactory(new PropertyValueFactory<>("catName"));
        catNameColumn.setPrefWidth(80);
        TableColumn<Item, String> tagNameColumn = new TableColumn<>(AppInfo.RES.getString("tag"));
        tagNameColumn.setCellValueFactory(new PropertyValueFactory<>("tagName"));
        tagNameColumn.setPrefWidth(140);
        TableColumn<Item, String> tagValueColumn = new TableColumn<>(AppInfo.RES.getString("value"));
        tagValueColumn.setCellValueFactory(new PropertyValueFactory<>("tagValue"));
        tagValueColumn.setPrefWidth(240);
        table.getColumns().setAll(catNameColumn, tagNameColumn, tagValueColumn);

        setScene(new Scene(table, catNameColSize + tagNameColSize + tagValueColSize + 20, 420));
    }

    public void setItems(Map<String, Map<String, String>> data) {
        ArrayList<Item> itemList = new ArrayList<>();
        for (String catName : data.keySet()) {
            Map<String, String> catData = data.get(catName);
            for (String tagName : catData.keySet()) {
                itemList.add(new Item(catName, tagName, catData.get(tagName)));
            }
        }
        itemList.sort((o1, o2) -> {
            int d = o1.getCatName().compareTo(o2.getCatName());
            if (d != 0) {
                return d;
            }
            return o1.getTagName().compareTo(o2.getTagName());
        });
        items.setAll(itemList);
    }

    public void clearItems() {
        items.clear();
    }


}
