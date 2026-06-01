package com.praiseview.controller;

import com.praiseview.model.Song;
import com.praiseview.model.Theme;
import com.praiseview.model.Verse;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;

public class ProjectionController {

    @FXML private StackPane projectionRoot;
    @FXML private TextFlow lyricsDisplay;
    @FXML private Label songTitleLabel;

    private Theme currentTheme = new Theme();
    private double currentFontSize = 52.0;

    public void displaySong(Song song, Verse currentVerse) {
        if (projectionRoot != null) {
            projectionRoot.setStyle("-fx-background-color: " + toRgbString(currentTheme.getBackgroundColor()) + ";");
        }

        if (songTitleLabel != null) {
            songTitleLabel.setText(song.getTitle());
            songTitleLabel.setStyle("-fx-text-fill: " + toRgbString(currentTheme.getTextColor()) + ";");
        }

        if (lyricsDisplay != null) {
            lyricsDisplay.getChildren().clear();
            Text verseText = new Text(currentVerse.getContent());
            verseText.setStyle("-fx-font-size: " + currentFontSize + "px; -fx-font-family: '" + currentTheme.getFontFamily() + "';");
            verseText.setFill(currentTheme.getTextColor());
            lyricsDisplay.getChildren().add(verseText);
            lyricsDisplay.setTextAlignment(TextAlignment.CENTER);
        }
    }

    public void clearScreen() {
        if (lyricsDisplay != null) lyricsDisplay.getChildren().clear();
        if (songTitleLabel != null) songTitleLabel.setText("");
    }

    public void blackout() {
        if (projectionRoot != null) {
            projectionRoot.setStyle("-fx-background-color: black;");
        }
        clearScreen();
    }

    public void increaseFont() {
        currentFontSize += 4;
    }

    public void decreaseFont() {
        currentFontSize = Math.max(28, currentFontSize - 4);
    }

    private String toRgbString(Color color) {
        return String.format("rgb(%d,%d,%d)", 
            (int)(color.getRed()*255), (int)(color.getGreen()*255), (int)(color.getBlue()*255));
    }
}