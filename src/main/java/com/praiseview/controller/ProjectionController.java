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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ProjectionController {

    @FXML private StackPane projectionRoot;
    @FXML private Label titleLabel;
    @FXML public TextFlow lyricsFlow; // Made public for MainController to access dimensions

    // FXML elements for content items
    @FXML private VBox textContentContainer;
    @FXML private ImageView itemImageView;
    @FXML private MediaView itemMediaView;
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

    private Theme activeTheme; // Store the currently active theme to check showTitle property

    // Fields to cache pagination parameters to prevent unnecessary re-pagination
    private Projectable lastPaginatedItem = null;
    private double lastPaginatedFontSize = -1;
    private double lastPaginatedWidth = -1;
    private double lastPaginatedHeight = -1;

    // Constants for text content area padding and title margin
    private static final double TEXT_HORIZONTAL_PADDING = 50.0; // Padding on left and right
    private static final double TEXT_VERTICAL_PADDING = 50.0;   // Padding on top and bottom
    private static final double TITLE_LYRICS_MARGIN = 20.0;     // Space between title and lyrics


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
        itemImageView.setVisible(false);
        itemImageView.setManaged(false);
        itemImageView.setImage(null); // Explicitly clear image
        itemMediaView.setVisible(false);
        itemMediaView.setManaged(false);
        pptPlaceholderContainer.setVisible(false); // Hide PPT placeholder
        pptPlaceholderContainer.setManaged(false);
        if (itemMediaPlayer != null) {
            itemMediaPlayer.stop();
            itemMediaPlayer.dispose();
            itemMediaPlayer = null;
            AppLogger.log("ProjectionController: Stopped and disposed item media player.");
        }
        // Do NOT hide logo here, showLogo() will handle its visibility
        // if (logoImageView != null) {
        //     logoImageView.setVisible(false);
        //     logoImageView.setManaged(false);
        // }
        titleLabel.setText(""); // Clear title
        titleLabel.setVisible(false); // Ensure title is hidden
        titleLabel.setManaged(false); // Ensure title is not taking up space

        // Theme background views
        if (themeBackgroundMediaPlayer != null) {
            themeBackgroundMediaPlayer.stop();
            themeBackgroundMediaPlayer.dispose();
            themeBackgroundMediaPlayer = null;
            AppLogger.log("ProjectionController: Stopped and disposed theme background media player.");
        }
        themeBackgroundImageView.setVisible(false);
        themeBackgroundImageView.setManaged(false);
        themeBackgroundImageView.setImage(null);
        themeBackgroundMediaView.setVisible(false);
        themeBackgroundMediaView.setManaged(false);
        themeBackgroundMediaView.setMediaPlayer(null);


        currentProjectedItem = null; // Clear current item state
        currentSubItemIndex = 0;
        currentProjectedItemPages = null; // Clear cached pages

        // Invalidate pagination cache
        lastPaginatedItem = null;
        lastPaginatedFontSize = -1;
        lastPaginatedWidth = -1;
        lastPaginatedHeight = -1;
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

        // Defensive check for activeTheme
        if (activeTheme == null) {
            AppLogger.log("ProjectionController: activeTheme is null, initializing with default theme.");
            activeTheme = new Theme(); // Initialize with default theme
            applyTheme(activeTheme); // Apply to set initial styles
        }

        // --- Start: Clear and reset all content views and media players ---
        textContentContainer.setVisible(false);
        textContentContainer.setManaged(false);
        lyricsFlow.getChildren().clear();
        itemImageView.setVisible(false);
        itemImageView.setManaged(false);
        itemImageView.setImage(null);
        itemMediaView.setVisible(false);
        itemMediaView.setManaged(false);
        pptPlaceholderContainer.setVisible(false);
        pptPlaceholderContainer.setManaged(false);
        
        // Stop and dispose of any existing item media player to prevent audio overlap
        if (itemMediaPlayer != null) {
            itemMediaPlayer.stop();
            itemMediaPlayer.dispose();
            itemMediaPlayer = null;
            AppLogger.log("ProjectionController: Stopped and disposed item media player before showing new item.");
        }
        itemMediaView.setMediaPlayer(null); // Clear media player from view

        // Hide logo when content is being displayed
        if (logoImageView != null) {
            logoImageView.setVisible(false);
            logoImageView.setManaged(false);
        }
        titleLabel.setText("");
        titleLabel.setVisible(false);
        titleLabel.setManaged(false);
        // --- End: Clear and reset all content views and media players ---


        this.currentProjectedItem = item;
        this.currentSubItemIndex = subItemIndex;

        // Calculate available dimensions for text content based on projectionRoot
        double availableWidth = projectionRoot.getWidth() - (2 * TEXT_HORIZONTAL_PADDING);
        double availableHeight = projectionRoot.getHeight() - (2 * TEXT_VERTICAL_PADDING);

        // Ensure dimensions are positive
        if (availableWidth <= 0) availableWidth = 100; // Fallback
        if (availableHeight <= 0) availableHeight = 100; // Fallback

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

                // Set title visibility and calculate its height for pagination
                if (activeTheme != null && activeTheme.isShowTitle()) {
                    titleLabel.setText(titleText);
                    // Temporarily apply style to measure height accurately
                    titleLabel.setStyle(String.format("-fx-font-family: '%s'; -fx-font-size: %.1fpx; -fx-text-fill: %s;",
                            activeTheme.getTitleFontFamily(), activeTheme.getTitleFontSize(), activeTheme.getTitleTextColor()));
                    // Force layout pass to get accurate preferred height
                    titleLabel.applyCss();
                    titleLabel.layout();
                    double titleHeight = titleLabel.prefHeight(availableWidth); // Measure title height given available width
                    availableHeight -= (titleHeight + TITLE_LYRICS_MARGIN);
                    titleLabel.setVisible(true);
                    titleLabel.setManaged(true);
                } else {
                    titleLabel.setText("");
                    titleLabel.setVisible(false);
                    titleLabel.setManaged(false);
                }

                // Ensure availableHeight remains positive after title deduction
                if (availableHeight <= 0) availableHeight = 100; // Fallback

                // Check if re-pagination is needed for the current item and dimensions
                boolean needsRepagination = false;
                if (currentProjectedItemPages == null ||
                    !Objects.equals(item, lastPaginatedItem) || // Check if item itself changed
                    currentFontSize != lastPaginatedFontSize ||
                    availableWidth != lastPaginatedWidth ||
                    availableHeight != lastPaginatedHeight) {
                    needsRepagination = true;
                    AppLogger.log("ProjectionController: Re-paginating text content due to change in item, font size, or dimensions.");
                }

                if (needsRepagination) {
                    currentProjectedItemPages = new ArrayList<>();
                    // For Prayer, use the new stateless paginateForDimensions method
                    if (item instanceof Prayer) {
                        currentProjectedItemPages.addAll(((Prayer) item).paginateForDimensions(currentFontSize, availableWidth, availableHeight));
                    } else if (item instanceof Announcement) {
                        // For Announcement, rePaginate is still used as its implementation hasn't changed
                        ((Announcement) item).rePaginate(currentFontSize, availableWidth, availableHeight);
                        totalSubItems = item.getSubItemCount(currentFontSize, availableWidth, availableHeight);
                        for (int i = 0; i < totalSubItems; i++) {
                            currentProjectedItemPages.add(item.getSubItemContent(i, currentFontSize, availableWidth, availableHeight));
                        }
                    } else { // For Song and other text-based items
                        totalSubItems = item.getSubItemCount(currentFontSize, availableWidth, availableHeight);
                        for (int i = 0; i < totalSubItems; i++) {
                            currentProjectedItemPages.add(item.getSubItemContent(i, currentFontSize, availableWidth, availableHeight));
                        }
                    }

                    // Update cached pagination parameters
                    lastPaginatedItem = item;
                    lastPaginatedFontSize = currentFontSize;
                    lastPaginatedWidth = availableWidth;
                    lastPaginatedHeight = availableHeight;
                }
                
                totalSubItems = currentProjectedItemPages.size();

                // Ensure subItemIndex is within bounds of the newly calculated pages
                if (currentProjectedItemPages != null && subItemIndex >= currentProjectedItemPages.size()) {
                    this.currentSubItemIndex = currentProjectedItemPages.size() - 1;
                    if (this.currentSubItemIndex < 0) this.currentSubItemIndex = 0; // Fallback for empty content
                }

                if (currentProjectedItemPages != null && !currentProjectedItemPages.isEmpty()) {
                    contentToDisplay = currentProjectedItemPages.get(this.currentSubItemIndex);
                }
                
                // Removed X/Y indicator from title for text-based items
                // if (totalSubItems > 1 && !(item instanceof Song)) {
                //     titleText += " (" + (this.currentSubItemIndex + 1) + "/" + totalSubItems + ")";
                // }

                // Re-set title text after pagination count is known
                if (activeTheme != null && activeTheme.isShowTitle()) {
                    titleLabel.setText(titleText);
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
                // Apply font settings from activeTheme
                mainText.setStyle(String.format("-fx-font-family: '%s'; -fx-font-size: %.1fpx; -fx-line-spacing: %.1fpx;",
                        activeTheme.getFontFamily(), activeTheme.getFontSize(), activeTheme.getLineSpacing()));
                lyricsFlow.getChildren().add(mainText);
                lyricsFlow.setTextAlignment(TextAlignment.valueOf(activeTheme.getTextAlignment().toUpperCase()));
                break;

            case "IMAGE":
                AppLogger.log("ProjectionController: Displaying image.");
                itemImageView.setVisible(true);
                itemImageView.setManaged(true);
                imageFile = new File(((MediaItem)item).getFilePath());
                AppLogger.log("ProjectionController: Image file path: " + imageFile.getAbsolutePath());
                if (imageFile.exists()) {
                    try {
                        Image image = new Image(imageFile.toURI().toString());
                        itemImageView.setImage(image);
                        itemImageView.setPreserveRatio(true);
                        itemImageView.fitWidthProperty().bind(projectionRoot.widthProperty());
                        itemImageView.fitHeightProperty().bind(projectionRoot.heightProperty());
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
                itemMediaView.setVisible(true);
                itemMediaView.setManaged(true);
                File videoFile = new File(((MediaItem)item).getFilePath());
                AppLogger.log("ProjectionController: Video file path: " + videoFile.getAbsolutePath());
                if (videoFile.exists()) {
                    try {
                        Media media = new Media(videoFile.toURI().toString());
                        // No need to stop/dispose here, already done at the start of showItem
                        itemMediaPlayer = new MediaPlayer(media);
                        itemMediaView.setMediaPlayer(itemMediaPlayer);
                        itemMediaPlayer.setCycleCount(MediaPlayer.INDEFINITE); // Loop video
                        itemMediaPlayer.play(); // Explicitly play the video
                        itemMediaView.setPreserveRatio(true);
                        itemMediaView.fitWidthProperty().bind(projectionRoot.widthProperty());
                        itemMediaView.fitHeightProperty().bind(projectionRoot.heightProperty());
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
                itemImageView.setVisible(true);
                itemImageView.setManaged(true);
                PptItem pptItem = (PptItem) item;
                if (pptItem.getRenderedSlideImagePaths() != null && !pptItem.getRenderedSlideImagePaths().isEmpty()) {
                    // Use projectionRoot dimensions for PPT pagination as well
                    String slideImagePath = pptItem.getSubItemContent(subItemIndex, currentFontSize, projectionRoot.getWidth(), projectionRoot.getHeight());
                    slideImageFile = new File(slideImagePath);
                    AppLogger.log("ProjectionController: PPT slide image path: " + slideImageFile.getAbsolutePath());

                    if (slideImageFile.exists()) {
                        try {
                            Image slideImage = new Image(slideImageFile.toURI().toString());
                            itemImageView.setImage(slideImage);
                            itemImageView.setPreserveRatio(true);
                            itemImageView.fitWidthProperty().bind(projectionRoot.widthProperty());
                            itemImageView.fitHeightProperty().bind(projectionRoot.heightProperty());
                            // Update title with slide number
                            titleText += " (" + (subItemIndex + 1) + "/" + pptItem.getSubItemCount(currentFontSize, projectionRoot.getWidth(), projectionRoot.getHeight()) + ")";
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
                        lyricsFlow.getChildren().add(new Text("File not found: " + slideImageFile.getName()));
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
        if (itemMediaPlayer != null) {
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
        if (itemMediaPlayer != null && itemMediaPlayer.getStatus() != MediaPlayer.Status.STOPPED) {
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

        // The logo should not be hidden by background application.
        // Its visibility is managed by showLogo() and showItem().
        // if (logoImageView != null) {
        //     logoImageView.setVisible(false);
        //     logoImageView.setManaged(false);
        // }

        if (theme.getBackgroundImagePath() != null && !theme.getBackgroundImagePath().isEmpty()) {
            File imageFile = new File(theme.getBackgroundImagePath());
            if (imageFile.exists()) {
                try {
                    Image image = new Image(imageFile.toURI().toString());
                    themeBackgroundImageView.setImage(image);
                    themeBackgroundImageView.setPreserveRatio(true);
                    themeBackgroundImageView.fitWidthProperty().bind(projectionRoot.widthProperty());
                    themeBackgroundImageView.fitHeightProperty().bind(projectionRoot.heightProperty());
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

        // Invalidate pagination cache to force re-pagination with new theme settings
        lastPaginatedItem = null;
        lastPaginatedFontSize = -1;
        lastPaginatedWidth = -1;
        lastPaginatedHeight = -1;

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
        // Ensure pagination is up-to-date before returning count
        // This might trigger a re-pagination if dimensions changed since last showItem call
        if (currentProjectedItem != null && currentProjectedItemPages == null) {
            // Recalculate dimensions similar to showItem for consistency
            double availableWidth = projectionRoot.getWidth() - (2 * TEXT_HORIZONTAL_PADDING);
            double availableHeight = projectionRoot.getHeight() - (2 * TEXT_VERTICAL_PADDING);

            if (availableWidth <= 0) availableWidth = 100;
            if (availableHeight <= 0) availableHeight = 100;

            if (activeTheme != null && activeTheme.isShowTitle()) {
                // Temporarily set title text and style to measure height
                String tempTitleText = currentProjectedItem.getTitle();
                // Removed X/Y indicator from title for text-based items
                // if (currentProjectedItem.getSubItemCount(currentFontSize, availableWidth, availableHeight) > 1 && !(currentProjectedItem instanceof Song)) {
                //     tempTitleText += " (99/99)"; // Max length for (X/Y)
                // }
                titleLabel.setText(tempTitleText);
                titleLabel.setStyle(String.format("-fx-font-family: '%s'; -fx-font-size: %.1fpx; -fx-text-fill: %s;",
                        activeTheme.getTitleFontFamily(), activeTheme.getTitleFontSize(), activeTheme.getTitleTextColor()));
                titleLabel.applyCss();
                titleLabel.layout();
                double titleHeight = titleLabel.prefHeight(availableWidth);
                availableHeight -= (titleHeight + TITLE_LYRICS_MARGIN);
            }
            if (availableHeight <= 0) availableHeight = 100;

            // Trigger pagination for the current item with these dimensions
            currentProjectedItemPages = new ArrayList<>();
            if (currentProjectedItem instanceof Prayer) {
                currentProjectedItemPages.addAll(((Prayer) currentProjectedItem).paginateForDimensions(currentFontSize, availableWidth, availableHeight));
            } else if (currentProjectedItem instanceof Announcement) {
                ((Announcement) currentProjectedItem).rePaginate(currentFontSize, availableWidth, availableHeight);
                int total = currentProjectedItem.getSubItemCount(currentFontSize, availableWidth, availableHeight);
                for (int i = 0; i < total; i++) {
                    currentProjectedItemPages.add(currentProjectedItem.getSubItemContent(i, currentFontSize, availableWidth, availableHeight));
                }
            } else { // For Song and other text-based items
                int total = currentProjectedItem.getSubItemCount(currentFontSize, availableWidth, availableHeight);
                for (int i = 0; i < total; i++) {
                    currentProjectedItemPages.add(currentProjectedItem.getSubItemContent(i, currentFontSize, availableWidth, availableHeight));
                }
            }

            // Update cached pagination parameters
            lastPaginatedItem = currentProjectedItem;
            lastPaginatedFontSize = currentFontSize;
            lastPaginatedWidth = availableWidth;
            lastPaginatedHeight = availableHeight;
        }

        if (currentProjectedItemPages != null) {
            return currentProjectedItemPages.size();
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
                    // Use projectionRoot dimensions for PPT pagination as well
                    content = ppt.getSubItemContent(currentSubItemIndex, currentFontSize, projectionRoot.getWidth(), projectionRoot.getHeight());
                } else {
                    AppLogger.log("ProjectionController.getCurrentDisplayedContent: currentProjectedItem is not PptItem for PPT type.");
                }
                break;
            default:
                AppLogger.log("ProjectionController.getCurrentDisplayedContent: Unhandled item type: " + currentProjectedItem.getType());
                break;
        }
        AppLogger.log("ProjectionController.getCurrentDisplayedContent: Returning content: " +content);
        return content;
    }

    public List<String> getCurrentProjectedItemPages() {
        return currentProjectedItemPages;
    }

    public TextFlow getLyricsFlow() {
        return lyricsFlow;
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
        if (projectionRoot.getScene() != null) {
            projectionRoot.getScene().setFill(Color.BLACK); // Also set scene fill to black
        }
        currentProjectedItem = null; // Clear the current item state
        AppLogger.log("ProjectionController: Screen blacked out.");
    }

    public void clear() {
        _clearAllContentAndMedia(); // Clear all content and media
        projectionRoot.setStyle("-fx-background-color: #0f0f0f;"); // Reset to default background
        showLogo(); // Show logo after clearing
        AppLogger.log("ProjectionController: Screen cleared to logo.");
    }

    public void showLogo() {
        // Defensive check for activeTheme
        if (activeTheme == null) {
            AppLogger.log("ProjectionController: activeTheme is null in showLogo, initializing with default theme.");
            activeTheme = new Theme(); // Initialize with default theme
            // No need to call applyTheme here, applyThemeBackgroundToProjection will use it
        }

        // Clear content-specific views, but keep theme background
        textContentContainer.setVisible(false);
        textContentContainer.setManaged(false);
        lyricsFlow.getChildren().clear();
        itemImageView.setVisible(false);
        itemImageView.setManaged(false);
        itemImageView.setImage(null);
        itemMediaView.setVisible(false);
        itemMediaView.setManaged(false);
        pptPlaceholderContainer.setVisible(false);
        pptPlaceholderContainer.setManaged(false);
        if (itemMediaPlayer != null) {
            itemMediaPlayer.stop();
            itemMediaPlayer.dispose();
            itemMediaPlayer = null;
        }

        projectionRoot.setStyle("-fx-background-color: #0f0f0f;"); // Ensure default background
        if (logoImageView != null) {
            // Load default logo if not already set
            if (logoImageView.getImage() == null) {
                try {
                    // Assuming a default logo image exists in resources
                    Image defaultLogo = new Image(getClass().getResourceAsStream("/com/praiseview/images/default_logo.png"));
                    logoImageView.setImage(defaultLogo);
                    logoImageView.setPreserveRatio(true);
                    logoImageView.setFitWidth(200); // Set a reasonable size for the logo
                    logoImageView.setFitHeight(200);
                    AppLogger.log("ProjectionController: Loaded default logo image.");
                } catch (Exception e) {
                    AppLogger.log("ProjectionController: Error loading default logo: " + e.getMessage());
                }
            }
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

        // The logo should not be hidden by background application.
        // Its visibility is managed by showLogo() and showItem().
        // if (logoImageView != null) {
        //     logoImageView.setVisible(false);
        //     logoImageView.setManaged(false);
        // }

        if (theme.getBackgroundImagePath() != null && !theme.getBackgroundImagePath().isEmpty()) {
            File imageFile = new File(theme.getBackgroundImagePath());
            if (imageFile.exists()) {
                try {
                    Image image = new Image(imageFile.toURI().toString());
                    themeBackgroundImageView.setImage(image);
                    themeBackgroundImageView.setPreserveRatio(true);
                    themeBackgroundImageView.fitWidthProperty().bind(projectionRoot.widthProperty());
                    themeBackgroundImageView.fitHeightProperty().bind(projectionRoot.heightProperty());
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

        // Invalidate pagination cache to force re-pagination with new theme settings
        lastPaginatedItem = null;
        lastPaginatedFontSize = -1;
        lastPaginatedWidth = -1;
        lastPaginatedHeight = -1;

        // Re-render the current projected item to ensure new styles are applied
        if (currentProjectedItem != null) {
            showItem(currentProjectedItem, currentSubItemIndex);
        }
    }


    public void setFontSize(double size) {
        currentFontSize = size;
        // Invalidate pagination cache to force re-pagination with new font size
        lastPaginatedItem = null;
        lastPaginatedFontSize = -1;
        lastPaginatedWidth = -1;
        lastPaginatedHeight = -1;
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
