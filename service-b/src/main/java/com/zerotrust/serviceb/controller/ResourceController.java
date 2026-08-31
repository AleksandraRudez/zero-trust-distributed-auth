package com.zerotrust.serviceb.controller;

import com.zerotrust.serviceb.client.ServiceAClient;
import com.zerotrust.serviceb.model.ApiResponse;
import com.zerotrust.serviceb.security.Context;
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
    private ServiceAClient serviceAClient;


    @GetMapping("/public/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(ApiResponse.success(
                "Service B je gore i radi",
                Map.of("service", "Service B", "port", "8082")
        ));
    }

    @GetMapping("/orchestrate")
    public ResponseEntity<?> orchestrate(@RequestAttribute("context") Context context) {
        log.info("GET /api/orchestrate - User: {}, Role: {}", context.getUserId(), context.getRole());

        try {
            ResponseEntity<Map> serviceAResponse = serviceAClient.ping();

            Map<String, Object> result = new HashMap<>();
            result.put("message", "Orchestration rezultat");
            result.put("user", context.getUserId());
            result.put("serviceAResponse", serviceAResponse.getBody());
            result.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(ApiResponse.success(
                    "Orchestration uspešan - Service B pozvao Service A",
                    result
            ));
        } catch (Exception e) {
            log.error("Orchestration failed: {}", e.getMessage());
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Orchestration failed: " + e.getMessage()
            ));
        }
    }

    // Endpoint pristup USER rola
    @PostMapping("/data")
    public ResponseEntity<?> processData(@RequestAttribute("context") Context context,
                                         @RequestBody Map<String, String> payload) {
        log.info("POST /api/data - User: {}", context.getUserId());
        return processInternal(context, payload);
    }

    // Endpoint za service-to-service pozive
    @PostMapping("/internal/data")
    public ResponseEntity<?> processDataInternal(@RequestAttribute("context") Context context,
                                                 @RequestBody Map<String, String> payload) {
        log.info("POST /api/internal/data - Service: {}", context.getUserId());
        return processInternal(context, payload);
    }

    private ResponseEntity<?> processInternal(Context context, Map<String, String> payload) {
        try {
            Map<String, String> dataToSend = Map.of(
                    "data", payload.getOrDefault("data", "default-data"),
                    "processedBy", "Service B"
            );

            ResponseEntity<Map> serviceAResponse = serviceAClient.callServiceAInternalProcess(dataToSend);

            Map<String, Object> result = new HashMap<>();
            result.put("message", "Podaci obrađeni");
            result.put("user", context.getUserId());
            result.put("serviceAResult", serviceAResponse.getBody());
            result.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(ApiResponse.success(
                    "Podaci uspešno obrađeni (Service B -> Service A)",
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
                "service", "Service B",
                "pong", "alive",
                "caller", context.getUserId()
        ));
    }
}