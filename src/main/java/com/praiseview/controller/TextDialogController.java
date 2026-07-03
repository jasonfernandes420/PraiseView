package com.praiseview.controller;

import com.praiseview.model.TextSlide;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;

public class TextDialogController {

    @FXML
    private TextField titleField;
    @FXML
    private TextArea bodyArea;
    @FXML
    private VBox mainContainer;

    private TextSlide text;

    /**
     * Initializes the controller. This method is automatically called
     * after the fxml file has been loaded.
     */
    @FXML
    private void initialize() {
        // Add slide support UI elements
        addSlideMarkerSupport();
    }

    /**
     * Adds the "New Slide" button and instructions to the UI
     */
    private void addSlideMarkerSupport() {
        // Create button and instructions
        Button newSlideBtn = new Button("+ New Slide");
        newSlideBtn.setPrefWidth(120);
        newSlideBtn.setStyle("-fx-font-size: 11px;");
        newSlideBtn.setOnAction(e -> insertSlideMarker());

        Label instructionsLabel = new Label(
            "💡 Use '+ New Slide' button to split content into multiple slides for projection.\n" +
            "Each [==slide==] marker creates a new slide. Existing texts without markers work as single slides."
        );
        instructionsLabel.setWrapText(true);
        instructionsLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 10px;");

        HBox controlBox = new HBox(10);
        controlBox.setStyle("-fx-alignment: center-left;");
        controlBox.setPadding(new Insets(5, 0, 0, 0));
        controlBox.getChildren().addAll(newSlideBtn);

        // Insert into main container
        mainContainer.getChildren().add(mainContainer.getChildren().size(), instructionsLabel);
        mainContainer.getChildren().add(mainContainer.getChildren().size(), controlBox);
    }

    /**
     * Inserts a slide marker [==slide==] at the current cursor position
     */
    private void insertSlideMarker() {
        int caretPosition = bodyArea.getCaretPosition();
        String currentText = bodyArea.getText();
        
        String slideMarker = "\n[==slide==]\n";
        
        // Insert at cursor position
        String newText = currentText.substring(0, caretPosition) + slideMarker + currentText.substring(caretPosition);
        bodyArea.setText(newText);
        
        // Move cursor after the marker
        bodyArea.positionCaret(caretPosition + slideMarker.length());
        bodyArea.requestFocus();
    }

    /**
     * Sets the text to be edited in the dialog.
     * If the text is null, it's a new text creation.
     * @param text The Text object to edit, or null for a new text.
     */
    public void setText(TextSlide text) {
        this.text = text;
        if (text != null) {
            titleField.setText(text.getTitle());
            bodyArea.setText(text.getContent());
        }
    }

    /**
     * Returns the Text object with the data entered in the dialog.
     * @return The Text object, or null if input is invalid.
     */
    public TextSlide getText() {
        String title = titleField.getText();
        String body = bodyArea.getText();

        if (title == null || title.trim().isEmpty()) {
            // In a real application, you might show an alert to the user
            System.err.println("Title cannot be empty.");
            return null;
        }

        if (text == null) {
            // New text
            return new TextSlide(title, body);
        } else {
            // Editing existing text
            text.setTitle(title);
            text.setContent(body);
            return text;
        }
    }
}
