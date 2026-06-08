package com.praiseview.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
public class Announcement implements Projectable {

    private String id = UUID.randomUUID().toString();
    private String title;
    private String content; // The actual announcement text

    // Transient fields for caching paginated content
    @JsonIgnore // Ignore this field during JSON serialization/deserialization
    private transient List<String> paginatedContent;
    @JsonIgnore
    private transient double cachedFontSize = -1;
    @JsonIgnore
    private transient double cachedMaxWidth = -1;
    @JsonIgnore
    private transient double cachedMaxHeight = -1;
    @JsonIgnore
    private transient String cachedContentHash; // To detect if 'content' has changed

    public Announcement(String content) {
        this.title = "Announcement";
        this.content = content;
        this.cachedContentHash = String.valueOf(Objects.hash(content));
    }

    // Lombok @Data generates getters/setters, but we need to ensure cachedContentHash is updated
    public void setContent(String content) {
        this.content = content;
        this.cachedContentHash = String.valueOf(Objects.hash(content)); // Update hash when content changes
        // Invalidate cache when content changes
        this.paginatedContent = null;
        this.cachedFontSize = -1;
        this.cachedMaxWidth = -1;
        this.cachedMaxHeight = -1;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }


    @Override
    public String getType() {
        return "ANNOUNCEMENT";
    }

    @Override
    public String getFullContent() {
        return content;
    }

    /**
     * Re-paginates the content if the dimensions or font size have changed, or if content itself changed.
     * Caches the result.
     * @param fontSize The font size for pagination.
     * @param maxWidth The maximum width for pagination.
     * @param maxHeight The maximum height for pagination.
     */
    public void rePaginate(double fontSize, double maxWidth, double maxHeight) {
        String currentContentHash = String.valueOf(Objects.hash(this.content));

        if (paginatedContent == null ||
            fontSize != cachedFontSize ||
            maxWidth != cachedMaxWidth ||
            maxHeight != cachedMaxHeight ||
            !currentContentHash.equals(cachedContentHash)) { // Check if content itself changed
            
            this.paginatedContent = TextPaginationUtil.paginateText(this.content, fontSize, maxWidth, maxHeight);
            this.cachedFontSize = fontSize;
            this.cachedMaxWidth = maxWidth;
            this.cachedMaxHeight = maxHeight;
            this.cachedContentHash = currentContentHash;
        }
    }

    @Override
    public String getSubItemContent(int index, double fontSize, double maxWidth, double maxHeight) {
        rePaginate(fontSize, maxWidth, maxHeight); // Ensure content is paginated for current dimensions
        if (paginatedContent != null && index >= 0 && index < paginatedContent.size()) {
            return paginatedContent.get(index);
        }
        return ""; // Return empty string if index is out of bounds or no content
    }

    @Override
    public int getSubItemCount(double fontSize, double maxWidth, double maxHeight) {
        rePaginate(fontSize, maxWidth, maxHeight); // Ensure content is paginated for current dimensions
        return paginatedContent != null ? paginatedContent.size() : 0;
    }

    @Override
    public String toString() {
        return title + ": " + (content.length() > 50 ? content.substring(0, 47) + "..." : content);
    }

    @Override
    public String getSubItemLabel(int index) {
        // Use arbitrary reasonable dimensions for label calculation, as it's just for the label text.
        // The actual content for projection will use projection-specific dimensions.
        // We need to call rePaginate here to ensure paginatedContent is populated for count.
        rePaginate(16.0, 400.0, 300.0); // Use preview-like dimensions for label calculation
        if (paginatedContent != null && index >= 0 && index < paginatedContent.size()) {
            return "Page " + (index + 1);
        }
        return "Page (N/A)";
    }
}
