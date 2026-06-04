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

public class MainController {

    // Left: Service Planner
    @FXML private ListView<ServiceItem> servicePlannerList;

    // Center: Editor + Preview
    @FXML private TextArea itemEditorArea;
    @FXML private StackPane livePreviewPane;
    @FXML private TextFlow livePreviewText;

    // Right: Controls
    @FXML private Button projectButton, blackoutButton, clearButton;
    @FXML private Button nextVerseButton, prevVerseButton;

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

        // Drag & Drop from Library to Service Order
        setupDragAndDrop();

        // Button Actions
        addSongButton.setOnAction(e -> openSongEditor(null));
        projectButton.setOnAction(e -> startProjection());
        blackoutButton.setOnAction(e -> blackout());
        clearButton.setOnAction(e -> clearScreen());

        // Verse Navigation
        if (nextVerseButton != null) nextVerseButton.setOnAction(e -> nextVerse());
        if (prevVerseButton != null) prevVerseButton.setOnAction(e -> previousVerse());
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
        System.out.println(
    "Projection Controller = "
            + PraiseViewApp.getProjectionController());
        if (currentQueueIndex < 0 || currentQueueIndex >= serviceQueue.size()) return;

        ServiceItem item = serviceQueue.get(currentQueueIndex);
        Song song = item.getSong();

        if (song == null || song.getVerseOrder().isEmpty()) return;

        int verseIndex = song.getVerseOrder().get(currentVersePosition);
        Verse verse = song.getVerses().get(verseIndex);

        System.out.println(
    "Showing: "
            + song.getTitle()
            + " Position: "
            + currentVersePosition);// Update Center Live Preview
        updateCenterPreview(song, verse);

        // Update Actual Projection (if available)
        ProjectionController proj = PraiseViewApp.getProjectionController();
        if (proj != null) {
            proj.showSlide(song, currentVersePosition);
        }
    }

    private void updateCenterPreview(Song song, Verse verse) {
        livePreviewText.getChildren().clear();

        Text titleText = new Text(song.getTitle() + "\n");
        titleText.setStyle("-fx-font-size: 22px; -fx-fill: #ffd700;");

        Text verseLabel = new Text(verse.getLabel() + "\n\n");
        verseLabel.setStyle("-fx-font-size: 16px; -fx-fill: #aaaaaa;");

        Text lyricsText = new Text(verse.getContent());
        lyricsText.setStyle("-fx-font-size: 18px; -fx-fill: white; -fx-line-spacing: 6px;");

        livePreviewText.getChildren().addAll(titleText, verseLabel, lyricsText);
    }
    @FXML
    private void nextVerse() {
        if (currentQueueIndex >= 0 && currentQueueIndex < serviceQueue.size()) {
            ServiceItem item = serviceQueue.get(currentQueueIndex);
            if (currentVersePosition < item.getSong().getVerseOrder().size() - 1) {
                currentVersePosition++;
                showCurrentItem();
            }
        }
    }

    @FXML
    private void previousVerse() {
        if (currentVersePosition > 0) {
            currentVersePosition--;
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
        serviceQueue.clear();
        servicePlannerList.getItems().clear();
        AppLogger.log("New service created");
    }

    @FXML private void saveService() {
        AppLogger.log("Save service requested");
        System.out.println("Service saved (placeholder)");
    }

    @FXML private void importService() {
        System.out.println("Import JSON - to be implemented");
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
}
