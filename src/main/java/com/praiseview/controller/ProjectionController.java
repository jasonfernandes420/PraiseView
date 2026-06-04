package com.praiseview.controller;

import com.praiseview.model.Song;
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
    @FXML private Label titleLabel;
    @FXML private TextFlow lyricsFlow;

    private double currentFontSize = 62.0;
    private Song currentSong;
    private int currentPosition = 0;   // Position in custom verseOrder

    @FXML
public void initialize() {

    System.out.println(
            "ProjectionController initialized"
    );
}
    public void showSlide(Song song, int position) {
        this.currentSong = song;
        this.currentPosition = position;

        if (song == null || song.getVerseOrder().isEmpty()) {
            clear();
            return;
        }

        int verseIndex = song.getVerseOrder().get(position);
        Verse verse = song.getVerses().get(verseIndex);

        // Background
        projectionRoot.setStyle("-fx-background-color: #0f0f0f;");

        // Title
        titleLabel.setText(song.getTitle() + " — " + verse.getLabel());
        titleLabel.setStyle("-fx-text-fill: #ffd700; -fx-font-size: 42px;");

        // Lyrics
        lyricsFlow.getChildren().clear();
        Text lyricsText = new Text(verse.getContent());
        lyricsText.setFill(Color.WHITE);
        lyricsText.setStyle("-fx-font-size: " + currentFontSize + "px; -fx-line-spacing: 8px;");

        lyricsFlow.getChildren().add(lyricsText);
        lyricsFlow.setTextAlignment(TextAlignment.CENTER);
    }

    public void nextVerse() {
        if (currentSong == null) return;
        if (currentPosition < currentSong.getVerseOrder().size() - 1) {
            currentPosition++;
            showSlide(currentSong, currentPosition);
        }
    }

    public void previousVerse() {
        if (currentSong == null) return;
        if (currentPosition > 0) {
            currentPosition--;
            showSlide(currentSong, currentPosition);
        }
    }

    public void blackout() {
        projectionRoot.setStyle("-fx-background-color: black;");
        clear();
    }

    public void clear() {
        lyricsFlow.getChildren().clear();
        titleLabel.setText("");
    }

    public void setFontSize(double size) {
        currentFontSize = size;
    }
}
