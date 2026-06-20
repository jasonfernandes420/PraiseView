package com.praiseview.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Verse {
    private String label;
    private String content;
    private VerseType type;

    public enum VerseType {
        VERSE, CHORUS, BRIDGE, CODA, PRE_CHORUS, OTHER
    }

    public String getLabel() { return label; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public VerseType getType() { return type; }
    public void setType(VerseType type) { this.type = type; }
    public void setLabel(String label) { this.label = label; }
    
    @Override
    public String toString() {
        return label;
    }
}