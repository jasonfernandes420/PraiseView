package com.praiseview.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AppLogger {

    private static final String LOG_FILE = "praiseview.log";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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
        try (PrintWriter writer = new PrintWriter(new FileWriter(LOG_FILE, true))) {
            writer.println(logLine);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static void clearLog() {
        new File(LOG_FILE).delete();
    }
}