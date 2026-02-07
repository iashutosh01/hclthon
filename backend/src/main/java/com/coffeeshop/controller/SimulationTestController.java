package com.coffeeshop.controller;

import com.coffeeshop.dto.SimulationResultDTO;
import com.coffeeshop.service.TestSimulationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/simulate")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class SimulationTestController {

    private final TestSimulationService testSimulationService;


    @PostMapping("/test")
    public ResponseEntity<SimulationResultDTO> startTestSimulation() {
        TestSimulationService.SimulationResult result = testSimulationService.runMonteCarlo();
        SimulationResultDTO dto = SimulationResultDTO.from(result);
        return ResponseEntity.ok(dto);
    }
}
