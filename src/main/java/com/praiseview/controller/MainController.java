package com.praiseview.controller;

import com.praiseview.PraiseViewApp;
import com.praiseview.db.DatabaseService;
import com.praiseview.model.Song;
import com.praiseview.ai.AutoAdvanceService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.stage.FileChooser;
import java.io.File;
import java.util.List;

public class MainController {

    @FXML private ListView<Song> songListView;
    @FXML private ListView<Song> serviceListView;
    @FXML private ToggleButton aiToggle;
    @FXML private Button addSongButton, projectButton, importButton, exportButton;
    @FXML private Slider fontSlider;

    private DatabaseService dbService = new DatabaseService();
    //private JsonService jsonService = new JsonService();
    private AutoAdvanceService aiService = new AutoAdvanceService(null);
    private ProjectionController projectionController;

    @FXML
    public void initialize() {
        loadSongs();

        // Drag & Drop Support
        setupDragAndDrop();

        addSongButton.setOnAction(e -> openSongEditor(null));
        projectButton.setOnAction(e -> startProjection());

        aiToggle.setOnAction(e -> {
            Song current = serviceListView.getSelectionModel().getSelectedItem();
            if (current != null) {
                aiService.toggle(aiToggle.isSelected(), current);
            }
        });

        // Manual override - disable AI when verse is selected
        serviceListView.setOnMouseClicked(e -> {
            if (aiToggle.isSelected()) {
                aiService.manualAdvance(serviceListView.getSelectionModel().getSelectedIndex());
                aiToggle.setSelected(false);
            }
        });
    }

    private void loadSongs() {
        songListView.getItems().setAll(dbService.loadAllSongs());
    }

    private void openSongEditor(Song song) {
        SongEditorDialog dialog = new SongEditorDialog(song);
        dialog.showAndWait().ifPresent(result -> {
            dbService.saveSong(result);
            loadSongs();
        });
    }

    private void setupDragAndDrop() {
        serviceListView.setOnDragOver(e -> {
            if (e.getGestureSource() != serviceListView && e.getDragboard().hasString()) {
                e.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
        });

        serviceListView.setOnDragDropped(e -> {
            Dragboard db = e.getDragboard();
            if (db.hasString()) {
                // Add logic to move song to service list
                e.setDropCompleted(true);
            }
        });
    }

    private void startProjection() {
        // Open projection window (already handled in PraiseViewApp)
        System.out.println("Projection Started");
    }

    @FXML
    private void importSongs() {
        FileChooser fc = new FileChooser();
        File file = fc.showOpenDialog(null);
        if (file != null) {
            // jsonService.importSongs(file);
            System.out.println("Imported songs");
        }
    }

    @FXML
    private void exportSongs() {
        FileChooser fc = new FileChooser();
        File file = fc.showSaveDialog(null);
        if (file != null) {
            // jsonService.exportSongs(songListView.getItems(), file);
            System.out.println("Exported songs");
        }
    }
}