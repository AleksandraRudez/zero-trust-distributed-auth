package com.zerotrust.serviceb.controller;

import com.zerotrust.serviceb.model.ApiResponse;
import com.zerotrust.serviceb.security.Context;
import com.zerotrust.serviceb.security.PolicyEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/policies")
@CrossOrigin("*")
public class PolicyAdminController {

    @Autowired
    private PolicyEngine policyEngine;

    @GetMapping
    public ResponseEntity<?> getPolicies(@RequestAttribute("context") Context context) {
        log.info("Admin {} pregleda politike", context.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Trenutne politike", Map.of(
                "allowedInternalCallers", policyEngine.getAllowedInternalCallers(),
                "accessWindowStart", policyEngine.getAccessWindowStart().toString(),
                "accessWindowEnd", policyEngine.getAccessWindowEnd().toString()
        )));
    }

    @PutMapping("/callers")
    public ResponseEntity<?> updateCaller(@RequestAttribute("context") Context context,
                                          @RequestBody Map<String, String> body) {
        String caller = body.get("caller");
        String action = body.get("action");
        log.info("Admin {} menja whitelist: {} {}", context.getUserId(), action, caller);

        if ("add".equalsIgnoreCase(action)) {
            policyEngine.addAllowedCaller(caller);
        } else if ("remove".equalsIgnoreCase(action)) {
            policyEngine.removeAllowedCaller(caller);
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error("Nepoznata akcija: " + action));
        }

        return ResponseEntity.ok(ApiResponse.success("Whitelist ažuriran",
                Map.of("allowedInternalCallers", policyEngine.getAllowedInternalCallers())));
    }

    @PutMapping("/window")
    public ResponseEntity<?> updateWindow(@RequestAttribute("context") Context context,
                                          @RequestBody Map<String, String> body) {
        LocalTime start = LocalTime.parse(body.get("start"));
        LocalTime end = LocalTime.parse(body.get("end"));
        log.info("Admin {} menja vremenski prozor: {} - {}", context.getUserId(), start, end);

        policyEngine.setAccessWindow(start, end);

        return ResponseEntity.ok(ApiResponse.success("Vremenski prozor ažuriran", Map.of(
                "accessWindowStart", start.toString(),
                "accessWindowEnd", end.toString()
        )));
    }
}