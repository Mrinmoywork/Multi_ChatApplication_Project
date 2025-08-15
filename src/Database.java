import java.sql.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class Database {
    private static final String URL = "jdbc:postgresql://localhost:5432/chat_app";
    private static final String USER = "postgres"; // <- change if needed
    private static final String PASS = "1234"; // <- change if needed

    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    public static boolean registerUser(String username, String password) {
        String sql = "INSERT INTO users(username, password_hash) VALUES (?, ?)";
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, hash(password));
            ps.executeUpdate();
            return true;
        } catch (Exception e) {
            System.out.println("Registration error: " + e.getMessage());
            return false;
        }
    }

    public static boolean loginUser(String username, String password) {
        String sql = "SELECT 1 FROM users WHERE username=? AND password_hash=?";
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, hash(password));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            System.out.println("Login error: " + e.getMessage());
            return false;
        }
    }

    public static void saveMessage(String sender, String receiver, String message) {
        String sql = "INSERT INTO messages(sender, receiver, message) VALUES (?, ?, ?)";
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sender);
            ps.setString(2, receiver);
            ps.setString(3, message);
            ps.executeUpdate();
        } catch (Exception e) {
            System.out.println("Save message error: " + e.getMessage());
        }
    }

    /** Fetch recent messages involving this user (both sent and received), including broadcasts. */
    public static List<String[]> fetchRecentMessages(String username, int limit) {
        String sql =
                "SELECT to_char(ts, 'YYYY-MM-DD HH24:MI:SS') as ts, sender, receiver, message " +
                        "FROM messages " +
                        "WHERE receiver = 'ALL' OR sender = ? OR receiver = ? " +
                        "ORDER BY ts DESC LIMIT ?";
        List<String[]> out = new ArrayList<>();
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, username);
            ps.setInt(3, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new String[] {
                            rs.getString("ts"),
                            rs.getString("sender"),
                            rs.getString("receiver"),
                            rs.getString("message")
                    });
                }
            }
        } catch (Exception e) {
            System.out.println("History fetch error: " + e.getMessage());
        }
        // reverse to oldest→newest for nicer display
        List<String[]> rev = new ArrayList<>();
        for (int i = out.size() - 1; i >= 0; i--) rev.add(out.get(i));
        return rev;
    }

    private static String hash(String password) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] h = md.digest(password.getBytes());
        StringBuilder sb = new StringBuilder();
        for (byte b : h) {
            String x = Integer.toHexString(0xff & b);
            if (x.length() == 1) sb.append('0');
            sb.append(x);
        }
        return sb.toString();
    }
}

