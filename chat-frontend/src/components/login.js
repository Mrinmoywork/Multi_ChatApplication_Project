// src/components/Login.js
import React, { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { Box, Paper, TextField, Button, Typography } from "@mui/material";
import { getWS, sendWS, subscribeWS } from "../api/ws";

export default function Login() {
    const nav = useNavigate();
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const [msg, setMsg] = useState("");

    const handleLogin = () => {
        setMsg("");
        getWS();
        sendWS({ type: "login", username, password });

        const unsub = subscribeWS((data) => {
            if (data.type === "login") {
                if (data.success) {
                    unsub();
                    nav("/chat", { state: { username } });
                } else {
                    setMsg(data.message || "Login failed");
                }
            }
        });
    };

    return (
        <Box sx={{ minHeight: "100vh", display: "grid", placeItems: "center", bgcolor: "#f0f2f5" }}>
            <Paper sx={{ p: 4, width: 360, display: "grid", gap: 2 }}>
                <Typography variant="h5" align="center">Login</Typography>
                <TextField label="Username" value={username} onChange={(e)=>setUsername(e.target.value)} />
                <TextField label="Password" type="password" value={password} onChange={(e)=>setPassword(e.target.value)} />
                <Button variant="contained" onClick={handleLogin}>Login</Button>
                {msg && <Typography color="error" align="center">{msg}</Typography>}
                <Typography align="center">
                    New here? <Link to="/register">Create an account</Link>
                </Typography>
            </Paper>
        </Box>
    );
}
