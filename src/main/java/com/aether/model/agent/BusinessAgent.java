package com.aether.model.agent;

import com.aether.engine.SimulationContext;
import com.aether.model.Agent.AgentType;

import java.util.Map;

/**
 * Appendix B Conceptual UML - BusinessAgent.
 * Models urban economic firms managing revenue, salaries, workforce hiring, and consumer demand (FR5).
 */
public class BusinessAgent extends AbstractAgent {

    public enum BusinessSector {
        TECHNOLOGY,
        COMMERCE,
        SERVICES,
        MANUFACTURING,
        HEALTHCARE
    }

    private final BusinessSector sector;
    private final String locationNodeId;
    private double revenue;
    private double expenses;
    private int employees;
    private int maxCapacity;
    private double demand;
    private double averageSalary;
    private double netProfit;

    public BusinessAgent(String id, String name, BusinessSector sector,
                         String locationNodeId, double posX, double posY,
                         int initialEmployees, int maxCapacity, double averageSalary) {
        super(id, name, AgentType.BUSINESS, posX, posY, "OPERATING", 85.0);
        this.sector = sector;
        this.locationNodeId = locationNodeId;
        this.employees = initialEmployees;
        this.maxCapacity = maxCapacity;
        this.averageSalary = averageSalary;
        this.demand = 50.0 + Math.random() * 50.0;
        this.revenue = initialEmployees * (averageSalary * 1.35);
        this.expenses = initialEmployees * averageSalary;
        this.netProfit = revenue - expenses;
    }

    public BusinessSector getSector() { return sector; }
    public String getLocationNodeId() { return locationNodeId; }
    public double getRevenue() { return revenue; }
    public double getExpenses() { return expenses; }
    public int getEmployees() { return employees; }
    public int getMaxCapacity() { return maxCapacity; }
    public double getDemand() { return demand; }
    public double getAverageSalary() { return averageSalary; }
    public double getNetProfit() { return netProfit; }

    public void setDemand(double demand) {
        this.demand = Math.max(0.0, demand);
    }

    @Override
    public void step(SimulationContext context) {
        if (!active) return;

        double vitality = context.getEconomicVitality();

        // 1. Demand fluctuates with economic vitality & random market forces
        double marketShift = (Math.random() - 0.48) * 4.0;
        this.demand = Math.max(10.0, Math.min(150.0, this.demand + marketShift * vitality));

        // 2. Revenue generation based on production capacity & demand
        double productivity = (double) employees / Math.max(1, maxCapacity);
        double salesMultiplier = Math.min(demand / 100.0, 1.25);
        this.revenue = (employees * averageSalary * 1.3) * salesMultiplier * vitality;

        // 3. Operating expenses (payroll + overhead)
        double payroll = (employees * averageSalary) / 30.0; // Daily payroll
        double overhead = payroll * 0.15;
        this.expenses = payroll + overhead;
        this.netProfit = (this.revenue / 30.0) - this.expenses;

        // 4. Autonomous hiring / workforce adjustment
        if (demand > 80.0 && employees < maxCapacity && netProfit > 0) {
            hire(1);
        } else if (demand < 30.0 && employees > 2 && netProfit < -500) {
            fire(1);
        }

        // 5. Update satisfaction based on profit margin
        if (netProfit >= 0) {
            setSatisfaction(this.satisfaction + 0.4);
            this.status = "PROFITABLE (" + sector.name() + ")";
        } else {
            setSatisfaction(this.satisfaction - 0.8);
            this.status = "OPERATING DEFICIT";
        }
    }

    public void hire(int count) {
        int toHire = Math.min(count, maxCapacity - employees);
        if (toHire > 0) {
            this.employees += toHire;
        }
    }

    public void fire(int count) {
        int toFire = Math.min(count, employees - 1);
        if (toFire > 0) {
            this.employees -= toFire;
        }
    }

    public void injectStimulus(double amount) {
        this.revenue += amount;
        this.demand += 15.0;
        hire(Math.min(3, maxCapacity - employees));
    }

    @Override
    public Map<String, Object> getInspectionDetails() {
        Map<String, Object> details = super.getInspectionDetails();
        details.put("Sector", sector.name());
        details.put("Hub Location", locationNodeId);
        details.put("Workforce", employees + " / " + maxCapacity + " employees");
        details.put("Average Salary", String.format("$%.0f/mo", averageSalary));
        details.put("Monthly Revenue", String.format("$%.2f", revenue));
        details.put("Daily Net Profit", String.format("$%.2f", netProfit));
        details.put("Market Demand Index", String.format("%.1f pts", demand));
        return details;
    }
}
