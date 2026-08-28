package com.gopinath_statslab.service;

import com.gopinath_statslab.model.QueryPlan;
import com.gopinath_statslab.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * NaivePlannerService — Phase 1: Reproduce the Problem.
 *
 * This service simulates a query planner that has NO statistics.
 * It makes decisions based on assumptions — exactly like a real
 * database planner would behave before ANALYZE has ever been run.
 *
 * LEARNING NOTES
 * ──────────────
 * @Service
 *   Marks this as a Spring bean. Spring creates one instance of this
 *   class and makes it available for injection wherever needed.
 *   Semantically equivalent to @Component but conveys "this is a
 *   business logic class."
 *
 * @RequiredArgsConstructor (Lombok)
 *   Generates a constructor with all `final` fields as parameters.
 *   Spring sees the constructor and injects the OrderRepository
 *   dependency automatically (constructor injection — the preferred way).
 *
 * @Slf4j (Lombok)
 *   Injects a logger: private static final Logger log = ...
 *   Use log.debug(), log.info(), log.warn(), log.error().
 *
 * THE CORE PROBLEM THIS CLASS DEMONSTRATES
 * ─────────────────────────────────────────
 * Without statistics, a planner must assume a UNIFORM DISTRIBUTION.
 * It has no idea that:
 *   country = 'India'   → 700,000 rows (70%)
 *   country = 'Iceland' →     120 rows  (0.012%)
 *
 * So it uses a default assumption like:
 *   "There are N distinct countries. Each country has 1/N of the rows."
 *
 * If the planner assumes 10 distinct countries:
 *   Estimated rows for any country = 1,000,000 / 10 = 100,000
 *
 * That estimate is wildly wrong for Iceland (actual: 120).
 * A selectivity of 10% triggers a FULL_SCAN decision.
 * Result: the planner reads 1,000,000 rows to return 120.
 *
 * THRESHOLD LOGIC
 * ───────────────
 * These thresholds mirror how real planners (PostgreSQL, MySQL, etc.)
 * decide between scan strategies:
 *
 *   selectivity < 5%   → INDEX_SCAN   (rare → index is worth it)
 *   selectivity 5–20%  → FILTER_SCAN  (moderate → partial scan)
 *   selectivity > 20%  → FULL_SCAN    (common → faster to scan all)
 *
 * The NAIVE planner always uses:
 *   estimatedSelectivity = 1.0 / assumedDistinctValues
 *
 * So it can NEVER correctly identify a rare value as rare.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NaivePlannerService {

    private final OrderRepository orderRepository;

    // ──────────────────────────────────────────────────────────────
    // The naive planner's hardcoded assumptions (no statistics)
    // ──────────────────────────────────────────────────────────────

    /** Total rows — naive planner assumes it doesn't know, uses a round estimate */
    private static final long ASSUMED_TOTAL_ROWS = 1_000_000L;

    /**
     * How many distinct values does the naive planner assume for each column?
     * Without statistics, it just picks a default.
     * This is the ROOT CAUSE of bad estimates for skewed data.
     */
    private static final int ASSUMED_DISTINCT_COUNTRIES  = 10;
    private static final int ASSUMED_DISTINCT_STATUSES   = 5;
    private static final int ASSUMED_DISTINCT_CATEGORIES = 10;

    /** Selectivity thresholds — determines which plan type to choose */
    private static final double INDEX_SCAN_THRESHOLD  = 0.05;  // < 5%
    private static final double FILTER_SCAN_THRESHOLD = 0.20;  // 5% – 20%
    // > 20% → FULL_SCAN

    // ──────────────────────────────────────────────────────────────
    // Public API
    // ──────────────────────────────────────────────────────────────

    /**
     * Plan and execute: WHERE country = ?
     *
     * Step-by-step:
     *  1. Estimate rows using naive uniform distribution assumption
     *  2. Pick a plan type based on estimated selectivity
     *  3. Actually run the count query to get actualRows
     *  4. Measure execution time
     *  5. Return QueryPlan with estimated vs actual comparison
     */
    public QueryPlan planByCountry(String country) {
        log.debug("NaivePlanner: planning WHERE country = '{}'", country);

        // STEP 1 — Naive cardinality estimate
        // Assumption: all countries are equally likely (uniform distribution)
        // Formula: totalRows / distinctCountries
        double estimatedSelectivity = 1.0 / ASSUMED_DISTINCT_COUNTRIES;
        long estimatedRows = Math.round(ASSUMED_TOTAL_ROWS * estimatedSelectivity);

        // STEP 2 — Choose plan type based on estimated selectivity
        String planType = choosePlanType(estimatedSelectivity);

        // STEP 3 & 4 — Run the actual query and measure time
        long startTime = System.currentTimeMillis();
        long actualRows = orderRepository.countByCountry(country);
        long actualCostMs = System.currentTimeMillis() - startTime;

        // STEP 5 — Simulate estimated cost based on plan type
        long estimatedCostMs = estimateCost(planType, estimatedRows);

        // Build the explanation string shown in the UI
        String explanation = buildExplanation(
                country, "country", estimatedRows, actualRows,
                estimatedSelectivity, planType, ASSUMED_DISTINCT_COUNTRIES
        );

        // Determine if the decision was optimal
        // (it would be optimal if estimated selectivity was close to actual)
        double actualSelectivity = (double) actualRows / ASSUMED_TOTAL_ROWS;
        String optimalPlanType = choosePlanType(actualSelectivity);
        boolean optimal = planType.equals(optimalPlanType);

        log.info("NaivePlanner country='{}': estimated={} actual={} plan={} optimal={}",
                country, estimatedRows, actualRows, planType, optimal);

        return QueryPlan.builder()
                .planType(planType)
                .estimatedRows(estimatedRows)
                .actualRows(actualRows)
                .estimatedCostMs(estimatedCostMs)
                .actualCostMs(actualCostMs)
                .statisticsMode(QueryPlan.STATS_NONE)
                .explanation(explanation)
                .selectivity(estimatedSelectivity)
                .columnFilter("country = '" + country + "'")
                .optimalDecision(optimal)
                .build();
    }

    /**
     * Plan and execute: WHERE status = ?
     */
    public QueryPlan planByStatus(String status) {
        log.debug("NaivePlanner: planning WHERE status = '{}'", status);

        double estimatedSelectivity = 1.0 / ASSUMED_DISTINCT_STATUSES;
        long estimatedRows = Math.round(ASSUMED_TOTAL_ROWS * estimatedSelectivity);
        String planType = choosePlanType(estimatedSelectivity);

        long startTime = System.currentTimeMillis();
        long actualRows = orderRepository.countByStatus(status);
        long actualCostMs = System.currentTimeMillis() - startTime;

        long estimatedCostMs = estimateCost(planType, estimatedRows);
        String explanation = buildExplanation(
                status, "status", estimatedRows, actualRows,
                estimatedSelectivity, planType, ASSUMED_DISTINCT_STATUSES
        );

        double actualSelectivity = (double) actualRows / ASSUMED_TOTAL_ROWS;
        boolean optimal = planType.equals(choosePlanType(actualSelectivity));

        return QueryPlan.builder()
                .planType(planType)
                .estimatedRows(estimatedRows)
                .actualRows(actualRows)
                .estimatedCostMs(estimatedCostMs)
                .actualCostMs(actualCostMs)
                .statisticsMode(QueryPlan.STATS_NONE)
                .explanation(explanation)
                .selectivity(estimatedSelectivity)
                .columnFilter("status = '" + status + "'")
                .optimalDecision(optimal)
                .build();
    }

    /**
     * Plan and execute: WHERE amount > ?
     * For numeric ranges, the naive planner assumes a UNIFORM distribution
     * across [min, max] — but our amount data is heavily skewed toward
     * the low end (squared distribution). So this estimate will also be wrong.
     */
    public QueryPlan planByAmountGreaterThan(BigDecimal minAmount) {
        log.debug("NaivePlanner: planning WHERE amount > {}", minAmount);

        // Assume amount range is 10 to 100,000. Naive: uniform distribution.
        double assumedMin = 10.0;
        double assumedMax = 100_000.0;
        double range = assumedMax - assumedMin;
        double requestedMin = minAmount.doubleValue();

        double estimatedSelectivity = (assumedMax - Math.max(requestedMin, assumedMin)) / range;
        estimatedSelectivity = Math.max(0.0, Math.min(1.0, estimatedSelectivity));
        long estimatedRows = Math.round(ASSUMED_TOTAL_ROWS * estimatedSelectivity);
        String planType = choosePlanType(estimatedSelectivity);

        long startTime = System.currentTimeMillis();
        long actualRows = orderRepository.countByAmountGreaterThan(minAmount);
        long actualCostMs = System.currentTimeMillis() - startTime;

        long estimatedCostMs = estimateCost(planType, estimatedRows);

        String explanation = String.format(
                "No statistics. Assumed uniform distribution of amount in [%.0f, %.0f]. " +
                "Estimated %.1f%% selectivity → %,d rows → %s chosen.",
                assumedMin, assumedMax, estimatedSelectivity * 100, estimatedRows, planType
        );

        double actualSelectivity = (double) actualRows / ASSUMED_TOTAL_ROWS;
        boolean optimal = planType.equals(choosePlanType(actualSelectivity));

        return QueryPlan.builder()
                .planType(planType)
                .estimatedRows(estimatedRows)
                .actualRows(actualRows)
                .estimatedCostMs(estimatedCostMs)
                .actualCostMs(actualCostMs)
                .statisticsMode(QueryPlan.STATS_NONE)
                .explanation(explanation)
                .selectivity(estimatedSelectivity)
                .columnFilter("amount > " + minAmount)
                .optimalDecision(optimal)
                .build();
    }

    // ──────────────────────────────────────────────────────────────
    // Private helpers
    // ──────────────────────────────────────────────────────────────

    /**
     * Chooses plan type based on estimated selectivity.
     *
     * These thresholds mirror real-world planner behaviour:
     *   < 5%  → so few rows that index traversal overhead is worth it
     *   5–20% → moderate — partial scan or filter
     *   > 20% → most rows match anyway, full scan is cheaper
     */
    private String choosePlanType(double selectivity) {
        if (selectivity < INDEX_SCAN_THRESHOLD) {
            return QueryPlan.PLAN_INDEX_SCAN;
        } else if (selectivity < FILTER_SCAN_THRESHOLD) {
            return QueryPlan.PLAN_FILTER_SCAN;
        } else {
            return QueryPlan.PLAN_FULL_SCAN;
        }
    }

    /**
     * Simulates the estimated execution cost in milliseconds.
     *
     * These are intentionally simplified for demo purposes:
     *   FULL_SCAN   = scan all rows at ~500 rows/ms
     *   FILTER_SCAN = scan estimated rows at ~800 rows/ms
     *   INDEX_SCAN  = lookup estimated rows at ~2000 rows/ms (index is fast)
     */
    private long estimateCost(String planType, long estimatedRows) {
        return switch (planType) {
            case QueryPlan.PLAN_FULL_SCAN   -> ASSUMED_TOTAL_ROWS / 500;
            case QueryPlan.PLAN_FILTER_SCAN -> estimatedRows / 800;
            case QueryPlan.PLAN_INDEX_SCAN  -> Math.max(1, estimatedRows / 2000);
            default -> estimatedRows / 500;
        };
    }

    /**
     * Builds the human-readable explanation string shown in the UI.
     */
    private String buildExplanation(
            String value, String column,
            long estimatedRows, long actualRows,
            double selectivity, String planType,
            int assumedDistinct
    ) {
        return String.format(
                "No statistics available for column '%s'. " +
                "Assumed %d distinct values with uniform distribution. " +
                "Estimated selectivity: %.4f%% → %,d estimated rows. " +
                "Actual rows: %,d. " +
                "Plan chosen: %s. " +
                "%s",
                column,
                assumedDistinct,
                selectivity * 100,
                estimatedRows,
                actualRows,
                planType,
                planType.equals(QueryPlan.PLAN_FULL_SCAN) && actualRows < 10_000
                        ? "⚠ Suboptimal: full scan for a rare value. This is the core problem."
                        : "✓ Plan is reasonable given the estimate."
        );
    }
}
