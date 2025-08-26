import javax.websocket.*;
import java.net.URI;

@ClientEndpoint
public class ChatTestClient {
    private Session session;

    public ChatTestClient() {
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            container.connectToServer(this, new URI("ws://localhost:8080/chat"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        System.out.println("Connected to server.");
    }

    @OnMessage
    public void onMessage(String message) {
        System.out.println("Received: " + message);
    }

    public void sendMessage(String msg) {
        try {
            session.getBasicRemote().sendText(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        ChatTestClient client = new ChatTestClient();
        Thread.sleep(1000); // wait for connection
        client.sendMessage("Hello from client!");
        Thread.sleep(5000); // wait to receive any responses
    }
}
