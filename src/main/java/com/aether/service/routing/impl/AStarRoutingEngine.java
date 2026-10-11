package com.aether.service.routing.impl;

import com.aether.model.city.CityGraph;
import com.aether.model.city.CityNode;
import com.aether.model.city.RoadSegment;
import com.aether.model.city.Route;
import com.aether.service.routing.RoutingEngine;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * MOD-03 RoutingEngine Implementation using the A* (A-Star) Pathfinding Algorithm.
 * Dynamically adjusts route costs based on live road congestion and avoids closed roads.
 */
@Service
public class AStarRoutingEngine implements RoutingEngine {

    private static final double MAX_SPEED_MPS = (90.0 * 1000.0) / 3600.0; // 25 m/s

    private static class NodeRecord implements Comparable<NodeRecord> {
        final String nodeId;
        final double gCost; // Actual travel time from start
        final double fCost; // gCost + hCost
        final RoadSegment incomingRoad;
        final NodeRecord parent;

        NodeRecord(String nodeId, double gCost, double fCost, RoadSegment incomingRoad, NodeRecord parent) {
            this.nodeId = nodeId;
            this.gCost = gCost;
            this.fCost = fCost;
            this.incomingRoad = incomingRoad;
            this.parent = parent;
        }

        @Override
        public int compareTo(NodeRecord other) {
            return Double.compare(this.fCost, other.fCost);
        }
    }

    @Override
    public Optional<Route> findRoute(CityGraph graph, String startNodeId, String endNodeId) {
        if (graph == null || startNodeId == null || endNodeId == null) {
            return Optional.empty();
        }

        CityNode startNode = graph.getNode(startNodeId);
        CityNode endNode = graph.getNode(endNodeId);

        if (startNode == null || endNode == null) {
            return Optional.empty();
        }

        if (startNodeId.equals(endNodeId)) {
            return Optional.of(new Route(List.of(startNode), Collections.emptyList(), 0.0, 0.0));
        }

        PriorityQueue<NodeRecord> openQueue = new PriorityQueue<>();
        Map<String, Double> bestCostToNode = new HashMap<>();
        Set<String> closedSet = new HashSet<>();

        double initialH = heuristic(startNode, endNode);
        openQueue.add(new NodeRecord(startNodeId, 0.0, initialH, null, null));
        bestCostToNode.put(startNodeId, 0.0);

        while (!openQueue.isEmpty()) {
            NodeRecord current = openQueue.poll();

            if (current.nodeId.equals(endNodeId)) {
                return Optional.of(reconstructRoute(graph, current));
            }

            if (closedSet.contains(current.nodeId)) {
                continue;
            }
            closedSet.add(current.nodeId);

            List<RoadSegment> outgoing = graph.getOutgoingRoads(current.nodeId);
            for (RoadSegment road : outgoing) {
                if (road.isClosed()) {
                    continue; // Skip roads closed in What-If scenarios or disruptions
                }

                String neighborId = road.getToNodeId();
                if (closedSet.contains(neighborId)) {
                    continue;
                }

                CityNode neighborNode = graph.getNode(neighborId);
                if (neighborNode == null) {
                    continue;
                }

                double travelTime = road.getTravelTimeSeconds();
                if (Double.isInfinite(travelTime) || Double.isNaN(travelTime)) {
                    continue;
                }

                double tentativeG = current.gCost + travelTime;
                double currentBestG = bestCostToNode.getOrDefault(neighborId, Double.MAX_VALUE);

                if (tentativeG < currentBestG) {
                    bestCostToNode.put(neighborId, tentativeG);
                    double h = heuristic(neighborNode, endNode);
                    double f = tentativeG + h;
                    openQueue.add(new NodeRecord(neighborId, tentativeG, f, road, current));
                }
            }
        }

        return Optional.empty(); // No path found (e.g., partitioned due to closures)
    }

    private double heuristic(CityNode a, CityNode b) {
        double dist = a.distanceTo(b);
        return dist / MAX_SPEED_MPS; // Admissible lower bound on time
    }

    private Route reconstructRoute(CityGraph graph, NodeRecord goalRecord) {
        LinkedList<CityNode> nodes = new LinkedList<>();
        LinkedList<RoadSegment> roads = new LinkedList<>();
        double totalDistance = 0.0;
        double totalTime = goalRecord.gCost;

        NodeRecord curr = goalRecord;
        while (curr != null) {
            CityNode node = graph.getNode(curr.nodeId);
            if (node != null) {
                nodes.addFirst(node);
            }
            if (curr.incomingRoad != null) {
                roads.addFirst(curr.incomingRoad);
                totalDistance += curr.incomingRoad.getLengthMeters();
            }
            curr = curr.parent;
        }

        return new Route(nodes, roads, totalDistance, totalTime);
    }
}
