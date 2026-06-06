package com.praiseview.controller;

import com.praiseview.PraiseViewApp;
import com.praiseview.db.DatabaseService;
import com.praiseview.model.Announcement;
import com.praiseview.model.Prayer;
import com.praiseview.model.Projectable;
import com.praiseview.model.ServiceItem;
import com.praiseview.model.Song;
import com.praiseview.model.Verse;
import com.praiseview.model.MediaItem; // Import MediaItem
import com.praiseview.model.PptItem; // Import PptItem
import com.praiseview.service.JsonService;
import com.praiseview.util.AppLogger;
import com.praiseview.util.PptRenderer; // Import PptRenderer for cleanup
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList; // Added for cleanup
import java.util.List;
import java.util.stream.Collectors;

public class MainController {

    // Left: Service Planner
    @FXML private ListView<ServiceItem> servicePlannerList;
    @FXML private Button editSongButton;

    // Center: Stage View
    @FXML private StackPane livePreviewPane;
    // FXML elements for mirroring projection content
    @FXML private VBox liveTextContentContainer;
    @FXML private Label stageViewTitle;
    @FXML private TextFlow livePreviewText;
    @FXML private ImageView liveImageView;
    @FXML private MediaView liveMediaView;
    @FXML private VBox livePptPlaceholderContainer;
    @FXML private Text livePptPlaceholderText;

    // Right: Controls (moved from old right pane)
    @FXML private Button projectButton, blackoutButton, clearButton;
    @FXML private Button nextVerseButton, prevVerseButton;
    @FXML private ListView<SubItemDisplayItem> currentSubItemList; // Renamed from currentSongVersesList

    // Bottom: Library
    @FXML private ListView<Song> songLibraryList;
    @FXML private TextField searchField;
    @FXML private Button addSongButton;

    // Prayer tab
    @FXML private ListView<Prayer> prayerList;
    @FXML private Button addPrayerButton;
    @FXML private Button editPrayerButton;

    // Media tabs
    @FXML private Button openImageButton;
    @FXML private Button clearImageButton;
    @FXML private ListView<MediaItem> imageList;
    @FXML private Button openVideoButton;
    @FXML private Button clearVideoButton;
    @FXML private ListView<MediaItem> videoList;
    @FXML private Button openPptButton;
    @FXML private Button clearPptButton;
    @FXML private ListView<PptItem> pptList; // Changed to PptItem

    private MediaPlayer liveMediaPlayer; // For video playback in the live preview

    private DatabaseService dbService = new DatabaseService();
    private JsonService jsonService = new JsonService();

    private ObservableList<Song> allSongs = FXCollections.observableArrayList();
    private FilteredList<Song> filteredSongs;
    private ObservableList<ServiceItem> serviceQueue = FXCollections.observableArrayList();
    private ObservableList<Prayer> allPrayers = FXCollections.observableArrayList();
    private ObservableList<MediaItem> imageLibrary = FXCollections.observableArrayList();
    private ObservableList<MediaItem> videoLibrary = FXCollections.observableArrayList();
    private ObservableList<PptItem> pptLibrary = FXCollections.observableArrayList();


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
        MenuItem deleteItem = new MenuItem("Delete");
        serviceContextMenu.getItems().addAll(deleteItem);

        deleteItem.setOnAction(e -> {
            int selectedIdx = servicePlannerList.getSelectionModel().getSelectedIndex();
            if (selectedIdx >= 0) {
                ServiceItem removedItem = serviceQueue.remove(selectedIdx);
                // Clean up PPT temp files if a PptItem is removed
                if (removedItem != null && removedItem.getContent() instanceof PptItem) {
                    ((PptItem) removedItem.getContent()).dispose();
                }
                // Reset if we deleted the current item
                if (currentQueueIndex >= serviceQueue.size()) {
                    currentQueueIndex = -1;
                }
                AppLogger.log("Item removed from service order");
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
        if (editPrayerButton != null) {
            editPrayerButton.setOnAction(e -> editSelectedPrayer());
        }

        // Media Tab Setup
        if (imageList != null) imageList.setItems(imageLibrary);
        if (openImageButton != null) openImageButton.setOnAction(e -> openMediaFiles(MediaItem.MediaType.IMAGE));
        if (clearImageButton != null) clearImageButton.setOnAction(e -> imageLibrary.clear());

        if (videoList != null) videoList.setItems(videoLibrary);
        if (openVideoButton != null) openVideoButton.setOnAction(e -> openMediaFiles(MediaItem.MediaType.VIDEO));
        if (clearVideoButton != null) clearVideoButton.setOnAction(e -> videoLibrary.clear());

        if (pptList != null) pptList.setItems(pptLibrary);
        if (openPptButton != null) openPptButton.setOnAction(e -> openMediaFiles(MediaItem.MediaType.PPT));
        if (clearPptButton != null) clearPptButton.setOnAction(e -> pptLibrary.clear());
    }

    public void setupSceneKeyHandler() {
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
                content.putString("SONG:" + selected.getId()); // Use ID for lookup
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
                content.putString("PRAYER:" + selected.getId()); // Use ID for lookup
                db.setContent(content);
                e.consume();
            }
        });

        // === Drag from Image List ===
        if (imageList != null) {
            imageList.setOnDragDetected(e -> {
                MediaItem selected = imageList.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    Dragboard db = imageList.startDragAndDrop(TransferMode.COPY);
                    ClipboardContent content = new ClipboardContent();
                    content.putString("IMAGE:" + selected.getFilePath()); // Use file path for lookup
                    db.setContent(content);
                    e.consume();
                }
            });
        }

        // === Drag from Video List ===
        if (videoList != null) {
            videoList.setOnDragDetected(e -> {
                MediaItem selected = videoList.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    Dragboard db = videoList.startDragAndDrop(TransferMode.COPY);
                    ClipboardContent content = new ClipboardContent();
                    content.putString("VIDEO:" + selected.getFilePath()); // Use file path for lookup
                    db.setContent(content);
                    e.consume();
                }
            });
        }

        // === Drag from PPT List ===
        if (pptList != null) {
            pptList.setOnDragDetected(e -> {
                PptItem selected = pptList.getSelectionModel().getSelectedItem(); // Changed to PptItem
                if (selected != null) {
                    Dragboard db = pptList.startDragAndDrop(TransferMode.COPY);
                    ClipboardContent content = new ClipboardContent();
                    content.putString("PPT:" + selected.getOriginalFilePath()); // Use original file path for lookup
                    db.setContent(content);
                    e.consume();
                }
            });
        }


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
            ServiceItem newItem = null;

            if (data.startsWith("SONG:")) {
                String songId = data.substring("SONG:".length());
                Song song = allSongs.stream().filter(s -> s.getId().equals(songId)).findFirst().orElse(null);
                if (song != null) {
                    newItem = new ServiceItem(song);
                }
            }
            else if (data.startsWith("PRAYER:")) {
                String prayerId = data.substring("PRAYER:".length());
                Prayer prayer = allPrayers.stream().filter(p -> p.getId().equals(prayerId)).findFirst().orElse(null);
                if (prayer != null) {
                    newItem = new ServiceItem(prayer);
                }
            }
            else if (data.startsWith("IMAGE:")) {
                String filePath = data.substring("IMAGE:".length());
                File file = new File(filePath);
                if (file.exists()) {
                    newItem = new ServiceItem(new MediaItem(file, MediaItem.MediaType.IMAGE));
                }
            }
            else if (data.startsWith("VIDEO:")) {
                String filePath = data.substring("VIDEO:".length());
                File file = new File(filePath);
                if (file.exists()) {
                    newItem = new ServiceItem(new MediaItem(file, MediaItem.MediaType.VIDEO));
                }
            }
            else if (data.startsWith("PPT:")) {
                String filePath = data.substring("PPT:".length());
                File file = new File(filePath);
                if (file.exists()) {
                    try {
                        newItem = new ServiceItem(new PptItem(file)); // Create PptItem
                    } catch (IOException ex) {
                        AppLogger.log("Failed to render PPT: " + ex.getMessage());
                        Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to load PPT: " + ex.getMessage());
                        alert.show();
                    }
                }
            }

            if (newItem != null) {
                serviceQueue.add(newItem);
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

    // Generic method to open media files
    private void openMediaFiles(MediaItem.MediaType type) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open " + type.name() + " Files");

        // Set extension filters based on media type
        switch (type) {
            case IMAGE:
                fileChooser.getExtensionFilters().addAll(
                        new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"),
                        new FileChooser.ExtensionFilter("All Files", "*.*")
                );
                break;
            case VIDEO:
                fileChooser.getExtensionFilters().addAll(
                        new FileChooser.ExtensionFilter("Video Files", "*.mp4", "*.avi", "*.mov", "*.wmv", "*.flv"),
                        new FileChooser.ExtensionFilter("All Files", "*.*")
                );
                break;
            case PPT:
                fileChooser.getExtensionFilters().addAll(
                        new FileChooser.ExtensionFilter("Presentation Files", "*.ppt", "*.pptx"),
                        new FileChooser.ExtensionFilter("All Files", "*.*")
                );
                break;
        }

        Window ownerWindow = livePreviewPane.getScene().getWindow();
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(ownerWindow);

        if (selectedFiles != null) {
            for (File file : selectedFiles) {
                if (type == MediaItem.MediaType.PPT) {
                    try {
                        pptLibrary.add(new PptItem(file)); // Create PptItem
                    } catch (IOException e) {
                        AppLogger.log("Failed to render PPT " + file.getName() + ": " + e.getMessage());
                        Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to load PPT " + file.getName() + ": " + e.getMessage());
                        alert.show();
                    }
                } else if (type == MediaItem.MediaType.IMAGE) {
                    imageLibrary.add(new MediaItem(file, type)); // Create MediaItem
                } else if (type == MediaItem.MediaType.VIDEO) {
                    videoLibrary.add(new MediaItem(file, type)); // Create MediaItem
                }
            }
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

    // Helper to hide all media preview elements
    private void hideAllLiveMediaViews() {
        if (liveTextContentContainer != null) {
            liveTextContentContainer.setVisible(false);
            liveTextContentContainer.setManaged(false);
        }
        if (liveImageView != null) {
            liveImageView.setVisible(false);
            liveImageView.setManaged(false);
            liveImageView.setImage(null); // Clear image
        }
        if (liveMediaView != null) {
            liveMediaView.setVisible(false);
            liveMediaView.setManaged(false);
            if (liveMediaPlayer != null) {
                liveMediaPlayer.stop();
                liveMediaPlayer.dispose();
                liveMediaPlayer = null;
            }
            liveMediaView.setMediaPlayer(null); // Clear media player
        }
        if (livePptPlaceholderContainer != null) {
            livePptPlaceholderContainer.setVisible(false);
            livePptPlaceholderContainer.setManaged(false);
        }
    }

    // This method now mirrors the projection screen
    private void updateCenterPreview() {
        AppLogger.log("MainController: updateCenterPreview called.");
        hideAllLiveMediaViews(); // Hide all before showing new content

        // Add null check here
        if (currentSubItemList != null) {
            currentSubItemList.getItems().clear(); // Clear sub-item list by default // Renamed
        }


        ProjectionController proj = PraiseViewApp.getProjectionController();
        if (proj == null) {
            AppLogger.log("MainController: ProjectionController is null.");
            stageViewTitle.setText(""); // Clear title even if no item
            return;
        }
        if (proj.getCurrentProjectedItem() == null) {
            AppLogger.log("MainController: No item currently projected.");
            stageViewTitle.setText(""); // Clear title even if no item
            return;
        }

        Projectable currentProjectedItem = proj.getCurrentProjectedItem();
        String displayedTitle = proj.getCurrentDisplayedTitle();
        String displayedContent = proj.getCurrentDisplayedContent(); // For text/PPT placeholder

        AppLogger.log("MainController: Projecting item type: " + currentProjectedItem.getType());
        AppLogger.log("MainController: Displayed Title: " + displayedTitle);
        AppLogger.log("MainController: Displayed Content (first 50 chars): " + (displayedContent.length() > 50 ? displayedContent.substring(0, 50) + "..." : displayedContent));


        stageViewTitle.setText(displayedTitle); // Always set title

        switch (currentProjectedItem.getType()) {
            case "SONG":
            case "PRAYER":
            case "ANNOUNCEMENT":
                if (liveTextContentContainer != null) {
                    liveTextContentContainer.setVisible(true);
                    liveTextContentContainer.setManaged(true);
                }
                if (livePreviewText != null) {
                    livePreviewText.getChildren().clear();
                    Text mainText = new Text(displayedContent);
                    mainText.setFill(javafx.scene.paint.Color.WHITE);
                    mainText.setStyle("-fx-font-size: " + PREVIEW_FONT_SIZE + "px; -fx-line-spacing: 8px;");
                    livePreviewText.getChildren().add(mainText);
                    livePreviewText.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
                }

                // Update sub-item list for text-based items
                updateSubItemList(currentProjectedItem); // Generalized call
                // Select the current sub-item in the list
                if (proj.getCurrentSubItemIndex() >= 0 && currentSubItemList != null && proj.getCurrentSubItemIndex() < currentSubItemList.getItems().size()) {
                    currentSubItemList.getSelectionModel().select(proj.getCurrentSubItemIndex());
                    currentSubItemList.scrollTo(proj.getCurrentSubItemIndex());
                }
                break;

            case "IMAGE":
                if (liveImageView != null) {
                    liveImageView.setVisible(true);
                    liveImageView.setManaged(true);
                    File imageFile = new File(((MediaItem)currentProjectedItem).getFilePath());
                    AppLogger.log("MainController: Loading image for preview: " + imageFile.getAbsolutePath());
                    if (imageFile.exists()) {
                        try {
                            Image image = new Image(imageFile.toURI().toString());
                            liveImageView.setImage(image);
                            // Bind image view size to parent pane size
                            liveImageView.fitWidthProperty().bind(livePreviewPane.widthProperty());
                            liveImageView.fitHeightProperty().bind(livePreviewPane.heightProperty());
                            liveImageView.setPreserveRatio(true);
                            AppLogger.log("MainController: Image loaded successfully for preview.");
                        } catch (Exception e) {
                            AppLogger.log("MainController: Error loading image for preview: " + e.getMessage());
                            // Fallback to text error
                            if (liveTextContentContainer != null) {
                                liveTextContentContainer.setVisible(true);
                                liveTextContentContainer.setManaged(true);
                            }
                            stageViewTitle.setText("Error Loading Image");
                            if (livePreviewText != null) {
                                livePreviewText.getChildren().clear();
                                livePreviewText.getChildren().add(new Text("Error loading image: " + imageFile.getName() + "\n" + e.getMessage()));
                            }
                        }
                    } else {
                        AppLogger.log("MainController: Image file not found for preview: " + imageFile.getAbsolutePath());
                        // Display error message on screen
                        if (liveTextContentContainer != null) {
                            liveTextContentContainer.setVisible(true);
                            liveTextContentContainer.setManaged(true);
                        }
                        stageViewTitle.setText("Error Loading Image");
                        if (livePreviewText != null) {
                            livePreviewText.getChildren().clear();
                            livePreviewText.getChildren().add(new Text("File not found: " + imageFile.getName()));
                        }
                    }
                } else {
                    AppLogger.log("MainController: liveImageView is null.");
                }
                break;

            case "VIDEO":
                if (liveMediaView != null) {
                    liveMediaView.setVisible(true);
                    liveMediaView.setManaged(true);
                    File videoFile = new File(((MediaItem)currentProjectedItem).getFilePath());
                    AppLogger.log("MainController: Preparing video preview placeholder for: " + videoFile.getAbsolutePath());
                    if (videoFile.exists()) {
                        // For preview, we'll just show a static image or text, not full video playback
                        // To play video in preview, uncomment below and manage liveMediaPlayer lifecycle
                        // Media media = new Media(videoFile.toURI().toString());
                        // liveMediaPlayer = new MediaPlayer(media);
                        // liveMediaView.setMediaPlayer(liveMediaPlayer);
                        // liveMediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                        // liveMediaPlayer.play();
                        // liveMediaView.setFitWidth(livePreviewPane.getWidth());
                        // liveMediaView.setFitHeight(livePreviewPane.getHeight());
                        // liveMediaView.setPreserveRatio(true);

                        // Placeholder for video preview
                        if (liveTextContentContainer != null) {
                            liveTextContentContainer.setVisible(true);
                            liveTextContentContainer.setManaged(true);
                        }
                        if (livePreviewText != null) {
                            livePreviewText.getChildren().clear();
                            livePreviewText.getChildren().add(new Text("Video Preview: " + currentProjectedItem.getTitle() + "\n(Playing on Projection Screen)"));
                        }
                    } else {
                        AppLogger.log("MainController: Video file not found for preview: " + videoFile.getAbsolutePath());
                        // Display error message on screen
                        if (liveTextContentContainer != null) {
                            liveTextContentContainer.setVisible(true);
                            liveTextContentContainer.setManaged(true);
                        }
                        stageViewTitle.setText("Error Loading Video");
                        if (livePreviewText != null) {
                            livePreviewText.getChildren().clear();
                            livePreviewText.getChildren().add(new Text("File not found: " + videoFile.getName()));
                        }
                    }
                } else {
                    AppLogger.log("MainController: liveMediaView is null.");
                }
                break;

            case "PPT":
                if (liveImageView != null) { // Reuse liveImageView for PPT slides
                    liveImageView.setVisible(true);
                    liveImageView.setManaged(true);
                    PptItem pptItem = (PptItem) currentProjectedItem;
                    if (pptItem.getRenderedSlideImagePaths() != null && !pptItem.getRenderedSlideImagePaths().isEmpty()) {
                        String slideImagePath = pptItem.getSubItemContent(proj.getCurrentSubItemIndex(), PREVIEW_FONT_SIZE, livePreviewPane.getWidth(), livePreviewPane.getHeight());
                        File slideImageFile = new File(slideImagePath);
                        AppLogger.log("MainController: Loading PPT slide image for preview: " + slideImageFile.getAbsolutePath());

                        if (slideImageFile.exists()) {
                            try {
                                Image slideImage = new Image(slideImageFile.toURI().toString());
                                liveImageView.setImage(slideImage);
                                liveImageView.fitWidthProperty().bind(livePreviewPane.widthProperty());
                                liveImageView.fitHeightProperty().bind(livePreviewPane.heightProperty());
                                liveImageView.setPreserveRatio(true);
                                AppLogger.log("MainController: PPT slide image loaded successfully for preview.");
                            } catch (Exception e) {
                                AppLogger.log("MainController: Error loading PPT slide image for preview: " + e.getMessage());
                                // Fallback to text error
                                if (liveTextContentContainer != null) {
                                    liveTextContentContainer.setVisible(true);
                                    liveTextContentContainer.setManaged(true);
                                }
                                stageViewTitle.setText("Error Loading PPT Slide");
                                if (livePreviewText != null) {
                                    livePreviewText.getChildren().clear();
                                    livePreviewText.getChildren().add(new Text("Failed to load slide " + (proj.getCurrentSubItemIndex() + 1) + ": " + slideImageFile.getName() + "\n" + e.getMessage()));
                                }
                            }
                        } else {
                            AppLogger.log("MainController: PPT slide image file not found for preview: " + slideImageFile.getAbsolutePath());
                            // Fallback to text error
                            if (liveTextContentContainer != null) {
                                liveTextContentContainer.setVisible(true);
                                liveTextContentContainer.setManaged(true);
                            }
                            stageViewTitle.setText("Error Loading PPT Slide");
                            if (livePreviewText != null) {
                                livePreviewText.getChildren().clear();
                                livePreviewText.getChildren().add(new Text("Slide image not found: " + slideImageFile.getName()));
                            }
                        }
                    } else {
                        AppLogger.log("MainController: No rendered slides found for PPT preview: " + pptItem.getTitle());
                        if (liveTextContentContainer != null) {
                            liveTextContentContainer.setVisible(true);
                            liveTextContentContainer.setManaged(true);
                        }
                        stageViewTitle.setText("Error Loading PPT");
                        if (livePreviewText != null) {
                            livePreviewText.getChildren().clear();
                            livePreviewText.getChildren().add(new Text("No slides rendered for: " + pptItem.getTitle()));
                        }
                    }
                } else {
                    AppLogger.log("MainController: liveImageView is null for PPT preview.");
                }
                // Update sub-item list for PPT slides
                updateSubItemList(currentProjectedItem); // Generalized call
                // Select the current sub-item in the list
                if (proj.getCurrentSubItemIndex() >= 0 && currentSubItemList != null && proj.getCurrentSubItemIndex() < currentSubItemList.getItems().size()) {
                    currentSubItemList.getSelectionModel().select(proj.getCurrentSubItemIndex());
                    currentSubItemList.scrollTo(proj.getCurrentSubItemIndex());
                }
                break;

            default:
                if (liveTextContentContainer != null) {
                    liveTextContentContainer.setVisible(true);
                    liveTextContentContainer.setManaged(true);
                }
                stageViewTitle.setText("Unsupported Item Type");
                if (livePreviewText != null) {
                    livePreviewText.getChildren().clear();
                    livePreviewText.getChildren().add(new Text("Cannot display: " + currentProjectedItem.getType()));
                }
                AppLogger.log("MainController: Unsupported item type: " + currentProjectedItem.getType());
                break;
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
        // Clean up all temporary PPT image directories on exit
        for (ServiceItem item : serviceQueue) {
            if (item.getContent() instanceof PptItem) {
                ((PptItem) item.getContent()).dispose();
            }
        }
        for (PptItem item : pptLibrary) {
            item.dispose();
        }
        System.exit(0);
    }

    @FXML private void showAbout() {
        System.out.println("About clicked");
    }

    // Renamed from updateVersesList to updateSubItemList
    private void updateSubItemList(Projectable item) {
        ObservableList<SubItemDisplayItem> subItems = FXCollections.observableArrayList();

        if (item != null) {
            // Use arbitrary reasonable dimensions for label calculation, as it's just for the label text.
            // The actual content for projection will use projection-specific dimensions.
            double labelCalcWidth = 400.0;
            double labelCalcHeight = 300.0;
            double labelCalcFontSize = 16.0;

            int totalSubItems = item.getSubItemCount(labelCalcFontSize, labelCalcWidth, labelCalcHeight);
            for (int i = 0; i < totalSubItems; i++) {
                String label = item.getSubItemLabel(i);
                String contentPreview = item.getSubItemContent(i, labelCalcFontSize, labelCalcWidth, labelCalcHeight);
                subItems.add(new SubItemDisplayItem(i, label, contentPreview));
            }
        }

        if (currentSubItemList != null) { // Added null check
            currentSubItemList.setItems(subItems);

            // Set custom cell factory to display sub-item labels with preview
            currentSubItemList.setCellFactory(lv -> new ListCell<SubItemDisplayItem>() {
                @Override
                protected void updateItem(SubItemDisplayItem item, boolean empty) {
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

            // Double-click handler to jump to sub-item
            currentSubItemList.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2) {
                    SubItemDisplayItem selected = currentSubItemList.getSelectionModel().getSelectedItem();
                    if (selected != null) {
                        currentSubItemIndex = selected.position;
                        showCurrentItem();
                    }
                }
            });
        } else {
            AppLogger.log("MainController: currentSubItemList is null, cannot update sub-item list.");
        }
    }

    // Helper class to display sub-item information (generalized from VerseDisplayItem)
    private static class SubItemDisplayItem {
        int position;
        String label;
        String content;

        SubItemDisplayItem(int position, String label, String content) {
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