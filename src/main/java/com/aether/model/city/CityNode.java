package com.aether.model.city;

import com.aether.model.Inspectable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents a spatial intersection, terminal, or hub in the urban road graph.
 */
public class CityNode implements Inspectable {

    public enum NodeType {
        INTERSECTION,
        RESIDENTIAL_HUB,
        COMMERCIAL_HUB,
        INDUSTRIAL_HUB,
        CIVIC_CENTER,
        TRANSIT_STATION
    }

    private final String id;
    private final String name;
    private final double x;
    private final double y;
    private final String district;
    private final NodeType type;

    public CityNode(String id, String name, double x, double y, String district, NodeType type) {
        this.id = id;
        this.name = name;
        this.x = x;
        this.y = y;
        this.district = district;
        this.type = type;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public double getX() { return x; }
    public double getY() { return y; }
    public String getDistrict() { return district; }
    public NodeType getType() { return type; }

    public double distanceTo(CityNode other) {
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    @Override
    public String getInspectableId() { return id; }

    @Override
    public String getInspectableName() { return name; }

    @Override
    public String getInspectableType() { return "CITY_NODE (" + type + ")"; }

    @Override
    public Map<String, Object> getInspectionDetails() {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("Node ID", id);
        details.put("Name", name);
        details.put("District", district);
        details.put("Type", type.name());
        details.put("Coordinates", String.format("(%.1f, %.1f)", x, y));
        return details;
    }
}
