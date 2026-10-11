# AETHER Lite — Autonomous Multi-Agent Urban Simulation & Decision Intelligence Platform

> **Projet J2E / Spring Boot 3 & Java 21 LTS**  
> Spécification : **AETHER Lite Cahier des Charges v2**  
> Créateurs du projet : **Louay Hamdi** & **Yassine Mezghani**  
> Devise : *“Simulate. Understand. Predict. Transform.”*

---

## 🏛️ Vue d'Ensemble & Architecture Réalisée

Le projet implémente fidèlement les 8 modules prescrits par le **Cahier des Charges AETHER Lite v2** ainsi que le modèle conceptuel UML (Annexe B) :

```
java j2e/
├── pom.xml                                           # Maven build & dépendances Spring Boot 3.3.4
├── mvnw.cmd                                          # Wrapper d'exécution Maven
├── README.md                                         # Documentation complète du projet
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── aether/
│   │   │           ├── AetherApplication.java        # Point d'entrée Spring Boot
│   │   │           │
│   │   │           ├── model/                        # Modèle Métier & Annexe B UML
│   │   │           │   ├── Inspectable.java          # <<interface>> Inspectable
│   │   │           │   ├── Agent.java                # Entité JPA persistante
│   │   │           │   ├── City.java                 # Entité JPA persistante
│   │   │           │   ├── SimulationEvent.java      # Entité JPA persistante
│   │   │           │   ├── agent/                    # Agents Autonomes (MOD-02 & Annexe B)
│   │   │           │   │   ├── AbstractAgent.java    # Base abstraite implémentant Inspectable
│   │   │           │   │   ├── CitizenAgent.java     # Salaires, routine, trajets, satisfaction
│   │   │           │   │   ├── VehicleAgent.java     # Navigation A*, vitesse dynamique, rerouting
│   │   │           │   │   └── BusinessAgent.java    # Recrutement, masse salariale, CA, rentabilité
│   │   │           │   └── city/                     # Topologie Urbaine (MOD-01)
│   │   │           │       ├── CityNode.java         # Intersections et pôles d'échange
│   │   │           │       ├── RoadSegment.java      # Tronçons routiers avec calcul de congestion
│   │   │           │       ├── Building.java         # Bâtiments résidentiels, tertiaires, civiques
│   │   │           │       ├── Neighborhood.java     # Quartiers urbains et zonage
│   │   │           │       ├── CityGraph.java        # Graphe d'adjacence thread-safe
│   │   │           │       └── Route.java            # Résultat d'itinéraire A*
│   │   │           │
│   │   │           ├── engine/                       # Moteurs de Simulation (MOD-01 à MOD-07)
│   │   │           │   ├── SimulationClock.java      # Horloge virtuelle (Jours, Heures, Minutes)
│   │   │           │   ├── SimulationContext.java    # Contexte environnemental des agents
│   │   │           │   ├── SimulationSnapshot.java   # Capture d'état pour scénarios What-If
│   │   │           │   ├── CityManager.java          # MOD-01 : Génération topologique Neo-Metropolis
│   │   │           │   ├── AgentEngine.java          # MOD-02 : Cycle de vie et concurrence multi-agents
│   │   │           │   ├── TrafficEngine.java        # MOD-04 : Calcul LOS (A à F) et alertes trafic
│   │   │           │   ├── EconomyEngine.java        # MOD-05 : PIB local, emploi, vitalité
│   │   │           │   ├── EventEngine.java          # MOD-06 : Événements en direct et archivage
│   │   │           │   ├── WhatIfEngine.java         # MOD-07 : Moteur What-If et comparatif décisionnel
│   │   │           │   └── SimulationEngine.java     # Moteur coordinateur central & boucle périodique
│   │   │           │
│   │   │           ├── service/                      # Couche Services
│   │   │           │   ├── CityService.java
│   │   │           │   ├── AgentService.java
│   │   │           │   ├── SimulationService.java
│   │   │           │   ├── routing/
│   │   │           │   │   ├── RoutingEngine.java    # MOD-03 : Interface de routage
│   │   │           │   │   └── impl/
│   │   │           │   │       └── AStarRoutingEngine.java # Algorithme de recherche A* heuristique
│   │   │           │   └── impl/
│   │   │           │       ├── CityServiceImpl.java
│   │   │           │       ├── AgentServiceImpl.java
│   │   │           │       └── SimulationServiceImpl.java
│   │   │           │
│   │   │           ├── websocket/                    # Communication Temps Réel
│   │   │           │   ├── WebSocketConfig.java      # Enregistrement endpoint /ws/simulation
│   │   │           │   └── SimulationWebSocketHandler.java # Diffusion bi-directionnelle
│   │   │           │
│   │   │           └── controller/                   # Contrôleurs MVC et REST API
│   │   │               ├── SimulationController.java # Vue Thymeleaf du Cockpit
│   │   │               ├── SimulationRestController.java # REST API : état, inspection, What-If
│   │   │               ├── CityController.java       # CRUD gestion des villes
│   │   │               └── AgentController.java      # REST API agents
│   │   │
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── static/
│   │       │   ├── css/main.css                      # Thème cyberpunk / cockpit moderne
│   │       │   └── js/simulation.js                  # Moteur Canvas 2D, pan/zoom, WebSockets
│   │       └── templates/
│   │           ├── fragments/
│   │           ├── city/
│   │           └── simulation/
│   │               └── cockpit.html                  # Tableau de bord décisionnel interactif
│   │
│   └── test/java/com/aether/
│       ├── AetherApplicationTests.java               # Validation du contexte Spring Boot
│       ├── routing/AStarRoutingEngineTest.java       # Tests unitaires de l'algorithme A*
│       └── engine/WhatIfEngineTest.java              # Tests unitaires des scénarios What-If
```

---

## 🧩 Couverture des Modules du Cahier des Charges

| Module | Rôle | Implémentation dans le Code |
| :--- | :--- | :--- |
| **MOD-01 CityManager** | Topologie, réseaux routiers, quartiers, bâtiments | `CityManager.java`, `CityGraph.java`, `RoadSegment.java`, `CityNode.java` |
| **MOD-02 AgentEngine** | Modèle multi-agent, Annexe B UML, concurrence | `AgentEngine.java`, `AbstractAgent.java`, `CitizenAgent.java`, `VehicleAgent.java`, `BusinessAgent.java` |
| **MOD-03 RoutingEngine**| Routage A* avec pénalisation de congestion et contournement | `AStarRoutingEngine.java`, `Route.java` |
| **MOD-04 TrafficEngine**| Calcul d'encombrement, Niveau de Service (LOS A à F) | `TrafficEngine.java` |
| **MOD-05 EconomyEngine**| Modélisation économique, masse salariale, PIB, emploi | `EconomyEngine.java` |
| **MOD-06 EventEngine**  | Détection, dispatching et persistence des événements | `EventEngine.java`, `SimulationEventRepository.java` |
| **MOD-07 WhatIfEngine** | Snapshot d'état, scénarios alternatifs, comparatif KPI | `WhatIfEngine.java`, `SimulationSnapshot.java` |
| **MOD-08 AnalyticsUI**  | Carte 2D Canvas, panneau d'inspection, WebSockets | `cockpit.html`, `simulation.js`, `main.css`, `SimulationWebSocketHandler.java` |

---

## 🎮 Fonctionnalités du Cockpit (MOD-08 AnalyticsUI)

1. **Carte Interactive 2D (HTML5 Canvas)** :
   - Rendu fluide du réseau routier de **Neo-Metropolis** (autoroutes, boulevards, avenues).
   - **Heatmap dynamique de trafic** : routes vertes (<30%), jaunes (30-70%), orange (70-85%) et rouge (>85%).
   - Tronçons fermés affichés avec des rayures d'avertissement et le pictogramme ⛔.
   - Véhicules autonomes animés en continu selon leur progression sur les segments routiers.
   - Contrôles de navigation : **Zoom In (+)**, **Zoom Out (-)**, **Recadrage (Fit)**, et **Déplacement (Pan par glisser-déposer)**.

2. **Panneau d'Inspection Interactif (Tiroir `Inspectable`)** :
   - Cliquez sur **n'importe quel élément de la carte** (véhicule, nœud d'intersection, route, bâtiment ou quartier).
   - Un tiroir latéral s'ouvre pour afficher ses caractéristiques en direct (coordonnées, vitesse, itinéraire, statut d'emploi, salaire, capacité, taux de congestion, etc.).

3. **Moteur de Scénarios What-If & Comparateur Décisionnel** :
   - **Fermeture de l'Avenue des Nations (Road Closure)** : déclenche le reroutage automatique des véhicules via l'algorithme A* et reporte le trafic sur les axes adjacents.
   - **Subvention Municipale ($25k/biz)** : stimulation des embauches et des revenus des entreprises.
   - **Extension du Réseau de Métro Léger (Transit Expansion)** : transfert modal de 30% des véhicules vers les transports collectifs.
   - **Coupure Électrique / Crise Énergétique (Energy Crisis)** : ralentissement de la production industrielle et baisse de satisfaction.
   - **Bouton Comparatif Décisionnel** : ouvre une fenêtre modale affichant un tableau comparatif détaillé (*Baseline* vs *Scénario*) et la recommandation d'intelligence décisionnelle.

4. **Contrôles Temporels & Télémétrie en Direct** :
   - Boutons **Play ▶**, **Pause ⏸**, **Step ⏭** (avance d'un tick unitaire), et **Reset ↺**.
   - Sélecteurs de vitesse : **1x**, **2x**, **5x**, **10x**.
   - Flux WebSocket bidirectionnel (`/ws/simulation`) avec reprise automatique par interrogation périodique.

---

## 🚀 Guide de Démarrage & Exécution

### 1. Prérequis
- **Java 21 LTS** ou supérieur (testé et validé sous Java 25 LTS).
- Port `8081` disponible (configurable dans `application.properties`).

### 2. Compilation et Tests Unitaires
Ouvrez un terminal PowerShell dans le dossier du projet :
```powershell
# Exécution de la suite de tests automatisés (100% de réussite)
.\mvnw.cmd test

# Création de l'exécutable JAR
.\mvnw.cmd package -DskipTests
```

### 3. Lancement de la Plateforme
```powershell
# Lancement direct du JAR repackagé
java -jar "target\aether-platform-1.0.0-SNAPSHOT.jar"
```
*Ou directement via votre IDE en lançant `AetherApplication.java`.*

### 4. Liens d'Accès
- 🎛️ **Cockpit Simulation AETHER Lite** : [http://localhost:8081](http://localhost:8081)
- 🏙️ **Gestion des Villes** : [http://localhost:8081/cities](http://localhost:8081/cities)
- 🗄️ **Console Base de Données H2** : [http://localhost:8081/h2-console](http://localhost:8081/h2-console)  
  *(JDBC URL: `jdbc:h2:mem:aetherdb`, Utilisateur: `sa`, Mot de passe vide)*
- 🔌 **REST API Échelle Complète** :
  - `GET /api/simulation/state`
  - `GET /api/simulation/city-graph`
  - `GET /api/simulation/inspect/{id}`
  - `GET /api/simulation/whatif/compare`
  - `POST /api/simulation/whatif/apply?scenario={NAME}`
