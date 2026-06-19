package com.praiseview.controller;

import com.praiseview.model.Song;
import com.praiseview.model.Verse;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SongEditorDialog extends Dialog<Song> {

    private Map<String, Integer> typeCounter = new HashMap<>();

    public SongEditorDialog(Song songToEdit) {
        setTitle(songToEdit == null ? "Add New Song" : "Edit Song");
        setResizable(true);
        getDialogPane().setPrefSize(950, 800);

        VBox mainLayout = new VBox(12);
        mainLayout.setPadding(new Insets(15));

        // Basic Info
        TextField titleField = new TextField(songToEdit != null ? songToEdit.getTitle() : "");
        titleField.setPromptText("Song title");
        
        TextField authorField = new TextField(songToEdit != null && songToEdit.getAuthor() != null ? songToEdit.getAuthor() : "");
        authorField.setPromptText("Author");
        
        TextField composerField = new TextField(songToEdit != null && songToEdit.getComposer() != null ? songToEdit.getComposer() : "");
        composerField.setPromptText("Composer");

        // Language and Category on one line
        HBox langCategoryBox = new HBox(15);
        
        ComboBox<String> languageCombo = new ComboBox<>();
        languageCombo.getItems().addAll("English", "Hindi", "Marathi", "Konkani", "Tamil");
        languageCombo.setValue(songToEdit != null && songToEdit.getLanguage() != null ? songToEdit.getLanguage() : "English");
        languageCombo.setPrefWidth(150);

        // MultiCombo for categories (using CheckComboBox concept via custom handling)
        ComboBox<String> categoryCombo = new ComboBox<>();
        categoryCombo.getItems().addAll("Entrance Hymn", "Penitential Rite", "Gloria",
                "Responsorial Psalm", "Gospel Acclamation", "Offertory", "Communion",
                "Meditation", "Recessional", "Adoration", "Marian Hymn", "Lenten Hymn",
                "Christmas Hymn", "Easter Hymn", "Holy Week", "Funeral", "Wedding");
        categoryCombo.setValue(songToEdit != null && songToEdit.getCategory() != null ? songToEdit.getCategory() : "");
        categoryCombo.setPrefWidth(250);

        Label langLabel = new Label("Language:");
        Label catLabel = new Label("Category:");
        langCategoryBox.getChildren().addAll(langLabel, languageCombo, catLabel, categoryCombo);

        // === Verse Section ===
        HBox verseSection = new HBox(15);

        // Left: Available Verses
        VBox availableBox = new VBox(10);
        availableBox.setPrefWidth(380);

        TextArea lyricsArea = new TextArea();
        lyricsArea.setPromptText("Paste lyrics here...");
        lyricsArea.setPrefHeight(300);
        lyricsArea.setWrapText(true);

        TextArea editVerseArea = new TextArea();
        editVerseArea.setPromptText("Select a verse to edit...");
        editVerseArea.setWrapText(true);
        editVerseArea.setPrefHeight(200);

        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("VERSE", "CHORUS", "BRIDGE", "PRE_CHORUS", "CODA");
        typeCombo.setValue("VERSE");

        ComboBox<String> editTypeCombo = new ComboBox<>();
        editTypeCombo.getItems().addAll("VERSE", "CHORUS", "BRIDGE", "PRE_CHORUS", "CODA");
        editTypeCombo.setValue("VERSE");

        Button addVerseBtn = new Button("➕ Add Verse");
        Button updateVerseBtn = new Button("✏️ Update Selected");
        Button deleteVerseBtn = new Button("🗑️ Delete Selected");

        ListView<Verse> availableList = new ListView<>();
        availableList.setPrefHeight(250);
        
        // Initialize typeCounter from existing song
        if (songToEdit != null && !songToEdit.getVerses().isEmpty()) {
            availableList.getItems().addAll(songToEdit.getVerses());
            for (Verse v : songToEdit.getVerses()) {
                String typeStr = v.getType().name();
                String[] parts = v.getLabel().split(" ");
                if (parts.length > 0) {
                    try {
                        int count = Integer.parseInt(parts[parts.length - 1]);
                        typeCounter.put(typeStr, Math.max(typeCounter.getOrDefault(typeStr, 0), count));
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
                editTypeCombo.setValue(newVal.getType().name());
            }
        });

        // Update verse button
        updateVerseBtn.setOnAction(e -> {
            Verse selected = availableList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                selected.setContent(editVerseArea.getText());
                selected.setType(Verse.VerseType.valueOf(editTypeCombo.getValue()));
                availableList.refresh();
            }
        });

        // Delete verse button
        deleteVerseBtn.setOnAction(e -> {
            Verse selected = availableList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                availableList.getItems().remove(selected);
                editVerseArea.clear();
            }
        });

        HBox addVerseButtonsBox = new HBox(8);
        addVerseButtonsBox.getChildren().addAll(addVerseBtn, deleteVerseBtn);

        HBox updateVerseBox = new HBox(8);
        updateVerseBox.getChildren().addAll(
                new VBox(5, new Label("Type:"), editTypeCombo),
                updateVerseBtn
        );

        availableBox.getChildren().addAll(
                new Label("Add New Verse"),
                new HBox(8, new Label("Type:"), typeCombo),
                lyricsArea,
                addVerseButtonsBox,
                new Separator(),
                new Label("Available Verses"),
                availableList,
                new Label("Edit Selected Verse"),
                editVerseArea,
                updateVerseBox
        );

        // Right: Performance Order
        VBox orderBox = new VBox(10);
        orderBox.setPrefWidth(380);

        ListView<Verse> orderList = new ListView<>();
        orderList.setPrefHeight(450);
        
        // Load existing verse order
        if (songToEdit != null && !songToEdit.getVerseOrder().isEmpty()) {
            for (int idx : songToEdit.getVerseOrder()) {
                if (idx < songToEdit.getVerses().size()) {
                    orderList.getItems().add(songToEdit.getVerses().get(idx));
                }
            }
        }

        Button addToOrderBtn = new Button("➕ Add to Performance Order");
        addToOrderBtn.setOnAction(e -> {
            Verse selected = availableList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                orderList.getItems().add(selected);
            }
        });

        Button removeFromOrderBtn = new Button("🗑️ Remove from Order");
        removeFromOrderBtn.setOnAction(e -> {
            Verse selected = orderList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                orderList.getItems().remove(selected);
            }
        });

        Button moveUpBtn = new Button("⬆️ Move Up");
        moveUpBtn.setOnAction(e -> {
            int idx = orderList.getSelectionModel().getSelectedIndex();
            if (idx > 0) {
                Verse item = orderList.getItems().remove(idx);
                orderList.getItems().add(idx - 1, item);
                orderList.getSelectionModel().select(idx - 1);
            }
        });

        Button moveDownBtn = new Button("⬇️ Move Down");
        moveDownBtn.setOnAction(e -> {
            int idx = orderList.getSelectionModel().getSelectedIndex();
            if (idx >= 0 && idx < orderList.getItems().size() - 1) {
                Verse item = orderList.getItems().remove(idx);
                orderList.getItems().add(idx + 1, item);
                orderList.getSelectionModel().select(idx + 1);
            }
        });

        HBox orderButtonsBox = new HBox(8);
        orderButtonsBox.getChildren().addAll(moveUpBtn, moveDownBtn, removeFromOrderBtn);

        orderBox.getChildren().addAll(
                new Label("Performance Order (Drag or use buttons)"),
                orderList,
                addToOrderBtn,
                orderButtonsBox
        );

        setupDragAndDrop(orderList);

        verseSection.getChildren().addAll(availableBox, orderBox);

        mainLayout.getChildren().addAll(
                new Label("Title:"), titleField,
                new Label("Author:"), authorField,
                new Label("Composer:"), composerField,
                langCategoryBox,
                new Separator(),
                verseSection
        );

        ScrollPane scrollPane = new ScrollPane(mainLayout);
        scrollPane.setFitToWidth(true);
        getDialogPane().setContent(scrollPane);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                Song song = songToEdit != null ? songToEdit : new Song();
                song.setTitle(titleField.getText().trim());
                song.setLanguage(languageCombo.getValue());
                song.setCategory(categoryCombo.getValue());
                song.setAuthor(authorField.getText().trim());
                song.setComposer(composerField.getText().trim());
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
                e.consume();
            }
        });

        listView.setOnDragOver(e -> {
            e.acceptTransferModes(TransferMode.MOVE);
            e.consume();
        });

        listView.setOnDragEntered(e -> {
            listView.setStyle("-fx-border-color: #0078d4; -fx-border-width: 2;");
        });

        listView.setOnDragExited(e -> {
            listView.setStyle("");
        });

        listView.setOnDragDropped(e -> {
            Dragboard db = e.getDragboard();
            if (db.hasString()) {
                try {
                    int draggedIndex = Integer.parseInt(db.getString());
                    int dropIndex = listView.getSelectionModel().getSelectedIndex();
                    if (dropIndex < 0) dropIndex = listView.getItems().size();
                    
                    if (draggedIndex != dropIndex && draggedIndex >= 0 && draggedIndex < listView.getItems().size()) {
                        Verse dragged = listView.getItems().remove(draggedIndex);
                        if (dropIndex > draggedIndex) dropIndex--;
                        listView.getItems().add(dropIndex, dragged);
                        listView.getSelectionModel().select(dropIndex);
                        e.setDropCompleted(true);
                    }
                } catch (NumberFormatException ex) {
                    e.setDropCompleted(false);
                }
            }
            e.consume();
        });
    }
}
