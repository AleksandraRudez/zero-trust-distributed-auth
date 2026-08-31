package com.zerotrust.servicec.controller;

import com.zerotrust.servicec.client.ServiceBClient;
import com.zerotrust.servicec.model.ApiResponse;
import com.zerotrust.servicec.security.Context;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
@CrossOrigin("*")
public class ResourceController {

    @Autowired
    private ServiceBClient serviceBClient;


    @GetMapping("/public/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(ApiResponse.success(
                "Service C je gore i radi",
                Map.of("service", "Service C", "port", "8083")
        ));
    }


    @GetMapping("/orchestrate")
    public ResponseEntity<?> orchestrate(@RequestAttribute("context") Context context) {
        log.info("GET /api/orchestrate - User: {}", context.getUserId());

        try {
            // Service C poziva Service B
            ResponseEntity<Map> serviceBResponse = serviceBClient.callServiceBOrchestrate(new HashMap<>());

            Map<String, Object> result = new HashMap<>();
            result.put("message", "Service C -> Service B -> Service A (kompletan lanac)");
            result.put("user", context.getUserId());
            result.put("serviceBResponse", serviceBResponse.getBody());
            result.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(ApiResponse.success(
                    "Kompletan lanac poziva uspešan",
                    result
            ));
        } catch (Exception e) {
            log.error("Orchestration failed: {}", e.getMessage());
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Orchestration failed: " + e.getMessage()
            ));
        }
    }


    @PostMapping("/data")
    public ResponseEntity<?> processData(@RequestAttribute("context") Context context,
                                         @RequestBody Map<String, String> payload) {
        log.info("POST /api/data - User: {}", context.getUserId());

        try {
            ResponseEntity<Map> serviceBResponse = serviceBClient.callServiceBData(payload);

            Map<String, Object> result = new HashMap<>();
            result.put("message", "Podaci prošli kroz lanac");
            result.put("user", context.getUserId());
            result.put("serviceBResult", serviceBResponse.getBody());
            result.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(ApiResponse.success(
                    "Podaci uspešno obrađeni kroz lanac servisa",
                    result
            ));
        } catch (Exception e) {
            log.error("Data processing failed: {}", e.getMessage());
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Processing failed: " + e.getMessage()
            ));
        }
    }


    @GetMapping("/internal/ping")
    public ResponseEntity<?> ping(@RequestAttribute("context") Context context) {
        log.info("Ping from: {}", context.getUserId());
        return ResponseEntity.ok(Map.of(
                "service", "Service C",
                "pong", "alive",
                "caller", context.getUserId()
        ));
    }
}