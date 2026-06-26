package com.praiseview.controller;

import com.praiseview.PraiseViewApp;
import com.praiseview.db.DatabaseService;
import com.praiseview.model.*;
import com.praiseview.service.JsonService;
import com.praiseview.service.PhoneRemoteServer;
import com.praiseview.service.UpdateService;
import com.praiseview.util.AppLogger;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
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
import javafx.stage.Stage;
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
    @FXML private Button moveUpButton;
    @FXML private Button moveDownButton;

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
     
    // Song categories
    @FXML private ListView<String> songCategoryList;
    @FXML private Button clearCategoryFilterButton;

    // Prayer tab
    @FXML private ListView<Prayer> prayerList;
    @FXML private Button addPrayerButton;
    @FXML private Button editPrayerButton;

    // Text tab
    @FXML private ListView<TextSlide> textLibraryList;
    @FXML private Button addTextButton;
    @FXML private Button editTextButton;

    // Media tabs
    @FXML private Button openImageButton;
    @FXML private Button clearImageButton;
    @FXML private ListView<MediaItem> imageList;
    @FXML private Button openVideoButton;
    @FXML private Button clearVideoButton;
    @FXML private ListView<MediaItem> videoList;
    @FXML private Button openAudioButton; // New FXML element for audio
    @FXML private Button clearAudioButton; // New FXML element for audio
    @FXML private ListView<MediaItem> audioList; // New FXML element for audio
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
    private ObservableList<TextSlide> allTexts = FXCollections.observableArrayList(); // New ObservableList for Texts
    private ObservableList<MediaItem> imageLibrary = FXCollections.observableArrayList();
    private ObservableList<MediaItem> videoLibrary = FXCollections.observableArrayList();
    private ObservableList<MediaItem> audioLibrary = FXCollections.observableArrayList(); // New ObservableList for Audio
    private ObservableList<PptItem> pptLibrary = FXCollections.observableArrayList();

    // Theme Management
    private ObservableList<Theme> availableThemes = FXCollections.observableArrayList();
    private static Path THEMES_FILE_PATH; // Changed to Path
    private Theme currentActiveTheme; // The theme currently applied to projection and preview
    private boolean livePreviewThemeHiddenByBlackout = false; // Track if preview theme was hidden by blackout


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
    
    // Track currently loaded service file for Save functionality
    private File currentServiceFile = null;


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
        loadTexts(); // Load texts on startup
        loadThemes(); // Load themes on startup

        // Force re-apply background after full layout
        Platform.runLater(() -> {
            Platform.runLater(() -> {  // Double runLater for safety
                if (currentActiveTheme != null) {
                    applyThemeBackgroundToLivePreview(currentActiveTheme);
                }
            });
        });
        debugBackgroundImage();

        // Initialize UpdateService
        updateService = new UpdateService(PraiseViewApp.getStaticHostServices());

        // Show logo on projected screen on startup
        ProjectionController proj = PraiseViewApp.getProjectionController();
        if (proj != null) {
            proj.clearScreen(true);
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

        // Setup Song Categories
        initializeSongCategories();
        songCategoryList.setOnMouseClicked(e -> {
            String selectedCategory = songCategoryList.getSelectionModel().getSelectedItem();
            if (selectedCategory != null && !selectedCategory.isEmpty()) {
                filteredSongs.setPredicate(song -> {
                    String lower = searchField.getText() != null ? searchField.getText().toLowerCase() : "";
                    boolean categoryMatch;
                     
                    if (selectedCategory.equals("[All Songs]")) {
                        categoryMatch = true;
                    } else if (selectedCategory.equals("Others")) {
                        // Match songs with no category or empty category
                        categoryMatch = (song.getCategory() == null || song.getCategory().trim().isEmpty());
                    } else {
                        // Match songs that contain this category (handling comma-separated categories)
                        if (song.getCategory() != null && !song.getCategory().isEmpty()) {
                            String[] categories = song.getCategory().split(",");
                            categoryMatch = false;
                            for (String cat : categories) {
                                if (cat.trim().equals(selectedCategory)) {
                                    categoryMatch = true;
                                    break;
                                }
                            }
                        } else {
                            categoryMatch = false;
                        }
                    }
                     
                    if (lower.isEmpty()) {
                        return categoryMatch;
                    } else {
                        return categoryMatch && (song.getTitle().toLowerCase().contains(lower) ||
                                (song.getCategory() != null && song.getCategory().toLowerCase().contains(lower)));
                    }
                });
            }
        });
         
        clearCategoryFilterButton.setOnAction(e -> {
            songCategoryList.getSelectionModel().clearSelection();
            filteredSongs.setPredicate(song -> {
                if (searchField.getText() == null || searchField.getText().isEmpty()) return true;
                String lower = searchField.getText().toLowerCase();
                return song.getTitle().toLowerCase().contains(lower) ||
                        (song.getCategory() != null && song.getCategory().toLowerCase().contains(lower));
            });
        });

        // Setup Service Planner
        servicePlannerList.setItems(serviceQueue);
        serviceQueue.addListener((ListChangeListener<ServiceItem>) change -> sendServiceListToPhone());
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
                    } else if (serviceItem.getContent() instanceof TextSlide) { // Handle TextSlide type
                        prefix = "TXT";
                    } else if (serviceItem.getContent() instanceof MediaItem) {
                        MediaItem media = (MediaItem) serviceItem.getContent();
                        if (media.getMediaType() == MediaItem.MediaType.IMAGE) {
                            prefix = "IMG";
                        } else if (media.getMediaType() == MediaItem.MediaType.VIDEO) {
                            prefix = "VID";
                        } else if (media.getMediaType() == MediaItem.MediaType.AUDIO) { // Handle Audio type
                            prefix = "AUD";
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

        // Handle DELETE key press on service list
        servicePlannerList.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.DELETE) {
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
                e.consume();
            } else if (e.isControlDown() && e.getCode() == javafx.scene.input.KeyCode.UP) {
                moveServiceItemUp();
                e.consume();
            } else if (e.isControlDown() && e.getCode() == javafx.scene.input.KeyCode.DOWN) {
                moveServiceItemDown();
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
        
        // Service list reorder buttons
        moveUpButton.setOnAction(e -> moveServiceItemUp());
        moveDownButton.setOnAction(e -> moveServiceItemDown());

        // Verse Navigation
        if (nextVerseButton != null) nextVerseButton.setOnAction(e -> nextItemOrSubItem());
        if (prevVerseButton != null) prevVerseButton.setOnAction(e -> previousItemOrSubItem());

        // Video Controls
        if (videoPlayPauseButton != null) videoPlayPauseButton.setOnAction(e -> playPauseMedia()); // Changed to playPauseMedia
        if (videoRewindButton != null) videoRewindButton.setOnAction(e -> onMediaRewind()); // Changed to onMediaRewind
        if (videoForwardButton != null) videoForwardButton.setOnAction(e -> onMediaForward()); // Changed to onMediaForward


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

        // Texts
        if (textLibraryList != null) textLibraryList.setItems(allTexts);
        if (addTextButton != null) addTextButton.setOnAction(e -> addNewText());
        if (editTextButton != null) editTextButton.setOnAction(e -> editSelectedText());

        // Media Tab Setup
        if (imageList != null) imageList.setItems(imageLibrary);
        if (openImageButton != null) openImageButton.setOnAction(e -> openMediaFiles(MediaItem.MediaType.IMAGE));
        if (clearImageButton != null) clearImageButton.setOnAction(e -> imageLibrary.clear());

        if (videoList != null) videoList.setItems(videoLibrary);
        if (openVideoButton != null) openVideoButton.setOnAction(e -> openMediaFiles(MediaItem.MediaType.VIDEO));
        if (clearVideoButton != null) clearVideoButton.setOnAction(e -> videoLibrary.clear());

        if (audioList != null) audioList.setItems(audioLibrary); // Setup audioList
        if (openAudioButton != null) openAudioButton.setOnAction(e -> openMediaFiles(MediaItem.MediaType.AUDIO)); // Setup openAudioButton
        if (clearAudioButton != null) clearAudioButton.setOnAction(e -> audioLibrary.clear()); // Setup clearAudioButton

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

       /* // Add listeners to livePreviewPane dimensions to re-apply theme background
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
        });*/
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

    private void loadTexts() {
        allTexts.setAll(dbService.loadAllTexts());
        if (textLibraryList != null) {
            textLibraryList.refresh();
        }
    }

    private void initializeSongCategories() {
        // Get unique categories from all songs (handling comma-separated categories)
        Set<String> categories = new TreeSet<>();
        categories.add("[All Songs]"); // Add "All Songs" as first option
         
        boolean[] hasUncategorized = {false};
        allSongs.forEach(song -> {
            if (song.getCategory() != null && !song.getCategory().trim().isEmpty()) {
                // Split by comma and add each category individually
                String[] cats = song.getCategory().split(",");
                for (String cat : cats) {
                    String trimmedCat = cat.trim();
                    if (!trimmedCat.isEmpty()) {
                        categories.add(trimmedCat);
                    }
                }
            } else {
                hasUncategorized[0] = true;
            }
        });
         
        if (hasUncategorized[0]) {
            categories.add("Others"); // Add "Others" for uncategorized songs
        }
         
        // Update the category list
        songCategoryList.setItems(FXCollections.observableArrayList(categories));
         
        // Listen for changes to allSongs and refresh categories
        allSongs.addListener((ListChangeListener<Song>) change -> {
            Set<String> updatedCategories = new TreeSet<>();
            updatedCategories.add("[All Songs]");
             
            boolean[] hasUncategorizedSongs = {false};
            allSongs.forEach(song -> {
                if (song.getCategory() != null && !song.getCategory().trim().isEmpty()) {
                    // Split by comma and add each category individually
                    String[] cats = song.getCategory().split(",");
                    for (String cat : cats) {
                        String trimmedCat = cat.trim();
                        if (!trimmedCat.isEmpty()) {
                            updatedCategories.add(trimmedCat);
                        }
                    }
                } else {
                    hasUncategorizedSongs[0] = true;
                }
            });
             
            if (hasUncategorizedSongs[0]) {
                updatedCategories.add("Others");
            }
             
            songCategoryList.setItems(FXCollections.observableArrayList(updatedCategories));
        });
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

        // === Drag from Texts ===
        if (textLibraryList != null) {
            textLibraryList.setOnDragDetected(e -> {
                TextSlide selected = textLibraryList.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    Dragboard db = textLibraryList.startDragAndDrop(TransferMode.COPY);
                    ClipboardContent content = new ClipboardContent();
                    content.putString("TEXT:" + selected.getId()); // Use ID for lookup
                    db.setContent(content);
                    e.consume();
                }
            });
        }

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

        // === Drag from Audio List ===
        if (audioList != null) {
            audioList.setOnDragDetected(e -> {
                MediaItem selected = audioList.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    Dragboard db = audioList.startDragAndDrop(TransferMode.COPY);
                    ClipboardContent content = new ClipboardContent();
                    content.putString("AUDIO:" + selected.getFilePath()); // Use file path for lookup
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
                else if (data.startsWith("TEXT:")) { // Handle Text drag
                    String textId = data.substring("TEXT:".length());
                    // Retrieve the actual TextSlide object from allTexts
                    TextSlide text = allTexts.stream().filter(t -> t.getId().equals(textId)).findFirst().orElse(null);
                    if (text != null) {
                        newItem = new ServiceItem(text);
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
                else if (data.startsWith("AUDIO:")) { // Handle Audio drag
                    String filePath = data.substring("AUDIO:".length());
                    File file = new File(filePath);
                    if (file.exists()) {
                        newItem = new ServiceItem(new MediaItem(file, MediaItem.MediaType.AUDIO));
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

    @FXML
    private void addNewText() {
        openTextEditor(null);
    }

    @FXML
    private void editSelectedText() {
        TextSlide selected = textLibraryList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            openTextEditor(selected);
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Please select a text in the library to edit.");
            alert.show();
        }
    }

    private void openTextEditor(TextSlide text) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/praiseview/view/text-dialog.fxml"));
            DialogPane dialogPane = loader.load();

            TextDialogController controller = loader.getController();
            controller.setText(text); // Pass the text object to the controller

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setDialogPane(dialogPane);
            dialog.initOwner(scene.getWindow());
            dialog.initModality(Modality.APPLICATION_MODAL);

            dialog.showAndWait().ifPresent(result -> {
                if (result == ButtonType.OK) {
                    TextSlide resultText = controller.getText();
                    if (resultText != null) {
                        dbService.saveText(resultText); // Save to DB
                        loadTexts(); // Refresh list
                        AppLogger.log("Text saved: " + resultText.getTitle());
                    }
                }
            });

        } catch (IOException e) {
            AppLogger.log("Error opening text editor: " + e.getMessage());
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Could not open Text Editor");
            alert.setContentText("An error occurred while loading the text editor: " + e.getMessage());
            alert.showAndWait();
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
            case AUDIO: // New case for AUDIO
                fileChooser.getExtensionFilters().addAll(
                        new FileChooser.ExtensionFilter("Audio Files", "*.mp3", "*.wav", "*.aac", "*.m4a"),
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
                        Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to load PPT: " + e.getMessage());
                        alert.show();
                    }
                } else if (type == MediaItem.MediaType.IMAGE) {
                    imageLibrary.add(new MediaItem(file, type)); // Create MediaItem
                } else if (type == MediaItem.MediaType.VIDEO) {
                    videoLibrary.add(new MediaItem(file, type)); // Create MediaItem
                } else if (type == MediaItem.MediaType.AUDIO) { // Add to audioLibrary
                    audioLibrary.add(new MediaItem(file, type));
                }
            }
        }
    }


    private void startProjection() {
        // Ensure the projection stage is open before attempting to project
        PraiseViewApp.ensureProjectionStageOpen();

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
        }else{
            AppLogger.log("ProjectionController is null after ensureProjectionStageOpen. Cannot show item.");
        }
        updateCenterPreview(); // Mirror the projection
        livePreviewPane.requestFocus(); // Ensure focus for arrow keys
        notifyPhoneRemoteStateChanged();
    }

    private void showCurrentItem() {
        // Ensure the projection stage is open before attempting to project
        PraiseViewApp.ensureProjectionStageOpen();

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
        } else {
            AppLogger.log("ProjectionController is null after ensureProjectionStageOpen. Cannot show item.");
        }
        updateCenterPreview(); // Mirror the projection
        livePreviewPane.requestFocus(); // Ensure focus for arrow keys
        notifyPhoneRemoteStateChanged();
    }

    private void notifyPhoneRemoteStateChanged() {
        sendServiceListToPhone();
        sendVerseListToPhone();
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
        // Disable video controls when not showing video or audio
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

        // Restore theme background if it was hidden by blackout
        if (livePreviewThemeHiddenByBlackout && currentActiveTheme != null) {
            // Clear the black background set by blackout
            if (livePreviewPane != null) {
                livePreviewPane.setStyle(""); // Clear inline styles
            }
            applyThemeBackgroundToLivePreview(currentActiveTheme);
            livePreviewThemeHiddenByBlackout = false; // Clear flag
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
        AppLogger.log("MainController: Displayed Content (first 50 chars): " + (displayedContent != null && displayedContent.length() > 50 ? displayedContent.substring(0, 50) + "..." : displayedContent));

        // Get actual projection dimensions for proper aspect ratio
        double projectionWidth = proj.projectionRoot.getWidth();
        double projectionHeight = proj.projectionRoot.getHeight();
        double projectionAspectRatio = projectionWidth / projectionHeight; // Should be 16:9 or similar
        
        AppLogger.log("MainController: Projection dimensions - Width: " + projectionWidth + ", Height: " + projectionHeight + ", Aspect Ratio: " + projectionAspectRatio);

        // Set title visibility based on currentActiveTheme.showTitle
        if (currentActiveTheme != null && currentActiveTheme.isShowTitle()) {
            stageViewTitle.setText(displayedTitle);
            stageViewTitle.setVisible(true);
            stageViewTitle.setManaged(true);
            
            // Use actual projection dimensions for scaling
            double previewWidth = livePreviewPane.getWidth();
            double scaleFactor = previewWidth / projectionWidth;
            double previewFontSize = currentActiveTheme.getTitleFontSize() * scaleFactor;

            stageViewTitle.setStyle(String.format(
                    "-fx-font-family: '%s'; -fx-font-size: %.1fpx; -fx-text-fill: %s;",
                    currentActiveTheme.getTitleFontFamily(),
                    previewFontSize,
                    currentActiveTheme.getTitleTextColor()
            ));
        } else {
            stageViewTitle.setText("");
            stageViewTitle.setVisible(false);
            stageViewTitle.setManaged(false);
        }


        switch (currentProjectedItem.getType()) {
            case "SONG":
            case "PRAYER":
            case "TEXT": // Handle Text type
            case "ANNOUNCEMENT":
                if (liveTextContentContainer != null) {
                    liveTextContentContainer.setVisible(true);
                    liveTextContentContainer.setManaged(true);
                    
                    // Calculate available width using same padding ratio as projection
                    // Projection uses: availableWidth = projectionRoot.getWidth() - (2 * TEXT_HORIZONTAL_PADDING)
                    // where TEXT_HORIZONTAL_PADDING = 50.0
                    double projectionPadding = 50.0;
                    double projectionAvailableWidth = projectionWidth - (2 * projectionPadding);
                    
                    // Scale padding proportionally for preview
                    double scaleFactor = livePreviewPane.getWidth() / projectionWidth;
                    double previewPadding = projectionPadding * scaleFactor;
                    double previewAvailableWidth = livePreviewPane.getWidth() - (2 * previewPadding);
                    
                    // Set padding on container
                    liveTextContentContainer.setPadding(new Insets(previewPadding, previewPadding, previewPadding, previewPadding));
                    
                    // Constrain TextFlow to available width for proper wrapping
                    liveTextContentContainer.setPrefWidth(livePreviewPane.getWidth());
                    liveTextContentContainer.setMaxWidth(livePreviewPane.getWidth());
                    
                    if (livePreviewText != null) {
                        livePreviewText.setPrefWidth(previewAvailableWidth);
                        livePreviewText.setMaxWidth(previewAvailableWidth);
                    }
                    
                    AppLogger.log("MainController: Preview padding scale - Projection padding: " + projectionPadding + 
                                 ", Preview padding: " + previewPadding + 
                                 ", Available width: " + previewAvailableWidth);
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

                    // Scale text based on actual projection dimensions
                    double previewWidth = livePreviewPane.getWidth();
                    double scaleFactor = previewWidth / projectionWidth;
                    double previewFontSize = currentActiveTheme.getFontSize() * scaleFactor;
                    double previewLineSpacing = currentActiveTheme.getLineSpacing() * scaleFactor;

                    mainText.setStyle(String.format(
                            "-fx-font-family: '%s'; " +
                                    "-fx-font-size: %.1fpx; " +
                                    "-fx-line-spacing: %.1fpx;",
                            currentActiveTheme.getFontFamily(),
                            previewFontSize,
                            previewLineSpacing
                    ));
                    livePreviewText.getChildren().add(mainText);
                    String alignment = currentActiveTheme != null ? currentActiveTheme.getTextAlignment() : null;
                    if ("LEFT".equalsIgnoreCase(alignment)) {
                        livePreviewText.setTextAlignment(javafx.scene.text.TextAlignment.LEFT);
                    } else if ("RIGHT".equalsIgnoreCase(alignment)) {
                        livePreviewText.setTextAlignment(javafx.scene.text.TextAlignment.RIGHT);
                    } else {
                        livePreviewText.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
                    }
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
            case "AUDIO": // Handle AUDIO type similarly to VIDEO for media controls
                if (liveItemMediaView != null) {
                    liveItemMediaView.setVisible(currentProjectedItem.getType().equals("VIDEO")); // Only show MediaView for video
                    liveItemMediaView.setManaged(currentProjectedItem.getType().equals("VIDEO")); // Only manage MediaView for video

                    File mediaFile = new File(((MediaItem)currentProjectedItem).getFilePath());
                    AppLogger.log("MainController: Preparing " + currentProjectedItem.getType() + " preview for: " + mediaFile.getAbsolutePath());
                    if (mediaFile.exists()) {
                        Media media = new Media(mediaFile.toURI().toString());
                        // liveItemMediaPlayer is already stopped/disposed at the start of updateCenterPreview
                        liveItemMediaPlayer = new MediaPlayer(media);
                        liveItemMediaView.setMediaPlayer(liveItemMediaPlayer);
                        liveItemMediaPlayer.setCycleCount(MediaPlayer.INDEFINITE); // Loop video/audio
                        liveItemMediaPlayer.setVolume(0.0); // Always mute the preview media player
                        liveItemMediaPlayer.play(); // Explicitly play the media

                        liveItemMediaView.fitWidthProperty().bind(livePreviewPane.widthProperty());
                        liveItemMediaView.fitHeightProperty().bind(livePreviewPane.heightProperty());
                        liveItemMediaView.setPreserveRatio(true);

                        // Enable media controls
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
                        AppLogger.log("MainController: " + currentProjectedItem.getType() + " file not found for preview: " + mediaFile.getAbsolutePath());
                        // Display error message on screen
                        if (liveTextContentContainer != null) {
                            liveTextContentContainer.setVisible(true);
                            liveTextContentContainer.setManaged(true);
                        }
                        stageViewTitle.setText("Error Loading " + currentProjectedItem.getType());
                        if (livePreviewText != null) {
                            livePreviewText.getChildren().clear();
                            livePreviewText.getChildren().add(new Text("File not found: " + mediaFile.getName()));
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
    private void playPauseMedia() { // Renamed from playPauseVideo
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
            proj.playPauseMedia(); // Changed to playPauseMedia
        }
    }

    @FXML
    private void onMediaRewind() { // Renamed from onVideoRewind
        seekMedia(-10.0); // Changed to seekMedia
    }

    @FXML
    private void onMediaForward() { // Renamed from onVideoForward
        seekMedia(10.0); // Changed to seekMedia
    }

    public boolean handleRemoteCommand(String command) {
        if (command == null || command.isBlank()) {
            return false;
        }

        String trimmed = command.trim();
        
        // Handle JSON commands with parameters
        if (trimmed.startsWith("{")) {
            return handleJsonRemoteCommand(trimmed);
        }
        
        // Handle simple text commands
        return handleSimpleRemoteCommand(trimmed.toLowerCase(Locale.ROOT));
    }

    private boolean handleJsonRemoteCommand(String jsonCommand) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            java.util.Map<String, Object> cmdMap = mapper.readValue(jsonCommand, java.util.Map.class);
            
            String cmd = (String) cmdMap.get("command");
            if (cmd == null) {
                return false;
            }
            
            cmd = cmd.toLowerCase(Locale.ROOT);
            
            return switch (cmd) {
                case "get-services" -> {
                    sendServiceListToPhone();
                    yield true;
                }
                case "select-service" -> {
                    Object indexObj = cmdMap.get("index");
                    if (indexObj != null) {
                        int index = ((Number) indexObj).intValue();
                        selectServiceAndSendVerses(index);
                        yield true;
                    }
                    yield false;
                }
                case "select-verse" -> {
                    Object verseIndexObj = cmdMap.get("verse_index");
                    Object contentIdObj = cmdMap.get("content_id");
                    if (verseIndexObj != null && contentIdObj != null) {
                        int verseIndex = ((Number) verseIndexObj).intValue();
                        String contentId = (String) contentIdObj;
                        selectVerseAndProject(contentId, verseIndex);
                        yield true;
                    }
                    yield false;
                }
                default -> handleSimpleRemoteCommand(cmd);
            };
        } catch (Exception e) {
            AppLogger.log("Error parsing JSON remote command: " + e.getMessage());
            return false;
        }
    }

    private boolean handleSimpleRemoteCommand(String command) {
        return switch (command) {
            case "start", "project" -> {
                startProjection();
                yield true;
            }
            case "next" -> {
                nextItemOrSubItem();
                yield true;
            }
            case "previous", "prev" -> {
                previousItemOrSubItem();
                yield true;
            }
            case "blackout" -> {
                blackout();
                yield true;
            }
            case "clear" -> {
                clearScreen();
                yield true;
            }
            case "playpause", "play-pause" -> {
                playPauseMedia();
                yield true;
            }
            case "rewind" -> {
                onMediaRewind();
                yield true;
            }
            case "forward" -> {
                onMediaForward();
                yield true;
            }
            case "get-services" -> {
                sendServiceListToPhone();
                yield true;
            }
            default -> {
                AppLogger.log("Unknown phone remote command: " + command);
                yield false;
            }
        };
    }

    private void sendServiceListToPhone() {
        PhoneRemoteServer server = PhoneRemoteServer.getInstance();
        if (server == null) {
            return;
        }

        java.util.List<ServiceListDTO.ServiceItemDTO> items = new java.util.ArrayList<>();
        for (int i = 0; i < serviceQueue.size(); i++) {
            ServiceItem item = serviceQueue.get(i);
            items.add(new ServiceListDTO.ServiceItemDTO(
                item.getId(),
                i,
                item.getTitle(),
                item.getType()
            ));
        }

        ServiceListDTO dto = new ServiceListDTO(items, currentQueueIndex);
        server.sendServiceListToClients(dto);
    }

    private void selectServiceAndSendVerses(int serviceIndex) {
        if (serviceIndex < 0 || serviceIndex >= serviceQueue.size()) {
            AppLogger.log("Invalid service index: " + serviceIndex);
            return;
        }

        currentQueueIndex = serviceIndex;
        currentSubItemIndex = 0;
        showCurrentItem();
    }

    private void selectVerseAndProject(String contentId, int verseIndex) {
        if (contentId == null || contentId.isBlank()) {
            AppLogger.log("Invalid content ID");
            return;
        }

        // Find the service by content ID
        int serviceIndex = -1;
        for (int i = 0; i < serviceQueue.size(); i++) {
            ServiceItem item = serviceQueue.get(i);
            Projectable content = item.getContent();
            if (content instanceof Song) {
                if (((Song) content).getId().equals(contentId)) {
                    serviceIndex = i;
                    break;
                }
            } else if (content instanceof Prayer) {
                if (((Prayer) content).getId().equals(contentId)) {
                    serviceIndex = i;
                    break;
                }
            } else if (content instanceof TextSlide) {
                if (((TextSlide) content).getId().equals(contentId)) {
                    serviceIndex = i;
                    break;
                }
            } else if (content instanceof MediaItem) {
                if (((MediaItem) content).getId().equals(contentId)) {
                    serviceIndex = i;
                    break;
                }
            } else if (content instanceof PptItem) {
                if (((PptItem) content).getId().equals(contentId)) {
                    serviceIndex = i;
                    break;
                }
            }
        }

        if (serviceIndex < 0) {
            AppLogger.log("Content not found with ID: " + contentId);
            return;
        }

        currentQueueIndex = serviceIndex;
        
        ServiceItem currentItem = serviceQueue.get(currentQueueIndex);
        Projectable projectable = currentItem.getContent();
        
        if (projectable == null) {
            return;
        }

        ProjectionController proj = PraiseViewApp.getProjectionController();
        if (proj == null) {
            return;
        }

        int maxVerses = proj.getCurrentProjectedItemSubItemCount();
        if (verseIndex >= 0 && verseIndex < maxVerses) {
            currentSubItemIndex = verseIndex;
            showCurrentItem();
        } else {
            AppLogger.log("Invalid verse index: " + verseIndex + " (max: " + maxVerses + ")");
        }
    }

    private void sendVerseListToPhone() {
        PhoneRemoteServer server = PhoneRemoteServer.getInstance();
        if (server == null || currentQueueIndex < 0 || currentQueueIndex >= serviceQueue.size()) {
            return;
        }

        ServiceItem currentItem = serviceQueue.get(currentQueueIndex);
        Projectable projectable = currentItem.getContent();
        
        if (projectable == null) {
            return;
        }

        ProjectionController proj = PraiseViewApp.getProjectionController();
        if (proj == null) {
            return;
        }

        String contentId = getContentId(projectable);
        if (contentId == null) {
            AppLogger.log("Could not get content ID for projectable");
            return;
        }

        java.util.List<String> projectedPages = proj.getCurrentProjectedItemPages();
        if (projectedPages == null || projectedPages.isEmpty()) {
            AppLogger.log("No projected pages available for phone verse list.");
            return;
        }

        int maxVerses = projectedPages.size();
        java.util.List<VerseListDTO.VerseItemDTO> verseItems = new java.util.ArrayList<>();

        for (int i = 0; i < maxVerses; i++) {
            String label = projectable.getSubItemLabel(i);
            String content = projectedPages.get(i);
            String preview = content.length() > 100 ? content.substring(0, 100) + "..." : content;
            verseItems.add(new VerseListDTO.VerseItemDTO(i, label, preview, content));
        }

        VerseListDTO dto = new VerseListDTO(
            currentItem.getTitle(),
            contentId,
            verseItems,
            currentSubItemIndex
        );
        server.sendVerseListToClients(dto);
    }

    private String getContentId(Projectable projectable) {
        if (projectable instanceof Song) {
            return ((Song) projectable).getId();
        } else if (projectable instanceof Prayer) {
            return ((Prayer) projectable).getId();
        } else if (projectable instanceof TextSlide) {
            return ((TextSlide) projectable).getId();
        } else if (projectable instanceof MediaItem) {
            return ((MediaItem) projectable).getId();
        } else if (projectable instanceof PptItem) {
            return ((PptItem) projectable).getId();
        }
        return null;
    }

    private void seekMedia(double seconds) { // Renamed from seekVideo
        // Control local preview media player
        if (liveItemMediaPlayer != null && liveItemMediaPlayer.getStatus() != MediaPlayer.Status.STOPPED) {
            Duration currentTime = liveItemMediaPlayer.getCurrentTime();
            Duration newTime = currentTime.add(Duration.seconds(seconds));
            liveItemMediaPlayer.seek(newTime);
        }

        // Control projection media player
        ProjectionController proj = PraiseViewApp.getProjectionController();
        if (proj != null) {
            proj.seekMedia(seconds); // Changed to seekMedia
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
        }

        // Mirror blackout in live preview pane
        if (livePreviewPane != null) {
            livePreviewPane.setStyle("-fx-background-color: black;");

            // Hide live preview background image
            if (liveThemeBackgroundImageView != null) {
                liveThemeBackgroundImageView.setVisible(false);
                liveThemeBackgroundImageView.setManaged(false);
                liveThemeBackgroundImageView.setImage(null);
            }

            if (liveThemeBackgroundMediaView != null) {
                liveThemeBackgroundMediaView.setVisible(false);
                liveThemeBackgroundMediaView.setManaged(false);
            }
        }

        livePreviewThemeHiddenByBlackout = true; // Mark preview theme as hidden by blackout
        updateCenterPreview(); // or whatever method refreshes the preview
        AppLogger.log("MainController: Blackout mirrored to preview pane.");
    }

    private void clearScreen() {
        ProjectionController proj = PraiseViewApp.getProjectionController();
        if (proj != null) {
            proj.clear(); // This will now call showLogo() internally
            updateCenterPreview(); // Mirror the clear state in the stage view
        }
        //showLivePreviewLogo(); // Show logo in live preview as well
    }

    private void moveServiceItemUp() {
        int selectedIdx = servicePlannerList.getSelectionModel().getSelectedIndex();
        if (selectedIdx > 0) {
            // Swap items
            ServiceItem item = serviceQueue.remove(selectedIdx);
            serviceQueue.add(selectedIdx - 1, item);
            
            // Maintain selection on the moved item
            servicePlannerList.getSelectionModel().select(selectedIdx - 1);
            servicePlannerList.scrollTo(selectedIdx - 1);
            
            AppLogger.log("Service item moved up from " + selectedIdx + " to " + (selectedIdx - 1));
        }
    }

    private void moveServiceItemDown() {
        int selectedIdx = servicePlannerList.getSelectionModel().getSelectedIndex();
        if (selectedIdx >= 0 && selectedIdx < serviceQueue.size() - 1) {
            // Swap items
            ServiceItem item = serviceQueue.remove(selectedIdx);
            serviceQueue.add(selectedIdx + 1, item);
            
            // Maintain selection on the moved item
            servicePlannerList.getSelectionModel().select(selectedIdx + 1);
            servicePlannerList.scrollTo(selectedIdx + 1);
            
            AppLogger.log("Service item moved down from " + selectedIdx + " to " + (selectedIdx + 1));
        }
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
            currentServiceFile = null; // Reset the loaded file reference
            clearScreen(); // Clear main preview as well
            updateWindowTitle(); // Update window title back to default
            AppLogger.log("New service created");
        }
    }

    @FXML private void saveService() {
        if (serviceQueue.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Cannot save empty service. Add items first.");
            alert.show();
            return;
        }

        // If we already have a file loaded, save directly to it
        if (currentServiceFile != null) {
            jsonService.saveService(new java.util.ArrayList<>(serviceQueue), currentServiceFile);
            AppLogger.log("Service saved: " + currentServiceFile.getName());
            updateWindowTitle();
            return;
        }

        // Otherwise, show Save As dialog
        FileChooser fc = new FileChooser();
        fc.setTitle("Save Service");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Service Files", "*.service"));
        fc.setInitialFileName("service_" + System.currentTimeMillis() + ".service");

        File file = fc.showSaveDialog(scene.getWindow());
        if (file != null) {
            currentServiceFile = file;
            jsonService.saveService(new java.util.ArrayList<>(serviceQueue), file);
            AppLogger.log("Service saved: " + file.getName());
            updateWindowTitle();
        }
    }

    @FXML private void saveServiceAs() {
        if (serviceQueue.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Cannot save empty service. Add items first.");
            alert.show();
            return;
        }

        // Always show Save As dialog
        FileChooser fc = new FileChooser();
        fc.setTitle("Save Service As");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Service Files", "*.service"));
        
        // Pre-populate with current file name if one exists
        if (currentServiceFile != null) {
            fc.setInitialFileName(currentServiceFile.getName());
            fc.setInitialDirectory(currentServiceFile.getParentFile());
        } else {
            fc.setInitialFileName("service_" + System.currentTimeMillis() + ".service");
        }

        File file = fc.showSaveDialog(scene.getWindow());
        if (file != null) {
            currentServiceFile = file; // Update to new file
            jsonService.saveService(new java.util.ArrayList<>(serviceQueue), file);
            AppLogger.log("Service saved as: " + file.getName());
            updateWindowTitle();
        }
    }

    @FXML private void importService() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Load Service");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Service Files", "*.service"));

        File file = fc.showOpenDialog(scene.getWindow());
        if (file != null) {
            java.util.List<ServiceItem> loadedService = jsonService.loadService(file);
            if (loadedService != null && !loadedService.isEmpty()) {
                serviceQueue.clear();
                serviceQueue.addAll(loadedService);
                currentServiceFile = file; // Track the loaded file for future saves
                // For Announcement, rePaginate is still used as its implementation hasn't changed
                /*for (ServiceItem item : serviceQueue) {
                    Projectable projectable = item.getContent();
                    if (projectable instanceof Announcement) {
                        // This call is now redundant as ProjectionController will handle pagination for all text types
                        // ((Announcement) projectable).rePaginate(PREVIEW_FONT_SIZE, PREVIEW_WIDTH_DEFAULT, PREVIEW_HEIGHT_DEFAULT);
                    }
                }*/
                servicePlannerList.refresh();
                currentQueueIndex = -1;
                currentSubItemIndex = 0;
                clearScreen(); // Clear main preview after loading new service
                updateWindowTitle(); // Update window title with loaded file name
                sendServiceListToPhone();
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
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Files", "*.*"));
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
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Files", "*.*"));

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
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Files", "*.*"));
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
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Files", "*.*"));

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

    @FXML private void exportTexts() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Texts");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
        fileChooser.setInitialFileName("texts_export.json");

        File file = fileChooser.showSaveDialog(null);
        if (file != null) {
            jsonService.exportTexts(new ArrayList<>(allTexts), file);
            AppLogger.log("Texts exported to: " + file.getAbsolutePath());
        }
    }

    @FXML private void importTexts() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Texts");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));

        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            List<TextSlide> importedTexts = jsonService.importTexts(file);
            if (importedTexts != null && !importedTexts.isEmpty()) {
                // Check for duplicates before adding
                int initialSize = allTexts.size();
                for (TextSlide newText : importedTexts) {
                    if (allTexts.stream().noneMatch(existingText -> existingText.getId().equals(newText.getId()))) {
                        allTexts.add(newText);
                        dbService.saveText(newText); // Save imported text to DB
                    } else {
                        AppLogger.log("Skipping duplicate text on import: " + newText.getTitle());
                    }
                }
                if (allTexts.size() > initialSize) {
                    textLibraryList.refresh();
                    AppLogger.log((allTexts.size() - initialSize) + " new texts imported from: " + file.getAbsolutePath());
                } else {
                    AppLogger.log("No new texts imported (all were duplicates or file was empty): " + file.getAbsolutePath());
                }
            } else {
                AppLogger.log("No texts imported or file was empty: " + file.getAbsolutePath());
            }
        }
    }

    @FXML private void exitApp() {
        AppLogger.log("Application exited");
        PhoneRemoteServer.stopServer();
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
            Theme defaultTheme = new Theme();
            availableThemes.add(defaultTheme);
            saveThemes();
            AppLogger.log("Default theme created and saved.");
        }

        // Ensure currentActiveTheme is set
        currentActiveTheme = availableThemes.getFirst();
        applyTheme(currentActiveTheme);   // This should still be here
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

        // Apply new background based on theme
        if (theme.getBackgroundImagePath() != null && !theme.getBackgroundImagePath().isEmpty()) {
            File imageFile = new File(theme.getBackgroundImagePath());
            if (imageFile.exists()) {
                try {
                    Image image = new Image(imageFile.toURI().toString(), true);

                    liveThemeBackgroundImageView.setImage(image);

                    // Unbind before any set to avoid "bound value cannot be set" error
                    liveThemeBackgroundImageView.fitWidthProperty().unbind();
                    liveThemeBackgroundImageView.fitHeightProperty().unbind();

                    // Bind to pane (this is the correct way)
                    liveThemeBackgroundImageView.fitWidthProperty().bind(livePreviewPane.widthProperty());
                    liveThemeBackgroundImageView.fitHeightProperty().bind(livePreviewPane.heightProperty());

                    liveThemeBackgroundImageView.setPreserveRatio(true);
                    liveThemeBackgroundImageView.setSmooth(true);
                    liveThemeBackgroundImageView.setVisible(true);
                    liveThemeBackgroundImageView.setManaged(true);

                    livePreviewPane.setStyle("");
                    AppLogger.log("MainController: Successfully applied background image: " + theme.getBackgroundImagePath());
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
                    liveThemeBackgroundMediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                    liveThemeBackgroundMediaPlayer.setVolume(0.0); // Mute background video
                    liveThemeBackgroundMediaPlayer.play();
                    liveThemeBackgroundMediaView.setMediaPlayer(liveThemeBackgroundMediaPlayer);
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
        debugBackgroundImage();
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

        Canvas canvas = new Canvas(width, height);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Background
        try {

            if (theme.getBackgroundImagePath() != null &&
                    !theme.getBackgroundImagePath().isBlank()) {

                File imageFile = new File(theme.getBackgroundImagePath());

                if (imageFile.exists()) {
                    Image bgImage = new Image(imageFile.toURI().toString());
                    gc.drawImage(bgImage, 0, 0, width, height);
                } else {
                    gc.setFill(Color.web(theme.getBackgroundColor()));
                    gc.fillRect(0, 0, width, height);
                }

            }else if (theme.getBackgroundVideoPath() != null &&
                    !theme.getBackgroundVideoPath().isBlank()) {

                gc.setFill(Color.web(theme.getBackgroundColor()));
                gc.fillRect(0, 0, width, height);

                gc.setFill(Color.RED);
                gc.fillRoundRect(width - 32, 4, 28, 12, 4, 4);

                gc.setFill(Color.WHITE);
                gc.setFont(Font.font("Arial", FontWeight.BOLD, 8));
                gc.fillText("VID", width - 28, 13);



            } else {

                gc.setFill(Color.web(theme.getBackgroundColor()));
                gc.fillRect(0, 0, width, height);

            }

        } catch (Exception e) {

            AppLogger.log("Theme preview background error: " + e.getMessage());

            gc.setFill(Color.BLACK);
            gc.fillRect(0, 0, width, height);
        }

        // Border
        gc.setStroke(Color.GRAY);
        gc.strokeRect(0, 0, width - 1, height - 1);

        // Title preview
        if (theme.isShowTitle()) {

            try {
                gc.setFill(Color.web(theme.getTitleTextColor()));
            } catch (Exception e) {
                gc.setFill(Color.GOLD);
            }

            try {
                gc.setFont(Font.font(
                        theme.getTitleFontFamily(),
                        FontWeight.BOLD,
                        11
                ));
            } catch (Exception e) {
                gc.setFont(Font.font("Arial", FontWeight.BOLD, 11));
            }

            gc.fillText("Title", 5, 15);
        }

        // Main text preview
        try {
            gc.setFill(Color.web(theme.getTextColor()));
        } catch (Exception e) {
            gc.setFill(Color.WHITE);
        }

        try {
            gc.setFont(Font.font(
                    theme.getFontFamily(),
                    FontWeight.NORMAL,
                    10
            ));
        } catch (Exception e) {
            gc.setFont(Font.font("Arial", FontWeight.NORMAL, 10));
        }

        gc.fillText("Amazing", 5, 35);
        gc.fillText("Grace", 5, 48);

        // Alignment indicator
        String align = theme.getTextAlignment();

        gc.setFill(Color.LIGHTGRAY);
        gc.setFont(Font.font("Arial", 8));

        if ("CENTER".equalsIgnoreCase(align)) {
            gc.fillText("Center", width - 35, height - 5);
        } else if ("RIGHT".equalsIgnoreCase(align)) {
            gc.fillText("Right", width - 30, height - 5);
        } else {
            gc.fillText("Left", width - 25, height - 5);
        }

        WritableImage image = new WritableImage(width, height);
        canvas.snapshot(null, image);

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
                protected void updateItem(SubItemDisplayItem displayItem, boolean empty) {
                    super.updateItem(displayItem, empty);
                    if (empty || displayItem == null) {
                        setText(null);
                    } else {
                        // For PPT items, only show the label (without path preview)
                        if (item instanceof PptItem) {
                            setText(displayItem.label);
                        } else {
                            // For other items, add null check for content before calling length()
                            String preview = (displayItem.content != null && displayItem.content.length() > 30) ?
                                    displayItem.content.substring(0, 30) + "..." : (displayItem.content != null ? displayItem.content : "");
                            setText(displayItem.label + ": " + preview);
                        }
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
            dbService.savePrayer(result);           // â† Save to DB
            loadPrayers();                          // Refresh list
            AppLogger.log("Prayer saved: " + result.getTitle());
        });
    }

    private void loadPrayers() {
        allPrayers.setAll(dbService.loadAllPrayers());
        prayerList.refresh(); // Explicitly refresh the ListView
    }

    /**
     * Updates the window title to show the currently loaded service file name.
     */
    private void updateWindowTitle() {
        if (scene != null && scene.getWindow() instanceof javafx.stage.Stage) {
            javafx.stage.Stage stage = (javafx.stage.Stage) scene.getWindow();
            if (currentServiceFile != null) {
                stage.setTitle("PraiseView - " + currentServiceFile.getName());
            } else {
                stage.setTitle("PraiseView");
            }
        }
    }

    @FXML
    private void showAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About PraiseView");
        alert.setHeaderText("PraiseView " + com.praiseview.util.VersionUtil.getVersion());
        
        String content = """
                Modern JavaFX alternative to OpenLP for church projection.
                
                A free and open-source worship projection software built for churches and worship services.
                
                âœ¨ Current Features:
                â€¢ Multi-monitor full-screen projection
                â€¢ Song, Prayer & Announcement management
                â€¢ Service planner
                â€¢ Custom themes (colors, fonts, backgrounds, logos)
                â€¢ Media support (Images, Videos, PPT, Background videos)
                â€¢ Live preview + navigation controls
                â€¢ Mobile App Companion (remote control)
                
                ðŸ›£ï¸ Future Plans / Roadmap:
                â€¢ AI Helper for automatic slide advancement
                â€¢ Improved PowerPoint integration (thumbnails + live control)
                â€¢ Text input for Specific Languages
                
                Made by - Jason Fernandes
                For any issues to be raised, whatsapp at +919969965966
                """;
    
        alert.setContentText(content);
        alert.getDialogPane().setMinWidth(520);
        alert.getDialogPane().setMinHeight(440);
     
        ButtonType githubButton = new ButtonType("Visit GitHub");
     
        alert.showAndWait().ifPresent(response -> {
            if (response == githubButton) {
                PraiseViewApp.getStaticHostServices().showDocument("https://github.com/jasonfernandes420/PraiseView");
            }
        });
    }
     
    @FXML
    private void showDocumentation() {
        try {
            HelpDialog helpDialog = new HelpDialog();
            helpDialog.show();
        } catch (Exception e) {
            AppLogger.log("Failed to open help documentation: " + e.getMessage());
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to Load Documentation");
            alert.setContentText("Could not load the documentation: " + e.getMessage());
            alert.show();
        }
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

    @FXML
    private void handleConnectPhone() {
        try {
            PhoneRemoteServer remoteServer = PhoneRemoteServer.start(this);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/praiseview/view/connect-phone-view.fxml"));
            VBox root = loader.load();
            ConnectPhoneController controller = loader.getController();

            String ipAddress = PhoneRemoteServer.findLocalIpAddress();
            int port = remoteServer.getPort();

            controller.setConnectionDetails(ipAddress, port);

            Stage stage = new Stage();
            stage.setTitle("Connect Phone via QR Code");
            stage.setScene(new javafx.scene.Scene(root));
            stage.initOwner(scene.getWindow());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setOnHidden(event -> controller.dispose());
            stage.showAndWait();

        } catch (IOException e) {
            AppLogger.log("Error opening Connect Phone dialog: " + e.getMessage());
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Could not open Connect Phone dialog");
            alert.setContentText("An error occurred while loading the connection dialog: " + e.getMessage());
            alert.showAndWait();
        }
    }

    private void debugBackgroundImage() {
        System.out.println("=== DEBUG BACKGROUND ===");
        System.out.println("currentActiveTheme: " + (currentActiveTheme != null ? currentActiveTheme.getName() : "null"));
        if (currentActiveTheme != null) {
            System.out.println("Background Image Path: " + currentActiveTheme.getBackgroundImagePath());
        }
        System.out.println("liveThemeBackgroundImageView visible: " + liveThemeBackgroundImageView.isVisible());
        System.out.println("liveThemeBackgroundImageView image: " + (liveThemeBackgroundImageView.getImage() != null));
        System.out.println("livePreviewPane width/height: " + livePreviewPane.getWidth() + " x " + livePreviewPane.getHeight());
        System.out.println("=========================");
    }

}
