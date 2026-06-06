package com.praiseview.controller;

import com.praiseview.model.Announcement;
import com.praiseview.model.Prayer;
import com.praiseview.model.Projectable;
import com.praiseview.model.Song;
import com.praiseview.model.MediaItem;
import com.praiseview.model.PptItem; // Import PptItem
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
        imageView.setVisible(false);
        imageView.setManaged(false);
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

        // Stop any existing media and hide all media views first
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

                // Determine if re-pagination is needed for text content
                boolean itemChanged = (this.currentProjectedItem == null || !this.currentProjectedItem.equals(item));
                boolean dimensionsChanged = (availableWidth != lastPaginationWidth || availableHeight != lastPaginationHeight);
                boolean fontSizeChanged = (currentFontSize != lastPaginationFontSize);
                boolean needsRepagination = itemChanged || dimensionsChanged || fontSizeChanged || currentProjectedItemPages == null;

                if (needsRepagination) {
                    AppLogger.log("ProjectionController: Re-paginating text content.");
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
                }

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

                lyricsFlow.getChildren().clear();
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
        if (currentProjectedItem instanceof Song || currentProjectedItem instanceof Prayer || currentProjectedItem instanceof Announcement) {
            if (lyricsFlow != null && !lyricsFlow.getChildren().isEmpty() && lyricsFlow.getChildren().get(0) instanceof Text) {
                return ((Text) lyricsFlow.getChildren().get(0)).getText();
            }
        } else if (currentProjectedItem instanceof MediaItem) {
            MediaItem media = (MediaItem) currentProjectedItem;
            if (media.getMediaType() == MediaItem.MediaType.IMAGE || media.getMediaType() == MediaItem.MediaType.VIDEO) {
                return media.getFilePath(); // Return file path for image/video
            }
        } else if (currentProjectedItem instanceof PptItem) {
            PptItem ppt = (PptItem) currentProjectedItem;
            // Return the path to the current slide image
            if (ppt.getRenderedSlideImagePaths() != null && !ppt.getRenderedSlideImagePaths().isEmpty() && currentSubItemIndex < ppt.getRenderedSlideImagePaths().size()) {
                return ppt.getRenderedSlideImagePaths().get(currentSubItemIndex);
            }
        }
        return ""; // Default for non-text or unhandled cases
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
