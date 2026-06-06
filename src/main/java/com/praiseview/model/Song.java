package com.praiseview.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Song implements Projectable { // Implement Projectable interface

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

    // Get verse at specific position in the custom order
    public Verse getVerseAtPosition(int position) {
        if (position < 0 || position >= verseOrder.size()) return null;
        int verseIndex = verseOrder.get(position);
        return verses.get(verseIndex);
    }
    // New method for custom order from dialog
    public void setVerseOrderFromList(List<Verse> orderedList) {
        this.verseOrder.clear();
        for (Verse v : orderedList) {
            int index = verses.indexOf(v);
            if (index != -1) {
                verseOrder.add(index);
            }
        }
    }

    public void addVerse(Verse verse) {
        this.verses.add(verse);
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return getTitle();   // This fixes class name display
    }

    // --- Projectable Interface Implementations ---

    @Override
    public String getType() {
        return "SONG";
    }

    @Override
    public String getFullContent() {
        StringBuilder fullContent = new StringBuilder();
        for (int i = 0; i < verseOrder.size(); i++) {
            Verse verse = getVerseAtPosition(i);
            if (verse != null) {
                fullContent.append(verse.getContent()).append("\n\n");
            }
        }
        return fullContent.toString().trim();
    }

    @Override
    public String getSubItemContent(int index, double fontSize, double maxWidth, double maxHeight) {
        Verse verse = getVerseAtPosition(index);
        return (verse != null) ? verse.getContent() : "";
    }

    @Override
    public int getSubItemCount(double fontSize, double maxWidth, double maxHeight) {
        return verseOrder.size();
    }
}
