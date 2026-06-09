package com.praiseview.controller;

import com.praiseview.PraiseViewApp;
import com.praiseview.db.DatabaseService;
import com.praiseview.model.*;
import com.praiseview.service.JsonService;
import com.praiseview.service.UpdateService; // Import UpdateService
import com.praiseview.util.AppLogger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Window;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
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
    @FXML private ImageView liveItemImageView; // Renamed from liveImageView
    @FXML private MediaView liveItemMediaView; // Renamed from liveMediaView
    @FXML private VBox livePptPlaceholderContainer;
    @FXML private Text livePptPlaceholderText;
    @FXML private ImageView liveLogoImageView; // Added for logo in Stage View

    // New FXML elements for theme backgrounds in live preview
    @FXML private ImageView liveThemeBackgroundImageView;
    @FXML private MediaView liveThemeBackgroundMediaView;


    // Right: Controls (moved from old right pane)
    @FXML private Button projectButton, blackoutButton, clearButton;
    @FXML private Button nextVerseButton, prevVerseButton;
    @FXML private ListView<SubItemDisplayItem> currentSubItemList; // Renamed from currentSongVersesList

    // Video Controls (newly added)
    @FXML private Button videoPlayPauseButton;
    @FXML private Button videoRewindButton;
    @FXML private Button videoForwardButton;

    // Right: Theme Editor
    @FXML private ListView<Theme> themeListView; // New FXML element for theme list
    @FXML private CheckBox showTitleCheckBox; // New FXML element for show title checkbox

    // Menu Items
    @FXML private MenuItem updateApplicationMenuItem;


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

    private MediaPlayer liveItemMediaPlayer; // For video playback in the live preview (content item)
    private MediaPlayer liveThemeBackgroundMediaPlayer; // For video playback in the live preview (theme background)


    private DatabaseService dbService = new DatabaseService();
    private JsonService jsonService = new JsonService();
    private UpdateService updateService; // Declare UpdateService

    private ObservableList<Song> allSongs = FXCollections.observableArrayList();
    private FilteredList<Song> filteredSongs;
    private ObservableList<ServiceItem> serviceQueue = FXCollections.observableArrayList();
    private ObservableList<Prayer> allPrayers = FXCollections.observableArrayList();
    private ObservableList<MediaItem> imageLibrary = FXCollections.observableArrayList();
    private ObservableList<MediaItem> videoLibrary = FXCollections.observableArrayList();
    private ObservableList<PptItem> pptLibrary = FXCollections.observableArrayList();

    // Theme Management
    private ObservableList<Theme> availableThemes = FXCollections.observableArrayList();
    private static Path THEMES_FILE_PATH; // Changed to Path
    private Theme currentActiveTheme; // The theme currently applied to projection and preview


    private int currentQueueIndex = -1;
    private int currentSubItemIndex = 0; // For songs: verse index, for prayers/announcements: page index
    private javafx.scene.Scene scene;

    // Constants for preview text sizing (can be adjusted or made dynamic)
    private static final double PREVIEW_FONT_SIZE = 16.0;
    private static final double PREVIEW_WIDTH_DEFAULT = 400.0; // Default width for preview pane
    private static final double PREVIEW_HEIGHT_DEFAULT = 300.0; // Default height for preview pane

    // For drag and drop reordering within servicePlannerList
    private int dragSourceIndex = -1;

    // Custom DataFormat for internal ListView reordering
    private static final DataFormat SERVICE_ITEM_REORDER = new DataFormat("application/x-java-service-item-reorder");


    public void setScene(javafx.scene.Scene scene) {
        this.scene = scene;
    }

    // New method to initialize themes file path
    private void initializeThemesPath() {
        try {
            String userHome = System.getProperty("user.home");
            Path appDataDir = Paths.get(userHome, "AppData", "Local", "PraiseView");

            if (!Files.exists(appDataDir)) {
                Files.createDirectories(appDataDir);
                AppLogger.log("Created application data directory for themes: " + appDataDir.toAbsolutePath());
            }
            THEMES_FILE_PATH = appDataDir.resolve("themes.json");
            AppLogger.log("Themes file path: " + THEMES_FILE_PATH.toAbsolutePath());
        } catch (IOException e) {
            AppLogger.log("Error initializing themes file path: " + e.getMessage());
            e.printStackTrace();
            // Fallback to current directory if app data path fails
            THEMES_FILE_PATH = Paths.get("themes.json");
            AppLogger.log("Falling back to current directory for themes file: " + THEMES_FILE_PATH.toAbsolutePath());
        }
    }

    @FXML
    public void initialize() {
        AppLogger.log("MainController: Initializing...");
        AppLogger.log("MainController: currentSubItemList (before setup): " + (currentSubItemList != null ? "NOT NULL" : "NULL"));

        initializeThemesPath(); // Initialize themes path first
        loadSongs();
        loadPrayers();
        loadThemes(); // Load themes on startup

        // Initialize UpdateService
        updateService = new UpdateService(PraiseViewApp.getStaticHostServices());

        // Show logo on projected screen on startup
        ProjectionController proj = PraiseViewApp.getProjectionController();
        if (proj != null) {
            proj.showLogo();
        }
        // Show logo on live preview pane on startup
        showLivePreviewLogo();


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
        servicePlannerList.setCellFactory(lv -> new ListCell<ServiceItem>() {
            @Override
            protected void updateItem(ServiceItem serviceItem, boolean empty) { // Renamed 'item' to 'serviceItem'
                super.updateItem(serviceItem, empty);
                if (empty || serviceItem == null) {
                    setText(null);
                } else {
                    String prefix = "";
                    if (serviceItem.getContent() instanceof Song) {
                        prefix = "HYM";
                    } else if (serviceItem.getContent() instanceof Prayer) {
                        prefix = "PRY";
                    } else if (serviceItem.getContent() instanceof Announcement) {
                        prefix = "ANN";
                    } else if (serviceItem.getContent() instanceof MediaItem) {
                        MediaItem media = (MediaItem) serviceItem.getContent();
                        if (media.getMediaType() == MediaItem.MediaType.IMAGE) {
                            prefix = "IMG";
                        } else if (media.getMediaType() == MediaItem.MediaType.VIDEO) {
                            prefix = "VID";
                        }
                    } else if (serviceItem.getContent() instanceof PptItem) {
                        prefix = "PPT";
                    }
                    setText(prefix + " - " + serviceItem.getContent().getTitle());
                }
            }
        });


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

        // Drag & Drop from Library to Service Order AND within Service Order
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

        // Video Controls
        if (videoPlayPauseButton != null) videoPlayPauseButton.setOnAction(e -> playPauseVideo());
        if (videoRewindButton != null) videoRewindButton.setOnAction(e -> onVideoRewind());
        if (videoForwardButton != null) videoForwardButton.setOnAction(e -> onVideoForward());


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

        // Theme Editor Pane Setup
        if (themeListView != null) {
            themeListView.setItems(availableThemes);
            themeListView.setCellFactory(lv -> new ListCell<Theme>() {
                private final ImageView imageView = new ImageView();
                private final Label nameLabel = new Label();
                private final HBox graphicBox = new HBox(5, imageView, nameLabel);

                {
                    imageView.setFitWidth(80); // Smaller preview image
                    imageView.setFitHeight(60);
                    imageView.setPreserveRatio(true);
                    graphicBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                }

                @Override
                protected void updateItem(Theme theme, boolean empty) {
                    super.updateItem(theme, empty);
                    if (empty || theme == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        nameLabel.setText(theme.getName());
                        // Dynamically generate theme preview
                        imageView.setImage(createThemePreviewImage(theme, (int)imageView.getFitWidth(), (int)imageView.getFitHeight()));
                        setGraphic(graphicBox);
                    }
                }
            });

            themeListView.setOnMouseClicked(this::handleThemeSelection);
        }

        if (showTitleCheckBox != null) {
            // Set initial state based on the current active theme
            if (currentActiveTheme != null) {
                showTitleCheckBox.setSelected(currentActiveTheme.isShowTitle());
            } else {
                showTitleCheckBox.setSelected(true); // Default if no theme is active yet
            }
            showTitleCheckBox.setOnAction(this::handleShowTitleToggle);
        }

        // Add listeners to livePreviewPane dimensions to re-apply theme background
        // This ensures correct scaling once the pane has its actual size after layout.
        livePreviewPane.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.doubleValue() > 0 && currentActiveTheme != null) {
                AppLogger.log("MainController: livePreviewPane width changed to " + newVal.doubleValue() + ", re-applying theme background.");
                applyThemeBackgroundToLivePreview(currentActiveTheme);
            }
        });
        livePreviewPane.heightProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.doubleValue() > 0 && currentActiveTheme != null) {
                AppLogger.log("MainController: livePreviewPane height changed to " + newVal.doubleValue() + ", re-applying theme background.");
                applyThemeBackgroundToLivePreview(currentActiveTheme);
            }
        });


        AppLogger.log("MainController: currentSubItemList (after setup): " + (currentSubItemList != null ? "NOT NULL" : "NULL"));
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


        // === Drop on Service Queue (from Library or for reordering) ===
        servicePlannerList.setOnDragOver(e -> {
            if (e.getDragboard().hasString() || e.getDragboard().hasContent(SERVICE_ITEM_REORDER)) {
                e.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            e.consume();
        });

        servicePlannerList.setOnDragDropped(e -> {
            Dragboard db = e.getDragboard();
            boolean success = false;

            if (db.hasContent(SERVICE_ITEM_REORDER)) { // Handle reordering within the list
                int draggedIdx = Integer.parseInt((String) db.getContent(SERVICE_ITEM_REORDER));
                
                // Determine the target index based on the drop location
                int targetIdx = -1;
                Node node = e.getGestureTarget() instanceof Node ? (Node) e.getGestureTarget() : null;

                // Find the ListCell that the drag is over
                ListCell<ServiceItem> targetCell = null;
                while (node != null && !(node instanceof ListCell) && node.getParent() != null) {
                    node = node.getParent();
                }
                if (node instanceof ListCell) {
                    targetCell = (ListCell<ServiceItem>) node;
                }

                if (targetCell != null && !targetCell.isEmpty()) {
                    // Dropped on an actual item
                    targetIdx = targetCell.getIndex();
                    // Determine if dropping before or after the target cell
                    // If the drop is in the lower half of the cell, insert after it
                    if (e.getY() > (targetCell.getBoundsInParent().getMinY() + targetCell.getHeight() / 2)) {
                        targetIdx++;
                    }
                } else {
                    // Dropped on empty space or an empty cell (e.g., below the last item)
                    targetIdx = serviceQueue.size(); // Append to the end
                }

                // Ensure targetIdx is within bounds
                if (targetIdx < 0) targetIdx = 0;
                if (targetIdx > serviceQueue.size()) targetIdx = serviceQueue.size();


                if (draggedIdx >= 0 && draggedIdx < serviceQueue.size()) {
                    ServiceItem draggedItem = serviceQueue.remove(draggedIdx);

                    // Adjust targetIdx if the item was removed from an earlier position
                    if (targetIdx > draggedIdx) {
                        targetIdx--;
                    }
                    // Ensure targetIdx is still valid after adjustment
                    if (targetIdx < 0) targetIdx = 0;
                    if (targetIdx > serviceQueue.size()) targetIdx = serviceQueue.size();

                    serviceQueue.add(targetIdx, draggedItem);
                    servicePlannerList.getSelectionModel().select(targetIdx);
                    AppLogger.log("Service item reordered from " + draggedIdx + " to " + targetIdx);
                    success = true;
                }
            } else if (db.hasString()) { // Handle drag from library (still uses PLAIN_TEXT)
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
                else if (data.startsWith("ANNOUNCEMENT:")) { // Assuming Announcement can also be dragged
                    String announcementId = data.substring("ANNOUNCEMENT:".length());
                    // You'll need a way to retrieve announcements from a library similar to songs/prayers
                    // For now, assuming you have an allAnnouncements list or similar
                    // Announcement announcement = allAnnouncements.stream().filter(a -> a.getId().equals(announcementId)).findFirst().orElse(null);
                    // if (announcement != null) {
                    //     announcement.rePaginate(PREVIEW_FONT_SIZE, PREVIEW_WIDTH_DEFAULT, PREVIEW_HEIGHT_DEFAULT);
                    //     newItem = new ServiceItem(announcement);
                    // }
                    AppLogger.log("MainController: Dragged Announcement not yet fully implemented.");
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
                    int targetIdx = servicePlannerList.getSelectionModel().getSelectedIndex();
                    if (targetIdx == -1) { // If dropped in empty space or no selection, add to end
                        serviceQueue.add(newItem);
                    } else {
                        serviceQueue.add(targetIdx, newItem);
                    }
                    AppLogger.log("Item added to service order: " + newItem.getContent().getTitle());
                    success = true;
                }
            }

            servicePlannerList.refresh(); // Explicitly refresh the ListView
            e.setDropCompleted(success);
            e.consume();
        });

        // === Drag within Service Queue ===
        servicePlannerList.setOnDragDetected(e -> {
            ServiceItem selectedItem = servicePlannerList.getSelectionModel().getSelectedItem();
            if (selectedItem != null && e.getButton() == MouseButton.PRIMARY) {
                Dragboard db = servicePlannerList.startDragAndDrop(TransferMode.MOVE);
                ClipboardContent content = new ClipboardContent();
                dragSourceIndex = servicePlannerList.getSelectionModel().getSelectedIndex();
                content.put(SERVICE_ITEM_REORDER, String.valueOf(dragSourceIndex)); // Use custom DataFormat
                db.setContent(content);
                e.consume();
            }
        });

        servicePlannerList.setOnDragDone(e -> {
            dragSourceIndex = -1; // Reset source index
            e.consume();
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
        Projectable projectable = item.getContent();

        // For Announcement, rePaginate is still used as its implementation hasn't changed
        if (projectable instanceof Announcement) {
            // This call is now redundant as ProjectionController will handle pagination for all text types
            // ((Announcement) projectable).rePaginate(PREVIEW_FONT_SIZE, PREVIEW_WIDTH_DEFAULT, PREVIEW_HEIGHT_DEFAULT);
        }

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

        // For Announcement, rePaginate is still used as its implementation hasn't changed
        if (projectable instanceof Announcement) {
            // This call is now redundant as ProjectionController will handle pagination for all text types
            // ((Announcement) projectable).rePaginate(PREVIEW_FONT_SIZE, PREVIEW_WIDTH_DEFAULT, PREVIEW_HEIGHT_DEFAULT);
        }

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
        if (liveItemImageView != null) {
            liveItemImageView.setVisible(false);
            liveItemImageView.setManaged(false);
            liveItemImageView.setImage(null); // Clear image
        }
        if (liveItemMediaView != null) {
            liveItemMediaView.setVisible(false);
            liveItemMediaView.setManaged(false);
            if (liveItemMediaPlayer != null) {
                liveItemMediaPlayer.stop();
                liveItemMediaPlayer.dispose();
                liveItemMediaPlayer = null;
            }
            liveItemMediaView.setMediaPlayer(null); // Clear media player
        }
        if (livePptPlaceholderContainer != null) {
            livePptPlaceholderContainer.setVisible(false);
            livePptPlaceholderContainer.setManaged(false);
        }
        // Do NOT hide the live preview logo here, showLivePreviewLogo() will handle its visibility
        // if (liveLogoImageView != null) {
        //     liveLogoImageView.setVisible(false);
        //     liveLogoImageView.setManaged(false);
        // }
        // Disable video controls when not showing video
        if (videoPlayPauseButton != null) videoPlayPauseButton.setDisable(true);
        if (videoRewindButton != null) videoRewindButton.setDisable(true);
        if (videoForwardButton != null) videoPlayPauseButton.setDisable(true);

        // Also hide theme background media views
        if (liveThemeBackgroundImageView != null) {
            liveThemeBackgroundImageView.setVisible(false);
            liveThemeBackgroundImageView.setManaged(false);
            liveThemeBackgroundImageView.setImage(null);
        }
        if (liveThemeBackgroundMediaView != null) {
            liveThemeBackgroundMediaView.setVisible(false);
            liveThemeBackgroundMediaView.setManaged(false);
            if (liveThemeBackgroundMediaPlayer != null) {
                liveThemeBackgroundMediaPlayer.stop();
                liveThemeBackgroundMediaPlayer.dispose();
                liveThemeBackgroundMediaPlayer = null;
            }
            liveThemeBackgroundMediaView.setMediaPlayer(null);
        }
    }

    // This method now mirrors the projection screen
    private void updateCenterPreview() {
        AppLogger.log("MainController: updateCenterPreview called.");
        
        // --- Start: Clear and reset all content views and media players ---
        liveTextContentContainer.setVisible(false);
        liveTextContentContainer.setManaged(false);
        liveItemImageView.setVisible(false);
        liveItemImageView.setManaged(false);
        liveItemImageView.setImage(null);
        liveItemMediaView.setVisible(false);
        liveItemMediaView.setManaged(false);
        if (liveItemMediaPlayer != null) {
            liveItemMediaPlayer.stop();
            liveItemMediaPlayer.dispose();
            liveItemMediaPlayer = null;
            AppLogger.log("MainController: Stopped and disposed live item media player before showing new item.");
        }
        liveItemMediaView.setMediaPlayer(null);
        livePptPlaceholderContainer.setVisible(false);
        livePptPlaceholderContainer.setManaged(false);
        liveLogoImageView.setVisible(false); // Hide logo when other content is displayed
        liveLogoImageView.setManaged(false); // Hide logo when other content is displayed
        if (videoPlayPauseButton != null) videoPlayPauseButton.setDisable(true);
        if (videoRewindButton != null) videoRewindButton.setDisable(true);
        if (videoForwardButton != null) videoPlayPauseButton.setDisable(true);
        // --- End: Clear and reset all content views and media players ---


        // Add null check here
        if (currentSubItemList != null) {
            currentSubItemList.getItems().clear(); // Clear sub-item list by default // Renamed
        }


        ProjectionController proj = PraiseViewApp.getProjectionController();
        if (proj == null) {
            AppLogger.log("MainController: ProjectionController is null.");
            stageViewTitle.setText(""); // Clear title even if no item
            stageViewTitle.setVisible(false); // Ensure title is hidden if no projection
            return;
        }
        if (proj.getCurrentProjectedItem() == null) {
            AppLogger.log("MainController: No item currently projected.");
            stageViewTitle.setText(""); // Clear title even if no item
            stageViewTitle.setVisible(false); // Ensure title is hidden if no projection
            return;
        }

        Projectable currentProjectedItem = proj.getCurrentProjectedItem();
        String displayedTitle = proj.getCurrentDisplayedTitle();
        String displayedContent = proj.getCurrentDisplayedContent(); // For text/PPT placeholder

        AppLogger.log("MainController: Projecting item type: " + currentProjectedItem.getType());
        AppLogger.log("MainController: Displayed Title: " + displayedTitle);
        AppLogger.log("MainController: Displayed Content (first 50 chars): " + (displayedContent.length() > 50 ? displayedContent.substring(0, 50) + "..." : displayedContent));

        // Set title visibility based on currentActiveTheme.showTitle
        if (currentActiveTheme != null && currentActiveTheme.isShowTitle()) {
            stageViewTitle.setText(displayedTitle);
            stageViewTitle.setVisible(true);
            stageViewTitle.setManaged(true);
            // Apply title-specific font settings
            stageViewTitle.setStyle(String.format("-fx-font-family: '%s'; -fx-font-size: %.1fpx; -fx-text-fill: %s;",
                    currentActiveTheme.getTitleFontFamily(), currentActiveTheme.getTitleFontSize(), currentActiveTheme.getTitleTextColor()));
        } else {
            stageViewTitle.setText("");
            stageViewTitle.setVisible(false);
            stageViewTitle.setManaged(false);
        }


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
                    // Set fill color using theme's text color
                    try {
                        // Ensure currentActiveTheme is not null here
                        if (currentActiveTheme != null) {
                            mainText.setFill(Color.web(currentActiveTheme.getTextColor()));
                        } else {
                            AppLogger.log("CRITICAL: currentActiveTheme is NULL in updateCenterPreview. Falling back to WHITE.");
                            mainText.setFill(Color.WHITE); // Fallback
                        }
                    } catch (IllegalArgumentException e) {
                        AppLogger.log("Invalid text color in active theme: '" + (currentActiveTheme != null ? currentActiveTheme.getTextColor() : "NULL THEME") + "'. Falling back to WHITE. Error: " + e.getMessage());
                        mainText.setFill(Color.WHITE); // Fallback
                    }
                    mainText.setStyle("-fx-font-family: '" + currentActiveTheme.getFontFamily() + "'; -fx-font-size: " + PREVIEW_FONT_SIZE + "px; -fx-line-spacing: 8px;");
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
                if (liveItemImageView != null) {
                    liveItemImageView.setVisible(true);
                    liveItemImageView.setManaged(true);
                    File imageFile = new File(((MediaItem)currentProjectedItem).getFilePath());
                    AppLogger.log("MainController: Loading image for preview: " + imageFile.getAbsolutePath());
                    if (imageFile.exists()) {
                        try {
                            Image image = new Image(imageFile.toURI().toString());
                            liveItemImageView.setImage(image);
                            // Bind image view size to parent pane size
                            liveItemImageView.fitWidthProperty().bind(livePreviewPane.widthProperty());
                            liveItemImageView.fitHeightProperty().bind(livePreviewPane.heightProperty());
                            liveItemImageView.setPreserveRatio(true);
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
                    AppLogger.log("MainController: liveItemImageView is null.");
                }
                break;

            case "VIDEO":
                if (liveItemMediaView != null) {
                    liveItemMediaView.setVisible(true);
                    liveItemMediaView.setManaged(true);
                    File videoFile = new File(((MediaItem)currentProjectedItem).getFilePath());
                    AppLogger.log("MainController: Preparing video preview for: " + videoFile.getAbsolutePath());
                    if (videoFile.exists()) {
                        Media media = new Media(videoFile.toURI().toString());
                        // liveItemMediaPlayer is already stopped/disposed at the start of updateCenterPreview
                        liveItemMediaPlayer = new MediaPlayer(media);
                        liveItemMediaView.setMediaPlayer(liveItemMediaPlayer);
                        liveItemMediaPlayer.setCycleCount(MediaPlayer.INDEFINITE); // Loop video
                        liveItemMediaPlayer.play(); // Explicitly play the video
                        liveItemMediaView.fitWidthProperty().bind(livePreviewPane.widthProperty());
                        liveItemMediaView.fitHeightProperty().bind(livePreviewPane.heightProperty());
                        liveItemMediaView.setPreserveRatio(true);

                        // Enable video controls
                        if (videoPlayPauseButton != null) {
                            videoPlayPauseButton.setDisable(false);
                            videoPlayPauseButton.setText("Pause"); // Initial state
                        }
                        if (videoRewindButton != null) videoRewindButton.setDisable(false);
                        if (videoForwardButton != null) videoForwardButton.setDisable(false);

                        liveItemMediaPlayer.statusProperty().addListener((obs, oldStatus, newStatus) -> {
                            if (videoPlayPauseButton != null) {
                                if (newStatus == MediaPlayer.Status.PLAYING) {
                                    videoPlayPauseButton.setText("Pause");
                                } else {
                                    videoPlayPauseButton.setText("Play");
                                }
                            }
                        });

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
                    AppLogger.log("MainController: liveItemMediaView is null.");
                }
                break;

            case "PPT":
                if (liveItemImageView != null) { // Reuse liveItemImageView for PPT slides
                    liveItemImageView.setVisible(true);
                    liveItemImageView.setManaged(true);
                    PptItem pptItem = (PptItem) currentProjectedItem;
                    if (pptItem.getRenderedSlideImagePaths() != null && !pptItem.getRenderedSlideImagePaths().isEmpty()) {
                        String slideImagePath = pptItem.getSubItemContent(proj.getCurrentSubItemIndex(), PREVIEW_FONT_SIZE, livePreviewPane.getWidth(), livePreviewPane.getHeight());
                        File slideImageFile = new File(slideImagePath);
                        AppLogger.log("MainController: Loading PPT slide image for preview: " + slideImageFile.getAbsolutePath());

                        if (slideImageFile.exists()) {
                            try {
                                Image slideImage = new Image(slideImageFile.toURI().toString());
                                liveItemImageView.setImage(slideImage);
                                liveItemImageView.fitWidthProperty().bind(livePreviewPane.widthProperty());
                                liveItemImageView.fitHeightProperty().bind(livePreviewPane.heightProperty());
                                liveItemImageView.setPreserveRatio(true);
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
                                livePreviewText.getChildren().add(new Text("File not found: " + slideImageFile.getName()));
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
                    AppLogger.log("MainController: liveItemImageView is null for PPT preview.");
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
    private void playPauseVideo() {
        // Control local preview media player
        if (liveItemMediaPlayer != null) {
            if (liveItemMediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                liveItemMediaPlayer.pause();
                if (videoPlayPauseButton != null) videoPlayPauseButton.setText("Play");
            } else {
                liveItemMediaPlayer.play();
                if (videoPlayPauseButton != null) videoPlayPauseButton.setText("Pause");
            }
        }

        // Control projection media player
        ProjectionController proj = PraiseViewApp.getProjectionController();
        if (proj != null) {
            proj.playPauseVideo();
        }
    }

    @FXML
    private void onVideoRewind() {
        seekVideo(-10.0);
    }

    @FXML
    private void onVideoForward() {
        seekVideo(10.0);
    }

    private void seekVideo(double seconds) {
        // Control local preview media player
        if (liveItemMediaPlayer != null && liveItemMediaPlayer.getStatus() != MediaPlayer.Status.STOPPED) {
            Duration currentTime = liveItemMediaPlayer.getCurrentTime();
            Duration newTime = currentTime.add(Duration.seconds(seconds));
            liveItemMediaPlayer.seek(newTime);
        }

        // Control projection media player
        ProjectionController proj = PraiseViewApp.getProjectionController();
        if (proj != null) {
            proj.seekVideo(seconds);
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

                // This call is now redundant as ProjectionController will handle pagination for all text types
                // if (prevProjectable instanceof Announcement) {
                //     ((Announcement) prevProjectable).rePaginate(proj.currentFontSize, proj.getLyricsFlow().getWidth(), proj.getLyricsFlow().getHeight());
                // }

                // currentSubItemIndex will be set to the last sub-item of the previous item
                // Ensure pagination is up-to-date in ProjectionController before getting count
                proj.showItem(prevProjectable, 0); // Temporarily show item to ensure pagination is done
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
            proj.clear(); // This will now call showLogo() internally
            updateCenterPreview(); // Mirror the clear state in the stage view
        }
        showLivePreviewLogo(); // Show logo in live preview as well
    }

    private void showLivePreviewLogo() {
        hideAllLiveMediaViews(); // Hide all other preview elements
        if (liveLogoImageView != null) {
            // Load default logo if not already set
            if (liveLogoImageView.getImage() == null) {
                try {
                    // Assuming a default logo image exists in resources
                    Image defaultLogo = new Image(getClass().getResourceAsStream("/com/praiseview/images/default_logo.png"));
                    liveLogoImageView.setImage(defaultLogo);
                    liveLogoImageView.setPreserveRatio(true);
                    liveLogoImageView.setFitWidth(200); // Set a reasonable size for the logo
                    liveLogoImageView.setFitHeight(200);
                    AppLogger.log("MainController: Loaded default logo image for live preview.");
                } catch (Exception e) {
                    AppLogger.log("MainController: Error loading default logo for live preview: " + e.getMessage());
                }
            }
            liveLogoImageView.setVisible(true);
            liveLogoImageView.setManaged(true);
            AppLogger.log("MainController: Displaying logo in live preview.");
        }
        stageViewTitle.setText(""); // Clear title when showing logo
        stageViewTitle.setVisible(false); // Hide title when showing logo

        // Re-apply theme background if active
        if (currentActiveTheme != null) {
            applyThemeBackgroundToLivePreview(currentActiveTheme);
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
                // For Announcement, rePaginate is still used as its implementation hasn't changed
                for (ServiceItem item : serviceQueue) {
                    Projectable projectable = item.getContent();
                    if (projectable instanceof Announcement) {
                        // This call is now redundant as ProjectionController will handle pagination for all text types
                        // ((Announcement) projectable).rePaginate(PREVIEW_FONT_SIZE, PREVIEW_WIDTH_DEFAULT, PREVIEW_HEIGHT_DEFAULT);
                    }
                }
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

    @FXML private void exportSongs() {
        // 1. Get all unique languages
        Set<String> uniqueLanguages = allSongs.stream()
                .map(Song::getLanguage)
                .filter(lang -> lang != null && !lang.trim().isEmpty())
                .collect(Collectors.toCollection(HashSet::new));

        // 2. Create a custom dialog for language selection
        Dialog<List<String>> dialog = new Dialog<>();
        dialog.setTitle("Export Songs by Language");
        dialog.setHeaderText("Select languages to export:");

        ButtonType exportButtonType = new ButtonType("Export", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(exportButtonType, ButtonType.CANCEL);

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        CheckBox exportAllCheckbox = new CheckBox("Export All Languages");
        exportAllCheckbox.setSelected(true); // Default to exporting all

        List<CheckBox> languageCheckBoxes = new ArrayList<>();
        if (uniqueLanguages.isEmpty()) {
            content.getChildren().add(new Label("No languages found in your song library. Exporting all songs."));
            exportAllCheckbox.setDisable(true);
        } else {
            for (String lang : uniqueLanguages) {
                CheckBox cb = new CheckBox(lang);
                cb.setSelected(true); // Default to selecting all found languages
                cb.disableProperty().bind(exportAllCheckbox.selectedProperty()); // Disable if "Export All" is selected
                languageCheckBoxes.add(cb);
                content.getChildren().add(cb);
            }
        }

        content.getChildren().add(0, exportAllCheckbox); // Add "Export All" at the top

        dialog.getDialogPane().setContent(content);

        // Convert the result to a list of selected languages
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == exportButtonType) {
                if (exportAllCheckbox.isSelected() || uniqueLanguages.isEmpty()) {
                    return new ArrayList<>(uniqueLanguages); // Return all unique languages to signify "export all"
                } else {
                    return languageCheckBoxes.stream()
                            .filter(CheckBox::isSelected)
                            .map(CheckBox::getText)
                            .collect(Collectors.toList());
                }
            }
            return null;
        });

        Optional<List<String>> result = dialog.showAndWait();

        result.ifPresent(selectedLanguages -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Songs Export");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
            fileChooser.setInitialFileName("songs_export.json");

            File file = fileChooser.showSaveDialog(null);
            if (file != null) {
                List<Song> songsToExport;
                if (selectedLanguages.containsAll(uniqueLanguages) && selectedLanguages.size() == uniqueLanguages.size()) {
                    // If all languages were selected (or no languages existed), export all songs
                    songsToExport = new ArrayList<>(allSongs);
                    AppLogger.log("Exporting all songs to: " + file.getAbsolutePath());
                } else {
                    // Filter songs by selected languages
                    songsToExport = allSongs.stream()
                            .filter(song -> selectedLanguages.contains(song.getLanguage()))
                            .collect(Collectors.toList());
                    AppLogger.log("Exporting songs for languages " + selectedLanguages + " to: " + file.getAbsolutePath());
                }
                jsonService.exportSongs(songsToExport, file);
            }
        });
    }

    @FXML private void importSongs() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Songs");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));

        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            List<Song> importedSongs = jsonService.importSongs(file);
            if (importedSongs != null && !importedSongs.isEmpty()) {
                // Check for duplicates before adding
                int initialSize = allSongs.size();
                for (Song newSong : importedSongs) {
                    if (allSongs.stream().noneMatch(existingSong -> existingSong.getId().equals(newSong.getId()))) {
                        allSongs.add(newSong);
                        dbService.saveSong(newSong); // Save imported song to DB
                    } else {
                        AppLogger.log("Skipping duplicate song on import: " + newSong.getTitle());
                    }
                }
                if (allSongs.size() > initialSize) {
                    songLibraryList.refresh();
                    AppLogger.log((allSongs.size() - initialSize) + " new songs imported from: " + file.getAbsolutePath());
                } else {
                    AppLogger.log("No new songs imported (all were duplicates or file was empty): " + file.getAbsolutePath());
                }
            }
        }
    }

    @FXML private void exportPrayers() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Prayers");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
        fileChooser.setInitialFileName("prayers_export.json");

        File file = fileChooser.showSaveDialog(null);
        if (file != null) {
            jsonService.exportPrayers(new ArrayList<>(allPrayers), file);
            AppLogger.log("Prayers exported to: " + file.getAbsolutePath());
        }
    }

    @FXML private void importPrayers() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Prayers");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));

        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            List<Prayer> importedPrayers = jsonService.importPrayers(file);
            if (importedPrayers != null && !importedPrayers.isEmpty()) {
                // Check for duplicates before adding
                int initialSize = allPrayers.size();
                for (Prayer newPrayer : importedPrayers) {
                    if (allPrayers.stream().noneMatch(existingPrayer -> existingPrayer.getId().equals(newPrayer.getId()))) {
                        allPrayers.add(newPrayer);
                        dbService.savePrayer(newPrayer); // Save imported prayer to DB
                    } else {
                        AppLogger.log("Skipping duplicate prayer on import: " + newPrayer.getTitle());
                    }
                }
                if (allPrayers.size() > initialSize) {
                    prayerList.refresh();
                    AppLogger.log((allPrayers.size() - initialSize) + " new prayers imported from: " + file.getAbsolutePath());
                } else {
                    AppLogger.log("No new prayers imported (all were duplicates or file was empty): " + file.getAbsolutePath());
                }
            } else {
                AppLogger.log("No prayers imported or file was empty: " + file.getAbsolutePath());
            }
        }
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

    // Theme Management Getters/Setters
    public ObservableList<Theme> getAvailableThemes() {
        return availableThemes;
    }

    public Theme getCurrentActiveTheme() {
        return currentActiveTheme;
    }

    public void setCurrentActiveTheme(Theme theme) {
        this.currentActiveTheme = theme;
    }

    private void loadThemes() {
        // Use the initialized THEMES_FILE_PATH
        File themesFile = THEMES_FILE_PATH.toFile();
        List<Theme> loadedThemes = null;

        if (themesFile.exists()) {
            loadedThemes = jsonService.importThemes(themesFile);
            if (loadedThemes != null && !loadedThemes.isEmpty()) {
                availableThemes.setAll(loadedThemes);
                AppLogger.log("Themes loaded from " + THEMES_FILE_PATH.toAbsolutePath());
            } else {
                AppLogger.log("No themes found in " + THEMES_FILE_PATH.toAbsolutePath() + ". Will create default.");
            }
        } else {
            AppLogger.log(THEMES_FILE_PATH.toAbsolutePath() + " not found. Will create default.");
        }

        // If no themes were loaded or found, create a default one
        if (availableThemes.isEmpty()) {
            Theme defaultTheme = new Theme(); // Uses default constructor with sensible defaults
            availableThemes.add(defaultTheme);
            saveThemes(); // Save the newly created default theme
            AppLogger.log("Default theme created and saved.");
        }

        // Ensure currentActiveTheme is set to the first available theme
        // This block is guaranteed to execute after availableThemes has at least one item.
        currentActiveTheme = availableThemes.get(0);
        applyTheme(currentActiveTheme); // Apply the theme immediately
    }

    private void createDefaultTheme() {
        // This method is now effectively replaced by the logic in loadThemes()
        // but kept for clarity if other parts of the code still call it.
        // The robust initialization is now handled in loadThemes().
        AppLogger.log("createDefaultTheme() called, but primary default creation is in loadThemes().");
        if (availableThemes.isEmpty()) {
            Theme defaultTheme = new Theme();
            availableThemes.add(defaultTheme);
            saveThemes();
        }
    }

    public void saveThemes() { // Made public so ThemeEditorController can call it
        // Use the initialized THEMES_FILE_PATH
        File themesFile = THEMES_FILE_PATH.toFile();
        jsonService.exportThemes(new ArrayList<>(availableThemes), themesFile);
        AppLogger.log("Themes saved to " + THEMES_FILE_PATH.toAbsolutePath());
    }

    /**
     * Applies the given theme to both the preview and projection screens.
     * This method will need to be expanded as more theme properties are supported.
     * @param theme The theme to apply.
     */
    public void applyTheme(Theme theme) { // Made public so ThemeEditorController can call it
        if (theme == null) {
            AppLogger.log("Attempted to apply a null theme.");
            return;
        }
        this.currentActiveTheme = theme;
        AppLogger.log("Applying theme: " + theme.getName());

        // Apply text properties to preview screen
        if (livePreviewText != null) {
            livePreviewText.setStyle(String.format("-fx-font-family: '%s'; -fx-font-size: %.1fpx; -fx-fill: %s; -fx-line-spacing: %.1fpx;",
                    theme.getFontFamily(), theme.getFontSize(), theme.getTextColor(), theme.getLineSpacing()));
            // Text alignment
            switch (theme.getTextAlignment().toUpperCase()) {
                case "LEFT":
                    livePreviewText.setTextAlignment(javafx.scene.text.TextAlignment.LEFT);
                    break;
                case "RIGHT":
                    livePreviewText.setTextAlignment(javafx.scene.text.TextAlignment.RIGHT);
                    break;
                case "CENTER":
                default:
                    livePreviewText.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
                    break;
            }
        }

        // Apply title-specific font settings to stageViewTitle
        if (stageViewTitle != null) {
            stageViewTitle.setStyle(String.format("-fx-font-family: '%s'; -fx-font-size: %.1fpx; -fx-text-fill: %s;",
                    theme.getTitleFontFamily(), theme.getTitleFontSize(), theme.getTitleTextColor()));
        }

        // Apply theme background to live preview
        applyThemeBackgroundToLivePreview(theme);

        // Update showTitleCheckBox state
        if (showTitleCheckBox != null) {
            showTitleCheckBox.setSelected(theme.isShowTitle());
        }

        // Apply to projection screen
        ProjectionController proj = PraiseViewApp.getProjectionController();
        if (proj != null) {
            proj.applyTheme(theme); // This method will be added to ProjectionController next
        }

        // Re-render current item to reflect new theme
        if (currentQueueIndex != -1 && currentQueueIndex < serviceQueue.size()) {
            showCurrentItem();
        } else {
            // If no item is projected, ensure title visibility is updated for the logo screen
            if (currentActiveTheme.isShowTitle()) {
                stageViewTitle.setVisible(true);
                stageViewTitle.setManaged(true);
            } else {
                stageViewTitle.setVisible(false);
                stageViewTitle.setManaged(false);
            }
        }
    }

    /**
     * Helper method to apply theme background to the live preview pane.
     * Separated to be reusable for showLivePreviewLogo.
     */
    private void applyThemeBackgroundToLivePreview(Theme theme) {
        // Stop and hide any existing theme background media
        if (liveThemeBackgroundMediaPlayer != null) {
            liveThemeBackgroundMediaPlayer.stop();
            liveThemeBackgroundMediaPlayer.dispose();
            liveThemeBackgroundMediaPlayer = null;
        }
        liveThemeBackgroundImageView.setVisible(false);
        liveThemeBackgroundImageView.setManaged(false);
        liveThemeBackgroundImageView.setImage(null);
        liveThemeBackgroundMediaView.setVisible(false);
        liveThemeBackgroundMediaView.setManaged(false);
        liveThemeBackgroundMediaView.setMediaPlayer(null);

        // The logo should not be hidden by background application.
        // Its visibility is managed by showLivePreviewLogo() and updateCenterPreview().
        // if (liveLogoImageView != null) {
        //     liveLogoImageView.setVisible(false);
        //     liveLogoImageView.setManaged(false);
        // }

        // Apply new background based on theme
        if (theme.getBackgroundImagePath() != null && !theme.getBackgroundImagePath().isEmpty()) {
            File imageFile = new File(theme.getBackgroundImagePath());
            if (imageFile.exists()) {
                try {
                    Image image = new Image(imageFile.toURI().toString());
                    liveThemeBackgroundImageView.setImage(image);
                    // Explicitly unbind before binding to prevent multiple bindings
                    liveThemeBackgroundImageView.fitWidthProperty().unbind();
                    liveThemeBackgroundImageView.fitHeightProperty().unbind();
                    liveThemeBackgroundImageView.fitWidthProperty().bind(livePreviewPane.widthProperty());
                    liveThemeBackgroundImageView.fitHeightProperty().bind(livePreviewPane.heightProperty());
                    liveThemeBackgroundImageView.setPreserveRatio(true);
                    liveThemeBackgroundImageView.setVisible(true);
                    liveThemeBackgroundImageView.setManaged(true);
                    livePreviewPane.setStyle(""); // Clear background color if image is present
                    AppLogger.log("MainController: Applied live preview background image: " + theme.getBackgroundImagePath());
                } catch (Exception e) {
                    AppLogger.log("MainController: Error applying live preview background image: " + e.getMessage());
                    livePreviewPane.setStyle("-fx-background-color: " + theme.getBackgroundColor() + ";"); // Fallback to color
                }
            } else {
                AppLogger.log("MainController: Live preview background image file not found: " + theme.getBackgroundImagePath());
                livePreviewPane.setStyle("-fx-background-color: " + theme.getBackgroundColor() + ";"); // Fallback to color
            }
        } else if (theme.getBackgroundVideoPath() != null && !theme.getBackgroundVideoPath().isEmpty()) {
            File videoFile = new File(theme.getBackgroundVideoPath());
            if (videoFile.exists()) {
                try {
                    Media media = new Media(videoFile.toURI().toString());
                    liveThemeBackgroundMediaPlayer = new MediaPlayer(media);
                    liveThemeBackgroundMediaView.setMediaPlayer(liveThemeBackgroundMediaPlayer);
                    liveThemeBackgroundMediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                    liveThemeBackgroundMediaPlayer.setVolume(0.0); // Mute background video
                    liveThemeBackgroundMediaPlayer.play();
                    // Explicitly unbind before binding to prevent multiple bindings
                    liveThemeBackgroundMediaView.fitWidthProperty().unbind();
                    liveThemeBackgroundMediaView.fitHeightProperty().unbind();
                    liveThemeBackgroundMediaView.fitWidthProperty().bind(livePreviewPane.widthProperty());
                    liveThemeBackgroundMediaView.fitHeightProperty().bind(livePreviewPane.heightProperty());
                    liveThemeBackgroundMediaView.setPreserveRatio(true);
                    liveThemeBackgroundMediaView.setVisible(true);
                    liveThemeBackgroundMediaView.setManaged(true);
                    livePreviewPane.setStyle(""); // Clear background color if video is present
                    AppLogger.log("MainController: Applied live preview background video: " + theme.getBackgroundVideoPath());
                } catch (Exception e) {
                    AppLogger.log("MainController: Error applying live preview background video: " + e.getMessage());
                    livePreviewPane.setStyle("-fx-background-color: " + theme.getBackgroundColor() + ";"); // Fallback to color
                }
            } else {
                AppLogger.log("MainController: Live preview background video file not found: " + theme.getBackgroundVideoPath());
                livePreviewPane.setStyle("-fx-background-color: " + theme.getBackgroundColor() + ";"); // Fallback to color
            }
        } else {
            livePreviewPane.setStyle("-fx-background-color: " + theme.getBackgroundColor() + ";"); // Apply background color
        }
    }


    @FXML
    private void handleThemeSelection(MouseEvent event) {
        if (event.getClickCount() == 2) { // Double-click to apply theme
            Theme selectedTheme = themeListView.getSelectionModel().getSelectedItem();
            if (selectedTheme != null) {
                applyTheme(selectedTheme);
            }
        }
    }

    @FXML
    private void handleShowTitleToggle(ActionEvent event) {
        if (currentActiveTheme != null) {
            currentActiveTheme.setShowTitle(showTitleCheckBox.isSelected());
            applyTheme(currentActiveTheme); // Re-apply theme to propagate change
            saveThemes(); // Save the updated theme setting
            AppLogger.log("Show Title toggled to: " + showTitleCheckBox.isSelected() + " for theme: " + currentActiveTheme.getName());
        }
    }


    @FXML
    private void openThemeEditor() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/praiseview/view/theme-editor-dialog.fxml"));
            DialogPane dialogPane = loader.load();

            ThemeEditorController themeEditorController = loader.getController();
            themeEditorController.setMainController(this); // Pass reference to MainController

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setDialogPane(dialogPane);
            dialog.initOwner(scene.getWindow()); // Set owner to main window
            dialog.initModality(Modality.APPLICATION_MODAL); // Block interaction with other windows

            dialog.showAndWait();

        } catch (IOException e) {
            AppLogger.log("Error opening theme editor: " + e.getMessage());
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Could not open Theme Editor");
            alert.setContentText("An error occurred while loading the theme editor: " + e.getMessage());
            alert.showAndWait();
        }
    }

    /**
     * Dynamically generates a visual preview image for a given theme.
     * This preview shows the theme's background color and a sample text with its font and text color.
     *
     * @param theme The theme for which to generate the preview.
     * @param width The desired width of the preview image.
     * @param height The desired height of the preview image.
     * @return A WritableImage representing the theme preview.
     */
    public Image createThemePreviewImage(Theme theme, int width, int height) {
        StackPane previewRoot = new StackPane();
        previewRoot.setPrefSize(width, height);
        previewRoot.setMinSize(width, height);
        previewRoot.setMaxSize(width, height);
        previewRoot.setAlignment(Pos.CENTER); // Center content in preview

        // Set background color
        try {
            previewRoot.setStyle("-fx-background-color: " + theme.getBackgroundColor() + ";");
        } catch (Exception e) {
            AppLogger.log("Error setting background color for theme preview: " + theme.getBackgroundColor() + " - " + e.getMessage());
            previewRoot.setStyle("-fx-background-color: #000000;"); // Fallback
        }

        VBox contentBox = new VBox(2); // Small spacing between title and text
        contentBox.setAlignment(Pos.CENTER);

        // Add sample title if showTitle is true
        if (theme.isShowTitle()) {
            Text sampleTitle = new Text("Title");
            try {
                sampleTitle.setFont(Font.font(theme.getTitleFontFamily(), FontWeight.BOLD, theme.getTitleFontSize() * 0.3)); // Scale title font size
            } catch (Exception e) {
                AppLogger.log("Error setting title font family for theme preview: " + theme.getTitleFontFamily() + " - " + e.getMessage());
                sampleTitle.setFont(Font.font("System", FontWeight.BOLD, theme.getTitleFontSize() * 0.3)); // Fallback
            }
            try {
                sampleTitle.setFill(Color.web(theme.getTitleTextColor()));
            } catch (Exception e) {
                AppLogger.log("Error setting title text color for theme preview: " + theme.getTitleTextColor() + " - " + e.getMessage());
                sampleTitle.setFill(Color.GOLD); // Fallback
            }
            contentBox.getChildren().add(sampleTitle);
        }

        // Add sample main text
        Text sampleText = new Text("Aa"); // Simple text to show font and color
        try {
            sampleText.setFont(Font.font(theme.getFontFamily(), FontWeight.NORMAL, theme.getFontSize() * 0.3)); // Scale font size
        } catch (Exception e) {
            AppLogger.log("Error setting font family for theme preview: " + theme.getFontFamily() + " - " + e.getMessage());
            sampleText.setFont(Font.font("System", FontWeight.NORMAL, theme.getFontSize() * 0.3)); // Fallback
        }

        try {
            sampleText.setFill(Color.web(theme.getTextColor()));
        } catch (Exception e) {
            AppLogger.log("Error setting text color for theme preview: " + theme.getTextColor() + " - " + e.getMessage());
            sampleText.setFill(Color.WHITE); // Fallback
        }
        contentBox.getChildren().add(sampleText);

        previewRoot.getChildren().add(contentBox);

        // Take a snapshot of the StackPane
        WritableImage image = new WritableImage(width, height);
        previewRoot.snapshot(new SnapshotParameters(), image);
        return image;
    }


    // Renamed from updateVersesList to updateSubItemList
    private void updateSubItemList(Projectable item) {
        ObservableList<SubItemDisplayItem> subItems = FXCollections.observableArrayList();
        ProjectionController proj = PraiseViewApp.getProjectionController();

        if (item != null && proj != null) {
            List<String> paginatedContent = proj.getCurrentProjectedItemPages();

            if (paginatedContent != null && !paginatedContent.isEmpty()) {
                for (int i = 0; i < paginatedContent.size(); i++) {
                    String label = item.getSubItemLabel(i); // Use the item's label logic
                    String contentPreview = paginatedContent.get(i);
                    subItems.add(new SubItemDisplayItem(i, label, contentPreview));
                }
            } else {
                AppLogger.log("MainController: No paginated content available from ProjectionController for sub-item list.");
                // Fallback to showing a single item if no pages are generated
                subItems.add(new SubItemDisplayItem(0, item.getSubItemLabel(0), item.getFullContent()));
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

    @FXML
    private void showAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About PraiseView");
        alert.setHeaderText("PraiseView " + com.praiseview.util.VersionUtil.getVersion());
        
        String content = """
                Modern JavaFX alternative to OpenLP for church projection.
                
                A free and open-source worship projection software built for churches and worship services.
                
                ✨ Current Features:
                • Multi-monitor full-screen projection
                • Song, Prayer & Announcement management
                • Service planner
                • Custom themes (colors, fonts, backgrounds, logos)
                • Media support (Images, Videos, PPT, Background videos)
                • Live preview + navigation controls
                
                🛣️ Future Plans / Roadmap:
                • Smooth Animations & Transitions between slides
                • Mobile App Companion (remote control)
                • AI Helper for automatic slide advancement
                • Improved PowerPoint integration (thumbnails + live control)
                • More import formats (ChordPro, OpenLP, etc.)
                """;
    
        alert.setContentText(content);
        alert.getDialogPane().setMinWidth(520);
        alert.getDialogPane().setMinHeight(440);
    
        ButtonType githubButton = new ButtonType("Visit GitHub");
        alert.getButtonTypes().add(githubButton);
    
        alert.showAndWait().ifPresent(response -> {
            if (response == githubButton) {
                PraiseViewApp.getStaticHostServices().showDocument("https://github.com/jasonfernandes420/PraiseView");
            }
        });
    }

    @FXML
    private void checkForApplicationUpdate() {
        AppLogger.log("MainController: Checking for application updates...");
        if (updateService != null) {
            updateService.checkForUpdate(true); // Pass true to show message even if no update
        } else {
            AppLogger.log("MainController: UpdateService not initialized.");
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Update Error");
            alert.setHeaderText(null);
            alert.setContentText("Update service is not available. Please restart the application.");
            alert.showAndWait();
        }
    }
}
