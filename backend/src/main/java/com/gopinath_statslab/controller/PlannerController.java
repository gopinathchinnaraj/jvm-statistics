package com.gopinath_statslab.controller;

import com.gopinath_statslab.model.BenchmarkResult;
import com.gopinath_statslab.model.QueryPlan;
import com.gopinath_statslab.service.BenchmarkService;
import com.gopinath_statslab.service.NaivePlannerService;
import com.gopinath_statslab.service.SmartPlannerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * PlannerController — unified REST API for all planner operations.
 *
 * Naive planner  → /api/planner/naive/*
 * Smart planner  → /api/planner/smart/*
 * Benchmark      → /api/planner/benchmark/*
 * Health         → /api/planner/health
 *
 * Note: QueryPlannerController.java has the original naive endpoints.
 * This controller adds smart + benchmark on the same base path.
 */
@Slf4j
@RestController
@RequestMapping("/api/planner")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
@RequiredArgsConstructor
public class PlannerController {

    private final NaivePlannerService naivePlannerService;
    private final SmartPlannerService smartPlannerService;
    private final BenchmarkService benchmarkService;

    // ── Smart planner ────────────────────────────────────────────

    @PostMapping("/smart/country")
    public ResponseEntity<QueryPlan> smartCountry(@RequestBody Map<String, String> body) {
        String country = body.getOrDefault("country", "Iceland");
        return ResponseEntity.ok(smartPlannerService.planByCountry(country));
    }

    @PostMapping("/smart/status")
    public ResponseEntity<QueryPlan> smartStatus(@RequestBody Map<String, String> body) {
        String status = body.getOrDefault("status", "REFUNDED");
        return ResponseEntity.ok(smartPlannerService.planByStatus(status));
    }

    @PostMapping("/smart/amount")
    public ResponseEntity<QueryPlan> smartAmount(@RequestBody Map<String, String> body) {
        BigDecimal amount = new BigDecimal(body.getOrDefault("minAmount", "90000"));
        return ResponseEntity.ok(smartPlannerService.planByAmountGreaterThan(amount));
    }

    // ── Benchmark ────────────────────────────────────────────────

    @PostMapping("/benchmark/country")
    public ResponseEntity<BenchmarkResult> benchCountry(@RequestBody Map<String, String> body) {
        String country = body.getOrDefault("country", "Iceland");
        return ResponseEntity.ok(benchmarkService.benchmarkCountry(country));
    }

    @PostMapping("/benchmark/status")
    public ResponseEntity<BenchmarkResult> benchStatus(@RequestBody Map<String, String> body) {
        String status = body.getOrDefault("status", "REFUNDED");
        return ResponseEntity.ok(benchmarkService.benchmarkStatus(status));
    }

    @PostMapping("/benchmark/amount")
    public ResponseEntity<BenchmarkResult> benchAmount(@RequestBody Map<String, String> body) {
        BigDecimal amount = new BigDecimal(body.getOrDefault("minAmount", "90000"));
        return ResponseEntity.ok(benchmarkService.benchmarkAmount(amount));
    }
}
