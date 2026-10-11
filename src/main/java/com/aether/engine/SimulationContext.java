package com.aether.engine;

import com.aether.model.city.CityGraph;
import com.aether.service.routing.RoutingEngine;

/**
 * Provides execution context and environment access to agents during simulation ticks.
 */
public interface SimulationContext {
    SimulationClock getClock();
    CityGraph getCityGraph();
    RoutingEngine getRoutingEngine();
    EventEngine getEventEngine();
    double getCongestionMultiplier();
    double getEconomicVitality();
}
