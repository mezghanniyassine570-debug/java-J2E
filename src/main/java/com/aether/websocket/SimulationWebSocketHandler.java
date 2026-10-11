package com.aether.websocket;

import com.aether.engine.SimulationEngine;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles real-time bi-directional WebSocket communication for simulation state broadcasts and client actions.
 * Connected to endpoint /ws/simulation.
 */
@Component
public class SimulationWebSocketHandler extends TextWebSocketHandler {

    private final SimulationEngine simulationEngine;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    @Autowired
    public SimulationWebSocketHandler(SimulationEngine simulationEngine) {
        this.simulationEngine = simulationEngine;
        // Register tick broadcast listener
        simulationEngine.registerTickListener(this::broadcastStatePacket);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        // Send initial state snapshot immediately upon connection
        try {
            Map<String, Object> state = simulationEngine.getSimulationStatePacket();
            String json = objectMapper.writeValueAsString(Map.of("type", "INIT_STATE", "data", state));
            session.sendMessage(new TextMessage(json));
        } catch (IOException ignored) {
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> command = objectMapper.readValue(message.getPayload(), Map.class);
            String action = (String) command.get("action");
            if (action == null) return;

            switch (action.toLowerCase()) {
                case "start" -> simulationEngine.start();
                case "pause" -> simulationEngine.pause();
                case "step" -> simulationEngine.step();
                case "reset" -> simulationEngine.reset();
                case "speed" -> {
                    Number spd = (Number) command.get("value");
                    if (spd != null) simulationEngine.setSpeed(spd.intValue());
                }
                case "whatif" -> {
                    String scenario = (String) command.get("scenario");
                    if (scenario != null) {
                        simulationEngine.getWhatIfEngine().applyScenario(
                                scenario,
                                simulationEngine.getCityGraph(),
                                simulationEngine.getAgentEngine(),
                                simulationEngine.getEconomyEngine(),
                                simulationEngine.getEventEngine()
                        );
                        simulationEngine.step();
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    public void broadcastStatePacket(Map<String, Object> packet) {
        if (sessions.isEmpty()) return;

        try {
            String json = objectMapper.writeValueAsString(Map.of("type", "TICK_UPDATE", "data", packet));
            TextMessage textMessage = new TextMessage(json);

            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        synchronized (session) {
                            session.sendMessage(textMessage);
                        }
                    } catch (IOException ignored) {
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }
}
