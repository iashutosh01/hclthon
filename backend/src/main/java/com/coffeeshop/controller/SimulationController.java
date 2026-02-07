package com.coffeeshop.controller;

import com.coffeeshop.service.SimulationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@RestController
@RequestMapping("/simulate")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SimulationController {

    private final SimulationService simulationService;

    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> start() {
        simulationService.startSimulation();
        return ResponseEntity.ok(Map.of(
            "status", "running",
            "message", "Simulation started. Customers arrive via Poisson (λ=1.4/min)."
        ));
    }

    @PostMapping("/stop")
    public ResponseEntity<Map<String, Object>> stop() {
        simulationService.stopSimulation();
        return ResponseEntity.ok(Map.of(
            "status", "stopped",
            "message", "Simulation stopped."
        ));
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Boolean>> status() {
        return ResponseEntity.ok(Map.of("running", simulationService.isRunning()));
    }
}
