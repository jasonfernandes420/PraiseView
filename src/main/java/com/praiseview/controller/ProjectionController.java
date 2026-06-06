package com.praiseview.controller;

import com.praiseview.model.Announcement;
import com.praiseview.model.Prayer;
import com.praiseview.model.Projectable;
import com.praiseview.model.Song;
import com.praiseview.util.TextPaginationUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;

import java.util.List;

public class ProjectionController {

    @FXML private StackPane projectionRoot;
    @FXML private Label titleLabel;
    @FXML public TextFlow lyricsFlow; // Made public for MainController to access dimensions

    public double currentFontSize = 62.0; // Made public for MainController to access

    private Projectable currentProjectedItem; // Stores the currently projected item
    private int currentSubItemIndex = 0; // Stores the current verse/page index
    private List<String> currentProjectedItemPages; // Cached paginated content for the current item
    private double lastPaginationFontSize = -1; // Track font size used for last pagination
    private double lastPaginationWidth = -1;    // Track width used for last pagination
    private double lastPaginationHeight = -1;   // Track height used for last pagination


    @FXML
    public void initialize() {
        System.out.println("ProjectionController initialized");
    }

    /**
     * Main method to display any Projectable item on the projection screen.
     *
     * @param item The Projectable item to display (Song, Prayer, Announcement, etc.).
     * @param subItemIndex The 0-based index of the sub-item (e.g., verse for a song, page for a prayer).
     */
    public void showItem(Projectable item, int subItemIndex) {
        if (item == null) {
            clear();
            return;
        }

        // Get actual dimensions of the lyricsFlow for accurate pagination
        // Fallback to reasonable defaults if not yet laid out
        double availableWidth = lyricsFlow.getWidth() > 0 ? lyricsFlow.getWidth() : 1000; // Default if not laid out
        double availableHeight = lyricsFlow.getHeight() > 0 ? lyricsFlow.getHeight() : 700; // Default if not laid out

        // Determine if re-pagination is needed
        boolean itemChanged = (this.currentProjectedItem == null || !this.currentProjectedItem.equals(item));
        boolean dimensionsChanged = (availableWidth != lastPaginationWidth || availableHeight != lastPaginationHeight);
        boolean fontSizeChanged = (currentFontSize != lastPaginationFontSize);
        
        boolean needsRepagination = itemChanged || dimensionsChanged || fontSizeChanged || currentProjectedItemPages == null;

        if (needsRepagination) {
            if (item instanceof Song) {
                // Songs are paginated by verses, not dynamic text flow.
                // Each verse is a "page". We just need the content of the specific verse.
                // The Projectable.getSubItemContent for Song already handles this.
                // We'll store each verse as a separate "page" in our cache for consistency.
                currentProjectedItemPages = new java.util.ArrayList<>();
                for (int i = 0; i < item.getSubItemCount(currentFontSize, availableWidth, availableHeight); i++) {
                    currentProjectedItemPages.add(item.getSubItemContent(i, currentFontSize, availableWidth, availableHeight));
                }
            } else { // For dynamic content (Prayer, Announcement)
                currentProjectedItemPages = TextPaginationUtil.paginateText(item.getFullContent(), currentFontSize, availableWidth, availableHeight);
            }
            this.currentProjectedItem = item;
            this.lastPaginationFontSize = currentFontSize;
            this.lastPaginationWidth = availableWidth;
            this.lastPaginationHeight = availableHeight;
        }

        this.currentSubItemIndex = subItemIndex;

        // Ensure subItemIndex is within bounds of the newly calculated pages
        if (currentProjectedItemPages != null && subItemIndex >= currentProjectedItemPages.size()) {
            this.currentSubItemIndex = currentProjectedItemPages.size() - 1;
            if (this.currentSubItemIndex < 0) this.currentSubItemIndex = 0; // Fallback for empty content
        }


        // Reset styling
        projectionRoot.setStyle("-fx-background-color: #0f0f0f;"); // Default background
        lyricsFlow.getChildren().clear();
        titleLabel.setText("");

        String contentToDisplay = "";
        if (currentProjectedItemPages != null && !currentProjectedItemPages.isEmpty()) {
            contentToDisplay = currentProjectedItemPages.get(this.currentSubItemIndex);
        }

        int totalSubItems = currentProjectedItemPages != null ? currentProjectedItemPages.size() : 0;

        String titleText = item.getTitle();
        // Only show (X/Y) if there's more than one sub-item/page AND it's not a Song (user requested no slide numbers for songs)
        if (totalSubItems > 1 && !(item instanceof Song)) { 
            titleText += " (" + (this.currentSubItemIndex + 1) + "/" + totalSubItems + ")";
        }
        titleLabel.setText(titleText);
        titleLabel.setStyle("-fx-text-fill: #ffd700; -fx-font-size: 42px;");

        Text mainText = new Text(contentToDisplay);
        mainText.setFill(Color.WHITE);
        mainText.setStyle("-fx-font-size: " + currentFontSize + "px; -fx-line-spacing: 8px;");

        lyricsFlow.getChildren().add(mainText);
        lyricsFlow.setTextAlignment(TextAlignment.CENTER);
    }

    /**
     * Returns the total number of sub-items (pages/verses) for the currently projected item.
     * This is used by MainController for navigation logic.
     * @return The total count of sub-items.
     */
    public int getCurrentProjectedItemSubItemCount() {
        return currentProjectedItemPages != null ? currentProjectedItemPages.size() : 0;
    }

    // New getters for MainController to mirror the projection
    public String getCurrentDisplayedContent() {
        if (lyricsFlow != null && !lyricsFlow.getChildren().isEmpty() && lyricsFlow.getChildren().get(0) instanceof Text) {
            return ((Text) lyricsFlow.getChildren().get(0)).getText();
        }
        return "";
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
        clear(); // Clear content, but keep background black
    }

    public void clear() {
        projectionRoot.setStyle("-fx-background-color: #0f0f0f;"); // Reset to default background
        lyricsFlow.getChildren().clear();
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
