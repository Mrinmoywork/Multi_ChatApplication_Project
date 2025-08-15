import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class ChatServer {
    private static final int PORT = 1234;
    private static final Map<String, PrintWriter> clients = new ConcurrentHashMap<>();
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) {
        System.out.println("Chat Server (PostgreSQL) started on port " + PORT);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket sock = serverSocket.accept();
                new ClientHandler(sock).start();
            }
        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }

    private static class ClientHandler extends Thread {
        private final Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private String username;

        ClientHandler(Socket socket) { this.socket = socket; }

        @Override
        public void run() {
            try {
                in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);

                // --- AUTH HANDSHAKE ---
                String auth = in.readLine(); // REGISTER|u|p  or  LOGIN|u|p
                if (auth == null) { close(); return; }
                String[] a = auth.split("\\|", 3);
                if (a.length != 3) { out.println("LOGIN_FAIL"); close(); return; }
                String cmd = a[0], u = a[1], p = a[2];

                boolean ok = false;
                if ("REGISTER".equalsIgnoreCase(cmd)) {
                    ok = Database.registerUser(u, p);
                    out.println(ok ? "REGISTER_SUCCESS" : "REGISTER_FAIL");
                } else if ("LOGIN".equalsIgnoreCase(cmd)) {
                    ok = Database.loginUser(u, p);
                    out.println(ok ? "LOGIN_SUCCESS" : "LOGIN_FAIL");
                } else {
                    out.println("LOGIN_FAIL");
                }
                if (!ok) { close(); return; }

                this.username = u;
                clients.put(username, out);
                System.out.println("User joined: " + username + " from " + socket.getRemoteSocketAddress());

                // send recent history (last 20)
                Database.fetchRecentMessages(username, 20).forEach(row ->
                        out.println("HISTORY|" + String.join("|", row))
                );

                // notify others (optional)
                broadcastSystem("User '" + username + "' joined");

                // --- MAIN LOOP ---
                String line;
                while ((line = in.readLine()) != null) {
                    if (line.startsWith("MSG|")) {
                        handleMsg(line);
                    } else if (line.startsWith("FILE|")) {
                        handleFile(line);
                    } else if ("/quit".equalsIgnoreCase(line.trim())) {
                        break;
                    }
                }
            } catch (IOException e) {
                System.out.println("Connection error (" + username + "): " + e.getMessage());
            } finally {
                if (username != null) {
                    clients.remove(username);
                    broadcastSystem("User '" + username + "' left");
                }
                close();
            }
        }

        private void handleMsg(String line) {
            // MSG|ALL|text  OR MSG|@user|text
            String[] p = line.split("\\|", 3);
            if (p.length < 3) return;
            String to = p[1];
            String text = p[2];
            String ts = TS.format(LocalDateTime.now());

            if ("ALL".equalsIgnoreCase(to)) {
                Database.saveMessage(username, "ALL", text);
                for (Map.Entry<String, PrintWriter> entry : clients.entrySet()) {
                    entry.getValue().println("MSG|" + ts + "|" + username + "|ALL|" + text);
                }
            } else if (to.startsWith("@")) {
                String target = to.substring(1);
                Database.saveMessage(username, target, text);
                PrintWriter w = clients.get(target);
                // send to sender (echo) and target if online
                PrintWriter me = clients.get(username);
                if (me != null) me.println("MSG|" + ts + "|" + username + "|" + target + "|" + text);
                if (w != null) w.println("MSG|" + ts + "|" + username + "|" + target + "|" + text);
            }
        }

        private void handleFile(String line) {
            // FILE|@user|filename|base64
            String[] p = line.split("\\|", 4);
            if (p.length < 4) return;
            String to = p[1];
            String filename = p[2];
            String base64 = p[3];
            String ts = TS.format(LocalDateTime.now());

            if (to.startsWith("@")) {
                String target = to.substring(1);
                // store an indicator in DB (not storing the file bytes)
                Database.saveMessage(username, target, "[file: " + filename + "]");
                PrintWriter w = clients.get(target);
                PrintWriter me = clients.get(username);
                if (me != null) me.println("FILE|" + ts + "|" + username + "|" + target + "|" + filename + "|" + base64);
                if (w != null) w.println("FILE|" + ts + "|" + username + "|" + target + "|" + filename + "|" + base64);
            }
        }

        private void broadcastSystem(String text) {
            String ts = TS.format(LocalDateTime.now());
            Database.saveMessage("SYSTEM", "ALL", text);
            for (PrintWriter w : clients.values()) {
                w.println("MSG|" + ts + "|SYSTEM|ALL|" + text);
            }
        }

        private void close() {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }
}
