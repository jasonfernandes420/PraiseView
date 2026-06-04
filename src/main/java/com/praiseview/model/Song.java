package com.praiseview.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Song {

    private String id = UUID.randomUUID().toString();
    private String title;
    private String language;        // English, Hindi, Kannada, Tamil
    private String category;        // Entrance Hymn, Offertory, etc.
    private String author;
    private String composer;
    private String copyright;
    private List<Verse> verses = new ArrayList<>();

    private List<Integer> verseOrder = new ArrayList<>();

    // Getters and Setters
    public String getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getComposer() { return composer; }
    public void setComposer(String composer) { this.composer = composer; }

    public String getCopyright() { return copyright; }
    public void setCopyright(String copyright) { this.copyright = copyright; }

    public List<Verse> getVerses() { return verses; }
    public void setVerses(List<Verse> verses) {
        this.verses = verses;
        // Auto-create default order
        if (verseOrder.isEmpty() && !verses.isEmpty()) {
            for (int i = 0; i < verses.size(); i++) {
                verseOrder.add(i);
            }
        }
    }

    public List<Integer> getVerseOrder() { return verseOrder; }
    public void setVerseOrder(List<Integer> verseOrder) {
        this.verseOrder = verseOrder;
    }

    // Get verse at specific position in the custom order
    public Verse getVerseAtPosition(int position) {
        if (position < 0 || position >= verseOrder.size()) return null;
        int verseIndex = verseOrder.get(position);
        return verses.get(verseIndex);
    }

    public int getTotalSlides() {
        return verseOrder.size();
    }

    public void addVerse(Verse verse) {
        this.verses.add(verse);
    }

    public void setId(String id) {
        this.id = id;
    }
}
