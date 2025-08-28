import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AuthService {

    public static boolean register(String username, String password) {
        try (Connection conn = DBUTIL.getConnection()) {
            // Check if username exists
            PreparedStatement check = conn.prepareStatement("SELECT * FROM users WHERE username=?");
            check.setString(1, username);
            ResultSet rs = check.executeQuery();
            if (rs.next()) return false;

            // Insert new user
            String hashed = PasswordUtil.hashPassword(password);
            PreparedStatement insert = conn.prepareStatement("INSERT INTO users (username, password) VALUES (?, ?)");
            insert.setString(1, username);
            insert.setString(2, hashed);
            insert.executeUpdate();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean login(String username, String password) {
        try (Connection conn = DBUTIL.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("SELECT password FROM users WHERE username=?");
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String hashed = rs.getString("password");
                return PasswordUtil.verifyPassword(password, hashed);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}

