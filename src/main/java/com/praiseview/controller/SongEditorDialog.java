package com.praiseview.controller;

import com.praiseview.model.Song;
import com.praiseview.model.Verse;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import org.controlsfx.control.CheckComboBox;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SongEditorDialog extends Dialog<Song> {

    private Map<String, Integer> typeCounter = new HashMap<>();

    public SongEditorDialog(Song songToEdit) {
        setTitle(songToEdit == null ? "Add New Song" : "Edit Song");
        setResizable(true);
        getDialogPane().setPrefSize(950, 820);

        VBox mainLayout = new VBox(12);
        mainLayout.setPadding(new Insets(15));

        // Basic Info
        TextField titleField = new TextField(songToEdit != null ? songToEdit.getTitle() : "");
        titleField.setPromptText("Song title");

        // Language and Category on one line
        HBox langCategoryBox = new HBox(15);
        
        ComboBox<String> languageCombo = new ComboBox<>();
        languageCombo.getItems().addAll("English", "Latin","Hindi", "Marathi", "Konkani", "Tamil");
        languageCombo.setValue(songToEdit != null && songToEdit.getLanguage() != null ? songToEdit.getLanguage() : "English");
        languageCombo.setPrefWidth(150);

        Label unicodeNoteLabel = new Label("Note: Paste only Unicode-compatible text. If the copied lyrics are not Unicode-compatible, convert them online first and paste the converted text.");
        unicodeNoteLabel.setWrapText(true);
        unicodeNoteLabel.setMaxWidth(520);
        unicodeNoteLabel.setStyle("-fx-text-fill: #a15c00; -fx-font-style: italic;");
        unicodeNoteLabel.setVisible(false);
        unicodeNoteLabel.setManaged(false);

        CheckComboBox<String> categoryCombo = new CheckComboBox<>();
        categoryCombo.getItems().addAll("Entrance Hymn", "Penitential Rite", "Gloria",
                "Responsorial Psalm", "Gospel Acclamation", "Offertory", "Sanctus (Holy Holy)", "Memorial Acclamation",
                "Lamb of God (Agnus Dei)", "Communion",
                "Meditation", "Recessional", "Adoration", "Marian Hymn", "Lenten Hymn","Advent Hymn",
                "Christmas Hymn", "Easter Hymn", "Holy Week", "Funeral", "Wedding");
        categoryCombo.setPrefWidth(250);
        
        // Load existing categories if editing

        if (songToEdit != null && songToEdit.getCategory() != null && !songToEdit.getCategory().isEmpty()) {
            String[] cats = songToEdit.getCategory().split(",");

            for (String cat : cats) {
                cat = cat.trim();
                int idx = categoryCombo.getItems().indexOf(cat);
                if (idx >= 0) {
                    categoryCombo.getCheckModel().check(idx);
                }
            }
        }

        Label langLabel = new Label("Language:");
        Label catLabel = new Label("Category:");
        langCategoryBox.getChildren().addAll(langLabel, languageCombo, catLabel, categoryCombo);

        Runnable updateUnicodeNote = () -> {
            String selectedLanguage = languageCombo.getValue();
            boolean showNote = selectedLanguage != null
                    && !"English".equalsIgnoreCase(selectedLanguage)
                    && !"Latin".equalsIgnoreCase(selectedLanguage);
            unicodeNoteLabel.setVisible(showNote);
            unicodeNoteLabel.setManaged(showNote);
        };
        languageCombo.valueProperty().addListener((obs, oldVal, newVal) -> updateUnicodeNote.run());
        updateUnicodeNote.run();

        // === Verse Section ===
        HBox verseSection = new HBox(15);

        // Left: Available Verses
        VBox availableBox = new VBox(10);
        availableBox.setPrefWidth(400);

        TextArea lyricsArea = new TextArea();
        lyricsArea.setPromptText("Paste lyrics here...");
        lyricsArea.setPrefHeight(250);
        lyricsArea.setWrapText(true);
        
        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("VERSE", "CHORUS", "BRIDGE", "PRE_CHORUS", "CODA");
        typeCombo.setValue("VERSE");
        
        // Don't apply custom font during editing - it can corrupt Unicode input
        // Use system default font for text editing to preserve correct character encoding

        Button addVerseBtn = new Button("Add Verse");
        Button deleteVerseBtn = new Button("Delete Selected");

        ListView<Verse> availableList = new ListView<>();
        availableList.setPrefHeight(200);
        
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

        // View and Edit selected verse lyrics
        TextArea viewVerseArea = new TextArea();
        viewVerseArea.setPromptText("Select a verse to view/edit its lyrics...");
        viewVerseArea.setWrapText(true);
        viewVerseArea.setEditable(true);
        viewVerseArea.setPrefHeight(150);

        // Show lyrics when verse selected
        availableList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                viewVerseArea.setText(newVal.getContent());
            } else {
                viewVerseArea.clear();
            }
        });

        // Update selected verse with new lyrics
        Button updateVerseBtn = new Button("Update Verse");
        updateVerseBtn.setOnAction(e -> {
            Verse selected = availableList.getSelectionModel().getSelectedItem();
            if (selected != null && !viewVerseArea.getText().trim().isEmpty()) {
                selected.setContent(viewVerseArea.getText().trim());
                availableList.refresh(); // Refresh list to reflect changes
            }
        });

        addVerseBtn.setOnAction(e -> {
            if (!lyricsArea.getText().trim().isEmpty()) {
                String type = typeCombo.getValue();
                int count = typeCounter.getOrDefault(type, 0) + 1;
                typeCounter.put(type, count);

                String label = type.replace("_", " ") + " " + count;

                Verse verse = new Verse(label, lyricsArea.getText().trim(), Verse.VerseType.valueOf(type));
                availableList.getItems().add(verse);
                lyricsArea.clear();
            }
        });

        // Delete verse button
        deleteVerseBtn.setOnAction(e -> {
            Verse selected = availableList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                availableList.getItems().remove(selected);
                viewVerseArea.clear();
            }
        });

        HBox addVerseButtonsBox = new HBox(8);
        addVerseButtonsBox.getChildren().addAll(addVerseBtn, deleteVerseBtn);

        HBox updateVerseButtonBox = new HBox(8);
        updateVerseButtonBox.getChildren().add(updateVerseBtn);

        availableBox.getChildren().addAll(
                new Label("Add New Verse"),
                new HBox(8, new Label("Type:"), typeCombo),
                lyricsArea,
                addVerseButtonsBox,
                new Separator(),
                new Label("Available Verses"),
                availableList,
                new Label("View/Edit Lyrics"),
                viewVerseArea,
                updateVerseButtonBox
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

        Button addToOrderBtn = new Button("Add to Performance Order");
        addToOrderBtn.setOnAction(e -> {
            Verse selected = availableList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                orderList.getItems().add(selected);
            }
        });

        Button removeFromOrderBtn = new Button("Remove from Order");
        removeFromOrderBtn.setOnAction(e -> {
            Verse selected = orderList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                orderList.getItems().remove(selected);
            }
        });

        Button moveUpBtn = new Button("Move Up");
        moveUpBtn.setOnAction(e -> {
            int idx = orderList.getSelectionModel().getSelectedIndex();
            if (idx > 0) {
                Verse item = orderList.getItems().remove(idx);
                orderList.getItems().add(idx - 1, item);
                orderList.getSelectionModel().select(idx - 1);
            }
        });

        Button moveDownBtn = new Button("Move Down");
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
                langCategoryBox,
                unicodeNoteLabel,
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
                
                // Get checked categories as comma-separated string
                List<String> checkedCategories = new ArrayList<>();
                for (String cat : categoryCombo.getItems()) {
                    if (categoryCombo.getCheckModel().isChecked(cat)) {
                        checkedCategories.add(cat);
                    }
                }
                song.setCategory(String.join(",", checkedCategories));
                
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