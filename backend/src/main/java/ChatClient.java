import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

public class ChatClient {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 1234;

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT)) {
            System.out.println("Connected to server " + SERVER_HOST + ":" + SERVER_PORT);

            BufferedReader in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out    = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
            BufferedReader kb  = new BufferedReader(new InputStreamReader(System.in));

            // --- AUTH ---
            System.out.print("Enter command (LOGIN/REGISTER): ");
            String cmd = kb.readLine().trim();
            System.out.print("Username: ");
            String username = kb.readLine().trim();
            System.out.print("Password: ");
            String password = kb.readLine().trim();

            out.println(cmd.toUpperCase() + "|" + username + "|" + password);

            String authResp = in.readLine();
            if (authResp == null || authResp.endsWith("_FAIL")) {
                System.out.println("Authentication failed. Exiting.");
                return;
            }
            System.out.println("Auth success. Welcome " + username + "!");
            System.out.println("""
                    Commands:
                      Type a message to broadcast to ALL
                      /w <user> <message>   -> private message
                      /file <user> <path>   -> send small file
                      /quit                 -> exit
                    """);

            // --- READER THREAD ---
            Thread reader = new Thread(() -> {
                try {
                    String line;
                    while ((line = in.readLine()) != null) {
                        if (line.startsWith("FILE|")) {
                            // FILE|ts|sender|receiver|filename|base64
                            String[] p = line.split("\\|", 6);
                            if (p.length == 6) {
                                String ts = p[1], from = p[2], to = p[3], filename = p[4], b64 = p[5];
                                saveIncomingFile(filename, b64);
                                System.out.println("[FILE] " + ts + " " + from + " → " + to + " : saved " + filename);
                            }
                        } else if (line.startsWith("MSG|")) {
                            // MSG|ts|sender|receiver|text
                            String[] p = line.split("\\|", 5);
                            if (p.length == 5) {
                                System.out.println("[" + p[1] + "] " + p[2] + " → " + p[3] + " : " + p[4]);
                            }
                        } else if (line.startsWith("HISTORY|")) {
                            // HISTORY|ts|sender|receiver|text
                            String[] p = line.split("\\|", 5);
                            if (p.length == 5) {
                                System.out.println("(history) [" + p[1] + "] " + p[2] + " → " + p[3] + " : " + p[4]);
                            }
                        } else {
                            System.out.println(line);
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Disconnected from server.");
                }
            });
            reader.setDaemon(true);
            reader.start();

            // --- SENDER LOOP ---
            String input;
            while ((input = kb.readLine()) != null) {
                input = input.trim();
                if (input.equalsIgnoreCase("/quit")) {
                    out.println("/quit");
                    break;
                } else if (input.startsWith("/w ")) {
                    // /w bob hello there
                    int sp2 = input.indexOf(' ', 3);
                    if (sp2 > 3) {
                        String target = input.substring(3, sp2).trim();
                        String msg = input.substring(sp2 + 1);
                        out.println("MSG|@" + target + "|" + msg);
                    } else {
                        System.out.println("Usage: /w <user> <message>");
                    }
                } else if (input.startsWith("/file ")) {
                    // /file bob /path/to/file
                    String[] parts = input.split("\\s+", 3);
                    if (parts.length == 3) {
                        String target = parts[1];
                        String path = parts[2];
                        try {
                            byte[] data = Files.readAllBytes(Paths.get(path));
                            String base64 = Base64.getEncoder().encodeToString(data);
                            String filename = Paths.get(path).getFileName().toString();
                            out.println("FILE|@" + target + "|" + filename + "|" + base64);
                        } catch (IOException e) {
                            System.out.println("File read error: " + e.getMessage());
                        }
                    } else {
                        System.out.println("Usage: /file <user> <path>");
                    }
                } else {
                    // broadcast
                    out.println("MSG|ALL|" + input);
                }
            }
        } catch (IOException e) {
            System.out.println("Client error: " + e.getMessage());
        }
        System.out.println("Bye!");
    }

    private static void saveIncomingFile(String filename, String base64) {
        try {
            byte[] data = Base64.getDecoder().decode(base64);
            File dir = new File("downloads");
            if (!dir.exists()) dir.mkdirs();
            File out = new File(dir, filename);
            try (FileOutputStream fos = new FileOutputStream(out)) {
                fos.write(data);
            }
        } catch (Exception e) {
            System.out.println("File save error: " + e.getMessage());
        }
    }
}
