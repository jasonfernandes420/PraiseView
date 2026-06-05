package com.praiseview.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceItem {

    private String id = UUID.randomUUID().toString();
    private String type;           // "SONG", "BIBLE", "ANNOUNCEMENT", "IMAGE"
    private Song song;
   // private BibleVerse bibleVerse; // for future use
    private String announcementText;
    private String title;

    public ServiceItem(Song song) {
        this.type = "SONG";
        this.song = song;
        this.title = song.getTitle();
    }

    public ServiceItem(String announcement) {
        this.type = "ANNOUNCEMENT";
        this.announcementText = announcement;
        this.title = "Announcement";
    }

    public String getId() { return id; }
    public String getType() { return type; }
    public Song getSong() { return song; }
    public String getTitle() { return title; }
    public String getAnnouncementText() { return announcementText; }

    @Override
    public String toString() {
        return title;
    }
}