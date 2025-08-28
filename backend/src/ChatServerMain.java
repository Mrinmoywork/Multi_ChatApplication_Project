import org.glassfish.tyrus.server.Server;

import java.util.Scanner;

public class ChatServerMain {
    public static void main(String[] args) {
        Server server = new Server("localhost", 8080, "/", null, ChatServerEndpoint.class);

        try {
            server.start();
            System.out.println("Server started. Press Enter to stop...");
            new Scanner(System.in).nextLine();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            server.stop();
            System.out.println("Server stopped.");
        }
    }
}
