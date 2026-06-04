package com.praiseview.model;

import java.util.UUID;

public class ServiceItem {

    private String id;
    private Song song;
    private Verse currentVerse;
    private String type; // "SONG", "BIBLE", "ANNOUNCEMENT", "IMAGE"

    public ServiceItem(Song song) {
        this.id = UUID.randomUUID().toString();
        this.song = song;
        this.type = "SONG";
        if (!song.getVerses().isEmpty()) {
            this.currentVerse = song.getVerses().get(0);
        }
    }

    public Song getSong() { return song; }
    public Verse getCurrentVerse() { return currentVerse; }
    public void setCurrentVerse(Verse verse) { this.currentVerse = verse; }
    public String getType() { return type; }

    @Override
    public String toString() {
        return song != null ? song.getTitle() : "Unknown Item";
    }
}
