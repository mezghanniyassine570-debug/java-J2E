/**
 * AETHER LITE - Real-time Multi-Agent Urban Simulation Cockpit
 * Canvas 2D Engine, WebSockets, Interactive Inspector & What-If Studio
 */

(function () {
    'use strict';

    // State Variables
    let cityGraph = { nodes: [], roads: [], buildings: [], neighborhoods: [] };
    let liveState = {
        tick: 480,
        formattedTime: "Day 1, 08:00",
        timePeriod: "Morning Rush Hour",
        isRunning: false,
        speedMultiplier: 1,
        activeScenario: "BASELINE",
        congestionRatio: 0.0,
        avgSpeedKmH: 45.0,
        levelOfService: "A (Free Flow)",
        avgSatisfaction: 85.0,
        employmentRate: 92.5,
        grossCityProduct: 1500000,
        citizenCount: 300,
        vehicleCount: 100,
        businessCount: 30,
        vehicles: [],
        roads: {},
        events: []
    };

    let socket = null;
    let isWsConnected = false;
    let inspectedEntityId = null;

    // Canvas view transform
    const canvas = document.getElementById('cityCanvas');
    if (!canvas) return;
    const ctx = canvas.getContext('2d');

    let scale = 0.95;
    let panX = 40;
    let panY = 20;
    let isDragging = false;
    let dragStartX = 0;
    let dragStartY = 0;

    let showHeatmap = true;
    let showVehicles = true;
    let showBuildings = true;
    let showAlgoVisualizer = false;

    // Initialization
    window.addEventListener('DOMContentLoaded', () => {
        setupTheme();
        setupCanvas();
        fetchCityGraph();
        connectWebSocket();
        setupControls();
        setupDefenseAssistant();
        setupInspector();
        setupWhatIfModal();
        requestAnimationFrame(renderLoop);
    });

    function setupCanvas() {
        resizeCanvas();
        window.addEventListener('resize', resizeCanvas);

        canvas.addEventListener('mousedown', (e) => {
            isDragging = true;
            dragStartX = e.clientX - panX;
            dragStartY = e.clientY - panY;
        });

        window.addEventListener('mousemove', (e) => {
            if (isDragging) {
                panX = e.clientX - dragStartX;
                panY = e.clientY - dragStartY;
            }
        });

        window.addEventListener('mouseup', () => { isDragging = false; });

        canvas.addEventListener('wheel', (e) => {
            e.preventDefault();
            const zoomFactor = e.deltaY < 0 ? 1.1 : 0.9;
            const newScale = Math.max(0.4, Math.min(2.5, scale * zoomFactor));

            // Zoom towards mouse pointer
            const rect = canvas.getBoundingClientRect();
            const mouseX = e.clientX - rect.left;
            const mouseY = e.clientY - rect.top;

            panX = mouseX - (mouseX - panX) * (newScale / scale);
            panY = mouseY - (mouseY - panY) * (newScale / scale);
            scale = newScale;
        }, { passive: false });

        // Canvas entity click detection
        canvas.addEventListener('click', handleCanvasClick);
    }

    function resizeCanvas() {
        const container = canvas.parentElement;
        canvas.width = container.clientWidth * window.devicePixelRatio;
        canvas.height = container.clientHeight * window.devicePixelRatio;
        canvas.style.width = container.clientWidth + 'px';
        canvas.style.height = container.clientHeight + 'px';
    }

    function fetchCityGraph() {
        fetch('/api/simulation/city-graph')
            .then(res => res.json())
            .then(data => {
                cityGraph = data;
            })
            .catch(err => console.error('Error fetching city topology:', err));
    }

    // WebSocket bi-directional streaming
    function connectWebSocket() {
        const proto = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        const wsUrl = `${proto}//${window.location.host}/ws/simulation`;

        socket = new WebSocket(wsUrl);

        socket.onopen = () => {
            isWsConnected = true;
            updateWsBadge(true);
        };

        socket.onmessage = (event) => {
            try {
                const msg = JSON.parse(event.data);
                if (msg.type === 'INIT_STATE' || msg.type === 'TICK_UPDATE') {
                    updateSimulationState(msg.data);
                }
            } catch (err) {
                console.error('WebSocket parse error:', err);
            }
        };

        socket.onclose = () => {
            isWsConnected = false;
            updateWsBadge(false);
            // Reconnect after 3s
            setTimeout(connectWebSocket, 3000);
        };

        socket.onerror = () => {
            isWsConnected = false;
            updateWsBadge(false);
        };
    }

    function sendCommand(action, payload = {}) {
        const data = Object.assign({ action }, payload);

        // Immediate optimistic UI feedback
        if (action === 'start') {
            liveState.isRunning = true;
            const btnPlay = document.getElementById('btnStart');
            const btnPause = document.getElementById('btnPause');
            if (btnPlay) btnPlay.disabled = true;
            if (btnPause) btnPause.disabled = false;
            const statusEl = document.getElementById('hudStatus');
            if (statusEl) {
                statusEl.textContent = "SIMULATION RUNNING";
                statusEl.className = "status-indicator status-active";
            }
        } else if (action === 'pause') {
            liveState.isRunning = false;
            const btnPlay = document.getElementById('btnStart');
            const btnPause = document.getElementById('btnPause');
            if (btnPlay) btnPlay.disabled = false;
            if (btnPause) btnPause.disabled = true;
            const statusEl = document.getElementById('hudStatus');
            if (statusEl) {
                statusEl.textContent = "SIMULATION IDLE";
                statusEl.className = "status-indicator status-idle";
            }
        }

        if (socket && socket.readyState === WebSocket.OPEN) {
            socket.send(JSON.stringify(data));
        }

        // Always invoke REST endpoint as well to guarantee state sync across browsers
        fetch(`/api/simulation/${action}`, { method: 'POST' })
            .then(res => res.json())
            .then(state => { if (state && state.formattedTime) updateSimulationState(state); })
            .catch(() => {});
    }

    // Active synchronization loop: guarantees real-time UI refresh at all times
    setInterval(() => {
        fetch('/api/simulation/state')
            .then(res => res.json())
            .then(state => {
                if (state) updateSimulationState(state);
            })
            .catch(() => {});
    }, 1000);

    function updateWsBadge(connected) {
        const badge = document.getElementById('wsBadge');
        if (!badge) return;
        if (connected) {
            badge.innerHTML = '<span class="pulse-dot"></span> LIVE WEBSOCKET';
            badge.style.color = '#34d399';
            badge.style.borderColor = 'rgba(16, 185, 129, 0.4)';
        } else {
            badge.innerHTML = '<span class="pulse-dot" style="background:#38bdf8"></span> LIVE POLLING';
            badge.style.color = '#38bdf8';
            badge.style.borderColor = 'rgba(56, 189, 248, 0.4)';
        }
    }

    function updateSimulationState(data) {
        Object.assign(liveState, data);

        // Update DOM elements
        const timeEl = document.getElementById('hudTime');
        if (timeEl) timeEl.textContent = data.formattedTime || "Day 1, 08:00";

        const periodEl = document.getElementById('hudPeriod');
        if (periodEl) periodEl.textContent = data.timePeriod || "";

        const statusEl = document.getElementById('hudStatus');
        if (statusEl) {
            if (data.isRunning) {
                statusEl.textContent = "SIMULATION RUNNING";
                statusEl.className = "status-indicator status-active";
            } else {
                statusEl.textContent = "SIMULATION IDLE";
                statusEl.className = "status-indicator status-idle";
            }
        }

        // Play/Pause button toggle
        const btnPlay = document.getElementById('btnStart');
        const btnPause = document.getElementById('btnPause');
        if (btnPlay && btnPause) {
            btnPlay.disabled = data.isRunning;
            btnPause.disabled = !data.isRunning;
        }

        // Active scenario pill
        const scenarioEl = document.getElementById('activeScenarioBadge');
        if (scenarioEl) scenarioEl.textContent = data.activeScenario || "BASELINE";

        // Update KPIs
        setText('kpiCongestion', (data.congestionRatio || 0) + '%');
        setText('kpiSpeed', (data.avgSpeedKmH || 45) + ' km/h');
        setText('kpiLOS', data.levelOfService || 'A');
        setText('kpiSatisfaction', (data.avgSatisfaction || 80) + '%');
        setText('kpiEmployment', (data.employmentRate || 90) + '%');
        setText('kpiGDP', '$' + formatNumber(data.grossCityProduct || 1500000));
        setText('kpiCitizens', formatNumber(data.citizenCount || 300));
        setText('kpiVehicles', formatNumber(data.vehicleCount || 100));
        setText('kpiBusinesses', formatNumber(data.businessCount || 30));

        // Update Progress Bars
        setBar('barCongestion', data.congestionRatio, getCongestionColor(data.congestionRatio / 100));
        setBar('barSatisfaction', data.avgSatisfaction, '#38bdf8');
        setBar('barEmployment', data.employmentRate, '#10b981');

        // Refresh Inspector if opened
        if (inspectedEntityId) {
            refreshInspector(inspectedEntityId);
        }

        // Update Live Events List
        if (data.events && Array.isArray(data.events)) {
            renderEvents(data.events);
        }
    }

    function setText(id, val) {
        const el = document.getElementById(id);
        if (el) el.textContent = val;
    }

    function setBar(id, percent, color) {
        const bar = document.getElementById(id);
        if (bar) {
            bar.style.width = Math.min(100, Math.max(0, percent)) + '%';
            if (color) bar.style.backgroundColor = color;
        }
    }

    function formatNumber(num) {
        return num ? num.toLocaleString() : '0';
    }

    function renderEvents(events) {
        const container = document.getElementById('liveEventList');
        if (!container) return;

        if (!events || events.length === 0) {
            container.innerHTML = '<div class="empty-state">No events recorded yet.</div>';
            return;
        }

        let html = '';
        events.slice(0, 15).forEach(ev => {
            const timeStr = ev.timestamp ? ev.timestamp.substring(11, 19) : '08:00:00';
            const sev = (ev.severity || 'INFO').toLowerCase();
            html += `
                <div class="event-item">
                    <span class="event-time">${timeStr}</span>
                    <span class="event-badge badge-${sev}">${ev.eventType}</span>
                    <span class="event-desc">${ev.description}</span>
                </div>
            `;
        });
        container.innerHTML = html;
    }

    // 2D Canvas Render Loop
    function renderLoop() {
        drawCanvas();
        requestAnimationFrame(renderLoop);
    }

    function drawCanvas() {
        const dpr = window.devicePixelRatio || 1;
        const isLight = document.body.classList.contains('theme-light');
        ctx.save();
        ctx.clearRect(0, 0, canvas.width, canvas.height);

        // Apply Pan & Zoom
        ctx.translate(panX * dpr, panY * dpr);
        ctx.scale(scale * dpr, scale * dpr);

        // Academic Grid Background (Cartesian Grid)
        drawAcademicGrid(ctx, isLight);

        // 1. Draw Neighborhood / District Backgrounds
        if (cityGraph.neighborhoods) {
            cityGraph.neighborhoods.forEach(dist => {
                const w = dist.maxX - dist.minX;
                const h = dist.maxY - dist.minY;
                ctx.fillStyle = dist.colorHex + (isLight ? '12' : '0d');
                ctx.strokeStyle = dist.colorHex + (isLight ? '40' : '25');
                ctx.lineWidth = 1;
                roundRect(ctx, dist.minX, dist.minY, w, h, 16);
                ctx.fill();
                ctx.stroke();

                // District Label
                ctx.fillStyle = dist.colorHex + (isLight ? 'b0' : '70');
                ctx.font = '700 11px "Plus Jakarta Sans"';
                ctx.fillText(dist.name.toUpperCase(), dist.minX + 16, dist.minY + 24);
            });
        }

        // 2. Draw Roads
        if (cityGraph.roads && cityGraph.nodes) {
            const nodeMap = {};
            cityGraph.nodes.forEach(n => nodeMap[n.id] = n);

            cityGraph.roads.forEach(road => {
                const from = nodeMap[road.fromNodeId];
                const to = nodeMap[road.toNodeId];
                if (!from || !to) return;

                const liveRoad = liveState.roads ? liveState.roads[road.id] : null;
                const isClosed = (liveRoad && liveRoad.closed) || road.closed;
                const congestion = liveRoad ? liveRoad.congestion : 0.0;
                const vehCount = liveRoad ? (liveRoad.vehicleCount || 0) : 0;

                ctx.beginPath();
                ctx.moveTo(from.x, from.y);
                ctx.lineTo(to.x, to.y);

                if (isClosed) {
                    // Closed road: striped hazard line
                    ctx.strokeStyle = '#ef4444';
                    ctx.lineWidth = 5;
                    ctx.setLineDash([8, 6]);
                    ctx.stroke();
                    ctx.setLineDash([]);

                    // Closed warning badge in middle of road
                    const midX = (from.x + to.x) / 2;
                    const midY = (from.y + to.y) / 2;
                    ctx.fillStyle = '#ef4444';
                    ctx.font = 'bold 12px sans-serif';
                    ctx.fillText('⛔ FERMÉ (A* Reroute)', midX - 35, midY - 6);
                } else {
                    // Open road with congestion heatmap coloring
                    const roadColor = showHeatmap ? getCongestionColor(congestion) : (isLight ? '#94a3b8' : '#334155');
                    ctx.strokeStyle = roadColor;
                    ctx.lineWidth = Math.min(8, 3 + (road.lanes || 2) * 1.5);
                    ctx.stroke();

                    // Road center dashed line
                    ctx.beginPath();
                    ctx.moveTo(from.x, from.y);
                    ctx.lineTo(to.x, to.y);
                    ctx.strokeStyle = isLight ? 'rgba(0, 0, 0, 0.15)' : 'rgba(255, 255, 255, 0.2)';
                    ctx.lineWidth = 1;
                    ctx.setLineDash([4, 4]);
                    ctx.stroke();
                    ctx.setLineDash([]);

                    // If Algo Visualizer active: display capacity & volume ratio
                    if (showAlgoVisualizer) {
                        const midX = (from.x + to.x) / 2;
                        const midY = (from.y + to.y) / 2;
                        ctx.fillStyle = isLight ? '#0f172a' : '#e2e8f0';
                        ctx.font = '600 9px "JetBrains Mono"';
                        const label = `${vehCount}/${road.capacity || 20} veh (${Math.round(congestion * 100)}%)`;
                        ctx.fillText(label, midX - 25, midY - 7);
                    }
                }
            });
        }

        // 3. Draw Buildings
        if (showBuildings && cityGraph.buildings) {
            cityGraph.buildings.forEach(bld => {
                const color = getBuildingColor(bld.type);
                ctx.fillStyle = color;
                ctx.strokeStyle = isLight ? 'rgba(0, 0, 0, 0.1)' : 'rgba(255, 255, 255, 0.15)';
                ctx.lineWidth = 1;

                roundRect(ctx, bld.x - 10, bld.y - 10, 20, 20, 4);
                ctx.fill();
                ctx.stroke();

                // Building label
                ctx.fillStyle = isLight ? '#475569' : '#94a3b8';
                ctx.font = '500 8.5px "Plus Jakarta Sans"';
                ctx.fillText(bld.name, bld.x - 14, bld.y + 20);
            });
        }

        // 4. Draw Intersections / Nodes
        if (cityGraph.nodes) {
            cityGraph.nodes.forEach(node => {
                ctx.beginPath();
                ctx.arc(node.x, node.y, showAlgoVisualizer ? 8 : 6, 0, Math.PI * 2);
                ctx.fillStyle = showAlgoVisualizer ? '#0284c7' : (isLight ? '#0284c7' : '#38bdf8');
                ctx.fill();
                ctx.strokeStyle = isLight ? '#ffffff' : '#0a0e17';
                ctx.lineWidth = 2;
                ctx.stroke();

                // Node title with ID badge in algo mode
                ctx.fillStyle = isLight ? '#0f172a' : '#cbd5e1';
                ctx.font = showAlgoVisualizer ? '700 10.5px "JetBrains Mono"' : '600 9.5px "Plus Jakarta Sans"';
                const nodeLabel = showAlgoVisualizer ? `[${node.id}] ${node.name}` : node.name;
                ctx.fillText(nodeLabel, node.x + 11, node.y + 4);
            });
        }

        // 5. Draw Animated Moving Vehicles
        if (showVehicles && liveState.vehicles && Array.isArray(liveState.vehicles)) {
            liveState.vehicles.forEach(v => {
                ctx.beginPath();
                ctx.arc(v.x, v.y, 4.5, 0, Math.PI * 2);
                ctx.fillStyle = isLight ? '#0284c7' : '#38bdf8';
                ctx.fill();
                ctx.strokeStyle = '#ffffff';
                ctx.lineWidth = 1.5;
                ctx.stroke();

                // Vehicle glow / halo
                ctx.beginPath();
                ctx.arc(v.x, v.y, 7, 0, Math.PI * 2);
                ctx.fillStyle = isLight ? 'rgba(2, 132, 199, 0.2)' : 'rgba(56, 189, 248, 0.25)';
                ctx.fill();
            });
        }

        ctx.restore();
    }

    function drawAcademicGrid(context, isLight) {
        context.save();
        context.strokeStyle = isLight ? 'rgba(15, 23, 42, 0.04)' : 'rgba(255, 255, 255, 0.03)';
        context.lineWidth = 1;
        const gridSize = 40;
        const width = 1400;
        const height = 900;

        for (let x = -200; x < width; x += gridSize) {
            context.beginPath();
            context.moveTo(x, -200);
            context.lineTo(x, height);
            context.stroke();
        }
        for (let y = -200; y < height; y += gridSize) {
            context.beginPath();
            context.moveTo(-200, y);
            context.lineTo(width, y);
            context.stroke();
        }
        context.restore();
    }

    function getCongestionColor(ratio) {
        if (ratio < 0.3) return '#10b981'; // Green
        if (ratio < 0.7) return '#f59e0b'; // Amber
        if (ratio < 0.85) return '#f97316'; // Orange
        return '#ef4444'; // Red
    }

    function getBuildingColor(type) {
        switch (type) {
            case 'RESIDENTIAL': return '#22c55e';
            case 'COMMERCIAL': return '#38bdf8';
            case 'INDUSTRIAL': return '#f59e0b';
            case 'HOSPITAL': return '#ec4899';
            case 'SCHOOL': return '#8b5cf6';
            case 'CIVIC': return '#6366f1';
            default: return '#64748b';
        }
    }

    function roundRect(ctx, x, y, width, height, radius) {
        ctx.beginPath();
        ctx.moveTo(x + radius, y);
        ctx.lineTo(x + width - radius, y);
        ctx.quadraticCurveTo(x + width, y, x + width, y + radius);
        ctx.lineTo(x + width, y + height - radius);
        ctx.quadraticCurveTo(x + width, y + height, x + width - radius, y + height);
        ctx.lineTo(x + radius, y + height);
        ctx.quadraticCurveTo(x, y + height, x, y + height - radius);
        ctx.lineTo(x, y + radius);
        ctx.quadraticCurveTo(x, y, x + radius, y);
        ctx.closePath();
    }

    // Canvas click detection for Inspector
    function handleCanvasClick(e) {
        const rect = canvas.getBoundingClientRect();
        const clickX = (e.clientX - rect.left - panX) / scale;
        const clickY = (e.clientY - rect.top - panY) / scale;

        // 1. Check vehicle hit
        if (liveState.vehicles) {
            for (const v of liveState.vehicles) {
                const dist = Math.hypot(v.x - clickX, v.y - clickY);
                if (dist <= 12) {
                    inspectEntity(v.id);
                    return;
                }
            }
        }

        // 2. Check node hit
        if (cityGraph.nodes) {
            for (const n of cityGraph.nodes) {
                const dist = Math.hypot(n.x - clickX, n.y - clickY);
                if (dist <= 14) {
                    inspectEntity(n.id);
                    return;
                }
            }
        }

        // 3. Check building hit
        if (cityGraph.buildings) {
            for (const b of cityGraph.buildings) {
                const dist = Math.hypot(b.x - clickX, b.y - clickY);
                if (dist <= 16) {
                    inspectEntity(b.id);
                    return;
                }
            }
        }

        // 4. Check road segment hit
        if (cityGraph.roads && cityGraph.nodes) {
            const nodeMap = {};
            cityGraph.nodes.forEach(n => nodeMap[n.id] = n);

            for (const r of cityGraph.roads) {
                const from = nodeMap[r.fromNodeId];
                const to = nodeMap[r.toNodeId];
                if (from && to) {
                    const distToSeg = distanceToSegment(clickX, clickY, from.x, from.y, to.x, to.y);
                    if (distToSeg <= 10) {
                        inspectEntity(r.id);
                        return;
                    }
                }
            }
        }
    }

    function distanceToSegment(px, py, x1, y1, x2, y2) {
        const l2 = (x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1);
        if (l2 === 0) return Math.hypot(px - x1, py - y1);
        let t = ((px - x1) * (x2 - x1) + (py - y1) * (y2 - y1)) / l2;
        t = Math.max(0, Math.min(1, t));
        return Math.hypot(px - (x1 + t * (x2 - x1)), py - (y1 + t * (y2 - y1)));
    }

    // Inspector Drawer logic
    function inspectEntity(id) {
        inspectedEntityId = id;
        refreshInspector(id);
        const drawer = document.getElementById('inspectorDrawer');
        if (drawer) drawer.classList.add('open');
    }

    function refreshInspector(id) {
        fetch(`/api/simulation/inspect/${id}`)
            .then(res => res.json())
            .then(entity => {
                setText('inspectName', entity.name || id);
                setText('inspectBadge', entity.type || 'ENTITY');

                const grid = document.getElementById('inspectGrid');
                if (grid && entity.details) {
                    let rows = '';
                    for (const [key, val] of Object.entries(entity.details)) {
                        rows += `
                            <div class="inspect-row">
                                <span class="inspect-key">${key}</span>
                                <span class="inspect-val">${val}</span>
                            </div>
                        `;
                    }
                    grid.innerHTML = rows;
                }
            })
            .catch(() => {});
    }

    function setupInspector() {
        const closeBtn = document.getElementById('drawerClose');
        if (closeBtn) {
            closeBtn.addEventListener('click', () => {
                inspectedEntityId = null;
                const drawer = document.getElementById('inspectorDrawer');
                if (drawer) drawer.classList.remove('open');
            });
        }
    }

    // Controls setup
    function setupControls() {
        const btnStart = document.getElementById('btnStart');
        if (btnStart) btnStart.addEventListener('click', () => sendCommand('start'));

        const btnPause = document.getElementById('btnPause');
        if (btnPause) btnPause.addEventListener('click', () => sendCommand('pause'));

        const btnStep = document.getElementById('btnStep');
        if (btnStep) btnStep.addEventListener('click', () => sendCommand('step'));

        const btnReset = document.getElementById('btnReset');
        if (btnReset) btnReset.addEventListener('click', () => sendCommand('reset'));

        // Speed buttons
        document.querySelectorAll('.speed-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const spd = parseInt(btn.dataset.speed, 10);
                sendCommand('speed', { value: spd });
                document.querySelectorAll('.speed-btn').forEach(b => b.classList.remove('active'));
                btn.classList.add('active');
            });
        });

        // Canvas View Tools
        const btnZoomIn = document.getElementById('btnZoomIn');
        if (btnZoomIn) btnZoomIn.addEventListener('click', () => { scale = Math.min(2.5, scale * 1.2); });

        const btnZoomOut = document.getElementById('btnZoomOut');
        if (btnZoomOut) btnZoomOut.addEventListener('click', () => { scale = Math.max(0.4, scale * 0.8); });

        const btnResetView = document.getElementById('btnResetView');
        if (btnResetView) btnResetView.addEventListener('click', () => {
            scale = 0.95;
            panX = 40;
            panY = 20;
        });

        const btnToggleHeatmap = document.getElementById('btnToggleHeatmap');
        if (btnToggleHeatmap) btnToggleHeatmap.addEventListener('click', () => {
            showHeatmap = !showHeatmap;
            btnToggleHeatmap.style.color = showHeatmap ? 'var(--accent)' : 'var(--text-muted)';
        });

        const btnToggleVehicles = document.getElementById('btnToggleVehicles');
        if (btnToggleVehicles) btnToggleVehicles.addEventListener('click', () => {
            showVehicles = !showVehicles;
            btnToggleVehicles.style.color = showVehicles ? 'var(--accent)' : 'var(--text-muted)';
        });

        const btnToggleBuildings = document.getElementById('btnToggleBuildings');
        if (btnToggleBuildings) btnToggleBuildings.addEventListener('click', () => {
            showBuildings = !showBuildings;
            btnToggleBuildings.style.color = showBuildings ? 'var(--accent)' : 'var(--text-muted)';
        });

        // Algo Visualizer & Graph Nodes toggle
        const btnToggleAlgo = document.getElementById('btnToggleAlgo');
        if (btnToggleAlgo) btnToggleAlgo.addEventListener('click', () => {
            showAlgoVisualizer = !showAlgoVisualizer;
            btnToggleAlgo.style.background = showAlgoVisualizer ? 'var(--accent-glow)' : 'transparent';
            btnToggleAlgo.style.borderColor = showAlgoVisualizer ? 'var(--accent)' : 'var(--border-color)';
        });

        // Print Simulation Audit Report
        const btnPrintReport = document.getElementById('btnPrintReport');
        if (btnPrintReport) btnPrintReport.addEventListener('click', () => {
            window.print();
        });
    }

    // Theme Management: Enforced Dark Mode
    function setupTheme() {
        document.body.classList.remove('theme-light');
        document.body.classList.add('theme-dark');
        localStorage.setItem('aether-theme', 'dark');
    }

    // Academic Defense Walkthrough Assistant (Pour les Enseignants)
    function setupDefenseAssistant() {
        const toggleBtn = document.getElementById('btnToggleDefenseMode');
        const banner = document.getElementById('defenseBanner');

        if (toggleBtn && banner) {
            toggleBtn.addEventListener('click', () => {
                banner.style.display = (banner.style.display === 'none') ? 'block' : 'none';
            });
        }

        // Stepper cards click actions
        document.querySelectorAll('.defense-step-card').forEach(card => {
            card.addEventListener('click', () => {
                document.querySelectorAll('.defense-step-card').forEach(c => c.classList.remove('active'));
                card.classList.add('active');

                const step = parseInt(card.dataset.step, 10);
                executeDefenseStep(step);
            });
        });
    }

    function executeDefenseStep(step) {
        switch (step) {
            case 1:
                // Étape 1: Concurrence & Horloge
                // Highlight HUD and advance 1 step
                sendCommand('step');
                const hud = document.querySelector('.sim-hud');
                if (hud) {
                    hud.scrollIntoView({ behavior: 'smooth' });
                    hud.style.outline = '2px solid var(--accent)';
                    setTimeout(() => hud.style.outline = '', 2000);
                }
                break;

            case 2:
                // Étape 2: Inspecter un agent polymorphe (Annexe B)
                if (liveState.vehicles && liveState.vehicles.length > 0) {
                    inspectEntity(liveState.vehicles[0].id);
                } else {
                    inspectEntity('CITIZEN_001');
                }
                break;

            case 3:
                // Étape 3: Activer l'analyse A* et afficher les nœuds du graphe
                showAlgoVisualizer = true;
                const btnToggleAlgo = document.getElementById('btnToggleAlgo');
                if (btnToggleAlgo) {
                    btnToggleAlgo.style.background = 'var(--accent-glow)';
                    btnToggleAlgo.style.borderColor = 'var(--accent)';
                }
                const canvasCard = document.querySelector('.canvas-viewport-card');
                if (canvasCard) {
                    canvasCard.scrollIntoView({ behavior: 'smooth' });
                    canvasCard.style.outline = '2px solid var(--accent)';
                    setTimeout(() => canvasCard.style.outline = '', 2000);
                }
                break;

            case 4:
                // Étape 4: Scénario What-If & Snapshot Memento
                const btnCompare = document.getElementById('btnCompareWhatIf');
                if (btnCompare) btnCompare.click();
                break;

            case 5:
                // Étape 5: Persistance & Événements
                const eventCard = document.getElementById('liveEventList');
                if (eventCard) {
                    eventCard.scrollIntoView({ behavior: 'smooth' });
                    eventCard.parentElement.style.outline = '2px solid var(--accent)';
                    setTimeout(() => eventCard.parentElement.style.outline = '', 2000);
                }
                break;
        }
    }

    // What-If Scenarios & Modal Comparison setup
    function setupWhatIfModal() {
        // Direct scenario action buttons
        document.querySelectorAll('.whatif-action-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                const scenario = btn.dataset.scenario;
                sendCommand('whatif', { scenario });
            });
        });

        // Comparison modal trigger
        const btnCompare = document.getElementById('btnCompareWhatIf');
        const modal = document.getElementById('whatIfModal');
        const modalClose = document.getElementById('modalClose');

        if (btnCompare && modal) {
            btnCompare.addEventListener('click', () => {
                fetch('/api/simulation/whatif/compare')
                    .then(res => res.json())
                    .then(data => {
                        renderComparisonModal(data);
                        modal.classList.add('open');
                    })
                    .catch(err => {
                        alert('Could not generate comparison. Ensure simulation baseline is recorded.');
                    });
            });
        }

        if (modalClose && modal) {
            modalClose.addEventListener('click', () => modal.classList.remove('open'));
        }

        window.addEventListener('click', (e) => {
            if (e.target === modal) modal.classList.remove('open');
        });

        // Architecture & OOP Guide Modal (For Professors & Evaluators)
        const btnDoc = document.getElementById('btnOpenDocModal');
        const docModal = document.getElementById('architectureDocModal');
        const docModalClose = document.getElementById('docModalClose');

        if (btnDoc && docModal) {
            btnDoc.addEventListener('click', () => docModal.classList.add('open'));
        }
        if (docModalClose && docModal) {
            docModalClose.addEventListener('click', () => docModal.classList.remove('open'));
        }
        window.addEventListener('click', (e) => {
            if (e.target === docModal) docModal.classList.remove('open');
        });
    }

    function renderComparisonModal(data) {
        setText('modalScenarioTitle', data.scenarioTitle || 'Scenario Evaluation');
        setText('modalAiRecText', data.aiRecommendation || 'Evaluation complete.');

        const tbody = document.getElementById('comparisonTableBody');
        if (!tbody || !data.metrics) return;

        let rows = '';
        data.metrics.forEach(m => {
            const badgeClass = m.isPositive ? 'delta-good' : 'delta-bad';
            rows += `
                <tr>
                    <td><strong>${m.metricName}</strong></td>
                    <td>${m.baselineValue}</td>
                    <td>${m.scenarioValue}</td>
                    <td><span class="delta-badge ${badgeClass}">${m.delta}</span></td>
                </tr>
            `;
        });
        tbody.innerHTML = rows;
    }

})();
