package com.praiseview.controller;

import com.praiseview.PraiseViewApp;
import com.praiseview.db.DatabaseService;
import com.praiseview.model.Announcement;
import com.praiseview.model.Prayer;
import com.praiseview.model.Projectable;
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

    // Center: Stage View
    @FXML private StackPane livePreviewPane;
    @FXML private Label stageViewTitle;
    @FXML private TextFlow livePreviewText;
    
    // Right: Controls (moved from old right pane)
    @FXML private Button projectButton, blackoutButton, clearButton;
    @FXML private Button nextVerseButton, prevVerseButton;
    @FXML private ListView<VerseDisplayItem> currentSongVersesList;

    // Bottom: Library
    @FXML private ListView<Song> songLibraryList;
    @FXML private TextField searchField;
    @FXML private Button addSongButton;

    //prayer tab
    @FXML private ListView<Prayer> prayerList;
    @FXML private Button addPrayerButton;
    @FXML private Button editPrayerButton; // Added for edit prayer functionality

    private DatabaseService dbService = new DatabaseService();
    private JsonService jsonService = new JsonService();

    private ObservableList<Song> allSongs = FXCollections.observableArrayList();
    private FilteredList<Song> filteredSongs;
    private ObservableList<ServiceItem> serviceQueue = FXCollections.observableArrayList();
    private ObservableList<Prayer> allPrayers = FXCollections.observableArrayList();

    private int currentQueueIndex = -1;
    private int currentSubItemIndex = 0; // For songs: verse index, for prayers/announcements: page index
    private javafx.scene.Scene scene;

    // Constants for preview text sizing (can be adjusted or made dynamic)
    private static final double PREVIEW_FONT_SIZE = 16.0;
    private static final double PREVIEW_WIDTH_DEFAULT = 400.0; // Default width for preview pane
    private static final double PREVIEW_HEIGHT_DEFAULT = 300.0; // Default height for preview pane


    public void setScene(javafx.scene.Scene scene) {
        this.scene = scene;
    }
    @FXML
    public void initialize() {
        loadSongs();
        loadPrayers();

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
        // Removed previewItem as next item preview pane is gone
        MenuItem deleteItem = new MenuItem("Delete");
        serviceContextMenu.getItems().addAll(deleteItem);

        // Removed previewItem.setOnAction

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
                currentSubItemIndex = 0;
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
        if (nextVerseButton != null) nextVerseButton.setOnAction(e -> nextItemOrSubItem());
        if (prevVerseButton != null) prevVerseButton.setOnAction(e -> previousItemOrSubItem());

        // Context menu for song library
        ContextMenu libraryContextMenu = new ContextMenu();
        // Removed previewLibraryItem as next item preview pane is gone
        // libraryContextMenu.getItems().add(previewLibraryItem); // Removed

        // Removed previewLibraryItem.setOnAction

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

        // Prayers
        prayerList.setItems(allPrayers);
        addPrayerButton.setOnAction(e -> openPrayerEditor(null));
        // Set action for editPrayerButton
        if (editPrayerButton != null) {
            editPrayerButton.setOnAction(e -> editSelectedPrayer());
        }
    }

    public void setupSceneKeyHandler() {
        // Called from PraiseViewApp after scene is set
        if (scene != null) {
            scene.setOnKeyPressed(this::handleArrowKey);
        }
    }

    private void handleArrowKey(KeyEvent e) {
        if (e.getCode() == KeyCode.LEFT) {
            previousItemOrSubItem();
            e.consume();
        } else if (e.getCode() == KeyCode.RIGHT) {
            nextItemOrSubItem();
            e.consume();
        }
    }

    private void loadSongs() {
        allSongs.setAll(dbService.loadAllSongs());
        songLibraryList.refresh();
    }

    private void setupDragAndDrop() {
        // === Drag from Song Library ===
        songLibraryList.setOnDragDetected(e -> {
            Song selected = songLibraryList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                Dragboard db = songLibraryList.startDragAndDrop(TransferMode.COPY);
                ClipboardContent content = new ClipboardContent();
                content.putString("SONG:" + selected.getId());
                db.setContent(content);
                e.consume();
            }
        });

        // === Drag from Prayers ===
        prayerList.setOnDragDetected(e -> {
            Prayer selected = prayerList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                Dragboard db = prayerList.startDragAndDrop(TransferMode.COPY);
                ClipboardContent content = new ClipboardContent();
                content.putString("PRAYER:" + selected.getId());
                db.setContent(content);
                e.consume();
            }
        });

        // === Drop on Service Queue ===
        servicePlannerList.setOnDragOver(e -> {
            if (e.getDragboard().hasString()) {
                e.acceptTransferModes(TransferMode.COPY);
            }
        });

        servicePlannerList.setOnDragDropped(e -> {
            Dragboard db = e.getDragboard();
            if (!db.hasString()) return;

            String data = db.getString();

            if (data.startsWith("SONG:")) {
                Song song = songLibraryList.getSelectionModel().getSelectedItem();
                if (song != null) {
                    serviceQueue.add(new ServiceItem(song)); // Create ServiceItem with Projectable
                }
            }
            else if (data.startsWith("PRAYER:")) {
                Prayer prayer = prayerList.getSelectionModel().getSelectedItem();
                if (prayer != null) {
                    serviceQueue.add(new ServiceItem(prayer)); // Create ServiceItem with Projectable
                }
            }

            servicePlannerList.refresh();
            e.setDropCompleted(true);
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

    // New method to edit selected prayer
    @FXML
    private void editSelectedPrayer() {
        Prayer selected = prayerList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            openPrayerEditor(selected);
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Please select a prayer in the library to edit.");
            alert.show();
        }
    }

    private void startProjection() {
        if (serviceQueue.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Please add items to the service order first.");
            alert.show();
            return;
        }
        if (currentQueueIndex == -1) currentQueueIndex = 0;
        currentSubItemIndex = 0; // Reset sub-item position for new item

        // Update projection first, then mirror in preview
        ServiceItem item = serviceQueue.get(currentQueueIndex);
        ProjectionController proj = PraiseViewApp.getProjectionController();
        if (proj != null) {
            proj.showItem(item.getContent(), currentSubItemIndex);
        }
        updateCenterPreview(); // Mirror the projection
        livePreviewPane.requestFocus(); // Ensure focus for arrow keys
    }

    private void showCurrentItem() {
        if (currentQueueIndex < 0 || currentQueueIndex >= serviceQueue.size()) {
            clearScreen();
            return;
        }

        ServiceItem item = serviceQueue.get(currentQueueIndex);
        Projectable projectable = item.getContent();

        // Update projection first, then mirror in preview
        ProjectionController proj = PraiseViewApp.getProjectionController();
        if (proj != null) {
            proj.showItem(projectable, currentSubItemIndex);
        }
        updateCenterPreview(); // Mirror the projection
        livePreviewPane.requestFocus(); // Ensure focus for arrow keys
    }

    // This method now mirrors the projection screen
    private void updateCenterPreview() {
        livePreviewText.getChildren().clear();
        currentSongVersesList.getItems().clear(); // Clear verse list by default

        ProjectionController proj = PraiseViewApp.getProjectionController();
        if (proj == null || proj.getCurrentProjectedItem() == null) {
            stageViewTitle.setText("");
            return;
        }

        // Get the currently displayed content and title directly from the ProjectionController
        stageViewTitle.setText(proj.getCurrentDisplayedTitle());
        
        Text mainText = new Text(proj.getCurrentDisplayedContent());
        mainText.setFill(javafx.scene.paint.Color.WHITE);
        mainText.setStyle("-fx-font-size: " + PREVIEW_FONT_SIZE + "px; -fx-line-spacing: 8px;");
        livePreviewText.getChildren().add(mainText);
        livePreviewText.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        
        // Only update verse list if it's a Song
        Projectable currentProjectedItem = proj.getCurrentProjectedItem();
        if (currentProjectedItem instanceof Song) {
            Song song = (Song) currentProjectedItem;
            updateVersesList(song);
            // Select the current verse in the list
            if (proj.getCurrentSubItemIndex() >= 0 && proj.getCurrentSubItemIndex() < currentSongVersesList.getItems().size()) {
                currentSongVersesList.getSelectionModel().select(proj.getCurrentSubItemIndex());
                currentSongVersesList.scrollTo(proj.getCurrentSubItemIndex());
            }
        }
    }

    @FXML
    private void nextItemOrSubItem() {
        if (currentQueueIndex < 0 || currentQueueIndex >= serviceQueue.size()) {
            return; // No item selected or queue is empty
        }

        ServiceItem currentItem = serviceQueue.get(currentQueueIndex);
        Projectable currentProjectable = currentItem.getContent();

        ProjectionController proj = PraiseViewApp.getProjectionController();
        if (proj == null) return;

        // Get total sub-items from the ProjectionController's current state
        int totalSubItems = proj.getCurrentProjectedItemSubItemCount();

        if (currentSubItemIndex < totalSubItems - 1) {
            currentSubItemIndex++; // Move to next sub-item (verse/page)
            showCurrentItem();
        } else {
            // Last sub-item of current Projectable, move to next ServiceItem
            if (currentQueueIndex < serviceQueue.size() - 1) {
                currentQueueIndex++;
                currentSubItemIndex = 0; // Reset sub-item position for new item
                showCurrentItem();
            }
        }
    }

    @FXML
    private void previousItemOrSubItem() {
        if (currentQueueIndex < 0 || currentQueueIndex >= serviceQueue.size()) {
            return; // No item selected or queue is empty
        }

        ServiceItem currentItem = serviceQueue.get(currentQueueIndex);
        Projectable currentProjectable = currentItem.getContent();

        ProjectionController proj = PraiseViewApp.getProjectionController();
        if (proj == null) return;

        if (currentSubItemIndex > 0) {
            currentSubItemIndex--; // Move to previous sub-item (verse/page)
            showCurrentItem();
        } else {
            // First sub-item of current Projectable, move to previous ServiceItem
            if (currentQueueIndex > 0) {
                currentQueueIndex--;
                // For previous item, go to its last sub-item
                ServiceItem prevItem = serviceQueue.get(currentQueueIndex);
                Projectable prevProjectable = prevItem.getContent();
                
                // Temporarily show the previous item on projection to get its correct sub-item count
                // This is a bit of a workaround to get the correct pagination for the *previous* item
                // without fully displaying it yet.
                proj.showItem(prevProjectable, 0); // Show first sub-item to trigger pagination calculation
                currentSubItemIndex = proj.getCurrentProjectedItemSubItemCount() - 1;
                
                showCurrentItem(); // Now display the previous item at its last sub-item
            }
        }
    }

    private void blackout() {
        ProjectionController proj = PraiseViewApp.getProjectionController();
        if (proj != null) {
            proj.blackout();
            updateCenterPreview(); // Mirror the blackout state in the stage view
        }
    }

    private void clearScreen() {
        ProjectionController proj = PraiseViewApp.getProjectionController();
        if (proj != null) {
            proj.clear();
            updateCenterPreview(); // Mirror the clear state in the stage view
        }
    }

    // Menu Actions
    @FXML private void newService() {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("New Service");
        confirmAlert.setHeaderText("Create New Service?");
        confirmAlert.setContentText("This will clear all items from the current service. Continue?");

        if (confirmAlert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            serviceQueue.clear();
            servicePlannerList.getItems().clear();
            currentQueueIndex = -1;
            currentSubItemIndex = 0;
            // Removed clearNextItemPreview() as next item preview pane is gone
            clearScreen(); // Clear main preview as well
            AppLogger.log("New service created");
        }
    }

    @FXML private void saveService() {
        if (serviceQueue.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Cannot save empty service. Add items first.");
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
                currentSubItemIndex = 0;
                clearScreen(); // Clear main preview after loading new service
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

    // Removed updateNextItemPreview() as next item preview pane is gone
    // Removed clearNextItemPreview() as next item preview pane is gone

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
                    currentSubItemIndex = selected.position;
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

    private void openPrayerEditor(Prayer prayer) {
        PrayerEditorDialog dialog = new PrayerEditorDialog(prayer);
        dialog.showAndWait().ifPresent(result -> {
            dbService.savePrayer(result);           // ← Save to DB
            loadPrayers();                          // Refresh list
            AppLogger.log("Prayer saved: " + result.getTitle());
        });
    }

    private void loadPrayers() {
        allPrayers.setAll(dbService.loadAllPrayers());
        prayerList.refresh(); // Explicitly refresh the ListView
    }
}
