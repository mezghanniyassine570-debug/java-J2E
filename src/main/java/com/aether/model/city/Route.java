package com.aether.model.city;

import java.util.Collections;
import java.util.List;

/**
 * Encapsulates a navigation path calculated by the RoutingEngine (MOD-03).
 */
public class Route {

    private final List<CityNode> nodes;
    private final List<RoadSegment> roads;
    private final double totalDistanceMeters;
    private final double estimatedTravelTimeSeconds;

    public Route(List<CityNode> nodes, List<RoadSegment> roads,
                 double totalDistanceMeters, double estimatedTravelTimeSeconds) {
        this.nodes = nodes;
        this.roads = roads;
        this.totalDistanceMeters = totalDistanceMeters;
        this.estimatedTravelTimeSeconds = estimatedTravelTimeSeconds;
    }

    public List<CityNode> getNodes() {
        return Collections.unmodifiableList(nodes);
    }

    public List<RoadSegment> getRoads() {
        return Collections.unmodifiableList(roads);
    }

    public double getTotalDistanceMeters() {
        return totalDistanceMeters;
    }

    public double getEstimatedTravelTimeSeconds() {
        return estimatedTravelTimeSeconds;
    }

    public boolean isEmpty() {
        return roads.isEmpty();
    }
}
