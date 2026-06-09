package com.praiseview.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
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

    // Custom constructor for when ID is not provided (e.g., from UI)
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
    }

    @Override
    public String getType() {
        return "TEXT";
    }

    @Override
    public List<String> paginateForDimensions(double fontSize, double maxWidth, double maxHeight) {
        // For a simple text, we'll treat the entire body as one page for now.
        // More sophisticated pagination would involve breaking the text into chunks.
        return Collections.singletonList(content);
    }

    @Override
    public String getSubItemContent(int index, double fontSize, double maxWidth, double maxHeight) {
        if (index == 0) {
            return content;
        }
        return ""; // Only one sub-item (the whole text) for now
    }

    @Override
    public int getSubItemCount(double fontSize, double maxWidth, double maxHeight) {
        return 1; // Only one sub-item (the whole text) for now
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
