package com.praiseview.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

public class MainController {

    @FXML
    private void showAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About PraiseView");
        alert.setHeaderText("PraiseView " + (com.praiseview.util.VersionUtil.getVersion() != null ? com.praiseview.util.VersionUtil.getVersion() : "1.1.1"));
        
        String content = """
                Modern JavaFX alternative to OpenLP for church projection.
                
                A free and open-source worship projection software built for churches and worship services.
                
                ✨ Current Features:
                • Multi-monitor full-screen projection
                • Song, Prayer & Announcement management
                • Service planner
                • Custom themes (colors, fonts, backgrounds, logos)
                • Media support (Images, Videos, PPT, Background videos)
                • Live preview + navigation controls
                
                🛣️ Future Plans / Roadmap:
                • Smooth Animations & Transitions between slides
                • Mobile App Companion (remote control)
                • AI Helper for automatic slide advancement
                • Improved PowerPoint integration (thumbnails + live control)
                • More import formats (ChordPro, OpenLP, etc.)
                """;

        alert.setContentText(content);
        alert.getDialogPane().setMinWidth(520);
        alert.getDialogPane().setMinHeight(440);

        ButtonType githubButton = new ButtonType("Visit GitHub");
        alert.getButtonTypes().add(githubButton);

        alert.showAndWait().ifPresent(response -> {
            if (response == githubButton) {
                PraiseViewApp.getStaticHostServices().showDocument("https://github.com/jasonfernandes420/PraiseView");
            }
        });
    }

    // TODO: Add other methods from original controller
}