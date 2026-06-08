package com.praiseview.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.praiseview.util.AppLogger;
import com.praiseview.util.VersionUtil;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class UpdateService {

    private final String CURRENT_VERSION = VersionUtil.getVersion();  // Update this with every release
    private final String GITHUB_REPO = "https://api.github.com/repos/jasonfernandes420/PraiseView/releases/latest";
    private final HostServices hostServices;

    public UpdateService(HostServices hostServices) {
        this.hostServices = hostServices;
    }

    public void checkForUpdate(boolean showNoUpdateMessage) {
        new Thread(() -> {
            try {
                AppLogger.log("Checking for update..");
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(GITHUB_REPO))
                        .header("Accept", "application/vnd.github.v3+json")
                        .timeout(java.time.Duration.ofSeconds(10))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                AppLogger.log("Checking for update. response.:"+response.statusCode());
                if (response.statusCode() == 200) {
                    AppLogger.log("Got something");
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode root = mapper.readTree(response.body());

                    String latestVersion = root.get("tag_name").asText().replace("v", "");
                    String releaseNotes = root.get("body").asText();
                    String downloadUrl = root.get("assets").get(0).get("browser_download_url").asText();

                    if (isNewerVersion(latestVersion)) {
                        AppLogger.log("If update present something");
                        Platform.runLater(() -> showUpdateAvailable(latestVersion, releaseNotes, downloadUrl));
                    } else if (showNoUpdateMessage) {
                        Platform.runLater(() -> {
                            Alert alert = new Alert(Alert.AlertType.INFORMATION);
                            alert.setTitle("No Updates");
                            alert.setHeaderText("You are using the latest version");
                            alert.setContentText("Version " + CURRENT_VERSION + " is up to date.");
                            alert.showAndWait();
                        });
                    }
                }
            } catch (Exception e) {
                if (showNoUpdateMessage) {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Update Check Failed");
                        alert.setContentText("Could not check for updates. Please check your internet connection.");
                        alert.showAndWait();
                    });
                }
            }
        }).start();
    }

    private boolean isNewerVersion(String latest) {
        return latest.compareTo(CURRENT_VERSION) > 0;
    }

    private void showUpdateAvailable(String version, String notes, String downloadUrl) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Update Available");
        alert.setHeaderText("Version " + version + " is available");
        alert.setContentText("Current version: " + CURRENT_VERSION + "\n\nRelease Notes:\n" + notes);

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                hostServices.showDocument(downloadUrl);  // Opens browser to download
            }
        });
    }
}