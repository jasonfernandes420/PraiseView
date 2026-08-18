package com.praiseview.controller;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ApplicationExtension.class)
class MainControllerIntegrationTest {

    private static String originalUserHome;
    private MainController controller;

    @BeforeAll
    static void useTemporaryApplicationDataDirectory() throws Exception {
        originalUserHome = System.getProperty("user.home");
        Path testHome = Files.createTempDirectory("praiseview-main-controller-test-");
        System.setProperty("user.home", testHome.toString());
    }

    @AfterAll
    static void restoreApplicationDataDirectory() {
        System.setProperty("user.home", originalUserHome);
    }

    @Start
    private void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/praiseview/view/main-view.fxml"));
        Parent root = loader.load();
        controller = loader.getController();
        stage.setScene(new Scene(root, 1280, 820));
        stage.show();
    }

    @Test
    void mainWindowInitializesItsThemeAndProjectionStatus(FxRobot robot) {
        robot.interact(() -> {
            assertNotNull(controller.getCurrentActiveTheme());
            assertFalse(controller.getAvailableThemes().isEmpty());

            Label status = robot.lookup("#projectionScreenStatusLabel").queryAs(Label.class);
            assertTrue(status.getText().equals("Projection screen available")
                    || status.getText().equals("No projection screen detected"));
        });
    }

    @Test
    void songSearchFieldAcceptsAndClearsSearchText(FxRobot robot) {
        robot.interact(() -> {
            TextField search = robot.lookup("#searchField").queryAs(TextField.class);
            search.setText("entrance");
            assertEquals("entrance", search.getText());

            search.clear();
            assertTrue(search.getText().isEmpty());
        });
    }
}
