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

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS songs (
                    id TEXT PRIMARY KEY,
                    title TEXT NOT NULL,
                    artist TEXT,
                    key TEXT,
                    copyright TEXT
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
            // Save song
            String sql = "INSERT OR REPLACE INTO songs (id, title, artist, key, copyright) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, song.getId());
                pstmt.setString(2, song.getTitle());
                pstmt.setString(3, song.getArtist());
                pstmt.setString(4, song.getKey());
                pstmt.setString(5, song.getCopyright());
                pstmt.executeUpdate();
            }

            // Save verses
            String deleteSql = "DELETE FROM verses WHERE song_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(deleteSql)) {
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

            ResultSet rs = stmt.executeQuery("SELECT * FROM songs");
            while (rs.next()) {
                Song song = new Song();
                song.setId(rs.getString("id"));
                song.setTitle(rs.getString("title"));
                song.setArtist(rs.getString("artist"));
                song.setKey(rs.getString("key"));
                // Load verses for this song (add logic if needed)
                songs.add(song);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return songs;
    }
}