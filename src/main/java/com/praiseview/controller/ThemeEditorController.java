package com.praiseview.controller;

import com.praiseview.model.Theme;
import com.praiseview.util.AppLogger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

public class ThemeEditorController {

    @FXML private ListView<Theme> themeListView;
    @FXML private TextField themeNameField;
    @FXML private ComboBox<String> fontFamilyComboBox;
    @FXML private Slider fontSizeSlider;
    @FXML private ColorPicker textColorPicker;
    @FXML private ColorPicker backgroundColorPicker;
    @FXML private TextField backgroundImagePathField;
    @FXML private TextField backgroundVideoPathField;
    @FXML private ComboBox<String> textAlignmentComboBox;
    @FXML private Slider lineSpacingSlider;

    @FXML private Button newThemeButton;
    @FXML private Button deleteThemeButton;
    @FXML private Button applyThemeButton;
    @FXML private Button saveThemeButton;

    private MainController mainController;
    private ObservableList<Theme> themes; // Reference to MainController's themes list
    private Theme selectedTheme;

    @FXML
    public void initialize() {
        // Populate font families (example, can be extended)
        fontFamilyComboBox.setItems(FXCollections.observableArrayList(
                "Arial", "Verdana", "Times New Roman", "Courier New", "Georgia", "Tahoma", "Trebuchet MS", "Impact"
        ));
        fontFamilyComboBox.getSelectionModel().select("Arial"); // Default

        // Populate text alignments
        textAlignmentComboBox.setItems(FXCollections.observableArrayList("LEFT", "CENTER", "RIGHT"));
        textAlignmentComboBox.getSelectionModel().select("CENTER"); // Default

        // Listener for theme selection
        themeListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                selectedTheme = newSelection;
                updateThemeDetails(newSelection);
                setControlsDisable(false); // Enable controls when a theme is selected
            } else {
                selectedTheme = null;
                clearThemeDetails();
                setControlsDisable(true); // Disable controls when no theme is selected
            }
        });

        // Initially disable detail controls
        setControlsDisable(true);
        deleteThemeButton.setDisable(true); // Delete button also disabled initially
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
        this.themes = mainController.getAvailableThemes(); // Get reference to main controller's list
        themeListView.setItems(this.themes);

        // Select the currently active theme if it exists in the list
        if (mainController.getCurrentActiveTheme() != null) {
            themeListView.getSelectionModel().select(mainController.getCurrentActiveTheme());
        } else if (!themes.isEmpty()) {
            themeListView.getSelectionModel().selectFirst();
        }
    }

    private void setControlsDisable(boolean disable) {
        themeNameField.setDisable(disable);
        fontFamilyComboBox.setDisable(disable);
        fontSizeSlider.setDisable(disable);
        textColorPicker.setDisable(disable);
        backgroundColorPicker.setDisable(disable);
        backgroundImagePathField.setDisable(disable);
        backgroundVideoPathField.setDisable(disable);
        textAlignmentComboBox.setDisable(disable);
        lineSpacingSlider.setDisable(disable);
        saveThemeButton.setDisable(disable);
        applyThemeButton.setDisable(disable);
        deleteThemeButton.setDisable(disable || themes.isEmpty()); // Delete button depends on list being non-empty
    }

    private void updateThemeDetails(Theme theme) {
        if (theme != null) {
            themeNameField.setText(theme.getName());
            fontFamilyComboBox.getSelectionModel().select(theme.getFontFamily());
            fontSizeSlider.setValue(theme.getFontSize());
            textColorPicker.setValue(Color.web(theme.getTextColor()));
            backgroundColorPicker.setValue(Color.web(theme.getBackgroundColor()));
            backgroundImagePathField.setText(theme.getBackgroundImagePath());
            backgroundVideoPathField.setText(theme.getBackgroundVideoPath());
            textAlignmentComboBox.getSelectionModel().select(theme.getTextAlignment());
            lineSpacingSlider.setValue(theme.getLineSpacing());
        }
    }

    private void clearThemeDetails() {
        themeNameField.clear();
        fontFamilyComboBox.getSelectionModel().clearSelection();
        fontSizeSlider.setValue(fontSizeSlider.getMin());
        textColorPicker.setValue(Color.WHITE);
        backgroundColorPicker.setValue(Color.BLACK);
        backgroundImagePathField.clear();
        backgroundVideoPathField.clear();
        textAlignmentComboBox.getSelectionModel().clearSelection();
        lineSpacingSlider.setValue(lineSpacingSlider.getMin());
    }

    @FXML
    private void handleNewTheme() {
        TextInputDialog dialog = new TextInputDialog("New Theme");
        dialog.setTitle("New Theme");
        dialog.setHeaderText("Create New Theme");
        dialog.setContentText("Please enter a name for the new theme:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            Theme newTheme = new Theme();
            newTheme.setName(name);
            themes.add(newTheme);
            mainController.saveThemes(); // Persist the new theme list
            themeListView.getSelectionModel().select(newTheme); // Select the new theme
            AppLogger.log("New theme created: " + name);
        });
    }

    @FXML
    private void handleDeleteTheme() {
        if (selectedTheme != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Delete Theme");
            alert.setHeaderText("Delete '" + selectedTheme.getName() + "'?");
            alert.setContentText("Are you sure you want to delete this theme? This action cannot be undone.");

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                themes.remove(selectedTheme);
                mainController.saveThemes(); // Persist the updated theme list
                AppLogger.log("Theme deleted: " + selectedTheme.getName());
                // If the deleted theme was the active one, clear it from main controller
                if (mainController.getCurrentActiveTheme() == selectedTheme) {
                    mainController.setCurrentActiveTheme(null);
                }
            }
        }
    }

    @FXML
    private void handleSaveTheme() {
        if (selectedTheme != null) {
            saveCurrentThemeDetails();
            mainController.saveThemes(); // Persist the updated theme list
            themeListView.refresh(); // Refresh list view to show updated name if changed
            AppLogger.log("Theme saved: " + selectedTheme.getName());
        }
    }

    @FXML
    private void handleApplyTheme() {
        if (selectedTheme != null) {
            saveCurrentThemeDetails(); // Ensure latest changes are applied
            mainController.applyTheme(selectedTheme);
            AppLogger.log("Theme applied: " + selectedTheme.getName());
        }
    }

    private void saveCurrentThemeDetails() {
        if (selectedTheme != null) {
            selectedTheme.setName(themeNameField.getText());
            selectedTheme.setFontFamily(fontFamilyComboBox.getValue());
            selectedTheme.setFontSize(fontSizeSlider.getValue());
            selectedTheme.setTextColor(toHexString(textColorPicker.getValue()));
            selectedTheme.setBackgroundColor(toHexString(backgroundColorPicker.getValue()));
            selectedTheme.setBackgroundImagePath(backgroundImagePathField.getText());
            selectedTheme.setBackgroundVideoPath(backgroundVideoPathField.getText());
            selectedTheme.setTextAlignment(textAlignmentComboBox.getValue());
            selectedTheme.setLineSpacing(lineSpacingSlider.getValue());
        }
    }

    private String toHexString(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }

    @FXML
    private void handleBrowseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Background Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
        );
        File selectedFile = fileChooser.showOpenDialog(getStage());
        if (selectedFile != null) {
            backgroundImagePathField.setText(selectedFile.getAbsolutePath());
            // Clear video path if image is selected
            backgroundVideoPathField.clear();
        }
    }

    @FXML
    private void handleClearImage() {
        backgroundImagePathField.clear();
    }

    @FXML
    private void handleBrowseVideo() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Background Video");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Video Files", "*.mp4", "*.avi", "*.mov", "*.wmv", "*.flv")
        );
        File selectedFile = fileChooser.showOpenDialog(getStage());
        if (selectedFile != null) {
            backgroundVideoPathField.setText(selectedFile.getAbsolutePath());
            // Clear image path if video is selected
            backgroundImagePathField.clear();
        }
    }

    @FXML
    private void handleClearVideo() {
        backgroundVideoPathField.clear();
    }

    private Stage getStage() {
        return (Stage) themeListView.getScene().getWindow();
    }
}
