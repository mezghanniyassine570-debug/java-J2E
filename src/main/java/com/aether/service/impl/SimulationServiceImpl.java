package com.aether.service.impl;

import com.aether.engine.SimulationEngine;
import com.aether.model.SimulationEvent;
import com.aether.service.SimulationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SimulationServiceImpl implements SimulationService {

    private final SimulationEngine simulationEngine;

    @Autowired
    public SimulationServiceImpl(SimulationEngine simulationEngine) {
        this.simulationEngine = simulationEngine;
    }

    @Override
    public void startSimulation() {
        simulationEngine.start();
    }

    @Override
    public void pauseSimulation() {
        simulationEngine.pause();
    }

    @Override
    public void resetSimulation() {
        simulationEngine.reset();
    }

    @Override
    public boolean isRunning() {
        return simulationEngine.isRunning();
    }

    @Override
    public void stepSimulation() {
        simulationEngine.step();
    }

    @Override
    public void setSpeed(int speedMultiplier) {
        simulationEngine.setSpeed(speedMultiplier);
    }

    @Override
    public void triggerWhatIfScenario(String scenarioName) {
        simulationEngine.getWhatIfEngine().applyScenario(
                scenarioName,
                simulationEngine.getCityGraph(),
                simulationEngine.getAgentEngine(),
                simulationEngine.getEconomyEngine(),
                simulationEngine.getEventEngine()
        );
    }

    @Override
    public List<SimulationEvent> getRecentEvents() {
        return simulationEngine.getEventEngine().getRecentEvents(50);
    }

    @Override
    public SimulationEngine getEngine() {
        return simulationEngine;
    }
}
