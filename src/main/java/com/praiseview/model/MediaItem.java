package com.praiseview.model;

import com.praiseview.util.TextPaginationUtil; // Potentially for displaying file path if needed
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.File;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MediaItem implements Projectable {

    public enum MediaType {
        IMAGE, VIDEO, AUDIO, PPT
    }

    private String id = UUID.randomUUID().toString();
    private String title; // Derived from file name
    private String filePath;
    private MediaType mediaType;

    public MediaItem(File file, MediaType type) {
        this.id = UUID.randomUUID().toString();
        this.filePath = file.getAbsolutePath();
        this.title = file.getName(); // Use file name as title
        this.mediaType = type;
    }

    // --- Projectable Interface Implementations ---

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getType() {
        return mediaType.name(); // Returns "IMAGE", "VIDEO", "AUDIO", "PPT"
    }

    @Override
    public String getFullContent() {
        // For media, the "full content" is primarily the file path
        // We might display the file path or a placeholder in text form
        return "Media File: " + title + "\nPath: " + filePath;
    }

    @Override
    public List<String> paginateForDimensions(double fontSize, double maxWidth, double maxHeight) {
        return List.of();
    }

    @Override
    public String getSubItemContent(int index, double fontSize, double maxWidth, double maxHeight) {
        // For single media items, we typically only have one "sub-item" (the media itself)
        // If we were to paginate text (e.g., for a PPT converted to text), we'd use TextPaginationUtil here.
        // For now, we'll just return the file path or a descriptive text.
        if (index == 0) {
            return "Displaying " + mediaType.name() + ":\n" + title;
        }
        return "";
    }

    @Override
    public int getSubItemCount(double fontSize, double maxWidth, double maxHeight) {
        // Most media items will be a single "slide" or "page"
        // PPTs might eventually be paginated, but for now, assume 1.
        return 1;
    }

    @Override
    public String toString() {
        return "[" + mediaType.name() + "] " + title;
    }

    // ... (existing code)

    @Override
    public String getSubItemLabel(int index) {
        // Media items are currently single sub-items
        return mediaType.name();
    }
}
