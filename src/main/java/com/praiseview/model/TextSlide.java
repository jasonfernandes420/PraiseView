package com.praiseview.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.praiseview.util.AppLogger;
import com.praiseview.util.TextPaginationUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TextSlide implements Projectable {

    private String id = UUID.randomUUID().toString();
    private String title;
    private String content;

    // Transient fields for pagination
    private transient List<String> paginatedPages;
    private transient double lastFontSize = -1;
    private transient double lastMaxWidth = -1;
    private transient double lastMaxHeight = -1;

    // Custom constructor for when ID is not provided (e.g., from UI)
    // The MainController should then retrieve the full object from allTexts
    public TextSlide(String title, String content) {
        this.title = title != null ? title : "";
        this.content = content != null ? content : "";
    }

    // Constructor used when dragging from library (only ID is known initially)
    // The MainController should then retrieve the full object from allTexts
    public TextSlide(String textId) {
        this.id = textId;
        this.title = ""; // Initialize to empty string
        this.content = ""; // Initialize to empty string
    }

    @Override
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title != null ? title : "";
    }

    @Override
    public String getFullContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content != null ? content : "";
        // Invalidate pagination cache when content changes
        this.paginatedPages = null;
    }

    @Override
    public String getType() {
        return "TEXT";
    }

    @Override
    public List<String> paginateForDimensions(double fontSize, double maxWidth, double maxHeight) {
        // Recalculate pagination only if dimensions or content have changed
        if (paginatedPages == null ||
            lastFontSize != fontSize ||
            lastMaxWidth != maxWidth ||
            lastMaxHeight != maxHeight) {

            AppLogger.log("TextSlide: Recalculating pagination for '" + this.title + "' with fontSize=" + fontSize + ", maxWidth=" + maxWidth + ", maxHeight=" + maxHeight);

            List<String> pages = TextPaginationUtil.paginateText(this.content, fontSize, maxWidth, maxHeight);
            paginatedPages = new ArrayList<>(pages);

            this.lastFontSize = fontSize;
            this.lastMaxWidth = maxWidth;
            this.lastMaxHeight = maxHeight;
        } else {
            AppLogger.log("TextSlide: Using cached pagination for '" + this.title + "'");
        }
        return new ArrayList<>(paginatedPages);
    }

    @Override
    public String getSubItemContent(int index, double fontSize, double maxWidth, double maxHeight) {
        // Ensure pagination is up-to-date before retrieving content
        paginateForDimensions(fontSize, maxWidth, maxHeight);
        if (paginatedPages == null || index < 0 || index >= paginatedPages.size()) {
            return "";
        }
        return paginatedPages.get(index);
    }

    @Override
    public int getSubItemCount(double fontSize, double maxWidth, double maxHeight) {
        // Ensure pagination is up-to-date before getting count
        paginateForDimensions(fontSize, maxWidth, maxHeight);
        return paginatedPages != null ? paginatedPages.size() : 1;
    }

    @Override
    public String getSubItemLabel(int index) {
        if (index == 0) {
            return "Text Content";
        }
        return "";
    }

    @Override
    public String toString() {
        return title != null && !title.isEmpty() ? title : "Untitled Text";
    }
}
