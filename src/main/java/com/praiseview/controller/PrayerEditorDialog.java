package com.praiseview.controller;

import com.praiseview.model.Prayer;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

public class PrayerEditorDialog extends Dialog<Prayer> {

    public PrayerEditorDialog(Prayer prayerToEdit) {
        setTitle(prayerToEdit == null ? "Add New Prayer" : "Edit Prayer");
        setResizable(true);
        getDialogPane().setPrefSize(700, 580);

        VBox layout = new VBox(12);
        layout.setPadding(new Insets(15));

        TextField titleField = new TextField(prayerToEdit != null ? prayerToEdit.getTitle() : "");
        ComboBox<String> categoryCombo = new ComboBox<>();
        categoryCombo.getItems().addAll("Ordinary", "Eucharistic", "Lenten", "Easter", "Seasonal", "Other");
        categoryCombo.setValue(prayerToEdit != null ? prayerToEdit.getCategory() : "Ordinary");

        TextArea contentArea = new TextArea(prayerToEdit != null ? prayerToEdit.getContent() : "");
        contentArea.setPromptText("Paste the full prayer text here...");
        contentArea.setPrefHeight(320);

        layout.getChildren().addAll(
                new Label("Prayer Title:"), titleField,
                new Label("Category:"), categoryCombo,
                new Label("Full Prayer Text:"), contentArea
        );

        getDialogPane().setContent(layout);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                Prayer prayer = prayerToEdit != null ? prayerToEdit : new Prayer();
                prayer.setTitle(titleField.getText().trim());
                prayer.setCategory(categoryCombo.getValue());
                prayer.setContent(contentArea.getText().trim());
                return prayer;
            }
            return null;
        });
    }
}