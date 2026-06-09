package com.praiseview.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.List;

/**
 * Interface for any item that can be projected onto the screen.
 * This allows the ProjectionController to handle different types of content polymorphically.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = Song.class, name = "SONG"),
        @JsonSubTypes.Type(value = Prayer.class, name = "PRAYER"),
        @JsonSubTypes.Type(value = TextSlide.class, name = "TEXT"),
        @JsonSubTypes.Type(value = MediaItem.class, name = "IMAGE"), // Add MediaItem for IMAGE
        @JsonSubTypes.Type(value = MediaItem.class, name = "VIDEO"), // Add MediaItem for VIDEO
        @JsonSubTypes.Type(value = PptItem.class, name = "PPT")    // Add PptItem for PPT
        // Add other Projectable implementations here as they are created
})
public interface Projectable {
    /**
     * Returns the main title of the projectable item.
     * @return The title string.
     */
    String getTitle();

    /**
     * Returns the type identifier of the projectable item (e.g., "SONG", "PRAYER", "IMAGE").
     * This is crucial for Jackson deserialization and can be used for specific rendering logic if needed.
     * @return The type string.
     */
    String getType();

    /**
     * Returns the raw, full content of the item. For songs, this might be the entire lyrics.
     * For prayers, the full prayer text. This is used by the ProjectionController for pagination.
     * @return The full content as a String.
     */
    String getFullContent();

    List<String> paginateForDimensions(double fontSize, double maxWidth, double maxHeight);

    /**
     * Returns the content for a specific sub-item (e.g., a verse for a song, a page for a prayer, an image path for a PPT slide).
     * The interpretation of 'index' depends on the item type.
     * @param index The index of the sub-item (e.g., verse number, page number).
     * @param fontSize The font size to consider for dynamic content (e.g., pagination).
     * @param maxWidth The maximum width available for rendering (for pagination).
     * @param maxHeight The maximum height available for rendering (for pagination).
     * @return The content for the specified sub-item.
     */
    String getSubItemContent(int index, double fontSize, double maxWidth, double maxHeight);

    /**
     * Returns the total number of sub-items (e.g., verses for a song, pages for a prayer, slides for a PPT).
     * This can be dynamic for items like prayers that are paginated based on display size.
     * @param fontSize The font size to consider for dynamic content (e.g., pagination).
     * @param maxWidth The maximum width available for rendering (for pagination).
     * @param maxHeight The maximum height available for rendering (for pagination).
     * @return The total count of sub-items.
     */
    int getSubItemCount(double fontSize, double maxWidth, double maxHeight);

    /**
     * Returns a user-friendly label for a specific sub-item (e.g., "Verse 1", "Page 2", "Slide 3").
     * This is used for displaying in lists like the "Current Item Verses/Pages" pane.
     * @param index The index of the sub-item.
     * @return A descriptive label for the sub-item.
     */
    String getSubItemLabel(int index);
}