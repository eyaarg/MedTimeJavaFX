package esprit.fx.services;

import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONObject;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NotificationWebSocketArij {
    private static final int PORT = 8766;
    private static final NotificationWebSocketArij INSTANCE = new NotificationWebSocketArij();

    private final Map<WebSocket, Integer> clients = new ConcurrentHashMap<>();
    private NotificationSocketServer server;
    private WebSocketClient client;
    private int currentUserId;
    private Runnable onNotification;

    private NotificationWebSocketArij() {
    }

    public static NotificationWebSocketArij getInstance() {
        return INSTANCE;
    }

    public synchronized void startForUser(int userId, Runnable onNotification) {
        if (userId <= 0) {
            return;
        }
        this.currentUserId = userId;
        this.onNotification = onNotification;
        startServerIfPossible();
        connectClient();
    }

    public void publishNotification(int userId) {
        if (userId <= 0) {
            return;
        }
        JSONObject payload = new JSONObject()
                .put("event", "notification")
                .put("userId", userId);

        broadcastToUser(userId, payload.toString());
        if (client != null && client.isOpen()) {
            client.send(new JSONObject()
                    .put("event", "notify")
                    .put("userId", userId)
                    .toString());
        }
    }

    public synchronized void stop() {
        try {
            if (client != null) {
                client.close();
            }
        } catch (Exception e) {
            System.err.println("[NotificationWebSocketArij] Erreur fermeture client: " + e.getMessage());
        }
    }

    private void startServerIfPossible() {
        if (server != null) {
            return;
        }
        try {
            server = new NotificationSocketServer(new InetSocketAddress("localhost", PORT));
            server.start();
            System.out.println("[NotificationWebSocketArij] Serveur WebSocket demarre sur ws://localhost:" + PORT);
        } catch (Exception e) {
            server = null;
            System.out.println("[NotificationWebSocketArij] Serveur deja disponible ou port occupe: " + e.getMessage());
        }
    }

    private void connectClient() {
        if (client != null && client.isOpen()) {
            return;
        }
        try {
            client = new WebSocketClient(new URI("ws://localhost:" + PORT + "/notifications")) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    send(new JSONObject()
                            .put("event", "register")
                            .put("userId", currentUserId)
                            .toString());
                    System.out.println("[NotificationWebSocketArij] Client connecte userId=" + currentUserId);
                }

                @Override
                public void onMessage(String message) {
                    handleClientMessage(message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    System.out.println("[NotificationWebSocketArij] Client ferme: " + reason);
                }

                @Override
                public void onError(Exception ex) {
                    System.err.println("[NotificationWebSocketArij] Client erreur: " + ex.getMessage());
                }
            };
            client.connect();
        } catch (Exception e) {
            System.err.println("[NotificationWebSocketArij] Connexion impossible: " + e.getMessage());
        }
    }

    private void handleClientMessage(String message) {
        try {
            JSONObject json = new JSONObject(message);
            if ("notification".equals(json.optString("event")) && json.optInt("userId") == currentUserId) {
                Runnable callback = onNotification;
                if (callback != null) {
                    callback.run();
                }
            }
        } catch (Exception e) {
            System.err.println("[NotificationWebSocketArij] Message ignore: " + e.getMessage());
        }
    }

    private void handleServerMessage(WebSocket conn, String message) {
        try {
            JSONObject json = new JSONObject(message);
            String event = json.optString("event");
            int userId = json.optInt("userId");
            if ("register".equals(event) && userId > 0) {
                clients.put(conn, userId);
            } else if ("notify".equals(event) && userId > 0) {
                JSONObject payload = new JSONObject()
                        .put("event", "notification")
                        .put("userId", userId);
                broadcastToUser(userId, payload.toString());
            }
        } catch (Exception e) {
            System.err.println("[NotificationWebSocketArij] Message serveur ignore: " + e.getMessage());
        }
    }

    private void broadcastToUser(int userId, String payload) {
        for (Map.Entry<WebSocket, Integer> entry : clients.entrySet()) {
            WebSocket conn = entry.getKey();
            if (entry.getValue() == userId && conn != null && conn.isOpen()) {
                conn.send(payload);
            }
        }
    }

    private class NotificationSocketServer extends WebSocketServer {
        NotificationSocketServer(InetSocketAddress address) {
            super(address);
        }

        @Override
        public void onOpen(WebSocket conn, ClientHandshake handshake) {
            System.out.println("[NotificationWebSocketArij] Connexion ouverte");
        }

        @Override
        public void onClose(WebSocket conn, int code, String reason, boolean remote) {
            clients.remove(conn);
        }

        @Override
        public void onMessage(WebSocket conn, String message) {
            handleServerMessage(conn, message);
        }

        @Override
        public void onError(WebSocket conn, Exception ex) {
            System.err.println("[NotificationWebSocketArij] Serveur erreur: " + ex.getMessage());
        }

        @Override
        public void onStart() {
            setConnectionLostTimeout(30);
        }
    }
}
