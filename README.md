𝐉𝐚𝐯𝐚 𝐒𝐨𝐜𝐤𝐞𝐭 𝐂𝐡𝐚𝐭 𝐀𝐩𝐩𝐥𝐢𝐜𝐚𝐭𝐢𝐨𝐧 𝐰𝐢𝐭𝐡 𝐏𝐨𝐬𝐭𝐠𝐫𝐞𝐒𝐐𝐋

📌 𝐎𝐯𝐞𝐫𝐯𝐢𝐞𝐰
This is a multi-client real-time chat application built using Java Socket Programming for network communication and PostgreSQL for message persistence. It demonstrates client-server architecture, multithreading, and database integration.

* 𝐋𝐢𝐯𝐞 𝐃𝐞𝐦𝐨 𝐋𝐢𝐧𝐤: https://drive.google.com/file/d/1fc_rExW1yxsRiwzXa7diyZ1MphXU9FHx/view?usp=sharing

🚀 𝐅𝐞𝐚𝐭𝐮𝐫𝐞𝐬
* Real-Time Messaging – Instant communication between multiple clients.
* Multi-Client Support – Server handles multiple connected clients simultaneously using threads.
* Persistent Storage – Messages saved to PostgreSQL with sender, content, and timestamp.
* Chat History Retrieval – New clients receive previous messages from the database.
* Cross-Platform – Works anywhere with Java & PostgreSQL.

🛠 𝐓𝐞𝐜𝐡 𝐒𝐭𝐚𝐜𝐤
Java – Core logic, socket programming, threading.
PostgreSQL – Database for storing messages.
JDBC – Java ↔ Database communication.

🖼 Architecture Diagram
graph TD
    subgraph Client Side
    A[Chat Client] -->|Send/Receive Messages| B[Socket Connection]
    end

    subgraph Server Side
    B --> C[Chat Server]
    C -->|Broadcast to Clients| A
    C --> D[Database Module]
    end

    subgraph Database
    D --> E[(PostgreSQL - messages table)]
    end


𝐖𝐨𝐫𝐤𝐟𝐥𝐨𝐰:

Client connects to the server via sockets.
Server spawns a thread for each client.
Messages are broadcast to all connected clients.
Every message is stored in PostgreSQL for history retrieval.

📂 𝐃𝐚𝐭𝐚𝐛𝐚𝐬𝐞 𝐒𝐭𝐫𝐮𝐜𝐭𝐮𝐫𝐞

𝐓𝐚𝐛𝐥𝐞: 𝐦𝐞𝐬𝐬𝐚𝐠𝐞𝐬
<img width="1916" height="1012" alt="image" src="https://github.com/user-attachments/assets/88797d89-f017-4041-a472-b3d0621f0668" />


⚙ 𝐒𝐞𝐭𝐮𝐩 𝐈𝐧𝐬𝐭𝐫𝐮𝐜𝐭𝐢𝐨𝐧𝐬
1️⃣ Install Requirements

Java JDK 8+
PostgreSQL (14+ recommended)
IntelliJ IDEA / Eclipse

2️⃣ Create Database & Table
CREATE DATABASE chat_app;

\c chat_app

CREATE TABLE messages (
    id SERIAL PRIMARY KEY,
    sender TEXT NOT NULL,
    message TEXT NOT NULL,
    ts TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

3️⃣ Update Credentials in Database.java
private static final String URL = "jdbc:postgresql://localhost:5432/chat_app";
private static final String USER = "postgres";
private static final String PASSWORD = "your_password";

4️⃣ Run the Application

Start ChatServer.java.
Open another terminal/IDE window and run ChatClient.java.
Run multiple clients to simulate multiple users.

📜 Example Output
Client 1:
[You]: Hello!
[Server]: Welcome to the chat room!


Client 2:
[John]: Hello!
[You]: Hi John!

<img width="1919" height="1019" alt="Screenshot 2025-08-15 191758" src="https://github.com/user-attachments/assets/d81ee742-b1b7-4ca7-ab4f-ed31e8bd20e4" />


📌 𝐅𝐮𝐭𝐮𝐫𝐞 𝐄𝐧𝐡𝐚𝐧𝐜𝐞𝐦𝐞𝐧𝐭𝐬 𝐃𝐚𝐭𝐚𝐛𝐚𝐬𝐞 𝐒𝐭𝐫𝐮𝐜𝐭𝐮𝐫𝐞
User authentication (Login/Signup system).
JavaFX/Swing UI for a modern interface.
Message encryption for security.
Deploy server to a remote cloud host.

📌📄 𝐋𝐢𝐜𝐞𝐧𝐬𝐞
This project is free for educational and personal use.
