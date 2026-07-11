package com.praiseview.controller;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.praiseview.PraiseViewApp;
import com.praiseview.service.PhoneRemoteServer;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;

import java.awt.image.BufferedImage;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class ConnectPhoneController {

    @FXML
    private ImageView qrCodeImageView;
    @FXML
    private Label connectionDetailsLabel;
    @FXML
    private Label connectionStatusLabel;
    @FXML
    private Hyperlink playStoreLink;

    private static final String APP_NAME = "PraiseView";
    private static final String PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=com.praiseview.companion";
    private String ipAddress;
    private int port; // This will be the WebSocket port
    private Consumer<Integer> connectionListener;

    @FXML
    public void initialize() {
        // For demonstration, let's use a placeholder IP and port.
        // In a real application, these would be dynamically determined.
        try {
            ipAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            ipAddress = "localhost"; // Fallback
            System.err.println("Could not determine IP address: " + e.getMessage());
        }
        port = 8080; // Example port for WebSocket server

        String connectionString = String.format("ws://%s:%d/%s", ipAddress, port, APP_NAME.toLowerCase());
        connectionDetailsLabel.setText(String.format("Details: IP: %s, Port: %d, App: %s", ipAddress, port, APP_NAME));
        updateConnectionStatus(0);

        generateQrCode(connectionString);
        
        // Setup Play Store link
        if (playStoreLink != null) {
            playStoreLink.setOnAction(e -> openPlayStoreLink());
        }
    }

    public void setConnectionDetails(String ipAddress, int port) {
        this.ipAddress = ipAddress;
        this.port = port;
        String connectionString = String.format("ws://%s:%d/%s", ipAddress, port, APP_NAME.toLowerCase());
        connectionDetailsLabel.setText(String.format("Details: IP: %s, Port: %d, App: %s", ipAddress, port, APP_NAME));
        generateQrCode(connectionString);
        watchConnectionStatus();
    }

    public void dispose() {
        PhoneRemoteServer server = PhoneRemoteServer.getInstance();
        if (server != null && connectionListener != null) {
            server.removeConnectionListener(connectionListener);
        }
        connectionListener = null;
    }

    private void watchConnectionStatus() {
        dispose();
        PhoneRemoteServer server = PhoneRemoteServer.getInstance();
        if (server == null || !server.isRunning()) {
            updateConnectionStatus(0);
            return;
        }

        connectionListener = count -> Platform.runLater(() -> updateConnectionStatus(count));
        server.addConnectionListener(connectionListener);
    }

    private void updateConnectionStatus(int activeConnections) {
        if (connectionStatusLabel == null) {
            return;
        }

        if (activeConnections > 0) {
            connectionStatusLabel.setText("Connected: " + activeConnections + " phone" + (activeConnections == 1 ? "" : "s"));
            connectionStatusLabel.setStyle("-fx-text-fill: #0f8a3b; -fx-font-weight: bold;");
            
            // Auto-close dialog after 2 seconds when phone connects
            Platform.runLater(() -> {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                Platform.runLater(this::closeDialog);
            });
        } else {
            connectionStatusLabel.setText("Waiting for phone connection...");
            connectionStatusLabel.setStyle("-fx-text-fill: #8a5a00; -fx-font-weight: bold;");
        }
    }

    private void closeDialog() {
        try {
            if (qrCodeImageView != null) {
                var stage = qrCodeImageView.getScene().getWindow();
                if (stage != null) {
                    stage.hide();
                }
            }
        } catch (Exception e) {
            System.err.println("Could not close dialog: " + e.getMessage());
        }
    }

    private void generateQrCode(String data) {
        int size = 300;
        Map<EncodeHintType, Object> hintMap = new HashMap<>();
        hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
        hintMap.put(EncodeHintType.MARGIN, 1); // Less white space around the QR code

        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix byteMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, size, size, hintMap);

            BufferedImage bufferedImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
            bufferedImage.createGraphics();

            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    bufferedImage.setRGB(i, j, byteMatrix.get(i, j) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }
            WritableImage fxImage = SwingFXUtils.toFXImage(bufferedImage, null);
            qrCodeImageView.setImage(fxImage);

        } catch (WriterException e) {
            System.err.println("Error generating QR code: " + e.getMessage());
            // Optionally, display an error message to the user
        }
    }

    private void openPlayStoreLink() {
        try {
            PraiseViewApp.getStaticHostServices().showDocument(PLAY_STORE_URL);
        } catch (Exception e) {
            System.err.println("Could not open Play Store link: " + e.getMessage());
        }
    }
}
