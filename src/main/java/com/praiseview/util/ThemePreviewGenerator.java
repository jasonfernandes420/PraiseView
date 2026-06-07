package com.praiseview.util;

import com.praiseview.model.Theme;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ThemePreviewGenerator {

    private static final int PREVIEW_WIDTH = 160;
    private static final int PREVIEW_HEIGHT = 120;
    private static final String SAMPLE_TEXT = "AaBbCc\n123"; // Sample text for preview

    // Cache for generated theme previews
    private static final Map<Theme, Image> previewCache = new HashMap<>();

    /**
     * Generates a small image preview for the given theme.
     * The preview is cached to avoid regenerating the same image multiple times.
     *
     * @param theme The theme for which to generate a preview.
     * @return An Image representing the theme preview.
     */
    public static Image generatePreview(Theme theme) {
        if (theme == null) {
            return null;
        }

        // Check cache first
        if (previewCache.containsKey(theme)) {
            return previewCache.get(theme);
        }

        // Create a temporary StackPane to render the theme
        StackPane previewPane = new StackPane();
        previewPane.setPrefSize(PREVIEW_WIDTH, PREVIEW_HEIGHT);
        previewPane.setStyle("-fx-background-color: " + theme.getBackgroundColor() + ";");

        // Apply background image if available
        if (theme.getBackgroundImagePath() != null && !theme.getBackgroundImagePath().isEmpty()) {
            File imageFile = new File(theme.getBackgroundImagePath());
            if (imageFile.exists()) {
                try {
                    Image backgroundImage = new Image(imageFile.toURI().toString(), PREVIEW_WIDTH, PREVIEW_HEIGHT, true, true);
                    javafx.scene.image.ImageView backgroundImageView = new javafx.scene.image.ImageView(backgroundImage);
                    backgroundImageView.setPreserveRatio(true);
                    backgroundImageView.setFitWidth(PREVIEW_WIDTH);
                    backgroundImageView.setFitHeight(PREVIEW_HEIGHT);
                    previewPane.getChildren().add(backgroundImageView);
                } catch (Exception e) {
                    AppLogger.log("Error loading background image for theme preview: " + theme.getName() + " - " + e.getMessage());
                }
            }
        }
        // Note: Background video is not supported in static image previews.

        // Add sample text
        TextFlow textFlow = new TextFlow();
        textFlow.setPrefWidth(PREVIEW_WIDTH - 20); // Some padding
        textFlow.setPrefHeight(PREVIEW_HEIGHT - 20);
        textFlow.setStyle("-fx-padding: 10px;");

        Text sampleText = new Text(SAMPLE_TEXT);
        sampleText.setFill(Color.web(theme.getTextColor()));

        // Try to apply font family, fallback to default if not found
        try {
            sampleText.setFont(Font.font(theme.getFontFamily(), Math.min(theme.getFontSize() / 4, 20))); // Smaller font size for preview
        } catch (Exception e) {
            AppLogger.log("Font family '" + theme.getFontFamily() + "' not found for preview, falling back to default. Error: " + e.getMessage());
            sampleText.setFont(Font.font("System", Math.min(theme.getFontSize() / 4, 20)));
        }

        textFlow.getChildren().add(sampleText);

        switch (theme.getTextAlignment().toUpperCase()) {
            case "LEFT":
                textFlow.setTextAlignment(TextAlignment.LEFT);
                break;
            case "RIGHT":
                textFlow.setTextAlignment(TextAlignment.RIGHT);
                break;
            case "CENTER":
            default:
                textFlow.setTextAlignment(TextAlignment.CENTER);
                break;
        }
        previewPane.getChildren().add(textFlow);

        // Take a snapshot
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT); // Ensure transparent background if not explicitly set
        WritableImage snapshot = previewPane.snapshot(params, null);

        // Cache and return
        previewCache.put(theme, snapshot);
        return snapshot;
    }

    /**
     * Clears the preview cache. Should be called when themes are modified or deleted.
     */
    public static void clearCache() {
        previewCache.clear();
        AppLogger.log("Theme preview cache cleared.");
    }
}
