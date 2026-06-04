package com.praiseview.controller;

import com.praiseview.PraiseViewApp;
import com.praiseview.db.DatabaseService;
import com.praiseview.model.ServiceItem;
import com.praiseview.model.Song;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import java.util.ArrayList;
import java.util.List;

public class MainController {

    @FXML private ListView<Song> songListView;
    @FXML private ListView<ServiceItem> serviceListView;
    @FXML private TextField searchField;
    @FXML private TextFlow currentSlidePreview;
    @FXML private TextFlow nextSlidePreview;

    @FXML private Button addSongButton, projectButton, nextButton, previousButton;
    @FXML private Button nextVerseButton, prevVerseButton, blackoutButton, clearButton;
    @FXML private ToggleButton aiToggle;
    @FXML private Slider fontSlider;

    private DatabaseService dbService = new DatabaseService();
    private ObservableList<Song> allSongs = FXCollections.observableArrayList();
    private FilteredList<Song> filteredSongs;
    private List<ServiceItem> serviceQueue = new ArrayList<>();

    private int currentQueueIndex = -1;
    private int currentVersePosition = 0;

    @FXML
    public void initialize() {
        loadSongs();

        // Search
        filteredSongs = new FilteredList<>(allSongs, p -> true);
        songListView.setItems(filteredSongs);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredSongs.setPredicate(song -> {
                if (newVal == null || newVal.isEmpty()) return true;
                String lower = newVal.toLowerCase();
                return song.getTitle().toLowerCase().contains(lower) ||
                        (song.getCategory() != null && song.getCategory().toLowerCase().contains(lower));
            });
        });

        // Drag & Drop from Song Library to Service Queue
        setupDragAndDrop();

        // Button Actions
        addSongButton.setOnAction(e -> openSongEditor(null));
        projectButton.setOnAction(e -> refreshProjection());
        nextButton.setOnAction(e -> goToNextSong());
        previousButton.setOnAction(e -> goToPreviousSong());
        nextVerseButton.setOnAction(e -> nextVerse());
        prevVerseButton.setOnAction(e -> previousVerse());
        blackoutButton.setOnAction(e -> blackoutProjection());
        clearButton.setOnAction(e -> clearProjection());
    }

    private void loadSongs() {
        allSongs.setAll(dbService.loadAllSongs());
    }

    private void setupDragAndDrop() {
        songListView.setOnDragDetected(e -> {
            Song selected = songListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                Dragboard db = songListView.startDragAndDrop(TransferMode.COPY);
                ClipboardContent content = new ClipboardContent();
                content.putString(selected.getId());
                db.setContent(content);
                e.consume();
            }
        });

        serviceListView.setOnDragOver(e -> {
            if (e.getDragboard().hasString()) e.acceptTransferModes(TransferMode.COPY);
        });

        serviceListView.setOnDragDropped(e -> {
            Song song = songListView.getSelectionModel().getSelectedItem();
            if (song != null) {
                serviceQueue.add(new ServiceItem(song));
                serviceListView.getItems().setAll(serviceQueue);
                e.setDropCompleted(true);
            }
        });
    }

    private void openSongEditor(Song song) {
        SongEditorDialog dialog = new SongEditorDialog(song);
        dialog.showAndWait().ifPresent(result -> {
            dbService.saveSong(result);
            loadSongs();
        });
    }

    private void refreshProjection() {
        if (serviceQueue.isEmpty()) return;
        if (currentQueueIndex == -1) currentQueueIndex = 0;
        currentVersePosition = 0;
        showCurrentItem();
    }

    private void showCurrentItem() {
        if (currentQueueIndex < 0 || currentQueueIndex >= serviceQueue.size()) return;

        ServiceItem item = serviceQueue.get(currentQueueIndex);
        ProjectionController proj = PraiseViewApp.getProjectionController();

        if (proj != null) {
            proj.showSlide(item.getSong(), currentVersePosition);
        }

        updateSlidePreviews();
    }

    private void updateSlidePreviews() {
        // Current
        currentSlidePreview.getChildren().clear();
        if (currentQueueIndex >= 0 && currentQueueIndex < serviceQueue.size()) {
            ServiceItem current = serviceQueue.get(currentQueueIndex);
            Text t1 = new Text(current.getSong().getTitle() + "\n" +
                    current.getSong().getVerseAtPosition(currentVersePosition).getLabel());
            t1.setStyle("-fx-fill: white;");
            currentSlidePreview.getChildren().add(t1);
        }

        // Next
        nextSlidePreview.getChildren().clear();
        if (currentQueueIndex + 1 < serviceQueue.size()) {
            ServiceItem next = serviceQueue.get(currentQueueIndex + 1);
            Text t2 = new Text(next.getSong().getTitle());
            t2.setStyle("-fx-fill: #aaaaaa;");
            nextSlidePreview.getChildren().add(t2);
        }
    }

    private void goToNextSong() {
        if (currentQueueIndex < serviceQueue.size() - 1) {
            currentQueueIndex++;
            currentVersePosition = 0;
            showCurrentItem();
        }
    }

    private void goToPreviousSong() {
        if (currentQueueIndex > 0) {
            currentQueueIndex--;
            currentVersePosition = 0;
            showCurrentItem();
        }
    }

    private void nextVerse() {
        if (currentQueueIndex >= 0 && currentQueueIndex < serviceQueue.size()) {
            ServiceItem item = serviceQueue.get(currentQueueIndex);
            if (currentVersePosition < item.getSong().getVerseOrder().size() - 1) {
                currentVersePosition++;
                showCurrentItem();
            }
        }
    }

    private void previousVerse() {
        if (currentVersePosition > 0) {
            currentVersePosition--;
            showCurrentItem();
        }
    }

    private void blackoutProjection() {
        ProjectionController proj = PraiseViewApp.getProjectionController();
        if (proj != null) proj.blackout();
    }

    private void clearProjection() {
        ProjectionController proj = PraiseViewApp.getProjectionController();
        if (proj != null) proj.clear();
    }
}
