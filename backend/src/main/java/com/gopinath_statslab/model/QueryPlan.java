package com.gopinath_statslab.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * QueryPlan — the result returned by any planner (naive or smart).
 *
 * This is a Plain Old Java Object (POJO) — no JPA annotations,
 * no database mapping. It's a data transfer object (DTO) that holds
 * the output of the query planning decision and gets serialized to
 * JSON for the frontend via @RestController.
 *
 * FIELDS EXPLAINED
 * ────────────────
 * planType         — "FULL_SCAN" or "INDEX_SCAN" or "FILTER_SCAN"
 * estimatedRows    — how many rows the planner THINKS will match
 * actualRows       — how many rows ACTUALLY matched (ground truth)
 * estimatedCostMs  — planner's predicted execution time (simulated)
 * actualCostMs     — real measured execution time
 * statisticsMode   — "NONE" | "STALE" | "GOOD" | "CORRUPTED"
 * explanation      — human-readable explanation of the plan decision
 * selectivity      — estimated fraction of rows that match (0.0 to 1.0)
 * columnFilter     — the WHERE clause condition (e.g. "country = 'Iceland'")
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryPlan {

    /**
     * The execution strategy the planner chose.
     */
    private String planType;

    /**
     * How many rows the planner estimated would match.
     * With no statistics: this is a wild guess.
     * With good statistics: this should be close to actualRows.
     */
    private long estimatedRows;

    /**
     * The true number of rows that matched the query.
     * Obtained by actually running the query against the DB.
     */
    private long actualRows;

    /**
     * Simulated estimated cost in milliseconds.
     * Formula (naive):   totalRows / 1000  (assume ~1000 rows/ms for full scan)
     * Formula (indexed): estimatedRows / 100 (index lookup is ~10x faster)
     */
    private long estimatedCostMs;

    /**
     * Actual measured wall-clock time to run the DB query.
     */
    private long actualCostMs;

    /**
     * The quality of statistics used to make this decision.
     * NONE      — no statistics at all (Phase 1, naive planner)
     * GOOD      — accurate histograms + MCV (Phase 3, smart planner)
     * CORRUPTED — deliberately wrong statistics (demo mode)
     */
    private String statisticsMode;

    /**
     * Human-readable explanation shown in the UI, e.g.:
     * "No statistics available. Assumed uniform distribution.
     *  Estimated 100,000 rows. Chose FULL_SCAN."
     */
    private String explanation;

    /**
     * Estimated fraction of rows that satisfy the WHERE clause.
     * 0.0001 = 0.01% (Iceland ~120 rows out of 1M)
     * 0.7000 = 70%   (India ~700,000 rows out of 1M)
     */
    private double selectivity;

    /**
     * The filter condition this plan was built for.
     * Example: "country = 'Iceland'"
     */
    private String columnFilter;

    /**
     * Whether the planner made a GOOD decision.
     * True  = plan type matches what an optimal planner would choose.
     * False = suboptimal plan (e.g., full scan when index would be better).
     */
    private boolean optimalDecision;

    // ──────────────────────────────────────────────────────────────
    // Plan type constants — used throughout the service layer.
    // Using constants avoids typos like "full_scan" vs "FULL_SCAN".
    // ──────────────────────────────────────────────────────────────
    public static final String PLAN_FULL_SCAN    = "FULL_SCAN";
    public static final String PLAN_INDEX_SCAN   = "INDEX_SCAN";
    public static final String PLAN_FILTER_SCAN  = "FILTER_SCAN";

    public static final String STATS_NONE        = "NONE";
    public static final String STATS_GOOD        = "GOOD";
    public static final String STATS_CORRUPTED   = "CORRUPTED";
}
