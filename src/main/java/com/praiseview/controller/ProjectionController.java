package com.praiseview.controller;

import com.praiseview.model.*;
import com.praiseview.util.AppLogger;
import com.praiseview.util.TextPaginationUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;

import java.io.File;
import java.util.List;

public class ProjectionController {

    @FXML private StackPane projectionRoot;
    @FXML private Label titleLabel;
    @FXML public TextFlow lyricsFlow; // Made public for MainController to access dimensions

    // FXML elements for content items
    @FXML private VBox textContentContainer;
    @FXML private ImageView itemImageView; // Renamed from imageView to match FXML fx:id
    @FXML private MediaView itemMediaView; // Renamed from mediaView to match FXML fx:id
    @FXML private VBox pptPlaceholderContainer;
    @FXML private Text pptPlaceholderText;
    @FXML private ImageView logoImageView;

    // New FXML elements for theme backgrounds
    @FXML private ImageView themeBackgroundImageView;
    @FXML private MediaView themeBackgroundMediaView;

    private MediaPlayer itemMediaPlayer; // For video playback of content items
    private MediaPlayer themeBackgroundMediaPlayer; // For video playback of theme backgrounds

    public double currentFontSize = 62.0; // Made public for MainController to access

    private Projectable currentProjectedItem; // Stores the currently projected item
    private int currentSubItemIndex = 0; // Stores the current verse/page index
    private List<String> currentProjectedItemPages; // Cached paginated content for the current item
    private double lastPaginationFontSize = -1; // Track font size used for last pagination
    private double lastPaginationWidth = -1;    // Track width used for last pagination
    private double lastPaginationHeight = -1;   // Track height used for last pagination

    private Theme activeTheme; // Store the currently active theme to check showTitle property


    @FXML
    public void initialize() {
        AppLogger.log("ProjectionController initialized");
        // Ensure media views are initially hidden
        _clearAllContentAndMedia(); // Clear everything first
        showLogo(); // Show logo on initialization
    }

    /**
     * Hides all content containers, stops media players, and clears text/image views.
     * This is a low-level clear, not intended to show the logo or set default background.
     */
    private void _clearAllContentAndMedia() {
        // Content-related views
        textContentContainer.setVisible(false);
        textContentContainer.setManaged(false);
        lyricsFlow.getChildren().clear(); // Explicitly clear TextFlow content
        itemImageView.setVisible(false); // Changed from imageView
        itemImageView.setManaged(false); // Changed from imageView
        itemImageView.setImage(null); // Explicitly clear image
        itemMediaView.setVisible(false); // Changed from mediaView
        itemMediaView.setManaged(false); // Changed from mediaView
        pptPlaceholderContainer.setVisible(false); // Hide PPT placeholder
        pptPlaceholderContainer.setManaged(false);
        if (itemMediaPlayer != null) {
            itemMediaPlayer.stop();
            itemMediaPlayer.dispose();
            itemMediaPlayer = null;
            AppLogger.log("ProjectionController: Stopped and disposed item media player.");
        }
        if (logoImageView != null) {
            logoImageView.setVisible(false);
            logoImageView.setManaged(false);
        }
        titleLabel.setText(""); // Clear title
        titleLabel.setVisible(false); // Ensure title is hidden
        titleLabel.setManaged(false); // Ensure title is not taking up space

        // Theme background views
        themeBackgroundImageView.setVisible(false);
        themeBackgroundImageView.setManaged(false);
        themeBackgroundImageView.setImage(null);
        themeBackgroundMediaView.setVisible(false);
        themeBackgroundMediaView.setManaged(false);
        if (themeBackgroundMediaPlayer != null) {
            themeBackgroundMediaPlayer.stop();
            themeBackgroundMediaPlayer.dispose();
            themeBackgroundMediaPlayer = null;
            AppLogger.log("ProjectionController: Stopped and disposed theme background media player.");
        }

        currentProjectedItem = null; // Clear current item state
        currentSubItemIndex = 0;
        currentProjectedItemPages = null; // Clear cached pages
        lastPaginationFontSize = -1; // Reset pagination state
        lastPaginationWidth = -1;
        lastPaginationHeight = -1;
    }

    /**
     * Main method to display any Projectable item on the projection screen.
     *
     * @param item The Projectable item to display (Song, Prayer, Announcement, etc.).
     * @param subItemIndex The 0-based index of the sub-item (e.g., verse for a song, page for a prayer).
     */
    public void showItem(Projectable item, int subItemIndex) {
        AppLogger.log("ProjectionController: showItem called for item type: " + item.getType() + ", subItemIndex: " + subItemIndex);
        if (item == null) {
            clear();
            AppLogger.log("ProjectionController: showItem called with null item.");
            return;
        }

        // Clear only content-specific views, theme background should persist
        textContentContainer.setVisible(false);
        textContentContainer.setManaged(false);
        lyricsFlow.getChildren().clear();
        itemImageView.setVisible(false); // Changed from imageView
        itemImageView.setManaged(false); // Changed from imageView
        itemImageView.setImage(null); // Changed from imageView
        itemMediaView.setVisible(false); // Changed from mediaView
        itemMediaView.setManaged(false); // Changed from mediaView
        pptPlaceholderContainer.setVisible(false);
        pptPlaceholderContainer.setManaged(false);
        if (itemMediaPlayer != null) {
            itemMediaPlayer.stop();
            itemMediaPlayer.dispose();
            itemMediaPlayer = null;
        }
        if (logoImageView != null) {
            logoImageView.setVisible(false);
            logoImageView.setManaged(false);
        }
        titleLabel.setText("");
        titleLabel.setVisible(false);
        titleLabel.setManaged(false);


        this.currentProjectedItem = item;
        this.currentSubItemIndex = subItemIndex;

        // Get actual dimensions of the lyricsFlow for accurate pagination
        // Fallback to reasonable defaults if not yet laid out
        double availableWidth = lyricsFlow.getWidth() > 0 ? lyricsFlow.getWidth() : 1000; // Default if not laid out
        double availableHeight = lyricsFlow.getHeight() > 0 ? lyricsFlow.getHeight() : 700; // Default if not laid out

        String titleText = item.getTitle();
        String contentToDisplay = "";
        int totalSubItems = 0;

        // Declare imageFile and slideImageFile here to ensure definite assignment
        File imageFile = null;
        File slideImageFile = null;

        // Handle different Projectable types
        switch (item.getType()) {
            case "SONG":
            case "PRAYER":
            case "ANNOUNCEMENT":
                AppLogger.log("ProjectionController: Displaying text content.");
                textContentContainer.setVisible(true);
                textContentContainer.setManaged(true);

                // --- FORCE RECALCULATION FOR TEXT-BASED ITEMS ---
                AppLogger.log("ProjectionController: Forcing re-pagination for text content.");
                if (item instanceof Song) {
                    currentProjectedItemPages = new java.util.ArrayList<>();
                    for (int i = 0; i < item.getSubItemCount(currentFontSize, availableWidth, availableHeight); i++) {
                        currentProjectedItemPages.add(item.getSubItemContent(i, currentFontSize, availableWidth, availableHeight));
                    }
                } else { // For dynamic content (Prayer, Announcement)
                    currentProjectedItemPages = TextPaginationUtil.paginateText(item.getFullContent(), currentFontSize, availableWidth, availableHeight);
                }
                this.lastPaginationFontSize = currentFontSize;
                this.lastPaginationWidth = availableWidth;
                this.lastPaginationHeight = availableHeight;
                // --- END FORCE RECALCULATION ---

                AppLogger.log("ProjectionController: currentProjectedItemPages after update (first page): " + (currentProjectedItemPages != null && !currentProjectedItemPages.isEmpty() ? (currentProjectedItemPages.get(0).length() > 50 ? currentProjectedItemPages.get(0).substring(0, 50) + "..." : currentProjectedItemPages.get(0)) : "EMPTY"));


                // Ensure subItemIndex is within bounds of the newly calculated pages
                if (currentProjectedItemPages != null && subItemIndex >= currentProjectedItemPages.size()) {
                    this.currentSubItemIndex = currentProjectedItemPages.size() - 1;
                    if (this.currentSubItemIndex < 0) this.currentSubItemIndex = 0; // Fallback for empty content
                }

                if (currentProjectedItemPages != null && !currentProjectedItemPages.isEmpty()) {
                    contentToDisplay = currentProjectedItemPages.get(this.currentSubItemIndex);
                }
                totalSubItems = currentProjectedItemPages != null ? currentProjectedItemPages.size() : 0;

                // Only show (X/Y) if there's more than one sub-item/page AND it's not a Song
                if (totalSubItems > 1 && !(item instanceof Song)) {
                    titleText += " (" + (this.currentSubItemIndex + 1) + "/" + totalSubItems + ")";
                }

                // Set title visibility based on activeTheme
                if (activeTheme != null && activeTheme.isShowTitle()) {
                    titleLabel.setText(titleText);
                    titleLabel.setVisible(true);
                    titleLabel.setManaged(true);
                    // Apply title-specific font settings
                    titleLabel.setStyle(String.format("-fx-font-family: '%s'; -fx-font-size: %.1fpx; -fx-text-fill: %s;",
                            activeTheme.getTitleFontFamily(), activeTheme.getTitleFontSize(), activeTheme.getTitleTextColor()));
                } else {
                    titleLabel.setText("");
                    titleLabel.setVisible(false);
                    titleLabel.setManaged(false);
                }

                lyricsFlow.getChildren().clear(); // Ensure cleared before adding new text
                Text mainText = new Text(contentToDisplay);
                // Set fill color using theme's text color
                try {
                    mainText.setFill(Color.web(activeTheme.getTextColor()));
                } catch (IllegalArgumentException | NullPointerException e) {
                    AppLogger.log("Invalid or null text color in active theme: '" + activeTheme.getTextColor() + "'. Falling back to WHITE. Error: " + e.getMessage());
                    mainText.setFill(Color.WHITE); // Fallback
                }
                mainText.setStyle("-fx-font-size: " + currentFontSize + "px; -fx-line-spacing: 8px;"); // Default, will be overridden by applyTheme
                lyricsFlow.getChildren().add(mainText);
                lyricsFlow.setTextAlignment(TextAlignment.CENTER); // Default, will be overridden by applyTheme
                break;

            case "IMAGE":
                AppLogger.log("ProjectionController: Displaying image.");
                itemImageView.setVisible(true); // Changed from imageView
                itemImageView.setManaged(true); // Changed from imageView
                imageFile = new File(((MediaItem)item).getFilePath()); // Assignment here
                AppLogger.log("ProjectionController: Image file path: " + imageFile.getAbsolutePath());
                if (imageFile.exists()) {
                    try {
                        Image image = new Image(imageFile.toURI().toString());
                        itemImageView.setImage(image); // Changed from imageView
                        itemImageView.setPreserveRatio(true); // Maintain aspect ratio // Changed from imageView
                        itemImageView.fitWidthProperty().bind(projectionRoot.widthProperty()); // Changed from imageView
                        itemImageView.fitHeightProperty().bind(projectionRoot.heightProperty()); // Changed from imageView
                        AppLogger.log("ProjectionController: Image loaded successfully.");
                    } catch (Exception e) {
                        AppLogger.log("ProjectionController: Error loading image: " + e.getMessage());
                        // Fallback to text error
                        textContentContainer.setVisible(true);
                        textContentContainer.setManaged(true);
                        titleLabel.setText("Error Loading Image");
                        lyricsFlow.getChildren().clear();
                        lyricsFlow.getChildren().add(new Text("File not found: " + imageFile.getName()));
                    }
                } else {
                    AppLogger.log("ProjectionController: Image file not found: " + imageFile.getAbsolutePath());
                    // Display error message on screen
                    textContentContainer.setVisible(true);
                    textContentContainer.setManaged(true);
                    titleLabel.setText("Error Loading Image");
                    lyricsFlow.getChildren().clear();
                    lyricsFlow.getChildren().add(new Text("File not found: " + imageFile.getName()));
                }
                // Set title visibility based on activeTheme
                if (activeTheme != null && activeTheme.isShowTitle()) {
                    titleLabel.setText(titleText);
                    titleLabel.setVisible(true);
                    titleLabel.setManaged(true);
                } else {
                    titleLabel.setText("");
                    titleLabel.setVisible(false);
                    titleLabel.setManaged(false);
                }
                break;

            case "VIDEO":
                AppLogger.log("ProjectionController: Displaying video.");
                itemMediaView.setVisible(true); // Changed from mediaView
                itemMediaView.setManaged(true); // Changed from mediaView
                File videoFile = new File(((MediaItem)item).getFilePath());
                AppLogger.log("ProjectionController: Video file path: " + videoFile.getAbsolutePath());
                if (videoFile.exists()) {
                    try {
                        Media media = new Media(videoFile.toURI().toString());
                        if (itemMediaPlayer != null) { // Use itemMediaPlayer
                            itemMediaPlayer.stop();
                            itemMediaPlayer.dispose();
                        }
                        itemMediaPlayer = new MediaPlayer(media); // Use itemMediaPlayer
                        itemMediaView.setMediaPlayer(itemMediaPlayer); // Changed from mediaView
                        itemMediaPlayer.setCycleCount(MediaPlayer.INDEFINITE); // Loop video
                        itemMediaPlayer.play();
                        itemMediaView.setPreserveRatio(true); // Changed from mediaView
                        itemMediaView.fitWidthProperty().bind(projectionRoot.widthProperty()); // Changed from mediaView
                        itemMediaView.fitHeightProperty().bind(projectionRoot.heightProperty()); // Changed from mediaView
                        AppLogger.log("ProjectionController: Video started successfully.");
                    } catch (Exception e) {
                        AppLogger.log("ProjectionController: Error loading video: " + e.getMessage());
                        // Fallback to text error
                        textContentContainer.setVisible(true);
                        textContentContainer.setManaged(true);
                        titleLabel.setText("Error Loading Video");
                        lyricsFlow.getChildren().clear();
                        lyricsFlow.getChildren().add(new Text("File not found: " + videoFile.getName()));
                    }
                } else {
                    AppLogger.log("ProjectionController: Video file not found: " + videoFile.getAbsolutePath());
                    // Display error message on screen
                    textContentContainer.setVisible(true);
                    textContentContainer.setManaged(true);
                    titleLabel.setText("Error Loading Video");
                    lyricsFlow.getChildren().clear();
                    lyricsFlow.getChildren().add(new Text("File not found: " + videoFile.getName()));
                }
                // Set title visibility based on activeTheme
                if (activeTheme != null && activeTheme.isShowTitle()) {
                    titleLabel.setText(titleText);
                    titleLabel.setVisible(true);
                    titleLabel.setManaged(true);
                } else {
                    titleLabel.setText("");
                    titleLabel.setVisible(false);
                    titleLabel.setManaged(false);
                }
                break;

            case "PPT": // Now handles PptItem by displaying rendered slide image
                AppLogger.log("ProjectionController: Displaying PPT slide.");
                itemImageView.setVisible(true); // Changed from imageView
                itemImageView.setManaged(true); // Changed from imageView
                PptItem pptItem = (PptItem) item;
                if (pptItem.getRenderedSlideImagePaths() != null && !pptItem.getRenderedSlideImagePaths().isEmpty()) {
                    String slideImagePath = pptItem.getSubItemContent(subItemIndex, currentFontSize, availableWidth, availableHeight);
                    slideImageFile = new File(slideImagePath); // Assignment here
                    AppLogger.log("ProjectionController: PPT slide image path: " + slideImageFile.getAbsolutePath());

                    if (slideImageFile.exists()) {
                        try {
                            Image slideImage = new Image(slideImageFile.toURI().toString());
                            itemImageView.setImage(slideImage); // Changed from imageView
                            itemImageView.setPreserveRatio(true); // Changed from imageView
                            itemImageView.fitWidthProperty().bind(projectionRoot.widthProperty()); // Changed from imageView
                            itemImageView.fitHeightProperty().bind(projectionRoot.heightProperty()); // Changed from imageView
                            // Update title with slide number
                            titleText += " (" + (subItemIndex + 1) + "/" + pptItem.getSubItemCount(currentFontSize, availableWidth, availableHeight) + ")";
                            AppLogger.log("ProjectionController: PPT slide image loaded successfully.");
                        } catch (Exception e) {
                            AppLogger.log("ProjectionController: Error loading PPT slide image: " + e.getMessage());
                            // Fallback to text error
                            textContentContainer.setVisible(true);
                            textContentContainer.setManaged(true);
                            titleLabel.setText("Error Loading PPT Slide");
                            lyricsFlow.getChildren().clear();
                            lyricsFlow.getChildren().add(new Text("Failed to load slide " + (subItemIndex + 1) + ": " + slideImageFile.getName()));
                        }
                    } else {
                        AppLogger.log("ProjectionController: PPT slide image file not found: " + slideImageFile.getAbsolutePath());
                        // Fallback to text error
                        textContentContainer.setVisible(true);
                        textContentContainer.setManaged(true);
                        titleLabel.setText("Error Loading PPT Slide");
                        lyricsFlow.getChildren().clear();
                        lyricsFlow.getChildren().add(new Text("Slide image not found: " + slideImageFile.getName()));
                    }
                } else {
                    AppLogger.log("ProjectionController: No rendered slides found for PPT: " + pptItem.getTitle());
                    // If no rendered slides, show a general error
                    textContentContainer.setVisible(true);
                    textContentContainer.setManaged(true);
                    titleLabel.setText("Error Loading PPT");
                    lyricsFlow.getChildren().clear();
                    lyricsFlow.getChildren().add(new Text("No slides rendered for: " + pptItem.getTitle()));
                }
                // Set title visibility based on activeTheme
                if (activeTheme != null && activeTheme.isShowTitle()) {
                    titleLabel.setText(titleText);
                    titleLabel.setVisible(true);
                    titleLabel.setManaged(true);
                } else {
                    titleLabel.setText("");
                    titleLabel.setVisible(false);
                    titleLabel.setManaged(false);
                }
                break;

            default:
                AppLogger.log("ProjectionController: Unsupported item type: " + item.getType());
                textContentContainer.setVisible(true);
                textContentContainer.setManaged(true);
                titleLabel.setText("Unsupported Item Type");
                lyricsFlow.getChildren().clear();
                lyricsFlow.getChildren().add(new Text("Cannot display: " + item.getType()));
                // Hide title for unsupported types
                titleLabel.setVisible(false);
                titleLabel.setManaged(false);
                break;
        }
    }

    /**
     * Toggles play/pause for the currently playing video.
     */
    public void playPauseVideo() {
        if (itemMediaPlayer != null) { // Use itemMediaPlayer
            if (itemMediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                itemMediaPlayer.pause();
                AppLogger.log("ProjectionController: Item video paused.");
            } else {
                itemMediaPlayer.play();
                AppLogger.log("ProjectionController: Item video played.");
            }
        } else {
            AppLogger.log("ProjectionController: No item media player available to play/pause.");
        }
    }

    /**
     * Seeks the currently playing video by a given number of seconds.
     * @param seconds The number of seconds to seek. Positive for forward, negative for backward.
     */
    public void seekVideo(double seconds) {
        if (itemMediaPlayer != null && itemMediaPlayer.getStatus() != MediaPlayer.Status.STOPPED) { // Use itemMediaPlayer
            Duration currentTime = itemMediaPlayer.getCurrentTime();
            Duration newTime = currentTime.add(Duration.seconds(seconds));
            itemMediaPlayer.seek(newTime);
            AppLogger.log("ProjectionController: Item video seeked by " + seconds + " seconds to " + newTime);
        } else {
            AppLogger.log("ProjectionController: No item media player available or video stopped for seeking.");
        }
    }

    /**
     * Applies the given theme to the projection screen.
     * @param theme The theme to apply.
     */
    public void applyTheme(Theme theme) {
        if (theme == null) {
            AppLogger.log("ProjectionController: Attempted to apply a null theme.");
            return;
        }
        this.activeTheme = theme; // Store the active theme
        AppLogger.log("ProjectionController: Applying theme: " + theme.getName());

        // Apply font, size, color, line spacing, and alignment to lyricsFlow
        lyricsFlow.setStyle(String.format("-fx-font-family: '%s'; -fx-font-size: %.1fpx; -fx-fill: %s; -fx-line-spacing: %.1fpx;",
                theme.getFontFamily(), theme.getFontSize(), theme.getTextColor(), theme.getLineSpacing()));

        // Apply font, size, and color to titleLabel
        titleLabel.setStyle(String.format("-fx-font-family: '%s'; -fx-font-size: %.1fpx; -fx-text-fill: %s;",
                theme.getTitleFontFamily(), theme.getTitleFontSize(), theme.getTitleTextColor())); // Use title-specific properties

        // Set title visibility based on the theme's showTitle property
        titleLabel.setVisible(theme.isShowTitle());
        titleLabel.setManaged(theme.isShowTitle());

        // Apply text alignment
        switch (theme.getTextAlignment().toUpperCase()) {
            case "LEFT":
                lyricsFlow.setTextAlignment(TextAlignment.LEFT);
                break;
            case "RIGHT":
                lyricsFlow.setTextAlignment(TextAlignment.RIGHT);
                break;
            case "CENTER":
            default:
                lyricsFlow.setTextAlignment(TextAlignment.CENTER);
                break;
        }

        // Handle background: color, image, or video
        // First, clear any existing theme background media
        if (themeBackgroundMediaPlayer != null) {
            themeBackgroundMediaPlayer.stop();
            themeBackgroundMediaPlayer.dispose();
            themeBackgroundMediaPlayer = null;
        }
        themeBackgroundImageView.setVisible(false);
        themeBackgroundImageView.setManaged(false);
        themeBackgroundImageView.setImage(null);
        themeBackgroundMediaView.setVisible(false);
        themeBackgroundMediaView.setManaged(false);
        themeBackgroundMediaView.setMediaPlayer(null);

        // Hide logo if a specific background is set
        if (logoImageView != null) {
            logoImageView.setVisible(false);
            logoImageView.setManaged(false);
        }

        if (theme.getBackgroundImagePath() != null && !theme.getBackgroundImagePath().isEmpty()) {
            File imageFile = new File(theme.getBackgroundImagePath());
            if (imageFile.exists()) {
                try {
                    Image image = new Image(imageFile.toURI().toString());
                    themeBackgroundImageView.setImage(image);
                    themeBackgroundImageView.setPreserveRatio(true);
                    themeBackgroundImageView.fitWidthProperty().bind(projectionRoot.widthProperty());
                    themeBackgroundImageView.fitHeightProperty().bind(projectionRoot.widthProperty()); // Should be heightProperty()
                    themeBackgroundImageView.setVisible(true);
                    themeBackgroundImageView.setManaged(true);
                    projectionRoot.setStyle(""); // Clear background color if image is present
                    AppLogger.log("ProjectionController: Applied background image: " + theme.getBackgroundImagePath());
                } catch (Exception e) {
                    AppLogger.log("ProjectionController: Error applying background image: " + e.getMessage());
                    projectionRoot.setStyle("-fx-background-color: " + theme.getBackgroundColor() + ";"); // Fallback to color
                }
            } else {
                AppLogger.log("ProjectionController: Background image file not found: " + theme.getBackgroundImagePath());
                projectionRoot.setStyle("-fx-background-color: " + theme.getBackgroundColor() + ";"); // Fallback to color
            }
        } else if (theme.getBackgroundVideoPath() != null && !theme.getBackgroundVideoPath().isEmpty()) {
            File videoFile = new File(theme.getBackgroundVideoPath());
            if (videoFile.exists()) {
                try {
                    Media media = new Media(videoFile.toURI().toString());
                    themeBackgroundMediaPlayer = new MediaPlayer(media);
                    themeBackgroundMediaView.setMediaPlayer(themeBackgroundMediaPlayer);
                    themeBackgroundMediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                    themeBackgroundMediaPlayer.setVolume(0.0); // Mute background video
                    themeBackgroundMediaPlayer.play();
                    themeBackgroundMediaView.setPreserveRatio(true);
                    themeBackgroundMediaView.fitWidthProperty().bind(projectionRoot.widthProperty());
                    themeBackgroundMediaView.fitHeightProperty().bind(projectionRoot.heightProperty());
                    themeBackgroundMediaView.setVisible(true);
                    themeBackgroundMediaView.setManaged(true);
                    projectionRoot.setStyle(""); // Clear background color if video is present
                    AppLogger.log("ProjectionController: Applied background video: " + theme.getBackgroundVideoPath());
                } catch (Exception e) {
                    AppLogger.log("ProjectionController: Error applying background video: " + e.getMessage());
                    projectionRoot.setStyle("-fx-background-color: " + theme.getBackgroundColor() + ";"); // Fallback to color
                }
            } else {
                AppLogger.log("ProjectionController: Background video file not found: " + theme.getBackgroundVideoPath());
                projectionRoot.setStyle("-fx-background-color: " + theme.getBackgroundColor() + ";"); // Fallback to color
            }
        } else {
            projectionRoot.setStyle("-fx-background-color: " + theme.getBackgroundColor() + ";"); // Apply background color
        }

        // Update currentFontSize for pagination calculations
        this.currentFontSize = theme.getFontSize();

        // Re-render the current projected item to ensure new styles are applied
        if (currentProjectedItem != null) {
            showItem(currentProjectedItem, currentSubItemIndex);
        }
    }


    /**
     * Returns the total number of sub-items (pages/verses) for the currently projected item.
     * This is used by MainController for navigation logic.
     * @return The total count of sub-items.
     */
    public int getCurrentProjectedItemSubItemCount() {
        if (currentProjectedItem != null) {
            // Use arbitrary reasonable dimensions for sub-item count calculation
            // The actual content for projection will use projection-specific dimensions.
            double calcWidth = 1000.0; // Matches default in showItem
            double calcHeight = 700.0; // Matches default in showItem
            double calcFontSize = currentFontSize; // Use current projection font size

            return currentProjectedItem.getSubItemCount(calcFontSize, calcWidth, calcHeight);
        }
        return 0;
    }

    // New getters for MainController to mirror the projection
    public String getCurrentDisplayedContent() {
        // This method now needs to return the content appropriate for the current item type.
        // For images/videos/PPT slides, it should return the path to the image, or a descriptive text.
        if (currentProjectedItem == null) {
            AppLogger.log("ProjectionController.getCurrentDisplayedContent: currentProjectedItem is null.");
            return "";
        }

        String content = "";
        switch (currentProjectedItem.getType()) {
            case "SONG":
            case "PRAYER":
            case "ANNOUNCEMENT":
                // Return content from the cached pages
                if (currentProjectedItemPages != null && currentSubItemIndex >= 0 && currentSubItemIndex < currentProjectedItemPages.size()) {
                    content = currentProjectedItemPages.get(currentSubItemIndex);
                } else {
                    AppLogger.log("ProjectionController.getCurrentDisplayedContent: currentProjectedItemPages is null or index out of bounds for text type.");
                }
                break;
            case "IMAGE":
            case "VIDEO":
                // Return file path for image/video
                if (currentProjectedItem instanceof MediaItem) {
                    content = ((MediaItem) currentProjectedItem).getFilePath();
                } else {
                    AppLogger.log("ProjectionController.getCurrentDisplayedContent: currentProjectedItem is not MediaItem for media type.");
                }
                break;
            case "PPT":
                // Return the path to the current slide image
                if (currentProjectedItem instanceof PptItem) {
                    PptItem ppt = (PptItem) currentProjectedItem;
                    if (ppt.getRenderedSlideImagePaths() != null && currentSubItemIndex >= 0 && currentSubItemIndex < ppt.getRenderedSlideImagePaths().size()) {
                        content = ppt.getRenderedSlideImagePaths().get(currentSubItemIndex);
                    } else {
                        AppLogger.log("ProjectionController.getCurrentDisplayedContent: PPT renderedSlideImagePaths is null or index out of bounds.");
                    }
                } else {
                    AppLogger.log("ProjectionController.getCurrentDisplayedContent: currentProjectedItem is not PptItem for PPT type.");
                }
                break;
            default:
                AppLogger.log("ProjectionController.getCurrentDisplayedContent: Unhandled item type: " + currentProjectedItem.getType());
                break;
        }
        AppLogger.log("ProjectionController.getCurrentDisplayedContent: Returning content (first 50 chars): " + (content.length() > 50 ? content.substring(0, 50) + "..." : content));
        return content;
    }


    public String getCurrentDisplayedTitle() {
        // Only return title text if it's currently visible
        return (titleLabel != null && titleLabel.isVisible()) ? titleLabel.getText() : "";
    }

    public Projectable getCurrentProjectedItem() {
        return currentProjectedItem;
    }

    public int getCurrentSubItemIndex() {
        return currentSubItemIndex;
    }

    public void blackout() {
        _clearAllContentAndMedia(); // Clear all content and media, including logo
        projectionRoot.setStyle("-fx-background-color: black;"); // Set background to black
        AppLogger.log("ProjectionController: Screen blacked out.");
    }

    public void clear() {
        _clearAllContentAndMedia(); // Clear all content and media
        projectionRoot.setStyle("-fx-background-color: #0f0f0f;"); // Reset to default background
        showLogo(); // Show logo after clearing
        AppLogger.log("ProjectionController: Screen cleared to logo.");
    }

    public void showLogo() {
        // Clear content-specific views, but keep theme background
        textContentContainer.setVisible(false);
        textContentContainer.setManaged(false);
        lyricsFlow.getChildren().clear();
        itemImageView.setVisible(false); // Changed from imageView
        itemImageView.setManaged(false); // Changed from imageView
        itemImageView.setImage(null); // Changed from imageView
        itemMediaView.setVisible(false); // Changed from mediaView
        itemMediaView.setManaged(false); // Changed from mediaView
        pptPlaceholderContainer.setVisible(false);
        pptPlaceholderContainer.setManaged(false);
        if (itemMediaPlayer != null) {
            itemMediaPlayer.stop();
            itemMediaPlayer.dispose();
            itemMediaPlayer = null;
        }

        projectionRoot.setStyle("-fx-background-color: #0f0f0f;"); // Ensure default background
        if (logoImageView != null) {
            logoImageView.setVisible(true);
            logoImageView.setManaged(true);
            AppLogger.log("ProjectionController: Displaying logo.");
        }
        titleLabel.setText(""); // Clear title when showing logo
        titleLabel.setVisible(false); // Explicitly hide title when showing logo
        titleLabel.setManaged(false);

        // Re-apply theme background if active
        if (activeTheme != null) {
            applyThemeBackgroundToProjection(activeTheme);
        }
    }

    /**
     * Helper method to apply theme background to the projection root.
     * Separated to be reusable for showLogo.
     */
    private void applyThemeBackgroundToProjection(Theme theme) {
        // Stop and hide any existing theme background media
        if (themeBackgroundMediaPlayer != null) {
            themeBackgroundMediaPlayer.stop();
            themeBackgroundMediaPlayer.dispose();
            themeBackgroundMediaPlayer = null;
        }
        themeBackgroundImageView.setVisible(false);
        themeBackgroundImageView.setManaged(false);
        themeBackgroundImageView.setImage(null);
        themeBackgroundMediaView.setVisible(false);
        themeBackgroundMediaView.setManaged(false);
        themeBackgroundMediaView.setMediaPlayer(null);

        // Apply new background based on theme
        if (theme.getBackgroundImagePath() != null && !theme.getBackgroundImagePath().isEmpty()) {
            File imageFile = new File(theme.getBackgroundImagePath());
            if (imageFile.exists()) {
                try {
                    Image image = new Image(imageFile.toURI().toString());
                    themeBackgroundImageView.setImage(image);
                    themeBackgroundImageView.setPreserveRatio(true);
                    themeBackgroundImageView.fitWidthProperty().bind(projectionRoot.widthProperty());
                    themeBackgroundImageView.fitHeightProperty().bind(projectionRoot.heightProperty()); // Corrected from widthProperty()
                    themeBackgroundImageView.setVisible(true);
                    themeBackgroundImageView.setManaged(true);
                    projectionRoot.setStyle(""); // Clear background color if image is present
                    AppLogger.log("ProjectionController: Applied background image: " + theme.getBackgroundImagePath());
                } catch (Exception e) {
                    AppLogger.log("ProjectionController: Error applying background image: " + e.getMessage());
                    projectionRoot.setStyle("-fx-background-color: " + theme.getBackgroundColor() + ";"); // Fallback to color
                }
            } else {
                AppLogger.log("ProjectionController: Background image file not found: " + theme.getBackgroundImagePath());
                projectionRoot.setStyle("-fx-background-color: " + theme.getBackgroundColor() + ";"); // Fallback to color
            }
        } else if (theme.getBackgroundVideoPath() != null && !theme.getBackgroundVideoPath().isEmpty()) {
            File videoFile = new File(theme.getBackgroundVideoPath());
            if (videoFile.exists()) {
                try {
                    Media media = new Media(videoFile.toURI().toString());
                    themeBackgroundMediaPlayer = new MediaPlayer(media);
                    themeBackgroundMediaView.setMediaPlayer(themeBackgroundMediaPlayer);
                    themeBackgroundMediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                    themeBackgroundMediaPlayer.setVolume(0.0); // Mute background video
                    themeBackgroundMediaPlayer.play();
                    themeBackgroundMediaView.setPreserveRatio(true);
                    themeBackgroundMediaView.fitWidthProperty().bind(projectionRoot.widthProperty());
                    themeBackgroundMediaView.fitHeightProperty().bind(projectionRoot.heightProperty());
                    themeBackgroundMediaView.setVisible(true);
                    themeBackgroundMediaView.setManaged(true);
                    projectionRoot.setStyle(""); // Clear background color if video is present
                    AppLogger.log("ProjectionController: Applied background video: " + theme.getBackgroundVideoPath());
                } catch (Exception e) {
                    AppLogger.log("ProjectionController: Error applying background video: " + e.getMessage());
                    projectionRoot.setStyle("-fx-background-color: " + theme.getBackgroundColor() + ";"); // Fallback to color
                }
            } else {
                AppLogger.log("ProjectionController: Background video file not found: " + theme.getBackgroundVideoPath());
                projectionRoot.setStyle("-fx-background-color: " + theme.getBackgroundColor() + ";"); // Fallback to color
            }
        } else {
            projectionRoot.setStyle("-fx-background-color: " + theme.getBackgroundColor() + ";"); // Apply background color
        }
    }


    public void setFontSize(double size) {
        currentFontSize = size;
        // Re-render the current item with the new font size, which will trigger re-pagination
        if (currentProjectedItem != null) {
            showItem(currentProjectedItem, currentSubItemIndex);
        }
    }

    // Expose paginateText for MainController's preview functionality
    public List<String> paginateText(String fullText, double fontSize, double maxWidth, double maxHeight) {
        return TextPaginationUtil.paginateText(fullText, fontSize, maxWidth, maxHeight);
    }
}