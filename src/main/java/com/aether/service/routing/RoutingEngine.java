package com.aether.service.routing;

import com.aether.model.city.CityGraph;
import com.aether.model.city.Route;

import java.util.Optional;

/**
 * MOD-03 RoutingEngine interface.
 * Computes optimal routes using A* graph search considering road capacity and closures.
 */
public interface RoutingEngine {
    Optional<Route> findRoute(CityGraph graph, String startNodeId, String endNodeId);
}
