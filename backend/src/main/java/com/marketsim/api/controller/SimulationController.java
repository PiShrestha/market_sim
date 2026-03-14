package com.marketsim.api.controller;

import com.marketsim.engine.SimulationEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * REST API for simulation control.
 * 
 * Endpoints:
 * - POST /api/simulation/start - Start the simulation
 * - POST /api/simulation/stop - Stop the simulation
 * - GET /api/simulation/status - Get current status
 */
@RestController
@RequestMapping("/api/simulation")
@RequiredArgsConstructor
@CrossOrigin(origins = { "http://localhost:5173", "http://localhost:3000" })
public class SimulationController {

    private final SimulationEngine simulationEngine;

    /**
     * Start the market simulation.
     */
    @PostMapping("/start")
    public ResponseEntity<SimulationEngine.SimulationStatus> startSimulation() {
        simulationEngine.start();
        return ResponseEntity.ok(simulationEngine.getStatus());
    }

    /**
     * Stop the market simulation.
     */
    @PostMapping("/stop")
    public ResponseEntity<SimulationEngine.SimulationStatus> stopSimulation() {
        simulationEngine.stop();
        return ResponseEntity.ok(simulationEngine.getStatus());
    }

    /**
     * Get current simulation status.
     */
    @GetMapping("/status")
    public ResponseEntity<SimulationEngine.SimulationStatus> getStatus() {
        return ResponseEntity.ok(simulationEngine.getStatus());
    }

    /**
     * Update the reference price (for testing scenarios).
     */
    @PostMapping("/reference-price")
    public ResponseEntity<Void> setReferencePrice(@RequestParam BigDecimal price) {
        simulationEngine.setReferencePrice(price);
        return ResponseEntity.ok().build();
    }
}
