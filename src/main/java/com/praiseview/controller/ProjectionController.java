package com.praiseview.controller;

import com.praiseview.model.Announcement;
import com.praiseview.model.Prayer;
import com.praiseview.model.Projectable;
import com.praiseview.model.Song;
import com.praiseview.model.MediaItem;
import com.praiseview.model.PptItem; // Import PptItem
import com.praiseview.model.Theme; // Import Theme
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
import javafx.util.Duration; // Import Duration

import java.io.File;
import java.util.List;

public class ProjectionController {

    @FXML private StackPane projectionRoot;
    @FXML private Label titleLabel;
    @FXML public TextFlow lyricsFlow; // Made public for MainController to access dimensions

    // New FXML elements for media
    @FXML private VBox textContentContainer;
    @FXML private ImageView imageView;
    @FXML private MediaView mediaView;
    @FXML private VBox pptPlaceholderContainer; // This will now be used for error/loading messages for PPT
    @FXML private Text pptPlaceholderText; // This will now be used for error/loading messages for PPT

    private MediaPlayer mediaPlayer; // For video playback

    public double currentFontSize = 62.0; // Made public for MainController to access

    private Projectable currentProjectedItem; // Stores the currently projected item
    private int currentSubItemIndex = 0; // Stores the current verse/page index
    private List<String> currentProjectedItemPages; // Cached paginated content for the current item
    private double lastPaginationFontSize = -1; // Track font size used for last pagination
    private double lastPaginationWidth = -1;    // Track width used for last pagination
    private double lastPaginationHeight = -1;   // Track height used for last pagination


    @FXML
    public void initialize() {
        AppLogger.log("ProjectionController initialized");
        // Ensure media views are initially hidden
        hideAllMediaViews();
    }

    private void hideAllMediaViews() {
        textContentContainer.setVisible(false);
        textContentContainer.setManaged(false);
        lyricsFlow.getChildren().clear(); // Explicitly clear TextFlow content
        imageView.setVisible(false);
        imageView.setManaged(false);
        imageView.setImage(null); // Explicitly clear image
        mediaView.setVisible(false);
        mediaView.setManaged(false);
        pptPlaceholderContainer.setVisible(false); // Hide PPT placeholder
        pptPlaceholderContainer.setManaged(false);
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
            mediaPlayer = null;
            AppLogger.log("ProjectionController: Stopped and disposed media player.");
        }
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

        // Clear all previous content and hide all views
        hideAllMediaViews();

        this.currentProjectedItem = item;
        this.currentSubItemIndex = subItemIndex;

        // Reset styling
        projectionRoot.setStyle("-fx-background-color: #0f0f0f;"); // Default background

        // Get actual dimensions of the lyricsFlow for accurate pagination
        // Fallback to reasonable defaults if not yet laid out
        double availableWidth = lyricsFlow.getWidth() > 0 ? lyricsFlow.getWidth() : 1000; // Default if not laid out
        double availableHeight = lyricsFlow.getHeight() > 0 ? lyricsFlow.getHeight() : 700; // Default if not laid out

        String titleText = item.getTitle();
        String contentToDisplay = "";
        int totalSubItems = 0;

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
                titleLabel.setText(titleText);
                titleLabel.setStyle("-fx-text-fill: #ffd700; -fx-font-size: 42px;");

                lyricsFlow.getChildren().clear(); // Ensure cleared before adding new text
                Text mainText = new Text(contentToDisplay);
                mainText.setFill(Color.WHITE);
                mainText.setStyle("-fx-font-size: " + currentFontSize + "px; -fx-line-spacing: 8px;");
                lyricsFlow.getChildren().add(mainText);
                lyricsFlow.setTextAlignment(TextAlignment.CENTER);
                break;

            case "IMAGE":
                AppLogger.log("ProjectionController: Displaying image.");
                imageView.setVisible(true);
                imageView.setManaged(true);
                File imageFile = new File(((MediaItem)item).getFilePath());
                AppLogger.log("ProjectionController: Image file path: " + imageFile.getAbsolutePath());
                if (imageFile.exists()) {
                    try {
                        Image image = new Image(imageFile.toURI().toString());
                        imageView.setImage(image);
                        imageView.setPreserveRatio(true); // Maintain aspect ratio
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
                titleLabel.setText(titleText); // Set title for image
                break;

            case "VIDEO":
                AppLogger.log("ProjectionController: Displaying video.");
                mediaView.setVisible(true);
                mediaView.setManaged(true);
                File videoFile = new File(((MediaItem)item).getFilePath());
                AppLogger.log("ProjectionController: Video file path: " + videoFile.getAbsolutePath());
                if (videoFile.exists()) {
                    Media media = new Media(videoFile.toURI().toString());
                    if (mediaPlayer != null) {
                        mediaPlayer.stop();
                        mediaPlayer.dispose();
                    }
                    mediaPlayer = new MediaPlayer(media);
                    mediaView.setMediaPlayer(mediaPlayer);
                    mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE); // Loop video
                    mediaPlayer.play();
                    mediaView.setPreserveRatio(true);
                    AppLogger.log("ProjectionController: Video started successfully.");
                } else {
                    AppLogger.log("ProjectionController: Video file not found: " + videoFile.getAbsolutePath());
                    // Display error message on screen
                    textContentContainer.setVisible(true);
                    textContentContainer.setManaged(true);
                    titleLabel.setText("Error Loading Video");
                    lyricsFlow.getChildren().clear();
                    lyricsFlow.getChildren().add(new Text("File not found: " + videoFile.getName()));
                }
                titleLabel.setText(titleText); // Set title for video
                break;

            case "PPT": // Now handles PptItem by displaying rendered slide image
                AppLogger.log("ProjectionController: Displaying PPT slide.");
                imageView.setVisible(true); // Reuse imageView for PPT slides
                imageView.setManaged(true);
                PptItem pptItem = (PptItem) item;
                if (pptItem.getRenderedSlideImagePaths() != null && !pptItem.getRenderedSlideImagePaths().isEmpty()) {
                    String slideImagePath = pptItem.getSubItemContent(subItemIndex, currentFontSize, availableWidth, availableHeight);
                    File slideImageFile = new File(slideImagePath);
                    AppLogger.log("ProjectionController: PPT slide image path: " + slideImageFile.getAbsolutePath());

                    if (slideImageFile.exists()) {
                        try {
                            Image slideImage = new Image(slideImageFile.toURI().toString());
                            imageView.setImage(slideImage);
                            imageView.setPreserveRatio(true);
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
                titleLabel.setText(titleText); // Set title for PPT
                break;

            default:
                AppLogger.log("ProjectionController: Unsupported item type: " + item.getType());
                textContentContainer.setVisible(true);
                textContentContainer.setManaged(true);
                titleLabel.setText("Unsupported Item Type");
                lyricsFlow.getChildren().clear();
                lyricsFlow.getChildren().add(new Text("Cannot display: " + item.getType()));
                break;
        }
    }

    /**
     * Toggles play/pause for the currently playing video.
     */
    public void playPauseVideo() {
        if (mediaPlayer != null) {
            if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                mediaPlayer.pause();
                AppLogger.log("ProjectionController: Video paused.");
            } else {
                mediaPlayer.play();
                AppLogger.log("ProjectionController: Video played.");
            }
        } else {
            AppLogger.log("ProjectionController: No media player available to play/pause.");
        }
    }

    /**
     * Seeks the currently playing video by a given number of seconds.
     * @param seconds The number of seconds to seek. Positive for forward, negative for backward.
     */
    public void seekVideo(double seconds) {
        if (mediaPlayer != null && mediaPlayer.getStatus() != MediaPlayer.Status.STOPPED) {
            Duration currentTime = mediaPlayer.getCurrentTime();
            Duration newTime = currentTime.add(Duration.seconds(seconds));
            mediaPlayer.seek(newTime);
            AppLogger.log("ProjectionController: Video seeked by " + seconds + " seconds to " + newTime);
        } else {
            AppLogger.log("ProjectionController: No media player available or video stopped for seeking.");
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
        AppLogger.log("ProjectionController: Applying theme: " + theme.getName());

        // Apply font, size, color, line spacing, and alignment to lyricsFlow
        lyricsFlow.setStyle(String.format("-fx-font-family: '%s'; -fx-font-size: %.1fpx; -fx-fill: %s; -fx-line-spacing: %.1fpx;",
                theme.getFontFamily(), theme.getFontSize(), theme.getTextColor(), theme.getLineSpacing()));
        
        // Apply font, size, and color to titleLabel
        titleLabel.setStyle(String.format("-fx-font-family: '%s'; -fx-font-size: %.1fpx; -fx-text-fill: %s;",
                theme.getFontFamily(), theme.getFontSize() * 0.7, theme.getTextColor())); // Title font size slightly smaller

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
        // First, clear any existing background media
        projectionRoot.setStyle("-fx-background-color: " + theme.getBackgroundColor() + ";"); // Default to color
        imageView.setVisible(false);
        imageView.setManaged(false);
        imageView.setImage(null);
        mediaView.setVisible(false);
        mediaView.setManaged(false);
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
            mediaPlayer = null;
        }

        if (theme.getBackgroundImagePath() != null && !theme.getBackgroundImagePath().isEmpty()) {
            File imageFile = new File(theme.getBackgroundImagePath());
            if (imageFile.exists()) {
                try {
                    Image image = new Image(imageFile.toURI().toString());
                    imageView.setImage(image);
                    imageView.setPreserveRatio(true);
                    imageView.setFitWidth(projectionRoot.getWidth());
                    imageView.setFitHeight(projectionRoot.getHeight());
                    imageView.setVisible(true);
                    imageView.setManaged(true);
                    AppLogger.log("ProjectionController: Applied background image: " + theme.getBackgroundImagePath());
                } catch (Exception e) {
                    AppLogger.log("ProjectionController: Error applying background image: " + e.getMessage());
                }
            } else {
                AppLogger.log("ProjectionController: Background image file not found: " + theme.getBackgroundImagePath());
            }
        } else if (theme.getBackgroundVideoPath() != null && !theme.getBackgroundVideoPath().isEmpty()) {
            File videoFile = new File(theme.getBackgroundVideoPath());
            if (videoFile.exists()) {
                try {
                    Media media = new Media(videoFile.toURI().toString());
                    mediaPlayer = new MediaPlayer(media);
                    mediaView.setMediaPlayer(mediaPlayer);
                    mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                    mediaPlayer.play();
                    mediaView.setPreserveRatio(true);
                    mediaView.setFitWidth(projectionRoot.getWidth());
                    mediaView.setFitHeight(projectionRoot.getHeight());
                    mediaView.setVisible(true);
                    mediaView.setManaged(true);
                    AppLogger.log("ProjectionController: Applied background video: " + theme.getBackgroundVideoPath());
                } catch (Exception e) {
                    AppLogger.log("ProjectionController: Error applying background video: " + e.getMessage());
                }
            } else {
                AppLogger.log("ProjectionController: Background video file not found: " + theme.getBackgroundVideoPath());
            }
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
        return titleLabel != null ? titleLabel.getText() : "";
    }

    public Projectable getCurrentProjectedItem() {
        return currentProjectedItem;
    }

    public int getCurrentSubItemIndex() {
        return currentSubItemIndex;
    }

    public void blackout() {
        projectionRoot.setStyle("-fx-background-color: black;");
        // Stop media playback on blackout
        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }
        clear(); // Clear content, but keep background black
    }

    public void clear() {
        projectionRoot.setStyle("-fx-background-color: #0f0f0f;"); // Reset to default background
        hideAllMediaViews(); // Hide all specific content views
        titleLabel.setText("");
        currentProjectedItem = null; // Clear current item state
        currentSubItemIndex = 0;
        currentProjectedItemPages = null; // Clear cached pages
        lastPaginationFontSize = -1; // Reset pagination state
        lastPaginationWidth = -1;
        lastPaginationHeight = -1;
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
