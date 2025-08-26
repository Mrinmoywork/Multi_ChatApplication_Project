// src/components/ChatRoom.js
import React, { useEffect, useRef, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { subscribeWS, sendWS, getWS } from "../api/ws";
import { Box, Paper, Typography, TextField, Button } from "@mui/material";
import { motion } from "framer-motion";

export default function ChatRoom() {
    const loc = useLocation();
    const nav = useNavigate();
    const username = loc.state?.username;
    const [messages, setMessages] = useState([]); // objects from server
    const [text, setText] = useState("");
    const listRef = useRef(null);

    useEffect(() => {
        if (!username) {
            nav("/login");
            return;
        }
        getWS();
        const unsub = subscribeWS((data) => {
            if (data.type === "chat" || data.type === "private") {
                // server sends: {type, sender, content, timestamp, receiver?}
                setMessages((prev) => [...prev, data]);
            } else if (data.type === "online") {
                // optional: handle online users
            }
        });
        return () => unsub();
    }, [username, nav]);

    useEffect(() => {
        if (listRef.current) listRef.current.scrollTop = listRef.current.scrollHeight;
    }, [messages]);

    const sendMessage = () => {
        if (!text.trim()) return;
        sendWS({ type: "chat", message: text.trim() });
        setText("");
    };

    return (
        <Box sx={{ minHeight: "100vh", display: "flex", flexDirection: "column", alignItems: "center", bgcolor: "#f0f2f5", p: 3 }}>
            <Typography variant="h4" sx={{ mb: 2 }}>Welcome, {username}</Typography>

            <Paper elevation={3} sx={{ width: "70%", height: "70vh", display: "flex", flexDirection: "column", p:2 }}>
                <Box ref={listRef} sx={{ flex: 1, overflowY: "auto", mb: 2 }}>
                    {messages.map((m, i) => {
                        const mine = m.sender === username;
                        const time = m.timestamp ? new Date(m.timestamp).toLocaleTimeString() : "";
                        return (
                            <motion.div
                                key={i}
                                initial={{ opacity: 0, x: mine ? 100 : -100 }}
                                animate={{ opacity: 1, x: 0 }}
                                transition={{ duration: 0.25 }}
                                style={{ display: "flex", justifyContent: mine ? "flex-end" : "flex-start", marginBottom: 8 }}
                            >
                                <Box
                                    sx={{
                                        bgcolor: mine ? "#1976d2" : "#e0e0e0",
                                        color: mine ? "#fff" : "#000",
                                        p: 1.25,
                                        borderRadius: 2,
                                        maxWidth: "70%",
                                        wordBreak: "break-word"
                                    }}
                                >
                                    <Typography variant="subtitle2" sx={{ fontWeight: 700 }}>{m.sender} <span style={{fontSize: "0.75rem", color: mine ? "#d1e7ff" : "#666", marginLeft:8}}>{time}</span></Typography>
                                    <Typography variant="body1">{m.content}</Typography>
                                </Box>
                            </motion.div>
                        );
                    })}
                </Box>

                <Box sx={{ display: "flex", gap: 1 }}>
                    <TextField fullWidth placeholder="Type a message..." value={text} onChange={(e)=>setText(e.target.value)} onKeyDown={(e)=> e.key === "Enter" && sendMessage()} />
                    <Button variant="contained" onClick={sendMessage}>Send</Button>
                </Box>
            </Paper>
        </Box>
    );
}
