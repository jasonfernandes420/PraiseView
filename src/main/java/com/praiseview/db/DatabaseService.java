package com.praiseview.db;

import com.praiseview.model.Song;
import com.praiseview.model.Verse;
import java.sql.*;
import java.util.*;

public class DatabaseService {

    private static final String DB_URL = "jdbc:sqlite:praiseview.db";

    public DatabaseService() {
        createTables();
    }

    private void createTables() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {

            // Updated songs table with all columns
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS songs (
                    id TEXT PRIMARY KEY,
                    title TEXT NOT NULL,
                    language TEXT,
                    category TEXT,
                    author TEXT,
                    composer TEXT,
                    copyright TEXT,
                    verse_order TEXT
                )""");

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS verses (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    song_id TEXT,
                    label TEXT,
                    content TEXT,
                    type TEXT,
                    position INTEGER,
                    FOREIGN KEY(song_id) REFERENCES songs(id)
                )""");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveSong(Song song) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {

            // Convert verseOrder to comma-separated string
            String verseOrderStr = song.getVerseOrder().isEmpty() ? "" : 
                String.join(",", song.getVerseOrder().stream().map(String::valueOf).toArray(String[]::new));

            // Save song
            String songSql = """
                INSERT OR REPLACE INTO songs (id, title, language, category, author, composer, copyright, verse_order)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)""";

            try (PreparedStatement pstmt = conn.prepareStatement(songSql)) {
                pstmt.setString(1, song.getId());
                pstmt.setString(2, song.getTitle());
                pstmt.setString(3, song.getLanguage());
                pstmt.setString(4, song.getCategory());
                pstmt.setString(5, song.getAuthor());
                pstmt.setString(6, song.getComposer());
                pstmt.setString(7, song.getCopyright() != null ? song.getCopyright() : "");
                pstmt.setString(8, verseOrderStr);
                pstmt.executeUpdate();
            }

            // Save verses
            try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM verses WHERE song_id = ?")) {
                pstmt.setString(1, song.getId());
                pstmt.executeUpdate();
            }

            String verseSql = "INSERT INTO verses (song_id, label, content, type, position) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(verseSql)) {
                for (int i = 0; i < song.getVerses().size(); i++) {
                    Verse v = song.getVerses().get(i);
                    pstmt.setString(1, song.getId());
                    pstmt.setString(2, v.getLabel());
                    pstmt.setString(3, v.getContent());
                    pstmt.setString(4, v.getType().name());
                    pstmt.setInt(5, i);
                    pstmt.executeUpdate();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<Song> loadAllSongs() {
        List<Song> songs = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery("SELECT * FROM songs ORDER BY title");
            while (rs.next()) {
                Song song = new Song();
                song.setId(rs.getString("id"));
                song.setTitle(rs.getString("title"));
                song.setLanguage(rs.getString("language"));
                song.setCategory(rs.getString("category"));
                song.setAuthor(rs.getString("author"));
                song.setComposer(rs.getString("composer"));
                
                // Load verses for this song
                List<Verse> verses = new ArrayList<>();
                try (Statement verseStmt = conn.createStatement()) {
                    ResultSet verseRs = verseStmt.executeQuery(
                        "SELECT label, content, type, position FROM verses WHERE song_id = '" + song.getId() + "' ORDER BY position");
                    while (verseRs.next()) {
                        Verse verse = new Verse(
                            verseRs.getString("label"),
                            verseRs.getString("content"),
                            Verse.VerseType.valueOf(verseRs.getString("type"))
                        );
                        verses.add(verse);
                    }
                }
                song.setVerses(verses);
                
                // Load verse order
                String verseOrderStr = rs.getString("verse_order");
                if (verseOrderStr != null && !verseOrderStr.isEmpty()) {
                    List<Integer> verseOrder = new ArrayList<>();
                    for (String num : verseOrderStr.split(",")) {
                        verseOrder.add(Integer.parseInt(num));
                    }
                    song.getVerseOrder().clear();
                    song.getVerseOrder().addAll(verseOrder);
                }
                
                songs.add(song);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return songs;
    }
}