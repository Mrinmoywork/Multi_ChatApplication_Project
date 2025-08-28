import jakarta.websocket.DeploymentException;
import org.glassfish.tyrus.server.Server;

public class ChatServerMain {

    public static void main(String[] args) {
        // Create the server on 0.0.0.0:8080
        Server server = new Server("0.0.0.0", 8080, "/", null, ChatServerEndpoint.class);

        try {
            server.start();
            System.out.println("Server started on port 8080. Press Ctrl+C to stop.");

            // Keep the server running indefinitely
            Thread.currentThread().join();

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (DeploymentException e) {
            throw new RuntimeException(e);
        } finally {
            server.stop();
            System.out.println("Server stopped.");
        }
    }
}