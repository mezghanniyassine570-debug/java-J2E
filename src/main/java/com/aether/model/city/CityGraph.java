package com.aether.model.city;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Graph representation of the urban road network and city topology.
 * Thread-safe for concurrent navigation lookups and dynamic state updates.
 */
public class CityGraph {

    private final Map<String, CityNode> nodes = new ConcurrentHashMap<>();
    private final Map<String, RoadSegment> roads = new ConcurrentHashMap<>();
    private final Map<String, List<RoadSegment>> outgoingRoads = new ConcurrentHashMap<>();
    private final Map<String, Building> buildings = new ConcurrentHashMap<>();
    private final Map<String, Neighborhood> neighborhoods = new ConcurrentHashMap<>();

    public void addNode(CityNode node) {
        nodes.put(node.getId(), node);
        outgoingRoads.putIfAbsent(node.getId(), Collections.synchronizedList(new ArrayList<>()));
    }

    public void addRoad(RoadSegment road) {
        roads.put(road.getId(), road);
        outgoingRoads.computeIfAbsent(road.getFromNodeId(), k -> Collections.synchronizedList(new ArrayList<>()))
                .add(road);
    }

    public void addBuilding(Building building) {
        buildings.put(building.getId(), building);
    }

    public void addNeighborhood(Neighborhood neighborhood) {
        neighborhoods.put(neighborhood.getId(), neighborhood);
    }

    public CityNode getNode(String id) {
        return nodes.get(id);
    }

    public RoadSegment getRoad(String id) {
        return roads.get(id);
    }

    public List<RoadSegment> getOutgoingRoads(String nodeId) {
        return outgoingRoads.getOrDefault(nodeId, Collections.emptyList());
    }

    public Building getBuilding(String id) {
        return buildings.get(id);
    }

    public Neighborhood getNeighborhood(String id) {
        return neighborhoods.get(id);
    }

    public Collection<CityNode> getAllNodes() {
        return Collections.unmodifiableCollection(nodes.values());
    }

    public Collection<RoadSegment> getAllRoads() {
        return Collections.unmodifiableCollection(roads.values());
    }

    public Collection<Building> getAllBuildings() {
        return Collections.unmodifiableCollection(buildings.values());
    }

    public Collection<Neighborhood> getAllNeighborhoods() {
        return Collections.unmodifiableCollection(neighborhoods.values());
    }

    /**
     * Finds the closest CityNode to arbitrary coordinates.
     */
    public CityNode findNearestNode(double x, double y) {
        CityNode best = null;
        double bestDist = Double.MAX_VALUE;
        for (CityNode node : nodes.values()) {
            double dx = node.getX() - x;
            double dy = node.getY() - y;
            double d = dx * dx + dy * dy;
            if (d < bestDist) {
                bestDist = d;
                best = node;
            }
        }
        return best;
    }

    public void clear() {
        nodes.clear();
        roads.clear();
        outgoingRoads.clear();
        buildings.clear();
        neighborhoods.clear();
    }
}
