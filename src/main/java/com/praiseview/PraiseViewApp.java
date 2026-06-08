package com.praiseview;

import com.praiseview.controller.MainController;
import com.praiseview.controller.ProjectionController;
import com.praiseview.service.UpdateService;
import javafx.application.Application;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.util.Objects;

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
