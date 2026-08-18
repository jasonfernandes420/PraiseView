package com.praiseview.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SongTest {

    @Test
    void usesTheConfiguredVerseOrderForProjection() {
        Song song = new Song();
        song.setTitle("Amazing Grace");
        Verse verse = new Verse("Verse 1", "Amazing grace", Verse.VerseType.VERSE);
        Verse chorus = new Verse("Chorus", "Praise the Lord", Verse.VerseType.CHORUS);
        song.setVerses(List.of(verse, chorus));
        song.setVerseOrderFromList(List.of(chorus, verse, chorus));

        assertEquals(3, song.getSubItemCount(48, 1920, 1080));
        assertEquals("Chorus", song.getSubItemLabel(0));
        assertEquals("Amazing grace", song.getSubItemContent(1, 48, 1920, 1080));
        assertEquals("Praise the Lord\n\nAmazing grace\n\nPraise the Lord", song.getFullContent());
    }

    @Test
    void returnsSafeValuesForAnInvalidVersePosition() {
        Song song = new Song();
        song.setVerses(List.of(new Verse("Verse 1", "Line", Verse.VerseType.VERSE)));

        assertNull(song.getVerseAtPosition(-1));
        assertEquals("", song.getSubItemContent(3, 48, 1920, 1080));
        assertEquals("Verse (N/A)", song.getSubItemLabel(3));
    }
}
