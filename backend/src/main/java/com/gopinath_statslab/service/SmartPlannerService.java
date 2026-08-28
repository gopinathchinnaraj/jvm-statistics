package com.gopinath_statslab.service;

import com.gopinath_statslab.model.ColumnStats;
import com.gopinath_statslab.model.QueryPlan;
import com.gopinath_statslab.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * SmartPlannerService — Phase 3: Implement a Possible Solution.
 *
 * Unlike NaivePlannerService which assumes uniform distribution,
 * this service uses the statistics collected by StatisticsCollectorService
 * to make accurate cardinality estimates.
 *
 * KEY DIFFERENCE:
 *   Naive: selectivity = 1 / assumedDistinctCount  (always wrong for skewed data)
 *   Smart: selectivity = MCV lookup OR histogram fraction  (close to truth)
 *
 * When statistics are GOOD  → smart planner estimates correctly → right plan
 * When statistics are CORRUPTED → smart planner is wrong again → wrong plan
 * This is the "before vs after" demo moment.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmartPlannerService {

    private final OrderRepository orderRepository;
    private final StatisticsStore statisticsStore;

    private static final long ASSUMED_TOTAL_ROWS = 1_000_000L;
    private static final double INDEX_SCAN_THRESHOLD  = 0.05;
    private static final double FILTER_SCAN_THRESHOLD = 0.20;

    public QueryPlan planByCountry(String country) {
        log.debug("SmartPlanner: planning WHERE country = '{}'", country);

        ColumnStats stats = statisticsStore.get("country");
        String statsMode = (stats != null) ? stats.getMode() : QueryPlan.STATS_NONE;

        double estimatedSelectivity;
        long totalRows = stats != null ? stats.getRowCount() : ASSUMED_TOTAL_ROWS;

        if (stats == null) {
            // No stats available — fall back to naive uniform assumption
            estimatedSelectivity = 0.1;
        } else {
            // Use MCV lookup — smart estimate!
            estimatedSelectivity = stats.estimateSelectivity(country);
        }

        long estimatedRows = Math.round(totalRows * estimatedSelectivity);
        String planType = choosePlanType(estimatedSelectivity);

        long startTime = System.currentTimeMillis();
        long actualRows = orderRepository.countByCountry(country);
        long actualCostMs = System.currentTimeMillis() - startTime;
        long estimatedCostMs = estimateCost(planType, estimatedRows, totalRows);

        double actualSelectivity = (double) actualRows / totalRows;
        boolean optimal = planType.equals(choosePlanType(actualSelectivity));

        String explanation = buildExplanation(
            "country", country, statsMode,
            estimatedSelectivity, estimatedRows, actualRows, planType, optimal
        );

        return QueryPlan.builder()
                .planType(planType)
                .estimatedRows(estimatedRows)
                .actualRows(actualRows)
                .estimatedCostMs(estimatedCostMs)
                .actualCostMs(actualCostMs)
                .statisticsMode(statsMode)
                .explanation(explanation)
                .selectivity(estimatedSelectivity)
                .columnFilter("country = '" + country + "'")
                .optimalDecision(optimal)
                .build();
    }

    public QueryPlan planByStatus(String status) {
        ColumnStats stats = statisticsStore.get("status");
        String statsMode = stats != null ? stats.getMode() : QueryPlan.STATS_NONE;

        double estimatedSelectivity = stats != null ? stats.estimateSelectivity(status) : 0.2;
        long totalRows = stats != null ? stats.getRowCount() : ASSUMED_TOTAL_ROWS;
        long estimatedRows = Math.round(totalRows * estimatedSelectivity);
        String planType = choosePlanType(estimatedSelectivity);

        long start = System.currentTimeMillis();
        long actualRows = orderRepository.countByStatus(status);
        long actualCostMs = System.currentTimeMillis() - start;

        boolean optimal = planType.equals(choosePlanType((double) actualRows / totalRows));

        return QueryPlan.builder()
                .planType(planType)
                .estimatedRows(estimatedRows)
                .actualRows(actualRows)
                .estimatedCostMs(estimateCost(planType, estimatedRows, totalRows))
                .actualCostMs(actualCostMs)
                .statisticsMode(statsMode)
                .explanation(buildExplanation("status", status, statsMode,
                        estimatedSelectivity, estimatedRows, actualRows, planType, optimal))
                .selectivity(estimatedSelectivity)
                .columnFilter("status = '" + status + "'")
                .optimalDecision(optimal)
                .build();
    }

    public QueryPlan planByAmountGreaterThan(BigDecimal minAmount) {
        ColumnStats stats = statisticsStore.get("amount");
        String statsMode = stats != null ? stats.getMode() : QueryPlan.STATS_NONE;

        double estimatedSelectivity;
        long totalRows = stats != null ? stats.getRowCount() : ASSUMED_TOTAL_ROWS;

        if (stats != null) {
            estimatedSelectivity = stats.estimateSelectivityGreaterThan(minAmount.doubleValue());
        } else {
            double assumedMax = 100_000.0, assumedMin = 10.0;
            estimatedSelectivity = (assumedMax - minAmount.doubleValue()) / (assumedMax - assumedMin);
            estimatedSelectivity = Math.max(0, Math.min(1, estimatedSelectivity));
        }

        long estimatedRows = Math.round(totalRows * estimatedSelectivity);
        String planType = choosePlanType(estimatedSelectivity);

        long start = System.currentTimeMillis();
        long actualRows = orderRepository.countByAmountGreaterThan(minAmount);
        long actualCostMs = System.currentTimeMillis() - start;

        boolean optimal = planType.equals(choosePlanType((double) actualRows / totalRows));

        String explanation = String.format(
            "Statistics mode: %s. Used histogram to estimate %.2f%% selectivity → %,d rows. Actual: %,d. Plan: %s. %s",
            statsMode, estimatedSelectivity * 100, estimatedRows, actualRows, planType,
            optimal ? "✓ Optimal plan." : "⚠ Suboptimal — histogram inaccurate."
        );

        return QueryPlan.builder()
                .planType(planType).estimatedRows(estimatedRows).actualRows(actualRows)
                .estimatedCostMs(estimateCost(planType, estimatedRows, totalRows))
                .actualCostMs(actualCostMs).statisticsMode(statsMode)
                .explanation(explanation).selectivity(estimatedSelectivity)
                .columnFilter("amount > " + minAmount).optimalDecision(optimal)
                .build();
    }

    // ── helpers ──────────────────────────────────────────────────

    private String choosePlanType(double selectivity) {
        if (selectivity < INDEX_SCAN_THRESHOLD)  return QueryPlan.PLAN_INDEX_SCAN;
        if (selectivity < FILTER_SCAN_THRESHOLD) return QueryPlan.PLAN_FILTER_SCAN;
        return QueryPlan.PLAN_FULL_SCAN;
    }

    private long estimateCost(String planType, long estimatedRows, long totalRows) {
        return switch (planType) {
            case QueryPlan.PLAN_FULL_SCAN   -> totalRows / 500;
            case QueryPlan.PLAN_FILTER_SCAN -> estimatedRows / 800;
            case QueryPlan.PLAN_INDEX_SCAN  -> Math.max(1, estimatedRows / 2000);
            default -> estimatedRows / 500;
        };
    }

    private String buildExplanation(String col, String value, String statsMode,
            double selectivity, long estimated, long actual, String plan, boolean optimal) {
        String source = "NONE".equals(statsMode) ? "No statistics — uniform fallback."
                : "CORRUPTED".equals(statsMode)  ? "⚠ Corrupted statistics used (uniform MCV)."
                : "Good MCV statistics used.";
        return String.format(
            "%s Column '%s' = '%s'. Estimated selectivity: %.4f%% → %,d rows. Actual: %,d. Plan: %s. %s",
            source, col, value, selectivity * 100, estimated, actual, plan,
            optimal ? "✓ Correct plan." : "⚠ Wrong plan chosen — estimation error."
        );
    }
}
