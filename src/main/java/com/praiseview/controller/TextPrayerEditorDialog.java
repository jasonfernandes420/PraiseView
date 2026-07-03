package com.praiseview.controller;

import javafx.geometry.Rectangle2D;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.stage.Screen;

/**
 * Unified editor for both Prayers and Custom Texts
 * Supports slide markers [==slide==] for multi-slide content
 */
public class TextPrayerEditorDialog extends Dialog<String> {
    
    private TextArea contentArea;
    private String initialContent;
    private String itemType; // "Prayer" or "Text"

    public TextPrayerEditorDialog(String itemType, String initialContent, String title) {
        this.itemType = itemType;
        this.initialContent = initialContent != null ? initialContent : "";
        
        setTitle(title);
        setResizable(true);
        
        Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();
        double dialogWidth = Math.min(750, visualBounds.getWidth() * 0.85);
        double dialogHeight = Math.min(600, visualBounds.getHeight() * 0.85);
        getDialogPane().setPrefSize(dialogWidth, dialogHeight);
        getDialogPane().setMinSize(500, 400);

        VBox layout = new VBox(12);
        layout.setPadding(new Insets(15));

        // Content editor
        Label contentLabel = new Label(itemType + " Content:");
        contentLabel.setStyle("-fx-font-weight: bold;");
        
        contentArea = new TextArea(this.initialContent);
        contentArea.setPromptText("Enter " + itemType.toLowerCase() + " text here...");
        contentArea.setWrapText(true);
        contentArea.setPrefHeight(400);
        VBox.setVgrow(contentArea, Priority.ALWAYS);

        // New Slide button
        Button newSlideBtn = new Button("+ New Slide");
        newSlideBtn.setPrefWidth(120);
        newSlideBtn.setStyle("-fx-font-size: 11px;");
        newSlideBtn.setOnAction(e -> insertSlideMarker());

        // Instructions
        Label instructionsLabel = new Label(
            "💡 Use '+ New Slide' button to split content into multiple slides for projection.\n" +
            "Each [==slide==] marker creates a new slide during rendering."
        );
        instructionsLabel.setWrapText(true);
        instructionsLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 10px;");

        HBox controlBox = new HBox(10);
        controlBox.setStyle("-fx-alignment: center-left;");
        controlBox.getChildren().addAll(newSlideBtn);

        layout.getChildren().addAll(
            contentLabel,
            contentArea,
            instructionsLabel,
            controlBox
        );

        getDialogPane().setContent(layout);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                return contentArea.getText().trim();
            }
            return null;
        });
    }

    /**
     * Inserts a slide marker [==slide==] at the current cursor position
     */
    private void insertSlideMarker() {
        int caretPosition = contentArea.getCaretPosition();
        String currentText = contentArea.getText();
        
        String slideMarker = "\n[==slide==]\n";
        
        // Insert at cursor position
        String newText = currentText.substring(0, caretPosition) + slideMarker + currentText.substring(caretPosition);
        contentArea.setText(newText);
        
        // Move cursor after the marker
        contentArea.positionCaret(caretPosition + slideMarker.length());
        contentArea.requestFocus();
    }

    /**
     * Factory method for Prayer editor
     */
    public static TextPrayerEditorDialog createPrayerEditor(String initialContent, String prayerTitle) {
        return new TextPrayerEditorDialog("Prayer", initialContent, prayerTitle);
    }

    /**
     * Factory method for Text editor
     */
    public static TextPrayerEditorDialog createTextEditor(String initialContent, String textTitle) {
        return new TextPrayerEditorDialog("Text", initialContent, textTitle);
    }
}
