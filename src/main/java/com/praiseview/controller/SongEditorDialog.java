package com.praiseview.controller;

import com.praiseview.model.Song;
import com.praiseview.model.Verse;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.StageStyle;

import java.util.ArrayList;

public class SongEditorDialog extends Dialog<Song> {

    public SongEditorDialog(Song songToEdit) {
        setTitle(songToEdit == null ? "Add New Song" : "Edit Song");

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(15));

        TextField titleField = new TextField(songToEdit != null ? songToEdit.getTitle() : "");
        TextField artistField = new TextField(songToEdit != null ? songToEdit.getArtist() : "");
        TextField keyField = new TextField(songToEdit != null ? songToEdit.getKey() : "");

        TextArea lyricsArea = new TextArea();
        lyricsArea.setPromptText("Paste lyrics here...");
        lyricsArea.setPrefHeight(200);

        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("VERSE", "CHORUS", "BRIDGE", "CODA", "PRECHORUS");
        typeCombo.setValue("VERSE");

        ListView<Verse> verseListView = new ListView<>();
        if (songToEdit != null) verseListView.getItems().addAll(songToEdit.getVerses());

        Button addVerseBtn = new Button("Add Verse");
        addVerseBtn.setOnAction(e -> {
            if (!lyricsArea.getText().isBlank()) {
                Verse verse = new Verse(
                        typeCombo.getValue() + " " + (verseListView.getItems().size() + 1),
                        lyricsArea.getText().trim(),
                        Verse.VerseType.valueOf(typeCombo.getValue())
                );
                verseListView.getItems().add(verse);
                lyricsArea.clear();
            }
        });

        vbox.getChildren().addAll(
                new Label("Title:"), titleField,
                new Label("Artist:"), artistField,
                new Label("Key:"), keyField,
                new Label("Verse Type:"), typeCombo,
                lyricsArea, addVerseBtn,
                new Label("Added Verses:"), verseListView
        );

        getDialogPane().setContent(vbox);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                Song song = songToEdit != null ? songToEdit : new Song();
                song.setTitle(titleField.getText());
                song.setArtist(artistField.getText());
                song.setKey(keyField.getText());
                song.setVerses(new ArrayList<>(verseListView.getItems()));
                return song;
            }
            return null;
        });
    }
}