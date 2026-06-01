package com.praiseview.model;

public class ServiceItem {
    private String id;
    private ItemType type;
    private Song song;
    private String title;
    private String content; // for non-song items

    public enum ItemType { SONG, BIBLE_VERSE, IMAGE, VIDEO, OTHER }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public ItemType getType() { return type; }
    public void setType(ItemType type) { this.type = type; }
    public Song getSong() { return song; }
    public void setSong(Song song) { this.song = song; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
}