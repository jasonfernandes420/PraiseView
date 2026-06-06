package com.praiseview.model;

import com.praiseview.util.TextPaginationUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Announcement implements Projectable {

    private String id = UUID.randomUUID().toString();
    private String title;
    private String content; // The actual announcement text

    public Announcement(String content) {
        this.title = "Announcement";
        this.content = content;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getType() {
        return "ANNOUNCEMENT";
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
        return "";
    }

    @Override
    public int getSubItemCount(double fontSize, double maxWidth, double maxHeight) {
        return TextPaginationUtil.paginateText(this.content, fontSize, maxWidth, maxHeight).size();
    }

    @Override
    public String toString() {
        return title + ": " + (content.length() > 50 ? content.substring(0, 47) + "..." : content);
    }

    @Override
    public String getSubItemLabel(int index) {
        // For announcements, sub-items are pages.
        // Similar to Prayer, re-paginate for accurate labeling.
        int totalPages = TextPaginationUtil.paginateText(this.content, 16.0, 400.0, 300.0).size(); // Use preview-like dimensions
        if (index >= 0 && index < totalPages) {
            return "Page " + (index + 1);
        }
        return "Page (N/A)";
    }
}
