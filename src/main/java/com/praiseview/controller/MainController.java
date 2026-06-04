package com.praiseview.controller;

import com.praiseview.PraiseViewApp;
import com.praiseview.db.DatabaseService;
import com.praiseview.model.ServiceItem;
import com.praiseview.model.Song;
import com.praiseview.service.UpdateService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.ArrayList;
import java.util.List;

public class MainController {

    // Left Panel - Service Planner
    @FXML private ListView<ServiceItem> servicePlannerList;

    // Center Panel
    @FXML private TextArea itemEditorArea;
    @FXML private StackPane livePreviewPane;
    @FXML private TextFlow livePreviewText;

    // Right Panel
    @FXML private TextFlow nextSlidePreview;
    @FXML private Button blackoutButton, clearButton;
    @FXML private ToggleButton aiToggle;
    @FXML private Slider fontSlider;

    // Bottom Library
    @FXML private ListView<Song> songLibraryList;

    // Toolbar
    @FXML private Button newServiceButton, saveServiceButton, loadServiceButton, exportButton;

    private DatabaseService dbService = new DatabaseService();
    private ObservableList<ServiceItem> serviceQueue = FXCollections.observableArrayList();
    private UpdateService updateService;
    private int currentIndex = -1;

    @FXML
    public void initialize() {
        // Load songs into bottom library
        loadLibrary();

        // Setup Service Planner
        servicePlannerList.setItems(serviceQueue);

        // Drag & Drop from Library to Planner
        setupDragAndDrop();

        // Button Actions
        newServiceButton.setOnAction(e -> newService());
        saveServiceButton.setOnAction(e -> saveService());
        blackoutButton.setOnAction(e -> blackout());
        clearButton.setOnAction(e -> clearScreen());

        // Selection change in service planner
        servicePlannerList.getSelectionModel().selectedIndexProperty().addListener((obs, old, newVal) -> {
            if (newVal.intValue() >= 0) {
                currentIndex = newVal.intValue();
                updateLivePreview();
                updateNextPreview();
            }
        });

        // Font slider listener
        fontSlider.valueProperty().addListener((obs, old, newVal) -> {
            ProjectionController proj = PraiseViewApp.getProjectionController();
            if (proj != null) {
                proj.setFontSize(newVal.doubleValue());
            }
        });


    }
    // Add this field
    private javafx.application.HostServices hostServices;

    // Add this setter method
    public void setHostServices(javafx.application.HostServices hostServices) {
        this.hostServices = hostServices;
    }
    // Add a menu item or button for manual check:
    @FXML
    private void checkForUpdates() {
        updateService.checkForUpdate(true);
    }

    private void loadLibrary() {
        songLibraryList.getItems().setAll(dbService.loadAllSongs());
    }

    private void setupDragAndDrop() {
        songLibraryList.setOnDragDetected(e -> {
            Song selected = songLibraryList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                Dragboard db = songLibraryList.startDragAndDrop(TransferMode.COPY);
                ClipboardContent content = new ClipboardContent();
                content.putString(selected.getId());
                db.setContent(content);
                e.consume();
            }
        });

        servicePlannerList.setOnDragOver(e -> {
            if (e.getDragboard().hasString()) {
                e.acceptTransferModes(TransferMode.COPY);
            }
        });

        servicePlannerList.setOnDragDropped(e -> {
            Song song = songLibraryList.getSelectionModel().getSelectedItem();
            if (song != null) {
                serviceQueue.add(new ServiceItem(song));
                e.setDropCompleted(true);
            }
        });
    }

    private void newService() {
        serviceQueue.clear();
        currentIndex = -1;
        itemEditorArea.clear();
        System.out.println("New service created");
    }

    private void saveService() {
        // TODO: Save to file / database
        System.out.println("Service saved");
    }

    private void updateLivePreview() {
        if (currentIndex < 0 || currentIndex >= serviceQueue.size()) return;

        ServiceItem item = serviceQueue.get(currentIndex);
        livePreviewText.getChildren().clear();

        Text title = new Text(item.getSong().getTitle() + "\n\n");
        title.setStyle("-fx-font-size: 22px; -fx-fill: #ffd700;");

        Text content = new Text(item.getSong().getVerseAtPosition(0).getContent());
        content.setStyle("-fx-font-size: 18px; -fx-fill: white;");

        livePreviewText.getChildren().addAll(title, content);

        // Send to actual projection
        ProjectionController proj = PraiseViewApp.getProjectionController();
        if (proj != null) {
            proj.showSlide(item.getSong(), 0);
        }
    }

    private void updateNextPreview() {
        nextSlidePreview.getChildren().clear();
        if (currentIndex + 1 < serviceQueue.size()) {
            ServiceItem next = serviceQueue.get(currentIndex + 1);
            Text text = new Text(next.getSong().getTitle());
            text.setStyle("-fx-fill: #aaaaaa; -fx-font-size: 16px;");
            nextSlidePreview.getChildren().add(text);
        }
    }

    private void blackout() {
        ProjectionController proj = PraiseViewApp.getProjectionController();
        if (proj != null) proj.blackout();
    }

    private void clearScreen() {
        ProjectionController proj = PraiseViewApp.getProjectionController();
        if (proj != null) proj.clear();
    }
}