package com.praiseview;

import com.praiseview.controller.MainController;
import com.praiseview.controller.ProjectionController;
import com.praiseview.service.UpdateService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle; // Import StageStyle
import javafx.scene.image.Image; // Import Image
import java.io.IOException;
import java.util.Objects;

public class PraiseViewApp extends Application {

    private static ProjectionController projectionController;
    private UpdateService updateService;
    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader mainLoader = new FXMLLoader(getClass().getResource("/com/praiseview/view/main-view.fxml"));
        Scene mainScene = new Scene(mainLoader.load(), 1480, 920);

        primaryStage.setTitle("PraiseView - Operator Control");
        primaryStage.setScene(mainScene);
        primaryStage.setMinWidth(1300);
        primaryStage.setMinHeight(780);
        primaryStage.show();

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

        // Pass HostServices to MainController
        //controller.setHostServices(getHostServices());

        // Initialize Update Service
        updateService = new UpdateService(getHostServices());
        setupProjectionScreen();
    }

    private void setupProjectionScreen() {

    try {

        FXMLLoader projLoader =
                new FXMLLoader(
                        getClass().getResource(
                                "/com/praiseview/view/projection-view.fxml"));

        Scene projScene = new Scene(projLoader.load());

        Stage projStage = new Stage();
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

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void stop() throws Exception{
        super.stop();
    }
}
