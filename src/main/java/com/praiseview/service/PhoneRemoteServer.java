package com.praiseview.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.praiseview.controller.MainController;
import com.praiseview.model.ServiceListDTO;
import com.praiseview.model.VerseListDTO;
import com.praiseview.util.AppLogger;
import javafx.application.Platform;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.*;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class PhoneRemoteServer extends WebSocketServer {

    public static final int DEFAULT_PORT = 8080;
    public static final String PATH = "/praiseview";
    private static final long COMMAND_DEBOUNCE_MS = 100; // Debounce rapid commands

    private static PhoneRemoteServer instance;

    private final MainController mainController;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicInteger connectionCount = new AtomicInteger(0);
    private final Set<Consumer<Integer>> connectionListeners = ConcurrentHashMap.newKeySet();
    private final Set<WebSocket> connectedClients = ConcurrentHashMap.newKeySet();
    private final Map<WebSocket, ScheduledFuture<?>> pingTasks = new ConcurrentHashMap<>();
    private final Map<WebSocket, Long> lastCommandTime = new ConcurrentHashMap<>(); // For debouncing

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, r -> {
        Thread t = new Thread(r, "PraiseView-Ping");
        t.setDaemon(true);
        return t;
    });

    private final ScheduledExecutorService commandExecutor = Executors.newScheduledThreadPool(2, r -> {
        Thread t = new Thread(r, "PraiseView-Commands");
        t.setDaemon(true);
        return t;
    });

    private PhoneRemoteServer(MainController mainController, int port) {
        super(new InetSocketAddress("0.0.0.0", port));
        this.mainController = mainController;
        setReuseAddr(true);
        setConnectionLostTimeout(45); // Reduced from 90s to 45s for better mobile network resilience
    }

    public static synchronized PhoneRemoteServer start(MainController mainController) {
        if (instance != null && instance.isRunning()) {
            return instance;
        }

        PhoneRemoteServer server = new PhoneRemoteServer(mainController, DEFAULT_PORT);
        try {
            server.start();
            instance = server;
            AppLogger.log("Phone remote WebSocket server started on port " + server.getPort());
            return instance;
        } catch (Exception e) {
            AppLogger.log("Failed on port " + DEFAULT_PORT + ": " + e.getMessage());
        }

        server = new PhoneRemoteServer(mainController, 0);
        try {
            server.start();
            instance = server;
            AppLogger.log("Phone remote WebSocket server started on port " + server.getPort());
            return instance;
        } catch (Exception e) {
            AppLogger.log("Could not start server: " + e.getMessage());
            return server;
        }
    }

    public static synchronized void stopServer() {
        if (instance != null) {
            instance.stop();
            instance = null;
        }
    }

    public boolean isRunning() {
        return !scheduler.isShutdown();
    }

    @Override
    public void stop() {
        scheduler.shutdownNow();
        commandExecutor.shutdownNow();
        pingTasks.values().forEach(task -> task.cancel(true));
        pingTasks.clear();
        lastCommandTime.clear();
        try {
            super.stop();
        } catch (Exception ignored) {}
        connectedClients.clear();
        connectionCount.set(0);
        notifyConnectionListeners(0);
        AppLogger.log("Phone remote WebSocket server stopped.");
    }

    public int getPort() {
        return getAddress() != null ? getAddress().getPort() : DEFAULT_PORT;
    }

    public void sendServiceListToClients(ServiceListDTO serviceList) {
        // Move serialization to background thread to avoid blocking FX thread
        commandExecutor.execute(() -> broadcastTyped("service_list", serviceList));
    }

    public void sendVerseListToClients(VerseListDTO verseList) {
        // Move serialization to background thread to avoid blocking FX thread
        commandExecutor.execute(() -> broadcastTyped("verse_list", verseList));
    }

    private void broadcastTyped(String type, Object data) {
        try {
            String json = objectMapper.writeValueAsString(Map.of("type", type, "data", data));
            broadcast(json);
        } catch (JsonProcessingException e) {
            AppLogger.log("Broadcast error: " + e.getMessage());
        }
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        connectedClients.add(conn);
        int count = connectionCount.incrementAndGet();
        notifyConnectionListeners(count);

        AppLogger.log("Phone remote connected: " + conn.getRemoteSocketAddress());
        conn.send("{\"status\":\"connected\",\"app\":\"PraiseView\"}");

        // Send current projection state immediately after connection (for reconnection scenarios)
        AppLogger.log("Phone remote connected - sending current projection state");
        commandExecutor.execute(() -> {
            try {
                // Send service list
                if (mainController != null) {
                    ServiceListDTO serviceList = mainController.getCurrentServiceList();
                    if (serviceList != null) {
                        String json = objectMapper.writeValueAsString(Map.of("type", "service_list", "data", serviceList));
                        try {
                            if (conn.isOpen()) conn.send(json);
                        } catch (Exception e) {
                            AppLogger.log("Failed to send service list after reconnect");
                        }
                    }
                    
                    // Send verse list for current item
                    VerseListDTO verseList = mainController.getCurrentVerseList();
                    if (verseList != null) {
                        String json = objectMapper.writeValueAsString(Map.of("type", "verse_list", "data", verseList));
                        try {
                            if (conn.isOpen()) conn.send(json);
                        } catch (Exception e) {
                            AppLogger.log("Failed to send verse list after reconnect");
                        }
                    }
                }
            } catch (JsonProcessingException e) {
                AppLogger.log("Error sending state after reconnect: " + e.getMessage());
            }
        });

        ScheduledFuture<?> pingTask = scheduler.scheduleAtFixedRate(() -> {
            if (conn.isOpen()) conn.sendPing();
        }, 5, 15, TimeUnit.SECONDS); // Start pinging after 5 seconds instead of 15
        pingTasks.put(conn, pingTask);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        connectedClients.remove(conn);
        ScheduledFuture<?> pingTask = pingTasks.remove(conn);
        if (pingTask != null) {
            pingTask.cancel(true);
        }
        lastCommandTime.remove(conn); // Clean up debounce tracking
        int count = Math.max(0, connectionCount.decrementAndGet());
        notifyConnectionListeners(count);

        AppLogger.log(String.format("Phone remote disconnected: %s | Code: %d | Reason: %s | Remote: %b",
                conn.getRemoteSocketAddress(), code, reason != null ? reason : "null", remote));
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        if (message.trim().toLowerCase().contains("ping")) {
            conn.send("{\"type\":\"pong\"}");
            return;
        }

        // Debounce rapid commands from same client
        long now = System.currentTimeMillis();
        Long lastTime = lastCommandTime.getOrDefault(conn, 0L);
        if (now - lastTime < COMMAND_DEBOUNCE_MS) {
            // Command came in too quickly, skip it
            try {
                conn.send("{\"status\":\"debounced\"}");
            } catch (Exception ignored) {}
            return;
        }
        lastCommandTime.put(conn, now);

        Platform.runLater(() -> {
            boolean handled = mainController.handleRemoteCommand(message);
            try {
                conn.send(handled ? "{\"status\":\"ok\"}" : "{\"status\":\"error\",\"message\":\"Unknown command\"}");
            } catch (Exception e) {
                AppLogger.log("Failed to send response");
            }
        });
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        if (conn != null) {
            ScheduledFuture<?> pingTask = pingTasks.remove(conn);
            if (pingTask != null) {
                pingTask.cancel(true);
            }
        }
        AppLogger.log("WebSocket error: " + ex.getMessage());
    }

    @Override
    public void onStart() {
        AppLogger.log("Phone remote WebSocket server started successfully");
    }

    private void notifyConnectionListeners(int count) {
        for (Consumer<Integer> listener : connectionListeners) {
            try {
                listener.accept(count);
            } catch (Exception ignored) {}
        }
    }

    public static String findLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (!networkInterface.isUp() || networkInterface.isLoopback() || networkInterface.isVirtual()) {
                    continue;
                }

                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (address instanceof Inet4Address && !address.isLoopbackAddress() && address.isSiteLocalAddress()) {
                        return address.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            AppLogger.log("Could not inspect network interfaces: " + e.getMessage());
        }

        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            AppLogger.log("Could not determine local IP address: " + e.getMessage());
            return "localhost";
        }
    }
    public static synchronized PhoneRemoteServer getInstance() {
        return instance;
    }

    public void addConnectionListener(Consumer<Integer> listener) {
        connectionListeners.add(listener);
        listener.accept(getConnectionCount());
    }

    public void removeConnectionListener(Consumer<Integer> listener) {
        connectionListeners.remove(listener);
    }

    public int getConnectionCount() {
        return connectionCount.get();
    }
}