package com.praiseview.controller;

import com.praiseview.model.Theme;
import com.praiseview.util.AppLogger;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.Optional;

public class ThemeEditorWizardController {

    // Theme Selection
    @FXML private ComboBox<Theme> themeSelectionComboBox;
    @FXML private Button selectThemeButton;
    @FXML private Button newThemeButton;
    @FXML private Button deleteThemeButton;

    // Step containers
    @FXML private StackPane mainStackPane;
    @FXML private VBox themeSelectionContainer;
    @FXML private VBox stepsContainer;
    @FXML private StackPane stepsStackPane;

    // Step 1: Background
    @FXML private ScrollPane step1Container;
    @FXML private ColorPicker backgroundColorPicker;
    @FXML private TextField backgroundImagePathField;
    @FXML private TextField backgroundVideoPathField;

    // Step 2: Main Text
    @FXML private ScrollPane step2Container;
    @FXML private ComboBox<String> fontFamilyComboBox;
    @FXML private Slider fontSizeSlider;
    @FXML private Label fontSizeLabel;
    @FXML private ColorPicker textColorPicker;
    @FXML private ComboBox<String> textAlignmentComboBox;
    @FXML private Slider lineSpacingSlider;
    @FXML private Label lineSpacingLabel;

    // Step 3: Title
    @FXML private ScrollPane step3Container;
    @FXML private CheckBox showTitleCheckBox;
    @FXML private CheckBox showTitleAsFirstSlideCheckBox;
    @FXML private ComboBox<String> titleFontFamilyComboBox;
    @FXML private Slider titleFontSizeSlider;
    @FXML private Label titleFontSizeLabel;
    @FXML private ColorPicker titleTextColorPicker;
    @FXML private GridPane titleSettingsGrid;

    // Step 4: Preview
    @FXML private VBox step4Container;
    @FXML private Canvas previewCanvas;

    // Navigation
    @FXML private Label currentStepLabel;
    @FXML private Label themeNameLabel;
    @FXML private Button prevButton;
    @FXML private Button nextButton;
    @FXML private Button finishButton;

    private int currentStep = 0;
    private final int totalSteps = 4;

    private MainController mainController;
    private Theme selectedTheme;

    private static final String PREVIEW_TEXT = "Amazing grace! how sweet the sound,\n" +
            "  That saved a wretch; like me!\n" +
            "I once was lost, but now am found,\n" +
            "  Was blind, but now I see.";

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
        themeSelectionComboBox.setItems(mainController.getAvailableThemes());
        if (!mainController.getAvailableThemes().isEmpty()) {
            themeSelectionComboBox.getSelectionModel().selectFirst();
        }
    }

    @FXML
    public void initialize() {
        // Initialize font combos
        fontFamilyComboBox.setItems(FXCollections.observableArrayList(Font.getFamilies()));
        fontFamilyComboBox.getSelectionModel().select("Arial");

        titleFontFamilyComboBox.setItems(FXCollections.observableArrayList(Font.getFamilies()));
        titleFontFamilyComboBox.getSelectionModel().select("Arial");

        // Initialize text alignment
        textAlignmentComboBox.setItems(FXCollections.observableArrayList("LEFT", "CENTER", "RIGHT"));
        textAlignmentComboBox.getSelectionModel().select("CENTER");

        // Initialize canvas
        previewCanvas.setWidth(500);
        previewCanvas.setHeight(300);

        // Bind title settings visibility
        titleSettingsGrid.visibleProperty().bind(
                showTitleCheckBox.selectedProperty().or(showTitleAsFirstSlideCheckBox.selectedProperty())
        );

        // Mutually exclusive checkboxes
        showTitleCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) showTitleAsFirstSlideCheckBox.setSelected(false);
        });
        showTitleAsFirstSlideCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) showTitleCheckBox.setSelected(false);
        });

        // Only add slider label updates, NO preview updates here
        fontSizeSlider.valueProperty().addListener((obs, old, newVal) -> {
            fontSizeLabel.setText(String.format("%.0f", newVal.doubleValue()));
        });
        lineSpacingSlider.valueProperty().addListener((obs, old, newVal) -> {
            lineSpacingLabel.setText(String.format("%.0f", newVal.doubleValue()));
        });
        titleFontSizeSlider.valueProperty().addListener((obs, old, newVal) -> {
            titleFontSizeLabel.setText(String.format("%.0f", newVal.doubleValue()));
        });

        // Start with theme selection
        showThemeSelection();
    }

    private void showThemeSelection() {
        mainStackPane.getChildren().forEach(child -> {
            child.setVisible(false);
            child.setManaged(false);
        });
        themeSelectionContainer.setVisible(true);
        themeSelectionContainer.setManaged(true);
    }

    private void showStep(int step) {
        if (step == 0) {
            showThemeSelection();
            return;
        }

        stepsStackPane.getChildren().forEach(child -> {
            child.setVisible(false);
            child.setManaged(false);
        });

        switch (step - 1) {
            case 0:
                step1Container.setVisible(true);
                step1Container.setManaged(true);
                currentStepLabel.setText("Background Settings");
                break;
            case 1:
                step2Container.setVisible(true);
                step2Container.setManaged(true);
                currentStepLabel.setText("Main Text Settings");
                break;
            case 2:
                step3Container.setVisible(true);
                step3Container.setManaged(true);
                currentStepLabel.setText("Title Settings");
                break;
            case 3:
                step4Container.setVisible(true);
                step4Container.setManaged(true);
                currentStepLabel.setText("Preview");
                updatePreview();
                break;
        }

        currentStep = step;
        updateButtonStates();
    }

    private void updateButtonStates() {
        prevButton.setDisable(currentStep <= 1);
        nextButton.setVisible(currentStep < totalSteps);
        nextButton.setManaged(currentStep < totalSteps);
        finishButton.setVisible(currentStep == totalSteps);
        finishButton.setManaged(currentStep == totalSteps);
    }

    private void updatePreview() {
        AppLogger.log("updatePreview called, currentStep=" + currentStep + ", totalSteps=" + totalSteps);
        
        if (currentStep != totalSteps - 1) {
            AppLogger.log("Skipping preview - not on step 4");
            return;
        }

        AppLogger.log("Drawing preview on canvas");
        GraphicsContext gc = previewCanvas.getGraphicsContext2D();
        double width = previewCanvas.getWidth();
        double height = previewCanvas.getHeight();

        AppLogger.log("Canvas size: " + width + "x" + height);

        // Draw background
        Color bgColor = backgroundColorPicker.getValue();
        gc.setFill(bgColor);
        gc.fillRect(0, 0, width, height);
        AppLogger.log("Filled background: " + bgColor);

        // Try to draw background image if set
        String imagePath = backgroundImagePathField.getText();
        if (imagePath != null && !imagePath.isEmpty()) {
            try {
                Image image = new Image("file:" + imagePath);
                gc.drawImage(image, 0, 0, width, height);
                AppLogger.log("Drew background image from: " + imagePath);
            } catch (Exception e) {
                AppLogger.log("Error loading preview image: " + e.getMessage());
            }
        }

        // Prepare text settings
        Color textColor = textColorPicker.getValue();
        String fontFamily = fontFamilyComboBox.getSelectionModel().getSelectedItem();
        double fontSize = fontSizeSlider.getValue();
        String alignment = textAlignmentComboBox.getSelectionModel().getSelectedItem();
        double lineSpacing = lineSpacingSlider.getValue();

        // Calculate scaling factor based on canvas size (reference size was 800x450)
        final double REFERENCE_WIDTH = 800;
        final double REFERENCE_HEIGHT = 450;
        double scaleX = width / REFERENCE_WIDTH;
        double scaleY = height / REFERENCE_HEIGHT;
        double scaleFactor = Math.min(scaleX, scaleY) * 0.7; // 0.9 to add some margin
        
        // Scale font sizes based on canvas
        int scaledFontSize = Math.max(8, (int) (fontSize * scaleFactor));
        
        AppLogger.log("Scale factor: " + scaleFactor + ", Original font size: " + fontSize + ", Scaled: " + scaledFontSize);
        AppLogger.log("Text settings - Font: " + fontFamily + ", Size: " + scaledFontSize + ", Color: " + textColor);

        gc.setFill(textColor);
        gc.setFont(new Font(fontFamily, scaledFontSize));

        // Draw title if enabled
        if (showTitleCheckBox.isSelected() || showTitleAsFirstSlideCheckBox.isSelected()) {
            Color titleColor = titleTextColorPicker.getValue();
            String titleFont = titleFontFamilyComboBox.getSelectionModel().getSelectedItem();
            double titleSize = titleFontSizeSlider.getValue();
            int scaledTitleSize = Math.max(6, (int) (titleSize * scaleFactor));

            gc.setFill(titleColor);
            gc.setFont(new Font(titleFont, scaledTitleSize));
            
            double x = 20;
            if ("CENTER".equals(alignment)) {
                x = width / 2;
            } else if ("RIGHT".equals(alignment)) {
                x = width - 20;
            }
            
            gc.setTextAlign(textAlignmentFromString(alignment));
            gc.fillText("Amazing Grace", x, 25);
            AppLogger.log("Drew title at y=25, scaled size: " + scaledTitleSize);
        }

        // Draw main text
        gc.setFill(textColor);
        gc.setFont(new Font(fontFamily, scaledFontSize));

        // Split text into lines and draw
        String[] lines = PREVIEW_TEXT.split("\n");
        double yOffset = showTitleCheckBox.isSelected() || showTitleAsFirstSlideCheckBox.isSelected() ? 50 : 25;
        double scaledLineSpacing = lineSpacing * scaleFactor;

        int lineCount = 0;
        for (String line : lines) {
            if (yOffset > height - 10) break;

            double x = 20;
            if ("CENTER".equals(alignment)) {
                x = width / 2;
            } else if ("RIGHT".equals(alignment)) {
                x = width - 20;
            }

            gc.setTextAlign(textAlignmentFromString(alignment));
            gc.fillText(line, x, yOffset);
            yOffset += scaledFontSize + scaledLineSpacing;
            lineCount++;
        }
        
        AppLogger.log("Drew " + lineCount + " lines of text");
    }

    private javafx.scene.text.TextAlignment textAlignmentFromString(String alignment) {
        if ("LEFT".equals(alignment)) return javafx.scene.text.TextAlignment.LEFT;
        if ("RIGHT".equals(alignment)) return javafx.scene.text.TextAlignment.RIGHT;
        return javafx.scene.text.TextAlignment.CENTER;
    }

    private void loadThemeProperties(Theme theme) {
        if (theme == null) return;

        themeNameLabel.setText("Theme: " + theme.getName());

        try {
            backgroundColorPicker.setValue(
                    theme.getBackgroundColor() != null && !theme.getBackgroundColor().isEmpty()
                            ? Color.web(theme.getBackgroundColor())
                            : Color.BLACK
            );
        } catch (Exception e) {
            backgroundColorPicker.setValue(Color.BLACK);
        }

        backgroundImagePathField.setText(theme.getBackgroundImagePath() != null ? theme.getBackgroundImagePath() : "");
        backgroundVideoPathField.setText(theme.getBackgroundVideoPath() != null ? theme.getBackgroundVideoPath() : "");

        fontFamilyComboBox.getSelectionModel().select(theme.getFontFamily());
        fontSizeSlider.setValue(theme.getFontSize());
        fontSizeLabel.setText(String.format("%.0f", theme.getFontSize()));

        try {
            textColorPicker.setValue(
                    theme.getTextColor() != null && !theme.getTextColor().isEmpty()
                            ? Color.web(theme.getTextColor())
                            : Color.WHITE
            );
        } catch (Exception e) {
            textColorPicker.setValue(Color.WHITE);
        }

        textAlignmentComboBox.getSelectionModel().select(theme.getTextAlignment());
        lineSpacingSlider.setValue(theme.getLineSpacing());
        lineSpacingLabel.setText(String.format("%.0f", theme.getLineSpacing()));

        showTitleCheckBox.setSelected(theme.isShowTitle());
        showTitleAsFirstSlideCheckBox.setSelected(theme.isShowTitleAsFirstSlide());

        titleFontFamilyComboBox.getSelectionModel().select(theme.getTitleFontFamily());
        titleFontSizeSlider.setValue(theme.getTitleFontSize());
        titleFontSizeLabel.setText(String.format("%.0f", theme.getTitleFontSize()));

        try {
            titleTextColorPicker.setValue(
                    theme.getTitleTextColor() != null && !theme.getTitleTextColor().isEmpty()
                            ? Color.web(theme.getTitleTextColor())
                            : Color.GOLD
            );
        } catch (Exception e) {
            titleTextColorPicker.setValue(Color.GOLD);
        }
    }

    private void updateThemeFromInputs(Theme theme) {
        if (theme == null) return;

        theme.setBackgroundColor(toHexString(backgroundColorPicker.getValue()));
        theme.setBackgroundImagePath(backgroundImagePathField.getText().isEmpty() ? null : backgroundImagePathField.getText());
        theme.setBackgroundVideoPath(backgroundVideoPathField.getText().isEmpty() ? null : backgroundVideoPathField.getText());

        theme.setFontFamily(fontFamilyComboBox.getSelectionModel().getSelectedItem());
        theme.setFontSize(fontSizeSlider.getValue());
        theme.setTextColor(toHexString(textColorPicker.getValue()));
        theme.setTextAlignment(textAlignmentComboBox.getSelectionModel().getSelectedItem());
        theme.setLineSpacing(lineSpacingSlider.getValue());

        theme.setShowTitle(showTitleCheckBox.isSelected());
        theme.setShowTitleAsFirstSlide(showTitleAsFirstSlideCheckBox.isSelected());
        theme.setTitleFontFamily(titleFontFamilyComboBox.getSelectionModel().getSelectedItem());
        theme.setTitleFontSize(titleFontSizeSlider.getValue());
        theme.setTitleTextColor(toHexString(titleTextColorPicker.getValue()));
    }

    @FXML
    private void handleSelectTheme() {
        selectedTheme = themeSelectionComboBox.getSelectionModel().getSelectedItem();
        if (selectedTheme != null) {
            loadThemeProperties(selectedTheme);
            // Show steps container
            themeSelectionContainer.setVisible(false);
            themeSelectionContainer.setManaged(false);
            stepsContainer.setVisible(true);
            stepsContainer.setManaged(true);
            currentStep = 0;
            showStep(1);
        }
    }

    @FXML
    private void handleChangeTheme() {
        showThemeSelection();
    }

    @FXML
    private void handleNewTheme() {
        TextInputDialog dialog = new TextInputDialog("My Theme");
        dialog.setTitle("New Theme");
        dialog.setHeaderText("Create New Theme");
        dialog.setContentText("Please enter the name for the new theme:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                Theme newTheme = new Theme();
                newTheme.setName(name.trim());
                mainController.getAvailableThemes().add(newTheme);
                mainController.saveThemes();
                themeSelectionComboBox.getSelectionModel().select(newTheme);
                selectedTheme = newTheme;
                loadThemeProperties(newTheme);

                // Show steps container
                themeSelectionContainer.setVisible(false);
                themeSelectionContainer.setManaged(false);
                stepsContainer.setVisible(true);
                stepsContainer.setManaged(true);
                currentStep = 0;
                showStep(1);

                AppLogger.log("New theme created: " + name);
            }
        });
    }

    @FXML
    private void handleDeleteTheme() {
        Theme themeToDelete = themeSelectionComboBox.getSelectionModel().getSelectedItem();
        if (themeToDelete != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Delete Theme");
            alert.setHeaderText("Delete '" + themeToDelete.getName() + "'?");
            alert.setContentText("Are you sure you want to delete this theme?");

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                mainController.getAvailableThemes().remove(themeToDelete);
                mainController.saveThemes();
                if (mainController.getCurrentActiveTheme() == themeToDelete) {
                    if (!mainController.getAvailableThemes().isEmpty()) {
                        mainController.applyTheme(mainController.getAvailableThemes().get(0));
                    }
                }
                AppLogger.log("Theme deleted: " + themeToDelete.getName());
            }
        }
    }

    @FXML
    private void handleNext() {
        if (currentStep < totalSteps) {
            showStep(currentStep + 1);
        }
    }

    @FXML
    private void handlePrevious() {
        if (currentStep > 1) {
            showStep(currentStep - 1);
        }
    }

    @FXML
    private void handleFinish() {
        if (selectedTheme != null) {
            updateThemeFromInputs(selectedTheme);
            mainController.saveThemes();
            mainController.applyTheme(selectedTheme);
            AppLogger.log("Theme saved and applied: " + selectedTheme.getName());
            getStage().close();
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
    }

    private String toHexString(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }

    private Stage getStage() {
        return (Stage) themeSelectionComboBox.getScene().getWindow();
    }
}
