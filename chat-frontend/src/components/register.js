// src/components/Register.js
import React, { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { Box, Paper, TextField, Button, Typography } from "@mui/material";
import { getWS, sendWS, subscribeWS } from "../api/ws";

export default function Register() {
    const nav = useNavigate();
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const [msg, setMsg] = useState("");

    const handleRegister = () => {
        setMsg("");
        getWS();
        sendWS({ type: "register", username, password });

        const unsub = subscribeWS((data) => {
            if (data.type === "register") {
                if (data.success) {
                    unsub();
                    setMsg("Registered! Redirecting to login...");
                    setTimeout(() => nav("/login"), 1200);
                } else {
                    setMsg(data.message || "Registration failed");
                }
            }
        });
    };

    return (
        <Box sx={{ minHeight: "100vh", display: "grid", placeItems: "center", bgcolor: "#f0f2f5" }}>
            <Paper sx={{ p: 4, width: 360, display: "grid", gap: 2 }}>
                <Typography variant="h5" align="center">Create account</Typography>
                <TextField label="Username" value={username} onChange={(e)=>setUsername(e.target.value)} />
                <TextField label="Password" type="password" value={password} onChange={(e)=>setPassword(e.target.value)} />
                <Button variant="contained" onClick={handleRegister}>Register</Button>
                {msg && <Typography color={msg.includes("Registered") ? "primary" : "error"} align="center">{msg}</Typography>}
                <Typography align="center">
                    Already have an account? <Link to="/login">Login</Link>
                </Typography>
            </Paper>
        </Box>
    );
}
