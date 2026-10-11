package com.aether.engine;

import com.aether.model.city.*;
import com.aether.model.city.Building.BuildingType;
import com.aether.model.city.CityNode.NodeType;
import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * MOD-01 CityManager.
 * Creates and manages the city topology, neighborhoods, road network, and buildings.
 */
@Component
public class CityManager {

    private final CityGraph cityGraph = new CityGraph();

    public CityManager() {
        initializeNeoMetropolisTopology();
    }

    public CityGraph getCityGraph() {
        return cityGraph;
    }

    public void initializeNeoMetropolisTopology() {
        cityGraph.clear();

        // 1. Urban Neighborhoods / Districts
        cityGraph.addNeighborhood(new Neighborhood(
                "DIST-DOWNTOWN", "Downtown Central & Financial", "Commercial skyscrapers and financial institutions",
                "#38bdf8", 300, 300, 600, 600
        ));
        cityGraph.addNeighborhood(new Neighborhood(
                "DIST-TECH", "Silicon Tech Corridor", "R&D centers, incubators, and smart offices",
                "#a855f7", 500, 200, 850, 450
        ));
        cityGraph.addNeighborhood(new Neighborhood(
                "DIST-NORTH-RES", "Uptown Green Hills", "Residential communities, parks, and schools",
                "#22c55e", 100, 100, 500, 300
        ));
        cityGraph.addNeighborhood(new Neighborhood(
                "DIST-SOUTH-RES", "Bayside Residential Suburb", "Waterfront apartments and recreational docks",
                "#34d399", 100, 650, 450, 850
        ));
        cityGraph.addNeighborhood(new Neighborhood(
                "DIST-INDUSTRIAL", "Harbor Logistics & Industry", "Heavy manufacturing, freight terminals, and warehouses",
                "#f59e0b", 600, 500, 900, 850
        ));

        // 2. City Intersections / Hub Nodes (Coordinates scaled for 900x700 canvas)
        addNode("N-UPTOWN-1", "Uptown North West", 220, 140, "DIST-NORTH-RES", NodeType.RESIDENTIAL_HUB);
        addNode("N-UPTOWN-2", "Uptown Green Park", 450, 140, "DIST-NORTH-RES", NodeType.RESIDENTIAL_HUB);
        addNode("N-AIRPORT", "Airport Gateway", 750, 140, "DIST-TECH", NodeType.TRANSIT_STATION);

        addNode("N-WEST-GATE", "West Gate Junction", 150, 340, "DIST-NORTH-RES", NodeType.INTERSECTION);
        addNode("N-CENTRAL-PLAZA", "Central City Plaza", 380, 340, "DIST-DOWNTOWN", NodeType.CIVIC_CENTER);
        addNode("N-TECH-HUB", "Tech Nexus Hub", 600, 340, "DIST-TECH", NodeType.COMMERCIAL_HUB);
        addNode("N-EAST-EXPRESS", "East Expressway Loop", 820, 340, "DIST-TECH", NodeType.INTERSECTION);

        addNode("N-FINANCIAL", "Financial District Cross", 380, 520, "DIST-DOWNTOWN", NodeType.COMMERCIAL_HUB);
        addNode("N-CIVIC-HALL", "Civic Hall & Courts", 600, 520, "DIST-DOWNTOWN", NodeType.CIVIC_CENTER);
        addNode("N-HARBOR-GATE", "Harbor Logistics Gate", 800, 520, "DIST-INDUSTRIAL", NodeType.INDUSTRIAL_HUB);

        addNode("N-BAYSIDE-1", "Bayside Pier West", 180, 720, "DIST-SOUTH-RES", NodeType.RESIDENTIAL_HUB);
        addNode("N-METRO-SOUTH", "South Central Metro", 380, 720, "DIST-SOUTH-RES", NodeType.TRANSIT_STATION);
        addNode("N-IND-PARK", "Industrial Park East", 620, 720, "DIST-INDUSTRIAL", NodeType.INDUSTRIAL_HUB);
        addNode("N-HARBOR-DOCK", "Deepwater Freight Dock", 840, 720, "DIST-INDUSTRIAL", NodeType.INDUSTRIAL_HUB);

        // 3. Bidirectional Road Segments
        // Horizontal corridors
        addBiRoad("RD-UPTOWN-CORRIDOR", "Uptown Promenade", "N-UPTOWN-1", "N-UPTOWN-2", 400, 50, 18, 2);
        addBiRoad("RD-AIRPORT-LINK", "Airport Highway Link", "N-UPTOWN-2", "N-AIRPORT", 550, 70, 24, 3);

        addBiRoad("RD-WEST-AVENUE", "West Boulevard", "N-WEST-GATE", "N-CENTRAL-PLAZA", 420, 50, 20, 2);
        addBiRoad("RD-INNOVATION-WAY", "Innovation Way", "N-CENTRAL-PLAZA", "N-TECH-HUB", 400, 50, 22, 2);
        addBiRoad("RD-TECH-FREEWAY", "Tech Freeway East", "N-TECH-HUB", "N-EAST-EXPRESS", 400, 60, 22, 2);

        // Primary Arterial Road: Targeted for What-If scenario closure
        addBiRoad("RD-DOWNTOWN-CENTRAL", "Avenue des Nations (Downtown)", "N-CENTRAL-PLAZA", "N-FINANCIAL", 350, 45, 26, 3);

        addBiRoad("RD-FINANCIAL-LINK", "Financial Exchange Way", "N-FINANCIAL", "N-CIVIC-HALL", 400, 50, 20, 2);
        addBiRoad("RD-HARBOR-FEEDER", "Harbor Access Boulevard", "N-CIVIC-HALL", "N-HARBOR-GATE", 380, 55, 20, 2);

        addBiRoad("RD-BAYSIDE-COAST", "Bayside Coastal Road", "N-BAYSIDE-1", "N-METRO-SOUTH", 380, 45, 16, 2);
        addBiRoad("RD-SOUTH-CROSSWAY", "South Industrial Cross", "N-METRO-SOUTH", "N-IND-PARK", 420, 50, 20, 2);
        addBiRoad("RD-DOCK-SPUR", "Deepwater Dock Spur", "N-IND-PARK", "N-HARBOR-DOCK", 390, 45, 18, 2);

        // Vertical Arterials
        addBiRoad("RD-WEST-CONNECTOR", "West Connector", "N-UPTOWN-1", "N-WEST-GATE", 370, 50, 16, 2);
        addBiRoad("RD-BAYSIDE-LINK", "Bayside Highway", "N-WEST-GATE", "N-BAYSIDE-1", 420, 50, 16, 2);

        addBiRoad("RD-UPTOWN-CENTRAL", "Uptown-Central Spine", "N-UPTOWN-2", "N-CENTRAL-PLAZA", 370, 50, 20, 2);
        addBiRoad("RD-FINANCIAL-SOUTH", "Metro-Financial Link", "N-FINANCIAL", "N-METRO-SOUTH", 370, 50, 22, 2);

        addBiRoad("RD-TECH-CIVIC", "Tech-Civic Connector", "N-TECH-HUB", "N-CIVIC-HALL", 350, 50, 20, 2);
        addBiRoad("RD-CIVIC-SOUTH", "Civic-Industrial Arterial", "N-CIVIC-HALL", "N-IND-PARK", 370, 50, 20, 2);

        addBiRoad("RD-AIRPORT-EXPRESS", "Eastern Airport Express", "N-AIRPORT", "N-EAST-EXPRESS", 370, 70, 25, 3);
        addBiRoad("RD-HARBOR-EXPRESS", "Harbor Freight Arterial", "N-EAST-EXPRESS", "N-HARBOR-GATE", 350, 60, 24, 3);
        addBiRoad("RD-DOCK-FREEWAY", "Port Freight Way", "N-HARBOR-GATE", "N-HARBOR-DOCK", 370, 50, 20, 2);

        // 4. Urban Buildings & Amenities
        addBuilding("BLD-RES-TOWER-A", "Skyline Residential Tower", BuildingType.RESIDENTIAL, "DIST-NORTH-RES", "N-UPTOWN-1", 200, 120, 200);
        addBuilding("BLD-RES-GREEN-1", "Green Hills Condominiums", BuildingType.RESIDENTIAL, "DIST-NORTH-RES", "N-UPTOWN-2", 430, 120, 180);
        addBuilding("BLD-RES-BAY-VILLAS", "Bayside Seaside Villas", BuildingType.RESIDENTIAL, "DIST-SOUTH-RES", "N-BAYSIDE-1", 160, 700, 150);
        addBuilding("BLD-RES-METRO-LOFTS", "Metro South Urban Lofts", BuildingType.RESIDENTIAL, "DIST-SOUTH-RES", "N-METRO-SOUTH", 360, 700, 220);

        addBuilding("BLD-TECH-NEXUS", "Quantum Tech Campus", BuildingType.COMMERCIAL, "DIST-TECH", "N-TECH-HUB", 620, 320, 350);
        addBuilding("BLD-AI-LABS", "Aether Neural Systems Lab", BuildingType.COMMERCIAL, "DIST-TECH", "N-EAST-EXPRESS", 840, 320, 200);
        addBuilding("BLD-FIN-TOWER", "Metropolis World Trade Tower", BuildingType.COMMERCIAL, "DIST-DOWNTOWN", "N-FINANCIAL", 400, 500, 400);
        addBuilding("BLD-CENTRAL-MALL", "Grand Central Plaza Galleria", BuildingType.COMMERCIAL, "DIST-DOWNTOWN", "N-CENTRAL-PLAZA", 360, 320, 300);

        addBuilding("BLD-CITY-HALL", "Neo-Metropolis City Hall", BuildingType.CIVIC, "DIST-DOWNTOWN", "N-CIVIC-HALL", 580, 500, 150);
        addBuilding("BLD-HOSPITAL", "Metropolis General Medical Center", BuildingType.HOSPITAL, "DIST-DOWNTOWN", "N-CENTRAL-PLAZA", 410, 360, 250);
        addBuilding("BLD-UNIVERSITY", "Polytechnic Institute of Urban AI", BuildingType.SCHOOL, "DIST-NORTH-RES", "N-UPTOWN-2", 470, 160, 300);
        addBuilding("BLD-PORT-CONTAINER", "Terminal 4 Port & Logistics", BuildingType.INDUSTRIAL, "DIST-INDUSTRIAL", "N-HARBOR-DOCK", 860, 700, 300);
        addBuilding("BLD-MFG-PLANT", "Advanced Robotics Foundry", BuildingType.INDUSTRIAL, "DIST-INDUSTRIAL", "N-IND-PARK", 640, 700, 250);
    }

    private void addNode(String id, String name, double x, double y, String district, NodeType type) {
        cityGraph.addNode(new CityNode(id, name, x, y, district, type));
    }

    private void addBiRoad(String idPrefix, String name, String nodeA, String nodeB,
                           double lengthMeters, double speedKmH, int capacity, int lanes) {
        cityGraph.addRoad(new RoadSegment(idPrefix + "-FWD", name + " [North/East]", nodeA, nodeB, lengthMeters, speedKmH, capacity, lanes));
        cityGraph.addRoad(new RoadSegment(idPrefix + "-REV", name + " [South/West]", nodeB, nodeA, lengthMeters, speedKmH, capacity, lanes));
    }

    private void addBuilding(String id, String name, BuildingType type, String district,
                            String nearestNodeId, double x, double y, int capacity) {
        cityGraph.addBuilding(new Building(id, name, type, district, nearestNodeId, x, y, capacity));
    }
}
