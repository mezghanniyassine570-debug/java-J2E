package com.aether.model.agent;

import com.aether.engine.SimulationContext;
import com.aether.model.Agent.AgentType;
import com.aether.model.city.CityGraph;
import com.aether.model.city.CityNode;
import com.aether.model.city.RoadSegment;
import com.aether.model.city.Route;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Appendix B Conceptual UML - VehicleAgent.
 * Navigates the urban road graph using A* pathfinding, adapts speed to road congestion,
 * and dynamically reroutes when encountering road closures.
 */
public class VehicleAgent extends AbstractAgent {

    private double currentSpeed; // km/h
    private final double maxSpeed; // km/h
    private String originNodeId;
    private String destinationNodeId;
    private Route activeRoute;
    private int currentRoadIndex = 0;
    private double segmentProgress = 0.0; // 0.0 to 1.0 along active road segment
    private int rerouteCount = 0;
    private RoadSegment currentlyOccupiedRoad = null;

    public VehicleAgent(String id, String name, double maxSpeed,
                        String originNodeId, String destinationNodeId) {
        super(id, name, AgentType.VEHICLE, 0.0, 0.0, "INITIALIZING", 90.0);
        this.maxSpeed = maxSpeed;
        this.currentSpeed = maxSpeed;
        this.originNodeId = originNodeId;
        this.destinationNodeId = destinationNodeId;
    }

    public double getCurrentSpeed() { return currentSpeed; }
    public double getMaxSpeed() { return maxSpeed; }
    public String getOriginNodeId() { return originNodeId; }
    public String getDestinationNodeId() { return destinationNodeId; }
    public Route getActiveRoute() { return activeRoute; }
    public int getRerouteCount() { return rerouteCount; }

    public RoadSegment getCurrentlyOccupiedRoad() {
        return currentlyOccupiedRoad;
    }

    @Override
    public void step(SimulationContext context) {
        if (!active) return;

        CityGraph graph = context.getCityGraph();

        // 1. If no active route or completed, compute new route
        if (activeRoute == null || activeRoute.isEmpty() || currentRoadIndex >= activeRoute.getRoads().size()) {
            assignNewDestination(context);
            return;
        }

        // 2. Check current road segment
        RoadSegment currentRoad = activeRoute.getRoads().get(currentRoadIndex);

        // 3. Dynamic Rerouting if road is closed (What-If disruption)
        if (currentRoad.isClosed()) {
            reroute(context);
            return;
        }

        // Ensure vehicle is registered on current road segment
        if (currentlyOccupiedRoad != currentRoad) {
            if (currentlyOccupiedRoad != null) {
                currentlyOccupiedRoad.decrementVehicles();
            }
            currentlyOccupiedRoad = currentRoad;
            currentlyOccupiedRoad.incrementVehicles();
        }

        // 4. Calculate effective travel speed adjusted for road congestion
        double effectiveRoadSpeed = currentRoad.getEffectiveSpeedKmH();
        this.currentSpeed = Math.min(this.maxSpeed, effectiveRoadSpeed);

        // 5. Advance position along road segment
        // Step represents ~10-30 seconds of virtual movement
        double speedMps = (this.currentSpeed * 1000.0) / 3600.0;
        double distanceCoveredThisTick = speedMps * 5.0; // 5 virtual seconds
        double segmentLength = Math.max(10.0, currentRoad.getLengthMeters());
        double progressDelta = distanceCoveredThisTick / segmentLength;

        this.segmentProgress += progressDelta;

        // 6. Update spatial (x, y) coordinates for 2D visualization
        CityNode fromNode = graph.getNode(currentRoad.getFromNodeId());
        CityNode toNode = graph.getNode(currentRoad.getToNodeId());
        if (fromNode != null && toNode != null) {
            double clampedProgress = Math.min(1.0, Math.max(0.0, segmentProgress));
            this.posX = fromNode.getX() + (toNode.getX() - fromNode.getX()) * clampedProgress;
            this.posY = fromNode.getY() + (toNode.getY() - fromNode.getY()) * clampedProgress;
        }

        this.status = String.format("TRAVERSING %s (%.0f km/h)", currentRoad.getName(), this.currentSpeed);

        // 7. If reached end of road segment, advance to next road in route
        if (this.segmentProgress >= 1.0) {
            this.segmentProgress = 0.0;
            this.currentRoadIndex++;

            if (currentRoadIndex >= activeRoute.getRoads().size()) {
                // Arrived at destination
                if (currentlyOccupiedRoad != null) {
                    currentlyOccupiedRoad.decrementVehicles();
                    currentlyOccupiedRoad = null;
                }
                this.originNodeId = this.destinationNodeId;
                assignNewDestination(context);
            }
        }
    }

    /**
     * Re-routes the vehicle using A* pathfinding from current position.
     */
    public void reroute(SimulationContext context) {
        this.rerouteCount++;
        if (currentlyOccupiedRoad != null) {
            currentlyOccupiedRoad.decrementVehicles();
            currentlyOccupiedRoad = null;
        }

        // Find nearest accessible node from current coordinates
        CityNode nearestNode = context.getCityGraph().findNearestNode(this.posX, this.posY);
        String startId = (nearestNode != null) ? nearestNode.getId() : this.originNodeId;

        Optional<Route> newPath = context.getRoutingEngine().findRoute(
                context.getCityGraph(), startId, this.destinationNodeId
        );

        if (newPath.isPresent() && !newPath.get().isEmpty()) {
            this.activeRoute = newPath.get();
            this.currentRoadIndex = 0;
            this.segmentProgress = 0.0;
            this.status = "REROUTED via A* (Avoided road closure)";
        } else {
            // Pick a completely different destination if original destination is trapped
            assignNewDestination(context);
        }
    }

    private void assignNewDestination(SimulationContext context) {
        CityGraph graph = context.getCityGraph();
        List<CityNode> allNodes = new ArrayList<>(graph.getAllNodes());
        if (allNodes.size() < 2) return;

        // Ensure origin is set
        if (originNodeId == null) {
            originNodeId = allNodes.get((int) (Math.random() * allNodes.size())).getId();
            CityNode orig = graph.getNode(originNodeId);
            if (orig != null) {
                this.posX = orig.getX();
                this.posY = orig.getY();
            }
        }

        // Pick distinct destination
        CityNode randomDest = allNodes.get((int) (Math.random() * allNodes.size()));
        while (randomDest.getId().equals(originNodeId) && allNodes.size() > 1) {
            randomDest = allNodes.get((int) (Math.random() * allNodes.size()));
        }
        this.destinationNodeId = randomDest.getId();

        Optional<Route> path = context.getRoutingEngine().findRoute(graph, originNodeId, destinationNodeId);
        if (path.isPresent() && !path.get().isEmpty()) {
            this.activeRoute = path.get();
            this.currentRoadIndex = 0;
            this.segmentProgress = 0.0;
            this.status = "NAVIGATING TO " + randomDest.getName();
        }
    }

    public void cleanup() {
        if (currentlyOccupiedRoad != null) {
            currentlyOccupiedRoad.decrementVehicles();
            currentlyOccupiedRoad = null;
        }
    }

    @Override
    public Map<String, Object> getInspectionDetails() {
        Map<String, Object> details = super.getInspectionDetails();
        details.put("Speed (Current/Max)", String.format("%.1f / %.0f km/h", currentSpeed, maxSpeed));
        details.put("Origin Hub", originNodeId);
        details.put("Destination Hub", destinationNodeId);
        details.put("Current Road", (currentlyOccupiedRoad != null) ? currentlyOccupiedRoad.getName() : "In Transition");
        details.put("Segment Progress", String.format("%.1f%%", segmentProgress * 100));
        details.put("Reroute Operations", rerouteCount);
        details.put("Route Steps Remaining", (activeRoute != null) ? (activeRoute.getRoads().size() - currentRoadIndex) : 0);
        return details;
    }
}
