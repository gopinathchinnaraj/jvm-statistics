package com.gopinath_statslab.service;

import com.gopinath_statslab.model.BenchmarkResult;
import com.gopinath_statslab.model.QueryPlan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * BenchmarkService — Phase 4: Before vs After.
 *
 * Runs the SAME query through BOTH planners and returns a side-by-side result.
 * This produces the final comparison table shown in the UI:
 *
 *              BEFORE (Naive)    AFTER (Smart)
 *  Estimated   100,000           137
 *  Actual      120               120
 *  Plan        FULL_SCAN ⚠       INDEX_SCAN ✓
 *  Cost (est)  2,000 ms          1 ms
 *  Cost (real) 850 ms            18 ms
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BenchmarkService {

    private final NaivePlannerService naivePlanner;
    private final SmartPlannerService smartPlanner;

    public BenchmarkResult benchmarkCountry(String country) {
        log.info("Benchmark: WHERE country = '{}'", country);
        QueryPlan naive = naivePlanner.planByCountry(country);
        QueryPlan smart = smartPlanner.planByCountry(country);
        return buildResult("country = '" + country + "'", naive, smart);
    }

    public BenchmarkResult benchmarkStatus(String status) {
        log.info("Benchmark: WHERE status = '{}'", status);
        QueryPlan naive = naivePlanner.planByStatus(status);
        QueryPlan smart = smartPlanner.planByStatus(status);
        return buildResult("status = '" + status + "'", naive, smart);
    }

    public BenchmarkResult benchmarkAmount(BigDecimal minAmount) {
        log.info("Benchmark: WHERE amount > {}", minAmount);
        QueryPlan naive = naivePlanner.planByAmountGreaterThan(minAmount);
        QueryPlan smart = smartPlanner.planByAmountGreaterThan(minAmount);
        return buildResult("amount > " + minAmount, naive, smart);
    }

    private BenchmarkResult buildResult(String filter, QueryPlan naive, QueryPlan smart) {
        long actual = smart.getActualRows(); // both run real query; use smart's result
        long errorNaive = Math.abs(naive.getEstimatedRows() - actual);
        long errorSmart = Math.abs(smart.getEstimatedRows() - actual);
        boolean smartBetter = errorSmart < errorNaive;

        String summary = String.format(
            "Estimation error: Naive=%,d rows off | Smart=%,d rows off. " +
            "Smart planner is %s. Plan: Naive=%s | Smart=%s.",
            errorNaive, errorSmart,
            smartBetter ? "MORE ACCURATE ✓" : "not better (stats may be corrupted)",
            naive.getPlanType(), smart.getPlanType()
        );

        return BenchmarkResult.builder()
                .columnFilter(filter)
                .naivePlan(naive)
                .smartPlan(smart)
                .actualRows(actual)
                .actualCostMs(smart.getActualCostMs())
                .estimationErrorNaive(errorNaive)
                .estimationErrorSmart(errorSmart)
                .smartIsBetter(smartBetter)
                .summary(summary)
                .build();
    }
}
