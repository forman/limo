package com.forman.limo;

import com.forman.limo.data.ImageItem;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;

import java.text.MessageFormat;

class ImageTile {
    public static final Image MISSING_IMAGE = new Image("com/forman/limo/resources/missing.png");

    interface Listener {
        void onImageTileMousePressed(MouseEvent event, ImageTile imageTile);

        void onImageTileContextMenuRequested(ContextMenuEvent event, ImageTile imageTile);
    }

    final ImageItem imageItem;
    final BorderPane imageTilePane;
    final ImageView imageView;

    private boolean selected;

    ImageTile(ImageItem imageItem, double imageFitWidth, boolean selected, Listener listener) {
        this.imageItem = imageItem;
        this.selected = selected;

        Label imageLabel = new Label(imageItem.file.getFileName().toString());

        Image image = imageItem.image;

        imageView = new ImageView(image != null ? image : MISSING_IMAGE);
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(imageFitWidth);

        Tooltip tooltip;
        if (image != null) {
            // TODO: get real size
            tooltip = new Tooltip(MessageFormat.format(AppInfo.RES.getString("0.x.1.pixels.n.2"),
                    image.getWidth(), image.getHeight(),
                    imageItem.file.getParent().toString()));
        } else {
            tooltip = new Tooltip(imageItem.file.getParent().toString());
        }
        Tooltip.install(imageView, tooltip);

        BorderPane.setMargin(imageView, new Insets(3, 3, 3, 3));
        BorderPane.setMargin(imageLabel, new Insets(0, 3, 0, 3));
        BorderPane.setAlignment(imageLabel, Pos.CENTER);

        imageTilePane = new BorderPane();
        imageTilePane.setId(imageItem.file.toString());

        imageTilePane.setPadding(new Insets(3));
        imageTilePane.setCenter(imageView);
        imageTilePane.setBottom(imageLabel);

        imageTilePane.setOnMousePressed(event -> {
            if (event.isPrimaryButtonDown()) {
                listener.onImageTileMousePressed(event, this);
            }
        });

        imageTilePane.setOnContextMenuRequested(event -> listener.onImageTileContextMenuRequested(event, this));

        updateState();
    }

    boolean isSelected() {
        return selected;
    }

    void setSelected(boolean selected) {
        this.selected = selected;
        updateState();
    }

    private void updateState() {
        imageView.setEffect(new DropShadow(selected ? 16 : 8, selected ? Color.BLUE : Color.BLACK));
    }

}
