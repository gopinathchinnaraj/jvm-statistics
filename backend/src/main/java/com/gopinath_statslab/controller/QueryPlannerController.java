package com.gopinath_statslab.controller;

import com.gopinath_statslab.model.QueryPlan;
import com.gopinath_statslab.service.NaivePlannerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * QueryPlannerController — REST API for Phase 1 (Naive Planner).
 *
 * LEARNING NOTES
 * ──────────────
 * @RestController
 *   = @Controller + @ResponseBody
 *   Every method return value is automatically serialized to JSON.
 *   You never call response.getWriter().write(...) yourself.
 *
 * @RequestMapping("/api/planner")
 *   All endpoints in this class start with /api/planner.
 *   So planByCountry() is available at: POST /api/planner/naive/country
 *
 * @CrossOrigin(origins = "http://localhost:5173")
 *   The React app runs on port 5173 (Vite default).
 *   Spring Boot runs on port 8080.
 *   Without CORS headers, the browser blocks requests from 5173 → 8080.
 *   This annotation adds the necessary Access-Control-Allow-Origin header.
 *
 * @PostMapping vs @GetMapping
 *   We use POST because the client sends a request body (the filter value).
 *   GET requests put parameters in the URL: ?country=Iceland
 *   POST requests put parameters in the JSON body — cleaner for complex queries.
 *
 * @RequestBody Map<String, String> body
 *   Spring deserializes the incoming JSON body into a Map.
 *   { "country": "Iceland" } → body.get("country") = "Iceland"
 *   In a larger app you'd create a dedicated request DTO class.
 *
 * ResponseEntity<QueryPlan>
 *   Wraps the response so you can control:
 *     - the HTTP status code (200 OK, 400 Bad Request, 500 Internal Server Error)
 *     - response headers
 *     - the body
 *
 * ENDPOINTS OVERVIEW
 * ──────────────────
 * POST /api/planner/naive/country    → plan WHERE country = ?
 * POST /api/planner/naive/status     → plan WHERE status = ?
 * POST /api/planner/naive/amount     → plan WHERE amount > ?
 * GET  /api/planner/health           → health check
 */
@Slf4j
@RestController
@RequestMapping("/api/planner")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
@RequiredArgsConstructor
public class QueryPlannerController {

    private final NaivePlannerService naivePlannerService;

    /**
     * POST /api/planner/naive/country
     *
     * Request body:
     * {
     *   "country": "Iceland"
     * }
     *
     * Response: QueryPlan JSON
     * {
     *   "planType": "FULL_SCAN",
     *   "estimatedRows": 100000,
     *   "actualRows": 120,
     *   ...
     * }
     *
     * This is the CORE Phase 1 demo endpoint. Run it with:
     *   { "country": "Iceland" }  → should show FULL_SCAN + bad estimate
     *   { "country": "India" }    → estimate will be wrong but plan might be OK
     */
    @PostMapping("/naive/country")
    public ResponseEntity<QueryPlan> planByCountry(@RequestBody Map<String, String> body) {
        String country = body.get("country");

        if (country == null || country.isBlank()) {
            return ResponseEntity
                    .badRequest()
                    .body(QueryPlan.builder()
                            .explanation("'country' field is required in request body.")
                            .build());
        }

        log.info("Naive plan requested: WHERE country = '{}'", country);
        QueryPlan plan = naivePlannerService.planByCountry(country);
        return ResponseEntity.ok(plan);
    }

    /**
     * POST /api/planner/naive/status
     *
     * Request body: { "status": "REFUNDED" }
     *
     * Interesting case: status has only 5 values, REFUNDED is only 4%.
     * Naive planner assumes 1/5 = 20% → might still choose FULL_SCAN.
     * Actual rows for REFUNDED ≈ 40,000.
     * Here the estimate is wrong but the plan might accidentally be OK.
     */
    @PostMapping("/naive/status")
    public ResponseEntity<QueryPlan> planByStatus(@RequestBody Map<String, String> body) {
        String status = body.get("status");

        if (status == null || status.isBlank()) {
            return ResponseEntity
                    .badRequest()
                    .body(QueryPlan.builder()
                            .explanation("'status' field is required in request body.")
                            .build());
        }

        log.info("Naive plan requested: WHERE status = '{}'", status);
        QueryPlan plan = naivePlannerService.planByStatus(status);
        return ResponseEntity.ok(plan);
    }

    /**
     * POST /api/planner/naive/amount
     *
     * Request body: { "minAmount": "90000" }
     *
     * Interesting case: amount is heavily skewed to the LOW end.
     * Naive planner assumes uniform distribution, so:
     *   WHERE amount > 90000 → estimated selectivity = (100000-90000)/100000 = 10%
     *   → FILTER_SCAN chosen
     * But actual rows with amount > 90000 might be far fewer
     * because most amounts are concentrated in the 10–5000 range.
     */
    @PostMapping("/naive/amount")
    public ResponseEntity<QueryPlan> planByAmount(@RequestBody Map<String, String> body) {
        String minAmountStr = body.get("minAmount");

        if (minAmountStr == null || minAmountStr.isBlank()) {
            return ResponseEntity
                    .badRequest()
                    .body(QueryPlan.builder()
                            .explanation("'minAmount' field is required in request body.")
                            .build());
        }

        try {
            BigDecimal minAmount = new BigDecimal(minAmountStr);
            log.info("Naive plan requested: WHERE amount > {}", minAmount);
            QueryPlan plan = naivePlannerService.planByAmountGreaterThan(minAmount);
            return ResponseEntity.ok(plan);
        } catch (NumberFormatException e) {
            return ResponseEntity
                    .badRequest()
                    .body(QueryPlan.builder()
                            .explanation("'minAmount' must be a valid number. Got: " + minAmountStr)
                            .build());
        }
    }

    /**
     * GET /api/planner/health
     *
     * Simple health check — confirms the backend is reachable.
     * The frontend polls this on startup to show a "connected" indicator.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "QueryPlannerController",
                "phase", "1 - Naive Planner (no statistics)",
                "timestamp", System.currentTimeMillis()
        ));
    }
}
