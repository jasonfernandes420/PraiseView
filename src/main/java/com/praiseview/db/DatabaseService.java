package com.praiseview.db;

import com.praiseview.model.Prayer;
import com.praiseview.model.Song;
import com.praiseview.model.TextSlide;
import com.praiseview.model.Verse;
import com.praiseview.util.AppLogger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseService {

    private static final String DB_FILE_NAME = "praiseview.db";
    private static String DB_URL;

    public DatabaseService() {
        initializeDbPath();
        createTables();
    }

    private void initializeDbPath() {
        try {
            // Get user's home directory
            String userHome = System.getProperty("user.home");
            // Construct path to AppData/Local (Windows specific, but generally works cross-platform for app data)
            Path appDataDir = Paths.get(userHome, "AppData", "Local", "PraiseView");

            // Create the directory if it doesn't exist
            if (!Files.exists(appDataDir)) {
                Files.createDirectories(appDataDir);
                AppLogger.log("Created application data directory: " + appDataDir.toAbsolutePath());
            }

            // Construct the full database URL
            Path dbPath = appDataDir.resolve(DB_FILE_NAME);
            DB_URL = "jdbc:sqlite:" + dbPath.toAbsolutePath().toString();
            AppLogger.log("Database URL: " + DB_URL);

        } catch (Exception e) {
            AppLogger.log("Error initializing database path: " + e.getMessage());
            e.printStackTrace();
            // Fallback to current directory if app data path fails
            DB_URL = "jdbc:sqlite:" + DB_FILE_NAME;
            AppLogger.log("Falling back to current directory for database: " + DB_URL);
        }
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
                    FOREIGN KEY(song_id) REFERENCES songs(id) ON DELETE CASCADE
                )"""); // Added ON DELETE CASCADE

            stmt.execute("""
            CREATE TABLE IF NOT EXISTS prayers (
                id TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                content TEXT,
                category TEXT
            )""");

            // New table for texts
            stmt.execute("""
            CREATE TABLE IF NOT EXISTS texts (
                id TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                body TEXT
            )""");

            AppLogger.log("Database tables created or already exist.");

        } catch (Exception e) {
            AppLogger.log("Error creating database tables: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void saveSong(Song song) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            conn.setAutoCommit(false); // Start transaction

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

            // Delete existing verses for this song before inserting new ones
            try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM verses WHERE song_id = ?")) {
                pstmt.setString(1, song.getId());
                pstmt.executeUpdate();
            }

            // Save new verses
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
            conn.commit(); // Commit transaction
            AppLogger.log("Song saved: " + song.getTitle());

        } catch (Exception e) {
            AppLogger.log("Error saving song " + song.getTitle() + ": " + e.getMessage());
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
                song.setCopyright(rs.getString("copyright")); // Ensure copyright is loaded
                
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
                        try {
                            verseOrder.add(Integer.parseInt(num));
                        } catch (NumberFormatException e) {
                            AppLogger.log("Invalid number in verse_order for song " + song.getTitle() + ": " + num);
                        }
                    }
                    song.getVerseOrder().clear();
                    song.getVerseOrder().addAll(verseOrder);
                }
                
                songs.add(song);
            }
            AppLogger.log("Loaded " + songs.size() + " songs from database.");
        } catch (Exception e) {
            AppLogger.log("Error loading all songs: " + e.getMessage());
            e.printStackTrace();
        }
        return songs;
    }

    public void savePrayer(Prayer prayer) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = """
            INSERT OR REPLACE INTO prayers (id, title, content, category)
            VALUES (?, ?, ?, ?)""";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, prayer.getId());
                pstmt.setString(2, prayer.getTitle());
                pstmt.setString(3, prayer.getContent());
                pstmt.setString(4, prayer.getCategory());
                pstmt.executeUpdate();
            }
            AppLogger.log("Prayer saved: " + prayer.getTitle());
        } catch (Exception e) {
            AppLogger.log("Error saving prayer " + prayer.getTitle() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public List<Prayer> loadAllPrayers() {
        List<Prayer> prayers = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery("SELECT * FROM prayers ORDER BY title");
            while (rs.next()) {
                Prayer p = new Prayer();
                p.setId(rs.getString("id"));
                p.setTitle(rs.getString("title"));
                p.setContent(rs.getString("content"));
                p.setCategory(rs.getString("category"));
                prayers.add(p);
            }
            AppLogger.log("Loaded " + prayers.size() + " prayers from database.");
        } catch (Exception e) {
            AppLogger.log("Error loading all prayers: " + e.getMessage());
            e.printStackTrace();
        }
        return prayers;
    }

    public void saveText(TextSlide text) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = """
            INSERT OR REPLACE INTO texts (id, title, body)
            VALUES (?, ?, ?)""";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, text.getId());
                pstmt.setString(2, text.getTitle());
                pstmt.setString(3, text.getContent());
                pstmt.executeUpdate();
            }
            AppLogger.log("Text saved: " + text.getTitle());
        } catch (Exception e) {
            AppLogger.log("Error saving text " + text.getTitle() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public List<TextSlide> loadAllTexts() {
        List<TextSlide> texts = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery("SELECT * FROM texts ORDER BY title");
            while (rs.next()) {
                TextSlide t = new TextSlide();
                t.setId(rs.getString("id"));
                t.setTitle(rs.getString("title"));
                t.setContent(rs.getString("body"));
                texts.add(t);
            }
            AppLogger.log("Loaded " + texts.size() + " texts from database.");
        } catch (Exception e) {
            AppLogger.log("Error loading all texts: " + e.getMessage());
            e.printStackTrace();
        }
        return texts;
    }

    public void deleteSong(String songId) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            conn.setAutoCommit(false); // Start transaction

            // Delete associated verses (ON DELETE CASCADE in table definition handles this automatically)
            // try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM verses WHERE song_id = ?")) {
            //     pstmt.setString(1, songId);
            //     pstmt.executeUpdate();
            // }

            // Delete the song
            try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM songs WHERE id = ?")) {
                pstmt.setString(1, songId);
                pstmt.executeUpdate();
            }
            conn.commit(); // Commit transaction
            AppLogger.log("Song deleted: " + songId);
        } catch (Exception e) {
            AppLogger.log("Error deleting song " + songId + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void deletePrayer(String prayerId) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM prayers WHERE id = ?")) {
                pstmt.setString(1, prayerId);
                pstmt.executeUpdate();
            }
            AppLogger.log("Prayer deleted: " + prayerId);
        } catch (Exception e) {
            AppLogger.log("Error deleting prayer " + prayerId + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void deleteText(String textId) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM texts WHERE id = ?")) {
                pstmt.setString(1, textId);
                pstmt.executeUpdate();
            }
            AppLogger.log("Text deleted: " + textId);
        } catch (Exception e) {
            AppLogger.log("Error deleting text " + textId + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
