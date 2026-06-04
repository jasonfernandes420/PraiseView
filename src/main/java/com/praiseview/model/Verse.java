package com.praiseview.model;

public class Verse {
    private String label;
    private String content;
    private VerseType type;

    public enum VerseType {
        VERSE, CHORUS, BRIDGE, CODA, PRECHORUS, OTHER
    }

    public Verse(String label, String content, VerseType type) {
        this.label = label;
        this.content = content;
        this.type = type;
    }

    public String getLabel() { return label; }
    public String getContent() { return content; }
    public VerseType getType() { return type; }
    @Override
    public String toString() {
        return label;        // This is important for ListView display
    }
}