// src/api/ws.js
let socket = null;
const listeners = new Set();
const WS_URL = "wss://multi-chatapplication-project.onrender.com/chat";
let reconnectTimeout = null;

// Create or get WebSocket connection
export function getWS() {
    if (socket && (socket.readyState === WebSocket.OPEN || socket.readyState === WebSocket.CONNECTING)) {
        return socket;
    }

    socket = new WebSocket(WS_URL);

    socket.onopen = () => {
        console.log("[WS] Connected");
    };

    socket.onclose = () => {
        console.log("[WS] Closed. Reconnecting in 3 seconds...");
        reconnectTimeout = setTimeout(getWS, 3000); // Auto-reconnect
    };

    socket.onerror = (e) => {
        console.error("[WS] Error", e);
        socket.close(); // ensure closure triggers reconnect
    };

    socket.onmessage = (evt) => {
        try {
            const data = JSON.parse(evt.data);
            listeners.forEach((fn) => {
                try { fn(data); } catch (err) { console.error(err); }
            });
        } catch (err) {
            console.error("Invalid JSON from server:", evt.data);
        }
    };

    return socket;
}

// Send a message via WebSocket
export function sendWS(obj) {
    const ws = getWS();
    const doSend = () => ws.send(JSON.stringify(obj));

    if (ws.readyState === WebSocket.OPEN) {
        doSend();
    } else {
        ws.addEventListener("open", doSend, { once: true });
    }
}

// Subscribe to messages
export function subscribeWS(fn) {
    listeners.add(fn);
    return () => listeners.delete(fn);
}

// Optional: cleanup function to stop reconnecting if needed
export function closeWS() {
    if (reconnectTimeout) clearTimeout(reconnectTimeout);
    if (socket) socket.close();
}
