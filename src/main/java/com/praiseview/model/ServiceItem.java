package com.praiseview.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
public class ServiceItem {

    private String id = UUID.randomUUID().toString();
    private Projectable content; // The actual projectable item (Song, Prayer, Announcement, etc.)

    // Custom constructor for Projectable content
    public ServiceItem(Projectable content) {
        this.id = UUID.randomUUID().toString(); // Ensure ID is generated for new items
        this.content = content;
    }

    // Explicitly define getType() to delegate to the Projectable content
    public String getType() {
        return content != null ? content.getType() : null;
    }

    // Explicitly define getTitle() to delegate to the Projectable content
    public String getTitle() {
        return content != null ? content.getTitle() : null;
    }

    @Override
    public String toString() {
        return getTitle();
    }
}
