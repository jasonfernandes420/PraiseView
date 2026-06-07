package com.praiseview.controller;

import com.praiseview.model.Theme;
import com.praiseview.util.AppLogger;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.Optional;

public class ThemeEditorController {

    @FXML private ListView<Theme> themeListView;
    @FXML private TextField themeNameField;
    @FXML private ComboBox<String> fontFamilyComboBox; // Changed from TextField to ComboBox
    @FXML private Slider fontSizeSlider;
    @FXML private ColorPicker textColorPicker;
    @FXML private ColorPicker backgroundColorPicker;
    @FXML private TextField backgroundImagePathField;
    @FXML private TextField backgroundVideoPathField;
    @FXML private ComboBox<String> textAlignmentComboBox;
    @FXML private Slider lineSpacingSlider;
    @FXML private CheckBox showTitleCheckBox;

    // New FXML elements for Title Font Settings
    @FXML private VBox titleSettingsContainer;
    @FXML private ComboBox<String> titleFontFamilyComboBox;
    @FXML private Slider titleFontSizeSlider;
    @FXML private ColorPicker titleTextColorPicker;


    @FXML private Button newThemeButton;
    @FXML private Button deleteThemeButton;
    @FXML private Button saveThemeButton;
    @FXML private Button applyThemeButton;

    private MainController mainController;
    private Theme selectedTheme; // The theme currently selected in the ListView and being edited

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
        themeListView.setItems(mainController.getAvailableThemes());
        if (!mainController.getAvailableThemes().isEmpty()) {
            // Select the currently active theme in the editor
            Theme activeTheme = mainController.getCurrentActiveTheme();
            if (activeTheme != null) {
                themeListView.getSelectionModel().select(activeTheme);
                selectedTheme = activeTheme;
                loadThemeProperties(selectedTheme);
            } else {
                themeListView.getSelectionModel().selectFirst();
                selectedTheme = themeListView.getSelectionModel().getSelectedItem();
                loadThemeProperties(selectedTheme);
            }
        }
        updateButtonStates();
    }

    @FXML
    public void initialize() {
        // Initialize font family combo box with system fonts
        fontFamilyComboBox.setItems(FXCollections.observableArrayList(Font.getFamilies()));
        fontFamilyComboBox.getSelectionModel().select("Arial"); // Default selection

        // Initialize text alignment combo box
        textAlignmentComboBox.setItems(FXCollections.observableArrayList("LEFT", "CENTER", "RIGHT"));
        textAlignmentComboBox.getSelectionModel().select("CENTER"); // Default selection

        // Initialize title font family combo box with system fonts
        titleFontFamilyComboBox.setItems(FXCollections.observableArrayList(Font.getFamilies()));
        titleFontFamilyComboBox.getSelectionModel().select("Arial"); // Default selection

        // Bind visibility of title settings to showTitleCheckBox
        titleSettingsContainer.visibleProperty().bind(showTitleCheckBox.selectedProperty());
        titleSettingsContainer.managedProperty().bind(showTitleCheckBox.selectedProperty());

        // Listener for theme selection changes
        themeListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                selectedTheme = newSelection;
                loadThemeProperties(newSelection);
            } else {
                clearThemeProperties();
                selectedTheme = null;
            }
            updateButtonStates();
        });

        // Add listeners to update selectedTheme properties dynamically (optional, but good for live preview)
        // For now, we'll rely on Save/Apply buttons to commit changes.
        // If we want live updates in the editor's preview, these listeners would update a temporary Theme object.
    }

    private void loadThemeProperties(Theme theme) {
        if (theme != null) {
            themeNameField.setText(theme.getName());
            fontFamilyComboBox.getSelectionModel().select(theme.getFontFamily()); // Updated for ComboBox
            fontSizeSlider.setValue(theme.getFontSize());

            // Text Color
            String currentTextColor = theme.getTextColor();
            try {
                textColorPicker.setValue(currentTextColor != null && !currentTextColor.isEmpty() ? Color.web(currentTextColor) : Color.WHITE);
            } catch (IllegalArgumentException | NullPointerException e) { // Catch NPE too
                AppLogger.log("Invalid or null text color in theme: '" + currentTextColor + "'. Falling back to WHITE. Error: " + e.getMessage());
                textColorPicker.setValue(Color.WHITE);
            }

            // Background Color
            String currentBackgroundColor = theme.getBackgroundColor();
            try {
                backgroundColorPicker.setValue(currentBackgroundColor != null && !currentBackgroundColor.isEmpty() ? Color.web(currentBackgroundColor) : Color.BLACK);
            } catch (IllegalArgumentException | NullPointerException e) { // Catch NPE too
                AppLogger.log("Invalid or null background color in theme: '" + currentBackgroundColor + "'. Falling back to BLACK. Error: " + e.getMessage());
                backgroundColorPicker.setValue(Color.BLACK);
            }

            backgroundImagePathField.setText(theme.getBackgroundImagePath() != null ? theme.getBackgroundImagePath() : "");
            backgroundVideoPathField.setText(theme.getBackgroundVideoPath() != null ? theme.getBackgroundVideoPath() : "");
            textAlignmentComboBox.getSelectionModel().select(theme.getTextAlignment());
            lineSpacingSlider.setValue(theme.getLineSpacing());
            showTitleCheckBox.setSelected(theme.isShowTitle());

            // Title Font Settings
            titleFontFamilyComboBox.getSelectionModel().select(theme.getTitleFontFamily());
            titleFontSizeSlider.setValue(theme.getTitleFontSize());

            // Title Text Color
            String currentTitleTextColor = theme.getTitleTextColor();
            try {
                titleTextColorPicker.setValue(currentTitleTextColor != null && !currentTitleTextColor.isEmpty() ? Color.web(currentTitleTextColor) : Color.GOLD);
            } catch (IllegalArgumentException | NullPointerException e) { // Catch NPE too
                AppLogger.log("Invalid or null title text color in theme: '" + currentTitleTextColor + "'. Falling back to GOLD. Error: " + e.getMessage());
                titleTextColorPicker.setValue(Color.GOLD);
            }

        } else {
            clearThemeProperties();
        }
    }

    private void clearThemeProperties() {
        themeNameField.clear();
        fontFamilyComboBox.getSelectionModel().select("Arial"); // Default font family
        fontSizeSlider.setValue(62);
        textColorPicker.setValue(Color.WHITE);
        backgroundColorPicker.setValue(Color.BLACK);
        backgroundImagePathField.clear();
        backgroundVideoPathField.clear();
        textAlignmentComboBox.getSelectionModel().select("CENTER");
        lineSpacingSlider.setValue(8);
        showTitleCheckBox.setSelected(true);

        // Clear Title Font Settings
        titleFontFamilyComboBox.getSelectionModel().select("Arial");
        titleFontSizeSlider.setValue(42);
        titleTextColorPicker.setValue(Color.web("#FFD700"));
    }

    private void updateThemeFromInputs(Theme theme) {
        if (theme != null) {
            theme.setName(themeNameField.getText());
            theme.setFontFamily(fontFamilyComboBox.getSelectionModel().getSelectedItem()); // Updated for ComboBox
            theme.setFontSize(fontSizeSlider.getValue());
            theme.setTextColor(toHexString(textColorPicker.getValue()));
            theme.setBackgroundColor(toHexString(backgroundColorPicker.getValue()));
            theme.setBackgroundImagePath(backgroundImagePathField.getText().isEmpty() ? null : backgroundImagePathField.getText());
            theme.setBackgroundVideoPath(backgroundVideoPathField.getText().isEmpty() ? null : backgroundVideoPathField.getText());
            theme.setTextAlignment(textAlignmentComboBox.getSelectionModel().getSelectedItem());
            theme.setLineSpacing(lineSpacingSlider.getValue());
            theme.setShowTitle(showTitleCheckBox.isSelected());

            // Update Title Font Settings
            theme.setTitleFontFamily(titleFontFamilyComboBox.getSelectionModel().getSelectedItem());
            theme.setTitleFontSize(titleFontSizeSlider.getValue());
            theme.setTitleTextColor(toHexString(titleTextColorPicker.getValue()));
        }
    }

    @FXML
    private void handleNewTheme() {
        TextInputDialog dialog = new TextInputDialog("New Theme");
        dialog.setTitle("New Theme");
        dialog.setHeaderText("Create New Theme");
        dialog.setContentText("Please enter the name for the new theme:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                Theme newTheme = new Theme();
                newTheme.setName(name.trim());
                mainController.getAvailableThemes().add(newTheme);
                themeListView.getSelectionModel().select(newTheme);
                mainController.saveThemes(); // Save themes after adding a new one
                themeListView.refresh(); // Refresh to show new theme with generated preview
                AppLogger.log("New theme created: " + name);
            }
        });
    }

    @FXML
    private void handleDeleteTheme() {
        Theme themeToDelete = themeListView.getSelectionModel().getSelectedItem();
        if (themeToDelete != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Delete Theme");
            alert.setHeaderText("Delete '" + themeToDelete.getName() + "'?");
            alert.setContentText("Are you sure you want to delete this theme? This action cannot be undone.");

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                mainController.getAvailableThemes().remove(themeToDelete);
                mainController.saveThemes(); // Save themes after deletion
                AppLogger.log("Theme deleted: " + themeToDelete.getName());
                // If the deleted theme was the active one, apply a default or first available
                if (mainController.getCurrentActiveTheme() == themeToDelete) {
                    if (!mainController.getAvailableThemes().isEmpty()) {
                        mainController.applyTheme(mainController.getAvailableThemes().get(0));
                    } else {
                        // Handle case where no themes are left (e.g., create a new default)
                        // For now, just clear the main view's theme
                        mainController.setCurrentActiveTheme(null);
                    }
                }
            }
        }
    }

    @FXML
    private void handleSaveTheme() {
        if (selectedTheme != null) {
            updateThemeFromInputs(selectedTheme);
            mainController.saveThemes(); // Persist changes to file
            themeListView.refresh(); // Refresh the list view to show updated name and generated preview
            AppLogger.log("Theme saved: " + selectedTheme.getName());
        }
    }

    @FXML
    private void handleApplyTheme() {
        if (selectedTheme != null) {
            updateThemeFromInputs(selectedTheme); // Ensure latest changes are applied
            mainController.applyTheme(selectedTheme); // Apply to main application
            mainController.saveThemes(); // Save the theme as it's now the active one
            themeListView.refresh(); // Refresh to show updated generated preview
            AppLogger.log("Theme applied: " + selectedTheme.getName());
        }
    }

    @FXML
    private void handleSelectBackgroundImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Background Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        File selectedFile = fileChooser.showOpenDialog(getStage());
        if (selectedFile != null) {
            backgroundImagePathField.setText(selectedFile.getAbsolutePath());
        }
    }

    @FXML
    private void handleClearBackgroundImage() {
        backgroundImagePathField.clear();
        if (selectedTheme != null) {
            selectedTheme.setBackgroundImagePath(null);
        }
        // Re-apply theme to update preview/projection if this was the active theme
        if (selectedTheme == mainController.getCurrentActiveTheme()) {
            mainController.applyTheme(selectedTheme);
        }
    }

    @FXML
    private void handleSelectBackgroundVideo() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Background Video");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Video Files", "*.mp4", "*.avi", "*.mov", "*.wmv", "*.flv"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        File selectedFile = fileChooser.showOpenDialog(getStage());
        if (selectedFile != null) {
            backgroundVideoPathField.setText(selectedFile.getAbsolutePath());
        }
    }

    @FXML
    private void handleClearBackgroundVideo() {
        backgroundVideoPathField.clear();
        if (selectedTheme != null) {
            selectedTheme.setBackgroundVideoPath(null);
        }
        // Re-apply theme to update preview/projection if this was the active theme
        if (selectedTheme == mainController.getCurrentActiveTheme()) {
            mainController.applyTheme(selectedTheme);
        }
    }

    private void updateButtonStates() {
        boolean isThemeSelected = selectedTheme != null;
        deleteThemeButton.setDisable(!isThemeSelected);
        saveThemeButton.setDisable(!isThemeSelected);
        applyThemeButton.setDisable(!isThemeSelected);
    }

    private String toHexString(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }

    private Stage getStage() {
        return (Stage) themeListView.getScene().getWindow();
    }
}
