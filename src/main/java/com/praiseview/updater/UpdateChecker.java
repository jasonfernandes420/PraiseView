package com.praiseview.updater;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.jar.Manifest;
import java.util.jar.Attributes;

public class UpdateChecker {

    // TODO: IMPORTANT! Replace with your GitHub repository owner and name
    // Example: If your repo is https://github.com/myuser/PraiseView-Full-Project
    // GITHUB_REPO_OWNER = "myuser"
    // GITHUB_REPO_NAME = "PraiseView-Full-Project"
    private static final String GITHUB_REPO_OWNER = "https://github.com/jasonfernandes420/PraiseView";
    private static final String GITHUB_REPO_NAME = "PraiseView"; // This should match your repo name

    private static final String GITHUB_API_LATEST_RELEASE = String.format(
            "https://api.github.com/repos/%s/%s/releases/latest", GITHUB_REPO_OWNER, GITHUB_REPO_NAME);

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Retrieves the current application version from the JAR's MANIFEST.MF.
     * This relies on the maven-jar-plugin configuration in pom.xml.
     * @return An Optional containing the version string, or empty if not found.
     */
    public static Optional<String> getCurrentAppVersion() {
        try {
            URL manifestUrl = UpdateChecker.class.getClassLoader().getResource("META-INF/MANIFEST.MF");
            if (manifestUrl != null) {
                Manifest manifest = new Manifest(manifestUrl.openStream());
                Attributes attributes = manifest.getMainAttributes();
                String version = attributes.getValue("Implementation-Version");
                if (version != null && !version.isEmpty()) {
                    return Optional.of(version);
                }
            }
            // Fallback for development environment (e.g., running from IDE)
            // You might want to read from pom.xml directly or have a default version
            System.out.println("Warning: Could not find Implementation-Version in MANIFEST.MF. Returning DEVELOPMENT_VERSION.");
            return Optional.of("0.0.0"); // Default version for dev or if manifest not found
        } catch (IOException e) {
            System.err.println("Error reading manifest for current version: " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Fetches the latest release information from GitHub API.
     * @return An Optional containing ReleaseInfo, or empty if no release found or error occurred.
     */
    public static Optional<ReleaseInfo> getLatestReleaseInfo() {
        try {
            URL url = new URL(GITHUB_API_LATEST_RELEASE);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
            connection.setRequestProperty("User-Agent", "PraiseView-Updater"); // GitHub API requires User-Agent

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    JsonNode rootNode = objectMapper.readTree(in);

                    String latestVersion = rootNode.path("tag_name").asText(); // e.g., v1.0.1
                    String downloadUrl = null;

                    // Find the MSI asset
                    for (JsonNode asset : rootNode.path("assets")) {
                        if (asset.path("name").asText().endsWith(".msi")) {
                            downloadUrl = asset.path("browser_download_url").asText();
                            break;
                        }
                    }

                    if (latestVersion != null && !latestVersion.isEmpty() && downloadUrl != null && !downloadUrl.isEmpty()) {
                        // Remove 'v' prefix if present for version comparison (e.g., "v1.0.1" -> "1.0.1")
                        if (latestVersion.startsWith("v")) {
                            latestVersion = latestVersion.substring(1);
                        }
                        return Optional.of(new ReleaseInfo(latestVersion, downloadUrl));
                    }
                }
            } else {
                System.err.println("GitHub API request failed with response code: " + responseCode);
                try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()))) {
                    String errorLine;
                    StringBuilder errorResponse = new StringBuilder();
                    while ((errorLine = errorReader.readLine()) != null) {
                        errorResponse.append(errorLine);
                    }
                    System.err.println("Error response: " + errorResponse.toString());
                }
            }
        } catch (IOException e) {
            System.err.println("Error checking for updates from GitHub: " + e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Compares two version strings (e.g., "1.0.0" vs "1.0.1").
     * @param newVersion The version to check against.
     * @param currentVersion The current application version.
     * @return True if newVersion is greater than currentVersion, false otherwise.
     */
    public static boolean isNewerVersion(String newVersion, String currentVersion) {
        String[] newParts = newVersion.split("\\.");
        String[] currentParts = currentVersion.split("\\.");

        int length = Math.max(newParts.length, currentParts.length);
        for (int i = 0; i < length; i++) {
            int newPart = i < newParts.length ? Integer.parseInt(newParts[i]) : 0;
            int currentPart = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;

            if (newPart < currentPart) {
                return false;
            }
            if (newPart > currentPart) {
                return true;
            }
        }
        return false; // Versions are the same
    }

    /**
     * Downloads the installer from the given URL to a temporary location.
     * @param downloadUrl The URL to download from.
     * @param fileName The desired file name for the downloaded installer (e.g., "PraiseView-Installer.msi").
     * @return An Optional containing the Path to the downloaded file, or empty if download failed.
     */
    public static Optional<Path> downloadInstaller(String downloadUrl, String fileName) {
        try {
            URL url = new URL(downloadUrl);
            Path tempDir = Files.createTempDirectory("PraiseView_Updater_");
            Path destination = tempDir.resolve(fileName);

            System.out.println("Downloading update from: " + downloadUrl + " to " + destination);
            try (java.io.InputStream in = url.openStream()) {
                Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
            }
            return Optional.of(destination);
        } catch (IOException e) {
            System.err.println("Error downloading installer: " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Launches the downloaded MSI installer and exits the current application.
     * This method is designed for Windows MSI installers.
     * @param installerPath The Path to the downloaded MSI file.
     */
    public static void launchInstallerAndExit(Path installerPath) {
        try {
            System.out.println("Launching installer: " + installerPath);
            // For Windows MSI, use msiexec with /i for install and /qn for quiet install (no UI)
            // You might want to use /qb for basic UI (progress bar only)
            ProcessBuilder pb = new ProcessBuilder("msiexec", "/i", installerPath.toAbsolutePath().toString(), "/qn");
            pb.start();
            System.out.println("Installer launched. Exiting application.");
            System.exit(0); // Exit the current application
        } catch (IOException e) {
            System.err.println("Error launching installer: " + e.getMessage());
            // Optionally, inform the user that auto-update failed and they need to install manually
        }
    }

    /**
     * Inner class to hold release information fetched from GitHub.
     */
    public static class ReleaseInfo {
        public final String version;
        public final String downloadUrl;

        public ReleaseInfo(String version, String downloadUrl) {
            this.version = version;
            this.downloadUrl = downloadUrl;
        }
    }
}
