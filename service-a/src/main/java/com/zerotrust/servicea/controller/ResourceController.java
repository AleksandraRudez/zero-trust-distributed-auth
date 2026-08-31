package com.zerotrust.servicea.controller;

import com.zerotrust.servicea.model.ApiResponse;
import com.zerotrust.servicea.security.Context;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
@CrossOrigin("*")
public class ResourceController {


    @GetMapping("/public/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(ApiResponse.success(
                "Service A je gore i radi",
                Map.of("service", "Service A", "port", "8081")
        ));
    }

    //Endpoint samo za korisnike sa USER rolom

    @GetMapping("/data")
    public ResponseEntity<?> getData(@RequestAttribute("context") Context context) {
        log.info("GET /api/data - User: {}, Role: {}", context.getUserId(), context.getRole());

        Map<String, Object> data = new HashMap<>();
        data.put("message", "Ovo su podaci iz Service A");
        data.put("user", context.getUserId());
        data.put("role", context.getRole());
        data.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(ApiResponse.success(
                "Podaci uspešno preuzeti",
                data
        ));
    }

    // Endpointi za servise i admina
    @PostMapping("/internal/process")
    public ResponseEntity<?> processData(@RequestAttribute("context") Context context,
                                         @RequestBody Map<String, String> payload) {
        log.info("POST /api/internal/process - Service: {}", context.getUserId());

        Map<String, Object> result = new HashMap<>();
        result.put("processed", true);
        result.put("inputData", payload.get("data"));
        result.put("processedBy", context.getUserId());
        result.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(ApiResponse.success(
                "Podaci uspešno obrađeni u Service A",
                result
        ));
    }

    // Endpoint samo za admina
    @DeleteMapping("/admin/clear")
    public ResponseEntity<?> clearData(@RequestAttribute("context") Context context) {
        log.info("DELETE /api/admin/clear - Admin: {}", context.getUserId());

        return ResponseEntity.ok(ApiResponse.success(
                "Podaci obrisani (simulacija)",
                Map.of("cleared", true, "by", context.getUserId())
        ));
    }

    //Ping endpoint za interno korišćenje servisa
    @GetMapping("/internal/ping")
    public ResponseEntity<?> ping(@RequestAttribute("context") Context context) {
        log.info("Ping from: {}", context.getUserId());
        return ResponseEntity.ok(Map.of(
                "service", "Service A",
                "pong", "alive",
                "caller", context.getUserId()
        ));
    }
}