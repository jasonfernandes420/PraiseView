package com.praiseview.controller;

import com.praiseview.model.Song;
import com.praiseview.model.Verse;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import java.util.ArrayList;
import java.util.List;

public class SongEditorDialog extends Dialog<Song> {

    public SongEditorDialog(Song songToEdit) {
        setTitle(songToEdit == null ? "Add New Song" : "Edit Song");
        setResizable(true);

        VBox mainLayout = new VBox(12);
        mainLayout.setPadding(new Insets(15));

        // Basic Information
        TextField titleField = new TextField(songToEdit != null ? songToEdit.getTitle() : "");
        ComboBox<String> languageCombo = new ComboBox<>();
        languageCombo.getItems().addAll("English", "Hindi", "Kannada", "Tamil");
        languageCombo.setValue(songToEdit != null ? songToEdit.getLanguage() : "English");

        ComboBox<String> categoryCombo = new ComboBox<>();
        categoryCombo.getItems().addAll("Entrance Hymn", "Penitential Rite", "Gloria",
                "Responsorial Psalm", "Gospel Acclamation", "Offertory", "Communion",
                "Meditation", "Recessional", "Adoration", "Marian Hymn", "Lenten Hymn",
                "Christmas Hymn", "Easter Hymn", "Holy Week", "Funeral", "Wedding");
        categoryCombo.setValue(songToEdit != null ? songToEdit.getCategory() : "Entrance Hymn");

        TextField authorField = new TextField(songToEdit != null ? songToEdit.getAuthor() : "");
        TextField composerField = new TextField(songToEdit != null ? songToEdit.getComposer() : "");

        // Verse Input Section
        TextArea lyricsArea = new TextArea();
        lyricsArea.setPromptText("Paste lyrics here...");
        lyricsArea.setPrefHeight(120);

        ComboBox<String> verseTypeCombo = new ComboBox<>();
        verseTypeCombo.getItems().addAll("VERSE", "CHORUS", "BRIDGE", "PRE_CHORUS", "CODA");
        verseTypeCombo.setValue("VERSE");

        Button addVerseBtn = new Button("Add Verse");
        ListView<Verse> verseListView = new ListView<>();

        // Populate existing verses if editing
        if (songToEdit != null && !songToEdit.getVerses().isEmpty()) {
            verseListView.getItems().addAll(songToEdit.getVerses());
        }

        // Add Verse Button Action
        addVerseBtn.setOnAction(e -> {
            if (!lyricsArea.getText().isBlank()) {
                String type = verseTypeCombo.getValue();
                String label = type.replace("_", " ") + " " + (verseListView.getItems().size() + 1);

                Verse newVerse = new Verse(label, lyricsArea.getText().trim(),
                        Verse.VerseType.valueOf(type));
                verseListView.getItems().add(newVerse);
                lyricsArea.clear();
            }
        });

        // Enable Drag & Drop Reordering
        setupDragAndDropReordering(verseListView);

        mainLayout.getChildren().addAll(
                new Label("Title:"), titleField,
                new Label("Language:"), languageCombo,
                new Label("Category:"), categoryCombo,
                new Label("Author:"), authorField,
                new Label("Composer:"), composerField,
                new Separator(),
                new Label("Verse Type:"), verseTypeCombo,
                lyricsArea, addVerseBtn,
                new Label("Verse Order (Drag to reorder):"), verseListView
        );

        getDialogPane().setContent(mainLayout);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                Song song = songToEdit != null ? songToEdit : new Song();
                song.setTitle(titleField.getText());
                song.setLanguage(languageCombo.getValue());
                song.setCategory(categoryCombo.getValue());
                song.setAuthor(authorField.getText());
                song.setComposer(composerField.getText());
                song.setVerses(new ArrayList<>(verseListView.getItems()));
                return song;
            }
            return null;
        });
    }

    // Drag & Drop Reordering Logic
    private void setupDragAndDropReordering(ListView<Verse> listView) {
        listView.setOnDragDetected(e -> {
            Verse selected = listView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                Dragboard db = listView.startDragAndDrop(TransferMode.MOVE);
                ClipboardContent content = new ClipboardContent();
                content.putString(String.valueOf(listView.getSelectionModel().getSelectedIndex()));
                db.setContent(content);
                e.consume();
            }
        });

        listView.setOnDragOver(e -> {
            if (e.getDragboard().hasString()) {
                e.acceptTransferModes(TransferMode.MOVE);
            }
        });

        listView.setOnDragDropped(e -> {
            Dragboard db = e.getDragboard();
            if (db.hasString()) {
                int draggedIndex = Integer.parseInt(db.getString());
                int dropIndex = listView.getSelectionModel().getSelectedIndex();

                if (dropIndex >= 0 && draggedIndex != dropIndex) {
                    Verse draggedVerse = listView.getItems().remove(draggedIndex);
                    listView.getItems().add(dropIndex, draggedVerse);
                    listView.getSelectionModel().select(dropIndex);
                }
                e.setDropCompleted(true);
            }
        });
    }
}
