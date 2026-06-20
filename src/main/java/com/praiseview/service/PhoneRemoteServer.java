package com.praiseview.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.praiseview.controller.MainController;
import com.praiseview.model.ServiceListDTO;
import com.praiseview.model.VerseListDTO;
import com.praiseview.util.AppLogger;
import javafx.application.Platform;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class PhoneRemoteServer {

    public static final int DEFAULT_PORT = 8080;
    public static final String PATH = "/praiseview";

    private static final String WEBSOCKET_MAGIC = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    private static PhoneRemoteServer instance;

    private final MainController mainController;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicInteger connectionCount = new AtomicInteger(0);
    private final Set<Consumer<Integer>> connectionListeners = ConcurrentHashMap.newKeySet();
    private final Set<BufferedOutputStream> connectedClients = ConcurrentHashMap.newKeySet();
    private final ExecutorService clientExecutor = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r, "PraiseView Phone Remote Client");
        thread.setDaemon(true);
        return thread;
    });

    private ServerSocket serverSocket;
    private Thread acceptThread;
    private volatile boolean running;

    private PhoneRemoteServer(MainController mainController) {
        this.mainController = mainController;
    }

    public static synchronized PhoneRemoteServer start(MainController mainController) {
        if (instance != null && instance.isRunning()) {
            return instance;
        }

        PhoneRemoteServer server = new PhoneRemoteServer(mainController);
        try {
            server.startSocket(DEFAULT_PORT);
            instance = server;
            return instance;
        } catch (IOException e) {
            AppLogger.log("Could not start phone remote on port " + DEFAULT_PORT + ": " + e.getMessage());
        }

        try {
            server.startSocket(0);
            instance = server;
            return instance;
        } catch (IOException e) {
            AppLogger.log("Could not start phone remote WebSocket server: " + e.getMessage());
            return server;
        }
    }

    public static synchronized PhoneRemoteServer getInstance() {
        return instance;
    }

    public static synchronized void stopServer() {
        if (instance == null) {
            return;
        }

        instance.stop();
        instance = null;
    }

    public int getPort() {
        return serverSocket == null ? DEFAULT_PORT : serverSocket.getLocalPort();
    }

    public boolean isRunning() {
        return running && serverSocket != null && !serverSocket.isClosed();
    }

    public int getConnectionCount() {
        return connectionCount.get();
    }

    public void addConnectionListener(Consumer<Integer> listener) {
        connectionListeners.add(listener);
        listener.accept(getConnectionCount());
    }

    public void removeConnectionListener(Consumer<Integer> listener) {
        connectionListeners.remove(listener);
    }

    /**
     * Sends the service list to all connected phone clients
     */
    public void sendServiceListToClients(ServiceListDTO serviceList) {
        try {
            String json = objectMapper.writeValueAsString(serviceList);
            Map<String, Object> message = new HashMap<>();
            message.put("type", "service_list");
            message.put("data", objectMapper.readValue(json, Map.class));
            sendToAllClients(objectMapper.writeValueAsString(message));
        } catch (JsonProcessingException e) {
            AppLogger.log("Error serializing service list: " + e.getMessage());
        }
    }

    /**
     * Sends the verse/item list to all connected phone clients
     */
    public void sendVerseListToClients(VerseListDTO verseList) {
        try {
            String json = objectMapper.writeValueAsString(verseList);
            Map<String, Object> message = new HashMap<>();
            message.put("type", "verse_list");
            message.put("data", objectMapper.readValue(json, Map.class));
            sendToAllClients(objectMapper.writeValueAsString(message));
        } catch (JsonProcessingException e) {
            AppLogger.log("Error serializing verse list: " + e.getMessage());
        }
    }

    private void sendToAllClients(String message) {
        if (connectedClients.isEmpty()) {
            AppLogger.log("No connected clients to send message to");
            return;
        }

        java.util.List<BufferedOutputStream> failedClients = new java.util.ArrayList<>();
        
        for (BufferedOutputStream client : connectedClients) {
            try {
                sendText(client, message);
            } catch (IOException e) {
                AppLogger.log("Failed to send to client: " + e.getMessage());
                failedClients.add(client);
            }
        }

        // Remove failed clients from the set
        connectedClients.removeAll(failedClients);
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

    private void startSocket(int port) throws IOException {
        serverSocket = new ServerSocket();
        serverSocket.setReuseAddress(true);
        serverSocket.bind(new InetSocketAddress("0.0.0.0", port));
        running = true;

        acceptThread = new Thread(this::acceptLoop, "PraiseView Phone Remote Server");
        acceptThread.setDaemon(true);
        acceptThread.start();

        AppLogger.log("Phone remote WebSocket server started on port " + getPort());
    }

    private void stop() {
        running = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            AppLogger.log("Error closing phone remote WebSocket server: " + e.getMessage());
        }
        connectedClients.clear();
        connectionCount.set(0);
        notifyConnectionListeners(0);
        clientExecutor.shutdownNow();
        AppLogger.log("Phone remote WebSocket server stopped.");
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                clientExecutor.submit(() -> handleClient(socket));
            } catch (IOException e) {
                if (running) {
                    AppLogger.log("Phone remote accept error: " + e.getMessage());
                }
            }
        }
    }

    private void handleClient(Socket socket) {
        boolean connected = false;
        BufferedOutputStream output = null;
        try (socket;
             BufferedInputStream input = new BufferedInputStream(socket.getInputStream());
             BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream())) {

            output = out;
            
            if (!handshake(input, output)) {
                return;
            }

            connectedClients.add(output);
            connected = true;
            int activeConnections = connectionCount.incrementAndGet();
            notifyConnectionListeners(activeConnections);
            AppLogger.log("Phone remote connected: " + socket.getRemoteSocketAddress());
            sendText(output, "{\"status\":\"connected\",\"app\":\"PraiseView\"}");

            String message;
            while (running && (message = readTextFrame(input)) != null) {
                handleMessage(output, message);
            }
        } catch (IOException e) {
            AppLogger.log("Phone remote client error: " + e.getMessage());
        } finally {
            if (output != null) {
                connectedClients.remove(output);
            }
            if (connected) {
                int activeConnections = Math.max(0, connectionCount.decrementAndGet());
                notifyConnectionListeners(activeConnections);
                AppLogger.log("Phone remote disconnected: " + socket.getRemoteSocketAddress());
            }
        }
    }

    private void notifyConnectionListeners(int activeConnections) {
        for (Consumer<Integer> listener : connectionListeners) {
            try {
                listener.accept(activeConnections);
            } catch (Exception e) {
                AppLogger.log("Phone remote connection listener error: " + e.getMessage());
            }
        }
    }

    private boolean handshake(BufferedInputStream input, BufferedOutputStream output) throws IOException {
        String request = readHttpRequest(input);
        if (request == null || request.isBlank()) {
            return false;
        }

        String[] lines = request.split("\\r?\\n");
        String requestLine = lines[0];
        if (requestLine == null || !requestLine.startsWith("GET " + PATH + " ")) {
            writeHttpResponse(output, "404 Not Found", "Use " + PATH);
            return false;
        }

        Map<String, String> headers = new HashMap<>();
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            int separator = line.indexOf(':');
            if (separator > 0) {
                headers.put(line.substring(0, separator).trim().toLowerCase(Locale.ROOT),
                        line.substring(separator + 1).trim());
            }
        }

        String key = headers.get("sec-websocket-key");
        if (key == null || key.isBlank()) {
            writeHttpResponse(output, "400 Bad Request", "Missing Sec-WebSocket-Key");
            return false;
        }

        String acceptKey = createAcceptKey(key);
        String response = "HTTP/1.1 101 Switching Protocols\r\n"
                + "Upgrade: websocket\r\n"
                + "Connection: Upgrade\r\n"
                + "Sec-WebSocket-Accept: " + acceptKey + "\r\n\r\n";
        output.write(response.getBytes(StandardCharsets.US_ASCII));
        output.flush();
        return true;
    }

    private String readHttpRequest(BufferedInputStream input) throws IOException {
        ByteArrayOutputStream requestBytes = new ByteArrayOutputStream();
        int current;
        int previous3 = -1;
        int previous2 = -1;
        int previous1 = -1;

        while ((current = input.read()) != -1) {
            requestBytes.write(current);
            if (previous3 == '\r' && previous2 == '\n' && previous1 == '\r' && current == '\n') {
                return requestBytes.toString(StandardCharsets.US_ASCII);
            }
            previous3 = previous2;
            previous2 = previous1;
            previous1 = current;
        }

        return null;
    }

    private void handleMessage(BufferedOutputStream output, String message) throws IOException {
        Platform.runLater(() -> {
            boolean handled = mainController.handleRemoteCommand(message);
            try {
                if (handled) {
                    sendText(output, "{\"status\":\"ok\"}");
                } else {
                    sendText(output, "{\"status\":\"error\",\"message\":\"Unknown command\"}");
                }
            } catch (IOException e) {
                AppLogger.log("Could not send phone remote response: " + e.getMessage());
            }
        });
    }

    private String parseCommand(String message) {
        String trimmed = message == null ? "" : message.trim();
        if (trimmed.startsWith("{")) {
            trimmed = parseJsonCommand(trimmed);
            if (trimmed == null) {
                AppLogger.log("Invalid phone remote JSON message: " + message);
                return null;
            }
        }

        return trimmed.toLowerCase(Locale.ROOT);
    }

    private String parseJsonCommand(String message) {
        String key = "\"command\"";
        int keyIndex = message.indexOf(key);
        if (keyIndex < 0) {
            return null;
        }

        int colonIndex = message.indexOf(':', keyIndex + key.length());
        if (colonIndex < 0) {
            return null;
        }

        int valueStart = message.indexOf('"', colonIndex + 1);
        if (valueStart < 0) {
            return null;
        }

        StringBuilder value = new StringBuilder();
        boolean escaping = false;
        for (int i = valueStart + 1; i < message.length(); i++) {
            char current = message.charAt(i);
            if (escaping) {
                value.append(current);
                escaping = false;
            } else if (current == '\\') {
                escaping = true;
            } else if (current == '"') {
                return value.toString();
            } else {
                value.append(current);
            }
        }

        return null;
    }

    private String readTextFrame(BufferedInputStream input) throws IOException {
        int firstByte = input.read();
        if (firstByte == -1) {
            return null;
        }

        int opcode = firstByte & 0x0F;
        if (opcode == 0x8) {
            return null;
        }
        if (opcode != 0x1) {
            return "";
        }

        int secondByte = input.read();
        if (secondByte == -1) {
            return null;
        }

        boolean masked = (secondByte & 0x80) != 0;
        long payloadLength = secondByte & 0x7F;
        if (payloadLength == 126) {
            payloadLength = ((long) input.read() << 8) | input.read();
        } else if (payloadLength == 127) {
            payloadLength = 0;
            for (int i = 0; i < 8; i++) {
                payloadLength = (payloadLength << 8) | input.read();
            }
        }

        byte[] mask = new byte[4];
        if (masked && input.read(mask) != mask.length) {
            return null;
        }

        byte[] payload = input.readNBytes((int) payloadLength);
        if (payload.length != payloadLength) {
            return null;
        }

        if (masked) {
            for (int i = 0; i < payload.length; i++) {
                payload[i] = (byte) (payload[i] ^ mask[i % 4]);
            }
        }

        return new String(payload, StandardCharsets.UTF_8);
    }

    private synchronized void sendText(BufferedOutputStream output, String text) throws IOException {
        byte[] payload = text.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream frame = new ByteArrayOutputStream();
        frame.write(0x81);

        if (payload.length <= 125) {
            frame.write(payload.length);
        } else if (payload.length <= 65535) {
            frame.write(126);
            frame.write((payload.length >>> 8) & 0xFF);
            frame.write(payload.length & 0xFF);
        } else {
            frame.write(127);
            for (int i = 7; i >= 0; i--) {
                frame.write((payload.length >>> (8 * i)) & 0xFF);
            }
        }

        frame.write(payload);
        output.write(frame.toByteArray());
        output.flush();
    }

    private String createAcceptKey(String key) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest((key + WEBSOCKET_MAGIC).getBytes(StandardCharsets.US_ASCII));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-1 is not available", e);
        }
    }

    private void writeHttpResponse(BufferedOutputStream output, String status, String body) throws IOException {
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
        String response = "HTTP/1.1 " + status + "\r\n"
                + "Content-Type: text/plain; charset=utf-8\r\n"
                + "Content-Length: " + bodyBytes.length + "\r\n\r\n";
        output.write(response.getBytes(StandardCharsets.US_ASCII));
        output.write(bodyBytes);
        output.flush();
    }
}
