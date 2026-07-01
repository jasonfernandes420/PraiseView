package com.praiseview.controller;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class HelpDialog {
    private final Stage stage;
    private final WebView webView;
    private final ComboBox<String> documentSelector;

    public HelpDialog() {
        stage = new Stage();
        stage.setTitle("PraiseView Help & Documentation");
        Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();
        stage.setWidth(Math.min(1000, visualBounds.getWidth() * 0.92));
        stage.setHeight(Math.min(700, visualBounds.getHeight() * 0.92));
        stage.setMinWidth(Math.min(700, visualBounds.getWidth() * 0.75));
        stage.setMinHeight(Math.min(500, visualBounds.getHeight() * 0.75));

        webView = new WebView();
        documentSelector = new ComboBox<>();
        documentSelector.setPrefWidth(200);

        BorderPane root = new BorderPane();
        
        // Top toolbar
        VBox toolbar = createToolbar();
        root.setTop(toolbar);
        
        // Main content area
        root.setCenter(webView);
        
        Scene scene = new Scene(root);
        stage.setScene(scene);
        
        initializeDocuments();
        loadDocument("index");
    }

    private VBox createToolbar() {
        VBox toolbar = new VBox(10);
        toolbar.setPadding(new Insets(10));
        toolbar.setStyle("-fx-border-color: #cccccc; -fx-border-width: 0 0 1 0;");

        Label label = new Label("Select Documentation:");
        HBox selectorBox = new HBox(10);
        selectorBox.getChildren().addAll(label, documentSelector);

        toolbar.getChildren().add(selectorBox);
        return toolbar;
    }

    private void initializeDocuments() {
        documentSelector.getItems().addAll(
            "Welcome (index)",
            "User Guide",
            "Features",
            "Getting Started"
        );
        
        documentSelector.setOnAction(e -> {
            String selected = documentSelector.getSelectionModel().getSelectedItem();
            if (selected != null) {
                if (selected.contains("Welcome")) {
                    loadDocument("index");
                } else if (selected.contains("User Guide")) {
                    loadDocument("user-guide");
                } else if (selected.contains("Features")) {
                    loadDocument("features");
                } else if (selected.contains("Getting Started")) {
                    loadDocument("getting-started");
                }
            }
        });
        
        documentSelector.getSelectionModel().selectFirst();
    }

    private void loadDocument(String page) {

        var url = getClass().getResource("/docs/" + page + ".html");

        if (url == null) {
            webView.getEngine().loadContent(
                    "<h2>Documentation not found</h2>");
            return;
        }

        webView.getEngine().load(url.toExternalForm());
    }


    public void show() {
        stage.show();
    }

    public void close() {
        stage.close();
    }
}
