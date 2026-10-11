package com.aether.model.city;

import com.aether.model.Inspectable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents urban structures (Homes, Offices, Markets, Civic Services).
 */
public class Building implements Inspectable {

    public enum BuildingType {
        RESIDENTIAL,
        COMMERCIAL,
        INDUSTRIAL,
        CIVIC,
        HOSPITAL,
        SCHOOL
    }

    private final String id;
    private final String name;
    private final BuildingType type;
    private final String district;
    private final String nearestNodeId;
    private final double x;
    private final double y;
    private final int capacity;
    private int occupantCount = 0;

    public Building(String id, String name, BuildingType type, String district,
                    String nearestNodeId, double x, double y, int capacity) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.district = district;
        this.nearestNodeId = nearestNodeId;
        this.x = x;
        this.y = y;
        this.capacity = capacity;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public BuildingType getType() { return type; }
    public String getDistrict() { return district; }
    public String getNearestNodeId() { return nearestNodeId; }
    public double getX() { return x; }
    public double getY() { return y; }
    public int getCapacity() { return capacity; }
    public int getOccupantCount() { return occupantCount; }

    public void setOccupantCount(int occupantCount) {
        this.occupantCount = occupantCount;
    }

    @Override
    public String getInspectableId() { return id; }

    @Override
    public String getInspectableName() { return name; }

    @Override
    public String getInspectableType() { return "BUILDING (" + type + ")"; }

    @Override
    public Map<String, Object> getInspectionDetails() {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("Building ID", id);
        details.put("Name", name);
        details.put("District", district);
        details.put("Type", type.name());
        details.put("Nearest Intersection", nearestNodeId);
        details.put("Coordinates", String.format("(%.1f, %.1f)", x, y));
        details.put("Capacity", capacity);
        details.put("Occupants", occupantCount);
        details.put("Occupancy Rate", capacity > 0 ? String.format("%.1f%%", (double) occupantCount / capacity * 100) : "N/A");
        return details;
    }
}
