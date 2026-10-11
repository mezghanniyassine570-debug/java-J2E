package com.aether.engine;

import com.aether.model.agent.BusinessAgent;
import com.aether.model.agent.CitizenAgent;
import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * MOD-05 EconomyEngine.
 * Computes city economic metrics: employment rate, gross output, wages, and vitality.
 */
@Component
public class EconomyEngine {

    private volatile double grossCityProduct = 1500000.0;
    private volatile double employmentRate = 92.5; // percentage
    private volatile double averageSalary = 3200.0;
    private volatile double economicVitality = 1.0; // multiplier (0.5 - 2.0)

    public void updateEconomicMetrics(Collection<CitizenAgent> citizens,
                                       Collection<BusinessAgent> businesses,
                                       EventEngine eventEngine) {
        if (citizens == null || businesses == null) return;

        // 1. Employment Calculation
        long totalCitizens = citizens.size();
        long employedCount = citizens.stream().filter(CitizenAgent::isEmployed).count();
        if (totalCitizens > 0) {
            this.employmentRate = ((double) employedCount / totalCitizens) * 100.0;
        }

        // 2. Average Salary
        double totalSalary = citizens.stream().mapToDouble(CitizenAgent::getIncome).sum();
        if (totalCitizens > 0) {
            this.averageSalary = totalSalary / totalCitizens;
        }

        // 3. Gross Business Output
        double totalRevenue = businesses.stream().mapToDouble(BusinessAgent::getRevenue).sum();
        this.grossCityProduct = totalRevenue;

        // 4. Economic Vitality computation
        // High employment + positive business revenue = healthy vitality
        double employmentWeight = employmentRate / 100.0;
        this.economicVitality = Math.max(0.6, Math.min(1.8, 0.5 + employmentWeight * 0.7));
    }

    public double getGrossCityProduct() { return grossCityProduct; }
    public double getEmploymentRate() { return employmentRate; }
    public double getAverageSalary() { return averageSalary; }
    public double getEconomicVitality() { return economicVitality; }

    public void setEconomicVitality(double vitality) {
        this.economicVitality = vitality;
    }
}
