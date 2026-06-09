package com.praiseview.controller;

import com.praiseview.model.TextSlide;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class TextDialogController {

    @FXML
    private TextField titleField;
    @FXML
    private TextArea bodyArea;

    private TextSlide text;

    /**
     * Initializes the controller. This method is automatically called
     * after the fxml file has been loaded.
     */
    @FXML
    private void initialize() {
        // Optional: Add any initial setup or validation listeners here
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
