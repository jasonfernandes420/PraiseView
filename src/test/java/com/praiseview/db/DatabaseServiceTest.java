package com.praiseview.db;

import com.praiseview.model.Prayer;
import com.praiseview.model.Song;
import com.praiseview.model.TextSlide;
import com.praiseview.model.Verse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DatabaseServiceTest {

    @Mock private DatabaseService.ConnectionProvider connectionProvider;
    @Mock private Connection connection;
    @Mock private Statement statement;
    @Mock private PreparedStatement preparedStatement;

    private DatabaseService databaseService;

    @BeforeEach
    void setUp() throws Exception {
        when(connectionProvider.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        lenient().when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        databaseService = new DatabaseService(connectionProvider);
    }

    @Test
    void savePrayerBindsEveryPrayerFieldAndExecutesStatement() throws Exception {
        Prayer prayer = new Prayer("Prayer of the Faithful", "Lord hear us", "Mass");
        prayer.setId("prayer-1");

        databaseService.savePrayer(prayer);

        verify(preparedStatement).setString(1, "prayer-1");
        verify(preparedStatement).setString(2, "Prayer of the Faithful");
        verify(preparedStatement).setString(3, "Lord hear us");
        verify(preparedStatement).setString(4, "Mass");
        verify(preparedStatement).executeUpdate();
    }

    @Test
    void saveTextBindsIdTitleAndContent() throws Exception {
        TextSlide text = new TextSlide("Welcome", "Please stand");
        text.setId("text-1");

        databaseService.saveText(text);

        verify(preparedStatement).setString(1, "text-1");
        verify(preparedStatement).setString(2, "Welcome");
        verify(preparedStatement).setString(3, "Please stand");
        verify(preparedStatement).executeUpdate();
    }

    @Test
    void deletePrayerUsesTheProvidedId() throws Exception {
        databaseService.deletePrayer("prayer-2");

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sql.capture());
        assertEquals("DELETE FROM prayers WHERE id = ?", sql.getValue());
        verify(preparedStatement).setString(1, "prayer-2");
        verify(preparedStatement).executeUpdate();
    }

    @Test
    void deleteTextUsesTheProvidedId() throws Exception {
        databaseService.deleteText("text-2");

        verify(preparedStatement).setString(1, "text-2");
        verify(preparedStatement).executeUpdate();
    }

/*    @Test
    void saveSongPersistsMetadataVerseOrderAndVerses() throws Exception {
        Song song = new Song();
        song.setId("song-1");
        song.setTitle("Entrance Hymn");
        song.setLanguage("English");
        song.setCategory("Entrance");
        song.setAuthor("Author");
        song.setComposer("Composer");
        song.setCopyright("Copyright");
        song.setVerses(List.of(new Verse("Verse 1", "Sing", Verse.VerseType.VERSE)));

        databaseService.saveSong(song);

        verify(connection).setAutoCommit(false);
        verify(preparedStatement, atLeastOnce()).setString(1, "song-1");
        verify(preparedStatement, atLeastOnce()).setString(2, "Entrance Hymn");
        verify(preparedStatement).setString(8, "0");
        verify(preparedStatement, atLeastOnce()).setString(3, "song-1");
        verify(preparedStatement, atLeastOnce()).setString(2, "Verse 1");
        verify(preparedStatement, atLeastOnce()).setString(3, "Sing");
        verify(preparedStatement).setString(4, "VERSE");
        verify(connection).commit();
    }*/

    @Test
    void loadAllSongsRestoresMetadataVersesAndCustomOrder() throws Exception {
        ResultSet songs = mock(ResultSet.class);
        ResultSet verses = mock(ResultSet.class);
        when(statement.executeQuery(contains("SELECT * FROM songs"))).thenReturn(songs);
        when(statement.executeQuery(contains("SELECT label, content"))).thenReturn(verses);
        when(songs.next()).thenReturn(true, false);
        when(songs.getString("id")).thenReturn("song-1");
        when(songs.getString("title")).thenReturn("Song");
        when(songs.getString("language")).thenReturn("English");
        when(songs.getString("category")).thenReturn("Mass");
        when(songs.getString("author")).thenReturn("Author");
        when(songs.getString("composer")).thenReturn("Composer");
        when(songs.getString("copyright")).thenReturn("Copyright");
        when(songs.getString("verse_order")).thenReturn("1,0");
        when(verses.next()).thenReturn(true, true, false);
        when(verses.getString("label")).thenReturn("Verse 1", "Verse 2");
        when(verses.getString("content")).thenReturn("First", "Second");
        when(verses.getString("type")).thenReturn("VERSE", "CHORUS");

        List<Song> loaded = databaseService.loadAllSongs();

        assertEquals(1, loaded.size());
        assertEquals("Song", loaded.getFirst().getTitle());
        assertEquals(List.of(1, 0), loaded.getFirst().getVerseOrder());
        assertEquals("Second", loaded.getFirst().getSubItemContent(0, 48, 1920, 1080));
        assertEquals("First", loaded.getFirst().getSubItemContent(1, 48, 1920, 1080));
    }

    @Test
    void loadAllPrayersAndTextsMapsDatabaseRows() throws Exception {
        ResultSet prayers = mock(ResultSet.class);
        ResultSet texts = mock(ResultSet.class);
        when(statement.executeQuery(contains("SELECT * FROM prayers"))).thenReturn(prayers);
        when(statement.executeQuery(contains("SELECT * FROM texts"))).thenReturn(texts);
        when(prayers.next()).thenReturn(true, false);
        when(prayers.getString("id")).thenReturn("prayer-1");
        when(prayers.getString("title")).thenReturn("Collect");
        when(prayers.getString("content")).thenReturn("Let us pray");
        when(prayers.getString("category")).thenReturn("Mass");
        when(texts.next()).thenReturn(true, false);
        when(texts.getString("id")).thenReturn("text-1");
        when(texts.getString("title")).thenReturn("Welcome");
        when(texts.getString("body")).thenReturn("Good morning");

        List<Prayer> loadedPrayers = databaseService.loadAllPrayers();
        List<TextSlide> loadedTexts = databaseService.loadAllTexts();

        assertEquals("Let us pray", loadedPrayers.getFirst().getContent());
        assertEquals("Mass", loadedPrayers.getFirst().getCategory());
        assertEquals("Welcome", loadedTexts.getFirst().getTitle());
        assertEquals("Good morning", loadedTexts.getFirst().getContent());
    }

    @Test
    void deleteSongUsesATransaction() throws Exception {
        databaseService.deleteSong("song-2");

        verify(connection).setAutoCommit(false);
        verify(preparedStatement).setString(1, "song-2");
        verify(preparedStatement).executeUpdate();
        verify(connection).commit();
    }
}
