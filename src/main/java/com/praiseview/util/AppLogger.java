package com.praiseview.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AppLogger {

    private static Path LOG_DIRECTORY;
    private static Path LOG_FILE_PATH;
    private static final String LOG_FILE_NAME = "praiseviewLog.log";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    static {
        initializeLogPath();
    }

    private static void initializeLogPath() {
        try {
            String userHome = System.getProperty("user.home");
            // For Windows, this typically resolves to C:\Users\<username>\AppData\Local\PraiseView
            // For macOS/Linux, it might resolve to /Users/<username>/PraiseView or similar,
            // though AppData is Windows-specific. For cross-platform, a more robust solution
            // might use XDG Base Directory Specification or similar, but for a JavaFX app
            // targeting Windows primarily, AppData/Local is common.
            LOG_DIRECTORY = Paths.get(userHome, "AppData", "Local", "PraiseView");

            if (!Files.exists(LOG_DIRECTORY)) {
                Files.createDirectories(LOG_DIRECTORY);
                System.out.println("Created application log directory: " + LOG_DIRECTORY.toAbsolutePath());
            }
            LOG_FILE_PATH = LOG_DIRECTORY.resolve(LOG_FILE_NAME);
            System.out.println("Log file path: " + LOG_FILE_PATH.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("Error initializing log file path: " + e.getMessage());
            e.printStackTrace();
            // Fallback to current directory if app data path fails
            LOG_FILE_PATH = Paths.get(LOG_FILE_NAME);
            System.err.println("Falling back to current directory for log file: " + LOG_FILE_PATH.toAbsolutePath());
        }
    }

    public static void log(String message) {
        log("INFO", message);
    }

    public static void error(String message, Exception e) {
        log("ERROR", message + " | Exception: " + e.getMessage());
        e.printStackTrace();
    }

    private static void log(String level, String message) {
        String timestamp = LocalDateTime.now().format(formatter);
        String logLine = String.format("[%s] [%s] %s", timestamp, level, message);

        System.out.println(logLine);  // Console

        // Write to file
        try (PrintWriter writer = new PrintWriter(new FileWriter(LOG_FILE_PATH.toFile(), true))) {
            writer.println(logLine);
        } catch (IOException ex) {
            System.err.println("Error writing to log file: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    public static void clearLog() {
        try {
            Files.deleteIfExists(LOG_FILE_PATH);
            System.out.println("Log file cleared: " + LOG_FILE_PATH.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("Error clearing log file: " + e.getMessage());
            e.printStackTrace();
        }
    }
}