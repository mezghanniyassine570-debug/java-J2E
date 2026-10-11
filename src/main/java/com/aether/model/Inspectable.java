package com.aether.model;

import java.util.Map;

/**
 * Appendix B Conceptual UML - Inspectable Interface.
 * Allows any simulation entity (agents, roads, buildings) to be dynamically inspected in the UI.
 */
public interface Inspectable {
    String getInspectableId();
    String getInspectableName();
    String getInspectableType();
    Map<String, Object> getInspectionDetails();
}
