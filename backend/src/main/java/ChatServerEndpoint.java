import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import org.json.JSONObject;

import java.sql.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/chat")
public class ChatServerEndpoint {

    private static final Map<Session, String> sessionUserMap = new ConcurrentHashMap<>();
    private static final Map<String, Session> userSessionMap = new ConcurrentHashMap<>();
    private static Connection conn;

    static {
        try {
            Class.forName("org.postgresql.Driver");

            String URL = System.getenv("DATABASE_URL");
            String USER = System.getenv("DB_USER");
            String PASS = System.getenv("DB_PASS");

            conn = DriverManager.getConnection(URL, USER, PASS);
            System.out.println("DB connected.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @OnOpen
    public void onOpen(Session session) {
        System.out.println("New connection: " + session.getId());
    }

    @OnMessage
    public void onMessage(Session session, String messageText) {
        try {
            JSONObject obj = new JSONObject(messageText);
            String type = obj.optString("type", "");

            switch (type) {
                case "register":
                    handleRegister(session, obj);
                    break;
                case "login":
                    handleLogin(session, obj);
                    break;
                case "chat":           // public
                    handleChatMessage(session, obj);
                    break;
                case "private":        // private message
                    handlePrivateMessage(session, obj);
                    break;
                case "ping":
                    send(session, new JSONObject().put("type", "pong").toString());
                    break;
                default:
                    send(session, new JSONObject().put("type", "error").put("message", "Unknown type").toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
            safeSend(session, new JSONObject().put("type", "error").put("message", "Server exception").toString());
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        String username = sessionUserMap.remove(session);
        if (username != null) userSessionMap.remove(username);
        broadcastOnlineUsers();
        System.out.println("Closed: " + session.getId() + " reason=" + reason);
    }

    @OnError
    public void onError(Session session, Throwable thr) {
        System.err.println("Error on session " + (session != null ? session.getId() : "null"));
        thr.printStackTrace();
    }

    // ---------- handlers ----------

    private void handleRegister(Session session, JSONObject obj) {
        String username = obj.optString("username", "").trim();
        String password = obj.optString("password", "");
        JSONObject res = new JSONObject().put("type", "register");

        if (username.isEmpty() || password.isEmpty()) {
            res.put("success", false).put("message", "Username and password required");
            send(session, res.toString());
            return;
        }

        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO users(username, password) VALUES (?, ?)"
        )) {
            ps.setString(1, username);
            ps.setString(2, password); // production: hash!
            ps.executeUpdate();
            res.put("success", true).put("message", "Registered successfully");
        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState())) {
                res.put("success", false).put("message", "Username already exists");
            } else {
                e.printStackTrace();
                res.put("success", false).put("message", "Server error");
            }
        }
        send(session, res.toString());
    }

    private void handleLogin(Session session, JSONObject obj) {
        String username = obj.optString("username", "").trim();
        String password = obj.optString("password", "");
        JSONObject res = new JSONObject().put("type", "login");

        if (username.isEmpty() || password.isEmpty()) {
            res.put("success", false).put("message", "Username and password required");
            send(session, res.toString());
            return;
        }

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM users WHERE username = ? AND password = ?"
        )) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    sessionUserMap.put(session, username);
                    userSessionMap.put(username, session);
                    res.put("success", true).put("message", "Login successful");
                    send(session, res.toString());
                    broadcastOnlineUsers();
                    sendRecentHistory(session, 50); // send last 50 messages
                    return;
                }
            }
            res.put("success", false).put("message", "Invalid credentials");
        } catch (SQLException e) {
            e.printStackTrace();
            res.put("success", false).put("message", "Server error");
        }
        send(session, res.toString());
    }

    private void handleChatMessage(Session session, JSONObject obj) {
        String sender = sessionUserMap.get(session);
        if (sender == null) { safeSend(session, new JSONObject().put("type","error").put("message","Not logged in").toString()); return; }
        String content = obj.optString("message", "").trim();
        if (content.isEmpty()) return;

        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO messages(sender, receiver, content, timestamp) VALUES (?, ?, ?, CURRENT_TIMESTAMP)"
        )) {
            ps.setString(1, sender);
            ps.setNull(2, Types.VARCHAR);
            ps.setString(3, content);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }

        JSONObject msg = new JSONObject()
                .put("type", "chat")
                .put("sender", sender)
                .put("content", content)
                .put("timestamp", new Timestamp(System.currentTimeMillis()).toString());

        broadcast(msg.toString());
    }

    private void handlePrivateMessage(Session session, JSONObject obj) {
        String sender = sessionUserMap.get(session);
        if (sender == null) return;

        String receiver = obj.optString("receiver", "").trim();
        String content = obj.optString("message", "").trim();
        if (receiver.isEmpty() || content.isEmpty()) return;

        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO messages(sender, receiver, content, timestamp) VALUES (?, ?, ?, CURRENT_TIMESTAMP)"
        )) {
            ps.setString(1, sender);
            ps.setString(2, receiver);
            ps.setString(3, content);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }

        JSONObject msg = new JSONObject()
                .put("type", "private")
                .put("sender", sender)
                .put("receiver", receiver)
                .put("content", content)
                .put("timestamp", new Timestamp(System.currentTimeMillis()).toString());

        Session r = userSessionMap.get(receiver);
        if (r != null && r.isOpen()) send(r, msg.toString());
        send(session, msg.toString()); // echo back to sender
    }

    // ---------- helpers ----------

    private void sendRecentHistory(Session session, int limit) {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT sender, receiver, content, timestamp FROM messages ORDER BY id DESC LIMIT ?"
        )) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                // send newest -> oldest; client can reverse if needed
                while (rs.next()) {
                    JSONObject m = new JSONObject()
                            .put("type", rs.getString("receiver") == null ? "chat" : "private")
                            .put("sender", rs.getString("sender"))
                            .put("receiver", rs.getString("receiver"))
                            .put("content", rs.getString("content"))
                            .put("timestamp", rs.getTimestamp("timestamp").toString());
                    send(session, m.toString());
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void broadcastOnlineUsers() {
        JSONObject msg = new JSONObject().put("type", "online").put("users", sessionUserMap.values());
        broadcast(msg.toString());
    }

    private void broadcast(String text) {
        for (Session s : sessionUserMap.keySet()) safeSend(s, text);
    }

    private void send(Session s, String text) {
        try {
            s.getBasicRemote().sendText(text);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void safeSend(Session s, String text) {
        if (s != null && s.isOpen()) send(s, text);
    }
}
