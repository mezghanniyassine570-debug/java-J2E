package com.aether.routing;

import com.aether.model.city.CityGraph;
import com.aether.model.city.CityNode;
import com.aether.model.city.RoadSegment;
import com.aether.model.city.Route;
import com.aether.service.routing.impl.AStarRoutingEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AStarRoutingEngineTest {

    private AStarRoutingEngine routingEngine;
    private CityGraph graph;

    @BeforeEach
    void setUp() {
        routingEngine = new AStarRoutingEngine();
        graph = new CityGraph();

        // Build a triangle network: A -> B -> C and direct A -> C
        // A (0,0) -> B (100, 0) -> C (100, 100)
        // A (0,0) -> C (100, 100)
        graph.addNode(new CityNode("A", "Node A", 0, 0, "D1", CityNode.NodeType.INTERSECTION));
        graph.addNode(new CityNode("B", "Node B", 100, 0, "D1", CityNode.NodeType.INTERSECTION));
        graph.addNode(new CityNode("C", "Node C", 100, 100, "D1", CityNode.NodeType.INTERSECTION));

        graph.addRoad(new RoadSegment("RD-AB", "A to B", "A", "B", 100, 50, 10, 1));
        graph.addRoad(new RoadSegment("RD-BC", "B to C", "B", "C", 100, 50, 10, 1));
        graph.addRoad(new RoadSegment("RD-AC", "A to C direct", "A", "C", 141, 50, 10, 1));
    }

    @Test
    void testFindDirectShortestRoute() {
        Optional<Route> routeOpt = routingEngine.findRoute(graph, "A", "C");
        assertTrue(routeOpt.isPresent(), "Route should be found from A to C");
        Route route = routeOpt.get();

        // Direct AC has distance 141m, which is faster than AB + BC (200m)
        assertEquals(1, route.getRoads().size());
        assertEquals("RD-AC", route.getRoads().get(0).getId());
    }

    @Test
    void testReroutingAroundClosedRoad() {
        // Close direct road AC (simulate What-If road closure)
        RoadSegment directRoad = graph.getRoad("RD-AC");
        assertNotNull(directRoad);
        directRoad.setClosed(true);

        Optional<Route> routeOpt = routingEngine.findRoute(graph, "A", "C");
        assertTrue(routeOpt.isPresent(), "Alternative route should be found via B");
        Route route = routeOpt.get();

        assertEquals(2, route.getRoads().size());
        assertEquals("RD-AB", route.getRoads().get(0).getId());
        assertEquals("RD-BC", route.getRoads().get(1).getId());
    }

    @Test
    void testSameNodeReturnsEmptyRoadRoute() {
        Optional<Route> routeOpt = routingEngine.findRoute(graph, "A", "A");
        assertTrue(routeOpt.isPresent());
        assertTrue(routeOpt.get().isEmpty());
    }
}
