package com.aether.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cities")
public class City {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private int totalPopulation;

    private int totalVehicles;

    private int totalBusinesses;

    private double energyConsumption;

    private double economicIndex;

    @OneToMany(mappedBy = "city", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Agent> agents = new ArrayList<>();

    public City() {}

    public City(String name, int totalPopulation, int totalVehicles, int totalBusinesses) {
        this.name = name;
        this.totalPopulation = totalPopulation;
        this.totalVehicles = totalVehicles;
        this.totalBusinesses = totalBusinesses;
        this.energyConsumption = 100.0;
        this.economicIndex = 1.0;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getTotalPopulation() { return totalPopulation; }
    public void setTotalPopulation(int totalPopulation) { this.totalPopulation = totalPopulation; }

    public int getTotalVehicles() { return totalVehicles; }
    public void setTotalVehicles(int totalVehicles) { this.totalVehicles = totalVehicles; }

    public int getTotalBusinesses() { return totalBusinesses; }
    public void setTotalBusinesses(int totalBusinesses) { this.totalBusinesses = totalBusinesses; }

    public double getEnergyConsumption() { return energyConsumption; }
    public void setEnergyConsumption(double energyConsumption) { this.energyConsumption = energyConsumption; }

    public double getEconomicIndex() { return economicIndex; }
    public void setEconomicIndex(double economicIndex) { this.economicIndex = economicIndex; }

    public List<Agent> getAgents() { return agents; }
    public void setAgents(List<Agent> agents) { this.agents = agents; }
}
