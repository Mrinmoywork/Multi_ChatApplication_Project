import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBUTIL {

    // Read from environment variables, fallback to defaults for local testing
    private static final String URL = System.getenv().getOrDefault(
            "DATABASE_URL", "jdbc:postgresql://localhost:5432/social_app"
    );
    private static final String USER = System.getenv().getOrDefault(
            "DB_USER", "postgres"
    );
    private static final String PASS = System.getenv().getOrDefault(
            "DB_PASS", "1234"
    );

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }
}

