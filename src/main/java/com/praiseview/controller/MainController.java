package com.praiseview.controller;

import com.praiseview.PraiseViewApp;
import com.praiseview.db.DatabaseService;
import com.praiseview.model.ServiceItem;
import com.praiseview.model.Song;
import com.praiseview.model.Verse;
import com.praiseview.service.JsonService;
import com.praiseview.util.AppLogger;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.stage.FileChooser;

import java.io.File;

public class MainController {

    // Left: Service Planner
    @FXML private ListView<ServiceItem> servicePlannerList;
    @FXML private Button editSongButton;

    // Center: Editor + Preview
    @FXML private TextArea itemEditorArea;
    @FXML private StackPane livePreviewPane;
    @FXML private Label stageViewTitle;
    @FXML private TextFlow livePreviewText;
    
    @FXML private StackPane nextItemPreviewPane;
    @FXML private Label nextItemTitle;
    @FXML private TextFlow nextItemPreviewText;

    // Right: Controls
    @FXML private Button projectButton, blackoutButton, clearButton;
    @FXML private Button nextVerseButton, prevVerseButton;
    @FXML private ListView<VerseDisplayItem> currentSongVersesList;

    // Bottom: Library
    @FXML private ListView<Song> songLibraryList;
    @FXML private TextField searchField;
    @FXML private Button addSongButton;

    private DatabaseService dbService = new DatabaseService();
    private JsonService jsonService = new JsonService();

    private ObservableList<Song> allSongs = FXCollections.observableArrayList();
    private FilteredList<Song> filteredSongs;
    private ObservableList<ServiceItem> serviceQueue = FXCollections.observableArrayList();

    private int currentQueueIndex = -1;
    private int currentVersePosition = 0;
    private javafx.scene.Scene scene;

    public void setScene(javafx.scene.Scene scene) {
        this.scene = scene;
    }
    @FXML
    public void initialize() {
        loadSongs();

        // Setup Song Library with Search
        filteredSongs = new FilteredList<>(allSongs, p -> true);
        songLibraryList.setItems(filteredSongs);

        searchField.textProperty().addListener((obs, old, newVal) -> {
            filteredSongs.setPredicate(song -> {
                if (newVal == null || newVal.isEmpty()) return true;
                String lower = newVal.toLowerCase();
                return song.getTitle().toLowerCase().contains(lower) ||
                        (song.getCategory() != null && song.getCategory().toLowerCase().contains(lower));
            });
        });

        // Setup Service Planner
        servicePlannerList.setItems(serviceQueue);

        // Context menu for service list
        ContextMenu serviceContextMenu = new ContextMenu();
        MenuItem previewItem = new MenuItem("Preview");
        MenuItem deleteItem = new MenuItem("Delete");
        serviceContextMenu.getItems().addAll(previewItem, deleteItem);

        previewItem.setOnAction(e -> {
            ServiceItem selected = servicePlannerList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                updateNextItemPreview(selected);
            }
        });

        deleteItem.setOnAction(e -> {
            int selectedIdx = servicePlannerList.getSelectionModel().getSelectedIndex();
            if (selectedIdx >= 0) {
                serviceQueue.remove(selectedIdx);
                // Reset if we deleted the current item
                if (currentQueueIndex >= serviceQueue.size()) {
                    currentQueueIndex = -1;
                }
                AppLogger.log("Song removed from service order");
            }
        });

        // Handle clicks on service items (double-click to start, right-click for context menu)
        servicePlannerList.setOnMouseClicked(e -> {
            ServiceItem selected = servicePlannerList.getSelectionModel().getSelectedItem();
            if (e.getClickCount() == 2 && selected != null) {
                // Double-click: start from that song
                currentQueueIndex = servicePlannerList.getSelectionModel().getSelectedIndex();
                currentVersePosition = 0;
                startProjection();
            } else if (e.getButton() == javafx.scene.input.MouseButton.SECONDARY && selected != null) {
                // Right-click: show context menu
                serviceContextMenu.show(servicePlannerList, e.getScreenX(), e.getScreenY());
                e.consume();
            }
        });

        // Drag & Drop from Library to Service Order
        setupDragAndDrop();

        // Button Actions
        addSongButton.setOnAction(e -> openSongEditor(null));
        projectButton.setOnAction(e -> startProjection());
        blackoutButton.setOnAction(e -> blackout());
        clearButton.setOnAction(e -> clearScreen());
        editSongButton.setOnAction(e -> editSelectedSong());

        // Verse Navigation
        if (nextVerseButton != null) nextVerseButton.setOnAction(e -> nextVerse());
        if (prevVerseButton != null) prevVerseButton.setOnAction(e -> previousVerse());

        // Context menu for song library
        ContextMenu libraryContextMenu = new ContextMenu();
        MenuItem previewLibraryItem = new MenuItem("Preview");
        libraryContextMenu.getItems().add(previewLibraryItem);

        previewLibraryItem.setOnAction(e -> {
            Song selected = songLibraryList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                // Create a temporary ServiceItem to preview
                ServiceItem tempItem = new ServiceItem(selected);
                updateNextItemPreview(tempItem);
            }
        });

        songLibraryList.setOnMouseClicked(e -> {
            if (e.getButton() == javafx.scene.input.MouseButton.SECONDARY) {
                Song selected = songLibraryList.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    libraryContextMenu.show(songLibraryList, e.getScreenX(), e.getScreenY());
                    e.consume();
                }
            }
        });

        // Add scene-level arrow key handler (global)
        livePreviewPane.setFocusTraversable(true);
    }

    public void setupSceneKeyHandler() {
        // Called from PraiseViewApp after scene is set
        if (scene != null) {
            scene.setOnKeyPressed(this::handleArrowKey);
        }
    }

    private void handleArrowKey(KeyEvent e) {
        if (e.getCode() == KeyCode.LEFT) {
            previousVerse();
            e.consume();
        } else if (e.getCode() == KeyCode.RIGHT) {
            nextVerse();
            e.consume();
        }
    }

    private void loadSongs() {
        allSongs.setAll(dbService.loadAllSongs());
        songLibraryList.refresh();   // Add this line
    }

    private void setupDragAndDrop() {
        songLibraryList.setOnDragDetected(e -> {
            Song selected = songLibraryList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                Dragboard db = songLibraryList.startDragAndDrop(TransferMode.COPY);
                ClipboardContent content = new ClipboardContent();
                content.putString(selected.getId());
                db.setContent(content);
                e.consume();
            }
        });

        servicePlannerList.setOnDragOver(e -> {
            if (e.getDragboard().hasString()) {
                e.acceptTransferModes(TransferMode.COPY);
            }
        });

        servicePlannerList.setOnDragDropped(e -> {
            Song song = songLibraryList.getSelectionModel().getSelectedItem();
            if (song != null) {
                serviceQueue.add(new ServiceItem(song));
                // If this is the first item, set currentQueueIndex to 0
                if (currentQueueIndex == -1 && serviceQueue.size() == 1) {
                    currentQueueIndex = 0;
                }
                servicePlannerList.refresh();
                e.setDropCompleted(true);
            }
        });
    }

    private void openSongEditor(Song song) {
        SongEditorDialog dialog = new SongEditorDialog(song);
        dialog.showAndWait().ifPresent(result -> {
            dbService.saveSong(result);
            loadSongs();
            AppLogger.log("Song added/updated: " + result.getTitle());
        });
    }

    private void editSelectedSong() {
        Song selected = songLibraryList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            openSongEditor(selected);
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Please select a song in the library to edit.");
            alert.show();
        }
    }

    private void startProjection() {
        if (serviceQueue.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Please add songs to the service order first.");
            alert.show();
            return;
        }
        if (currentQueueIndex == -1) currentQueueIndex = 0;
        currentVersePosition = 0;
        showCurrentItem();
    }

    private void showCurrentItem() {

        ServiceItem item = serviceQueue.get(currentQueueIndex);
        Song song = item.getSong();

        if (song == null || song.getVerseOrder().isEmpty()) return;

        int verseIndex = song.getVerseOrder().get(currentVersePosition);
        Verse verse = song.getVerses().get(verseIndex);

        updateCenterPreview(song, verse);

        // Update Actual Projection (if available)
        ProjectionController proj = PraiseViewApp.getProjectionController();
        if (proj != null) {
            proj.showSlide(song, currentVersePosition);
        }
    }

    private void updateCenterPreview(Song song, Verse verse) {
        // Update title (just the song title, no verse label)
        stageViewTitle.setText(song.getTitle());

        // Update lyrics
        livePreviewText.getChildren().clear();
        Text lyricsText = new Text(verse.getContent());
        lyricsText.setFill(javafx.scene.paint.Color.WHITE);
        lyricsText.setStyle("-fx-font-size: 16px; -fx-line-spacing: 8px;");

        livePreviewText.getChildren().add(lyricsText);
        livePreviewText.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        
        // Update current song verses list
        updateVersesList(song);
    }
    @FXML
    private void nextVerse() {
        if (currentQueueIndex >= 0 && currentQueueIndex < serviceQueue.size()) {
            ServiceItem item = serviceQueue.get(currentQueueIndex);
            // If there are more verses in the current song, go to next verse
            if (currentVersePosition < item.getSong().getVerseOrder().size() - 1) {
                currentVersePosition++;
                showCurrentItem();
            } else {
                // Current song is done, move to next song
                if (currentQueueIndex < serviceQueue.size() - 1) {
                    currentQueueIndex++;
                    currentVersePosition = 0;
                    showCurrentItem();
                }
            }
        }
    }

    @FXML
    private void previousVerse() {
        if (currentVersePosition > 0) {
            currentVersePosition--;
            showCurrentItem();
        } else if (currentQueueIndex > 0) {
            // At start of current song, go to previous song's last verse
            currentQueueIndex--;
            ServiceItem item = serviceQueue.get(currentQueueIndex);
            currentVersePosition = item.getSong().getVerseOrder().size() - 1;
            showCurrentItem();
        }
    }

    private void blackout() {
        ProjectionController proj = PraiseViewApp.getProjectionController();
        if (proj != null) proj.blackout();
    }

    private void clearScreen() {
        ProjectionController proj = PraiseViewApp.getProjectionController();
        if (proj != null) proj.clear();
    }

    // Menu Actions
    @FXML private void newService() {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("New Service");
        confirmAlert.setHeaderText("Create New Service?");
        confirmAlert.setContentText("This will clear all songs from the current service. Continue?");

        if (confirmAlert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            serviceQueue.clear();
            servicePlannerList.getItems().clear();
            currentQueueIndex = -1;
            currentVersePosition = 0;
            clearNextItemPreview();
            AppLogger.log("New service created");
        }
    }

    @FXML private void saveService() {
        if (serviceQueue.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Cannot save empty service. Add songs first.");
            alert.show();
            return;
        }

        FileChooser fc = new FileChooser();
        fc.setTitle("Save Service");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Service Files", "*.service"));
        fc.setInitialFileName("service_" + System.currentTimeMillis() + ".service");

        File file = fc.showSaveDialog(null);
        if (file != null) {
            jsonService.saveService(new java.util.ArrayList<>(serviceQueue), file);
            AppLogger.log("Service saved: " + file.getName());
        }
    }

    @FXML private void importService() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Load Service");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Service Files", "*.service"));

        File file = fc.showOpenDialog(null);
        if (file != null) {
            java.util.List<ServiceItem> loadedService = jsonService.loadService(file);
            if (loadedService != null && !loadedService.isEmpty()) {
                serviceQueue.clear();
                serviceQueue.addAll(loadedService);
                servicePlannerList.refresh();
                currentQueueIndex = -1;
                currentVersePosition = 0;
                AppLogger.log("Service loaded: " + file.getName());
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to load service file.");
                alert.show();
            }
        }
    }

    @FXML private void exportService() {
        System.out.println("Export JSON - to be implemented");
    }

    @FXML private void exitApp() {
        AppLogger.log("Application exited");
        System.exit(0);
    }

    @FXML private void showAbout() {
        System.out.println("About clicked");
    }

    private void updateNextItemPreview(ServiceItem item) {
        if (item == null || item.getSong() == null) {
            clearNextItemPreview();
            return;
        }

        Song song = item.getSong();
        if (song.getVerseOrder().isEmpty()) {
            clearNextItemPreview();
            return;
        }

        // Show first verse of the song
        int verseIndex = song.getVerseOrder().get(0);
        Verse verse = song.getVerses().get(verseIndex);

        nextItemTitle.setText(song.getTitle());
        nextItemPreviewText.getChildren().clear();
        
        Text lyricsText = new Text(verse.getContent());
        lyricsText.setFill(javafx.scene.paint.Color.WHITE);
        lyricsText.setStyle("-fx-font-size: 16px; -fx-line-spacing: 8px;");

        nextItemPreviewText.getChildren().add(lyricsText);
        nextItemPreviewText.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
    }

    private void clearNextItemPreview() {
        nextItemTitle.setText("");
        nextItemPreviewText.getChildren().clear();
    }

    private void updateVersesList(Song song) {
        ObservableList<VerseDisplayItem> verseItems = FXCollections.observableArrayList();
        
        if (song != null && !song.getVerseOrder().isEmpty()) {
            for (int i = 0; i < song.getVerseOrder().size(); i++) {
                int verseIndex = song.getVerseOrder().get(i);
                Verse verse = song.getVerses().get(verseIndex);
                verseItems.add(new VerseDisplayItem(i, verse.getLabel(), verse.getContent()));
            }
        }
        
        currentSongVersesList.setItems(verseItems);
        
        // Set custom cell factory to display verse labels with preview
        currentSongVersesList.setCellFactory(lv -> new ListCell<VerseDisplayItem>() {
            @Override
            protected void updateItem(VerseDisplayItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String preview = item.content.length() > 30 ? 
                        item.content.substring(0, 30) + "..." : item.content;
                    setText(item.label + ": " + preview);
                }
            }
        });
        
        // Double-click handler to jump to verse
        currentSongVersesList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                VerseDisplayItem selected = currentSongVersesList.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    currentVersePosition = selected.position;
                    showCurrentItem();
                }
            }
        });
    }

    // Helper class to display verse information
    private static class VerseDisplayItem {
        int position;
        String label;
        String content;

        VerseDisplayItem(int position, String label, String content) {
            this.position = position;
            this.label = label;
            this.content = content;
        }
    }
}
