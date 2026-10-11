package com.aether.service;

import com.aether.model.SimulationEvent;
import java.util.List;

public interface SimulationService {
    void startSimulation();
    void pauseSimulation();
    void resetSimulation();
    boolean isRunning();
    void triggerWhatIfScenario(String scenarioName);
    List<SimulationEvent> getRecentEvents();
    void stepSimulation();
    void setSpeed(int speedMultiplier);
    com.aether.engine.SimulationEngine getEngine();
}
