package com.aether.model.city;

import com.aether.model.Inspectable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents an urban district or neighborhood with zoning profile and visual bounds.
 */
public class Neighborhood implements Inspectable {

    private final String id;
    private final String name;
    private final String description;
    private final String colorHex;
    private final double minX;
    private final double minY;
    private final double maxX;
    private final double maxY;

    public Neighborhood(String id, String name, String description, String colorHex,
                        double minX, double minY, double maxX, double maxY) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.colorHex = colorHex;
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getColorHex() { return colorHex; }
    public double getMinX() { return minX; }
    public double getMinY() { return minY; }
    public double getMaxX() { return maxX; }
    public double getMaxY() { return maxY; }

    @Override
    public String getInspectableId() { return id; }

    @Override
    public String getInspectableName() { return name; }

    @Override
    public String getInspectableType() { return "NEIGHBORHOOD"; }

    @Override
    public Map<String, Object> getInspectionDetails() {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("District ID", id);
        details.put("Name", name);
        details.put("Profile", description);
        details.put("Bounds", String.format("[X: %.0f-%.0f, Y: %.0f-%.0f]", minX, maxX, minY, maxY));
        return details;
    }
}
