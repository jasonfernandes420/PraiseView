package com.praiseview.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.praiseview.util.AppLogger; // Added for logging
import com.praiseview.util.TextPaginationUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
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

    public Prayer(String title, String content, String category) {
        this.title = title;
        this.content = content;
        this.category = category;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

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
     * For now, prayers are treated as a single page.
     * This method returns a list containing the entire prayer content as one element.
     */
    public List<String> paginateForDimensions(double fontSize, double maxWidth, double maxHeight) {
        // Log the call to confirm this simplified logic is being used
        AppLogger.log("Prayer: Using simplified single-page pagination for '" + this.title + "'");
        return Collections.singletonList(this.content);
    }

    @Override
    public String getSubItemContent(int index, double fontSize, double maxWidth, double maxHeight) {
        if (index == 0) {
            return this.content; // Return full content for the first (and only) page
        }
        return ""; // Only one page for prayers
    }

    @Override
    public int getSubItemCount(double fontSize, double maxWidth, double maxHeight) {
        return 1; // Prayers are always one page for now
    }

    @Override
    public String getSubItemLabel(int index) {
        return "Page " + (index + 1); // Will always be "Page 1"
    }
}
