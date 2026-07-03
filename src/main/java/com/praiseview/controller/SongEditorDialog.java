package com.praiseview.controller;

import com.praiseview.model.Song;
import com.praiseview.model.Verse;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Priority;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import org.controlsfx.control.CheckComboBox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SongEditorDialog extends Dialog<Song> {

    private int currentStep = 1;
    private final int maxSteps = 4;
    private Song workingSong;
    private boolean isEditing;
    
    // UI Components
    private TextField titleField;
    private ComboBox<String> languageCombo;
    private CheckComboBox<String> categoryCombo;
    private Label unicodeNoteLabel;
    
    // Step 2 - Verses
    private TextArea lyricsArea;
    private ComboBox<String> typeCombo;
    private ListView<Verse> verseList;
    private TextArea viewVerseArea;
    private Button editVerseBtn;
    private Map<String, Integer> typeCounter;
    
    // Step 3 - Performance Order
    private ListView<Verse> orderList;
    
    // Wizard controls
    private Button nextBtn;
    private Button prevBtn;
    private Button finishBtn;
    private Label stepIndicator;
    private StackPane stepContent;

    public SongEditorDialog(Song songToEdit) {
        isEditing = songToEdit != null;
        workingSong = isEditing ? songToEdit : new Song();
        typeCounter = new HashMap<>();
        
        setTitle(isEditing ? "Edit Song" : "Add New Song");
        setResizable(true);
        Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();
        double dialogWidth = Math.min(630, visualBounds.getWidth() * 0.72);
        double dialogHeight = Math.min(585, visualBounds.getHeight() * 0.76);
        getDialogPane().setPrefSize(dialogWidth, dialogHeight);
        getDialogPane().setMinSize(450, 405);

        VBox mainLayout = new VBox(10);
        mainLayout.setPadding(new Insets(15));
        
        // Step indicator
        stepIndicator = new Label("Step 1 of 4: Basic Information");
        stepIndicator.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        
        // Content area
        stepContent = new StackPane();
        stepContent.setPrefHeight(400);
        VBox.setVgrow(stepContent, Priority.ALWAYS);
        
        mainLayout.getChildren().addAll(
            stepIndicator,
            new Separator(),
            stepContent,
            new Separator(),
            createButtonBar()
        );
        
        ScrollPane scrollPane = new ScrollPane(mainLayout);
        scrollPane.setFitToWidth(true);
        getDialogPane().setContent(scrollPane);
        getDialogPane().getButtonTypes().clear();
        
        setResultConverter(btn -> null);
        
        showStep(1);
    }

    private HBox createButtonBar() {
        HBox buttonBar = new HBox(10);
        buttonBar.setStyle("-fx-alignment: center-right;");
        
        prevBtn = new Button("← Previous");
        prevBtn.setPrefWidth(100);
        prevBtn.setOnAction(e -> previousStep());
        
        nextBtn = new Button("Next →");
        nextBtn.setPrefWidth(100);
        nextBtn.setOnAction(e -> nextStep());
        
        Button cancelBtn = new Button("Cancel");
        cancelBtn.setPrefWidth(100);
        cancelBtn.setOnAction(e -> getDialogPane().getScene().getWindow().hide());
        
        finishBtn = new Button("Finish");
        finishBtn.setPrefWidth(100);
        finishBtn.setOnAction(e -> finishWizard());
        finishBtn.setVisible(false);
        finishBtn.setManaged(false);
        
        buttonBar.getChildren().addAll(prevBtn, nextBtn, finishBtn, cancelBtn);
        return buttonBar;
    }

    private void showStep(int step) {
        currentStep = step;
        stepContent.getChildren().clear();
        
        switch (step) {
            case 1 -> stepContent.getChildren().add(createStep1());
            case 2 -> stepContent.getChildren().add(createStep2());
            case 3 -> stepContent.getChildren().add(createStep3());
            case 4 -> stepContent.getChildren().add(createStep4());
        }
        
        updateStepIndicator();
        updateButtonStates();
    }

    private void updateStepIndicator() {
        String[] stepNames = {"Basic Information", "Verses", "Performance Order", "Confirmation"};
        stepIndicator.setText("Step " + currentStep + " of 4: " + stepNames[currentStep - 1]);
    }

    private void updateButtonStates() {
        prevBtn.setDisable(currentStep == 1);
        
        if (currentStep == 4) {
            nextBtn.setVisible(false);
            nextBtn.setManaged(false);
            finishBtn.setVisible(true);
            finishBtn.setManaged(true);
        } else {
            nextBtn.setVisible(true);
            nextBtn.setManaged(true);
            finishBtn.setVisible(false);
            finishBtn.setManaged(false);
        }
    }

    private VBox createStep1() {
        VBox step1 = new VBox(12);
        step1.setPadding(new Insets(15));
        step1.setStyle("-fx-border-color: #e0e0e0; -fx-border-radius: 5; -fx-background-color: #f9f9f9;");
        
        // Title
        Label titleLabel = new Label("Song Title:");
        titleLabel.setStyle("-fx-font-weight: bold;");
        titleField = new TextField(workingSong.getTitle() != null ? workingSong.getTitle() : "");
        titleField.setPromptText("Enter song title");
        titleField.setPrefHeight(35);
        titleField.setStyle("-fx-font-size: 12px; -fx-padding: 8px;");
        
        // Language
        Label languageLabel = new Label("Language:");
        languageLabel.setStyle("-fx-font-weight: bold;");
        languageCombo = new ComboBox<>();
        languageCombo.getItems().addAll("English", "Latin", "Hindi", "Marathi", "Konkani", "Tamil");
        languageCombo.setValue(workingSong.getLanguage() != null ? workingSong.getLanguage() : "English");
        languageCombo.setPrefHeight(35);
        
        // Unicode note
        unicodeNoteLabel = new Label("Note: Paste only Unicode-compatible text. If copied lyrics are not Unicode-compatible, convert them online first.");
        unicodeNoteLabel.setWrapText(true);
        unicodeNoteLabel.setStyle("-fx-text-fill: #a15c00; -fx-font-style: italic; -fx-font-size: 11px;");
        unicodeNoteLabel.setVisible(false);
        unicodeNoteLabel.setManaged(false);
        
        languageCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            boolean showNote = newVal != null && !"English".equalsIgnoreCase(newVal) && !"Latin".equalsIgnoreCase(newVal);
            unicodeNoteLabel.setVisible(showNote);
            unicodeNoteLabel.setManaged(showNote);
        });
        
        // Category
        Label categoryLabel = new Label("Category:");
        categoryLabel.setStyle("-fx-font-weight: bold;");
        categoryCombo = new CheckComboBox<>();
        categoryCombo.getItems().addAll("Entrance Hymn", "Penitential Rite", "Gloria",
                "Responsorial Psalm", "Gospel Acclamation", "Offertory", "Sanctus (Holy Holy)", "Memorial Acclamation",
                "Lamb of God (Agnus Dei)", "Communion", "Meditation", "Recessional", "Adoration", 
                "Marian Hymn", "Lenten Hymn", "Advent Hymn", "Christmas Hymn", "Easter Hymn", 
                "Holy Week", "Funeral", "Wedding");
        categoryCombo.setPrefHeight(35);
        
        // Load existing categories
        if (isEditing && workingSong.getCategory() != null && !workingSong.getCategory().isEmpty()) {
            String[] cats = workingSong.getCategory().split(",");
            for (String cat : cats) {
                cat = cat.trim();
                int idx = categoryCombo.getItems().indexOf(cat);
                if (idx >= 0) {
                    categoryCombo.getCheckModel().check(idx);
                }
            }
        }
        
        step1.getChildren().addAll(
            titleLabel, titleField,
            languageLabel, languageCombo,
            unicodeNoteLabel,
            categoryLabel, categoryCombo
        );
        
        return step1;
    }

    private VBox createStep2() {
        VBox step2 = new VBox(12);
        step2.setPadding(new Insets(15));
        step2.setStyle("-fx-border-color: #e0e0e0; -fx-border-radius: 5; -fx-background-color: #f9f9f9;");
        
        if (isEditing) {
            // For editing: show existing verses with Add/Edit buttons
            Label titleLabel = new Label("Existing Verses & Chorus");
            titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
            
            HBox verseControlsBox = new HBox(8);
            Button addNewVerseBtn = new Button("+ Add New Verse");
            editVerseBtn = new Button("Edit Selected");
            Button deleteVerseBtn = new Button("Delete Selected");
            
            verseList = new ListView<>();
            verseList.setPrefHeight(200);
            verseList.setCellFactory(lv -> new ListCell<>() {
                @Override
                protected void updateItem(Verse item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getLabel());
                }
            });
            
            // Initialize from existing song
            if (workingSong.getVerses() != null) {
                verseList.getItems().addAll(workingSong.getVerses());
                for (Verse v : workingSong.getVerses()) {
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
            
            // View/Edit lyrics
            Label lyricsLabel = new Label("Lyrics:");
            lyricsLabel.setStyle("-fx-font-weight: bold;");
            viewVerseArea = new TextArea();
            viewVerseArea.setPromptText("Select a verse to view/edit its lyrics...");
            viewVerseArea.setWrapText(true);
            viewVerseArea.setEditable(true);
            viewVerseArea.setPrefHeight(150);
            
            verseList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    viewVerseArea.setText(newVal.getContent());
                } else {
                    viewVerseArea.clear();
                }
            });
            
            editVerseBtn.setOnAction(e -> {
                Verse selected = verseList.getSelectionModel().getSelectedItem();
                if (selected != null && !viewVerseArea.getText().trim().isEmpty()) {
                    selected.setContent(viewVerseArea.getText().trim());
                    verseList.refresh();
                }
            });
            
            addNewVerseBtn.setOnAction(e -> showAddVerseDialog());
            
            deleteVerseBtn.setOnAction(e -> {
                Verse selected = verseList.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    verseList.getItems().remove(selected);
                    viewVerseArea.clear();
                }
            });
            
            verseControlsBox.getChildren().addAll(addNewVerseBtn, editVerseBtn, deleteVerseBtn);
            
            step2.getChildren().addAll(
                titleLabel,
                verseList,
                verseControlsBox,
                new Separator(),
                lyricsLabel,
                viewVerseArea
            );
        } else {
            // For new song: add verses step by step
            Label titleLabel = new Label("Add Verses & Chorus");
            titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
            
            HBox typeBox = new HBox(10);
            Label typeLabel = new Label("Verse Type:");
            typeLabel.setStyle("-fx-font-weight: bold;");
            typeCombo = new ComboBox<>();
            typeCombo.getItems().addAll("VERSE", "CHORUS", "BRIDGE", "PRE_CHORUS", "CODA");
            typeCombo.setValue("VERSE");
            typeCombo.setPrefWidth(150);
            typeBox.getChildren().addAll(typeLabel, typeCombo);
            
            Label lyricsLabel = new Label("Lyrics:");
            lyricsLabel.setStyle("-fx-font-weight: bold;");
            lyricsArea = new TextArea();
            lyricsArea.setPromptText("Paste or type lyrics here...");
            lyricsArea.setWrapText(true);
            lyricsArea.setPrefHeight(200);
            
            Button addVerseBtn = new Button("Add This Verse");
            Button clearBtn = new Button("Clear");
            HBox addButtonBox = new HBox(8);
            addButtonBox.getChildren().addAll(addVerseBtn, clearBtn);
            
            addVerseBtn.setOnAction(e -> {
                if (!lyricsArea.getText().trim().isEmpty()) {
                    String type = typeCombo.getValue();
                    int count = typeCounter.getOrDefault(type, 0) + 1;
                    typeCounter.put(type, count);
                    
                    String label = type.replace("_", " ") + " " + count;
                    Verse verse = new Verse(label, lyricsArea.getText().trim(), Verse.VerseType.valueOf(type));
                    
                    verseList.getItems().add(verse);
                    lyricsArea.clear();
                    typeCombo.setValue("VERSE");
                }
            });
            
            clearBtn.setOnAction(e -> lyricsArea.clear());
            
            // Verses list
            Label verseListLabel = new Label("Added Verses:");
            verseListLabel.setStyle("-fx-font-weight: bold;");
            verseList = new ListView<>();
            verseList.setPrefHeight(150);
            verseList.setCellFactory(lv -> new ListCell<>() {
                @Override
                protected void updateItem(Verse item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getLabel());
                }
            });
            
            step2.getChildren().addAll(
                titleLabel,
                typeBox,
                lyricsLabel,
                lyricsArea,
                addButtonBox,
                new Separator(),
                verseListLabel,
                verseList
            );
        }
        
        return step2;
    }

    private VBox createStep3() {
        VBox step3 = new VBox(12);
        step3.setPadding(new Insets(15));
        step3.setStyle("-fx-border-color: #e0e0e0; -fx-border-radius: 5; -fx-background-color: #f9f9f9;");
        
        Label titleLabel = new Label("Performance Order");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        
        Label instructionLabel = new Label("Drag verses to arrange or use buttons to organize the performance order:");
        instructionLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 11px;");
        instructionLabel.setWrapText(true);
        
        // Available verses on left
        Label availableLabel = new Label("Available Verses:");
        availableLabel.setStyle("-fx-font-weight: bold;");
        ListView<Verse> availableList = new ListView<>();
        availableList.setPrefHeight(200);
        availableList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Verse item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getLabel());
            }
        });
        availableList.getItems().addAll(verseList.getItems());
        
        // Performance order on right
        Label orderLabel = new Label("Performance Order:");
        orderLabel.setStyle("-fx-font-weight: bold;");
        orderList = new ListView<>();
        orderList.setPrefHeight(200);
        orderList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Verse item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getLabel());
            }
        });
        
        // Load existing order if editing
        if (isEditing && workingSong.getVerseOrder() != null && !workingSong.getVerseOrder().isEmpty()) {
            for (int idx : workingSong.getVerseOrder()) {
                if (idx < verseList.getItems().size()) {
                    orderList.getItems().add(verseList.getItems().get(idx));
                }
            }
        }
        
        setupDragAndDrop(orderList);
        
        // Control buttons
        Button addToOrderBtn = new Button("→ Add to Order");
        Button removeFromOrderBtn = new Button("← Remove");
        Button moveUpBtn = new Button("↑ Move Up");
        Button moveDownBtn = new Button("↓ Move Down");
        Button clearOrderBtn = new Button("Clear Order");
        
        addToOrderBtn.setOnAction(e -> {
            Verse selected = availableList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                orderList.getItems().add(selected);
            }
        });
        
        removeFromOrderBtn.setOnAction(e -> {
            Verse selected = orderList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                orderList.getItems().remove(selected);
            }
        });
        
        moveUpBtn.setOnAction(e -> {
            int idx = orderList.getSelectionModel().getSelectedIndex();
            if (idx > 0) {
                Verse item = orderList.getItems().remove(idx);
                orderList.getItems().add(idx - 1, item);
                orderList.getSelectionModel().select(idx - 1);
            }
        });
        
        moveDownBtn.setOnAction(e -> {
            int idx = orderList.getSelectionModel().getSelectedIndex();
            if (idx >= 0 && idx < orderList.getItems().size() - 1) {
                Verse item = orderList.getItems().remove(idx);
                orderList.getItems().add(idx + 1, item);
                orderList.getSelectionModel().select(idx + 1);
            }
        });
        
        clearOrderBtn.setOnAction(e -> orderList.getItems().clear());
        
        HBox buttonBox = new HBox(8);
        buttonBox.getChildren().addAll(addToOrderBtn, removeFromOrderBtn, moveUpBtn, moveDownBtn, clearOrderBtn);
        
        HBox listsBox = new HBox(15);
        VBox availableBox = new VBox(8);
        availableBox.getChildren().addAll(availableLabel, availableList);
        HBox.setHgrow(availableBox, Priority.ALWAYS);
        
        VBox orderBox = new VBox(8);
        orderBox.getChildren().addAll(orderLabel, orderList);
        HBox.setHgrow(orderBox, Priority.ALWAYS);
        
        listsBox.getChildren().addAll(availableBox, orderBox);
        
        step3.getChildren().addAll(
            titleLabel,
            instructionLabel,
            listsBox,
            buttonBox
        );
        
        return step3;
    }

    private VBox createStep4() {
        VBox step4 = new VBox(12);
        step4.setPadding(new Insets(15));
        step4.setStyle("-fx-border-color: #e0e0e0; -fx-border-radius: 5; -fx-background-color: #f9f9f9;");
        
        Label titleLabel = new Label("Confirmation");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        
        TextArea summaryArea = new TextArea();
        summaryArea.setEditable(false);
        summaryArea.setWrapText(true);
        summaryArea.setPrefHeight(300);
        
        StringBuilder summary = new StringBuilder();
        summary.append("SONG SUMMARY\n");
        summary.append("=".repeat(50)).append("\n\n");
        summary.append("Title: ").append(titleField.getText().trim()).append("\n");
        summary.append("Language: ").append(languageCombo.getValue()).append("\n");
        
        List<String> checkedCategories = new ArrayList<>();
        for (String cat : categoryCombo.getItems()) {
            if (categoryCombo.getCheckModel().isChecked(cat)) {
                checkedCategories.add(cat);
            }
        }
        summary.append("Categories: ").append(checkedCategories.isEmpty() ? "None" : String.join(", ", checkedCategories)).append("\n\n");
        
        summary.append("VERSES (" + verseList.getItems().size() + ")\n");
        summary.append("-".repeat(50)).append("\n");
        for (Verse v : verseList.getItems()) {
            summary.append("• ").append(v.getLabel()).append("\n");
        }
        
        summary.append("\nPERFORMANCE ORDER (" + orderList.getItems().size() + ")\n");
        summary.append("-".repeat(50)).append("\n");
        for (int i = 0; i < orderList.getItems().size(); i++) {
            summary.append((i + 1)).append(". ").append(orderList.getItems().get(i).getLabel()).append("\n");
        }
        
        if (orderList.getItems().isEmpty()) {
            summary.append("⚠ No verses in performance order\n");
        }
        
        summaryArea.setText(summary.toString());
        
        step4.getChildren().addAll(
            titleLabel,
            summaryArea
        );
        
        return step4;
    }

    private void nextStep() {
        if (!validateStep(currentStep)) {
            return;
        }
        
        if (currentStep < maxSteps) {
            showStep(currentStep + 1);
        }
    }

    private void previousStep() {
        if (currentStep > 1) {
            showStep(currentStep - 1);
        }
    }

    private boolean validateStep(int step) {
        switch (step) {
            case 1:
                if (titleField.getText().trim().isEmpty()) {
                    showAlert("Validation Error", "Please enter a song title.");
                    return false;
                }
                return true;
            case 2:
                if (verseList.getItems().isEmpty()) {
                    showAlert("Validation Error", "Please add at least one verse.");
                    return false;
                }
                return true;
            case 3:
                if (orderList.getItems().isEmpty()) {
                    showAlert("Validation Error", "Please add at least one verse to the performance order.");
                    return false;
                }
                return true;
            default:
                return true;
        }
    }

    private void finishWizard() {
        Song song = isEditing ? workingSong : new Song();
        song.setTitle(titleField.getText().trim());
        song.setLanguage(languageCombo.getValue());
        
        List<String> checkedCategories = new ArrayList<>();
        for (String cat : categoryCombo.getItems()) {
            if (categoryCombo.getCheckModel().isChecked(cat)) {
                checkedCategories.add(cat);
            }
        }
        song.setCategory(String.join(",", checkedCategories));
        
        song.setVerses(new ArrayList<>(verseList.getItems()));
        song.setVerseOrderFromList(orderList.getItems());
        
        setResult(song);
        getDialogPane().getScene().getWindow().hide();
    }

    private void showAddVerseDialog() {
        Dialog<Verse> dialog = new Dialog<>();
        dialog.setTitle("Add New Verse");
        dialog.setResizable(true);
        dialog.getDialogPane().setPrefSize(500, 350);
        
        VBox content = new VBox(10);
        content.setPadding(new Insets(15));
        
        Label typeLabel = new Label("Verse Type:");
        typeLabel.setStyle("-fx-font-weight: bold;");
        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("VERSE", "CHORUS", "BRIDGE", "PRE_CHORUS", "CODA");
        typeCombo.setValue("VERSE");
        
        Label lyricsLabel = new Label("Lyrics:");
        lyricsLabel.setStyle("-fx-font-weight: bold;");
        TextArea lyricsArea = new TextArea();
        lyricsArea.setPromptText("Enter verse lyrics...");
        lyricsArea.setWrapText(true);
        lyricsArea.setPrefHeight(200);
        
        content.getChildren().addAll(
            typeLabel, typeCombo,
            lyricsLabel, lyricsArea
        );
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK && !lyricsArea.getText().trim().isEmpty()) {
                String type = typeCombo.getValue();
                int count = typeCounter.getOrDefault(type, 0) + 1;
                typeCounter.put(type, count);
                String label = type.replace("_", " ") + " " + count;
                return new Verse(label, lyricsArea.getText().trim(), Verse.VerseType.valueOf(type));
            }
            return null;
        });
        
        dialog.showAndWait().ifPresent(verse -> {
            verseList.getItems().add(verse);
        });
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void setupDragAndDrop(ListView<Verse> listView) {
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

        listView.setOnDragEntered(e -> listView.setStyle("-fx-border-color: #0078d4; -fx-border-width: 2;"));
        listView.setOnDragExited(e -> listView.setStyle(""));

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
