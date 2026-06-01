package com.praiseview.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Song {
    private String id = UUID.randomUUID().toString();
    private String title;
    private String artist;
    private String key;
    private List<Verse> verses = new ArrayList<>();
    private String copyright;
    private String notes;

    public String getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public List<Verse> getVerses() {
        return null;
    }
    public void addVerse(Verse verse) { this.verses.add(verse); }
    public String getCopyright() { return copyright; }
    public void setCopyright(String copyright) { this.copyright = copyright; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public void setVerses(ArrayList<Verse> verses) {
    }

    public void setId(String id) {
    }
}