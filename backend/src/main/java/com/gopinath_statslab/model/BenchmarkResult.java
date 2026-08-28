package com.gopinath_statslab.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Result of running the same query through both the naive and smart planner */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BenchmarkResult {
    private String columnFilter;
    private QueryPlan naivePlan;
    private QueryPlan smartPlan;

    private long actualRows;
    private long actualCostMs;

    // Derived fields shown in the UI
    private long estimationErrorNaive;   // |naive.estimated - actual|
    private long estimationErrorSmart;   // |smart.estimated - actual|
    private boolean smartIsBetter;
    private String summary;
}
