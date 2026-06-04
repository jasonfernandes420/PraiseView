package com.praiseview;

import com.praiseview.controller.MainController;
import com.praiseview.controller.ProjectionController;
import com.praiseview.service.UpdateService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import java.io.IOException;

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

        MainController controller = mainLoader.getController();

        // Pass HostServices to MainController
        //controller.setHostServices(getHostServices());

        // Initialize Update Service
        updateService = new UpdateService(getHostServices());
        setupProjectionScreen();
    }

    private void setupProjectionScreen() {
        var screens = Screen.getScreens();

        if (screens.size() > 1) {
            Screen projector = screens.get(1);

            try {
                FXMLLoader projLoader = new FXMLLoader(getClass().getResource("/com/praiseview/view/projection-view.fxml"));
                Scene projScene = new Scene(projLoader.load());

                Stage projStage = new Stage();
                projStage.setTitle("PraiseView - Live Projection");
                projStage.setScene(projScene);

                projStage.setX(projector.getBounds().getMinX());
                projStage.setY(projector.getBounds().getMinY());
                projStage.setWidth(projector.getBounds().getWidth());
                projStage.setHeight(projector.getBounds().getHeight());
                projStage.setFullScreen(true);

                projStage.show();

                projectionController = projLoader.getController();
                System.out.println("✅ Projection successfully started on second monitor!");

            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("⚠️ Only one screen found. Projection running in window mode.");
        }
    }

    public static ProjectionController getProjectionController() {
        return projectionController;
    }

    public static void main(String[] args) {
        launch();
    }
}
