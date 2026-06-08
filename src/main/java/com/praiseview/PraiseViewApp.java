package com.praiseview;

import com.praiseview.controller.MainController;
import com.praiseview.controller.ProjectionController;
import com.praiseview.service.UpdateService;
import com.praiseview.updater.UpdateChecker;
import javafx.application.Application;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;

public class PraiseViewApp extends Application {

    private static ProjectionController projectionController;
    private UpdateService updateService;
    private Stage primaryStage; // Keep a reference to the primary stage
    private Stage projStage; // Keep a reference to the projection stage

    private static HostServices staticHostServices; // Static field to hold HostServices

    @Override
    public void start(Stage primaryStage) throws IOException {
        this.primaryStage = primaryStage; // Store primary stage reference
        staticHostServices = getHostServices(); // Assign HostServices to the static field

        // 1. Setup projection screen
        setupProjectionScreen();

        // Add handler to close projection stage when primary stage closes
        primaryStage.setOnCloseRequest(event -> {
            if (projStage != null) {
                projStage.close();
            }
            // Also ensure the application exits cleanly, especially if there are background threads
            Platform.exit();
            System.exit(0);
        });


        // Load main UI only after update check is initiated, and potentially shown later
        FXMLLoader mainLoader = new FXMLLoader(getClass().getResource("/com/praiseview/view/main-view.fxml"));
        Scene mainScene = new Scene(mainLoader.load(), 1480, 920);

        primaryStage.setTitle("PraiseView - Operator Control");
        primaryStage.setScene(mainScene);
        primaryStage.setMinWidth(1300);
        primaryStage.setMinHeight(780);
        // primaryStage.show(); // Moved to Platform.runLater in update check or after if no update

        // Set application icon for the primary stage
        try {
            Image icon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/logo-transparent.png")));
            primaryStage.getIcons().add(icon);
        } catch (Exception e) {
            System.err.println("Error loading application icon: " + e.getMessage());
        }

        MainController controller = mainLoader.getController();
        controller.setScene(mainScene);
        controller.setupSceneKeyHandler();

        // Initialize Update Service
        updateService = new UpdateService(getHostServices());

        // --- Update Check Logic ---
        Optional<String> currentVersionOpt = UpdateChecker.getCurrentAppVersion();
        String currentVersion = currentVersionOpt.orElse("0.0.0"); // Default if not found

        System.out.println("Current Application Version: " + currentVersion);

        // Perform update check in a background thread to not block UI startup
        Executors.newSingleThreadExecutor().execute(() -> {
            Optional<UpdateChecker.ReleaseInfo> latestReleaseOpt = UpdateChecker.getLatestReleaseInfo();

            if (latestReleaseOpt.isPresent()) {
                UpdateChecker.ReleaseInfo latestRelease = latestReleaseOpt.get();
                System.out.println("Latest Release Version: " + latestRelease.version);

                if (UpdateChecker.isNewerVersion(latestRelease.version, currentVersion)) {
                    Platform.runLater(() -> {
                        // Show update dialog on the JavaFX Application Thread
                        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                        alert.setTitle("Update Available");
                        alert.setHeaderText("A new version of PraiseView is available!");
                        alert.setContentText("Current version: " + currentVersion + "\n" +
                                           "New version: " + latestRelease.version + "\n\n" +
                                           "Would you like to download and install the update now?");

                        Optional<ButtonType> result = alert.showAndWait();
                        if (result.isPresent() && result.get() == ButtonType.OK) {
                            // User wants to update, proceed with download and install
                            primaryStage.hide(); // Hide main window during update
                            Optional<Path> downloadedInstaller = UpdateChecker.downloadInstaller(
                                latestRelease.downloadUrl, "PraiseView_Installer_" + latestRelease.version + ".msi");

                            if (downloadedInstaller.isPresent()) {
                                UpdateChecker.launchInstallerAndExit(downloadedInstaller.get());
                            } else {
                                // Handle download failure
                                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                                errorAlert.setTitle("Update Failed");
                                errorAlert.setHeaderText("Failed to download update.");
                                errorAlert.setContentText("Please try again later or download manually from GitHub.");
                                errorAlert.showAndWait();
                                primaryStage.show(); // Show main window again if update failed
                            }
                        } else {
                            // User declined update or closed dialog, continue with current version
                            System.out.println("User declined update.");
                            primaryStage.show(); // Ensure primary stage is shown if update check happened before it was shown
                        }
                    });
                } else {
                    Platform.runLater(() -> {
                        System.out.println("You are running the latest version.");
                        primaryStage.show(); // Ensure primary stage is shown
                    });
                }
            } else {
                Platform.runLater(() -> {
                    System.err.println("Could not check for updates. Continuing with current version.");
                    primaryStage.show(); // Ensure primary stage is shown
                });
            }
        });
        // --- End Update Check Logic ---
    }

    private void setupProjectionScreen() {
        try {
            FXMLLoader projLoader =
                    new FXMLLoader(
                            getClass().getResource(
                                    "/com/praiseview/view/projection-view.fxml"));

            Scene projScene = new Scene(projLoader.load());

            projStage = new Stage(); // Assign to class field
            projStage.setTitle("PraiseView - Live Projection");
            projStage.setScene(projScene);

            // Set application icon for the projection stage
            try {
                Image icon = new Image(getClass().getResourceAsStream("/com/praiseview/images/logo-transparent.png"));
                projStage.getIcons().add(icon);
            } catch (Exception e) {
                System.err.println("Error loading projection stage icon: " + e.getMessage());
            }

            var screens = Screen.getScreens();

            if (screens.size() > 1) {
                Screen projector = screens.get(1);

                // Make it undecorated to remove OS window borders/title bar
                projStage.initStyle(StageStyle.UNDECORATED);

                // Set position and size to cover the entire projector screen
                projStage.setX(projector.getBounds().getMinX());
                projStage.setY(projector.getBounds().getMinY());
                projStage.setWidth(projector.getBounds().getWidth());
                projStage.setHeight(projector.getBounds().getHeight());

                // Keep projection window always on top
                projStage.setAlwaysOnTop(true);

                // Removed setFullScreen(true) as we are using undecorated + manual sizing
                projStage.setFullScreenExitHint("");  // Still good to keep, even if not strictly fullScreen
                projStage.setFullScreenExitKeyCombination(javafx.scene.input.KeyCombination.NO_MATCH);  // Disable ESC exit

                System.out.println("✅ Projection started on second monitor (undecorated)");

            } else {
                projStage.setWidth(1280);
                projStage.setHeight(720);
                // For windowed mode, we might still want it undecorated or not, depending on preference.
                // Keeping it decorated for now if only one screen.
                System.out.println("⚠️ Running projection in window mode");
            }

            projStage.show();

            // Request focus on the projection stage
            projStage.toFront();
            projStage.requestFocus();

            projectionController = projLoader.getController();

            System.out.println(
                    "ProjectionController = " + projectionController);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static ProjectionController getProjectionController() {
        return projectionController;
    }

    // New static method to provide HostServices
    public static HostServices getStaticHostServices() {
        return staticHostServices;
    }

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void stop() throws Exception{
        super.stop();
    }
}
