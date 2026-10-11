package com.aether.engine;

import com.aether.model.Inspectable;
import com.aether.model.agent.VehicleAgent;
import com.aether.model.city.CityGraph;
import com.aether.service.routing.RoutingEngine;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Master Simulation Engine for AETHER Lite.
 * Coordinates the city topology, multi-agent life cycle, A* routing, traffic,
 * economic flows, What-If branching, and WebSocket broadcast pulses.
 */
@Component
public class SimulationEngine implements SimulationContext {

    private final CityManager cityManager;
    private final AgentEngine agentEngine;
    private final RoutingEngine routingEngine;
    private final TrafficEngine trafficEngine;
    private final EconomyEngine economyEngine;
    private final EventEngine eventEngine;
    private final WhatIfEngine whatIfEngine;
    private final SimulationClock clock = new SimulationClock();

    private final AtomicBoolean running = new AtomicBoolean(false);
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> currentTask;

    private final List<Consumer<Map<String, Object>>> tickListeners = new CopyOnWriteArrayList<>();

    @Autowired
    public SimulationEngine(CityManager cityManager,
                            AgentEngine agentEngine,
                            RoutingEngine routingEngine,
                            TrafficEngine trafficEngine,
                            EconomyEngine economyEngine,
                            EventEngine eventEngine,
                            WhatIfEngine whatIfEngine) {
        this.cityManager = cityManager;
        this.agentEngine = agentEngine;
        this.routingEngine = routingEngine;
        this.trafficEngine = trafficEngine;
        this.economyEngine = economyEngine;
        this.eventEngine = eventEngine;
        this.whatIfEngine = whatIfEngine;
    }

    @PostConstruct
    public void init() {
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Aether-Sim-Core");
            t.setDaemon(true);
            return t;
        });

        // Initialize City and Seed Agents
        cityManager.initializeNeoMetropolisTopology();
        agentEngine.populateAgents(cityManager.getCityGraph());

        // Capture initial baseline snapshot
        SimulationSnapshot baseline = whatIfEngine.takeSnapshot("BASELINE", clock, trafficEngine, economyEngine, agentEngine);
        whatIfEngine.setBaselineSnapshot(baseline);

        eventEngine.emit("SYSTEM_READY", "AETHER Urban Simulation Core initialized with Neo-Metropolis topology.", "INFO");
    }

    @PreDestroy
    public void shutdown() {
        pause();
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
    }

    public synchronized void start() {
        if (running.compareAndSet(false, true)) {
            scheduleLoop();
            eventEngine.emit("SIMULATION_STARTED", "Simulation cycle started.", "INFO");
            notifyTickListeners();
        }
    }

    public synchronized void pause() {
        if (running.compareAndSet(true, false)) {
            if (currentTask != null) {
                currentTask.cancel(false);
            }
            eventEngine.emit("SIMULATION_PAUSED", "Simulation cycle paused.", "INFO");
            notifyTickListeners();
        }
    }

    public synchronized void step() {
        // Execute a single discrete simulation tick
        executeTick();
    }

    public synchronized void reset() {
        pause();
        clock.reset();
        cityManager.initializeNeoMetropolisTopology();
        agentEngine.populateAgents(cityManager.getCityGraph());
        whatIfEngine.resetToBaseline(cityManager.getCityGraph(), agentEngine, economyEngine, eventEngine);

        SimulationSnapshot baseline = whatIfEngine.takeSnapshot("BASELINE", clock, trafficEngine, economyEngine, agentEngine);
        whatIfEngine.setBaselineSnapshot(baseline);

        eventEngine.emit("SIMULATION_RESET", "Simulation reset to initial conditions.", "WARNING");
        notifyTickListeners();
    }

    public synchronized void setSpeed(int speedMultiplier) {
        clock.setSpeedMultiplier(speedMultiplier);
        if (running.get()) {
            if (currentTask != null) currentTask.cancel(false);
            scheduleLoop();
        }
        notifyTickListeners();
    }

    private void scheduleLoop() {
        int delayMs = switch (clock.getSpeedMultiplier()) {
            case 2 -> 500;
            case 5 -> 200;
            case 10 -> 100;
            default -> 1000;
        };

        currentTask = scheduler.scheduleWithFixedDelay(this::executeTickSafely, 0, delayMs, TimeUnit.MILLISECONDS);
    }

    private void executeTickSafely() {
        try {
            executeTick();
        } catch (Throwable t) {
            // NFR-05: Simulation robustness
            System.err.println("Error during simulation tick: " + t.getMessage());
        }
    }

    /**
     * Executes one complete simulation cycle across all modules.
     */
    public synchronized void executeTick() {
        // 1. Advance simulation clock
        clock.advanceTick();

        // 2. Concurrently step all agents (Citizens, Vehicles, Businesses)
        agentEngine.stepAllAgents(this);

        // 3. Update Traffic calculations
        trafficEngine.updateTrafficMetrics(cityManager.getCityGraph(), eventEngine);

        // 4. Update Economic calculations
        economyEngine.updateEconomicMetrics(agentEngine.getAllCitizens(), agentEngine.getAllBusinesses(), eventEngine);

        // 5. Broadcast live state to WebSocket / UI
        notifyTickListeners();
    }

    public void registerTickListener(Consumer<Map<String, Object>> listener) {
        tickListeners.add(listener);
    }

    public void unregisterTickListener(Consumer<Map<String, Object>> listener) {
        tickListeners.remove(listener);
    }

    private void notifyTickListeners() {
        Map<String, Object> statePacket = getSimulationStatePacket();
        for (Consumer<Map<String, Object>> listener : tickListeners) {
            try {
                listener.accept(statePacket);
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Builds lightweight JSON-friendly snapshot packet for the UI.
     */
    public Map<String, Object> getSimulationStatePacket() {
        Map<String, Object> packet = new LinkedHashMap<>();
        packet.put("tick", clock.getTick());
        packet.put("formattedTime", clock.getFormattedTime());
        packet.put("timePeriod", clock.getTimePeriodLabel());
        packet.put("hour", clock.getHour());
        packet.put("minute", clock.getMinute());
        packet.put("speedMultiplier", clock.getSpeedMultiplier());
        packet.put("isRunning", running.get());
        packet.put("activeScenario", whatIfEngine.getActiveScenario());

        // Traffic KPIs
        packet.put("congestionRatio", Math.round(trafficEngine.getAverageCongestionRatio() * 1000.0) / 10.0);
        packet.put("avgSpeedKmH", Math.round(trafficEngine.getAverageNetworkSpeedKmH() * 10.0) / 10.0);
        packet.put("levelOfService", trafficEngine.getLevelOfService());

        // Agent & Economic KPIs
        packet.put("citizenCount", agentEngine.getAllCitizens().size());
        packet.put("vehicleCount", agentEngine.getAllVehicles().size());
        packet.put("businessCount", agentEngine.getAllBusinesses().size());
        packet.put("avgSatisfaction", Math.round(agentEngine.getAverageCitizenSatisfaction() * 10.0) / 10.0);
        packet.put("employmentRate", Math.round(economyEngine.getEmploymentRate() * 10.0) / 10.0);
        packet.put("grossCityProduct", Math.round(economyEngine.getGrossCityProduct()));
        packet.put("economicVitality", Math.round(economyEngine.getEconomicVitality() * 100.0) / 100.0);

        // Sample Vehicle Positions for 2D animated rendering (lightweight)
        List<Map<String, Object>> vehiclePositions = new ArrayList<>();
        for (VehicleAgent v : agentEngine.getAllVehicles()) {
            if (!v.isActive()) continue;
            vehiclePositions.add(Map.of(
                    "id", v.getId(),
                    "x", Math.round(v.getPosX() * 10.0) / 10.0,
                    "y", Math.round(v.getPosY() * 10.0) / 10.0,
                    "speed", Math.round(v.getCurrentSpeed()),
                    "road", (v.getCurrentlyOccupiedRoad() != null) ? v.getCurrentlyOccupiedRoad().getId() : ""
            ));
        }
        packet.put("vehicles", vehiclePositions);

        // Road Congestion Map (for coloring canvas roads in real time)
        Map<String, Object> roadCongestion = new HashMap<>();
        cityManager.getCityGraph().getAllRoads().forEach(r -> {
            roadCongestion.put(r.getId(), Map.of(
                    "congestion", Math.round(r.getCongestionRatio() * 100.0) / 100.0,
                    "speed", Math.round(r.getEffectiveSpeedKmH()),
                    "vehicles", r.getCurrentVehicles(),
                    "closed", r.isClosed()
            ));
        });
        packet.put("roads", roadCongestion);

        // Recent Events
        packet.put("events", eventEngine.getRecentEvents(12));

        return packet;
    }

    public Inspectable inspect(String id) {
        return agentEngine.findInspectable(id, cityManager.getCityGraph());
    }

    public boolean isRunning() { return running.get(); }

    // SimulationContext implementation
    @Override public SimulationClock getClock() { return clock; }
    @Override public CityGraph getCityGraph() { return cityManager.getCityGraph(); }
    @Override public RoutingEngine getRoutingEngine() { return routingEngine; }
    @Override public EventEngine getEventEngine() { return eventEngine; }
    @Override public double getCongestionMultiplier() { return trafficEngine.getCongestionPenaltyMultiplier(); }
    @Override public double getEconomicVitality() { return economyEngine.getEconomicVitality(); }

    // Getters for subsystems
    public CityManager getCityManager() { return cityManager; }
    public AgentEngine getAgentEngine() { return agentEngine; }
    public TrafficEngine getTrafficEngine() { return trafficEngine; }
    public EconomyEngine getEconomyEngine() { return economyEngine; }
    public WhatIfEngine getWhatIfEngine() { return whatIfEngine; }
}
