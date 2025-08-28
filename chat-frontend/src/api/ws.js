// src/api/ws.js
let socket = null;
const listeners = new Set();

const WS_URL = "wss://multi-chatapplication-project.onrender.com/chat";

export function getWS() {
    if (socket && (socket.readyState === WebSocket.OPEN || socket.readyState === WebSocket.CONNECTING)) {
        return socket;
    }
    socket = new WebSocket(WS_URL);

    socket.onopen = () => console.log("[WS] open");
    socket.onclose = () => console.log("[WS] close");
    socket.onerror = (e) => console.error("[WS] error", e);
    socket.onmessage = (evt) => {
        try {
            const data = JSON.parse(evt.data);
            listeners.forEach((fn) => {
                try { fn(data); } catch (e) { console.error(e); }
            });
        } catch (e) {
            console.error("Invalid JSON from server:", evt.data);
        }
    };
    return socket;
}

export function sendWS(obj) {
    const ws = getWS();
    const doSend = () => ws.send(JSON.stringify(obj));
    if (ws.readyState === WebSocket.OPEN) doSend();
    else ws.addEventListener("open", doSend, { once: true });
}

export function subscribeWS(fn) {
    listeners.add(fn);
    return () => listeners.delete(fn);
}
