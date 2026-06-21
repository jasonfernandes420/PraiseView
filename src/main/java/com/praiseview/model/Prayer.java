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
public class Prayer implements Projectable {

    private String id = UUID.randomUUID().toString();
    private String title;
    private String content;
    private String category;

    // Transient fields for pagination
    private transient List<String> paginatedPages;
    private transient double lastFontSize = -1;
    private transient double lastMaxWidth = -1;
    private transient double lastMaxHeight = -1;

    public Prayer(String title, String content, String category) {
        this.title = title;
        this.content = content;
        this.category = category;
    }

    public void setContent(String content) {
        this.content = content;
        // Invalidate pagination cache when content changes
        this.paginatedPages = null;
    }

    @Override
    public String toString() {
        return title;
    }

    @Override
    public String getType() {
        return "PRAYER";
    }

    @Override
    public String getFullContent() {
        return content;
    }

    /**
     * Paginate the prayer content based on the provided dimensions.
     * The pagination is cached and recalculated only if dimensions or content change.
     *
     * @param fontSize The font size to use for measurement.
     * @param maxWidth The maximum width available for a line of text.
     * @param maxHeight The maximum height available for a page of text.
     * @return A list of strings, where each string represents a page of content.
     */

    public List<String> paginateForDimensions(double fontSize, double maxWidth, double maxHeight) {
        // Recalculate pagination only if dimensions or content have changed
        if (paginatedPages == null ||
            lastFontSize != fontSize ||
            lastMaxWidth != maxWidth ||
            lastMaxHeight != maxHeight) {

            AppLogger.log("Prayer: Recalculating pagination for '" + this.title + "' with fontSize=" + fontSize + ", maxWidth=" + maxWidth + ", maxHeight=" + maxHeight);

            List<String> pages = TextPaginationUtil.paginateText(this.content, fontSize, maxWidth, maxHeight);
            paginatedPages = new ArrayList<>(pages);

            this.lastFontSize = fontSize;
            this.lastMaxWidth = maxWidth;
            this.lastMaxHeight = maxHeight;
        } else {
            AppLogger.log("Prayer: Using cached pagination for '" + this.title + "'");
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
        return paginatedPages == null ? 0 : paginatedPages.size();
    }

    @Override
    public String getSubItemLabel(int index) {
        return "Page " + (index + 1);
    }
}
