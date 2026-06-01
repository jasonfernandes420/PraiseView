package com.praiseview;

import com.praiseview.controller.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;

public class PraiseViewApp extends Application {

    private static Stage primaryStage;
    private static Stage projectionStage;

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;
        
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/praiseview/view/main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1400, 900);
        scene.getStylesheets().add(getClass().getResource("/com/praiseview/view/dark-theme.css").toExternalForm());
        
        stage.setTitle("PraiseView - Control");
        stage.setScene(scene);
        stage.show();

        setupProjectionScreen();
    }

    private void setupProjectionScreen() {
        var screens = Screen.getScreens();
        if (screens.size() > 1) {
            Screen projectorScreen = screens.get(1);
            projectionStage = new Stage();
            projectionStage.setTitle("PraiseView - Projection");
            
            projectionStage.setX(projectorScreen.getBounds().getMinX());
            projectionStage.setY(projectorScreen.getBounds().getMinY());
            projectionStage.setWidth(projectorScreen.getBounds().getWidth());
            projectionStage.setHeight(projectorScreen.getBounds().getHeight());
            
            // Will be populated by controller later
            projectionStage.show();
            System.out.println("Projection screen activated on second monitor.");
        } else {
            System.out.println("Single screen mode. Projection will open in separate window.");
        }
    }

    public static Stage getPrimaryStage() { return primaryStage; }
    public static Stage getProjectionStage() { return projectionStage; }

    public static void main(String[] args) {
        launch();
    }
}