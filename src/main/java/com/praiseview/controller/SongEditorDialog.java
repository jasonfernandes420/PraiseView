package com.praiseview.controller;

import com.praiseview.model.Song;
import com.praiseview.model.Verse;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SongEditorDialog extends Dialog<Song> {

    private Map<String, Integer> typeCounter = new HashMap<>();

    public SongEditorDialog(Song songToEdit) {
        setTitle(songToEdit == null ? "Add New Song" : "Edit Song");
        setResizable(true);
        getDialogPane().setPrefSize(850, 720);   // Bigger dialog

        VBox mainLayout = new VBox(12);
        mainLayout.setPadding(new Insets(15));

        // Basic Info
        TextField titleField = new TextField(songToEdit != null ? songToEdit.getTitle() : "");
        ComboBox<String> languageCombo = new ComboBox<>();
        languageCombo.getItems().addAll("English", "Hindi", "Kannada", "Tamil");
        languageCombo.setValue(songToEdit != null ? songToEdit.getLanguage() : "English");

        ComboBox<String> categoryCombo = new ComboBox<>();
        categoryCombo.getItems().addAll("Entrance Hymn", "Penitential Rite", "Gloria",
                "Responsorial Psalm", "Gospel Acclamation", "Offertory", "Communion",
                "Meditation", "Recessional", "Adoration", "Marian Hymn", "Lenten Hymn",
                "Christmas Hymn", "Easter Hymn", "Holy Week", "Funeral", "Wedding");

        // === Verse Section ===
        HBox verseSection = new HBox(15);

        // Left: Available Verses
        VBox availableBox = new VBox(10);
        availableBox.setPrefWidth(300);

        TextArea lyricsArea = new TextArea();
        lyricsArea.setPromptText("Paste lyrics here...");
        lyricsArea.setPrefHeight(100);        // Reduced for edit area

        TextArea editVerseArea = new TextArea();
        editVerseArea.setPromptText("Select a verse to edit...");
        editVerseArea.setWrapText(true);
        editVerseArea.setPrefHeight(150);

        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("VERSE", "CHORUS", "BRIDGE", "PRE_CHORUS", "CODA");
        typeCombo.setValue("VERSE");

        Button addVerseBtn = new Button("➕ Add Verse");
        Button updateVerseBtn = new Button("✏ Update Selected Verse");

        ListView<Verse> availableList = new ListView<>();
        
        // Initialize typeCounter from existing song
        if (songToEdit != null && !songToEdit.getVerses().isEmpty()) {
            availableList.getItems().addAll(songToEdit.getVerses());
            for (Verse v : songToEdit.getVerses()) {
                String typeStr = v.getType().name();
                String[] parts = v.getLabel().split(" ");
                if (parts.length > 0) {
                    try {
                        int count = Integer.parseInt(parts[parts.length - 1]);
                        typeCounter.put(typeStr, count);
                    } catch (NumberFormatException e) {
                        typeCounter.put(typeStr, typeCounter.getOrDefault(typeStr, 0) + 1);
                    }
                }
            }
        }

        addVerseBtn.setOnAction(e -> {
            if (!lyricsArea.getText().trim().isEmpty()) {
                String type = typeCombo.getValue();
                int count = typeCounter.getOrDefault(type, 0) + 1;
                typeCounter.put(type, count);

                String label = type.replace("_", " ") + " " + count;

                Verse verse = new Verse(label, lyricsArea.getText().trim(), Verse.VerseType.valueOf(type));
                availableList.getItems().add(verse);
                lyricsArea.clear();
                editVerseArea.clear();
            }
        });

        // Handle verse selection for editing
        availableList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                editVerseArea.setText(newVal.getContent());
            }
        });

        // Update verse button
        updateVerseBtn.setOnAction(e -> {
            Verse selected = availableList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                selected.setContent(editVerseArea.getText());
                availableList.refresh();
            }
        });

        availableBox.getChildren().addAll(
                new Label("Available Verses"),
                typeCombo,
                lyricsArea,
                addVerseBtn,
                new Label("Edit Verse Content"),
                editVerseArea,
                updateVerseBtn,
                availableList
        );

        // Right: Performance Order
        VBox orderBox = new VBox(10);
        orderBox.setPrefWidth(340);

        ListView<Verse> orderList = new ListView<>();
        
        // Load existing verse order
        if (songToEdit != null && !songToEdit.getVerseOrder().isEmpty()) {
            for (int idx : songToEdit.getVerseOrder()) {
                if (idx < songToEdit.getVerses().size()) {
                    orderList.getItems().add(songToEdit.getVerses().get(idx));
                }
            }
        }

        Button addToOrderBtn = new Button("→ Add to Performance Order");
        addToOrderBtn.setOnAction(e -> {
            Verse selected = availableList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                orderList.getItems().add(selected);   // Allows repeats
            }
        });

        setupDragAndDrop(orderList);

        orderBox.getChildren().addAll(
                new Label("Performance Order (Drag to reorder)"),
                orderList,
                addToOrderBtn
        );

        verseSection.getChildren().addAll(availableBox, orderBox);

        mainLayout.getChildren().addAll(
                new Label("Title:"), titleField,
                new Label("Language:"), languageCombo,
                new Label("Category:"), categoryCombo,
                new Separator(),
                verseSection
        );

        getDialogPane().setContent(mainLayout);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                Song song = songToEdit != null ? songToEdit : new Song();
                song.setTitle(titleField.getText().trim());
                song.setLanguage(languageCombo.getValue());
                song.setCategory(categoryCombo.getValue());
                song.setVerses(new ArrayList<>(availableList.getItems()));
                song.setVerseOrderFromList(orderList.getItems());
                return song;
            }
            return null;
        });
    }

    private void setupDragAndDrop(ListView<Verse> listView) {
        listView.setCellFactory(lv -> new ListCell<Verse>() {
            @Override
            protected void updateItem(Verse item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getLabel());
            }
        });

        // Drag & Drop logic
        listView.setOnDragDetected(e -> {
            if (listView.getSelectionModel().getSelectedItem() != null) {
                Dragboard db = listView.startDragAndDrop(TransferMode.MOVE);
                ClipboardContent content = new ClipboardContent();
                content.putString(String.valueOf(listView.getSelectionModel().getSelectedIndex()));
                db.setContent(content);
            }
        });

        listView.setOnDragOver(e -> e.acceptTransferModes(TransferMode.MOVE));

        listView.setOnDragDropped(e -> {
            Dragboard db = e.getDragboard();
            if (db.hasString()) {
                int draggedIndex = Integer.parseInt(db.getString());
                int dropIndex = listView.getSelectionModel().getSelectedIndex();
                if (dropIndex >= 0 && draggedIndex != dropIndex) {
                    Verse dragged = listView.getItems().remove(draggedIndex);
                    listView.getItems().add(dropIndex, dragged);
                    listView.getSelectionModel().select(dropIndex);
                }
                e.setDropCompleted(true);
            }
        });
    }
}