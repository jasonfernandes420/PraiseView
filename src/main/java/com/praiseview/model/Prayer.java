package com.praiseview.model;

import com.praiseview.util.TextPaginationUtil; // Import the utility class
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Prayer implements Projectable { // Implement Projectable interface

    private String id = UUID.randomUUID().toString();
    private String title;
    private String content;
    private String category;        // e.g., "Ordinary", "Lenten", "Eucharistic", "Seasonal"

    public Prayer(String title, String content, String category) {
        this.title = title;
        this.content = content;
        this.category = category;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    @Override
    public String toString() {
        return title;
    }

    // --- Projectable Interface Implementations ---

    @Override
    public String getType() {
        return "PRAYER";
    }

    @Override
    public String getFullContent() {
        return content;
    }

    @Override
    public String getSubItemContent(int index, double fontSize, double maxWidth, double maxHeight) {
        List<String> pages = TextPaginationUtil.paginateText(this.content, fontSize, maxWidth, maxHeight);
        if (index >= 0 && index < pages.size()) {
            return pages.get(index);
        }
        return ""; // Return empty string if index is out of bounds
    }

    @Override
    public int getSubItemCount(double fontSize, double maxWidth, double maxHeight) {
        return TextPaginationUtil.paginateText(this.content, fontSize, maxWidth, maxHeight).size();
    }
}
