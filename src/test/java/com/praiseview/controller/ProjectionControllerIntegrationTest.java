package com.praiseview.controller;

import com.praiseview.model.Song;
import com.praiseview.model.Theme;
import com.praiseview.model.Verse;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ApplicationExtension.class)
class ProjectionControllerIntegrationTest {

    private ProjectionController controller;

    @Start
    private void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/praiseview/view/projection-view.fxml"));
        Parent root = loader.load();
        controller = loader.getController();
        stage.setScene(new Scene(root, 1280, 720));
        stage.show();
    }

    @Test
    void projectsTheSelectedSongVerseAndTitle(FxRobot robot) {
        robot.interact(() -> {
            Song song = song();
            controller.showItem(song, 1);

            assertSame(song, controller.getCurrentProjectedItem());
            assertEquals(1, controller.getCurrentSubItemIndex());
            assertEquals("Verse 2", controller.getCurrentDisplayedContent());
            assertEquals("Test Song", controller.getCurrentDisplayedTitle());
            assertEquals(2, controller.getCurrentProjectedItemSubItemCount());
        });
    }

    @Test
    void supportsATitleSlideBeforeSongVerses(FxRobot robot) {
        robot.interact(() -> {
            Theme theme = new Theme();
            theme.setShowTitleAsFirstSlide(true);
            controller.applyTheme(theme);
            controller.showItem(song(), 0);

            assertTrue(controller.isCurrentSongTitleSlide());
            assertEquals("", controller.getCurrentDisplayedContent());
            assertEquals(3, controller.getCurrentProjectedItemSubItemCount());

            controller.showItem(controller.getCurrentProjectedItem(), 1);
            assertFalse(controller.isCurrentSongTitleSlide());
            assertEquals("Verse 1", controller.getCurrentDisplayedContent());
        });
    }

    @Test
    void blackoutClearsTheCurrentProjection(FxRobot robot) {
        robot.interact(() -> {
            controller.showItem(song(), 0);
            controller.blackout();

            assertNull(controller.getCurrentProjectedItem());
            assertEquals("", controller.getCurrentDisplayedContent());
            assertTrue(controller.projectionRoot.getStyle().contains("black"));
        });
    }

    @Test
    void clearReturnsTheProjectionToTheLogoState(FxRobot robot) {
        robot.interact(() -> {
            controller.showItem(song(), 0);
            controller.clear();

            assertNull(controller.getCurrentProjectedItem());
            assertEquals("", controller.getCurrentDisplayedContent());
        });
    }

    private Song song() {
        Song song = new Song();
        song.setTitle("Test Song");
        song.setVerses(List.of(
                new Verse("Verse 1", "Verse 1", Verse.VerseType.VERSE),
                new Verse("Verse 2", "Verse 2", Verse.VerseType.VERSE)));
        return song;
    }
}
